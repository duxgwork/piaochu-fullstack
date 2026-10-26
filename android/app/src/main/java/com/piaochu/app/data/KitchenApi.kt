package com.piaochu.app.data
import android.content.Context
import com.google.gson.GsonBuilder
import com.piaochu.app.domain.*
import okhttp3.OkHttpClient
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
/** DTO 在这里转为领域对象；认证错误会清除当前环境的会话，UI 不继续显示旧账号的数据。 */
class KitchenApi(client:OkHttpClient,baseUrl:String,private val store:SessionStore):KitchenRepository {
    private val gson=GsonBuilder().disableJdkUnsafe().create()
    private val service=Retrofit.Builder().baseUrl(NetworkSettings.normalizedUrl(baseUrl)).client(client)
        .addConverterFactory(GsonConverterFactory.create(gson)).build().create(KitchenService::class.java)
    override fun currentUser()=store.session?.toUser()
    override fun forgetSession()=store.clear()
    override suspend fun catalog()=service.catalog().bodyRequired().domain()
    override suspend fun me()=service.me().bodyRequired().domain()
    override suspend fun login(username:String,password:String)=service.login(LoginBody(username.trim(),password)).bodyRequired().let{store.save(it);it.toUser()}
    override suspend fun register(username:String,displayName:String,password:String)=service.register(RegisterBody(username.trim(),displayName.trim(),password)).bodyRequired().let{store.save(it);it.toUser()}
    override suspend fun logout(){try{service.logout().ensureSuccess()}finally{store.clear()}}
    override suspend fun addresses()=service.addresses().bodyRequired().map{it.domain()}
    override suspend fun saveAddress(id:Long?,contact:String,phone:String,detail:String,label:String):Address {
        val body=AddressBody(contact.trim(),phone.trim(),detail.trim(),label.trim())
        return (if(id==null)service.addAddress(body)else service.editAddress(id,body)).bodyRequired().domain()
    }
    override suspend fun deleteAddress(id:Long){service.deleteAddress(id).ensureSuccess()}
    override suspend fun orders()=service.orders().bodyRequired().map{it.domain()}
    override suspend fun createOrder(key:String,addressId:Long,items:Map<Long,Int>,note:String)=
        service.order(OrderBody(key,addressId,items.toSortedMap().map{ItemBody(it.key,it.value)},note.trim())).bodyRequired().domain()
    override suspend fun cancelOrder(id:Long)=service.cancel(id).bodyRequired().domain()
    override suspend fun receiveOrder(id:Long)=service.receive(id).bodyRequired().domain()
    override suspend fun bookings()=service.bookings().bodyRequired().map{it.domain()}
    override suspend fun createBooking(key:String,draft:BookingDraft)=service.book(BookingBody(key,draft.serviceType,draft.scene,draft.scheduledAt,draft.people,draft.budgetCents,draft.addressId,draft.note.trim())).bodyRequired().domain()
    override suspend fun cancelBooking(id:Long)=service.cancelBooking(id).bodyRequired().domain()
    private fun <T> Response<T>.bodyRequired():T{ensureSuccess();return needed(body())}
    private fun <T> Response<T>.ensureSuccess(){
        if(isSuccessful)return
        val error=errorBody()?.use{runCatching{gson.fromJson(it.charStream(),ErrorDto::class.java)}.getOrNull()}
        if(code()==401)store.clear()
        throw ServiceException(code(),error?.message?:"请求失败 HTTP ${code()}",headers()["X-Request-Id"]?:error?.requestId)
    }
    companion object {
        fun create(context:Context,config:NetworkConfig):KitchenApi {
            val store=SessionStore(context.applicationContext,config.baseUrl)
            return KitchenApi(HttpClientFactory.create(config){store.session?.token},config.baseUrl,store)
        }
    }
}
