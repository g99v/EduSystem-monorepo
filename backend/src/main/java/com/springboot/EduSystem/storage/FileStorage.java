package com.springboot.EduSystem.storage;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

/**
 * Stores uploaded files on a shared root, disambiguated by a key prefix
 * (for example {@code lectures} or {@code assignments}).
 */
public interface FileStorage {
    StoredFile store(MultipartFile file, String keyPrefix);

    Resource load(String storageKey);

    void delete(String storageKey);

    boolean exists(String storageKey);

    record StoredFile(
            String storageKey,
            String originalFilename,
            String contentType,
            long fileSize
    ) {
    }
}
