package app.memo.android.browser

import org.json.JSONObject

/** Scripts return JSON strings. They execute only inside the active WebView; no JavaScript bridge is exposed. */
object BrowserScripts {
    val capturePage: String = """
        (function(){
          const clean = s => (s || '').replace(/\u00a0/g, ' ').replace(/[\t ]+/g, ' ').trim();
          const visible = el => { const r = el.getBoundingClientRect(); const st = getComputedStyle(el); return r.width > 0 && r.height > 0 && st.visibility !== 'hidden' && st.display !== 'none'; };
          const body = document.body;
          const text = clean(body ? body.innerText : '').slice(0, 15000);
          const headings = Array.from(document.querySelectorAll('h1,h2,h3')).filter(visible).slice(0,80).map(e => clean(e.innerText)).filter(Boolean);
          const links = Array.from(document.querySelectorAll('a[href]')).filter(visible).slice(0,200).map(a => ({label:clean(a.innerText || a.getAttribute('aria-label') || a.title).slice(0,240),url:a.href})).filter(a => a.url && /^https?:/i.test(a.url));
          return JSON.stringify({ok:true,kind:'Web page',title:document.title || location.hostname,url:location.href,content:text,summary:headings.slice(0,12).join(' · ') || text.slice(0,360),metadata:{headings:headings,links:links,scrollY:window.scrollY,scrollHeight:document.documentElement ? document.documentElement.scrollHeight : 0,capturedAt:Date.now()}});
        })();
    """.trimIndent()

    val extractLinks: String = """
        (function(){
          const visible = el => { const r = el.getBoundingClientRect(); const st = getComputedStyle(el); return r.width > 0 && r.height > 0 && st.visibility !== 'hidden' && st.display !== 'none'; };
          const links = Array.from(document.querySelectorAll('a[href]')).filter(visible).map(a => ({label:(a.innerText || a.getAttribute('aria-label') || a.title || '').replace(/\s+/g,' ').trim().slice(0,240),url:a.href})).filter(a => a.url && /^https?:/i.test(a.url)).filter((a,i,arr) => arr.findIndex(x => x.url === a.url) === i).slice(0,300);
          return JSON.stringify({ok:true,kind:'Link collection',title:'Links · ' + (document.title || location.hostname),url:location.href,content:links.map((x,i)=>(i+1)+'. '+(x.label || '(no label)')+'\n'+x.url).join('\n\n') || 'No visible links found.',summary:links.length+' unique visible links collected.',metadata:{count:links.length,links:links}});
        })();
    """.trimIndent()

    fun findText(query: String): String {
        val q = JSONObject.quote(query.trim())
        return """
          (function(){
            const q = $q;
            const text = (document.body ? document.body.innerText : '').replace(/\u00a0/g,' ');
            const lower = text.toLocaleLowerCase(); const needle = q.toLocaleLowerCase(); const matches=[]; let from=0; let index;
            while(needle && (index=lower.indexOf(needle, from))>=0 && matches.length<30){ matches.push(text.slice(Math.max(0,index-240),Math.min(text.length,index+q.length+320)).replace(/\n{3,}/g,'\n\n').trim()); from=index+Math.max(needle.length,1); }
            return JSON.stringify({ok:true,kind:'Text search',title:'Matches · '+(document.title || location.hostname),url:location.href,content:matches.length ? matches.map((m,i)=>(i+1)+'. '+m).join('\n\n---\n\n') : 'No matching text found in the current page.',summary:matches.length+' match(es) for “'+q+'”.',metadata:{query:q,count:matches.length}});
          })();
        """.trimIndent()
    }

    fun preparePrompt(prompt: String): String {
        val quoted = JSONObject.quote(prompt)
        return """
          (function(){
            const prompt = $quoted;
            const visible = el => { const r = el.getBoundingClientRect(); const st = getComputedStyle(el); return r.width > 0 && r.height > 0 && st.visibility !== 'hidden' && st.display !== 'none' && !el.disabled; };
            const candidates = Array.from(document.querySelectorAll('textarea,[contenteditable="true"],[role="textbox"]')).filter(visible);
            if(!candidates.length) return JSON.stringify({ok:false,kind:'Prompt preparation',title:'Prompt not prepared',url:location.href,content:'No visible text composer was found. Open a conversation and try again.',summary:'Composer not found.',metadata:{}});
            const el = candidates[candidates.length-1]; el.focus();
            if(el.isContentEditable || el.getAttribute('contenteditable')==='true') { el.textContent=prompt; }
            else { const proto = Object.getPrototypeOf(el); const setter = Object.getOwnPropertyDescriptor(proto,'value') && Object.getOwnPropertyDescriptor(proto,'value').set; if(setter) setter.call(el,prompt); else el.value=prompt; }
            el.dispatchEvent(new InputEvent('input',{bubbles:true,inputType:'insertText',data:prompt})); el.dispatchEvent(new Event('change',{bubbles:true}));
            return JSON.stringify({ok:true,kind:'Prompt preparation',title:'Prompt prepared · '+(document.title || location.hostname),url:location.href,content:'Prompt filled into the visible composer. It has not been sent. Review it on the page before using Send prepared prompt.',summary:'Prompt filled; not sent.',metadata:{composer:el.tagName,role:el.getAttribute('role')||'',length:prompt.length}});
          })();
        """.trimIndent()
    }

    fun sendPreparedPrompt(prompt: String): String {
        val quoted = JSONObject.quote(prompt)
        return """
          (function(){
            const prompt = $quoted;
            const normalize = s => (s || '').replace(/\u00a0/g,' ').replace(/\s+/g,' ').trim();
            const visible = el => { const r=el.getBoundingClientRect(); const st=getComputedStyle(el); return r.width>0 && r.height>0 && st.visibility!=='hidden' && st.display!=='none' && !el.disabled; };
            const editors=Array.from(document.querySelectorAll('textarea,input,[contenteditable="true"],[role="textbox"]')).filter(visible);
            const editor=editors[editors.length-1];
            if(!editor) return JSON.stringify({ok:false,clicked:false,kind:'Prompt submission',title:'Cannot send prompt',url:location.href,content:'No visible composer was found. The prompt was not sent.',summary:'Composer not found.',metadata:{}});
            const value = () => normalize(editor.isContentEditable || editor.getAttribute('contenteditable')==='true' ? (editor.innerText || editor.textContent) : editor.value);
            const wanted=normalize(prompt);
            if(!wanted || !value().includes(wanted)) return JSON.stringify({ok:false,clicked:false,kind:'Prompt submission',title:'Composer changed',url:location.href,content:'The visible composer no longer contains the prepared prompt. Nothing was sent. Review the page and prepare the prompt again.',summary:'Prepared text no longer matches composer.',metadata:{}});
            const bodyText = () => {
              const clone=document.body ? document.body.cloneNode(true) : null;
              if(!clone) return '';
              clone.querySelectorAll('textarea,input,[contenteditable="true"],[role="textbox"],button,nav').forEach(el=>el.remove());
              return normalize(clone.innerText || clone.textContent);
            };
            const assistantTexts = () => {
              const host=(location.hostname||'').toLowerCase().replace(/^www\./,'');
              let selectors=['[data-message-author-role="assistant"]','[data-testid*="assistant-message"]','[data-testid*="model-response"]','[data-testid*="answer-content"]','[class*="assistant-message"]','[class*="model-response"]','[class*="answer-content"]','.model-response-text','.ds-markdown','.font-claude-message','main [class*="prose"]'];
              if(host==='gemini.google.com') selectors=['model-response','message-content','.model-response-text','[data-message-type="model-response"]',...selectors];
              if(host==='perplexity.ai') selectors=['[data-testid*="answer"]','main .prose','main [class*="prose"]',...selectors];
              if(host==='deepseek.com'||host==='chat.deepseek.com') selectors=['.ds-markdown','[class*="markdown"]',...selectors];
              for(const selector of selectors){
                const nodes=Array.from(document.querySelectorAll(selector)).filter(el=>{const r=el.getBoundingClientRect();const st=getComputedStyle(el);return r.width>0&&r.height>0&&st.visibility!=='hidden'&&st.display!=='none';});
                const values=[]; for(const node of nodes){const text=normalize(node.innerText||node.textContent);if(text.length>20&&!values.includes(text)&&!values.some(value=>value.length>text.length&&value.includes(text)))values.push(text);}
                if(values.length) return values.slice(-30);
              }
              return [];
            };
            const prefix=wanted.slice(0,Math.min(160,wanted.length));
            const occurrences=(text,needle)=>{ if(!needle) return 0; let count=0,from=0,at; while((at=text.toLocaleLowerCase().indexOf(needle.toLocaleLowerCase(),from))>=0){count++;from=at+needle.length;} return count; };
            const beforeText=bodyText();
            const countNode=document.querySelectorAll('[data-message-author-role="user"]').length;
            const beforeAssistant=assistantTexts();
            window.__synthosSubmitState={prompt:prompt,prefix:prefix,beforeOccurrences:occurrences(beforeText,prefix),beforeUserMessages:countNode,beforeAssistantCount:beforeAssistant.length,beforeAssistantLatest:beforeAssistant[beforeAssistant.length-1]||'',lastReplyText:'',stablePolls:0,at:Date.now()};
            const candidates=Array.from((editor.closest('form') || document).querySelectorAll('button,[role="button"]')).filter(visible);
            const send=candidates.find(b=>/\bsend\b/i.test((b.getAttribute('aria-label')||'')+' '+(b.getAttribute('title')||'')+' '+(b.getAttribute('data-testid')||''))) || candidates.find(b=>/^send$/i.test(normalize(b.innerText)));
            if(!send){ return JSON.stringify({ok:false,clicked:false,kind:'Prompt submission',title:'Send control not found',url:location.href,content:'No recognizable send button was found in the composer. Memo did not press Enter or send anything; submit the reviewed prompt manually, then continue the workflow.',summary:'No safe send control found.',metadata:{composer:editor.tagName}}); }
            send.click();
            return JSON.stringify({ok:true,clicked:true,kind:'Prompt submission',title:'Submission requested',url:location.href,content:'The recognized send control was clicked. Memo will verify that the prompt appears as a new conversation message and is no longer in the composer.',summary:'Send control clicked; awaiting evidence.',metadata:{method:'button',label:(send.getAttribute('aria-label')||send.innerText||'Send').slice(0,100)}});
          })();
        """.trimIndent()
    }

    fun verifyPromptSubmission(prompt: String, preserveState: Boolean = false): String {
        val quoted = JSONObject.quote(prompt)
        val consumeState = (!preserveState).toString()
        return """
          (function(){
            const wanted=$quoted;
            const consumeState=$consumeState;
            const normalize=s=>(s||'').replace(/\u00a0/g,' ').replace(/\s+/g,' ').trim();
            const state=window.__synthosSubmitState;
            if(!state || state.prompt!==wanted) return JSON.stringify({verified:false,contextPresent:false,composerCleared:false,reason:'Submission context was lost because the page changed. Check the conversation manually.'});
            const clone=document.body ? document.body.cloneNode(true) : null;
            if(!clone) return JSON.stringify({verified:false,contextPresent:true,composerCleared:false,reason:'The page did not expose readable conversation content. Check the result manually.'});
            clone.querySelectorAll('textarea,input,[contenteditable="true"],[role="textbox"]').forEach(el=>el.remove());
            const text=normalize(clone.innerText || clone.textContent);
            const prefix=normalize(wanted).slice(0,Math.min(160,normalize(wanted).length));
            const occurrences=(value,needle)=>{ if(!needle) return 0; let count=0,from=0,at; while((at=value.toLocaleLowerCase().indexOf(needle.toLocaleLowerCase(),from))>=0){count++;from=at+needle.length;} return count; };
            const afterOccurrences=occurrences(text,prefix);
            const userMessageCount=document.querySelectorAll('[data-message-author-role="user"]').length;
            const editors=Array.from(document.querySelectorAll('textarea,input,[contenteditable="true"],[role="textbox"]'));
            const editor=editors.reverse().find(el=>{const r=el.getBoundingClientRect();const st=getComputedStyle(el);return r.width>0&&r.height>0&&st.visibility!=='hidden'&&st.display!=='none';});
            const current=editor ? normalize(editor.isContentEditable || editor.getAttribute('contenteditable')==='true' ? (editor.innerText || editor.textContent) : editor.value) : '';
            const composerCleared=!current.toLocaleLowerCase().includes(prefix.toLocaleLowerCase());
            const newUserMessage=userMessageCount>state.beforeUserMessages;
            const newVisiblePrompt=afterOccurrences>state.beforeOccurrences;
            const verified=composerCleared && (newUserMessage || newVisiblePrompt);
            if(consumeState) delete window.__synthosSubmitState;
            return JSON.stringify({verified:verified,contextPresent:true,reason:verified?'New prompt observed in the page.':'The page did not show a new prompt with the composer cleared. It may still be sending, or the site structure may be unsupported.',composerCleared:composerCleared,newUserMessage:newUserMessage,newVisiblePrompt:newVisiblePrompt,beforeOccurrences:state.beforeOccurrences,afterOccurrences:afterOccurrences,beforeUserMessages:state.beforeUserMessages,afterUserMessages:userMessageCount});
          })();
        """.trimIndent()
    }

    /** Read the newest assistant response. The submit baseline prevents old answers being mistaken for this run. */
    val pollAssistantResponse: String = """
        (function(){
          const normalize=s=>(s||'').replace(/\u00a0/g,' ').replace(/\s+/g,' ').trim();
          const visible=el=>{const r=el.getBoundingClientRect();const st=getComputedStyle(el);return r.width>0&&r.height>0&&st.visibility!=='hidden'&&st.display!=='none';};
          const state=window.__synthosSubmitState;
          if(!state) return JSON.stringify({status:'NEEDS_REVIEW',reason:'The browser lost the submission baseline. Inspect the conversation manually before retrying.'});
          const host=(location.hostname||'').toLowerCase().replace(/^www\./,'');
          let selectors=['[data-message-author-role="assistant"]','[data-testid*="assistant-message"]','[data-testid*="model-response"]','[data-testid*="answer-content"]','[class*="assistant-message"]','[class*="model-response"]','[class*="answer-content"]','.model-response-text','.ds-markdown','.font-claude-message','main [class*="prose"]'];
          if(host==='gemini.google.com') selectors=['model-response','message-content','.model-response-text','[data-message-type="model-response"]',...selectors];
          if(host==='perplexity.ai') selectors=['[data-testid*="answer"]','main .prose','main [class*="prose"]',...selectors];
          if(host==='deepseek.com'||host==='chat.deepseek.com') selectors=['.ds-markdown','[class*="markdown"]',...selectors];
          let messages=[];
          for(const selector of selectors){
            let nodes=[];try{nodes=Array.from(document.querySelectorAll(selector)).filter(visible);}catch(_){}
            const values=[];
            for(const node of nodes){
              const text=normalize(node.innerText||node.textContent);
              if(text.length<24||values.includes(text)) continue;
              if(values.some(value=>value.length>text.length&&value.includes(text))) continue;
              values.push(text);
            }
            if(values.length){messages=values.slice(-30);break;}
          }
          messages=messages.filter((value,index,array)=>array.indexOf(value)===index);
          if(!messages.length) return JSON.stringify({status:'PENDING',reason:'No readable assistant response detected yet.',assistantCount:0});
          const latest=messages[messages.length-1];
          const changed=messages.length>Number(state.beforeAssistantCount||0)||latest!==String(state.beforeAssistantLatest||'');
          const stop=Array.from(document.querySelectorAll('button,[role="button"]')).some(el=>{
            if(!visible(el)) return false;
            const label=normalize((el.getAttribute('aria-label')||'')+' '+(el.getAttribute('title')||'')+' '+(el.getAttribute('data-testid')||''));
            return /\b(stop generating|stop response|cancel response|stop answer)\b/i.test(label);
          });
          const busy=!!document.querySelector('[aria-busy="true"],[data-is-streaming="true"]')||stop;
          if(changed&&latest===String(state.lastReplyText||'')) state.stablePolls=Number(state.stablePolls||0)+1;
          else { state.lastReplyText=latest; state.stablePolls=0; }
          const stable=Number(state.stablePolls||0)>=3;
          const done=changed&&latest.length>=24&&stable&&!busy;
          return JSON.stringify({status:done?'COMPLETED':'PENDING',response:changed?latest.slice(0,14000):'',assistantCount:messages.length,changed:changed,stablePolls:Number(state.stablePolls||0),busy:busy,reason:done?'A new assistant response was detected and stopped changing.':'Waiting for a new, stable assistant response.'});
        })();
    """.trimIndent()

    val startScrollCapture: String = """
        (function(){
          window.__synthosCaptureDone = false; window.__synthosCaptureCancelled = false; window.__synthosCaptureResult = null;
          (async function(){
            const normalize = s => (s || '').replace(/\u00a0/g,' ').replace(/\s+/g,' ').trim();
            const collected = new Map();
            const articleSelector = 'article,[role="article"],[data-testid*="post"],[data-testid*="feed-item"]';
            for(let pass=0; pass<6 && !window.__synthosCaptureCancelled; pass++){
              const cards=Array.from(document.querySelectorAll(articleSelector));
              for(const card of cards){
                const text=normalize(card.innerText || card.textContent).slice(0,2200);
                if(text.length>35){ const key=text.toLocaleLowerCase().slice(0,180); if(!collected.has(key)) collected.set(key,text); }
              }
              if(pass<5 && !window.__synthosCaptureCancelled){ window.scrollBy(0,Math.max(350,Math.floor(window.innerHeight*0.75))); await new Promise(resolve=>setTimeout(resolve,650)); }
            }
            window.scrollTo(0,0);
            if(window.__synthosCaptureCancelled){ window.__synthosCaptureResult=JSON.stringify({ok:false,kind:'Feed review',title:'Feed review cancelled',url:location.href,content:'The user cancelled this feed review.',summary:'Feed review cancelled.',metadata:{cancelled:true}}); window.__synthosCaptureDone=true; return; }
            const cards=Array.from(collected.values()).slice(0,60);
            const fallback=normalize(document.body ? document.body.innerText : '').slice(0,15000);
            const content=cards.length ? cards.map((x,i)=>(i+1)+'. '+x).join('\n\n---\n\n') : fallback;
            window.__synthosCaptureResult=JSON.stringify({ok:true,kind:'Feed review',title:'Feed review · '+(document.title || location.hostname),url:location.href,content:content,summary:cards.length+' distinct visible content card(s) collected after six scroll passes.'+(cards.length===0?' No article-like cards were detected, so visible page text was saved instead.':''),metadata:{cards:cards.length,passes:6,scrollHeight:document.documentElement ? document.documentElement.scrollHeight : 0,capturedAt:Date.now()}});
            window.__synthosCaptureDone=true;
          })().catch(error=>{ window.__synthosCaptureResult=JSON.stringify({ok:false,kind:'Feed review',title:'Feed review failed',url:location.href,content:String(error && error.message || error),summary:'Could not finish scrolling the page.',metadata:{}}); window.__synthosCaptureDone=true; });
          return 'started';
        })();
    """.trimIndent()

    val pollScrollCapture: String = "window.__synthosCaptureDone ? (window.__synthosCaptureResult || 'null') : 'PENDING';"

    val cancelScrollCapture: String = "window.__synthosCaptureCancelled = true; 'cancelled';"
}
