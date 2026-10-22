package com.mp.karental.entity;


import com.mp.karental.constant.EBookingStatus;
import com.mp.karental.constant.EPaymentType;
import com.mp.karental.security.SecurityUtil;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents a booking in the database
 * <p>
 * This entity maps to a database table for storing booking information
 * </p>
 *
 * @author DieuTTH4
 * @version 1.0
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Slf4j
public class Booking {
    @Id
    @Column(name = "booking_number", unique = true, nullable = false)
    String bookingNumber;

    @ManyToOne
    @JoinColumn(name = "car_id")
    Car car;

    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Enumerated(EnumType.STRING)
    EBookingStatus status = EBookingStatus.PENDING_DEPOSIT;

    @Column(nullable = false)
    String pickUpLocation;

    @Column(nullable = false)
    LocalDateTime pickUpTime;

    @Column(nullable = false)
    LocalDateTime dropOffTime;

    @Column(name = "base_price", nullable = false)
    long basePrice;

    @Column(nullable = false)
    long deposit;

    @Enumerated(EnumType.STRING)
    EPaymentType paymentType;

    //=============================================================
    @Column(nullable = false)
    String driverFullName;

    @Column(nullable = false, columnDefinition = "VARCHAR(10)")
    String driverPhoneNumber;

    @Column(nullable = false, columnDefinition = "VARCHAR(12)")
    String driverNationalId;

    @Column(nullable = false)
    LocalDate driverDob;

    @Column(nullable = false)
    String driverEmail;

    @Column(nullable = false)
    String driverDrivingLicenseUri;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "driver_driving_license_asset_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    UploadAsset driverDrivingLicenseAsset;

    @Column(nullable = false)
    String driverCityProvince;

    @Column(nullable = false)
    String driverDistrict;

    @Column(nullable = false)
    String driverWard;

    @Column(nullable = false)
    String driverHouseNumberStreet;

    @Column(nullable = false)
    String updateBy;


    @PostPersist
    public void onPostPersist() {
        log.info("By: {} - Successfully created Booking with id: {}", SecurityUtil.getCurrentAccountId(), this.bookingNumber);
    }

    @PreUpdate
    public void onPreUpdate() {
        String accountId;
        try {
            accountId = SecurityUtil.getCurrentAccountId();
        } catch (Exception e) {
            accountId = "System";
        }
        log.info("By: {} - Updating Booking: {}", accountId, this);
    }

    @PostUpdate
    public void onPostUpdate() {
        String accountId;
        try {
            accountId = SecurityUtil.getCurrentAccountId();
        } catch (Exception e) {
            accountId = "System";
        }
        log.info("By: {} - Updated Booking: {}", accountId, this);    }
}
