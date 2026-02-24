package org.example._9javaspringjsvue.repository;

import org.example._9javaspringjsvue.entity.Order;
import org.example._9javaspringjsvue.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    // Найти все заказы пользователя
    List<Order> findByUser(User user);

    // Найти заказы по статусу
    List<Order> findByStatus(String status);

    // Посчитать общую сумму заказов пользователя
    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.user.id = :userId")
    Double getTotalSpentByUserId(@Param("userId") Long userId);
}
