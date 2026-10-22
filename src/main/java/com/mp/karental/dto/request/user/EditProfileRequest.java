package com.mp.karental.dto.request.user;

import com.mp.karental.validation.*;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;

/**
 * Represents the request to edit profile
 *
 * @author AnhHP9
 *
 * @version 1.0
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class EditProfileRequest {

    @RequiredField(fieldName = "Full name")
    @Pattern(regexp = "^[\\p{L}\\s-]+$", message = "INVALID_NAME")
    String fullName;

    @RequiredField(fieldName = "Date of birth")
    @ValidAge(min = 18, message = "INVALID_DATE_OF_BIRTH")
    LocalDate dob;


    @NotBlank(message = "REQUIRED_FIELD")
    @RequiredField(fieldName = "Phone number")
    @Pattern(regexp = "^0[\\d]{9}$", message = "INVALID_PHONE_NUMBER")
    String phoneNumber;

    @RequiredField(fieldName = "National ID")
    @Pattern(regexp = "^[0-9]{12}$", message = "INVALID_NATIONAL_ID")
    @NotBlank(message = "REQUIRED_FIELD")
    String nationalId;

    String cityProvince;
    String district;
    String ward;
    String houseNumberStreet;

    String drivingLicenseAssetId;

    @RequiredField(fieldName = "Email")
    @Email(message = "INVALID_EMAIL")
    String email;

}
