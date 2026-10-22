package com.mp.karental.repository;

import com.mp.karental.constant.EUploadAssetStatus;
import com.mp.karental.entity.UploadAsset;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UploadAssetRepository extends JpaRepository<UploadAsset, String> {
    @Query(value = "select (select count(*) from car where registration_paper_asset_id = :id) + " +
            "(select count(*) from car where certificate_of_inspection_asset_id = :id) + " +
            "(select count(*) from car where insurance_asset_id = :id) + " +
            "(select count(*) from car where car_image_front_asset_id = :id) + " +
            "(select count(*) from car where car_image_back_asset_id = :id) + " +
            "(select count(*) from car where car_image_left_asset_id = :id) + " +
            "(select count(*) from car where car_image_right_asset_id = :id) + " +
            "(select count(*) from booking where driver_driving_license_asset_id = :id) + " +
            "(select count(*) from user_profile where driving_license_asset_id = :id)", nativeQuery = true)
    long countDomainReferences(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select asset from UploadAsset asset where asset.id = :id")
    Optional<UploadAsset> findByIdForUpdate(@Param("id") String id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<UploadAsset> findByStatusAndCreatedAtBeforeOrderByCreatedAtAsc(
            EUploadAssetStatus status, LocalDateTime cutoff, Pageable pageable);

    List<UploadAsset> findByStatusOrderByCreatedAtAsc(EUploadAssetStatus status, Pageable pageable);
}
