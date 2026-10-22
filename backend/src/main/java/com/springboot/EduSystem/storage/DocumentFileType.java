package com.springboot.EduSystem.storage;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

public enum DocumentFileType {
    PDF(
            "pdf",
            "application/pdf",
            Set.of("application/pdf", "application/octet-stream")
    ),
    PPT(
            "ppt",
            "application/vnd.ms-powerpoint",
            Set.of(
                    "application/vnd.ms-powerpoint",
                    "application/mspowerpoint",
                    "application/x-mspowerpoint",
                    "application/octet-stream"
            )
    ),
    PPTX(
            "pptx",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            Set.of(
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "application/zip",
                    "application/x-zip-compressed",
                    "application/octet-stream"
            )
    );

    private final String extension;
    private final String contentType;
    private final Set<String> acceptedContentTypes;

    DocumentFileType(
            String extension,
            String contentType,
            Set<String> acceptedContentTypes
    ) {
        this.extension = extension;
        this.contentType = contentType;
        this.acceptedContentTypes = acceptedContentTypes;
    }

    public String getExtension() {
        return extension;
    }

    public String getContentType() {
        return contentType;
    }

    public boolean acceptsContentType(String declaredContentType) {
        if (declaredContentType == null || declaredContentType.isBlank()) {
            return true;
        }

        return acceptedContentTypes.contains(
                declaredContentType.toLowerCase(Locale.ROOT)
        );
    }

    public static Optional<DocumentFileType> fromFilename(String filename) {
        if (filename == null) {
            return Optional.empty();
        }

        int extensionStart = filename.lastIndexOf('.');
        if (extensionStart < 0 || extensionStart == filename.length() - 1) {
            return Optional.empty();
        }

        String extension = filename.substring(extensionStart + 1)
                .toLowerCase(Locale.ROOT);

        return Arrays.stream(values())
                .filter(fileType -> fileType.extension.equals(extension))
                .findFirst();
    }
}
