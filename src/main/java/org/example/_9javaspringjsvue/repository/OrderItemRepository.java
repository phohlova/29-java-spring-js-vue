package org.example._9javaspringjsvue.repository;

import org.example._9javaspringjsvue.entity.Order;
import org.example._9javaspringjsvue.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    // Найти все товары в заказе
    List<OrderItem> findByOrder(Order order);
}
