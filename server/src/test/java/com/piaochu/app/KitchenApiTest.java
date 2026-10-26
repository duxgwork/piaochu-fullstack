package com.piaochu.app;
import com.fasterxml.jackson.databind.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.*;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.junit.jupiter.api.Assertions.*;
/** 通过真实 HTTP 映射、鉴权过滤器、事务与数据库验证，而非直接调用业务方法。 */
@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:kitchen;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1", "app.operator.username=test_operator", "app.operator.password=operator-pass-123"})
@AutoConfigureMockMvc
class KitchenApiTest {
 @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired AccountRepository accounts; @Autowired SessionRepository sessions;
 JsonNode call(String method,String path,String token,Object body,int expected)throws Exception {
  var request=request(org.springframework.http.HttpMethod.valueOf(method),path).contentType(MediaType.APPLICATION_JSON);
  if(token!=null) request.header("Authorization","Bearer "+token);
  if(body!=null) request.content(json.writeValueAsBytes(body));
  var response=mvc.perform(request).andReturn().getResponse();
  assertEquals(expected,response.getStatus(),response.getContentAsString());
  assertNotNull(response.getHeader("X-Request-Id"));
  return response.getContentAsByteArray().length==0?json.nullNode():json.readTree(response.getContentAsByteArray());
 }
 String register()throws Exception {
  return call("POST","/api/auth/register",null,Map.of("username","u"+UUID.randomUUID().toString().replace("-","").substring(0,18),"displayName","接口测试用户","password","test-pass-123"),201).get("token").asText();
 }
 JsonNode address(String token)throws Exception {return call("POST","/api/addresses",token,Map.of("contactName","测试联系人","phone","13800000000","detail","成都 测试地址","label","公司"),201);}
 Map<String,Object> order(long addressId,long dishId,String key) { return Map.of("clientRequestId",key,"addressId",addressId,"items",List.of(Map.of("dishId",dishId,"quantity",2)),"note","少辣"); }
 Map<String,Object> booking(long addressId,String type,String scene,String key) {return Map.of("clientRequestId",key,"addressId",addressId,"serviceType",type,"scene",scene,"scheduledAt",OffsetDateTime.now(ZoneOffset.ofHours(8)).plusDays(2).withNano(0).toString(),"people",12,"budgetCents",8000,"note","无花生");}
 @Test void authenticationIsRevocableAndSecretsAreHashed()throws Exception {
  String token=register();call("GET","/api/auth/me",token,null,200);
  assertTrue(sessions.findByTokenHash(Passwords.digest(token)).isPresent());
  assertTrue(accounts.findAll().stream().noneMatch(a->a.passwordHash.equals("test-pass-123")));
  call("POST","/api/auth/logout",token,Map.of(),204);call("GET","/api/orders",token,null,401);
  call("POST","/api/auth/login",null,Map.of("username","missing_user","password","wrong-pass-123"),401);
 }
 @Test void ownedAddressesAndSnapshotsCannotBeReadOrChangedByAnotherUser()throws Exception {
  String a=register(),b=register();long id=address(a).get("id").asLong();
  assertEquals(0,call("GET","/api/addresses",b,null,200).size());
  var input=Map.of("contactName","别人","phone","13900000000","detail","篡改地址","label","");
  call("PUT","/api/addresses/"+id,b,input,404);call("DELETE","/api/addresses/"+id,b,null,404);
  call("PUT","/api/addresses/"+id,a,input,200);call("DELETE","/api/addresses/"+id,a,null,204);
 }
 @Test void orderFreezesServerPriceAndIsIdempotentAndOwnerScoped()throws Exception {
  String a=register(),b=register();long addressId=address(a).get("id").asLong();
  JsonNode dish=call("GET","/api/catalog",null,null,200).get("dishes").get(0);
  var input=order(addressId,dish.get("id").asLong(),UUID.randomUUID().toString());
  JsonNode o=call("POST","/api/orders",a,input,201);long id=o.get("id").asLong();
  assertEquals(dish.get("priceCents").asLong()*2,o.get("subtotalCents").asLong());
  assertEquals(id,call("POST","/api/orders",a,input,201).get("id").asLong());
  var changed=new HashMap<>(input);changed.put("note","另一份订单");call("POST","/api/orders",a,changed,409);
  call("GET","/api/orders/"+id,b,null,404);call("POST","/api/orders/"+id+"/cancel",b,Map.of(),404);
  call("POST","/api/orders/"+id+"/receive",a,Map.of(),409);
  call("DELETE","/api/addresses/"+addressId,a,null,204);
  assertEquals("成都 测试地址",call("GET","/api/orders/"+id,a,null,200).get("address").asText());
  assertEquals("CANCELLED",call("POST","/api/orders/"+id+"/cancel",a,Map.of(),200).get("status").asText());
 }
 @Test void operatorOwnsFulfillmentAndCustomerConfirmsReceipt()throws Exception {
  String a=register();long addressId=address(a).get("id").asLong();long dish=call("GET","/api/catalog",null,null,200).get("dishes").get(0).get("id").asLong();
  long id=call("POST","/api/orders",a,order(addressId,dish,UUID.randomUUID().toString()),201).get("id").asLong();
  call("GET","/api/operator/orders",a,null,403);
  String op=call("POST","/api/auth/login",null,Map.of("username","test_operator","password","operator-pass-123"),200).get("token").asText();
  call("PATCH","/api/operator/orders/"+id+"/status",op,Map.of("status","DELIVERING"),409);
  for(String state:List.of("CONFIRMED","PREPARING","DELIVERING")) call("PATCH","/api/operator/orders/"+id+"/status",op,Map.of("status",state),200);
  assertEquals("COMPLETED",call("POST","/api/orders/"+id+"/receive",a,Map.of(),200).get("status").asText());
 }
 @Test void bothBookingKindsRequireOwnedAddressAndActualQuote()throws Exception {
  String a=register(),b=register();long aid=address(a).get("id").asLong();
  for(String type:List.of("GROUP","CHEF")) {
   var input=booking(aid,type,type.equals("GROUP")?"企业工餐":"家宴代工",UUID.randomUUID().toString());
   call("POST","/api/bookings",b,input,404);
   var saved=call("POST","/api/bookings",a,input,201);long id=saved.get("id").asLong();
   assertTrue(saved.get("quotedTotalCents").isNull());assertEquals("REQUESTED",saved.get("status").asText());
   assertEquals(id,call("POST","/api/bookings",a,input,201).get("id").asLong());
   call("GET","/api/bookings/"+id,b,null,404);
   assertEquals("CANCELLED",call("POST","/api/bookings/"+id+"/cancel",a,Map.of(),200).get("status").asText());
  }
 }
 @Test void invalidDatesQuantitiesAndMissingAuthenticationAreRejected()throws Exception {
  call("GET","/api/orders",null,null,401);String a=register();long aid=address(a).get("id").asLong();
  var input=new HashMap<>(booking(aid,"CHEF","家宴代工",UUID.randomUUID().toString()));input.put("scheduledAt",OffsetDateTime.now().minusDays(1).toString());
  call("POST","/api/bookings",a,input,400);
  input.put("scheduledAt",OffsetDateTime.now().plusDays(2).toString());input.put("people",0);call("POST","/api/bookings",a,input,400);
  call("POST","/api/orders",a,Map.of("clientRequestId",UUID.randomUUID(),"addressId",aid,"items",List.of(Map.of("dishId",1,"quantity",-1)),"note",""),400);
  call("POST","/api/auth/register",null,Map.of("username","abcd","displayName","用户","password","short"),400);
 }
 @Test void bookingConfirmationRequiresQuoteAndValidTransition()throws Exception {
  String a=register();long aid=address(a).get("id").asLong();long id=call("POST","/api/bookings",a,booking(aid,"CHEF","现场烹饪",UUID.randomUUID().toString()),201).get("id").asLong();
  String op=call("POST","/api/auth/login",null,Map.of("username","test_operator","password","operator-pass-123"),200).get("token").asText();
  call("PATCH","/api/operator/bookings/"+id+"/status",op,Map.of("status","COMPLETED"),409);
  call("PATCH","/api/operator/bookings/"+id+"/status",op,Map.of("status","CONFIRMED"),400);
  var confirmed=call("PATCH","/api/operator/bookings/"+id+"/status",op,Map.of("status","CONFIRMED","quotedTotalCents",96000),200);
  assertEquals(96000,confirmed.get("quotedTotalCents").asLong());
  call("POST","/api/bookings/"+id+"/cancel",a,Map.of(),409);
  call("PATCH","/api/operator/bookings/"+id+"/status",op,Map.of("status","COMPLETED"),200);
 }
 @Test void authenticationEndpointIsRateLimited()throws Exception {
  for(int i=0;i<31;i++) {
   var request=post("/api/auth/login").with(r->{r.setRemoteAddr("198.51.100.9");return r;}).contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"rate_missing\",\"password\":\"test-pass-123\"}");
   assertEquals(i<30?401:429,mvc.perform(request).andReturn().getResponse().getStatus());
  }
 }

 @Test void encodedOperatorPathStillChecksApplicationRole()throws Exception {
  String token=register();
  var request=get(java.net.URI.create("/api/%6fperator/orders")).header("Authorization","Bearer "+token);
  assertEquals(403,mvc.perform(request).andReturn().getResponse().getStatus());
 }

}
