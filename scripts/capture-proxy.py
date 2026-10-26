#!/usr/bin/env python3
"""仅转发本机8090的HTTP调试代理。记录对账号密码/令牌/联系方式脱敏，不支持HTTPS CONNECT。"""
import argparse,http.client,http.server,json,os,pathlib,threading,urllib.parse
p=argparse.ArgumentParser(description=__doc__);p.add_argument('--listen',default='127.0.0.1');p.add_argument('--port',type=int,default=18890);a=p.parse_args()
root=pathlib.Path(__file__).resolve().parents[1]/'captures';root.mkdir(mode=0o700,exist_ok=True)
file=root/'http.jsonl';lock=threading.Lock()
SECRET={'authorization','password','token','phone','contactname','detail','address','addresssnapshot'}
def clean(value):
    if isinstance(value,dict):return {k:('[REDACTED]' if k.lower() in SECRET else clean(v)) for k,v in value.items()}
    if isinstance(value,list):return [clean(x) for x in value]
    return value
def body_json(raw):
    try:return clean(json.loads(raw)) if raw else None
    except Exception:return '[non-json]'
class Proxy(http.server.BaseHTTPRequestHandler):
    protocol_version='HTTP/1.1'
    def do_CONNECT(self):self.send_error(501,'HTTPS requires Charles/Proxyman')
    def forward(self):
        url=urllib.parse.urlsplit(self.path)
        if url.scheme!='http' or url.hostname not in {'127.0.0.1','localhost'} or url.port!=8090:
            self.send_error(403,'Only http://127.0.0.1:8090 is allowed');return
        length=int(self.headers.get('Content-Length','0'))
        if length>131072:self.send_error(413);return
        request_body=self.rfile.read(length)
        skipped={'proxy-connection','connection','host','transfer-encoding','content-length'}
        headers={k:v for k,v in self.headers.items() if k.lower() not in skipped}
        headers['Host']='127.0.0.1:8090'
        path=urllib.parse.urlunsplit(('','',url.path or '/',url.query,''))
        conn=http.client.HTTPConnection('127.0.0.1',8090,timeout=20)
        try:
            conn.request(self.command,path,body=request_body,headers=headers);response=conn.getresponse();data=response.read()
            self.send_response(response.status)
            for k,v in response.getheaders():
                if k.lower() not in {'transfer-encoding','connection','content-length'}:self.send_header(k,v)
            self.send_header('Content-Length',str(len(data)));self.end_headers();self.wfile.write(data)
            event={'method':self.command,'path':path,'status':response.status,'requestId':response.getheader('X-Request-Id'),'request':body_json(request_body),'response':body_json(data)}
            with lock:
                fd=os.open(file,os.O_WRONLY|os.O_APPEND|os.O_CREAT,0o600)
                with os.fdopen(fd,'a') as stream:stream.write(json.dumps(event,ensure_ascii=False)+'\n')
            print(self.command,path,'->',response.status,flush=True)
        except (OSError,http.client.HTTPException):self.send_error(502,'Backend unavailable')
        finally:conn.close()
    do_GET=do_POST=do_PUT=do_PATCH=do_DELETE=forward
    def log_message(self,*args):pass
print(f'Piaochu HTTP proxy: {a.listen}:{a.port}; backend 127.0.0.1:8090',flush=True)
http.server.ThreadingHTTPServer((a.listen,a.port),Proxy).serve_forever()
