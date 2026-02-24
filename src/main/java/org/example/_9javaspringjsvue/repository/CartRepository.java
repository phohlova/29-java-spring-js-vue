package org.example._9javaspringjsvue.repository;

import org.example._9javaspringjsvue.entity.Cart;
import org.example._9javaspringjsvue.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    // Найти корзину пользователя
    Optional<Cart> findByUser(User user);

    // Проверить существование корзины
    boolean existsByUser(User user);
}
