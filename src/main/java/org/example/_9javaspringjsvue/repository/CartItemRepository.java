package org.example._9javaspringjsvue.repository;

import jakarta.transaction.Transactional;
import org.example._9javaspringjsvue.entity.Cart;
import org.example._9javaspringjsvue.entity.CartItem;
import org.example._9javaspringjsvue.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    // Найти все товары в корзине
    List<CartItem> findByCart(Cart cart);

    // Найти конкретный товар в корзине
    CartItem findByCartAndProduct(Cart cart, Product product);

    // Проверить наличие товара в корзине
    boolean existsByCartAndProduct(Cart cart, Product product);

    // Удалить все товары из корзины
    @Modifying
    @Transactional
    void deleteByCart(Cart cart);

    // Посчитать общее количество товаров в корзине
    @Query("SELECT SUM(ci.quantity) FROM CartItem ci WHERE ci.cart.id = :cartId")
    Integer countTotalQuantityByCartId(@Param("cartId") Long cartId);

    // Посчитать общую сумму заказа
    @Query("SELECT SUM(p.basePrice * ci.quantity) FROM CartItem ci " +
            "JOIN ci.product p WHERE ci.cart.id = :cartId")
    Double calculateTotalAmount(@Param("cartId") Long cartId);
}
