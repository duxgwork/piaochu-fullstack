package com.piaochu.app;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.*;
/** 金额统一为人民币分。订单价格来自数据库，不接受客户端上传的总价。 */
public final class Contracts {
    private Contracts() {}
    public record Register(@Pattern(regexp="[a-zA-Z0-9_]{4,32}",message="账号需为4至32位字母、数字或下划线") @NotNull String username,
        @NotBlank @Size(max=40) String displayName,@NotNull @Size(min=8,max=128) String password) {}
    public record Login(@NotBlank @Size(max=32) String username,@NotNull @Size(min=8,max=128) String password) {}
    public record User(long id,String username,String displayName,String role) {
        static User from(Account a) { return new User(a.id,a.username,a.displayName,a.role); }
    }
    public record Auth(String token,String expiresAt,User user) {}
    public record Actor(long id,String sessionHash,String role) {}
    public record AddressInput(@NotBlank @Size(max=40) String contactName,
        @NotNull @Pattern(regexp="1[3-9][0-9]{9}",message="请填写11位大陆手机号") String phone,
        @NotBlank @Size(max=200) String detail,@NotNull @Size(max=20) String label) {}
    public record AddressView(long id,String contactName,String phone,String detail,String label) {
        static AddressView from(Address a) { return new AddressView(a.id,a.contactName,a.phone,a.detail,a.label); }
    }
    public record Dish(long id,String category,String name,String description,long priceCents,String artKey,boolean isDemo) {
        static Dish from(MenuItem a) { return new Dish(a.id,a.category,a.name,a.description,a.priceCents,a.artKey,a.demo); }
    }
    public record Catalog(String kitchenName,boolean demoMode,List<Dish> dishes,long deliveryCents,long freeDeliveryThresholdCents) {}
    public record ItemInput(@NotNull @Positive Long dishId,@Min(1) @Max(20) int quantity) {}
    public record OrderInput(@NotNull UUID clientRequestId,@NotNull @Positive Long addressId,
        @NotEmpty @Size(max=20) List<@Valid ItemInput> items,@NotNull @Size(max=500) String note) {}
    public record LineView(long dishId,String name,long unitCents,int quantity) {}
    public record OrderView(long id,String status,long subtotalCents,long deliveryCents,long totalCents,
        String contactName,String phone,String address,String note,boolean isDemo,String createdAt,List<LineView> items) {}
    public record BookingInput(@NotNull UUID clientRequestId,@Pattern(regexp="GROUP|CHEF") @NotNull String serviceType,
        @NotBlank @Size(max=30) String scene,@NotNull OffsetDateTime scheduledAt,@Min(1) @Max(50000) int people,
        @Min(1000) @Max(1000000) long budgetCents,@NotNull @Positive Long addressId,@NotNull @Size(max=500) String note) {}
    public record BookingView(long id,String serviceType,String scene,String scheduledAt,int people,long budgetCents,
        Long quotedTotalCents,String status,String contactName,String phone,String address,String note,String createdAt) {
        static BookingView from(Booking b) { return new BookingView(b.id,b.serviceType,b.scene,b.scheduledAt.toString(),b.people,
            b.budgetCents,b.quotedTotalCents,b.status,b.contactName,b.phone,b.addressSnapshot,b.note,b.createdAt.toString()); }
    }
    public record StatusInput(@NotBlank String status,Long quotedTotalCents) {}
}
