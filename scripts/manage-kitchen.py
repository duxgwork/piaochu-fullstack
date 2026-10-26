#!/usr/bin/env python3
"""受控运营入口：读取订单/预约，并按状态机接单、制作、配送、报价。密码通过交互输入。"""
import argparse,getpass,json,urllib.request,urllib.error
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--base',default='http://127.0.0.1:8090');p.add_argument('--username',required=True)
p.add_argument('resource',choices=['orders','bookings']);p.add_argument('--id',type=int);p.add_argument('--status');p.add_argument('--quote-cents',type=int)
a=p.parse_args();token=None

def call(method,path,body=None):
    headers={'Content-Type':'application/json'}
    if token:headers['Authorization']='Bearer '+token
    req=urllib.request.Request(a.base.rstrip('/')+path,data=None if body is None else json.dumps(body).encode(),headers=headers,method=method)
    with urllib.request.urlopen(req,timeout=20) as response:
        content=response.read();return json.loads(content) if content else None
try:
    token=call('POST','/api/auth/login',{'username':a.username,'password':getpass.getpass('运营密码: ')})['token']
    if a.id is not None:
        if not a.status:p.error('修改状态需提供--status')
        result=call('PATCH',f'/api/operator/{a.resource}/{a.id}/status',{'status':a.status,'quotedTotalCents':a.quote_cents})
    else:result=call('GET','/api/operator/'+a.resource)
    print(json.dumps(result,ensure_ascii=False,indent=2))
except urllib.error.HTTPError as error:
    print('HTTP',error.code,error.read().decode());raise SystemExit(1)
finally:
    if token:
        try:call('POST','/api/auth/logout',{})
        except Exception:pass
