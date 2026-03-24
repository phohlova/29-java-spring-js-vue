package org.example._9javaspringjsvue.service;

import org.apache.catalina.UserDatabase;
import org.example._9javaspringjsvue.dto.AuthResponse;
import org.example._9javaspringjsvue.dto.LoginRequest;
import org.example._9javaspringjsvue.dto.RegisterRequest;
import org.example._9javaspringjsvue.entity.User;
import org.example._9javaspringjsvue.repository.UserRepository;
import org.example._9javaspringjsvue.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.ZonedDateTime;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtUtil jwtUtil;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;
    private User testUser;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest("Ivan", "Ivanov", "test@example.com", "password");
        loginRequest = new LoginRequest("test@example.com", "password");

        testUser = new User();
        testUser.setId(1L);
        testUser.setFirstName("Ivan");
        testUser.setLastName("Ivanov");
        testUser.setEmail("test@example.com");
        testUser.setPasswordHash("hashed_password");
        testUser.setRole("USER");
        testUser.setCreatedAt(ZonedDateTime.now());
    }

    @Test
    public void register_ShouldThrowException_WhenEmailExists() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(true);

        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("Аккаунт с такой почтой уже существует", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    public void register_ShouldSuccess_WhenEmailNotExists() {
        when(userRepository.existsByEmail(registerRequest.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(registerRequest.getPassword())).thenReturn("hashed_password");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // эмуляция загрузки пользователя для генерации токена
        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(testUser.getEmail())).thenReturn(userDetails);
        when(jwtUtil.generateToken(userDetails)).thenReturn("fake_jwt_token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("fake_jwt_token", response.getToken());
        assertEquals("test@example.com", response.getEmail());
        assertEquals("USER", response.getRole());

        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    public void login_ShouldThrowException_WhenInvalidCredentials() {
        doThrow(new BadCredentialsException("Неверный пароль")).when(authenticationManager)
                .authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(RuntimeException.class, () -> {
            authService.login(loginRequest);
        });
    }

    @Test
    public void login_ShouldSuccess_WhenCredentialsValid() {
        Authentication mockAuth = mock(Authentication.class);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(mockAuth);

        UserDetails userDetails = mock(UserDetails.class);
        when(userDetailsService.loadUserByUsername(loginRequest.getEmail())).thenReturn(userDetails);
        when(userRepository.findByEmail(loginRequest.getEmail())).thenReturn(Optional.of(testUser));
        when(jwtUtil.generateToken(userDetails)).thenReturn("valid_jwt_token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("valid_jwt_token", response.getToken());
        assertEquals("Ivan", response.getFirstName());
    }
}
