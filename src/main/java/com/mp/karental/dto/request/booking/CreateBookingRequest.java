package com.mp.karental.dto.request.booking;

import com.mp.karental.constant.EPaymentType;
import com.mp.karental.validation.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Represents the request payload for booking.
 * <p>
 * This class encapsulates the necessary data required to create a booking,
 * including booking information.
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
@ValidBookingTime(message = "INVALID_BOOKING_TIME")
@ValidAddressComponent(message = "INVALID_ADDRESS_COMPONENT")
@ValidDriverDob(min = 18, message = "INVALID_DATE_OF_BIRTH")
@Schema(name = "request.booking.CreateBookingRequest", description = "DTO contain necessary information to make a car")
public class CreateBookingRequest {
    @RequiredField(fieldName = "Car id")
    @Schema(example = "car1", description = "The id of the car user wants to rent")
    String carId;

    @CreationTimestamp
    @Schema(example = "2004-11-08T07:00:00", description = "The timestamp when the booking was created")
    LocalDateTime createdAt;

    // Note: pickUpLocation validation is skipped - backend constructs it from car entity
    // @ValidAddress annotation removed since backend has access to car's full address
    @Schema(example = "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng", description = "The pickup location for the car rental (constructed from car entity on backend)")
    String pickUpLocation;

    @Schema(example = "2004-11-08T09:00:00", description = "The pickup time for the rental car")
    LocalDateTime pickUpTime;

    @Schema(example = "2004-11-08T18:00:00", description = "The drop-off time for the rental car")
    LocalDateTime dropOffTime;

    @ValidPaymentType
    @Schema(description = "The payment type for the booking", example = "WALLET")
    EPaymentType paymentType;

    //driver
    //=============================================================
    @Schema(example = "John Doe", description = "The full name of the driver")
    String driverFullName;

    @Schema(example = "0886980035", description = "The phone number of the driver")
    String driverPhoneNumber;

    @Schema(example = "A123456789", description = "The national ID of the driver")
    String driverNationalId;

    // Note: driverDob validation is conditional - only validated when isDriver = true
    // When isDriver = false, backend uses account profile DOB
    @Schema(example = "2004-11-08", description = "The date of birth of the driver (must be at least 18 years old, required only when isDriver = true)")
    LocalDate driverDob;

    @Email(message = "INVALID_EMAIL")
    @Schema(example = "john.doe@example.com", description = "The email address of the driver")
    String driverEmail;

    @ValidDocument(message = "INVALID_DOCUMENT_FILE")
    @Schema(type = "string", format = "binary", description = "Driver driving license file (.doc, .docx, .pdf, .jpeg, .jpg, .png)")
    MultipartFile driverDrivingLicense;

    @Schema(example = "Tỉnh Hà Giang", description = "The city or province of the driver")
    String driverCityProvince;

    @Schema(example = "Thành phố Hà Giang", description = "The district of the driver")
    String driverDistrict;

    @Schema(example = "Phường Quang Trung", description = "The ward of the driver")
    String driverWard;

    @Schema(example = "211 Trần Duy Hưng", description = "The house number and street of the driver")
    String driverHouseNumberStreet;

    // is the driver of the booking is different from the renter(true) or renter is driver of this booking (false)
    @Schema(example = "true", description = "Indicates if the driver is different from the renter (true) or the renter is also the driver (false)")
    boolean isDriver;
}
