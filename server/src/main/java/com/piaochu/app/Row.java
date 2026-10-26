package com.piaochu.app;
import jakarta.persistence.*;
import java.time.Instant;
/** 实体只用于持久化，HTTP 返回值由 Contracts 显式映射，密码与令牌摘要不会序列化到客户端。 */
@MappedSuperclass
abstract class Row {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) Long id;
    @Column(name="created_at",nullable=false) Instant createdAt;
    @Column(name="updated_at",nullable=false) Instant updatedAt;
    @Version long version;
    void stamp(Instant now) { if(createdAt==null) createdAt=now; updatedAt=now; }
}
