package com.piaochu.app.data
import com.google.gson.GsonBuilder
import com.piaochu.app.domain.money
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.*
import org.junit.Assert.*
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
class RetrofitContractTest {
 @Test fun retailRequestUsesJsonCentsContractAndStableSubmissionKey()=runBlocking {
  MockWebServer().use{server->
   server.enqueue(MockResponse().setResponseCode(409).setBody("{\"message\":\"重复提交\"}"))
   val service=Retrofit.Builder().baseUrl(server.url("/")).addConverterFactory(GsonConverterFactory.create()).build().create(KitchenService::class.java)
   val response=service.order(OrderBody("submission-1",3,listOf(ItemBody(5,2)),"少辣"))
   assertEquals(409,response.code());val request=server.takeRequest();assertEquals("POST",request.method);assertEquals("/api/orders",request.path)
   val body=GsonBuilder().create().fromJson(request.body.readUtf8(),Map::class.java)
   assertEquals("submission-1",body["clientRequestId"]);assertFalse(body.containsKey("userId"));assertFalse(body.containsKey("totalCents"))
  }
 }
 @Test fun missingPriceIsRejectedInsteadOfShowingFreeFood(){
  val dto=GsonBuilder().disableJdkUnsafe().create().fromJson("{\"id\":1,\"name\":\"菜品\"}",DishDto::class.java)
  assertThrows(IllegalStateException::class.java){dto.domain()}
 }
 @Test fun endpointRejectsCredentialsAndApiPath(){
  for(url in listOf("not-a-url","http://host/api/orders","http://user:password@host/"))assertThrows(IllegalArgumentException::class.java){NetworkSettings.validate(NetworkConfig(url))}
  NetworkSettings.validate(NetworkConfig("http://10.0.2.2:8090/"))
 }
 @Test fun centsNeverUseBinaryFloatingPoint(){assertEquals("32.01",money(3201));assertEquals("0",money(0));assertEquals("100",money(10000))}
}
