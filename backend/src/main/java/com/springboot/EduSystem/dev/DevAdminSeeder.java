package com.springboot.EduSystem.dev;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.springboot.EduSystem.user.Role;
import com.springboot.EduSystem.user.UserAccountService;
import com.springboot.EduSystem.user.UserRepository;

/**
 * Seeds a single ADMIN account, but only while the {@code dev} profile is active.
 *
 * <p>Because the class is annotated with {@link Profile}, the bean is never created in
 * other profiles, so it cannot introduce a well-known credential into production.
 *
 * <p>The account is created through {@link UserAccountService#create}, which reuses the
 * application's own {@code PasswordEncoder} and email normalisation, so the stored hash is
 * always valid and this avoids hard-coding a bcrypt hash in a Liquibase changeset.
 */
@Configuration
@Profile("dev")
public class DevAdminSeeder {
    private static final Logger log = LoggerFactory.getLogger(DevAdminSeeder.class);

    @Bean
    ApplicationRunner seedDevAdmin(
            UserRepository users,
            UserAccountService accounts,
            @Value("${app.dev-admin.email:admin@example.com}") String email,
            @Value("${app.dev-admin.password:password123}") String password
    ) {
        return arguments -> {
            if (users.existsByEmailIgnoreCase(email)) {
                log.info("Dev admin account '{}' already exists; skipping seed", email);
                return;
            }

            accounts.create(email, password, Role.ADMIN);
            log.warn("Seeded dev admin account '{}' with a well-known password. "
                    + "This runs only with the 'dev' profile.", email);
        };
    }
}
