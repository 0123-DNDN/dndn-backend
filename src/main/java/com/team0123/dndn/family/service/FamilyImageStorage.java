package com.team0123.dndn.family.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Component
public class FamilyImageStorage {

    public static final Path UPLOAD_DIRECTORY = Path.of(
            "uploads",
            "family"
    ).toAbsolutePath().normalize();

    static final long MAX_FILE_SIZE = 10L * 1024 * 1024;
    private static final String WEB_PATH_PREFIX = "/uploads/family/";
    private static final Map<String, String> ALLOWED_CONTENT_TYPES = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );
    private static final Map<String, String> ALLOWED_EXTENSIONS = Map.of(
            "jpg", "jpg",
            "jpeg", "jpg",
            "png", "png",
            "webp", "webp"
    );
    private static final Logger log =
            LoggerFactory.getLogger(FamilyImageStorage.class);

    private final Path uploadDirectory;

    public FamilyImageStorage() {
        this(UPLOAD_DIRECTORY);
    }

    FamilyImageStorage(Path uploadDirectory) {
        this.uploadDirectory = uploadDirectory.toAbsolutePath().normalize();
    }

    public String store(MultipartFile image) {
        validate(image);

        String extension = resolveExtension(image);
        String storedFilename = UUID.randomUUID() + "." + extension;
        Path target = uploadDirectory.resolve(storedFilename).normalize();

        if (!target.getParent().equals(uploadDirectory)) {
            throw new IllegalArgumentException("올바르지 않은 이미지 저장 경로입니다.");
        }

        boolean targetAlreadyExisted = Files.exists(target);
        try {
            Files.createDirectories(uploadDirectory);
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, target);
            }
            return WEB_PATH_PREFIX + storedFilename;
        } catch (IOException exception) {
            if (!targetAlreadyExisted) {
                try {
                    Files.deleteIfExists(target);
                } catch (IOException cleanupException) {
                    log.warn("저장에 실패한 일부 이미지 파일을 삭제하지 못했습니다.");
                }
            }
            throw new IllegalArgumentException(
                    "이미지 파일을 저장하지 못했습니다."
            );
        }
    }

    public void deleteIfExists(String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(WEB_PATH_PREFIX)) {
            return;
        }

        String storedFilename = imageUrl.substring(WEB_PATH_PREFIX.length());
        Path target = uploadDirectory.resolve(storedFilename).normalize();
        if (!target.getParent().equals(uploadDirectory)) {
            return;
        }

        try {
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            log.warn("FamilyPost DB 저장 실패 후 이미지 파일을 삭제하지 못했습니다.");
        }
    }

    private void validate(MultipartFile image) {
        if (image == null || image.isEmpty()) {
            throw new IllegalArgumentException("이미지 파일이 필요합니다.");
        }

        log.info("originalFilename={}", image.getOriginalFilename());
        log.info("contentType={}", image.getContentType());
        log.info("size={}", image.getSize());

        if (image.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException(
                    "이미지 파일은 10MB 이하여야 합니다."
            );
        }
    }

    private String resolveExtension(MultipartFile image) {
        String contentType = normalizeContentType(image.getContentType());
        String mimeExtension = contentType == null
                ? null
                : ALLOWED_CONTENT_TYPES.get(contentType);
        if (mimeExtension != null) {
            return mimeExtension;
        }

        boolean unknownContentType = contentType == null
                || contentType.isBlank()
                || contentType.equals("application/octet-stream");
        if (!unknownContentType) {
            throw unsupportedImageType();
        }

        String originalFilename = image.getOriginalFilename();
        if (originalFilename == null) {
            throw unsupportedImageType();
        }

        int extensionSeparator = originalFilename.lastIndexOf('.');
        if (extensionSeparator < 0
                || extensionSeparator == originalFilename.length() - 1) {
            throw unsupportedImageType();
        }

        String originalExtension = originalFilename
                .substring(extensionSeparator + 1)
                .toLowerCase(Locale.ROOT);
        String normalizedExtension = ALLOWED_EXTENSIONS.get(originalExtension);
        if (normalizedExtension == null) {
            throw unsupportedImageType();
        }
        return normalizedExtension;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }
        int parameterSeparator = contentType.indexOf(';');
        String mediaType = parameterSeparator < 0
                ? contentType
                : contentType.substring(0, parameterSeparator);
        return mediaType.trim().toLowerCase(Locale.ROOT);
    }

    private IllegalArgumentException unsupportedImageType() {
        return new IllegalArgumentException(
                "JPEG, PNG, WEBP 이미지 파일만 업로드할 수 있습니다."
        );
    }
}
