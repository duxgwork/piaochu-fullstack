package com.piaochu.app;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
/** 所有用户私有资源查询都带 userId；客户端传来的资源 ID 本身不能证明归属。 */
interface SessionRepository extends JpaRepository<Session,Long> {
    Optional<Session> findByTokenHash(String hash);
    void deleteByTokenHash(String hash);
    void deleteByExpiresAtBefore(java.time.Instant now);
}
