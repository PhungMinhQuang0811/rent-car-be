package com.mp.karental.entity;

import com.mp.karental.constant.EUploadAssetStatus;
import com.mp.karental.constant.EUploadPurpose;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "upload_asset", indexes = {
        @Index(name = "idx_upload_asset_status_created", columnList = "status, created_at"),
        @Index(name = "idx_upload_asset_uploader", columnList = "uploader_account_id")
})
@Getter
@Setter
@NoArgsConstructor
public class UploadAsset {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "public_id", nullable = false, unique = true, length = 512)
    private String publicId;

    @Column(name = "secure_url", nullable = false, columnDefinition = "TEXT")
    private String secureUrl;

    @Column(name = "resource_type", nullable = false, length = 16)
    private String resourceType;

    @Column(name = "original_filename", nullable = false, length = 255)
    private String originalFilename;

    @Column(name = "content_type", nullable = false, length = 128)
    private String contentType;

    @Column(nullable = false)
    private long size;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private EUploadPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private EUploadAssetStatus status = EUploadAssetStatus.UPLOADED;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "uploader_account_id", nullable = false)
    @ToString.Exclude
    private Account uploader;

    @Column(name = "owner_entity_type", length = 32)
    private String ownerEntityType;

    @Column(name = "owner_entity_id", length = 64)
    private String ownerEntityId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "attached_at")
    private LocalDateTime attachedAt;
}
