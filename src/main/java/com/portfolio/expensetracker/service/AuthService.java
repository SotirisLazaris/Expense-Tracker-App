package com.portfolio.expensetracker.service;

import com.portfolio.expensetracker.dto.AuthResponse;
import com.portfolio.expensetracker.dto.LoginRequest;
import com.portfolio.expensetracker.dto.RegisterRequest;
import com.portfolio.expensetracker.exception.ConflictException;
import com.portfolio.expensetracker.model.User;
import com.portfolio.expensetracker.repository.UserRepository;
import com.portfolio.expensetracker.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final CategoryService categoryService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService,
                       CategoryService categoryService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.categoryService = categoryService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("An account with this email already exists");
        }

        User user = userRepository.save(
                new User(email, passwordEncoder.encode(request.password()), request.fullName().trim()));
        categoryService.createDefaultCategories(user);

        return AuthResponse.bearer(jwtService.generateToken(user), user.getEmail(), user.getFullName());
    }

    public AuthResponse login(LoginRequest request) {
        // Throws BadCredentialsException (-> 401 in GlobalExceptionHandler) on failure.
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim().toLowerCase(), request.password()));
        User user = (User) authentication.getPrincipal();
        return AuthResponse.bearer(jwtService.generateToken(user), user.getEmail(), user.getFullName());
    }
}
