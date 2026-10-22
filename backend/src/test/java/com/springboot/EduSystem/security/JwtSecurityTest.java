package com.springboot.EduSystem.security;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.security.MessageDigest;
import com.springboot.EduSystem.security.internal.ApiKeyAuthenticator;
import com.springboot.EduSystem.security.internal.InternalApiKey;
import com.springboot.EduSystem.security.internal.InternalApiKeyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.springboot.EduSystem.auth.AuthController;
import com.springboot.EduSystem.auth.AuthService;
import com.springboot.EduSystem.auth.InternalAuthController;
import com.springboot.EduSystem.course.Course;
import com.springboot.EduSystem.enrollment.EnrollmentRepository;
import com.springboot.EduSystem.exception.GlobalExceptionHandler;
import com.springboot.EduSystem.instructor.Instructor;
import com.springboot.EduSystem.instructor.InstructorRepository;
import com.springboot.EduSystem.school.SchoolRepository;
import com.springboot.EduSystem.student.StudentRepository;
import com.springboot.EduSystem.user.Role;
import com.springboot.EduSystem.user.User;
import com.springboot.EduSystem.user.UserAccountService;
import com.springboot.EduSystem.user.UserRepository;

import jakarta.servlet.http.Cookie;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringJUnitWebConfig(JwtSecurityTest.TestConfig.class)
@TestPropertySource(properties = {"app.cors.allowed-origin=http://localhost:5173", "app.auth.cookie-secure=false"})
class JwtSecurityTest {
    @Autowired WebApplicationContext context;
    @Autowired UserRepository users;
    @Autowired InstructorRepository instructors;
    @Autowired StudentRepository students;
    @Autowired TestJwtIssuer jwtService;
    @Autowired JwtEncoder encoder;
    @Autowired PasswordEncoder passwordEncoder;
    private MockMvc mvc;
    private User user;
    private InternalApiKey kongKey;
    private static final String API_KEY = "kong-test." + "A".repeat(43);
    @Autowired InternalApiKeyRepository apiKeys;

    @BeforeEach
    void setUp() throws Exception {
        reset(users, instructors, students, apiKeys);
        kongKey = new InternalApiKey();
        kongKey.setKeyId("kong-test");
        kongKey.setClientId("kong");
        kongKey.setHash(MessageDigest.getInstance("SHA-256").digest(API_KEY.getBytes(StandardCharsets.UTF_8)));
        kongKey.setAuthorities(Set.of("INTERNAL_AUTH_VERIFY"));
        when(apiKeys.findById("kong-test")).thenReturn(Optional.of(kongKey));
        user = new User();
        ReflectionTestUtils.setField(user, "id", 5);
        user.setEmail("student@example.com");
        user.setPassword(passwordEncoder.encode("password123"));
        user.setRole(Role.STUDENT);
        user.setEnabled(true);
        when(users.findById(5)).thenReturn(Optional.of(user));
        when(users.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .addFilters(context.getBean(FilterChainProxy.class))
                .build();
    }

    @Test
    void internalVerifyReturnsProfileWithoutTokenOrSession() throws Exception {
        MvcResult result = mvc.perform(withCsrf(withApiKey(post("/internal/auth/verify")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, "no-store"))
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();
        assertStateless(result);
    }

    @Test
    void invalidLoginReturnsJsonUnauthorizedWithoutSession() throws Exception {
        MvcResult result = mvc.perform(withCsrf(withApiKey(post("/internal/auth/verify")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"password\":\"incorrect\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid email or password"))
                .andReturn();
        assertStateless(result);
    }

    @Test
    void internalVerifyRejectsMissingApiKeyEvenWithJwtAndSpoofedHeaders() throws Exception {
        mvc.perform(withCsrf(post("/internal/auth/verify"))
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .header("X-SSL-Client-CN", "kong-auth")
                        .header("X-Forwarded-Client-Cert", "CN=kong-auth")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE));
    }

    @Test
    void internalVerifyRejectsWrongApiKeyAndDuplicateHeaders() throws Exception {
        mvc.perform(withCsrf(post("/internal/auth/verify"))
                        .header("X-Internal-Api-Key", "kong-test." + "B".repeat(43)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid service credential"));
        mvc.perform(withCsrf(post("/internal/auth/verify"))
                        .header("X-Internal-Api-Key", API_KEY, API_KEY))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void internalPermissionsAreCheckedBeforeControllerAndCanDifferByEndpoint() throws Exception {
        kongKey.setAuthorities(Set.of("INTERNAL_USERS_READ"));
        mvc.perform(withCsrf(withApiKey(post("/internal/auth/verify"))))
                .andExpect(status().isForbidden());
        mvc.perform(withApiKey(get("/internal/users/profile")))
                .andExpect(status().isOk()).andExpect(content().string("kong"));
        kongKey.setAuthorities(Set.of("INTERNAL_AUTH_VERIFY"));
        mvc.perform(withApiKey(get("/internal/users/profile")))
                .andExpect(status().isForbidden());
    }

    @Test
    void internalVerifyStillRequiresMatchingCsrf() throws Exception {
        mvc.perform(withApiKey(post("/internal/auth/verify"))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        MvcResult csrf = mvc.perform(get("/api/auth/csrf")).andReturn();
        mvc.perform(withApiKey(post("/internal/auth/verify"))
                        .cookie(csrf.getResponse().getCookie("CSRF_TOKEN"))
                        .header("X-CSRF-TOKEN", "wrong-token")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void apiKeyDoesNotAuthenticateNormalApiRequests() throws Exception {
        mvc.perform(withApiKey(get("/api/auth/me")))
                .andExpect(status().isUnauthorized());
        mvc.perform(withApiKey(get("/api/auth/me"))
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk());
    }

    @Test
    void internalChainDeniesEveryOtherEndpointAndMethod() throws Exception {
        mvc.perform(withApiKey(get("/internal/auth/verify")))
                .andExpect(status().isForbidden());
        mvc.perform(withCsrf(withApiKey(post("/internal/anything"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void oldPublicLoginRemainsUnavailable() throws Exception {
        mvc.perform(withCsrf(post("/api/auth/login"))
                        .header(HttpHeaders.AUTHORIZATION, bearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"student@example.com\",\"password\":\"password123\"}"))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(longs = {599, 600, 601})
    void accessTokenExpiresAtTenMinutesWithoutGracePeriod(long elapsedSeconds) {
        Instant issuedAt = Instant.now().minusSeconds(60);
        TestJwtIssuer service = new TestJwtIssuer(encoder, Clock.fixed(issuedAt, ZoneOffset.UTC),
                "edusystem", "edusystem-api", 600);
        String token = service.createToken(user).getTokenValue();
        RSAPublicKey publicKey = (RSAPublicKey) context.getBean(KeyPair.class).getPublic();
        JwtDecoder decoder = new JwtConfig().jwtDecoder(publicKey,
                Clock.fixed(issuedAt.plusSeconds(elapsedSeconds), ZoneOffset.UTC),
                "edusystem", "edusystem-api");
        if (elapsedSeconds < 600) {
            assertEquals(issuedAt.plusSeconds(600).getEpochSecond(), decoder.decode(token).getExpiresAt().getEpochSecond());
        } else {
            assertThrows(org.springframework.security.oauth2.jwt.JwtException.class, () -> decoder.decode(token));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"refresh", "logout"})
    void removedAuthEndpointsAreUnavailable(String endpoint) throws Exception {
        mvc.perform(post("/api/auth/" + endpoint).header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void sessionCookieDoesNotAuthenticateRequests() throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/me").cookie(new Cookie("JSESSIONID", "old-session")))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.message").isString())
                .andReturn();
        assertStateless(result);
    }

    @Test
    void bearerWriteDoesNotRequireCsrfToken() throws Exception {
        MvcResult result = mvc.perform(post("/api/test/write")
                        .header(HttpHeaders.AUTHORIZATION, bearerToken()))
                .andExpect(status().isOk())
                .andExpect(content().string("student@example.com"))
                .andReturn();
        assertStateless(result);
    }

    @Test
    void currentDatabaseRoleControlsPermissionsForSameToken() throws Exception {
        String token = bearerToken();
        mvc.perform(get("/api/test/admin").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
        user.setRole(Role.ADMIN);
        mvc.perform(get("/api/test/admin").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        user.setRole(Role.STUDENT);
        mvc.perform(get("/api/test/admin").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    @Test
    void disabledAndDeletedAccountsCannotUsePreviouslyIssuedToken() throws Exception {
        String token = bearerToken();
        user.setEnabled(false);
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
        when(users.findById(5)).thenReturn(Optional.empty());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emailChangesPreserveCourseOwnershipChecks() throws Exception {
        String token = bearerToken();
        user.setEmail("instructor-new@example.com");
        user.setRole(Role.INSTRUCTOR);
        Instructor instructor = new Instructor();
        ReflectionTestUtils.setField(instructor, "id", 7);
        when(instructors.findByUser_EmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(instructor));
        mvc.perform(get("/api/test/course").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk());
        ReflectionTestUtils.setField(instructor, "id", 8);
        mvc.perform(get("/api/test/course").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"expired", "future", "issuer", "audience", "missing-expiry", "subject", "missing-subject"})
    void rejectsInvalidClaims(String invalidClaim) throws Exception {
        Instant now = Instant.now();
        JwtClaimsSet.Builder claims = JwtClaimsSet.builder()
                .issuer("edusystem").audience(List.of("edusystem-api"))
                .issuedAt(now.minusSeconds(1200)).notBefore(now.minusSeconds(1200));
        if (!invalidClaim.equals("missing-subject")) {
            claims.subject("5");
        }
        if (!invalidClaim.equals("missing-expiry")) {
            claims.expiresAt(invalidClaim.equals("expired") ? now.minusSeconds(120) : now.plusSeconds(600));
        }
        switch (invalidClaim) {
            case "future" -> claims.notBefore(now.plusSeconds(120));
            case "issuer" -> claims.issuer("another-app");
            case "audience" -> claims.audience(List.of("another-api"));
            case "subject" -> claims.subject("invalid-user-id");
        }
        String token = encoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS256).build(), claims.build())).getTokenValue();
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void rejectsMalformedTokensAndSignaturesFromAnotherKey() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token"))
                .andExpect(status().isUnauthorized());
        KeyPair otherKeys = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        JwtEncoder otherEncoder = NimbusJwtEncoder.withKeyPair(
                (RSAPublicKey) otherKeys.getPublic(), (RSAPrivateKey) otherKeys.getPrivate()).build();
        TestJwtIssuer otherService = new TestJwtIssuer(otherEncoder, Clock.systemUTC(), "edusystem", "edusystem-api", 600);
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION,
                        "Bearer " + otherService.createToken(user).getTokenValue()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsOtherSigningAlgorithms() throws Exception {
        KeyPair keys = context.getBean(KeyPair.class);
        JwtEncoder otherEncoder = NimbusJwtEncoder.withKeyPair(
                        (RSAPublicKey) keys.getPublic(), (RSAPrivateKey) keys.getPrivate())
                .algorithm(SignatureAlgorithm.RS512).build();
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("edusystem").audience(List.of("edusystem-api")).subject("5")
                .issuedAt(now).expiresAt(now.plusSeconds(600)).build();
        String token = otherEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(SignatureAlgorithm.RS512).build(), claims)).getTokenValue();
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void corsAllowsBearerHeaderForConfiguredOrigin() throws Exception {
        mvc.perform(options("/api/auth/me")
                        .header(HttpHeaders.ORIGIN, "http://localhost:5173")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "Authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS, "Authorization"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"register/student"})
    void authWritesRequireCsrf(String endpoint) throws Exception {
        mvc.perform(post("/api/auth/" + endpoint)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void mismatchedCsrfTokenIsRejected() throws Exception {
        MvcResult csrf = mvc.perform(get("/api/auth/csrf")).andReturn();
        mvc.perform(post("/api/auth/register/student")
                        .cookie(csrf.getResponse().getCookie("CSRF_TOKEN"))
                        .header("X-CSRF-TOKEN", "incorrect"))
                .andExpect(status().isForbidden());
        assertStateless(csrf);
    }

    @Test
    void pemKeysCanBeLoadedForSigningAndValidation() throws Exception {
        KeyPair keys = context.getBean(KeyPair.class);
        JwtConfig configuration = new JwtConfig();
        String publicPem = "-----BEGIN PUBLIC KEY-----\n"
                + Base64.getEncoder().encodeToString(keys.getPublic().getEncoded())
                + "\n-----END PUBLIC KEY-----\n";
        String privatePem = "-----BEGIN PRIVATE KEY-----\n"
                + Base64.getEncoder().encodeToString(keys.getPrivate().getEncoded())
                + "\n-----END PRIVATE KEY-----\n";
        RSAPublicKey publicKey = configuration.jwtPublicKey(
                new ByteArrayResource(publicPem.getBytes(StandardCharsets.US_ASCII)));
        RSAPrivateKey privateKey = (RSAPrivateKey) keys.getPrivate();
        TestJwtIssuer service = new TestJwtIssuer(testEncoder(publicKey, privateKey),
                Clock.systemUTC(), "edusystem", "edusystem-api", 600);
        String token = service.createToken(user).getTokenValue();

        assertEquals("5", configuration.jwtDecoder(publicKey, Clock.systemUTC(), "edusystem", "edusystem-api")
                .decode(token).getSubject());
    }

    private String bearerToken() {
        return "Bearer " + jwtService.createToken(user).getTokenValue();
    }

    private MockHttpServletRequestBuilder withApiKey(MockHttpServletRequestBuilder request) {
        return request.secure(true).header("X-Internal-Api-Key", API_KEY);
    }

    private static JwtEncoder testEncoder(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
        return NimbusJwtEncoder.withKeyPair(publicKey, privateKey).build();
    }

    // Test fixture only: production JWT signing belongs to Kong.
    static class TestJwtIssuer {
        private final JwtEncoder encoder;
        private final Clock clock;
        private final String issuer;
        private final String audience;
        private final long ttl;

        TestJwtIssuer(JwtEncoder encoder, Clock clock, String issuer, String audience, long ttl) {
            this.encoder = encoder;
            this.clock = clock;
            this.issuer = issuer;
            this.audience = audience;
            this.ttl = ttl;
        }

        org.springframework.security.oauth2.jwt.Jwt createToken(User user) {
            Instant now = clock.instant();
            var claims = JwtClaimsSet.builder().issuer(issuer).audience(List.of(audience))
                    .subject(String.valueOf(user.getId())).issuedAt(now).notBefore(now)
                    .expiresAt(now.plusSeconds(ttl)).claim("roles", List.of(user.getRole().name())).build();
            return encoder.encode(JwtEncoderParameters.from(
                    JwsHeader.with(SignatureAlgorithm.RS256).build(), claims));
        }
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request) throws Exception {
        MvcResult result = mvc.perform(get("/api/auth/csrf")).andExpect(status().isOk()).andReturn();
        String token = JsonMapper.builder().build()
                .readTree(result.getResponse().getContentAsString()).get("token").asString();
        return request.cookie(result.getResponse().getCookie("CSRF_TOKEN")).header("X-CSRF-TOKEN", token);
    }

    private void assertStateless(MvcResult result) {
        assertNull(result.getRequest().getSession(false));
        assertNull(result.getResponse().getCookie("JSESSIONID"));
    }

    @Configuration
    @EnableWebMvc
    @EnableWebSecurity
    @Import({SecurityConfig.class, JwtAuthenticationConverter.class, SecurityErrorHandler.class,
            ApiKeyAuthenticator.class, CustomUserDetailsService.class, AuthController.class, InternalAuthController.class, AuthService.class,
            GlobalExceptionHandler.class, CourseAccessPolicy.class, TestController.class})
    static class TestConfig {
        @Bean InternalApiKeyRepository apiKeys() { return mock(InternalApiKeyRepository.class); }
        @Bean UserRepository users() { return mock(UserRepository.class); }
        @Bean UserAccountService accounts() { return mock(UserAccountService.class); }
        @Bean StudentRepository students() { return mock(StudentRepository.class); }
        @Bean SchoolRepository schools() { return mock(SchoolRepository.class); }
        @Bean InstructorRepository instructors() { return mock(InstructorRepository.class); }
        @Bean EnrollmentRepository enrollments() { return mock(EnrollmentRepository.class); }
        @Bean Clock clock() { return Clock.systemUTC(); }

        @Bean
        KeyPair keys() throws Exception {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            return generator.generateKeyPair();
        }

        @Bean
        JwtEncoder encoder(KeyPair keys) {
            return testEncoder((RSAPublicKey) keys.getPublic(), (RSAPrivateKey) keys.getPrivate());
        }

        @Bean
        JwtDecoder decoder(KeyPair keys) {
            return new JwtConfig().jwtDecoder((RSAPublicKey) keys.getPublic(), Clock.systemUTC(), "edusystem", "edusystem-api");
        }

        @Bean
        TestJwtIssuer jwtService(JwtEncoder encoder) {
            return new TestJwtIssuer(encoder, Clock.systemUTC(), "edusystem", "edusystem-api", 600);
        }
    }

    @RestController
    public static class TestController {
        private final CourseAccessPolicy courseAccess;

        public TestController(CourseAccessPolicy courseAccess) {
            this.courseAccess = courseAccess;
        }

        @GetMapping("/internal/users/profile")
        public String serviceIdentity(Authentication authentication) {
            return authentication.getName();
        }

        @PostMapping("/api/test/write")
        public String write(Authentication authentication) {
            return authentication.getName();
        }

        @GetMapping("/api/test/admin")
        @PreAuthorize("hasRole('ADMIN')")
        public String admin() {
            return "allowed";
        }

        @GetMapping("/api/test/course")
        public String course(Authentication authentication) {
            Instructor owner = new Instructor();
            ReflectionTestUtils.setField(owner, "id", 7);
            Course course = new Course();
            course.setInstructor(owner);
            courseAccess.requireCourseManagement(course, authentication);
            return "allowed";
        }
    }
}
