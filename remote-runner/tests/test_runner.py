import json, os, tempfile, threading, time, unittest
from http.server import ThreadingHTTPServer
from urllib.request import Request, urlopen
from urllib.error import HTTPError
from server import (Handler, GuestRunner, RunnerStore, PolicyError, validate_workflow, PROTOCOL)

GOOD={"name":"Open settings and verify","targetPackage":"com.example.app","steps":[
 {"type":"LAUNCH_APP","value":"","delayMillis":0},
 {"type":"WAIT_TEXT","value":"Settings","delayMillis":0}]}

class FakeGuest:
    def __init__(self, connected=True, seen=True): self.is_connected=connected; self.seen=seen; self.calls=[]
    def connected(self): return self.is_connected
    def launch(self,p): self.calls.append(("launch",p))
    def tap_text(self,t): self.calls.append(("tap",t))
    def back(self): self.calls.append(("back",))
    def wait_text(self,t,timeout=10): self.calls.append(("wait",t)); return self.seen

class RunnerTests(unittest.TestCase):
    def test_valid_workflow_is_bounded_and_normalized(self):
        result=validate_workflow(GOOD); self.assertEqual(result["steps"][-1]["type"],"WAIT_TEXT")
    def test_rejects_missing_postcondition(self):
        with self.assertRaisesRegex(PolicyError,"final step"): validate_workflow({**GOOD,"steps":GOOD["steps"][:1]})
    def test_rejects_shell_or_unknown_step(self):
        with self.assertRaisesRegex(PolicyError,"unsupported"): validate_workflow({**GOOD,"steps":[{"type":"SHELL","value":"rm -rf /"}]})
    def test_rejects_sensitive_text(self):
        with self.assertRaises(PolicyError): validate_workflow({**GOOD,"steps":[{"type":"TAP_TEXT","value":"Submit payment"},{"type":"WAIT_TEXT","value":"Done"}]})
    def test_rejects_too_many_steps(self):
        steps=[{"type":"DELAY","delayMillis":0} for _ in range(12)]+[{"type":"WAIT_TEXT","value":"Done"}]
        with self.assertRaises(PolicyError): validate_workflow({**GOOD,"steps":steps})
    def test_requires_explicit_approval_and_connected_guest(self):
        with tempfile.TemporaryDirectory() as d:
            store=RunnerStore(os.path.join(d,"db.sqlite")); guest=FakeGuest(); r=GuestRunner(store,guest,execute=False)
            with self.assertRaises(PolicyError): r.submit(GOOD,False)
            guest.is_connected=False
            with self.assertRaisesRegex(RuntimeError,"no authorized"): r.submit(GOOD,True)
    def test_verified_only_after_final_text_observed(self):
        with tempfile.TemporaryDirectory() as d:
            g=FakeGuest(seen=True); r=GuestRunner(RunnerStore(os.path.join(d,"db")),g,execute=False)
            jid=r.submit(GOOD,True)["id"]; r._execute(jid)
            self.assertEqual(r.store.get(jid)["status"],"VERIFIED")
    def test_missing_postcondition_is_unverified(self):
        with tempfile.TemporaryDirectory() as d:
            g=FakeGuest(seen=False); r=GuestRunner(RunnerStore(os.path.join(d,"db")),g,execute=False)
            jid=r.submit(GOOD,True)["id"]; r._execute(jid)
            self.assertEqual(r.store.get(jid)["status"],"UNVERIFIED")
    def test_job_survives_store_restart_and_running_is_interrupted(self):
        with tempfile.TemporaryDirectory() as d:
            path=os.path.join(d,"db"); s=RunnerStore(path); jid=s.create(validate_workflow(GOOD)); s.update(jid,status="RUNNING")
            s.db.close(); s2=RunnerStore(path); self.assertEqual(s2.get(jid)["status"],"INTERRUPTED")
    def test_health_declares_local_only(self):
        with tempfile.TemporaryDirectory() as d:
            r=GuestRunner(RunnerStore(os.path.join(d,"db")),FakeGuest(),execute=False); h=r.health()
            self.assertEqual(h["protocol"],PROTOCOL); self.assertEqual(h["aiPolicy"],"local-only")
    def test_api_requires_bearer_token_and_supports_health(self):
        with tempfile.TemporaryDirectory() as d:
            server=ThreadingHTTPServer(("127.0.0.1",0),Handler); server.token="t"*40; server.runner=GuestRunner(RunnerStore(os.path.join(d,"db")),FakeGuest(),execute=False)
            thread=threading.Thread(target=server.serve_forever,daemon=True); thread.start(); base=f"http://127.0.0.1:{server.server_port}"
            try:
                with self.assertRaises(HTTPError) as err: urlopen(base+"/health")
                self.assertEqual(err.exception.code,401)
                req=Request(base+"/health",headers={"Authorization":"Bearer "+server.token})
                with urlopen(req) as resp: body=json.loads(resp.read())
                self.assertEqual(body["aiPolicy"],"local-only")
            finally: server.shutdown(); server.server_close(); thread.join(2)
    def test_api_rejects_unapproved_submission(self):
        with tempfile.TemporaryDirectory() as d:
            server=ThreadingHTTPServer(("127.0.0.1",0),Handler); server.token="t"*40; server.runner=GuestRunner(RunnerStore(os.path.join(d,"db")),FakeGuest(),execute=False)
            thread=threading.Thread(target=server.serve_forever,daemon=True); thread.start(); base=f"http://127.0.0.1:{server.server_port}"
            try:
                data=json.dumps({"workflow":GOOD,"approved":False}).encode(); req=Request(base+"/jobs",data=data,headers={"Authorization":"Bearer "+server.token,"Content-Type":"application/json"},method="POST")
                with self.assertRaises(HTTPError) as err: urlopen(req)
                self.assertEqual(err.exception.code,400)
            finally: server.shutdown(); server.server_close(); thread.join(2)

if __name__=="__main__": unittest.main()
