package com.mp.karental.validation;

import com.mp.karental.validation.validator.ValidDriverDobValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Custom annotation to validate driver DOB conditionally based on isDriver flag.
 * Only validates when isDriver = true.
 */
@Documented
@Constraint(validatedBy = ValidDriverDobValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDriverDob {
    String message() default "INVALID_DATE_OF_BIRTH";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int min(); // Minimum age required
}

