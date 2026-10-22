package com.mp.karental.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.mp.karental.entity.UploadAsset;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

/**
 * Cloudinary storage adapter for staged asset uploads, deletion, and URL generation.
 *
 * @author DieuTTH4
 *
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class FileService {
    Cloudinary cloudinary;

    public record StoredFile(String publicId, String secureUrl, String resourceType) {}

    @NonFinal
    @Value("${cloudinary.folder.name:karental}")
    String folderName;

    public StoredFile uploadGeneratedFile(MultipartFile file, String publicId, String resourceType) throws IOException {
        Map uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "public_id", publicId,
                        "folder", folderName,
                        "overwrite", false,
                        "resource_type", resourceType,
                        "unique_filename", false
                )
        );
        return new StoredFile(
                String.valueOf(uploadResult.get("public_id")),
                String.valueOf(uploadResult.get("secure_url")),
                String.valueOf(uploadResult.get("resource_type"))
        );
    }

    public boolean deleteFile(String publicId, String resourceType) {
        try {
            Map result = cloudinary.uploader().destroy(publicId,
                    ObjectUtils.asMap("resource_type", resourceType, "invalidate", true));
            return "ok".equals(result.get("result")) || "not found".equals(result.get("result"));
        } catch (Exception e) {
            log.error("Cloudinary delete failed for public id {}", publicId, e);
            return false;
        }
    }

    public String getFileUrl(String publicId) {
        if (publicId == null || publicId.isEmpty()) {
            return null;
        }

        if (publicId.startsWith("https://") || publicId.startsWith("http://")) {
            return publicId;
        }
        String lower = publicId.toLowerCase();
        String resourceType = "image";
        if (lower.endsWith(".doc") || lower.endsWith(".docx")) {
            resourceType = "raw";
        }
        return cloudinary.url()
                .secure(true)
                .resourceType(resourceType)
                .generate(folderName + "/" + publicId);
    }

    public String getFileUrl(UploadAsset asset, String legacyPublicId) {
        if (asset != null && asset.getSecureUrl() != null && !asset.getSecureUrl().isBlank()) {
            return asset.getSecureUrl();
        }
        return getFileUrl(legacyPublicId);
    }
}
