package com.piaochu.app;
import jakarta.persistence.*;
import java.time.Instant;
/** 实体只用于持久化，HTTP 返回值由 Contracts 显式映射，密码与令牌摘要不会序列化到客户端。 */
@Entity @Table(name="accounts")
class Account extends Row {
    @Column(nullable=false,unique=true,length=32) String username;
    @Column(name="display_name",nullable=false,length=40) String displayName;
    @Column(name="password_hash",nullable=false,length=256) String passwordHash;
    @Column(nullable=false,length=16) String role;
}
