package com.mp.karental.entity;

import com.mp.karental.constant.ECarStatus;
import com.mp.karental.security.SecurityUtil;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.CollectionType;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents a car entity in the system.
 * <p>
 * This entity maps to a database table for storing car information,
 * </p>
 *
 * @author QuangPM20
 * @version 1.0
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Slf4j
public class Car {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    @Column(name = "license_plate", nullable = false, unique = true)
    String licensePlate;

    @Column(nullable = false)
    String brand;

    @Column(nullable = false)
    String model;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    ECarStatus status;

    @Column(nullable = false)
    String color;

    @Column(name = "number_of_seats", nullable = false)
    int numberOfSeats;

    @Column(name = "production_year", nullable = false)
    int productionYear;

    @Column(nullable = false)
    float mileage;

    @Column(name = "fuel_consumption")
    float fuelConsumption;

    @Column(name = "base_price", nullable = false)
    long basePrice;

    @Column(nullable = false)
    long deposit;

    //address
    @Column(name = "city_province",nullable = false)
    String cityProvince;

    @Column(nullable = false)
    String district;

    @Column(nullable = false)
    String ward;

    @Column(name = "house_number_street",nullable = false)
    String houseNumberStreet;

    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "additional_function")
    String additionalFunction;

    @Column(name = "term_of_use")
    String termOfUse;

    @Column(nullable = false)
    boolean isAutomatic = true;
    @Column(nullable = false)
    boolean isGasoline = true;

    //documents
    @Column(name = "registration_paper_uri")
    String registrationPaperUri;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registration_paper_asset_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    UploadAsset registrationPaperAsset;
    @Column(nullable = false)
    boolean registrationPaperUriIsVerified = true;
    @Column(name = "certificate_of_inspection_uri")
    String certificateOfInspectionUri;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "certificate_of_inspection_asset_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    UploadAsset certificateOfInspectionAsset;
    @Column(nullable = false)
    boolean certificateOfInspectionUriIsVerified = true;
    @Column(name = "insurance_uri")
    String insuranceUri;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insurance_asset_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    UploadAsset insuranceAsset;
    @Column(nullable = false)
    boolean insuranceUriIsVerified = true;

    //car image
    @Column(name = "car_image_front")
    String carImageFront;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_image_front_asset_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    UploadAsset carImageFrontAsset;
    @Column(name = "car_image_back")
    String carImageBack;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_image_back_asset_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    UploadAsset carImageBackAsset;
    @Column(name = "car_image_left")
    String carImageLeft;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_image_left_asset_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    UploadAsset carImageLeftAsset;
    @Column(name = "car_image_right")
    String carImageRight;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "car_image_right_asset_id")
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    UploadAsset carImageRightAsset;

    @ManyToOne
    @JoinColumn(name = "account_id")
    Account account;

    @CreationTimestamp
    LocalDateTime createdAt;

    @UpdateTimestamp
    LocalDateTime updatedAt;

    @Column(nullable = false)
    String updateBy;

    @PostPersist
    public void onPostPersist() {
        log.info("By: {} - Successfully created Car with id: {}", SecurityUtil.getCurrentAccountId(), this.id);
    }

    @PreUpdate
    public void onPreUpdate() {
        log.info("By: {} - Updating Car: {}", SecurityUtil.getCurrentAccountId(), this);
    }

    @PostUpdate
    public void onPostUpdate() {
        log.info("By: {} - Updated Car: {}", SecurityUtil.getCurrentAccountId(), this);
    }

}
