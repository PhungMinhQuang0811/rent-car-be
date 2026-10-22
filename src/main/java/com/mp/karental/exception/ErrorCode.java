package com.mp.karental.exception;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * Enumeration representing custom error codes for the application.
 * <p>
 * This centralized error handling mechanism allows for consistent error reporting across the application.
 * </p>
 *
 * @author DieuTTH4
 *
 * @version 1.0
 */
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public enum ErrorCode {
    //range 1xxx
    SUCCESS(1000, "Success", HttpStatus.OK),

    //range 2xxx
    REQUIRED_FIELD(2000, "{fieldName} is required.", HttpStatus.BAD_REQUEST),
    INVALID_NAME(2001, "The full name can only contain alphabet characters.", HttpStatus.BAD_REQUEST),
    INVALID_EMAIL(2002, "Please enter a valid email address.", HttpStatus.BAD_REQUEST),
    NOT_UNIQUE_EMAIL(2003, "Email already existed. Please try another email.", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_NUMBER(2004, "Invalid phone number.", HttpStatus.BAD_REQUEST),
    NOT_UNIQUE_PHONE_NUMBER(2005, "The phone number already existed. Please try another phone number.", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(2006, "Password must contain at least one number, one numeral, and seven characters.", HttpStatus.BAD_REQUEST),
    INVALID_LICENSE(2007, "Invalid license plate format! Expected format: (11-99)(A-Z)-(000-999).(00-99).", HttpStatus.BAD_REQUEST),
    NOT_UNIQUE_LICENSE(2008, "License plate already existed. Please try another license plate", HttpStatus.BAD_REQUEST),
    INVALID_COLOR(2009, "Your color were not predefined. Please try another color", HttpStatus.BAD_REQUEST),
    INVALID_ADDITIONAL_FUNTION(2010, "Your additional functions were not predefined. Please try another function", HttpStatus.BAD_REQUEST),
    INVALID_PRODUCTION_YEAR(2011, "Your production year was not predefined. Please try another year", HttpStatus.BAD_REQUEST),
    INVALID_NUMBER_OF_SEAT(2012, "Invalid number of seats. Allowed values are 4, 5, or 7.", HttpStatus.BAD_REQUEST),
    INVALID_BRAND(2013, "Your brand were not predefined. Please try another brand", HttpStatus.BAD_REQUEST),
    INVALID_MODEL(2014, "Your model were not predefined. Please try another model", HttpStatus.BAD_REQUEST),
    INVALID_BRAND_MODEL(2015, "Your brand-model were not matched. Please try again", HttpStatus.BAD_REQUEST),
    INVALID_DOCUMENT_FILE(2016,"Invalid file type. Accepted formats are .doc, .docx, .pdf, .jpeg, .jpg, .png", HttpStatus.BAD_REQUEST),
    INVALID_CAR_IMAGE_FILE(2017,"Invalid file type. Accepted formats are .jpg, .jpeg, .png, .gif", HttpStatus.BAD_REQUEST),
    INVALID_VALUE_MIN(2018,"This attribute must be >=0", HttpStatus.BAD_REQUEST),
    INVALID_ADDRESS(2019,"The address is invalid", HttpStatus.BAD_REQUEST),
    INVALID_DATE_OF_BIRTH(2020, "Date of birth must be at least 18 years old.", HttpStatus.BAD_REQUEST),
    INVALID_IMAGE_FILE(2021, "Invalid image file type. Accepted formats are .jpg, .jpeg, .png", HttpStatus.BAD_REQUEST),
    INVALID_NATIONAL_ID(2022, "National ID must contain exactly 12 digits.", HttpStatus.BAD_REQUEST),
    NOT_UNIQUE_NATIONAL_ID(2023, "The national id already existed. Please try another national id", HttpStatus.BAD_REQUEST),
    INVALID_CAR_STATUS_CHANGE(2024,"Allowed transitions: NOT_VERIFIED → STOPPED, STOPPED → NOT_VERIFIED, VERIFIED → STOPPED", HttpStatus.BAD_REQUEST),
    INVALID_DATE_RANGE(2025, "Invalid date range. Pick-up date must be before drop-off date.", HttpStatus.BAD_REQUEST),
    INVALID_BOOKING_TIME(2026,"Invalid booking time", HttpStatus.BAD_REQUEST),
    INVALID_PAYMENT_TYPE(2027,"Payment can only WALLET/CASH/BANK_TRANSFER", HttpStatus.BAD_REQUEST),
    INVALID_ADDRESS_COMPONENT(2028,"Invalid address component", HttpStatus.BAD_REQUEST),
    INVALID_DATE_FORMAT(2029, "Invalid date format. Please use yyyy-MM-dd'T'HH:mm:ss", HttpStatus.BAD_REQUEST),
    INVALID_TRANSACTION_TYPE(2030,"The transaction type is invalid", HttpStatus.BAD_REQUEST),
    INVALID_DRIVER_INFO(2031,"Driver's information is different from account holder, but the information is not fulfilled",HttpStatus.BAD_REQUEST),
    INVALID_COMMENT_LENGTH(2032,"The comment length is invalid", HttpStatus.BAD_REQUEST),
    INVALID_RATING_RANGE(2033, "The rating range is invalid", HttpStatus.BAD_REQUEST),
    INVALID_CAR_STATUS(2034, "The status must be NOT_VERIFIED", HttpStatus.BAD_REQUEST),

    //range 3xxx
    UPLOAD_OBJECT_TO_S3_FAIL(3001, "There was error occurred during uploading files. Please try again.", HttpStatus.SERVICE_UNAVAILABLE),
    ROLE_NOT_FOUND_IN_DB(3002, "The entity role requested is not found in the database.", HttpStatus.BAD_REQUEST),
    ACCOUNT_NOT_FOUND_IN_DB(3003, "The account is not exist in the system.", HttpStatus.BAD_REQUEST),
    MAXIMUM_FILE_UPLOAD_EXCEED(3004, "Maximum file upload exceeded. Each file should not exceed 5Mb", HttpStatus.PAYLOAD_TOO_LARGE),
    SEND_VERIFY_EMAIL_TO_USER_FAIL(3005, "There was error during sending verify email, please try again.", HttpStatus.SERVICE_UNAVAILABLE),
    INCORRECT_PASSWORD(3006, "Current password is incorrect.", HttpStatus.BAD_REQUEST),
    CAR_NOT_FOUND_IN_DB(3007, "The car is not exist in the system", HttpStatus.BAD_REQUEST),
    CAR_NOT_VERIFIED(3008, "This car has not been verified and cannot be viewed.", HttpStatus.FORBIDDEN),
    CAR_STOPPED(3009, "This car has stopped and cannot be viewed.", HttpStatus.FORBIDDEN),
    CAR_NOT_AVAILABLE(3010, "The car is not available", HttpStatus.BAD_REQUEST),

    VNPAY_SIGNING_FAILED(3011,"Your information is wrong. ", HttpStatus.BAD_REQUEST),
    VNPAY_CHECKSUM_FAILED(3012,"VNPAY Checksum sequence has error. ", HttpStatus.BAD_REQUEST),
    VNPAY_PAYMENT_FAILED(3013,"Payment failed by some reasons.", HttpStatus.BAD_REQUEST),
    PAYMENT_FAILED(3014,"Amount is exceeded wallet balance.", HttpStatus.BAD_REQUEST),
    WALLET_NOT_FOUND_IN_DB(3015, "The wallet is not exist in the system", HttpStatus.BAD_REQUEST),
    SEND_FORGOT_PASSWORD_EMAIL_TO_USER_FAIL(3016, "There was error during sending forgot password email, please try again.", HttpStatus.SERVICE_UNAVAILABLE),
    TRANSACTION_NOT_FOUND_IN_DB(3017, "The transaction is not exist in the system", HttpStatus.BAD_REQUEST),
    BOOKING_NOT_FOUND_IN_DB(3018, "The booking is not exist in the system", HttpStatus.BAD_REQUEST),
    BOOKING_CANNOT_BE_EDITED(3019, "The booking cannot be edited as it is already in its current status.", HttpStatus.BAD_REQUEST),
    INVALID_BOOKING_STATUS(3020, "The booking status does not allow this action.", HttpStatus.BAD_REQUEST),
    BOOKING_EXPIRED(3021, "This booking has expired and cannot be confirmed.", HttpStatus.BAD_REQUEST),

    EMAIL_NOT_USED_BY_ANY_ACCOUNT(3022, "The email address you’ve entered does not exist. Please try again.", HttpStatus.BAD_REQUEST),
    BOOKING_CANNOT_CANCEL(3023,"The booking cannot be cancelled as it is already in progress, pending payment, completed,waiting_confirmed_return_car or cancelled.", HttpStatus.BAD_REQUEST),
    BOOKING_CANNOT_PICKUP(3024,"The booking cannot be pickup when status not confirmed", HttpStatus.FORBIDDEN),
    SEND_CANCELLED_BOOKING_EMAIL_FAIL(3025,"There was error during sending cancelLed booking email to user.", HttpStatus.SERVICE_UNAVAILABLE),
    SEND_WAITING_CONFIRM_EMAIL_FAIL(3026,"There was error during sending waiting confirm email to user.", HttpStatus.SERVICE_UNAVAILABLE),
    SEND_WALLET_UPDATE_EMAIL_FAIL(3027,"There was error during sending wallet update email to user.", HttpStatus.SERVICE_UNAVAILABLE),
    SEND_CONFIRMED_BOOKING_EMAIL_FAIL(3028,"There was error during sending confirmed booking email to user.", HttpStatus.SERVICE_UNAVAILABLE),
    SEND_COMPLETED_BOOKING_EMAIL_FAIL(3029,"There was error during sending completed booking email to user.", HttpStatus.SERVICE_UNAVAILABLE),
    SEND_PENDING_PAYMENT_BOOKING_EMAIL_FAIL(3030,"There was error during sending pending payment booking email to user.", HttpStatus.SERVICE_UNAVAILABLE),
    FEEDBACK_TIME_EXPIRED(3031, "Feedback is only allowed within 30 days after drop-off.", HttpStatus.BAD_REQUEST),
    FEEDBACK_TOO_LONG(3032, "Feedback content must not exceed 2000 characters.", HttpStatus.BAD_REQUEST),
    BOOKING_NOT_COMPLETED(3033, "This booking is not COMPLETED.", HttpStatus.BAD_REQUEST),
    FEEDBACK_NOT_FOUND(3034, "Feedback not found for the given booking ID.", HttpStatus.NOT_FOUND),
    FEEDBACK_ALREADY_EXISTS(3035, "This booking is already exists.", HttpStatus.BAD_REQUEST),
    CAR_CANNOT_RETURN(3036,"The car cannot be return when booking status is not in-progress", HttpStatus.FORBIDDEN),
    CAR_CANNOT_STOPPED(3037,"The car cannot be stopped when has on-time booking", HttpStatus.FORBIDDEN),
    SEND_WAITING_CONFIRMED_RETURN_CAR_BOOKING_EMAIL_FAIL(3038,"There was error during sending waiting confirmed return car email to user", HttpStatus.SERVICE_UNAVAILABLE),
    SEND_REMINDER_PICK_UP_EMAIL_FAIL(3039,"There was error during sending reminder pick up email to user", HttpStatus.SERVICE_UNAVAILABLE),
    SEND_REMINDER_DROP_OFF_EMAIL_FAIL(3040,"There was error during sending reminder drop off email to user", HttpStatus.SERVICE_UNAVAILABLE),
    SEND_EARLY_RETURN_REJECTED_EMAIL_FAIL(3041, "There was error during sending reject early return car email to user", HttpStatus.SERVICE_UNAVAILABLE),
    EXCEL_DATA_LOAD_FAILED(3042, "Failed to load data from Excel file", HttpStatus.SERVICE_UNAVAILABLE),
    SEND_CAR_VERIFICATION_EMAIL_FAIL(3043,"There was error during sending verify car to user", HttpStatus.SERVICE_UNAVAILABLE),

    UNSUPPORTED_PAYMENT_TYPE(3044, "This payment type is not supported in this case", HttpStatus.BAD_REQUEST),
    INSUFFICIENT_BALANCE(3045,"Your wallet does not have enough balance", HttpStatus.BAD_REQUEST),
    UPLOAD_ASSET_NOT_FOUND(3046, "Uploaded asset was not found.", HttpStatus.BAD_REQUEST),
    UPLOAD_ASSET_ALREADY_ATTACHED(3047, "Uploaded asset is no longer available for attachment.", HttpStatus.CONFLICT),
    UPLOAD_ASSET_PURPOSE_MISMATCH(3048, "Uploaded asset cannot be used for this field.", HttpStatus.BAD_REQUEST),
    //range 4xxx
    UNCATEGORIZED_EXCEPTION(4000, "There was error happen during run time", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_ERROR_KEY(4001, "The error key could be misspelled", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_LOGIN_INFORMATION(4002, "Either email address or password is incorrect. Please try again.", HttpStatus.UNAUTHORIZED),
    UNAUTHENTICATED(4003, "Unauthenticated access. The access token is invalid", HttpStatus.UNAUTHORIZED), //401
    UNAUTHORIZED(4004, "User doesn't have permission to access the endpoint.", HttpStatus.FORBIDDEN), //403
//    ACCESS_TOKEN_EXPIRED(4005, "The access token is expired. Please try again", HttpStatus.UNAUTHORIZED),
    ACCOUNT_IS_INACTIVE(4006, "Your account is inactive.", HttpStatus.FORBIDDEN),
    REFRESH_TOKEN_EXPIRED(4008, "The refresh token is expired. Please login again.", HttpStatus.UNAUTHORIZED),

    INVALID_REFRESH_TOKEN(4009, "Invalid refresh token. Please try again.", HttpStatus.UNAUTHORIZED),
    FORBIDDEN_CAR_ACCESS(4010, "Can not view detail/edit car of another account", HttpStatus.FORBIDDEN),
    INVALID_ONETIME_TOKEN(4011, "The token is invalid or this link has expired or has been used.", HttpStatus.BAD_REQUEST),
    INVALID_FORGOT_PASSWORD_TOKEN(4012, "This link has expired. Please go back to Homepage and try again.", HttpStatus.BAD_REQUEST),
    FORBIDDEN_PROFILE_INCOMPLETE(4013, "Please complete your individual profile to booking", HttpStatus.FORBIDDEN),
    FORBIDDEN_BOOKING_ACCESS(4014, "Can not view detail/edit booking of another account", HttpStatus.FORBIDDEN),
    INVALID_CSRF_TOKEN(4015, "Invalid or missing CSRF token. Access denied.", HttpStatus.FORBIDDEN),
    UPLOAD_ASSET_NOT_OWNED(4016, "Uploaded asset does not belong to this account.", HttpStatus.FORBIDDEN),

    ;

    /**
     * A unique numeric identifier for the error.
     */
    int code;
    /**
     * A human-readable message describing the error.
     */
    String message;
    /**
     * The corresponding HTTP status to be returned when the error occurs.
     */
    HttpStatusCode httpStatusCode;

}
