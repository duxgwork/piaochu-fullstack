package com.piaochu.app;
import jakarta.persistence.*;
import java.time.Instant;
/** 实体只用于持久化，HTTP 返回值由 Contracts 显式映射，密码与令牌摘要不会序列化到客户端。 */
@Entity @Table(name="menu_items")
class MenuItem extends Row {
    @Column(nullable=false,length=20) String category;
    @Column(nullable=false,length=80) String name;
    @Column(nullable=false,length=300) String description;
    @Column(name="price_cents",nullable=false) long priceCents;
    @Column(name="art_key",nullable=false,length=20) String artKey;
    @Column(name="is_demo",nullable=false) boolean demo;
    @Column(nullable=false) boolean available;
}
