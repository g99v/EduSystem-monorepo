package com.springboot.EduSystem.security.internal;

import org.springframework.data.jpa.repository.JpaRepository;

public interface InternalApiKeyRepository extends JpaRepository<InternalApiKey, String> {

}
