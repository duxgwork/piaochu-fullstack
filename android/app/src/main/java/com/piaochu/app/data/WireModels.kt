package com.piaochu.app.data
import com.piaochu.app.domain.*
/** 可空 DTO 防止 Gson 缺字段时静默生成一个价格为0的领域对象。 */
internal fun <T> needed(value:T?):T=checkNotNull(value){"服务端响应缺少必要字段"}
data class UserDto(val id:Long?=null,val username:String?=null,val displayName:String?=null,val role:String?=null){fun domain()=User(needed(id),needed(username),needed(displayName),needed(role))}
data class AuthDto(val token:String?=null,val expiresAt:String?=null,val user:UserDto?=null){fun toUser()=needed(user).domain()}
data class AddressDto(val id:Long?=null,val contactName:String?=null,val phone:String?=null,val detail:String?=null,val label:String?=null){fun domain()=Address(needed(id),needed(contactName),needed(phone),needed(detail),needed(label))}
data class DishDto(val id:Long?=null,val category:String?=null,val name:String?=null,val description:String?=null,val priceCents:Long?=null,val artKey:String?=null,val isDemo:Boolean?=null){fun domain()=Dish(needed(id),needed(category),needed(name),needed(description),needed(priceCents),needed(artKey),needed(isDemo))}
data class CatalogDto(val kitchenName:String?=null,val demoMode:Boolean?=null,val dishes:List<DishDto>?=null,val deliveryCents:Long?=null,val freeDeliveryThresholdCents:Long?=null){fun domain()=Catalog(needed(kitchenName),needed(demoMode),needed(dishes).map{it.domain()},needed(deliveryCents),needed(freeDeliveryThresholdCents))}
data class LineDto(val dishId:Long?=null,val name:String?=null,val unitCents:Long?=null,val quantity:Int?=null){fun domain()=OrderLine(needed(dishId),needed(name),needed(unitCents),needed(quantity))}
data class OrderDto(val id:Long?=null,val status:String?=null,val subtotalCents:Long?=null,val deliveryCents:Long?=null,val totalCents:Long?=null,val contactName:String?=null,val phone:String?=null,val address:String?=null,val note:String?=null,val isDemo:Boolean?=null,val createdAt:String?=null,val items:List<LineDto>?=null){fun domain()=FoodOrder(needed(id),needed(status),needed(subtotalCents),needed(deliveryCents),needed(totalCents),needed(contactName),needed(phone),needed(address),needed(note),needed(isDemo),needed(createdAt),needed(items).map{it.domain()})}
data class BookingDto(val id:Long?=null,val serviceType:String?=null,val scene:String?=null,val scheduledAt:String?=null,val people:Int?=null,val budgetCents:Long?=null,val quotedTotalCents:Long?=null,val status:String?=null,val contactName:String?=null,val phone:String?=null,val address:String?=null,val note:String?=null,val createdAt:String?=null){fun domain()=Booking(needed(id),needed(serviceType),needed(scene),needed(scheduledAt),needed(people),needed(budgetCents),quotedTotalCents,needed(status),needed(contactName),needed(phone),needed(address),needed(note),needed(createdAt))}
data class ErrorDto(val message:String?=null,val requestId:String?=null)
data class RegisterBody(val username:String,val displayName:String,val password:String)
data class LoginBody(val username:String,val password:String)
data class AddressBody(val contactName:String,val phone:String,val detail:String,val label:String)
data class ItemBody(val dishId:Long,val quantity:Int)
data class OrderBody(val clientRequestId:String,val addressId:Long,val items:List<ItemBody>,val note:String)
data class BookingBody(val clientRequestId:String,val serviceType:String,val scene:String,val scheduledAt:String,val people:Int,val budgetCents:Long,val addressId:Long,val note:String)
