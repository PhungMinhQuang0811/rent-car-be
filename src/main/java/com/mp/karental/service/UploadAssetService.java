package com.mp.karental.service;

import com.mp.karental.constant.EUploadAssetStatus;
import com.mp.karental.constant.EUploadPurpose;
import com.mp.karental.constant.ERole;
import com.mp.karental.dto.response.file.UploadAssetResponse;
import com.mp.karental.entity.Account;
import com.mp.karental.entity.UploadAsset;
import com.mp.karental.exception.AppException;
import com.mp.karental.exception.ErrorCode;
import com.mp.karental.repository.AccountRepository;
import com.mp.karental.repository.UploadAssetRepository;
import com.mp.karental.security.SecurityUtil;
import com.mp.karental.validation.validator.UploadedFileSignature;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadAssetService {
    private final FileService fileService;
    private final UploadAssetRepository uploadAssetRepository;
    private final AccountRepository accountRepository;

    @Value("${application.upload.max-file-size-bytes:5242880}")
    private long maxFileSizeBytes;

    @Value("${application.upload.orphan-retention-hours:24}")
    private long orphanRetentionHours;

    @Value("${application.upload.cleanup-batch-size:100}")
    private int cleanupBatchSize;

    public UploadAssetResponse upload(MultipartFile file, EUploadPurpose purpose) {
        if (file == null || file.isEmpty() || file.getSize() > maxFileSizeBytes) {
            throw new AppException(ErrorCode.MAXIMUM_FILE_UPLOAD_EXCEED);
        }
        String originalFilename = file.getOriginalFilename();
        if (!StringUtils.hasText(originalFilename)) {
            originalFilename = "upload";
        }
        String originalName = StringUtils.cleanPath(originalFilename);
        String extension = extensionOf(originalName);
        boolean extensionAllowed = isAllowed(purpose, extension);
        if (!extensionAllowed) {
            throwInvalidFileError(purpose);
        }

        boolean signatureAllowed;
        if (isImageExtension(extension)) {
            signatureAllowed = UploadedFileSignature.isAllowedImage(file, extension);
        } else {
            signatureAllowed = UploadedFileSignature.isAllowedDocument(file, extension);
        }
        if (!signatureAllowed) {
            throwInvalidFileError(purpose);
        }

        String uploaderId = SecurityUtil.getCurrentAccountId();
        Account uploader = accountRepository.findById(uploaderId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND_IN_DB));
        if (uploader.getRole() == null || uploader.getRole().getName() == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        ERole role = uploader.getRole().getName();
        if (!isRoleAllowedForPurpose(role, purpose)) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        String namespace = namespaceFor(purpose);
        String resourceType = resourceTypeFor(extension);
        String generatedPublicId = namespace + "/" + uploaderId + "/" + UUID.randomUUID();
        if ("raw".equals(resourceType)) {
            generatedPublicId = generatedPublicId + "." + extension;
        }

        FileService.StoredFile stored;
        try {
            stored = fileService.uploadGeneratedFile(file, generatedPublicId, resourceType);
        } catch (IOException | RuntimeException e) {
            log.error("Upload provider failed for account {} and purpose {}", uploaderId, purpose, e);
            throw new AppException(ErrorCode.UPLOAD_OBJECT_TO_S3_FAIL);
        }

        UploadAsset asset = new UploadAsset();
        asset.setPublicId(stored.publicId());
        asset.setSecureUrl(stored.secureUrl());
        asset.setResourceType(stored.resourceType());
        asset.setOriginalFilename(originalName.substring(Math.max(0, originalName.lastIndexOf('/') + 1)));
        asset.setContentType(contentTypeFor(extension));
        asset.setSize(file.getSize());
        asset.setPurpose(purpose);
        asset.setStatus(EUploadAssetStatus.UPLOADED);
        asset.setUploader(uploader);
        try {
            asset = uploadAssetRepository.saveAndFlush(asset);
        } catch (RuntimeException e) {
            fileService.deleteFile(stored.publicId(), stored.resourceType());
            throw e;
        }

        return UploadAssetResponse.builder()
                .assetId(asset.getId())
                .purpose(asset.getPurpose())
                .build();
    }

    @Transactional
    public UploadAsset attach(String assetId, EUploadPurpose expectedPurpose,
                              String ownerEntityType, String ownerEntityId,
                              String expectedUploaderId, boolean allowOperatorUploader) {
        if (assetId == null || assetId.isBlank()) {
            throw new AppException(ErrorCode.UPLOAD_ASSET_NOT_FOUND);
        }
        UploadAsset asset = uploadAssetRepository.findByIdForUpdate(assetId)
                .orElseThrow(() -> new AppException(ErrorCode.UPLOAD_ASSET_NOT_FOUND));
        if (asset.getPurpose() != expectedPurpose) {
            throw new AppException(ErrorCode.UPLOAD_ASSET_PURPOSE_MISMATCH);
        }
        if (!asset.getUploader().getId().equals(expectedUploaderId) && !allowOperatorUploader) {
            throw new AppException(ErrorCode.UPLOAD_ASSET_NOT_OWNED);
        }
        if (asset.getStatus() != EUploadAssetStatus.UPLOADED) {
            throw new AppException(ErrorCode.UPLOAD_ASSET_ALREADY_ATTACHED);
        }
        asset.setStatus(EUploadAssetStatus.ATTACHED);
        asset.setOwnerEntityType(ownerEntityType);
        asset.setOwnerEntityId(ownerEntityId);
        asset.setAttachedAt(LocalDateTime.now());
        return asset;
    }

    @Transactional
    public void markForDeletion(UploadAsset asset) {
        if (asset != null && uploadAssetRepository.countDomainReferences(asset.getId()) == 0) {
            asset.setStatus(EUploadAssetStatus.DELETE_PENDING);
        }
    }

    @Transactional
    public void markExpiredUploadsForDeletion() {
        LocalDateTime cutoff = LocalDateTime.now().minusHours(orphanRetentionHours);
        var expired = uploadAssetRepository.findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
                EUploadAssetStatus.UPLOADED, cutoff, PageRequest.of(0, Math.max(1, cleanupBatchSize)));
        expired.forEach(asset -> asset.setStatus(EUploadAssetStatus.DELETE_PENDING));
    }

    public void cleanupPendingUploads() {
        var pending = uploadAssetRepository.findByStatusOrderByCreatedAtAsc(
                EUploadAssetStatus.DELETE_PENDING, PageRequest.of(0, Math.max(1, cleanupBatchSize)));
        // Delete staged objects only after their grace period has expired and they were marked pending.
        for (UploadAsset asset : pending) {
            if (fileService.deleteFile(asset.getPublicId(), asset.getResourceType())) {
                uploadAssetRepository.deleteById(asset.getId());
            }
        }
    }

    private static String extensionOf(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static boolean isImageExtension(String extension) {
        return Set.of("jpg", "jpeg", "png", "gif").contains(extension);
    }

    private static boolean isAllowed(EUploadPurpose purpose, String extension) {
        if (purpose == null) {
            return false;
        }
        if (purpose == EUploadPurpose.CAR_IMAGE) {
            return Set.of("jpg", "jpeg", "png", "gif").contains(extension);
        }
        return Set.of("doc", "docx", "pdf", "jpg", "jpeg", "png").contains(extension);
    }

    private static String namespaceFor(EUploadPurpose purpose) {
        if (purpose == EUploadPurpose.CAR_IMAGE) {
            return "car-images";
        } else if (purpose == EUploadPurpose.CAR_DOCUMENT) {
            return "car-documents";
        } else if (purpose == EUploadPurpose.BOOKING_DRIVER_LICENSE) {
            return "booking-driver-licenses";
        }
        return "profile-driver-licenses";
    }

    private static String contentTypeFor(String extension) {
        if ("jpg".equals(extension) || "jpeg".equals(extension)) {
            return "image/jpeg";
        } else if ("png".equals(extension)) {
            return "image/png";
        } else if ("gif".equals(extension)) {
            return "image/gif";
        } else if ("pdf".equals(extension)) {
            return "application/pdf";
        } else if ("doc".equals(extension)) {
            return "application/msword";
        } else if ("docx".equals(extension)) {
            return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        }
        return "application/octet-stream";
    }

    private static String resourceTypeFor(String extension) {
        if ("doc".equals(extension) || "docx".equals(extension)) {
            return "raw";
        }
        return "image";
    }

    private static boolean isRoleAllowedForPurpose(ERole role, EUploadPurpose purpose) {
        if ((purpose == EUploadPurpose.CAR_IMAGE || purpose == EUploadPurpose.CAR_DOCUMENT)
                && role == ERole.CAR_OWNER) {
            return true;
        }
        if (purpose == EUploadPurpose.BOOKING_DRIVER_LICENSE && role == ERole.CUSTOMER) {
            return true;
        }
        if (purpose == EUploadPurpose.PROFILE_DRIVER_LICENSE
                && (role == ERole.CUSTOMER || role == ERole.CAR_OWNER || role == ERole.OPERATOR)) {
            return true;
        }
        return false;
    }

    private static void throwInvalidFileError(EUploadPurpose purpose) {
        if (purpose == EUploadPurpose.CAR_IMAGE) {
            throw new AppException(ErrorCode.INVALID_CAR_IMAGE_FILE);
        }
        throw new AppException(ErrorCode.INVALID_DOCUMENT_FILE);
    }
}
