package com.piaochu.app;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
/** 所有用户私有资源查询都带 userId；客户端传来的资源 ID 本身不能证明归属。 */
interface OrderRepository extends JpaRepository<FoodOrder,Long> {
    List<FoodOrder> findTop100ByUserIdOrderByIdDesc(Long userId);
    List<FoodOrder> findTop100ByOrderByIdDesc();
    Optional<FoodOrder> findByIdAndUserId(Long id,Long userId);
    Optional<FoodOrder> findByUserIdAndRequestKey(Long userId,String key);
}
