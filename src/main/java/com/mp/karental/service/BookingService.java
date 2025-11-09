package com.mp.karental.service;

import com.mp.karental.constant.EBookingStatus;
import com.mp.karental.constant.EPaymentType;
import com.mp.karental.dto.request.booking.CreateBookingRequest;
import com.mp.karental.constant.ERole;
import com.mp.karental.dto.request.booking.EditBookingRequest;
import com.mp.karental.dto.response.booking.BookingResponse;
import com.mp.karental.dto.response.booking.BookingThumbnailResponse;
import com.mp.karental.dto.response.booking.BookingListResponse;
import com.mp.karental.dto.response.booking.WalletResponse;
import com.mp.karental.entity.*;
import com.mp.karental.exception.AppException;
import com.mp.karental.exception.ErrorCode;
import com.mp.karental.mapper.BookingMapper;
import com.mp.karental.repository.BookingRepository;
import com.mp.karental.repository.CarRepository;
import com.mp.karental.repository.WalletRepository;
import com.mp.karental.security.SecurityUtil;
import com.mp.karental.util.RedisUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 * Service class for handling booking operations.
 *
 * @author QuangPM20
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BookingService {
    BookingRepository bookingRepository;
    CarRepository carRepository;
    BookingMapper bookingMapper;
    RedisUtil redisUtil;
    WalletRepository walletRepository;
    FileService fileService;
    CarService carService;
    TransactionService transactionService;
    EmailService emailService;

    // Define constant field names to avoid repetition
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final String FIELD_BASE_PRICE = "basePrice";

    /**
     * Creates a new booking for a car rental.
     *
     * @param createBookingRequest The booking request details.
     * @return BookingResponse containing booking details.
     * @throws AppException if there are validation issues or car availability problems.
     */
    public BookingResponse createBooking(CreateBookingRequest createBookingRequest) throws AppException {
        // Get the current logged-in user's account ID and account details
        Account customerAccount = prepareBooking();
        String customerAccountId = customerAccount.getId();

        // Retrieve car details from the database, throw an exception if not found.
        Car car = carRepository.findById(createBookingRequest.getCarId())
                .orElseThrow(() -> new AppException(ErrorCode.CAR_NOT_FOUND_IN_DB));
        // Retrieve the customer's wallet, throw an exception if not found.
        Wallet walletCustomer = walletRepository.findById(customerAccountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND_IN_DB));

        // Check if the car is available for the requested pickup and drop-off time.
        if (!carService.isCarAvailable(car.getId(), createBookingRequest.getPickUpTime(), createBookingRequest.getDropOffTime())) {
            throw new AppException(ErrorCode.CAR_NOT_AVAILABLE);
        }

        // Map the booking request to a Booking entity.
        Booking booking = bookingMapper.toBooking(createBookingRequest);
        booking.setBookingNumber(redisUtil.generateBookingNumber());
        
        // Construct pickUpLocation from car entity (backend has access to full address)
        // Format: "City, District, Ward, House number and street"
        String pickUpLocation = car.getCityProvince() + ", " + 
                                car.getDistrict() + ", " + 
                                car.getWard() + ", " + 
                                car.getHouseNumberStreet();
        booking.setPickUpLocation(pickUpLocation);

        // Upload the driver's license to S3 storage.
        String drivingLicenseKey;

        // Upload the driver's license to S3 storage if the driver information is provided.
        if (createBookingRequest.isDriver()) {
            //the renter is different from the driver
            //ensure that all driver's information is not null and not empty
            validateDriverInfo(
                    createBookingRequest.getDriverFullName(),
                    createBookingRequest.getDriverPhoneNumber(),
                    createBookingRequest.getDriverNationalId(),
                    createBookingRequest.getDriverDob(),
                    createBookingRequest.getDriverEmail(),
                    createBookingRequest.getDriverCityProvince(),
                    createBookingRequest.getDriverDistrict(),
                    createBookingRequest.getDriverWard(),
                    createBookingRequest.getDriverHouseNumberStreet()
            );

            MultipartFile drivingLicense = createBookingRequest.getDriverDrivingLicense();
            drivingLicenseKey = uploadDriverDrivingLicense(drivingLicense, booking);
        } else {
            // Use existing license URI from account profile if the driver is same as the renter
            drivingLicenseKey = customerAccount.getProfile().getDrivingLicenseUri();
            setAccountProfileToDriver(booking, customerAccount); // Set the profile information to the booking.
        }

        booking.setDriverDrivingLicenseUri(drivingLicenseKey);

        // Assign renter account and car to the booking.
        booking.setAccount(customerAccount);
        booking.setCar(car);

        // Store car deposit and base price at the time of booking.
        booking.setDeposit(car.getDeposit());
        booking.setBasePrice(car.getBasePrice());
        booking.setUpdateBy(SecurityUtil.getCurrentAccountId());

        // Save the booking to the database first (so account relationship is persisted)
        bookingRepository.save(booking);
        
        // Handle paying deposit.
        if (booking.getPaymentType().equals(EPaymentType.WALLET)
                && walletCustomer.getBalance() >= car.getDeposit()) {
            // If the customer using wallet and has enough balance in the wallet
            payBookingDepositUsingWallet(booking, customerAccountId);
        } else {
            // the customer not using wallet to pay deposit or the wallet's balance is not enough
            booking.setStatus(EBookingStatus.PENDING_DEPOSIT);
            redisUtil.cachePendingDepositBooking(booking.getBookingNumber());
            bookingRepository.save(booking);
        }

        return buildBookingResponse(booking, drivingLicenseKey);
    }

    /**
     * Uploads the driver's driving license for a booking.
     * Validates if a driving license file is provided and then uploads it to a designated location.
     *
     * @param drivingLicense The uploaded file containing the driver's driving license.
     * @param booking        The booking associated with the driver's license.
     * @return The generated file key for the uploaded license.
     * @throws AppException If the driving license is not provided.
     */
    private String uploadDriverDrivingLicense(MultipartFile drivingLicense, Booking booking) {
        if (drivingLicense == null || drivingLicense.isEmpty()) {
            // Renter did not provide a driver's driving license
            throw new AppException(ErrorCode.INVALID_DRIVER_INFO); // Validate driver's license is provided.
        }
        // Generate a unique key for storing the driver's license file
        String drivingLicenseKey = "booking/" + booking.getBookingNumber() + "/driver-driving-license"
                + fileService.getFileExtension(drivingLicense);

        // Upload the driver's license file
        fileService.uploadFile(drivingLicense, drivingLicenseKey);

        return drivingLicenseKey;
    }

    /**
     * Processes the booking deposit payment using the customer's wallet.
     * Updates the booking status and cancels overlapping pending deposit bookings.
     * Notifies affected customers via email and removes cached pending deposit bookings.
     *
     * @param booking The booking for which the deposit payment is being processed.
     * @throws AppException If email notifications fail to send.
     */
    private void payBookingDepositUsingWallet(Booking booking, String customerAccountId) {
        // Process the deposit payment and update the booking status
        // Pass customerAccountId directly to avoid issues with booking.getAccount().getId() before booking is saved
        transactionService.payDeposit(booking, customerAccountId);
        handleBookingConfirmation(booking);
    }

    private void confirmBookingDepositNotUsingWallet(Booking booking) {
        handleBookingConfirmation(booking);
    }

    private void handleBookingConfirmation(Booking booking) {
        // Process the deposit payment and update the booking status
        booking.setStatus(EBookingStatus.WAITING_CONFIRMED);
        // Cancel overlapping pending deposit bookings for the same car
        List<Booking> overlappingBookings = bookingRepository.findByCarIdAndStatusAndTimeOverlap(
                booking.getCar().getId(),
                EBookingStatus.PENDING_DEPOSIT,
                booking.getPickUpTime(),
                booking.getDropOffTime()
        );

        for (Booking pendingBooking : overlappingBookings) {
            pendingBooking.setStatus(EBookingStatus.CANCELLED);
            bookingRepository.saveAndFlush(pendingBooking);

            // Notify the customer about the cancellation
            String reason = "Your booking has been canceled because another customer has successfully placed a deposit for this car within the same rental period.";
            emailService.sendCancelledBookingEmail(pendingBooking.getAccount().getEmail(),
                    pendingBooking.getCar().getBrand() + " " + pendingBooking.getCar().getModel(),
                    reason);

        }

        // Remove the cached pending deposit booking from Redis
        redisUtil.removeCachePendingDepositBooking(booking.getBookingNumber());

        // Send confirmation emails to both the customer and car owner
        emailService.sendWaitingConfirmedEmail(booking.getAccount().getEmail(),
                booking.getCar().getAccount().getEmail(),
                booking.getCar().getBrand() + " " + booking.getCar().getModel(),
                booking.getBookingNumber()
        );
    }


    /**
     * Edits an existing booking based on the provided booking number and update request.
     *
     * @param editBookingRequest The request details for updating the booking, including new car information and driver’s license.
     * @param bookingNumber      The booking number of the booking to be edited.
     * @return BookingResponse containing the updated booking details.
     * @throws AppException If there are any validation issues, the booking is not found, or the user doesn’t have access to edit the booking.
     */
    public BookingResponse editBooking(EditBookingRequest editBookingRequest, String bookingNumber) throws AppException {
        Booking booking = validateAndGetBookingCustomer(bookingNumber);

        //do not allow edit
        if (booking.getStatus() != EBookingStatus.PENDING_DEPOSIT &&
                booking.getStatus() != EBookingStatus.WAITING_CONFIRMED &&
                booking.getStatus() != EBookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_BE_EDITED);
        }

        // Update the booking details using the request data
        bookingMapper.editBooking(booking, editBookingRequest);

        //Update the booking uri
        String drivingLicenseKey;

        // If the driver is provided, validate and possibly upload a new driver's license.
        if (editBookingRequest.isDriver()) {
            // Ensure that the required driver fields are not null and not empty
            validateDriverInfo(
                    editBookingRequest.getDriverFullName(),
                    editBookingRequest.getDriverPhoneNumber(),
                    editBookingRequest.getDriverNationalId(),
                    editBookingRequest.getDriverDob(),
                    editBookingRequest.getDriverEmail(),
                    editBookingRequest.getDriverCityProvince(),
                    editBookingRequest.getDriverDistrict(),
                    editBookingRequest.getDriverWard(),
                    editBookingRequest.getDriverHouseNumberStreet()
            );

            MultipartFile drivingLicense = editBookingRequest.getDriverDrivingLicense();
            drivingLicenseKey = uploadDriverDrivingLicense(drivingLicense, booking);
        } else {
            // Use existing license URI from account profile if the driver is same as the renter
            drivingLicenseKey = booking.getAccount().getProfile().getDrivingLicenseUri();
            setAccountProfileToDriver(booking, booking.getAccount()); // Set the profile information to the booking.
        }

        booking.setDriverDrivingLicenseUri(drivingLicenseKey);
        booking.setUpdateBy(SecurityUtil.getCurrentAccountId());

        // Save the booking to the database.
        bookingRepository.saveAndFlush(booking);

        return buildBookingResponse(booking, drivingLicenseKey);
    }

    /**
     * Prepares the booking by ensuring the account is valid and the profile is complete.
     * This method updates the status of existing bookings and retrieves the current logged-in user's account details.
     * It also checks if the account's profile is complete before allowing further booking actions.
     *
     * @return The account of the currently logged-in user.
     * @throws AppException If the account's profile is incomplete or any error occurs during the booking preparation.
     */
    private Account prepareBooking() throws AppException {
        // Get the current logged-in user's account ID and account details
        Account accountCustomer = SecurityUtil.getCurrentAccount();

        // Ensure the account has completed the individual profile
        if (!isProfileComplete(accountCustomer.getProfile())) {
            throw new AppException(ErrorCode.FORBIDDEN_PROFILE_INCOMPLETE);
        }

        return accountCustomer;
    }

    /**
     * Builds a BookingResponse object containing the booking details and additional calculated data.
     * This method calculates the rental duration in days, converts the booking entity to a response DTO,
     * and sets the relevant fields such as the driver's license URL, car ID, and total price.
     * It also determines if the booking is for a driver or not based on the URI of the driver's license.
     *
     * @param booking The booking entity containing the booking details.
     * @param s3Key   The S3 key for the driver's license file.
     * @return A BookingResponse object containing the booking details and calculated values.
     */
    private BookingResponse buildBookingResponse(Booking booking, String s3Key) {
        // Calculate rental duration in minutes.
        long minutes = Duration.between(booking.getPickUpTime(), booking.getDropOffTime()).toMinutes();
        long days = (long) Math.ceil(minutes / (24.0 * 60)); // Convert minutes to full days.

        // Convert the booking entity to a response DTO.
        BookingResponse bookingResponse = bookingMapper.toBookingResponse(booking);
        bookingResponse.setDriverDrivingLicenseUrl(fileService.getFileUrl(s3Key));
        bookingResponse.setCarId(booking.getCar().getId());
        bookingResponse.setTotalPrice(booking.getBasePrice() * days);
        bookingResponse.setDriver(!booking.getDriverDrivingLicenseUri().startsWith("user/"));
        return bookingResponse;
    }

    /**
     * Checks if a given string is null or empty.
     * This method returns true if the string is either null or contains only whitespace characters;
     * otherwise, it returns false.
     *
     * @param str The string to check.
     * @return True if the string is null or empty (after trimming); otherwise, false.
     */
    private boolean isNullOrEmpty(String str) {
        return str == null || str.trim().isEmpty();
    }

    /**
     * Sets the driver's profile information to the booking object.
     * This method takes an Account object, which contains the user's profile details, and updates the corresponding
     * driver information in the Booking object. The driver's personal details, such as full name, phone number,
     * national ID, date of birth, email, and address (city, district, ward, house number/street) are set to the
     * booking entity.
     *
     * @param booking The booking object that will be updated with the driver's information.
     * @param account The account object that contains the current logged-in user's profile details.
     */
    private void setAccountProfileToDriver(Booking booking, Account account) {
        booking.setDriverFullName(account.getProfile().getFullName());
        booking.setDriverPhoneNumber(account.getProfile().getPhoneNumber());
        booking.setDriverNationalId(account.getProfile().getNationalId());
        booking.setDriverDob(account.getProfile().getDob());
        booking.setDriverEmail(account.getEmail());
        booking.setDriverCityProvince(account.getProfile().getCityProvince());
        booking.setDriverDistrict(account.getProfile().getDistrict());
        booking.setDriverWard(account.getProfile().getWard());
        booking.setDriverHouseNumberStreet(account.getProfile().getHouseNumberStreet());
    }

    /**
     * Validates driver information to ensure all required fields are provided.
     *
     * @param fullName          The full name of the driver (must not be null or empty).
     * @param phoneNumber       The phone number of the driver (must not be null or empty).
     * @param nationalId        The national ID of the driver (must not be null or empty).
     * @param dob               The date of birth of the driver (must not be null).
     * @param email             The email of the driver (must not be null or empty).
     * @param city              The city or province of the driver (must not be null or empty).
     * @param district          The district of the driver (must not be null or empty).
     * @param ward              The ward of the driver (must not be null or empty).
     * @param houseNumberStreet The house number and street address of the driver (must not be null or empty).
     * @throws AppException if any required field is missing or empty.
     */
    private void validateDriverInfo(String fullName, String phoneNumber, String nationalId,
                                    LocalDate dob, String email, String city, String district,
                                    String ward, String houseNumberStreet) {
        boolean isValid = Stream.of(
                fullName,
                phoneNumber,
                nationalId,
                email,
                city,
                district,
                ward,
                houseNumberStreet
        ).noneMatch(this::isNullOrEmpty);
        if (dob == null || !isValid) {
            throw new AppException(ErrorCode.INVALID_DRIVER_INFO);
        }
        // Validate phone number format (must be exactly 10 digits)
        if (!phoneNumber.matches("^0\\d{9}$")) {
            throw new AppException(ErrorCode.INVALID_PHONE_NUMBER);
        }

        // Validate national ID format (must be between 9 and 12 digits)
        if (!nationalId.matches("\\d{9,12}")) {
            throw new AppException(ErrorCode.INVALID_NATIONAL_ID);
        }
    }

    /**
     * to check the account must complete the profile before booking
     *
     * @param profile the profile of the current account
     * @return the profile with full information
     */
    private boolean isProfileComplete(UserProfile profile) {
        // Check all required fields including DOB (needed when renter is driver)
        boolean hasAllRequiredFields = Stream.of(
                profile.getNationalId(),
                profile.getDrivingLicenseUri(),
                profile.getPhoneNumber(),
                profile.getCityProvince(),
                profile.getDistrict(),
                profile.getWard(),
                profile.getHouseNumberStreet(),
                profile.getFullName()
        ).noneMatch(this::isNullOrEmpty);
        
        // Also check DOB is not null (required when renter is driver)
        return hasAllRequiredFields && profile.getDob() != null;
    }


    /**
     * Retrieves the list of bookings for the renter (based on accountId).
     * If the status is null or invalid, it returns all bookings.
     *
     * @param page   the page number (starting from 0)
     * @param size   the number of records per page
     * @param sort   sorting string in the format "field,DIRECTION" (e.g., "updatedAt,DESC")
     * @param status booking status (nullable to fetch all bookings)
     * @return list of user bookings wrapped in `BookingListResponse`
     */
    public BookingListResponse getBookingsOfCustomer(int page, int size, String sort, String status) {
        // Retrieve the currently logged-in user's account ID
        String accountId = SecurityUtil.getCurrentAccountId();

        // Create a Pageable object for pagination and sorting
        Pageable pageable = createPageable(page, size, sort);

        Page<Booking> bookings;

        // Convert the status string into the EBookingStatus enum
        EBookingStatus bookingStatus = parseStatus(status);

        // If the status is valid, fetch bookings filtered by status
        // Otherwise, fetch all bookings for the user
        bookings = (bookingStatus != null)
                ? bookingRepository.findByAccountIdAndStatus(accountId, bookingStatus, pageable)
                : bookingRepository.findByAccountId(accountId, pageable);
        logBookingAccess(accountId, bookings.getTotalElements());
        // Convert the list of bookings into a BookingListResponse object to return
        return getBookingListResponse(bookings, true);
    }

    /**
     * Retrieves the list of bookings for operator (based on accountId).
     * If the status is null or invalid, it returns all bookings.
     *
     * @param page   the page number (starting from 0)
     * @param size   the number of records per page
     * @param sort   sorting string in the format "field,DIRECTION" (e.g., "updatedAt,DESC")
     * @param status booking status (nullable to fetch all bookings)
     * @return list of user bookings wrapped in `BookingListResponse`
     */
    public BookingListResponse getBookingsOfOperator(int page, int size, String sort, String status) {
        // Retrieve the currently logged-in user's account ID
        String accountId = SecurityUtil.getCurrentAccountId();

        // Create a Pageable object for pagination and sorting
        Pageable pageable = createPageable(page, size, sort);

        Page<Booking> bookings;

        // Convert the status string into the EBookingStatus enum
        EBookingStatus bookingStatus = parseStatus(status);
        // If the status is valid, fetch bookings filtered by status
        bookings = (bookingStatus != null)
                ? bookingRepository.findBookingsByStatus(bookingStatus, pageable)
                : bookingRepository.findAllBookings(pageable);
        logBookingAccess(accountId, bookings.getTotalElements());
        // Convert the list of bookings into a BookingListResponse object to return
        return getBookingListResponse(bookings, false);
    }


    /**
     * Retrieves the list of bookings for the car owner (based on ownerId). (Car Owner view his/her rentals)
     * If the status is null or invalid, it returns all bookings except those in PENDING_DEPOSIT status.
     */
    public BookingListResponse getBookingsOfCarOwner(int page, int size, String sort, String status) {
        // Retrieve the car owner's account ID
        String ownerId = SecurityUtil.getCurrentAccountId();
        Pageable pageable = createPageable(page, size, sort);

        Page<Booking> bookings;
        // Parse the provided status string into an enum value
        EBookingStatus bookingStatus = parseStatus(status);
        bookings = (bookingStatus != null)
                ? bookingRepository.findBookingsByCarOwnerIdAndStatus(ownerId, bookingStatus, EBookingStatus.PENDING_DEPOSIT, pageable)
                : bookingRepository.findBookingsByCarOwnerId(ownerId, EBookingStatus.PENDING_DEPOSIT, pageable);
        logBookingAccess(ownerId, bookings.getTotalElements());
        return getBookingListResponse(bookings, false);
    }

    private void logBookingAccess(String accountId, long recordCount) {
        log.info("Successfully accessed booking information, number of records: {}, accessed by: {}",
                recordCount, accountId);
    }

    /**
     * Converts a page of `Booking` objects into `BookingListResponse`.
     * Includes booking details, car images, and additional computed fields.
     */
    private BookingListResponse getBookingListResponse(Page<Booking> bookings, boolean isCustomer) {
        // Retrieve the current user's account ID
        String currentUserId = SecurityUtil.getCurrentAccountId();

        // Map booking entities to response DTOs
        Page<BookingThumbnailResponse> bookingResponses = bookings.map(booking -> {
            BookingThumbnailResponse response = bookingMapper.toBookingThumbnailResponse(booking);

            // Calculate rental duration in days and total price
            long totalHours = Duration.between(booking.getPickUpTime(), booking.getDropOffTime()).toHours();
            long numberOfDay = (long) Math.ceil((double) totalHours / 24);
            long totalPrice = numberOfDay * booking.getBasePrice();

            response.setNumberOfDay((int) numberOfDay);
            response.setTotalPrice(totalPrice);

            response.setCustomerEmail(booking.getAccount().getEmail());
            response.setCustomerPhoneNumber(booking.getAccount().getProfile().getPhoneNumber());
            response.setPaymentType(booking.getPaymentType());

            // Retrieve car images
            response.setCarImageFrontUrl(fileService.getFileUrl(booking.getCar().getCarImageFront()));
            response.setCarImageBackUrl(fileService.getFileUrl(booking.getCar().getCarImageBack()));
            response.setCarImageLeftUrl(fileService.getFileUrl(booking.getCar().getCarImageLeft()));
            response.setCarImageRightUrl(fileService.getFileUrl(booking.getCar().getCarImageRight()));

            return response;
        });

        // Count the number of ongoing bookings for the current user
        // If Customer, get status booking ongoing
        int totalOnGoingBookings = isCustomer
                ? bookingRepository.countOngoingBookingsByCar(
                currentUserId,
                List.of(
                        EBookingStatus.PENDING_DEPOSIT,
                        EBookingStatus.WAITING_CONFIRMED,
                        EBookingStatus.CONFIRMED,
                        EBookingStatus.IN_PROGRESS,
                        EBookingStatus.PENDING_PAYMENT
                )
        )
                : 0;
        // Count the number of bookings that are waiting for confirmation for the car owner
        // If Car Owner, get booking waiting confirm
        int totalWaitingConfirmBooking = isCustomer
                ? 0
                : bookingRepository.countBookingsByOwnerAndStatus(currentUserId, EBookingStatus.WAITING_CONFIRMED);

        return new BookingListResponse(totalOnGoingBookings, totalWaitingConfirmBooking, bookingResponses);
    }


    /**
     * Creates a `Pageable` object for pagination and sorting.
     * Ensures default values are set if the input is invalid.
     */
    private Pageable createPageable(int page, int size, String sort) {
        // Ensure size is within a reasonable range (default to 10 if invalid)
        size = (size > 0 && size <= 100) ? size : 10;

        // Ensure page number is non-negative (default to 0 if negative)
        page = Math.max(page, 0);

        // Default sorting values
        String sortField = FIELD_UPDATED_AT;
        Sort.Direction sortDirection = Sort.Direction.DESC;

        // Parse sorting parameters if provided
        if (sort != null && !sort.isBlank()) {
            String[] sortParams = sort.split(",");

            // Validate and extract sorting field and direction
            if (sortParams.length == 2) {
                String requestedField = sortParams[0].trim();
                String requestedDirection = sortParams[1].trim().toUpperCase();

                // Validate field name (only allow predefined fields)
                if (FIELD_BASE_PRICE.equals(requestedField)) {
                    sortField = requestedField;
                }

                // Validate sorting direction
                if ("ASC".equals(requestedDirection) || "DESC".equals(requestedDirection)) {
                    sortDirection = Sort.Direction.valueOf(requestedDirection);
                }
            }
        }

        // Create and return a pageable object with sorting
        return PageRequest.of(page, size, Sort.by(sortDirection, sortField));
    }


    /**
     * Converts a status string to an `EBookingStatus` enum.
     * If the status is invalid or not found, returns `null` (defaulting to all bookings).
     */
    private EBookingStatus parseStatus(String statusStr) {
        if (statusStr == null || statusStr.isBlank()) {
            return null; // Return null to get all bookings
        }
        try {
            // Convert the input string to an EBookingStatus enum
            return EBookingStatus.valueOf(statusStr.toUpperCase());

        } catch (IllegalArgumentException e) {
            log.info("parsing booking status {} to enum fail", statusStr);
            return null;
        }
    }

    /**
     * this method to get the wallet by account login
     *
     * @return wallet of that account
     */
    public WalletResponse getWallet() {
        // Get the current logged-in user's account ID.
        String accountId = SecurityUtil.getCurrentAccountId();
        Wallet wallet = walletRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND_IN_DB));
        return new WalletResponse(
                wallet.getId(),
                wallet.getBalance()
        );
    }

    /**
     * Retrieves booking details by booking number and ensures the user has access to the booking.
     * This method checks the current logged-in user's account ID and compares it with the booking's account ID
     * to ensure the user has access to the booking details. If the booking is not found or the user does not have
     * permission, an exception is thrown.
     * Ensures proper authorization for CAR_OWNER and CUSTOMER roles.
     *
     * @param bookingNumber The booking number to fetch the booking details.
     * @return A BookingResponse object containing the booking details.
     * @throws AppException If the booking is not found or the user does not have permission to access the booking.
     */
    public BookingResponse getBookingDetailsByBookingNumber(String bookingNumber) {
        // Get the full account details of the currently logged-in user
        Account account = SecurityUtil.getCurrentAccount();

        Booking booking;

        // Check if the user is a CAR_OWNER
        if (ERole.CAR_OWNER.equals(account.getRole().getName())) {
            // Retrieve the booking details only if the booking is associated with the car owner's vehicles
            booking = validateAndGetBookingCarOwner(bookingNumber);

            // Check if the user is a CUSTOMER
        } else if (ERole.CUSTOMER.equals(account.getRole().getName())) {
            booking = validateAndGetBookingCustomer(bookingNumber);
            // If the user role is neither CAR_OWNER nor CUSTOMER, throw an unauthorized error
        } else {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }
        log.info("Successfully accessed booking's information, bookingNo: {}, accessed by: {}",
                bookingNumber, account.getId());
        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());

    }

    /**
     * Confirms a booking by the car owner and returns updated booking details.
     * <p>
     * Car Owner approve a booking, equivalent to allow the customer to rent the car as booking information
     * </p>
     *
     * @return BookingResponse containing updated booking details.
     * @throws AppException if validation fails.
     */
    public BookingResponse confirmBooking(String bookingNumber) {
        log.info("Car owner {} is confirming booking {}", SecurityUtil.getCurrentAccount().getId(), bookingNumber);
        Booking booking = validateAndGetBookingCarOwner(bookingNumber);
        // Ensure the booking status is valid for confirmation
        if (!EBookingStatus.WAITING_CONFIRMED.equals(booking.getStatus())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
        processOverdueWaitingBookings();
        // Ensure the booking has not expired
        if (booking.getPickUpTime() == null || LocalDateTime.now().isAfter(booking.getPickUpTime())) {
            throw new AppException(ErrorCode.BOOKING_EXPIRED);
        }

        // Update the booking status to CONFIRMED
        booking.setStatus(EBookingStatus.CONFIRMED);
        booking.setUpdateBy(SecurityUtil.getCurrentAccount().getId());
        bookingRepository.saveAndFlush(booking);

        emailService.sendConfirmBookingEmail(booking.getAccount().getEmail(),
                booking.getCar().getBrand() + " " + booking.getCar().getModel(),
                booking.getBookingNumber());

        log.info("Booking {} confirmed successfully by car owner {}", bookingNumber, booking.getAccount().getId());

        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
    }

    /**
     * Cancels a booking based on the provided booking number.
     * This method ensures that only the booking owner can cancel the booking.
     * Depending on the current booking status, it processes refunds, updates wallet balances,
     * and sends appropriate email notifications to the customer and car owner.
     *
     * @param bookingNumber The unique identifier of the booking to be canceled.
     * @return A {@link BookingResponse} containing the updated booking details.
     * @throws AppException If the booking is not found, the user is unauthorized to cancel,
     *                      or the booking is in a non-cancellable state.
     */
    public BookingResponse cancelBooking(String bookingNumber) {
        Booking booking = validateAndGetBookingCustomer(bookingNumber);

        // Check if the booking is in a state that cannot be canceled
        if (booking.getStatus() != EBookingStatus.PENDING_DEPOSIT &&
                booking.getStatus() != EBookingStatus.WAITING_CONFIRMED &&
                booking.getStatus() != EBookingStatus.CONFIRMED) {
            throw new AppException(ErrorCode.BOOKING_CANNOT_CANCEL);
        }

        EBookingStatus bookingStatus = booking.getStatus();
        String carName = booking.getCar().getBrand() + " " + booking.getCar().getModel();

        // If the booking is in WAITING_CONFIRMED status, refund the full deposit to the customer
        if (bookingStatus == EBookingStatus.WAITING_CONFIRMED) {
            transactionService.refundAllDeposit(booking);
        }

        // If the booking is in CONFIRMED status, refund 70% of the deposit and notify the car owner
        if (bookingStatus == EBookingStatus.CONFIRMED) {
            transactionService.refundPartialDeposit(booking);
            // Send an email notification to the customer about the partial refund (70%)
            emailService.sendBookingCancellationEmailToCarOwner(booking.getCar().getAccount().getEmail(), bookingNumber, carName);
        }

        //the booking is in PENDING_DEPOSIT status, only send a cancellation email to the customer
        emailService.sendBookingCancellationEmailToCustomer(booking.getAccount().getEmail(), bookingStatus, bookingNumber, carName);

        // Update the booking status to CANCELLED
        booking.setStatus(EBookingStatus.CANCELLED);
        booking.setUpdateBy(SecurityUtil.getCurrentAccount().getId());
        bookingRepository.saveAndFlush(booking);

        // Return the updated booking details
        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
    }

    /**
     * Confirms the pick-up of a booking if it meets the necessary conditions.
     *
     * @param bookingNumber The unique booking number.
     * @return BookingResponse containing updated booking details.
     * @throws AppException If the booking is not found, access is denied, or conditions are not met.
     */
    public BookingResponse confirmPickUp(String bookingNumber) {
        Booking booking = validateAndGetBookingCustomer(bookingNumber);
        processOverduePickUpAndDropOffBookings();
        // Validate if the booking is eligible for pick-up confirmation
        if (booking.getStatus() != EBookingStatus.CONFIRMED) { // Cannot confirm pick-up too early
            throw new AppException(ErrorCode.BOOKING_CANNOT_PICKUP);
        }
        // Update the booking status to IN_PROGRESS to indicate the pick-up process has started
        booking.setStatus(EBookingStatus.IN_PROGRESS);
        booking.setUpdateBy(SecurityUtil.getCurrentAccount().getId());

        // Save the updated booking status to the database
        bookingRepository.saveAndFlush(booking);

        // Return the updated booking details along with the driver's license URI (if applicable)
        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
    }

    /**
     * Handles the process of returning a car after a booking.
     * This method validates the booking, checks the payment status,
     * processes refunds or payments, updates the booking status, and sends email notifications.
     *
     * @param bookingNumber The unique identifier of the booking.
     * @return BookingResponse containing updated booking details.
     * @throws AppException If the booking is not found, unauthorized access is detected,
     *                      payment processing fails, or email notifications cannot be sent.
     */
    public BookingResponse returnCar(String bookingNumber) {
        // Validate and retrieve the booking
        Booking booking = validateAndGetBookingCustomer(bookingNumber);
        //WALLET is the only payment type when return car
        booking.setPaymentType(EPaymentType.WALLET);
        // Ensure the booking is in progress and before drop-off time is not exceeded
        if (booking.getStatus() != EBookingStatus.IN_PROGRESS) {
            throw new AppException(ErrorCode.CAR_CANNOT_RETURN);
        }
        processOverduePickUpAndDropOffBookings();
        if (LocalDateTime.now().isBefore(booking.getDropOffTime())) {
            //set the status of booking to WAITING_CONFIRMED_RETURN_CAR
            booking.setStatus(EBookingStatus.WAITING_CONFIRMED_RETURN_CAR);
            //send email to notify the car owner confirm the request return early
            emailService.sendWaitingConfirmReturnCarEmail(booking.getCar().getAccount().getEmail()
                    , bookingNumber);
            // Stop execution to prevent payment deduction before confirmation
            return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
        }

        processPaymentAndFinalizeBooking(booking);

        // Return the updated booking response
        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
    }

    /**
     * Handles the process for car owner to confirm early return car request of customer.
     * @param bookingNumber the booking number of the booking request return early
     * @return BookingResponse containing updated booking details.
     */
    public BookingResponse confirmEarlyReturnCar(String bookingNumber) {
        // Fetch the booking from the database using the booking number
        Booking booking = validateAndGetBookingCarOwner(bookingNumber);
        if (!EBookingStatus.WAITING_CONFIRMED_RETURN_CAR.equals(booking.getStatus())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
        processOverdueWaitingBookings();

        processPaymentAndFinalizeBooking(booking);

        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
    }

    /**
     * Handles the process for a car owner to reject a booking with status WAITING_CONFIRMED.
     * This method cancels the booking and refunds the deposit.
     *
     * @param bookingNumber The booking number of the booking with status WAITING_CONFIRMED.
     * @return BookingResponse containing the updated booking details.
     */
    public BookingResponse rejectWaitingConfirmedBooking(String bookingNumber) {
        // Validate and retrieve the booking, ensuring it belongs to the authenticated car owner
        Booking booking = validateAndGetBookingCarOwner(bookingNumber);

        // Ensure the booking status is WAITING_CONFIRMED before proceeding
        if (!EBookingStatus.WAITING_CONFIRMED.equals(booking.getStatus())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }

        // Update the booking status to CANCELLED
        booking.setStatus(EBookingStatus.CANCELLED);
        booking.setUpdateBy(SecurityUtil.getCurrentAccount().getId());

        bookingRepository.saveAndFlush(booking);

        // Process the refund for the booking deposit
        transactionService.refundAllDeposit(booking);
        emailService.sendCancelledBookingEmail(booking.getAccount().getEmail(),
                booking.getCar().getBrand() + " " + booking.getCar().getModel(),
                "This booking was declined by car owner");
        // Return the updated booking details as a response
        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
    }

    /**
     * Handles the process for a car owner to reject a booking with status WAITING_CONFIRMED_RETURN_CAR.
     * This method updates the booking status to IN_PROGRESS instead of canceling it.
     *
     * @param bookingNumber The booking number of the booking with status WAITING_CONFIRMED_RETURN_CAR.
     * @return BookingResponse containing the updated booking details.
     */
    public BookingResponse rejectWaitingConfirmedEarlyReturnCarBooking(String bookingNumber) {
        // Validate and retrieve the booking, ensuring it belongs to the authenticated car owner
        Booking booking = validateAndGetBookingCarOwner(bookingNumber);

        // Ensure the booking status is WAITING_CONFIRMED_RETURN_CAR before proceeding
        if (!EBookingStatus.WAITING_CONFIRMED_RETURN_CAR.equals(booking.getStatus())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }

        // Update the booking status to IN_PROGRESS instead of canceling
        booking.setStatus(EBookingStatus.IN_PROGRESS);
        booking.setUpdateBy(SecurityUtil.getCurrentAccount().getId());

        bookingRepository.saveAndFlush(booking);
        emailService.sendEarlyReturnRejectedEmail(booking.getAccount().getEmail(), bookingNumber);
        // Return the updated booking details as a response
        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
    }


    /**
     * Handles the process for operator to confirm the deposit with payment type bank/cash
     * pending->waiting confirm
     * @param bookingNumber The booking number of the booking with status PENDING DEPOSIT and payment type not wallet.
     */
    public BookingResponse confirmDeposit(String bookingNumber) {
        Booking booking = validateAndGetBookingOperator(bookingNumber);
        //change the status to waiting confirmed and save the booking,
        confirmBookingDepositNotUsingWallet(booking);
        booking.setUpdateBy(SecurityUtil.getCurrentAccount().getId());
        bookingRepository.saveAndFlush(booking);
        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
    }

    /**
     * Handles the process for operator to reject the deposit with payment type bank/cash
     * pending->cancelled
     * @param bookingNumber The booking number of the booking with status PENDING DEPOSIT and payment type not wallet.
     */
    public BookingResponse rejectDeposit(String bookingNumber) {
        Booking booking = validateAndGetBookingOperator(bookingNumber);
        // Retrieve customer and car owner email addresses
        String customerEmail = booking.getAccount().getEmail();

        //change the status to cancelled and save the booking,
        // then send email waiting confirmed to customer and owner
        booking.setStatus(EBookingStatus.CANCELLED);
        booking.setUpdateBy(SecurityUtil.getCurrentAccount().getId());
        bookingRepository.saveAndFlush(booking);
        emailService.sendCancelledBookingEmail(customerEmail,
                booking.getCar().getBrand() + " " + booking.getCar().getModel(),
                "operator reject your booking");
        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
    }

    /**
     * allows customer to pay deposit again(wallet) if before the customer was not enough balance in wallet
     * pending deposit->waiting confirm
     * @param bookingNumber The booking number of the booking with status PENDING DEPOSIT of this current account(customer)
     */
    public BookingResponse payDepositAgain(String bookingNumber) {
        //validate and get booking of customer
        Booking booking = validateAndGetBookingCustomer(bookingNumber);
        if(!EBookingStatus.PENDING_DEPOSIT.equals(booking.getStatus())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
        if(!EPaymentType.WALLET.equals(booking.getPaymentType())) {
            throw new AppException(ErrorCode.UNSUPPORTED_PAYMENT_TYPE);
        }
        booking.setUpdateBy(SecurityUtil.getCurrentAccount().getId());
        //pay deposit again - use account ID from booking since it's already saved
        String customerAccountId = booking.getAccount().getId();
        payBookingDepositUsingWallet(booking, customerAccountId);
        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
    }

    /**
     * allows customer to pay total payment again(wallet) if before the customer was not enough balance in wallet
     * pending payment->completed
     * @param bookingNumber The booking number of the booking with status PENDING PAYMENT of this current account(customer)
     */
    public BookingResponse payTotalPaymentAgain(String bookingNumber) {
        //validate and get booking of customer
        Booking booking = validateAndGetBookingCustomer(bookingNumber);
        if(!EBookingStatus.PENDING_PAYMENT.equals(booking.getStatus())) {
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
        //pay total payment again
        processPaymentAndFinalizeBooking(booking);
        return buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());
    }

    /**
     * Validates and retrieves a booking based on the booking number.
     * Ensures that the booking exists and belongs to the currently authenticated user with role operator.
     *
     * @param bookingNumber The unique identifier of the booking.
     * @return The validated Booking object.
     * @throws AppException If the booking is not found or the user does not have permission to access it.
     */
    private Booking validateAndGetBookingOperator(String bookingNumber) {
        // Retrieve the currently authenticated account(operator)
        Account account = SecurityUtil.getCurrentAccount();
        String accountId = account.getId();

        // Fetch the booking from the database using the booking number
        Booking booking = bookingRepository.findBookingByBookingNumber(bookingNumber);

        // If no booking is found, throw an exception
        if (booking == null) {
            log.info("Failed to access booking's information, bookingNo: {}, accessed by: {} (Operator)",
                    bookingNumber, accountId);
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND_IN_DB);
        }
        // operator can only change when booking is pending deposit
        if(!EBookingStatus.PENDING_DEPOSIT.equals(booking.getStatus())) {
            log.info("Failed to access booking's information due to invalid status, bookingNo: {}, accessed by: {} (Operator)",
                    bookingNumber, accountId);
            throw new AppException(ErrorCode.INVALID_BOOKING_STATUS);
        }
        // if payment type of booking is wallet -> operator not supported, the system is handle
        if(EPaymentType.WALLET.equals(booking.getPaymentType())) {
            log.info("Failed to access booking's information due to unsupported payment type, bookingNo: {}, accessed by: {} (Operator)",
                    bookingNumber, accountId);
            throw new AppException(ErrorCode.UNSUPPORTED_PAYMENT_TYPE);
        }
        return booking;
    }



    /**
     * Validates and retrieves a booking based on the booking number.
     * Ensures that the booking exists and belongs to the currently authenticated user with role customer.
     *
     * @param bookingNumber The unique identifier of the booking.
     * @return The validated Booking object.
     * @throws AppException If the booking is not found or the user does not have permission to access it.
     */
    private Booking validateAndGetBookingCustomer(String bookingNumber) {
        // Retrieve the currently authenticated account(customer)
        Account account = SecurityUtil.getCurrentAccount();

        // Fetch the booking from the database using the booking number
        Booking booking = bookingRepository.findBookingByBookingNumber(bookingNumber);

        // If no booking is found, throw an exception
        if (booking == null) {
            log.info("Failed to access booking's information, bookingNo: {}, accessed by: {} (Customer)",
                    bookingNumber, account.getId());
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND_IN_DB);
        }
        // Ensure the booking belongs to the current authenticated user
        if (!booking.getAccount().getId().equals(account.getId())) {
            log.info("Failed to access booking's information due to forbidden access, bookingNo: {}, accessed by: {} (Customer)",
                    bookingNumber, account.getId());
            throw new AppException(ErrorCode.FORBIDDEN_BOOKING_ACCESS);
        }
        return booking;
    }

    /**
     * Validates and retrieves a booking based on the booking number.
     * <p>
     * This method ensures that:
     * - The booking exists in the database.
     * - The currently authenticated user (car owner) is the owner of the car in the booking.
     * </p>
     *
     * @param bookingNumber The unique identifier of the booking.
     * @return The validated {@link Booking} object if all conditions are met.
     * @throws AppException If the booking is not found or the authenticated user does not own the car in the booking.
     */
    private Booking validateAndGetBookingCarOwner(String bookingNumber) {
        // Retrieve the booking from the database using the booking number
        Booking booking = bookingRepository.findBookingByBookingNumber(bookingNumber);

        // If no booking is found, throw an exception indicating that the booking does not exist
        if (booking == null) {
            log.info("Failed to access booking's information, bookingNo: {}, accessed by: {} (Car Owner)",
                    bookingNumber, SecurityUtil.getCurrentAccountId());
            throw new AppException(ErrorCode.BOOKING_NOT_FOUND_IN_DB);
        }

        // Get the currently authenticated user's account ID (car owner)
        String accountId = SecurityUtil.getCurrentAccountId();

        // Check if the booking belongs to the current car owner
        if (!booking.getCar().getAccount().getId().equals(accountId)) {
            // If the user is not the car owner, throw an exception indicating forbidden access
            log.info("Failed to access booking's information due to forbidden access, bookingNo: {}, accessed by: {} (Car Owner)",
                    bookingNumber, accountId);
            throw new AppException(ErrorCode.FORBIDDEN_CAR_ACCESS);
        }
        // Return the validated booking object
        return booking;
    }

    /**
     * Processes the final payment for a booking and updates its status accordingly.
     * <p>
     * - If the customer has enough balance to cover the remaining amount, the payment is completed,
     *   and both the customer and car owner receive confirmation emails.
     * - If the customer does not have enough balance, the booking status is set to PENDING_PAYMENT,
     *   and a reminder email is sent.
     * </p>
     *
     * @param booking The booking for which payment needs to be processed.
     * @throws AppException if the customer's wallet is not found.
     */
    private void processPaymentAndFinalizeBooking(Booking booking) {
        // Retrieve customer and car owner email addresses
        String customerEmail = booking.getAccount().getEmail();
        String carOwnerEmail = booking.getCar().getAccount().getEmail();

        // Retrieve the customer's wallet, throw an exception if not found
        Wallet walletCustomer = walletRepository.findById(booking.getAccount().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND_IN_DB));

        // Build booking response to get necessary details
        BookingResponse response = buildBookingResponse(booking, booking.getDriverDrivingLicenseUri());

        // Calculate total payment amount
        long totalPayment = response.getTotalPrice();
        // Calculate the car owner's share (92% of total payment)
        long carOwnerShare = (long) (0.92 * totalPayment);
        // Calculate the remaining money after deducting the total payment from the deposit
        long remainingMoney = booking.getDeposit() - totalPayment;

        // If the remaining amount is negative and the customer's wallet balance is insufficient
        if (remainingMoney < 0 && walletCustomer.getBalance() < -remainingMoney) {
            // Set booking status to pending payment
            booking.setStatus(EBookingStatus.PENDING_PAYMENT);
            // Email the customer notifying them of the pending payment
            emailService.sendPendingPaymentEmail(customerEmail, booking.getBookingNumber(), -remainingMoney);
        } else {
            // Process the final payment transaction
            transactionService.offsetFinalPayment(booking);
            // Mark the booking as completed
            booking.setStatus(EBookingStatus.COMPLETED);
            // Email the car owner about the payment they received
            emailService.sendPaymentEmailToCarOwner(carOwnerEmail, booking.getBookingNumber(), carOwnerShare);
            // Email the customer confirming the payment and remaining balance (if any)
            emailService.sendPaymentEmailToCustomer(
                    customerEmail,
                    booking.getBookingNumber(),
                    remainingMoney,
                    remainingMoney >= 0
            );
        }
        booking.setUpdateBy(SecurityUtil.getCurrentAccount().getId());

        // Save the updated booking status to the database
        bookingRepository.saveAndFlush(booking);
    }


    /**
     * process overdue pick up time and drop off time to reminder customer
     */
    public void processOverduePickUpAndDropOffBookings() {
        //find all booking status CONFIRMED with pick up time <= now
        List<Booking> overdueBookingPickUps = bookingRepository.findOverduePickups(EBookingStatus.CONFIRMED, LocalDateTime.now());
        //find all booking status IN_PROGRESS with drop off time <= now
        List<Booking> overdueBookingDropOffs = bookingRepository.findOverdueDropOffs(EBookingStatus.IN_PROGRESS, LocalDateTime.now());
        //send email reminder

        overdueBookingPickUps.forEach(emailService::sendPickUpReminderEmail);
        overdueBookingDropOffs.forEach(emailService::sendDropOffReminderEmail);
    }

    /**
     * process overdue waiting in status WAITING CONFIRMED AND WAITING CONFIRMED RETURN CAR to auto handle when owner miss
     */
    public void processOverdueWaitingBookings() {
        LocalDateTime now = LocalDateTime.now();
        //waiting confirm -> cancelled, refund full

        //find all booking will cancel to refund
        List<Booking> overdueWaitingConfirmBookings = bookingRepository.findOverduePickups(EBookingStatus.WAITING_CONFIRMED, now);
        //update all bookings status WAITING CONFIRMED with pick up time <= now + 1 minute
        int updatedWaitingConfirmBookings = bookingRepository.bulkUpdateWaitingConfirmedStatus(EBookingStatus.CANCELLED
                , EBookingStatus.WAITING_CONFIRMED, now.minusMinutes(1));
        // If any bookings were updated, process refunds and send cancellation emails
        if (updatedWaitingConfirmBookings > 0) {
            for(Booking booking : overdueWaitingConfirmBookings) { // Loop through each overdue booking
                transactionService.refundAllDeposit(booking);
                emailService.sendCancelledBookingEmail(booking.getAccount().getEmail(),
                        booking.getCar().getBrand() + " " + booking.getCar().getModel(),
                        "This booking was declined by car owner");
            }
        }

        //waiting confirm return car -> in-progress
        //update all bookings status WAITING CONFIRMED RETURN CAR with drop off time <= now - 1 minute
        List<Booking> overdueWaitingConfirmReturnCarBookings = bookingRepository.findOverdueDropOffs(EBookingStatus.WAITING_CONFIRMED_RETURN_CAR, now);
        int updatedWaitingConfirmedReturn = bookingRepository.bulkUpdateWaitingConfirmedReturnCarStatus(EBookingStatus.IN_PROGRESS
                , EBookingStatus.WAITING_CONFIRMED_RETURN_CAR, now.minusMinutes(1));
        // If any bookings were updated, send email notifications
        if(updatedWaitingConfirmedReturn > 0) {
            for(Booking booking : overdueWaitingConfirmReturnCarBookings) { // Loop through each overdue booking
                emailService.sendEarlyReturnRejectedEmail(booking.getAccount().getEmail(), booking.getBookingNumber());
            }
        }
    }
}

