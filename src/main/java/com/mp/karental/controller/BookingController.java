package com.mp.karental.controller;

import com.mp.karental.dto.request.booking.CreateBookingRequest;
import com.mp.karental.dto.request.booking.EditBookingRequest;
import com.mp.karental.constant.EBookingStatus;
import com.mp.karental.dto.response.*;
import com.mp.karental.dto.response.booking.BookingResponse;
import com.mp.karental.dto.response.booking.WalletResponse;
import com.mp.karental.dto.response.user.UserResponse;
import com.mp.karental.service.BookingService;
import com.mp.karental.dto.response.booking.BookingListResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(value = "/booking", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@Validated
@Tag(name = "Booking", description = "API for managing booking")
public class BookingController {

    BookingService bookingService;  // Service layer dependency for handling business logic

    @Operation(
            summary = "Create a booking",
            description = "This api allows customers to make a booking to rent a car ",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class)
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3007 | The car is not exist in the system.|
                                    | 3003 | The account is not exist in the system.|
                                    | 3010 | The car is not available.|
                                    | 2031 | Driver's information is different from account holder, but the information is not fulfilled.|
                                    | 2004 | Invalid phone number.|
                                    | 2022 | National ID must contain exactly 12 digits.|
                                    | 2026 | Invalid booking time.|
                                    | 2028 | Invalid address component.|
                                    | 2002 | Please enter a valid email address.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3001 | There was error occurred during uploading files. Please try again.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3025 | There was error during sending cancelLed booking email to user.|
                                    | 3026 | There was error during sending waiting confirm email to user.|
                                    """
                    ),
            }
    )
    @PostMapping(value = "/customer/create-book", consumes = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<BookingResponse> createBooking(@RequestBody @Valid CreateBookingRequest bookingRequest) {
        log.info("create booking {}", bookingRequest);
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.createBooking(bookingRequest))
                .build();
    }

    @Operation(
            summary = "Edit a booking",
            description = "This api allows customers to view/edit the booking details after renting a car",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class)
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3019 | The booking cannot be edited as it is already in its current status.|
                                    | 2031 | Driver's information is different from account holder, but the information is not fulfilled.|
                                    | 2004 | Invalid phone number.|
                                    | 2022 | National ID must contain exactly 12 digits.|
                                    | 2002 | Please enter a valid email address.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4014 | Can not view detail/edit booking of another account.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3001 | There was error occurred during uploading files. Please try again.|
                                    """
                    ),
            }
    )
    @PutMapping(value = "/customer/edit-book/{bookingNumber}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ApiResponse<BookingResponse> editBooking(@RequestBody @Valid EditBookingRequest editBookingRequest, @PathVariable @Parameter(description = "The booking number to be edited", example = "BK202410200001") String bookingNumber)  {
        log.info("edit booking {}", editBookingRequest);
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.editBooking(editBookingRequest, bookingNumber))
                .build();
    }

    /**
     * API endpoint to retrieve the list of bookings for the current customer.
     *
     * @param page the page number (default is 0)
     * @param size the number of records per page (default is 10)
     * @param sort sorting field and direction in the format "field,DIRECTION" (default is "createdAt,DESC")
     * @return a paginated list of bookings wrapped in `ApiResponse<Page<BookingThumbnailResponse>>`
     */
    @Operation(
            summary = "View list bookings",
            description = "Customer view their list of booking.",
            parameters = {
                    @Parameter(name = "page", description = "Page number (default = 0)", example = "0"),
                    @Parameter(name = "size", description = "Number of records per page (default = 10)", example = "10"),
                    @Parameter(name = "status", description = "Booking status filter", schema = @Schema(implementation = EBookingStatus.class)),
                    @Parameter(name = "sort", description = "Sorting field and direction (default = 'updatedAt,DESC')", example = "updatedAt,DESC")
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Successful!")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingListResponse.class)
                                            )
                                    }
                            )
                    )
            }
    )
    @GetMapping("/customer/my-bookings")
    public ApiResponse<BookingListResponse> getBookings(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) EBookingStatus status,
            @RequestParam(defaultValue = "updatedAt,DESC") String sort) {

        BookingListResponse response = bookingService.getBookingsOfCustomer(page, size, sort,
                (status != null) ? status.name() : null);

        return ApiResponse.<BookingListResponse>builder()
                .data(response)
                .build();
    }


    @Operation(
            summary = "Get all booking",
            description = "This api allows operators to view all the bookings in the system",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingListResponse.class)
                                            )
                                    }
                            )
                    ),
            }
    )
    @GetMapping("/operator/all-bookings")
    public ApiResponse<BookingListResponse> getBookingsForOperator(
            @Parameter(description = "Page number for pagination", example = "0")
            @RequestParam(defaultValue = "0") int page,

            @Parameter(description = "Size of the page", example = "10")
            @RequestParam(defaultValue = "10") int size,

            @Parameter(description = "Filter by booking status", example = "CONFIRMED")
            @RequestParam(required = false) EBookingStatus status,

            @Parameter(description = "Sorting criteria in format: field,order", example = "updatedAt,DESC")
            @RequestParam(defaultValue = "updatedAt,DESC") String sort
    ) {

        BookingListResponse response = bookingService.getBookingsOfOperator(page, size, sort,
                (status != null) ? status.name() : null);

        return ApiResponse.<BookingListResponse>builder()
                .data(response)
                .build();
    }

    /**
     * API endpoint to retrieve the list of bookings for the current customer.
     *
     * @param page the page number (default is 0)
     * @param size the number of records per page (default is 10)
     * @param sort sorting field and direction in the format "field,DIRECTION" (default is "updatedAt,DESC")
     * @return a paginated list of bookings wrapped in `ApiResponse<Page<BookingThumbnailResponse>>`
     */
    @Operation(
            summary = "View list rentals",
            description = "Car owner can view their list of rentals",
            parameters = {
                    @Parameter(name = "page", description = "Page number (default = 0)", example = "0"),
                    @Parameter(name = "size", description = "Number of records per page (default = 10)", example = "10"),
                    @Parameter(name = "status", description = "Booking status filter", schema = @Schema(implementation = EBookingStatus.class)),
                    @Parameter(name = "sort", description = "Sorting field and direction (default = 'updatedAt,DESC')", example = "updatedAt,DESC")
            },
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Successful!")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingListResponse.class)
                                            )
                                    }
                            )
                    )
            }
    )
    @GetMapping("/car-owner/rentals")
    public ApiResponse<BookingListResponse> getBookingsForCarOwner(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) EBookingStatus status,
            @RequestParam(defaultValue = "updatedAt,DESC") String sort) {

        BookingListResponse response = bookingService.getBookingsOfCarOwner(page, size, sort,
                (status != null) ? status.name() : null);

        return ApiResponse.<BookingListResponse>builder()
                .data(response)
                .build();
    }

    @Operation(
            summary = "Get balance of wallet",
            description = "This api allows customers to view the balance of their wallet",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = WalletResponse.class)
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3003 | The account is not exist in the system.|
                                    """
                    ),
            }
    )
    @GetMapping("/get-wallet")
    public ApiResponse<WalletResponse> getWallet() {
        WalletResponse wallet = bookingService.getWallet();
        return ApiResponse.<WalletResponse>builder()
                .data(wallet)
                .build();
    }
    @Operation(
            summary = "Get a booking for customer",
            description = "This api allows customers to view the booking details by booking number",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class)
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4014 | Can not view detail/edit booking of another account.|
                                    | 4004 | User doesn't have permission to access the endpoint.|
                                    """
                    ),
            }
    )
    @GetMapping("/customer/{bookingNumber}")
    public ApiResponse<BookingResponse> getBookingByBookingNumber(@PathVariable @Parameter(description = "The booking number to be view", example = "BK202410200001") String bookingNumber) {
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.getBookingDetailsByBookingNumber(bookingNumber))
                .build();
    }


    @Operation(
            summary = "Get a booking for car owner",
            description = "This api allows car owner to view the booking details by booking number",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class)
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4010 | Can not view detail/edit car of another account.|
                                    | 4004 | User doesn't have permission to access the endpoint.|
                                    """
                    ),
            }
    )
    @GetMapping("/car-owner/{bookingNumber}")
    public ApiResponse<BookingResponse> getCustomerBookingDetails(@PathVariable @Parameter(description = "The booking number to be view", example = "BK202410200001") String bookingNumber) {
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.getBookingDetailsByBookingNumber(bookingNumber))
                .build();
    }

    @Operation(
            summary = "Confirm the booking",
            description = "This api allows car owner to confirm the booking for customer",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class,
                                                            example = """
                                            {
                                                "bookingNumber": "BK202410200001",
                                                "carId": "car1",
                                                "status": "CONFIRMED",
                                                "pickUpLocation": "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng",
                                                "pickUpTime": "2004-11-08T09:00:00",
                                                "dropOffTime": "2004-11-08T18:00:00",
                                                "totalPrice": 50000,
                                                "basePrice": 50000,
                                                "deposit": 10000,
                                                "paymentType": "WALLET",
                                                "driverFullName": "John Doe",
                                                "driverPhoneNumber": "0886980035",
                                                "driverNationalId": "A123456789",
                                                "driverDob": "2004-11-08",
                                                "driverEmail": "john.doe@example.com",
                                                "driverDrivingLicenseUrl": "booking/123456/driver-driving-license.jpg",
                                                "driverCityProvince": "Tỉnh Hà Giang",
                                                "driverDistrict": "Thành phố Hà Giang",
                                                "driverWard": "Phường Quang Trung",
                                                "driverHouseNumberStreet": "211 Trần Duy Hưng",
                                                "isDriver": true
                                            }
                                            """
                                                    )
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3020 | The booking status does not allow this action.|
                                    | 3021 | This booking has expired and cannot be confirmed.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4010 | Can not view detail/edit car of another account.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3028 | There was error during sending confirmed booking email to user.|
                                    """
                    ),
            }
    )
    @PutMapping("/car-owner/{bookingNumber}/confirm")
    public ApiResponse<BookingResponse> confirmBooking(@PathVariable @Parameter(description = "The booking number to be confirm", example = "BK202410200001") String bookingNumber) {
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.confirmBooking(bookingNumber))
                .build();
    }

    @Operation(
            summary = "Cancel the booking",
            description = "This api allows customer to cancel the booking",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class,
                                                            example = """
                                            {
                                                "bookingNumber": "BK202410200001",
                                                "carId": "car1",
                                                "status": "CANCELLED",
                                                "pickUpLocation": "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng",
                                                "pickUpTime": "2004-11-08T09:00:00",
                                                "dropOffTime": "2004-11-08T18:00:00",
                                                "totalPrice": 50000,
                                                "basePrice": 50000,
                                                "deposit": 10000,
                                                "paymentType": "WALLET",
                                                "driverFullName": "John Doe",
                                                "driverPhoneNumber": "0886980035",
                                                "driverNationalId": "A123456789",
                                                "driverDob": "2004-11-08",
                                                "driverEmail": "john.doe@example.com",
                                                "driverDrivingLicenseUrl": "booking/123456/driver-driving-license.jpg",
                                                "driverCityProvince": "Tỉnh Hà Giang",
                                                "driverDistrict": "Thành phố Hà Giang",
                                                "driverWard": "Phường Quang Trung",
                                                "driverHouseNumberStreet": "211 Trần Duy Hưng",
                                                "isDriver": true
                                            }
                                            """
                                                    )
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3023 | The booking cannot be cancelled as it is already in progress, pending payment, completed,waiting_confirmed_return_car or cancelled.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4014 | Can not view detail/edit booking of another account.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3025 | There was error during sending cancelLed booking email to user.|
                                    """
                    ),
            }
    )
    @PutMapping("/customer/cancel-booking/{bookingNumber}")
    public ApiResponse<BookingResponse> cancelBooking(@PathVariable @Parameter(description = "The booking number to be cancel", example = "BK202410200001") String bookingNumber)  {
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.cancelBooking(bookingNumber))
                .build();
    }

    @Operation(
            summary = "Confirm pick up the booking",
            description = "This api allows customer to confirm pick up the booking",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class,
                                                            example = """
                                            {
                                                "bookingNumber": "BK202410200001",
                                                "carId": "car1",
                                                "status": "IN_PROGRESS",
                                                "pickUpLocation": "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng",
                                                "pickUpTime": "2004-11-08T09:00:00",
                                                "dropOffTime": "2004-11-08T18:00:00",
                                                "totalPrice": 50000,
                                                "basePrice": 50000,
                                                "deposit": 10000,
                                                "paymentType": "WALLET",
                                                "driverFullName": "John Doe",
                                                "driverPhoneNumber": "0886980035",
                                                "driverNationalId": "A123456789",
                                                "driverDob": "2004-11-08",
                                                "driverEmail": "john.doe@example.com",
                                                "driverDrivingLicenseUrl": "booking/123456/driver-driving-license.jpg",
                                                "driverCityProvince": "Tỉnh Hà Giang",
                                                "driverDistrict": "Thành phố Hà Giang",
                                                "driverWard": "Phường Quang Trung",
                                                "driverHouseNumberStreet": "211 Trần Duy Hưng",
                                                "isDriver": true
                                            }
                                            """
                                                    )
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3024 | The booking cannot be pickup when status not confirmed.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4014 | Can not view detail/edit booking of another account.|
                                    """
                    ),
            }
    )
    @PutMapping("/customer/confirm-pick-up/{bookingNumber}")
    public ApiResponse<BookingResponse> confirmPickUpBooking(@PathVariable @Parameter(description = "The booking number to be confirm pick up", example = "BK202410200001") String bookingNumber){
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.confirmPickUp(bookingNumber))
                .build();
    }

    @Operation(
            summary = "Return car",
            description = "This api allows customer to return the car",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class,
                                                            example = """
                                            {
                                                "bookingNumber": "BK202410200001",
                                                "carId": "car1",
                                                "status": "PENDING_PAYMENT",
                                                "pickUpLocation": "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng",
                                                "pickUpTime": "2004-11-08T09:00:00",
                                                "dropOffTime": "2004-11-08T18:00:00",
                                                "totalPrice": 50000,
                                                "basePrice": 50000,
                                                "deposit": 10000,
                                                "paymentType": "WALLET",
                                                "driverFullName": "John Doe",
                                                "driverPhoneNumber": "0886980035",
                                                "driverNationalId": "A123456789",
                                                "driverDob": "2004-11-08",
                                                "driverEmail": "john.doe@example.com",
                                                "driverDrivingLicenseUrl": "booking/123456/driver-driving-license.jpg",
                                                "driverCityProvince": "Tỉnh Hà Giang",
                                                "driverDistrict": "Thành phố Hà Giang",
                                                "driverWard": "Phường Quang Trung",
                                                "driverHouseNumberStreet": "211 Trần Duy Hưng",
                                                "isDriver": true
                                            }
                                            """
                                                    )
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3036 | The car cannot be return when booking status is not in-progress.|
                                    | 3003 | The account is not exist in the system.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4014 | Can not view detail/edit booking of another account.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3038 | There was error during sending waiting confirmed return car email to user.|
                                    | 3030 | There was error during sending pending payment booking email to user.|
                                    | 3029 | There was error during sending completed booking email to user.|
                                    """
                    ),
            }
    )
    @PutMapping("/customer/return-car/{bookingNumber}")
    public ApiResponse<BookingResponse> returnCar(@PathVariable @Parameter(description = "The booking number to be return car", example = "BK202410200001") String bookingNumber) {
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.returnCar(bookingNumber))
                .build();
    }

    @Operation(
            summary = "Confirm early return car",
            description = "This api allows car owner to confirm the request return early the car",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class,
                                                            example = """
                                            {
                                                "bookingNumber": "BK202410200001",
                                                "carId": "car1",
                                                "status": "COMPLETED",
                                                "pickUpLocation": "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng",
                                                "pickUpTime": "2004-11-08T09:00:00",
                                                "dropOffTime": "2004-11-08T18:00:00",
                                                "totalPrice": 50000,
                                                "basePrice": 50000,
                                                "deposit": 10000,
                                                "paymentType": "WALLET",
                                                "driverFullName": "John Doe",
                                                "driverPhoneNumber": "0886980035",
                                                "driverNationalId": "A123456789",
                                                "driverDob": "2004-11-08",
                                                "driverEmail": "john.doe@example.com",
                                                "driverDrivingLicenseUrl": "booking/123456/driver-driving-license.jpg",
                                                "driverCityProvince": "Tỉnh Hà Giang",
                                                "driverDistrict": "Thành phố Hà Giang",
                                                "driverWard": "Phường Quang Trung",
                                                "driverHouseNumberStreet": "211 Trần Duy Hưng",
                                                "isDriver": true
                                            }
                                            """
                                                    )
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3020 | The booking status does not allow this action.|
                                    | 3003 | The account is not exist in the system.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4010 | Can not view detail/edit car of another account.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3030 | There was error during sending pending payment booking email to user.|
                                    | 3029 | There was error during sending completed booking email to user.|
                                    """
                    ),
            }
    )
    @PutMapping("/car-owner/confirm-early-return/{bookingNumber}")
    public ApiResponse<BookingResponse> confirmEarlyReturnCar(@PathVariable @Parameter(description = "The booking number to be confirm return early car", example = "BK202410200001") String bookingNumber){
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.confirmEarlyReturnCar(bookingNumber))
                .build();
    }

    @Operation(
            summary = "Reject early return car",
            description = "This api allows car owner to reject the request return early the car",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class,
                                                            example = """
                                            {
                                                "bookingNumber": "BK202410200001",
                                                "carId": "car1",
                                                "status": "IN_PROGRESS",
                                                "pickUpLocation": "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng",
                                                "pickUpTime": "2004-11-08T09:00:00",
                                                "dropOffTime": "2004-11-08T18:00:00",
                                                "totalPrice": 50000,
                                                "basePrice": 50000,
                                                "deposit": 10000,
                                                "paymentType": "WALLET",
                                                "driverFullName": "John Doe",
                                                "driverPhoneNumber": "0886980035",
                                                "driverNationalId": "A123456789",
                                                "driverDob": "2004-11-08",
                                                "driverEmail": "john.doe@example.com",
                                                "driverDrivingLicenseUrl": "booking/123456/driver-driving-license.jpg",
                                                "driverCityProvince": "Tỉnh Hà Giang",
                                                "driverDistrict": "Thành phố Hà Giang",
                                                "driverWard": "Phường Quang Trung",
                                                "driverHouseNumberStreet": "211 Trần Duy Hưng",
                                                "isDriver": true
                                            }
                                            """
                                                    )
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3020 | The booking status does not allow this action.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4010 | Can not view detail/edit car of another account.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3041 | There was error during sending reject early return car email to user.|
                                    """
                    ),
            }
    )
    @PutMapping("/car-owner/reject-early-return/{bookingNumber}")
    public ApiResponse<BookingResponse> rejectEarlyReturnCar(@PathVariable @Parameter(description = "The booking number to be reject early return car", example = "BK202410200001") String bookingNumber){
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.rejectWaitingConfirmedEarlyReturnCarBooking(bookingNumber))
                .build();
    }

    @Operation(
            summary = "Reject Booking",
            description = "This api allows car owner to reject the booking",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class,
                                                            example = """
                                            {
                                                "bookingNumber": "BK202410200001",
                                                "carId": "car1",
                                                "status": "CANCELLED",
                                                "pickUpLocation": "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng",
                                                "pickUpTime": "2004-11-08T09:00:00",
                                                "dropOffTime": "2004-11-08T18:00:00",
                                                "totalPrice": 50000,
                                                "basePrice": 50000,
                                                "deposit": 10000,
                                                "paymentType": "WALLET",
                                                "driverFullName": "John Doe",
                                                "driverPhoneNumber": "0886980035",
                                                "driverNationalId": "A123456789",
                                                "driverDob": "2004-11-08",
                                                "driverEmail": "john.doe@example.com",
                                                "driverDrivingLicenseUrl": "booking/123456/driver-driving-license.jpg",
                                                "driverCityProvince": "Tỉnh Hà Giang",
                                                "driverDistrict": "Thành phố Hà Giang",
                                                "driverWard": "Phường Quang Trung",
                                                "driverHouseNumberStreet": "211 Trần Duy Hưng",
                                                "isDriver": true
                                            }
                                            """
                                                    )
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3020 | The booking status does not allow this action.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4010 | Can not view detail/edit car of another account.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3025 | There was error during sending cancelLed booking email to user.|
                                    """
                    ),
            }
    )
    @PutMapping("/car-owner/reject-booking/{bookingNumber}")
    public ApiResponse<BookingResponse> rejectBooking(@PathVariable @Parameter(description = "The booking number to be reject booking", example = "BK202410200001") String bookingNumber){
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.rejectWaitingConfirmedBooking(bookingNumber))
                .build();
    }

    @Operation(
            summary = "Confirm Deposit Operator",
            description = "This api allows operator to confirm the deposit of the booking",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class,
                                                            example = """
                                            {
                                                "bookingNumber": "BK202410200001",
                                                "carId": "car1",
                                                "status": "WAITING_CONFIRMED",
                                                "pickUpLocation": "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng",
                                                "pickUpTime": "2004-11-08T09:00:00",
                                                "dropOffTime": "2004-11-08T18:00:00",
                                                "totalPrice": 50000,
                                                "basePrice": 50000,
                                                "deposit": 10000,
                                                "paymentType": "WALLET",
                                                "driverFullName": "John Doe",
                                                "driverPhoneNumber": "0886980035",
                                                "driverNationalId": "A123456789",
                                                "driverDob": "2004-11-08",
                                                "driverEmail": "john.doe@example.com",
                                                "driverDrivingLicenseUrl": "booking/123456/driver-driving-license.jpg",
                                                "driverCityProvince": "Tỉnh Hà Giang",
                                                "driverDistrict": "Thành phố Hà Giang",
                                                "driverWard": "Phường Quang Trung",
                                                "driverHouseNumberStreet": "211 Trần Duy Hưng",
                                                "isDriver": true
                                            }
                                            """
                                                    )
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3020 | The booking status does not allow this action.|
                                    | 3044 | This payment type is not supported in this case.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3026 | There was error during sending waiting confirm email to user.|
                                    """
                    ),
            }
    )
    @PutMapping("/operator/confirm-deposit/{bookingNumber}")
    public ApiResponse<BookingResponse> confirmDeposit(@PathVariable @Parameter(description = "The booking number to be confirm deposit", example = "BK202410200001") String bookingNumber){
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.confirmDeposit(bookingNumber))
                .build();
    }

    @Operation(
            summary = "Reject Deposit Operator",
            description = "This api allows operator to reject the deposit of the booking",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class,
                                                            example = """
                                            {
                                                "bookingNumber": "BK202410200001",
                                                "carId": "car1",
                                                "status": "CANCELLED",
                                                "pickUpLocation": "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng",
                                                "pickUpTime": "2004-11-08T09:00:00",
                                                "dropOffTime": "2004-11-08T18:00:00",
                                                "totalPrice": 50000,
                                                "basePrice": 50000,
                                                "deposit": 10000,
                                                "paymentType": "WALLET",
                                                "driverFullName": "John Doe",
                                                "driverPhoneNumber": "0886980035",
                                                "driverNationalId": "A123456789",
                                                "driverDob": "2004-11-08",
                                                "driverEmail": "john.doe@example.com",
                                                "driverDrivingLicenseUrl": "booking/123456/driver-driving-license.jpg",
                                                "driverCityProvince": "Tỉnh Hà Giang",
                                                "driverDistrict": "Thành phố Hà Giang",
                                                "driverWard": "Phường Quang Trung",
                                                "driverHouseNumberStreet": "211 Trần Duy Hưng",
                                                "isDriver": true
                                            }
                                            """
                                                    )
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3020 | The booking status does not allow this action.|
                                    | 3044 | This payment type is not supported in this case.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3025 | There was error during sending cancelLed booking email to user.|
                                    """
                    ),
            }
    )
    @PutMapping("/operator/reject-deposit/{bookingNumber}")
    public ApiResponse<BookingResponse> rejectDeposit(@PathVariable @Parameter(description = "The booking number to be reject deposit", example = "BK202410200001") String bookingNumber){
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.rejectDeposit(bookingNumber))
                .build();
    }

    @Operation(
            summary = "Pay deposit again",
            description = "This api allows customer to pay deposit again",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class,
                                                            example = """
                                            {
                                                "bookingNumber": "BK202410200001",
                                                "carId": "car1",
                                                "status": "WAITING_CONFIRMED",
                                                "pickUpLocation": "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng",
                                                "pickUpTime": "2004-11-08T09:00:00",
                                                "dropOffTime": "2004-11-08T18:00:00",
                                                "totalPrice": 50000,
                                                "basePrice": 50000,
                                                "deposit": 10000,
                                                "paymentType": "WALLET",
                                                "driverFullName": "John Doe",
                                                "driverPhoneNumber": "0886980035",
                                                "driverNationalId": "A123456789",
                                                "driverDob": "2004-11-08",
                                                "driverEmail": "john.doe@example.com",
                                                "driverDrivingLicenseUrl": "booking/123456/driver-driving-license.jpg",
                                                "driverCityProvince": "Tỉnh Hà Giang",
                                                "driverDistrict": "Thành phố Hà Giang",
                                                "driverWard": "Phường Quang Trung",
                                                "driverHouseNumberStreet": "211 Trần Duy Hưng",
                                                "isDriver": true
                                            }
                                            """
                                                    )
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3020 | The booking status does not allow this action.|
                                    | 3044 | This payment type is not supported in this case.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4014 | Can not view detail/edit booking of another account.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3025 | There was error during sending cancelLed booking email to user.|
                                    | 3026 | There was error during sending waiting confirm email to user.|
                                    """
                    ),
            }
    )
    @PutMapping("/customer/pay-deposit-again/{bookingNumber}")
    public ApiResponse<BookingResponse> payDepositAgain(@PathVariable @Parameter(description = "The booking number to be pay deposit again", example = "BK202410200001") String bookingNumber){
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.payDepositAgain(bookingNumber))
                .build();
    }

    @Operation(
            summary = "Pay deposit again",
            description = "This api allows customer to pay deposit again",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(
                                    schema = @Schema(type = "object"),
                                    schemaProperties = {
                                            @SchemaProperty(
                                                    name = "code",
                                                    schema = @Schema(type = "string", example = "1000")
                                            ),
                                            @SchemaProperty(
                                                    name = "message",
                                                    schema = @Schema(type = "string", example = "Success")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = BookingResponse.class,
                                                            example = """
                                            {
                                                "bookingNumber": "BK202410200001",
                                                "carId": "car1",
                                                "status": "COMPLETED",
                                                "pickUpLocation": "Tỉnh Hà Giang, Thành phố Hà Giang, Phường Quang Trung, 211 Trần Duy Hưng",
                                                "pickUpTime": "2004-11-08T09:00:00",
                                                "dropOffTime": "2004-11-08T18:00:00",
                                                "totalPrice": 50000,
                                                "basePrice": 50000,
                                                "deposit": 10000,
                                                "paymentType": "WALLET",
                                                "driverFullName": "John Doe",
                                                "driverPhoneNumber": "0886980035",
                                                "driverNationalId": "A123456789",
                                                "driverDob": "2004-11-08",
                                                "driverEmail": "john.doe@example.com",
                                                "driverDrivingLicenseUrl": "booking/123456/driver-driving-license.jpg",
                                                "driverCityProvince": "Tỉnh Hà Giang",
                                                "driverDistrict": "Thành phố Hà Giang",
                                                "driverWard": "Phường Quang Trung",
                                                "driverHouseNumberStreet": "211 Trần Duy Hưng",
                                                "isDriver": true
                                            }
                                            """
                                                    )
                                            )
                                    }
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = """
                                    Bad request
                                    |code  | message |
                                    |------|-------------|
                                    | 3018 | The booking is not exist in the system.|
                                    | 3020 | The booking status does not allow this action.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 4014 | Can not view detail/edit booking of another account.|
                                    """
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service_Unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3029 | There was error during sending completed booking email to user.|
                                    | 3030 | There was error during sending pending payment booking email to user.|
                                    """
                    ),
            }
    )
    @PutMapping("/customer/pay-total-payment-again/{bookingNumber}")
    public ApiResponse<BookingResponse> payTotalPaymentAgain(@PathVariable @Parameter(description = "The booking number to be pay deposit again", example = "BK202410200001") String bookingNumber){
        return ApiResponse.<BookingResponse>builder()
                .data(bookingService.payTotalPaymentAgain(bookingNumber))
                .build();
    }

}
