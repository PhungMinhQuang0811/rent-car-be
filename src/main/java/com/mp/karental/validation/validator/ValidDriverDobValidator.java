package com.mp.karental.validation.validator;

import com.mp.karental.dto.request.booking.CreateBookingRequest;
import com.mp.karental.validation.ValidDriverDob;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;

/**
 * Validator for driver DOB that only validates when isDriver = true.
 * When isDriver = false, the backend uses the renter's account profile DOB.
 */
public class ValidDriverDobValidator implements ConstraintValidator<ValidDriverDob, CreateBookingRequest> {
    private int minAge;

    @Override
    public void initialize(ValidDriverDob constraintAnnotation) {
        this.minAge = constraintAnnotation.min();
    }

    @Override
    public boolean isValid(CreateBookingRequest request, ConstraintValidatorContext context) {
        if (request == null) {
            return true;
        }

        // Only validate driverDob when isDriver = true
        // When isDriver = false, backend uses account profile DOB
        if (!request.isDriver()) {
            return true; // Skip validation - backend will use account profile
        }

        // When isDriver = true, driverDob must be provided and valid
        LocalDate driverDob = request.getDriverDob();
        if (driverDob == null) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("INVALID_DATE_OF_BIRTH")
                    .addPropertyNode("driverDob")
                    .addConstraintViolation();
            return false;
        }

        // Check if age is at least minAge
        int age = Period.between(driverDob, LocalDate.now()).getYears();
        if (age < minAge) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate("INVALID_DATE_OF_BIRTH")
                    .addPropertyNode("driverDob")
                    .addConstraintViolation();
            return false;
        }

        return true;
    }
}

