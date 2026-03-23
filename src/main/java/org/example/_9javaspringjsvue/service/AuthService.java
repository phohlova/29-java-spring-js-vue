package org.example._9javaspringjsvue.service;

import org.example._9javaspringjsvue.dto.LoginRequest;
import org.example._9javaspringjsvue.dto.RegisterRequest;
import org.example._9javaspringjsvue.dto.AuthResponse;
import org.example._9javaspringjsvue.entity.User;
import org.example._9javaspringjsvue.repository.UserRepository;
import org.example._9javaspringjsvue.security.JwtUtil;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final CustomUserDetailsService userDetailsService;

    // Конструктор с внедрением зависимостей
    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtUtil jwtUtil,
                       AuthenticationManager authenticationManager,
                       CustomUserDetailsService userDetailsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    /**
     * ТЗ: "Страница регистрации"
     * Регистрация нового пользователя
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // ТЗ: "аккаунт с такой почтой должен быть единственный в системе"
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Аккаунт с такой почтой уже существует");
        }

        // Создаём нового пользователя
        User user = new User();
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        // ТЗ: пароль хешируется
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        // ТЗ: "Все пользователи, зарегистрированные через страницу регистрации, могут быть только простыми пользователями"
        user.setRole("USER");
        user.setCreatedAt(ZonedDateTime.now());

        userRepository.save(user);

        // Генерируем JWT токен
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String token = jwtUtil.generateToken(userDetails);

        // Возвращаем ответ
        return new AuthResponse(
                token,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }

    /**
     * ТЗ: "Страница авторизации"
     * Вход в систему
     */
    public AuthResponse login(LoginRequest request) {
        // Аутентификация (проверка email/пароля)
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        // Загружаем пользователя
        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        // Генерируем токен
        String token = jwtUtil.generateToken(userDetails);

        return new AuthResponse(
                token,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getRole()
        );
    }

    /**
     * Получение текущего пользователя по email
     */
    public User getCurrentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));
    }
}