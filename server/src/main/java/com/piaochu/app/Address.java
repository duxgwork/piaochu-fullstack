package com.piaochu.app;
import jakarta.persistence.*;
import java.time.Instant;
/** 实体只用于持久化，HTTP 返回值由 Contracts 显式映射，密码与令牌摘要不会序列化到客户端。 */
@Entity @Table(name="addresses")
class Address extends Row {
    @Column(name="user_id",nullable=false) Long userId;
    @Column(name="contact_name",nullable=false,length=40) String contactName;
    @Column(nullable=false,length=20) String phone;
    @Column(nullable=false,length=200) String detail;
    @Column(nullable=false,length=20) String label;
}
