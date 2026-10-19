package com.mp.karental.validation.validator;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

final class UploadedFileSignature {
    private UploadedFileSignature() {}

    static boolean isAllowedImage(MultipartFile file, String extension) {
        try {
            byte[] bytes = file.getBytes();
            return switch (extension.toLowerCase(Locale.ROOT)) {
                case "jpg", "jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
                case "png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
                case "gif" -> startsWithAscii(bytes, "GIF87a") || startsWithAscii(bytes, "GIF89a");
                default -> false;
            };
        } catch (IOException e) {
            return false;
        }
    }

    static boolean isAllowedDocument(MultipartFile file, String extension) {
        try {
            byte[] bytes = file.getBytes();
            return switch (extension.toLowerCase(Locale.ROOT)) {
                case "jpg", "jpeg" -> startsWith(bytes, 0xFF, 0xD8, 0xFF);
                case "png" -> startsWith(bytes, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A);
                case "pdf" -> startsWithAscii(bytes, "%PDF-");
                case "doc" -> startsWith(bytes, 0xD0, 0xCF, 0x11, 0xE0, 0xA1, 0xB1, 0x1A, 0xE1);
                case "docx" -> startsWith(bytes, 0x50, 0x4B, 0x03, 0x04);
                default -> false;
            };
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean startsWith(byte[] bytes, int... signature) {
        if (bytes.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((bytes[i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }

    private static boolean startsWithAscii(byte[] bytes, String signature) {
        byte[] expected = signature.getBytes(StandardCharsets.US_ASCII);
        if (bytes.length < expected.length) return false;
        for (int i = 0; i < expected.length; i++) {
            if (bytes[i] != expected[i]) return false;
        }
        return true;
    }
}
