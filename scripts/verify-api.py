#!/usr/bin/env python3
"""通过真实HTTP验证用户端业务，创建独立随机账号，不打印密码或令牌，不清空其他用户数据。"""
import datetime,json,secrets,sys,urllib.error,urllib.request,uuid
base=(sys.argv[1] if len(sys.argv)>1 else 'http://127.0.0.1:8090').rstrip('/')
token=None

def call(method,path,body=None,expected=200):
    headers={'Content-Type':'application/json','X-Request-Id':'verify-'+uuid.uuid4().hex}
    if token:headers['Authorization']='Bearer '+token
    req=urllib.request.Request(base+path,data=None if body is None else json.dumps(body,ensure_ascii=False).encode(),headers=headers,method=method)
    try:
        with urllib.request.urlopen(req,timeout=20) as response:
            assert response.status==expected,(path,response.status);data=response.read();return json.loads(data) if data else None
    except urllib.error.HTTPError as error:
        if error.code!=expected:raise AssertionError((path,error.code,error.read().decode())) from error
        return None
try:
    assert call('GET','/api/health')['status']=='UP'
    catalog=call('GET','/api/catalog');assert catalog['dishes'],'示例菜单为空；请确认DEMO_DATA设置'
    call('GET','/api/orders',expected=401)
    auth=call('POST','/api/auth/register',{'username':'verify_'+uuid.uuid4().hex[:16],'displayName':'独立接口验证','password':secrets.token_urlsafe(20)},201);token=auth['token']
    address=call('POST','/api/addresses',{'contactName':'接口验证','phone':'13800000000','detail':'成都 接口验证地址','label':'测试'},201)
    order_body={'clientRequestId':str(uuid.uuid4()),'addressId':address['id'],'items':[{'dishId':catalog['dishes'][0]['id'],'quantity':1}],'note':'仅接口验证'}
    order=call('POST','/api/orders',order_body,201);assert call('POST','/api/orders',order_body,201)['id']==order['id']
    assert call('POST',f"/api/orders/{order['id']}/cancel",{})['status']=='CANCELLED'
    for kind,scene in [('GROUP','企业工餐'),('CHEF','家宴代工')]:
        b=call('POST','/api/bookings',{'clientRequestId':str(uuid.uuid4()),'serviceType':kind,'scene':scene,'scheduledAt':(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=2)).isoformat(),'people':10,'budgetCents':8000,'addressId':address['id'],'note':'仅接口验证'},201)
        assert b['quotedTotalCents'] is None
        assert call('POST',f"/api/bookings/{b['id']}/cancel",{})['status']=='CANCELLED'
    call('DELETE',f"/api/addresses/{address['id']}",expected=204)
    call('POST','/api/auth/logout',{},204);call('GET','/api/orders',expected=401);token=None
    print('PASS: 账号鉴权、地址、零售幂等下单、取消、团餐/私厨预约与退出失效')
    print('验证账号与已取消业务记录保留在当前测试数据库。')
finally:
    if token:
        try:call('POST','/api/auth/logout',{},204)
        except Exception:pass
