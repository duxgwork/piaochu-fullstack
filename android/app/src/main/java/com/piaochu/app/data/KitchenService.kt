package com.piaochu.app.data
import retrofit2.Response
import retrofit2.http.*
/** Retrofit 根据这些注解生成实现，所有页面通过同一个接口服务访问真实 Java 后端。 */
internal interface KitchenService {
    @GET("api/catalog") suspend fun catalog():Response<CatalogDto>
    @POST("api/auth/register") suspend fun register(@Body body:RegisterBody):Response<AuthDto>
    @POST("api/auth/login") suspend fun login(@Body body:LoginBody):Response<AuthDto>
    @GET("api/auth/me") suspend fun me():Response<UserDto>
    @POST("api/auth/logout") suspend fun logout():Response<Unit>
    @GET("api/addresses") suspend fun addresses():Response<List<AddressDto>>
    @POST("api/addresses") suspend fun addAddress(@Body body:AddressBody):Response<AddressDto>
    @PUT("api/addresses/{id}") suspend fun editAddress(@Path("id") id:Long,@Body body:AddressBody):Response<AddressDto>
    @DELETE("api/addresses/{id}") suspend fun deleteAddress(@Path("id") id:Long):Response<Unit>
    @GET("api/orders") suspend fun orders():Response<List<OrderDto>>
    @POST("api/orders") suspend fun order(@Body body:OrderBody):Response<OrderDto>
    @POST("api/orders/{id}/cancel") suspend fun cancel(@Path("id") id:Long):Response<OrderDto>
    @POST("api/orders/{id}/receive") suspend fun receive(@Path("id") id:Long):Response<OrderDto>
    @GET("api/bookings") suspend fun bookings():Response<List<BookingDto>>
    @POST("api/bookings") suspend fun book(@Body body:BookingBody):Response<BookingDto>
    @POST("api/bookings/{id}/cancel") suspend fun cancelBooking(@Path("id") id:Long):Response<BookingDto>
}
