package com.piaochu.app.domain
import java.io.IOException
/** 领域层不引用 Android、Retrofit 或数据库类型。金额始终使用人民币分。 */
data class User(val id:Long,val username:String,val displayName:String,val role:String)
data class Address(val id:Long,val contactName:String,val phone:String,val detail:String,val label:String)
data class Dish(val id:Long,val category:String,val name:String,val description:String,val priceCents:Long,val artKey:String,val isDemo:Boolean)
data class Catalog(val kitchenName:String,val demoMode:Boolean,val dishes:List<Dish>,val deliveryCents:Long,val freeDeliveryThresholdCents:Long)
data class OrderLine(val dishId:Long,val name:String,val unitCents:Long,val quantity:Int)
data class FoodOrder(val id:Long,val status:String,val subtotalCents:Long,val deliveryCents:Long,val totalCents:Long,
    val contactName:String,val phone:String,val address:String,val note:String,val isDemo:Boolean,val createdAt:String,val items:List<OrderLine>)
data class Booking(val id:Long,val serviceType:String,val scene:String,val scheduledAt:String,val people:Int,val budgetCents:Long,
    val quotedTotalCents:Long?,val status:String,val contactName:String,val phone:String,val address:String,val note:String,val createdAt:String)
data class BookingDraft(val serviceType:String,val scene:String,val scheduledAt:String,val people:Int,val budgetCents:Long,val addressId:Long,val note:String)
class ServiceException(val status:Int,message:String,val requestId:String?):IOException(message)
interface KitchenRepository {
    fun currentUser():User?
    fun forgetSession()
    suspend fun catalog():Catalog
    suspend fun me():User
    suspend fun login(username:String,password:String):User
    suspend fun register(username:String,displayName:String,password:String):User
    suspend fun logout()
    suspend fun addresses():List<Address>
    suspend fun saveAddress(id:Long?,contact:String,phone:String,detail:String,label:String):Address
    suspend fun deleteAddress(id:Long)
    suspend fun orders():List<FoodOrder>
    suspend fun createOrder(key:String,addressId:Long,items:Map<Long,Int>,note:String):FoodOrder
    suspend fun cancelOrder(id:Long):FoodOrder
    suspend fun receiveOrder(id:Long):FoodOrder
    suspend fun bookings():List<Booking>
    suspend fun createBooking(key:String,draft:BookingDraft):Booking
    suspend fun cancelBooking(id:Long):Booking
}
fun money(cents:Long):String=java.math.BigDecimal.valueOf(cents,2).stripTrailingZeros().toPlainString()
fun statusLabel(status:String):String=when(status){
    "PENDING_CONFIRMATION"->"待商家确认";"CONFIRMED"->"已确认";"PREPARING"->"制作中";"DELIVERING"->"配送中"
    "COMPLETED"->"已完成";"CANCELLED"->"已取消";"REQUESTED"->"待联系报价";else->"状态待刷新"
}
