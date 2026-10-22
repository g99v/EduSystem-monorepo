package com.springboot.EduSystem.user;

import com.springboot.EduSystem.exception.EmailAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserAccountServiceTest {
    @Mock UserRepository users;
    @Mock PasswordEncoder encoder;
    @InjectMocks UserAccountService service;

    @Test
    void encodesPasswordAndNormalizesEmail() {
        when(encoder.encode("password123")).thenReturn("bcrypt-hash");
        when(users.saveAndFlush(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User user = service.create("  Student@Example.com ", "password123", Role.STUDENT);

        assertEquals("student@example.com", user.getEmail());
        assertEquals("bcrypt-hash", user.getPassword());
        assertEquals(Role.STUDENT, user.getRole());
        assertTrue(user.isEnabled());
    }

    @Test
    void duplicateEmailDoesNotEncodeOrSave() {
        when(users.existsByEmailIgnoreCase("taken@example.com")).thenReturn(true);
        assertThrows(EmailAlreadyExistsException.class,
                () -> service.create("taken@example.com", "password123", Role.STUDENT));
        verify(encoder, never()).encode(any());
        verify(users, never()).saveAndFlush(any());
    }
}
