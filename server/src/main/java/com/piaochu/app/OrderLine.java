package com.piaochu.app;
import jakarta.persistence.*;
import java.time.Instant;
/** 实体只用于持久化，HTTP 返回值由 Contracts 显式映射，密码与令牌摘要不会序列化到客户端。 */
@Entity @Table(name="order_lines")
class OrderLine extends Row {
    @Column(name="order_id",nullable=false) Long orderId;
    @Column(name="menu_item_id",nullable=false) Long menuItemId;
    @Column(nullable=false,length=80) String name;
    @Column(name="unit_cents",nullable=false) long unitCents;
    @Column(nullable=false) int quantity;
}
