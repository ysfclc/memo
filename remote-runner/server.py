#!/usr/bin/env python3
"""Optional, policy-constrained Memo guest runner. Standard library only."""
from __future__ import annotations
import argparse, hashlib, json, os, re, secrets, shutil, sqlite3, ssl, subprocess, tempfile, threading, time, uuid
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import urlparse

PROTOCOL = "synthos.remote-runner.v1"
ALLOWED = {"LAUNCH_APP", "TAP_TEXT", "WAIT_TEXT", "BACK", "DELAY"}
BLOCKED = re.compile(r"payment|purchase|transfer|submit|publish|delete|book|captcha|password|credential|security code|one.?time code|reward|advert", re.I)
MAX_BODY = 128 * 1024
MAX_STEPS = 12

class PolicyError(ValueError): pass

def validate_workflow(payload):
    if not isinstance(payload, dict): raise PolicyError("workflow must be an object")
    name = str(payload.get("name", "Memo workflow"))[:120]
    target = str(payload.get("targetPackage", ""))[:200]
    steps = payload.get("steps")
    if not target or not re.fullmatch(r"[A-Za-z0-9_.]+", target): raise PolicyError("valid targetPackage required")
    if not isinstance(steps, list) or not 1 <= len(steps) <= MAX_STEPS: raise PolicyError("workflow must contain 1-12 steps")
    cleaned = []
    for i, raw in enumerate(steps):
        if not isinstance(raw, dict): raise PolicyError(f"step {i+1} must be an object")
        kind = str(raw.get("type", ""))
        value = str(raw.get("value", ""))[:200]
        delay = raw.get("delayMillis", 0)
        if kind not in ALLOWED: raise PolicyError(f"unsupported step type at step {i+1}")
        if kind in {"TAP_TEXT", "WAIT_TEXT"} and (not value.strip() or BLOCKED.search(value)):
            raise PolicyError(f"unsafe or empty text selector at step {i+1}")
        if kind == "LAUNCH_APP" and value and value != target: raise PolicyError("launch package must match targetPackage")
        if kind == "DELAY":
            if not isinstance(delay, int) or isinstance(delay, bool) or not 0 <= delay <= 10000: raise PolicyError("delay must be 0-10000ms")
        cleaned.append({"type":kind,"value":value,"delayMillis":delay if isinstance(delay,int) and not isinstance(delay,bool) else 0})
    if cleaned[-1]["type"] != "WAIT_TEXT": raise PolicyError("final step must be WAIT_TEXT postcondition")
    if BLOCKED.search(name + " " + target): raise PolicyError("workflow contains a blocked sensitive action")
    return {"name":name,"targetPackage":target,"steps":cleaned}

class RunnerStore:
    def __init__(self, path):
        self.path = str(path); Path(self.path).parent.mkdir(parents=True, exist_ok=True)
        self.lock = threading.RLock(); self.db = sqlite3.connect(self.path, check_same_thread=False)
        self.db.execute("PRAGMA journal_mode=WAL")
        self.db.execute("CREATE TABLE IF NOT EXISTS jobs (id TEXT PRIMARY KEY, status TEXT NOT NULL, workflow TEXT NOT NULL, evidence TEXT NOT NULL, created REAL NOT NULL, updated REAL NOT NULL, cancel INTEGER NOT NULL DEFAULT 0)")
        self.db.commit()
        self.db.execute("UPDATE jobs SET status='INTERRUPTED', updated=? WHERE status='RUNNING'", (time.time(),)); self.db.commit()
    def create(self, workflow):
        jid = str(uuid.uuid4()); now=time.time()
        with self.lock:
            self.db.execute("INSERT INTO jobs VALUES(?,?,?,?,?,?,0)",(jid,"QUEUED",json.dumps(workflow),"[]",now,now)); self.db.commit()
        return jid
    def get(self, jid):
        with self.lock:
            row=self.db.execute("SELECT id,status,workflow,evidence,created,updated,cancel FROM jobs WHERE id=?",(jid,)).fetchone()
        if not row: return None
        return {"id":row[0],"status":row[1],"workflow":json.loads(row[2]),"evidence":json.loads(row[3]),"createdAt":row[4],"updatedAt":row[5],"cancelRequested":bool(row[6])}
    def update(self, jid, status=None, evidence=None, cancel=None):
        with self.lock:
            old=self.get(jid)
            if not old: return False
            self.db.execute("UPDATE jobs SET status=?, evidence=?, cancel=?, updated=? WHERE id=?",(status or old["status"],json.dumps(evidence if evidence is not None else old["evidence"]),int(cancel if cancel is not None else old["cancelRequested"]),time.time(),jid)); self.db.commit()
        return True

class AdbGuest:
    def __init__(self, adb="adb", serial=None, timeout=12): self.adb=adb; self.serial=serial; self.timeout=timeout
    def _run(self, *args, timeout=None):
        cmd=[self.adb]
        if self.serial: cmd += ["-s", self.serial]
        cmd += list(args)
        p=subprocess.run(cmd, capture_output=True, text=True, timeout=timeout or self.timeout, check=False)
        if p.returncode: raise RuntimeError((p.stderr or p.stdout or "ADB command failed")[:400])
        return p.stdout
    def connected(self):
        try: return "device" in self._run("get-state").strip()
        except Exception: return False
    def launch(self, package): self._run("shell","monkey","-p",package,"-c","android.intent.category.LAUNCHER","1")
    def back(self): self._run("shell","input","keyevent","4")
    def tap_text(self, text):
        nodes=self._hierarchy(); matches=[]
        for node in nodes:
            if node["text"] == text and node["clickable"]: matches.append(node)
        if len(matches)!=1: raise RuntimeError("visible text is missing, ambiguous, or not clickable")
        x,y=matches[0]["center"]; self._run("shell","input","tap",str(x),str(y))
    def wait_text(self, text, timeout=10):
        deadline=time.monotonic()+timeout
        while time.monotonic()<deadline:
            if any(n["text"]==text for n in self._hierarchy()): return True
            time.sleep(.35)
        return False
    def _hierarchy(self):
        # Keep the raw XML in a temporary file only; always remove it after parsing.
        fd,path=tempfile.mkstemp(prefix="synthos-ui-",suffix=".xml"); os.close(fd)
        try:
            self._run("shell","uiautomator","dump","/sdcard/window.xml",timeout=15)
            self._run("pull","/sdcard/window.xml",path,timeout=15)
            import xml.etree.ElementTree as ET
            root=ET.parse(path).getroot(); out=[]
            for n in root.iter("node"):
                text=n.attrib.get("text","")
                if not text: continue
                # Never return or persist editable field values.
                if n.attrib.get("class","").endswith(("EditText", "AutoCompleteTextView")): continue
                b=n.attrib.get("bounds",""); m=re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]",b)
                if not m: continue
                x1,y1,x2,y2=map(int,m.groups())
                out.append({"text":text[:160],"clickable":n.attrib.get("clickable")=="true","center":((x1+x2)//2,(y1+y2)//2)})
            return out
        finally:
            try: os.unlink(path)
            except OSError: pass
            try: self._run("shell","rm","-f","/sdcard/window.xml",timeout=4)
            except Exception: pass

class GuestRunner:
    def __init__(self, store, guest, execute=True): self.store=store; self.guest=guest; self.execute=execute; self.lock=threading.Lock()
    def health(self): return {"protocol":PROTOCOL,"capabilities":["native-allowlisted-workflows","visible-text-postconditions","cancellation"],"guestConnected":bool(self.guest.connected()),"aiPolicy":"local-only"}
    def submit(self, workflow, approved):
        if approved is not True: raise PolicyError("explicit user approval is required")
        clean=validate_workflow(workflow)
        if not self.guest.connected(): raise RuntimeError("no authorized guest device is connected")
        jid=self.store.create(clean)
        if self.execute: threading.Thread(target=self._execute,args=(jid,),daemon=True).start()
        return {"id":jid,"status":"QUEUED" if self.execute else "SIMULATED_NOT_EXECUTED"}
    def cancel(self, jid):
        job=self.store.get(jid)
        if not job: return False
        if job["status"] in {"VERIFIED","FAILED","CANCELLED","INTERRUPTED"}: return True
        self.store.update(jid,cancel=True); return True
    def _execute(self, jid):
        if not self.lock.acquire(blocking=False): self.store.update(jid,status="FAILED",evidence=[{"error":"runner busy; serial execution required"}]); return
        try:
            job=self.store.get(jid); wf=job["workflow"]; evidence=[]; verified=False; self.store.update(jid,status="RUNNING")
            for index,step in enumerate(wf["steps"]):
                job=self.store.get(jid)
                if job["cancelRequested"]: self.store.update(jid,status="CANCELLED",evidence=evidence+[{"step":index+1,"result":"cancelled"}]); return
                kind=step["type"]; value=step["value"]
                try:
                    if kind=="LAUNCH_APP": self.guest.launch(wf["targetPackage"])
                    elif kind=="TAP_TEXT": self.guest.tap_text(value)
                    elif kind=="BACK": self.guest.back()
                    elif kind=="DELAY": time.sleep(step["delayMillis"]/1000)
                    elif kind=="WAIT_TEXT":
                        ok=self.guest.wait_text(value)
                        evidence.append({"step":index+1,"type":kind,"result":"observed" if ok else "postcondition_missing","expected":value})
                        self.store.update(jid,evidence=evidence)
                        if not ok: self.store.update(jid,status="UNVERIFIED",evidence=evidence); return
                        verified = index == len(wf["steps"]) - 1
                        continue
                    evidence.append({"step":index+1,"type":kind,"result":"executed"})
                    self.store.update(jid,evidence=evidence)
                except Exception as exc:
                    evidence.append({"step":index+1,"type":kind,"result":"failed","error":str(exc)[:300]})
                    self.store.update(jid,status="FAILED",evidence=evidence); return
            self.store.update(jid,status="VERIFIED" if verified else "UNVERIFIED",evidence=evidence if verified else evidence+[ {"result":"missing_postcondition"} ])
        finally: self.lock.release()

class Handler(BaseHTTPRequestHandler):
    server_version="MemoGuest/1"
    def log_message(self, fmt, *args): pass
    def _json(self, code, payload):
        body=json.dumps(payload,separators=(",",":")).encode(); self.send_response(code); self.send_header("Content-Type","application/json; charset=utf-8"); self.send_header("Content-Length",str(len(body))); self.send_header("Cache-Control","no-store"); self.end_headers(); self.wfile.write(body)
    def _auth(self):
        supplied=self.headers.get("Authorization",""); expected="Bearer "+self.server.token
        return secrets.compare_digest(supplied.encode(),expected.encode())
    def _body(self):
        length=int(self.headers.get("Content-Length","0"))
        if length<=0 or length>MAX_BODY: raise PolicyError("request body must be 1-131072 bytes")
        raw=self.rfile.read(length)
        try: return json.loads(raw)
        except Exception: raise PolicyError("invalid JSON")
    def do_GET(self):
        if not self._auth(): return self._json(401,{"error":"unauthorized"})
        if self.path=="/health": return self._json(200,self.server.runner.health())
        if self.path.startswith("/jobs/"):
            job=self.server.runner.store.get(self.path.split("/",2)[2])
            return self._json(200,job) if job else self._json(404,{"error":"job not found"})
        return self._json(404,{"error":"not found"})
    def do_POST(self):
        if not self._auth(): return self._json(401,{"error":"unauthorized"})
        try:
            data=self._body()
            if self.path=="/jobs": return self._json(202,self.server.runner.submit(data.get("workflow"),data.get("approved") is True))
            if self.path.startswith("/jobs/") and self.path.endswith("/cancel"):
                jid=self.path.split("/")[2]
                return self._json(200,{"cancelRequested":self.server.runner.cancel(jid)})
            return self._json(404,{"error":"not found"})
        except PolicyError as e: return self._json(400,{"error":str(e)})
        except RuntimeError as e: return self._json(503,{"error":str(e)})
        except Exception: return self._json(500,{"error":"internal runner error"})

def main():
    p=argparse.ArgumentParser(); p.add_argument("--host",default="127.0.0.1"); p.add_argument("--port",type=int,default=8765); args=p.parse_args()
    token=os.environ.get("MEMO_BEARER_TOKEN", os.environ.get("SYNTHOS_BEARER_TOKEN", "")); cert=os.environ.get("MEMO_TLS_CERT", os.environ.get("SYNTHOS_TLS_CERT", "")); key=os.environ.get("MEMO_TLS_KEY", os.environ.get("SYNTHOS_TLS_KEY", ""))
    if len(token)<32 or not cert or not key: raise SystemExit("Set MEMO_BEARER_TOKEN (32+ chars), MEMO_TLS_CERT and MEMO_TLS_KEY")
    if args.host not in {"127.0.0.1","::1","localhost"} and os.environ.get("MEMO_ACK_PUBLIC_BIND", os.environ.get("SYNTHOS_ACK_PUBLIC_BIND", ""))!="YES_I_HAVE_FIREWALLED_AND_TLS_TERMINATED_THIS_HOST": raise SystemExit("Public bind refused; configure firewall/TLS and set MEMO_ACK_PUBLIC_BIND")
    adb=os.environ.get("MEMO_ADB", os.environ.get("SYNTHOS_ADB", "adb")); serial=os.environ.get("MEMO_ADB_SERIAL", os.environ.get("SYNTHOS_ADB_SERIAL", "")) or None; db=os.environ.get("MEMO_RUNNER_DB", os.environ.get("SYNTHOS_RUNNER_DB", "./memo-jobs.sqlite3"))
    runner=GuestRunner(RunnerStore(db),AdbGuest(adb,serial))
    server=ThreadingHTTPServer((args.host,args.port),Handler); server.token=token; server.runner=runner
    context=ssl.SSLContext(ssl.PROTOCOL_TLS_SERVER); context.load_cert_chain(cert,key); server.socket=context.wrap_socket(server.socket,server_side=True)
    print(f"Memo guest runner listening on https://{args.host}:{args.port} (AI policy: local-only)",flush=True)
    try: server.serve_forever()
    except KeyboardInterrupt: pass
    finally: server.server_close()
if __name__=="__main__": main()
