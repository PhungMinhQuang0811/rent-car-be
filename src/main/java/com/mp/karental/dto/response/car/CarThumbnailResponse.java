package com.mp.karental.dto.response.car;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;

/**
 * DTO (Data Transfer Object) representing a summary of a car.
 * Used to display a list of cars in the car rental system.
 *
 * @author AnhHP9
 *
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Schema(name = "response.car.CarThumbnailResponse",description = "Summary response containing essential car details for listing.")
public class CarThumbnailResponse {

    @Schema(description = "Unique identifier of the car", example = "123e4567-e89b-12d3-a456-426614174000")
    private String id;

    @Schema(description = "Brand of the car", example = "Toyota")
    private String brand;

    @Schema(description = "Model of the car", example = "Camry")
    private String model;

    @Schema(description = "Year the car was manufactured", example = "2020")
    private int productionYear;

    @Schema(description = "Current status of the car", example = "AVAILABLE")
    private String status;

    @Schema(description = "Current mileage of the car in kilometers", example = "15000.5")
    private float mileage;

    @Schema(description = "Base rental price of the car per day", example = "500000")
    private long basePrice;

    @Schema(description = "Address where the car is available for pickup", example = "123 Main St, Hanoi, Vietnam")
    private String address;

    @Schema(description = "URL of the car's front-side image", example = "https://example.com/car_front.jpg")
    private String carImageFront;

    @Schema(description = "URL of the car's right-side image", example = "https://example.com/car_right.jpg")
    private String carImageRight;

    @Schema(description = "URL of the car's left-side image", example = "https://example.com/car_left.jpg")
    private String carImageLeft;

    @Schema(description = "URL of the car's back-side image", example = "https://example.com/car_back.jpg")
    private String carImageBack;

    @Schema(description = "Total number of rides the car has been booked for", example = "25")
    long noOfRides;

    @Schema(description = "Average rating of the car based on customer reviews", example = "4.5")
    private double averageRatingByCar;

    @Schema(description = "Last updated timestamp of the car details", example = "2024-03-28T15:30:00")
    private LocalDateTime updatedAt;

    @Schema(description = "Number of seats in the car", example = "5")
    private int numberOfSeats;

    @Schema(description = "Whether the car has automatic transmission", example = "true")
    private boolean isAutomatic;

    @Schema(description = "Whether the car uses gasoline fuel", example = "true")
    private boolean isGasoline;
}