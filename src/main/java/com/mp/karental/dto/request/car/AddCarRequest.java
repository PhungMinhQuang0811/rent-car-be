package com.mp.karental.dto.request.car;

import com.mp.karental.validation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Represents the request payload for add a new car.
 * <p>
 * This class encapsulates the necessary data required to create a new car,
 * including car information.
 * </p>
 * @author QuangPM20
 *
 * @version 1.0
 */

@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@ValidBrandModel(message = "INVALID_BRAND_MODEL")
@Schema(name = "request.car.AddCarRequest", description = "DTO contain necessary information to add a car")
public class AddCarRequest {

    @RequiredField(fieldName = "License plate")
    @Pattern(regexp = "^(1[1-9]|[2-9][0-9])[A-Z]-\\d{3}\\.\\d{2}$", message = "INVALID_LICENSE")
    @UniqueLicensePlate(message = "NOT_UNIQUE_LICENSE")
    @Schema(example = "28F-125.13", pattern = "^(1[1-9]|[2-9][0-9])[A-Z]-\\d{3}\\.\\d{2}$", description = "The license plate number following the format (11-99)(A-Z)-(000-999).(00-99)")
    String licensePlate;

    @RequiredField(fieldName = "Brand of the car")
    @ValidBrand(message = "INVALID_BRAND")
    @Schema(example = "Toyota", description = "The brand of the car, which must be from a predefined list of valid brands")
    String brand;

    @RequiredField(fieldName = "Car model")
    @ValidModel(message = "INVALID_MODEL")
    @Schema(example = "Camry", description = "The model of the car, which must be from a predefined list of valid models for the selected brand")
    String model;

    @RequiredField(fieldName = "Color of the car")
    @ValidColor(message = "INVALID_COLOR")
    @Schema(example = "Black", description = "The color of the car, which must be from a predefined list of valid colors")
    String color;

    @ValidNumberOfSeats(message = "INVALID_NUMBER_OF_SEAT")
    @Schema(example = "4", description = "The number of seats in the car, allowed values are 4, 5, or 7")
    int numberOfSeats;

    @ValidProductionYear(message = "INVALID_PRODUCTION_YEAR")
    @Schema(example = "2000", description = "The production year of the car, must be between 1990 and 2030")
    int productionYear;

    @Min(value = 0, message = "INVALID_VALUE_MIN")
    @RequiredField(fieldName = "Mileage that the car has gone")
    @Schema(example = "15000", description = "The mileage of the car in kilometers")
    float mileage;

    @Min(value = 0, message = "INVALID_VALUE_MIN")
    @Schema(example = "7.5", description = "The fuel consumption of the car in liters per 100 kilometers")
    float fuelConsumption;

    @Min(value = 0, message = "INVALID_VALUE_MIN")
    @RequiredField(fieldName = "Car's base price")
    @Schema(example = "1000000", description = "The base rental price of the car per day")
    long basePrice;

    @Min(value = 0, message = "INVALID_VALUE_MIN")
    @RequiredField(fieldName = "Deposit value")
    @Schema(example = "500000", description = "The deposit required for renting the car")
    long deposit;

    @RequiredField(fieldName = "Address")
    @ValidAddress(message = "INVALID_ADDRESS")
    @Schema(example = "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng", description = "The address where the car is available for rent")
    String address;

    @Schema(example = "xe toyota camry", description = "A brief description of the car")
    String description;

    @ValidAdditionalFunction(message = "INVALID_ADDITIONAL_FUNTION")
    @Schema(example = "Sun Roof, Bluetooth", description = "Additional functions or features available in the car")
    String additionalFunction;

    @Schema(example = "No smoking", description = "Terms of use or rental restrictions for the car")
    String termOfUse;

    @RequiredField(fieldName = "Transmission type")
    @Schema(example = "true", description = "Indicates whether the car has an automatic transmission (true) or manual (false)")
    boolean isAutomatic;

    @RequiredField(fieldName = "Fuel type")
    @Schema(example = "true", description = "Indicates whether the car uses gasoline (true) or diesel (false)")
    boolean isGasoline;

    // Document files
    @NotBlank(message = "REQUIRED_FIELD")
    @RequiredField(fieldName = "Registration paper asset ID")
    String registrationPaperAssetId;

    @NotBlank(message = "REQUIRED_FIELD")
    @RequiredField(fieldName = "Certificate of inspection asset ID")
    String certificateOfInspectionAssetId;

    @NotBlank(message = "REQUIRED_FIELD")
    @RequiredField(fieldName = "Insurance asset ID")
    String insuranceAssetId;

    // Car images
    @NotBlank(message = "REQUIRED_FIELD")
    @RequiredField(fieldName = "Car's front side image asset ID")
    String carImageFrontAssetId;

    @NotBlank(message = "REQUIRED_FIELD")
    @RequiredField(fieldName = "Car's back side image asset ID")
    String carImageBackAssetId;

    @NotBlank(message = "REQUIRED_FIELD")
    @RequiredField(fieldName = "Car's left side image asset ID")
    String carImageLeftAssetId;

    @NotBlank(message = "REQUIRED_FIELD")
    @RequiredField(fieldName = "Car's right side image asset ID")
    String carImageRightAssetId;
}
