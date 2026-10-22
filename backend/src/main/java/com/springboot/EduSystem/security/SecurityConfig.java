package com.springboot.EduSystem.security;

import com.springboot.EduSystem.security.internal.ApiKeyAuthenticator;
import com.springboot.EduSystem.security.internal.InternalApiKeyFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(CustomUserDetailsService userDetailsService,
                                                PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    CookieCsrfTokenRepository authCsrfTokens(@Value("${app.auth.cookie-secure}") boolean secure) {
        CookieCsrfTokenRepository csrfTokens = new CookieCsrfTokenRepository();
        csrfTokens.setCookieName("CSRF_TOKEN");
        csrfTokens.setHeaderName("X-CSRF-TOKEN");
        csrfTokens.setCookieCustomizer(cookie -> cookie.path("/api/auth").sameSite("Lax").secure(secure));
        return csrfTokens;
    }

    @Bean
    @Order(0)
    SecurityFilterChain internalAuthFilterChain(HttpSecurity http,
                                               CookieCsrfTokenRepository authCsrfTokens,
                                               SecurityErrorHandler securityErrorHandler,
                                               ApiKeyAuthenticator authenticator)
            throws Exception {
        var apiKeyFilter = new InternalApiKeyFilter(authenticator);

        http
                .securityMatcher("/internal/**")
                .csrf(csrf -> csrf.csrfTokenRepository(authCsrfTokens))
                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .securityContext(context ->
                        context.securityContextRepository(
                                new NullSecurityContextRepository()))
                .addFilterBefore(
                        apiKeyFilter,
                        AnonymousAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/internal/auth/verify")
                        .hasAuthority("INTERNAL_AUTH_VERIFY")
                        .requestMatchers(HttpMethod.GET, "/internal/users/**")
                        .hasAuthority("INTERNAL_USERS_READ")
                        .anyRequest().denyAll())
                .exceptionHandling(errors -> errors
                        .authenticationEntryPoint((request, response, exception) -> {
                            response.setStatus(401);
                            response.setContentType("application/json");
                            response.getWriter().write("{\"message\":\"Access denied\"}");
                        })
                        .accessDeniedHandler(securityErrorHandler));
        return http.build();
    }

    @Bean
    @Order(1)
    SecurityFilterChain authFilterChain(HttpSecurity http,
                                       SecurityErrorHandler securityErrorHandler,
                                       CorsConfigurationSource corsConfigurationSource,
                                       CookieCsrfTokenRepository authCsrfTokens) {
        try {
            http
                    .securityMatcher("/api/auth/csrf", "/api/auth/register/student")
                    .cors(cors -> cors.configurationSource(corsConfigurationSource))
                    .csrf(csrf -> csrf.csrfTokenRepository(authCsrfTokens))
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .requestCache(cache -> cache.disable())
                    .formLogin(form -> form.disable())
                    .httpBasic(basic -> basic.disable())
                    .logout(logout -> logout.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers(HttpMethod.GET, "/api/auth/csrf").permitAll()
                            .requestMatchers(HttpMethod.POST, "/api/auth/register/student").permitAll()
                            .anyRequest().denyAll())
                    .exceptionHandling(errors -> errors
                            .authenticationEntryPoint(securityErrorHandler)
                            .accessDeniedHandler(securityErrorHandler));
            return http.build();
        } catch (Exception exception) {
            throw new IllegalStateException("Could not configure auth security", exception);
        }
    }

    @Bean
    @Order(2)
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            JwtAuthenticationConverter jwtAuthenticationConverter,
                                            SecurityErrorHandler securityErrorHandler,
                                            CorsConfigurationSource corsConfigurationSource) {
        try {
            http
                    .cors(cors -> cors.configurationSource(corsConfigurationSource))
                    .csrf(csrf -> csrf.disable())
                    .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                    .requestCache(cache -> cache.disable())
                    .formLogin(form -> form.disable())
                    .httpBasic(basic -> basic.disable())
                    .logout(logout -> logout.disable())
                    .authorizeHttpRequests(auth -> auth
                            .requestMatchers("/error").permitAll()
                            .anyRequest().authenticated())
                    .exceptionHandling(errors -> errors
                            .authenticationEntryPoint(securityErrorHandler)
                            .accessDeniedHandler(securityErrorHandler))
                    .oauth2ResourceServer(resourceServer -> resourceServer
                            .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                            .authenticationEntryPoint(securityErrorHandler)
                            .accessDeniedHandler(securityErrorHandler));
            return http.build();
        } catch (Exception e) {
            throw new IllegalStateException("Could not configure security", e);
        }
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origin}") String frontendOrigin) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(frontendOrigin));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Content-Type", "Authorization", "X-CSRF-TOKEN", "Accept"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
