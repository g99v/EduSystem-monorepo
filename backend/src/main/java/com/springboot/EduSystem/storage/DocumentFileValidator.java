package com.springboot.EduSystem.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.springboot.EduSystem.exception.InvalidDocumentFileException;

@Component
public class DocumentFileValidator {
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final byte[] OLE_COMPOUND_DOCUMENT_SIGNATURE = {
            (byte) 0xD0, (byte) 0xCF, 0x11, (byte) 0xE0,
            (byte) 0xA1, (byte) 0xB1, 0x1A, (byte) 0xE1
    };
    private static final int PDF_HEADER_SCAN_SIZE = 1024;
    private static final int MAX_PPTX_ENTRIES_TO_SCAN = 1000;

    public ValidatedFile validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidDocumentFileException("File should not be empty");
        }

        String originalFilename = normalizeOriginalFilename(file.getOriginalFilename());
        DocumentFileType fileType = DocumentFileType.fromFilename(originalFilename)
                .orElseThrow(() -> new InvalidDocumentFileException(
                        "Only PDF, PPT, and PPTX files are supported"
                ));

        if (!fileType.acceptsContentType(file.getContentType())) {
            throw new InvalidDocumentFileException(
                    "The declared file type does not match the "
                            + fileType.getExtension().toUpperCase()
                            + " extension"
            );
        }

        if (!hasExpectedSignature(file, fileType)) {
            throw new InvalidDocumentFileException(
                    "The file contents are not a valid "
                            + fileType.getExtension().toUpperCase()
                            + " document"
            );
        }

        return new ValidatedFile(
                originalFilename,
                fileType,
                file.getSize()
        );
    }

    private boolean hasExpectedSignature(MultipartFile file, DocumentFileType fileType) {
        try {
            return switch (fileType) {
                case PDF -> hasPdfSignature(file);
                case PPT -> hasPptSignature(file);
                case PPTX -> hasPptxStructure(file);
            };
        } catch (IOException exception) {
            throw new InvalidDocumentFileException(
                    "Could not read the uploaded file",
                    exception
            );
        }
    }

    private boolean hasPdfSignature(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(PDF_HEADER_SCAN_SIZE);
            return containsSignature(header, PDF_SIGNATURE);
        }
    }

    private boolean hasPptSignature(MultipartFile file) throws IOException {
        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(OLE_COMPOUND_DOCUMENT_SIGNATURE.length);
            return startsWith(header, OLE_COMPOUND_DOCUMENT_SIGNATURE);
        }
    }

    private boolean hasPptxStructure(MultipartFile file) throws IOException {
        boolean hasContentTypes = false;
        boolean hasPresentation = false;
        int scannedEntries = 0;

        try (ZipInputStream zipInputStream = new ZipInputStream(file.getInputStream())) {
            ZipEntry entry;
            while ((entry = zipInputStream.getNextEntry()) != null
                    && scannedEntries++ < MAX_PPTX_ENTRIES_TO_SCAN) {
                String entryName = entry.getName();
                if ("[Content_Types].xml".equals(entryName)) {
                    hasContentTypes = true;
                } else if ("ppt/presentation.xml".equals(entryName)) {
                    hasPresentation = true;
                }

                if (hasContentTypes && hasPresentation) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean startsWith(byte[] value, byte[] prefix) {
        if (value.length < prefix.length) {
            return false;
        }

        for (int index = 0; index < prefix.length; index++) {
            if (value[index] != prefix[index]) {
                return false;
            }
        }

        return true;
    }

    private boolean containsSignature(byte[] value, byte[] signature) {
        if (value.length < signature.length) {
            return false;
        }

        for (int offset = 0; offset <= value.length - signature.length; offset++) {
            boolean match = true;
            for (int index = 0; index < signature.length; index++) {
                if (value[offset + index] != signature[index]) {
                    match = false;
                    break;
                }
            }

            if (match) {
                return true;
            }
        }

        return false;
    }

    private String normalizeOriginalFilename(String submittedFilename) {
        if (submittedFilename == null || submittedFilename.isBlank()) {
            throw new InvalidDocumentFileException("File name is required");
        }

        String normalizedSeparators = submittedFilename.replace('\\', '/');
        int filenameStart = normalizedSeparators.lastIndexOf('/');
        String filename = normalizedSeparators.substring(filenameStart + 1).trim();

        if (filename.isEmpty()) {
            throw new InvalidDocumentFileException("File name is required");
        }

        return filename;
    }

    public record ValidatedFile(
            String originalFilename,
            DocumentFileType fileType,
            long fileSize
    ) {
        public String contentType() {
            return fileType.getContentType();
        }
    }
}
