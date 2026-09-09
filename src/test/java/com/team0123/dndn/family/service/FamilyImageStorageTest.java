package com.team0123.dndn.family.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FamilyImageStorageTest {

    @TempDir
    Path tempDirectory;

    @Test
    void storesJpegWithUuidFilenameAndRelativeWebPath() throws Exception {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);
        byte[] content = new byte[]{1, 2, 3};

        String imageUrl = storage.store(new MockMultipartFile(
                "image",
                "../../original-family-photo.jpeg",
                "image/jpeg",
                content
        ));

        assertTrue(imageUrl.matches(
                "^/uploads/family/[0-9a-f-]{36}\\.jpg$"
        ));
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        assertFalse(filename.contains("original-family-photo"));
        assertArrayEquals(content, Files.readAllBytes(tempDirectory.resolve(filename)));
    }

    @Test
    void storesPngWithSafeExtension() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);

        String imageUrl = storage.store(new MockMultipartFile(
                "image",
                "photo.png",
                "image/png",
                new byte[]{1}
        ));

        assertTrue(imageUrl.endsWith(".png"));
    }

    @Test
    void storesWebpFromKnownMimeType() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);

        String imageUrl = storage.store(new MockMultipartFile(
                "image",
                "photo.webp",
                "image/webp",
                new byte[]{1}
        ));

        assertTrue(imageUrl.endsWith(".webp"));
    }

    @Test
    void octetStreamJpegFallsBackToNormalizedJpgExtension() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);

        String imageUrl = storage.store(new MockMultipartFile(
                "image",
                "우사하나.JPEG",
                "application/octet-stream",
                new byte[]{1}
        ));

        assertTrue(imageUrl.matches(
                "^/uploads/family/[0-9a-f-]{36}\\.jpg$"
        ));
        assertFalse(imageUrl.contains("우사하나"));
    }

    @Test
    void octetStreamPngFallsBackToPngExtension() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);

        String imageUrl = storage.store(new MockMultipartFile(
                "image",
                "photo.PNG",
                "application/octet-stream",
                new byte[]{1}
        ));

        assertTrue(imageUrl.endsWith(".png"));
    }

    @Test
    void octetStreamWebpFallsBackToWebpExtension() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);

        String imageUrl = storage.store(new MockMultipartFile(
                "image",
                "photo.WeBp",
                "application/octet-stream",
                new byte[]{1}
        ));

        assertTrue(imageUrl.endsWith(".webp"));
    }

    @Test
    void nullContentTypeJpegFallsBackToJpgExtension() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);

        String imageUrl = storage.store(new MockMultipartFile(
                "image",
                "photo.jpeg",
                null,
                new byte[]{1}
        ));

        assertTrue(imageUrl.endsWith(".jpg"));
    }

    @Test
    void octetStreamExecutableIsRejected() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile(
                        "image",
                        "malware.exe",
                        "application/octet-stream",
                        new byte[]{1}
                ))
        );
    }

    @Test
    void explicitPdfMimeIsRejectedEvenWithJpgFilename() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile(
                        "image",
                        "document.jpg",
                        "application/pdf",
                        new byte[]{1}
                ))
        );
    }

    @Test
    void unknownMimeWithMissingOrInvalidExtensionIsRejected() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile(
                        "image",
                        "no-extension",
                        "application/octet-stream",
                        new byte[]{1}
                ))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile(
                        "image",
                        "photo.gif",
                        null,
                        new byte[]{1}
                ))
        );
    }

    @Test
    void rejectsEmptyFile() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile(
                        "image", "empty.jpg", "image/jpeg", new byte[0]
                ))
        );
    }

    @Test
    void rejectsNonImageMimeType() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile(
                        "image", "file.txt", "text/plain", new byte[]{1}
                ))
        );
    }

    @Test
    void rejectsFileLargerThanTenMegabytes() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);
        byte[] oversized = new byte[(int) FamilyImageStorage.MAX_FILE_SIZE + 1];

        assertThrows(
                IllegalArgumentException.class,
                () -> storage.store(new MockMultipartFile(
                        "image", "large.jpg", "image/jpeg", oversized
                ))
        );
    }

    @Test
    void storageFailureDoesNotReturnImageUrl() throws Exception {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);
        MultipartFile image = mock(MultipartFile.class);
        when(image.isEmpty()).thenReturn(false);
        when(image.getSize()).thenReturn(1L);
        when(image.getContentType()).thenReturn("image/jpeg");
        when(image.getInputStream()).thenThrow(new IOException("failure"));

        assertThrows(IllegalArgumentException.class, () -> storage.store(image));
    }

    @Test
    void deletesStoredFileAfterDatabaseFailure() {
        FamilyImageStorage storage = new FamilyImageStorage(tempDirectory);
        String imageUrl = storage.store(new MockMultipartFile(
                "image", "photo.webp", "image/webp", new byte[]{1}
        ));
        String filename = imageUrl.substring(imageUrl.lastIndexOf('/') + 1);
        Path storedFile = tempDirectory.resolve(filename);
        assertTrue(Files.exists(storedFile));

        storage.deleteIfExists(imageUrl);

        assertFalse(Files.exists(storedFile));
    }
}
