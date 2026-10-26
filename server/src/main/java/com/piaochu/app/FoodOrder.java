package com.piaochu.app;
import jakarta.persistence.*;
import java.time.Instant;
/** 实体只用于持久化，HTTP 返回值由 Contracts 显式映射，密码与令牌摘要不会序列化到客户端。 */
@Entity @Table(name="food_orders",uniqueConstraints=@UniqueConstraint(columnNames={"user_id","request_key"}))
class FoodOrder extends Row {
    @Column(name="user_id",nullable=false) Long userId;
    @Column(name="request_key",nullable=false,length=36) String requestKey;
    @Column(name="request_digest",nullable=false,length=64) String requestDigest;
    @Column(nullable=false,length=32) String status;
    @Column(name="subtotal_cents",nullable=false) long subtotalCents;
    @Column(name="delivery_cents",nullable=false) long deliveryCents;
    @Column(name="total_cents",nullable=false) long totalCents;
    @Column(name="contact_name",nullable=false,length=40) String contactName;
    @Column(nullable=false,length=20) String phone;
    @Column(name="address_snapshot",nullable=false,length=200) String addressSnapshot;
    @Column(nullable=false,length=500) String note;
    @Column(name="is_demo",nullable=false) boolean demo;
}
