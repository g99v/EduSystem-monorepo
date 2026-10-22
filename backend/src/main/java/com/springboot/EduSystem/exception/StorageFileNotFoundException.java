package com.springboot.EduSystem.exception;

public class StorageFileNotFoundException extends StorageException {
    public StorageFileNotFoundException(String storageKey) {
        super("Lecture file " + storageKey + " was not found");
    }
}
