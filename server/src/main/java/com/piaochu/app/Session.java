package com.piaochu.app;
import jakarta.persistence.*;
import java.time.Instant;
/** 实体只用于持久化，HTTP 返回值由 Contracts 显式映射，密码与令牌摘要不会序列化到客户端。 */
@Entity @Table(name="sessions")
class Session extends Row {
    @Column(name="user_id",nullable=false) Long userId;
    @Column(name="token_hash",nullable=false,unique=true,length=64) String tokenHash;
    @Column(name="expires_at",nullable=false) Instant expiresAt;
}
