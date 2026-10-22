package com.springboot.EduSystem.user;

import com.springboot.EduSystem.exception.EmailAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class UserAccountService {
    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;

    public UserAccountService(UserRepository users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public User create(String email, String rawPassword, Role role) {
        String normalizedEmail = email.trim().toLowerCase(Locale.ROOT);
        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new EmailAlreadyExistsException();
        }
        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setEnabled(true);
        try {
            return users.saveAndFlush(user);
        } catch (DataIntegrityViolationException exception) {

            throw new EmailAlreadyExistsException();
        }
    }
}
