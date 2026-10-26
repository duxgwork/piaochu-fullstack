package com.piaochu.app;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
/** 所有用户私有资源查询都带 userId；客户端传来的资源 ID 本身不能证明归属。 */
interface BookingRepository extends JpaRepository<Booking,Long> {
    List<Booking> findTop100ByUserIdOrderByIdDesc(Long userId);
    List<Booking> findTop100ByOrderByIdDesc();
    Optional<Booking> findByIdAndUserId(Long id,Long userId);
    Optional<Booking> findByUserIdAndRequestKey(Long userId,String key);
}
