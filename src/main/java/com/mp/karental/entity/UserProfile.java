package com.mp.karental.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mp.karental.security.SecurityUtil;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents a user profile entity in the system.
 * <p>
 * This entity maps to a database table for storing user information,
 * including full name, date of birth, national id, phone number, address, driving licence and associated user account.
 * </p>
 *
 * @author DieuTTH4
 *
 * @version 1.0
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Slf4j
public class UserProfile {
    @Id
    String id;

    @OneToOne
    @JoinColumn(name = "id")
    @MapsId
    @JsonIgnore
    @ToString.Exclude
    Account account;

    String fullName;
    LocalDate dob;

    @Column(unique = true, columnDefinition = "VARCHAR(12)")
    String nationalId;

    @Column(unique = true, nullable = false, columnDefinition = "VARCHAR(10)")
    String phoneNumber;

    String cityProvince;
    String district;
    String ward;
    String houseNumberStreet;

    @Column(unique = true)
    String drivingLicenseUri;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driving_license_asset_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    UploadAsset drivingLicenseAsset;

    @PostPersist
    public void onPostPersist() {
        String accountId;
        try {
            accountId = SecurityUtil.getCurrentAccountId();
        } catch (Exception e) {
            accountId = this.id;
        }
        log.info("By: {} - Successfully created UserProfile with id: {}", accountId, this.id);
    }

    @PreUpdate
    public void onPreUpdate() {
        log.info("By: {} - Updating UserProfile: {}", SecurityUtil.getCurrentAccountId(), this);
    }

    @PostUpdate
    public void onPostUpdate() {
        log.info("By: {} - Updated UserProfile: {}", SecurityUtil.getCurrentAccountId(), this);
    }
}
