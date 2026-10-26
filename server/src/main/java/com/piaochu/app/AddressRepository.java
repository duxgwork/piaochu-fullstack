package com.piaochu.app;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
/** 所有用户私有资源查询都带 userId；客户端传来的资源 ID 本身不能证明归属。 */
interface AddressRepository extends JpaRepository<Address,Long> {
    List<Address> findByUserIdOrderByIdDesc(Long userId);
    Optional<Address> findByIdAndUserId(Long id,Long userId);
    long countByUserId(Long userId);
}
