package com.springboot.EduSystem.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Locale;
import java.util.UUID;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.springboot.EduSystem.exception.StorageException;
import com.springboot.EduSystem.exception.StorageFileNotFoundException;

import jakarta.annotation.PostConstruct;

@Service
public class FileSystemFileStorage implements FileStorage {
    private static final String DEFAULT_CONTENT_TYPE = "application/octet-stream";

    private final Path storageRoot;

    public FileSystemFileStorage(StorageProperties properties) {
        this.storageRoot = properties.getDirectory()
                .toAbsolutePath()
                .normalize();
    }

    @PostConstruct
    public void initialize() {
        try {
            Files.createDirectories(storageRoot);
        } catch (IOException exception) {
            throw new StorageException("Could not initialize file storage", exception);
        }
    }

    @Override
    public StoredFile store(MultipartFile file, String keyPrefix) {
        if (file == null || file.isEmpty()) {
            throw new StorageException("Cannot store an empty file");
        }

        String originalFilename = normalizeOriginalFilename(file.getOriginalFilename());
        String storageKey = buildStorageKey(keyPrefix, originalFilename);
        Path destination = resolveStorageKey(storageKey);

        try (InputStream inputStream = file.getInputStream()) {
            Files.createDirectories(destination.getParent());
            Files.copy(inputStream, destination);
        } catch (IOException exception) {
            throw new StorageException("Could not store file", exception);
        }

        String contentType = DocumentFileType.fromFilename(originalFilename)
                .map(DocumentFileType::getContentType)
                .orElseGet(() -> file.getContentType() == null
                        ? DEFAULT_CONTENT_TYPE
                        : file.getContentType());

        return new StoredFile(
                storageKey,
                originalFilename,
                contentType,
                file.getSize()
        );
    }

    @Override
    public Resource load(String storageKey) {
        Path storedFile = resolveStorageKey(storageKey);

        if (!Files.isRegularFile(storedFile, LinkOption.NOFOLLOW_LINKS)) {
            throw new StorageFileNotFoundException(storageKey);
        }

        return new FileSystemResource(storedFile);
    }

    @Override
    public void delete(String storageKey) {
        Path storedFile = resolveStorageKey(storageKey);

        try {
            Files.deleteIfExists(storedFile);
        } catch (IOException exception) {
            throw new StorageException("Could not delete file " + storageKey, exception);
        }
    }

    @Override
    public boolean exists(String storageKey) {
        return Files.isRegularFile(
                resolveStorageKey(storageKey),
                LinkOption.NOFOLLOW_LINKS
        );
    }

    private Path resolveStorageKey(String storageKey) {
        if (storageKey == null || storageKey.isBlank()) {
            throw new StorageException("Storage key should not be blank");
        }

        Path resolvedPath = storageRoot.resolve(storageKey).normalize();
        if (!resolvedPath.startsWith(storageRoot)) {
            throw new StorageException("Invalid file storage key");
        }

        return resolvedPath;
    }

    private String buildStorageKey(String keyPrefix, String originalFilename) {
        String prefix = normalizeKeyPrefix(keyPrefix);
        String filename = UUID.randomUUID() + extractExtension(originalFilename);
        return prefix.isEmpty() ? filename : prefix + "/" + filename;
    }

    private String normalizeKeyPrefix(String keyPrefix) {
        if (keyPrefix == null || keyPrefix.isBlank()) {
            return "";
        }

        String prefix = keyPrefix.trim().toLowerCase(Locale.ROOT);
        if (!prefix.matches("[a-z0-9][a-z0-9-]{0,31}")) {
            throw new StorageException("Invalid file storage key prefix");
        }

        return prefix;
    }

    private String normalizeOriginalFilename(String submittedFilename) {
        if (submittedFilename == null || submittedFilename.isBlank()) {
            return "file";
        }

        String normalizedSeparators = submittedFilename.replace('\\', '/');
        String filename = Path.of(normalizedSeparators).getFileName().toString().trim();
        return filename.isEmpty() ? "file" : filename;
    }

    private String extractExtension(String filename) {
        int extensionStart = filename.lastIndexOf('.');
        if (extensionStart <= 0 || extensionStart == filename.length() - 1) {
            return "";
        }

        String extension = filename.substring(extensionStart + 1);
        if (!extension.matches("[A-Za-z0-9]{1,10}")) {
            return "";
        }

        return "." + extension.toLowerCase(Locale.ROOT);
    }
}
