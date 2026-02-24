package org.example._9javaspringjsvue.repository;

import org.example._9javaspringjsvue.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    // Аккаунт с такой почтой должен быть единственный
    Optional<User> findByEmail(String email);

    // Проверка существования email при регистрации
    boolean existsByEmail(String email);

    // Загрузка пользователя по email
    Optional<User> findByEmailAndIsDeletedFalse(String email);

    // Поиск администраторов (для внутреннего использования)
    @Query("SELECT u FROM User u WHERE u.role = 'ADMIN'")
    List<User> findAllAdmins();
}
