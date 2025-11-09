package com.mp.karental.service;

import com.mp.karental.constant.EBookingStatus;
import com.mp.karental.constant.ECarStatus;
import com.mp.karental.dto.request.car.AddCarRequest;
import com.mp.karental.dto.request.car.CarDetailRequest;
import com.mp.karental.dto.request.car.EditCarRequest;
import com.mp.karental.dto.request.car.SearchCarRequest;
import com.mp.karental.dto.response.car.CarDetailResponse;
import com.mp.karental.dto.response.car.CarDocumentsResponse;
import com.mp.karental.dto.response.car.CarResponse;
import com.mp.karental.dto.response.car.CarThumbnailResponse;
import com.mp.karental.entity.Account;
import com.mp.karental.entity.Booking;
import com.mp.karental.entity.Car;
import com.mp.karental.exception.AppException;
import com.mp.karental.exception.ErrorCode;
import com.mp.karental.mapper.CarMapper;
import com.mp.karental.repository.BookingRepository;
import com.mp.karental.repository.CarRepository;
import com.mp.karental.repository.FeedbackRepository;
import com.mp.karental.security.SecurityUtil;
import com.mp.karental.util.RedisUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

/**
 * Service class for handling car operations.
 *
 * @author QuangPM20, AnhPH9
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CarService {

    CarRepository carRepository;
    CarMapper carMapper;
    FileService fileService;
    BookingRepository bookingRepository;
    EmailService emailService;
    RedisUtil redisUtil;

    // Define constant field names to avoid repetition
    private static final String FIELD_PRODUCTION_YEAR = "productionYear";
    private static final String FIELD_PRICE = "basePrice";
    private final FeedbackRepository feedbackRepository;

    /**
     * Adds a new car to the system.
     *
     * @param request The request object containing car details.
     * @return The response object containing the newly added car details.
     * @throws AppException If the account is not found in the database.
     */
    public CarResponse addNewCar(AddCarRequest request) throws AppException {
        // Get the current user account id
        String accountId = SecurityUtil.getCurrentAccountId();

        // Retrieve the account from the database, throw an exception if not found
        Account account = SecurityUtil.getCurrentAccount();

        // Map the request data to a Car entity
        Car car = carMapper.toCar(request);

        // Associate the car with the current account
        car.setAccount(account);
        car.setCreatedAt(LocalDateTime.now());

        // Set default status for the new car
        car.setStatus(ECarStatus.NOT_VERIFIED);

        // Set transmission and fuel type based on request
        car.setAutomatic(request.isAutomatic());
        car.setGasoline(request.isGasoline());
        car.setUpdateBy(accountId);

        // Set car address components from request
        setCarAddress(request, car);

        // Save the initial car entity in the database
        car = carRepository.save(car);

        // Process and upload car-related files
        processUploadFiles(request, accountId, car);

        // Save the updated car entity after processing files
        car = carRepository.save(car);

        // Map the saved car entity to a response object
        CarResponse carResponse = carMapper.toCarResponse(car);

        // Set address information in the response
        carResponse.setAddress(request.getAddress());

        // Set car ID in the response
        carResponse.setId(car.getId());

        // Generate and set file URLs for the response
        setCarResponseUrls(carResponse, car);

        // Return the response with the saved car details
        return carResponse;
    }

    /**
     * Edits an existing car's details.
     *
     * @param request The request object containing the updated car details.
     * @param id      The ID of the car to be edited.
     * @return The response object containing the updated car details.
     * @throws AppException If the account or car is not found in the database.
     */
    public CarResponse editCar(EditCarRequest request, String id) throws AppException {
        // Retrieve the current user account ID to ensure the user is logged in
        String accountId = SecurityUtil.getCurrentAccountId();

        // Retrieve the car entity from the database using the provided car ID
        // If the car does not exist, throw an exception
        Car car = carRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.CAR_NOT_FOUND_IN_DB));
        ECarStatus currentStatus = car.getStatus();
        // Update the car's status
        ECarStatus newStatus = request.getStatus();
        if (newStatus == null) {
            newStatus = car.getStatus(); // Keep the existing status if none is provided
        }

        // Ensure that the currently logged-in user is the owner of the car
        // Prevents unauthorized users from modifying someone else's car
        if (!car.getAccount().getId().equals(accountId)) {
            throw new AppException(ErrorCode.FORBIDDEN_CAR_ACCESS);
        }
        //check if car is booked, can not stop the car
        if (newStatus == ECarStatus.STOPPED && isCarCurrentlyBooked(car.getId())) {
            throw new AppException(ErrorCode.CAR_CANNOT_STOPPED); // Cannot stop a car that has active bookings.
        }
        // Update the car details using the request data
        carMapper.editCar(car, request);

        if (!isValidStatusChange(currentStatus, newStatus)){
            throw new AppException(ErrorCode.INVALID_CAR_STATUS_CHANGE);
        }

        car.setStatus(newStatus); // Convert status to uppercase for consistency
        // when new status is stopped, all bookings of the car in status pending-deposit is cancelled
        if (newStatus == ECarStatus.STOPPED) {
            cancelPendingDepositsForStoppedCar(car.getId());
        }
        car.setUpdateBy(accountId);
        // Update the car's address details
        setCarAddress(request, car);

        // Process and upload any new files associated with the car
        processUploadFiles(request, accountId, car);

        // Save the updated car details in the database
        car = carRepository.save(car);

        // Convert the updated car entity into a response object
        CarResponse carResponse = carMapper.toCarResponse(car);

        // Set the address in the response object
        carResponse.setAddress(request.getAddress());

        // Generate and attach file URLs to the response
        setCarResponseUrls(carResponse, car);

        // Return the updated car response
        return carResponse;
    }

    /**
     * Checks if the status change from the current status to the new status is valid.
     *
     * @param currentStatus The current status of the car.
     * @param newStatus     The new status to be checked.
     * @return true if the status change is valid, false otherwise.
     */
    private boolean isValidStatusChange(ECarStatus currentStatus, ECarStatus newStatus) {
        if (currentStatus == newStatus) {
            return true;  // If the current status and new status are the same, return true (valid).
        }
        return switch (currentStatus) {  // Switch statement to handle transitions based on the current status.
            case NOT_VERIFIED, VERIFIED -> newStatus == ECarStatus.STOPPED;  // Can transition to STOPPED.
            case STOPPED -> newStatus == ECarStatus.NOT_VERIFIED;  // Can transition to NOT_VERIFIED.
        };
    }

    /**
     * This method to check the car currently booked or not
     * @param carId the id of the car
     * @return true if the car is booked by account ID, false otherwise
     */
    private boolean isCarCurrentlyBooked(String carId) {
        // 2 status is not booked
        List<EBookingStatus> excludedStatuses = Arrays.asList(EBookingStatus.CANCELLED, EBookingStatus.PENDING_DEPOSIT);
        return bookingRepository.hasActiveBooking(carId,excludedStatuses);
    }
    /**
     * Cancels all pending deposit bookings for a car that has been stopped.
     * Updates the status of affected bookings, notifies customers via email,
     * and removes cached pending deposit bookings.
     *
     * @param carId The ID of the car that has been stopped.
     * @throws AppException If an error occurs while sending cancellation emails.
     */
    private void cancelPendingDepositsForStoppedCar(String carId) {
        // Fetch only pending deposit bookings for this specific car
        List<Booking> pendingDeposits = bookingRepository.findByCarIdAndStatus(carId, EBookingStatus.PENDING_DEPOSIT);

        // If no pending deposits exist, log and return
        if (pendingDeposits.isEmpty()) {
            log.info("No pending deposit bookings found for car ID: {}", carId);
            return;
        }

        // Process each pending deposit booking
        for (Booking booking : pendingDeposits) {
            // Update booking status to cancelled
            booking.setStatus(EBookingStatus.CANCELLED);
            bookingRepository.save(booking); //  Persist changes

            // Prepare cancellation reason message
            String reason = "Your booking " + booking.getBookingNumber() + " was automatically canceled because the car owner has stopped rent the car. Please choose another car.";

            // Send cancellation email to the customer
            emailService.sendCancelledBookingEmail(booking.getAccount().getEmail(),
                    booking.getCar().getBrand() + " " + booking.getCar().getModel(),
                    reason );

            // Remove the cached pending deposit booking from Redis
            redisUtil.removeCachePendingDepositBooking(booking.getBookingNumber());

            // Log the cancellation
            log.info("Booking {} has been cancelled due to car {} being stopped.", booking.getBookingNumber(), carId);
        }
    }

    /**
     * Set URLs for car response to avoid duplication.
     */
    private void setCarResponseUrls(CarResponse response, Car car) {
        response.setRegistrationPaperUrl(fileService.getFileUrl(car.getRegistrationPaperUri()));
        response.setCertificateOfInspectionUrl(fileService.getFileUrl(car.getCertificateOfInspectionUri()));
        response.setInsuranceUrl(fileService.getFileUrl(car.getInsuranceUri()));
        response.setCarImageFrontUrl(fileService.getFileUrl(car.getCarImageFront()));
        response.setCarImageBackUrl(fileService.getFileUrl(car.getCarImageBack()));
        response.setCarImageLeftUrl(fileService.getFileUrl(car.getCarImageLeft()));
        response.setCarImageRightUrl(fileService.getFileUrl(car.getCarImageRight()));
    }

    /**
     * Extracts and sets the address components of a car from the request object.
     *
     * @param request The request object containing the address string.
     * @param car     The car entity to which the address will be assigned.
     * @throws AppException If the address format is incorrect.
     */
    private void setCarAddress(Object request, Car car) throws AppException {
        String address = null;

        // Check if the request is an instance of AddCarRequest and retrieve the address
        if (request instanceof AddCarRequest addCarRequest) {
            address = addCarRequest.getAddress();
        }
        // Check if the request is an instance of EditCarRequest and retrieve the address
        else if (request instanceof EditCarRequest editCarRequest) {
            address = editCarRequest.getAddress();
        }

        // If an address is provided and is not empty, split it into components
        if (address != null && !address.isEmpty()) {
            // Split the address into 4 parts: City/Province, District, Ward, House Number & Street
            String[] addressParts = address.split(",", 4);

            // Assign each part to the corresponding car attributes, ensuring to trim whitespace
            car.setCityProvince(addressParts[0].trim());   // First part: City/Province
            car.setDistrict(addressParts[1].trim());       // Second part: District
            car.setWard(addressParts[2].trim());           // Third part: Ward
            car.setHouseNumberStreet(addressParts[3].trim()); // Fourth part: House Number & Street
        }
    }

    /**
     * Handles file uploads for a car and assigns the corresponding URIs.
     *
     * @param request   The request object containing the uploaded files.
     * @param accountId The ID of the account uploading the files.
     * @param car       The car entity to which the file URIs will be assigned.
     * @throws AppException If file upload fails.
     */
    private void processUploadFiles(Object request, String accountId, Car car) throws AppException {

        // Generate base URIs for storing documents and images in S3
        String baseDocumentsUri = String.format("car/%s/%s/documents/", accountId, car.getId());
        String baseImagesUri = String.format("car/%s/%s/images/", accountId, car.getId());

        // Initialize file storage keys for car images
        String s3KeyImageFront;
        String s3KeyImageBack;
        String s3KeyImageLeft;
        String s3KeyImageRight;

        // Initialize document and image file variables
        MultipartFile registrationPaper;
        MultipartFile certificateOfInspection;
        MultipartFile insurance;
        MultipartFile imageFront;
        MultipartFile imageBack;
        MultipartFile imageLeft;
        MultipartFile imageRight;

        // If the request is an AddCarRequest, handle document and image uploads
        if (request instanceof AddCarRequest addCarRequest) {
            registrationPaper = addCarRequest.getRegistrationPaper();
            certificateOfInspection = addCarRequest.getCertificateOfInspection();
            insurance = addCarRequest.getInsurance();
            imageFront = addCarRequest.getCarImageFront();
            imageBack = addCarRequest.getCarImageBack();
            imageLeft = addCarRequest.getCarImageLeft();
            imageRight = addCarRequest.getCarImageRight();

            // Construct S3 keys for document files
            String s3KeyRegistration = baseDocumentsUri + "registration-paper" + fileService.getFileExtension(registrationPaper);
            String s3KeyCertificate = baseDocumentsUri + "certificate-of-inspection" + fileService.getFileExtension(certificateOfInspection);
            String s3KeyInsurance = baseDocumentsUri + "insurance" + fileService.getFileExtension(insurance);

            s3KeyImageFront = baseImagesUri + "front" + fileService.getFileExtension(imageFront);
            s3KeyImageBack = baseImagesUri + "back" + fileService.getFileExtension(imageBack);
            s3KeyImageLeft = baseImagesUri + "left" + fileService.getFileExtension(imageLeft);
            s3KeyImageRight = baseImagesUri + "right" + fileService.getFileExtension(imageRight);

            // Upload document files to S3
            fileService.uploadFile(registrationPaper, s3KeyRegistration);
            fileService.uploadFile(certificateOfInspection, s3KeyCertificate);
            fileService.uploadFile(insurance, s3KeyInsurance);

            fileService.uploadFile(imageFront, s3KeyImageFront);
            fileService.uploadFile(imageBack, s3KeyImageBack);
            fileService.uploadFile(imageLeft, s3KeyImageLeft);
            fileService.uploadFile(imageRight, s3KeyImageRight);

            // Set document URIs in the car object
            car.setRegistrationPaperUri(s3KeyRegistration);
            car.setCertificateOfInspectionUri(s3KeyCertificate);
            car.setInsuranceUri(s3KeyInsurance);
            // Set image URIs in the car object
            car.setCarImageFront(s3KeyImageFront);
            car.setCarImageBack(s3KeyImageBack);
            car.setCarImageLeft(s3KeyImageLeft);
            car.setCarImageRight(s3KeyImageRight);
        }

        // If the request is an EditCarRequest, only update image files
        if (request instanceof EditCarRequest editCarRequest) {
            //when car status is not verify, owner can change the documents to have valid document to verify by operator
            if (car.getStatus() == ECarStatus.NOT_VERIFIED || car.getStatus() == ECarStatus.STOPPED) {
                if (editCarRequest.getRegistrationPaper() != null && !editCarRequest.getRegistrationPaper().isEmpty()) {
                    String s3KeyRegistration = baseDocumentsUri + "registration-paper" + fileService.getFileExtension(editCarRequest.getRegistrationPaper());
                    fileService.uploadFile(editCarRequest.getRegistrationPaper(), s3KeyRegistration);
                    car.setRegistrationPaperUri(s3KeyRegistration);
                }
                if (editCarRequest.getCertificateOfInspection() != null && !editCarRequest.getCertificateOfInspection().isEmpty()) {
                    String s3KeyCertificate = baseDocumentsUri + "certificate-of-inspection" + fileService.getFileExtension(editCarRequest.getCertificateOfInspection());
                    fileService.uploadFile(editCarRequest.getCertificateOfInspection(), s3KeyCertificate);
                    car.setCertificateOfInspectionUri(s3KeyCertificate);
                }
                if (editCarRequest.getInsurance() != null && !editCarRequest.getInsurance().isEmpty()) {
                    String s3KeyInsurance = baseDocumentsUri + "insurance" + fileService.getFileExtension(editCarRequest.getInsurance());
                    fileService.uploadFile(editCarRequest.getInsurance(), s3KeyInsurance);
                    car.setInsuranceUri(s3KeyInsurance);
                }
            }

            if (editCarRequest.getCarImageFront() != null && !editCarRequest.getCarImageFront().isEmpty()) {
                s3KeyImageFront = baseImagesUri + "front" + fileService.getFileExtension(editCarRequest.getCarImageFront());
                fileService.uploadFile(editCarRequest.getCarImageFront(), s3KeyImageFront);
                car.setCarImageFront(s3KeyImageFront);
            }
            if (editCarRequest.getCarImageBack() != null && !editCarRequest.getCarImageBack().isEmpty()) {
                s3KeyImageBack = baseImagesUri + "back" + fileService.getFileExtension(((EditCarRequest) request).getCarImageBack());
                fileService.uploadFile(((EditCarRequest) request).getCarImageBack(), s3KeyImageBack);
                car.setCarImageBack(s3KeyImageBack);
            }
            if (editCarRequest.getCarImageLeft() != null && !editCarRequest.getCarImageLeft().isEmpty()) {
                s3KeyImageLeft = baseImagesUri + "left" + fileService.getFileExtension(editCarRequest.getCarImageLeft());
                fileService.uploadFile(editCarRequest.getCarImageLeft(), s3KeyImageLeft);
                car.setCarImageLeft(s3KeyImageLeft);
            }
            if (editCarRequest.getCarImageRight() != null && !(editCarRequest.getCarImageRight().isEmpty())) {
                s3KeyImageRight = baseImagesUri + "right" + fileService.getFileExtension(editCarRequest.getCarImageRight());
                fileService.uploadFile(editCarRequest.getCarImageRight(), s3KeyImageRight);
                car.setCarImageRight(s3KeyImageRight);
            }
        }

    }

    /**
     * Retrieves a paginated list of cars belonging to the current user.
     *
     * @param page The page number.
     * @param size The number of cars per page.
     * @param sort The sorting criteria in the format "field,direction".
     * @return A paginated list of car thumbnails.
     */
    public Page<CarThumbnailResponse> getCarsByUserId(int page, int size, String sort) {
        String accountId = SecurityUtil.getCurrentAccountId();

        // Validate and create pagination parameters
        Pageable pageable = getPageable(page, size, sort);

        // Retrieve the list of cars owned by the current user
        Page<Car> cars = carRepository.findByAccountId(accountId, pageable);
        log.info("Successfully access to car information, number of records: {}, accessBy {}", cars.getTotalElements(), accountId);

        return cars.map(car -> {
            CarThumbnailResponse response = carMapper.toCarThumbnailResponse(car);
            response.setAddress(car.getWard() + ", " + car.getCityProvince());
            response.setCarImageFront(fileService.getFileUrl(car.getCarImageFront()));
            response.setCarImageBack(fileService.getFileUrl(car.getCarImageBack()));
            response.setCarImageLeft(fileService.getFileUrl(car.getCarImageLeft()));
            response.setCarImageRight(fileService.getFileUrl(car.getCarImageRight()));

            // Call the reusable method to get the rating
            response.setAverageRatingByCar(getAverageRatingByCar(car.getId()));

            long noOfRides = bookingRepository.countCompletedBookingsByCar(car.getId());
            response.setNoOfRides(noOfRides);

            return response;
        });
    }

    /**
     * Retrieves detailed information of a car and checks its booking status within a given time range.
     *
     * @param request CarDetailRequest object with carId, pickUp, and dropOff times.
     * @return CarDetailResponse containing car details, booking status, images, and address visibility.
     */
    public CarDetailResponse getCarDetail(CarDetailRequest request) {
        String accountId = SecurityUtil.getCurrentAccountId();

        // Validate that the pick-up date is before the drop-off date
        if (request.getPickUpTime().isAfter(request.getDropOffTime())) {
            log.info("Fail access to car's information, invalid date range, accessBy {}", accountId);
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        // Validate booking time requirements (don't throw error, but mark as unavailable if invalid)
        // This allows users to see car details even with invalid times, but understand why it's not available
        LocalDateTime now = LocalDateTime.now();
        boolean hasValidBookingTime = true;
        
        // Check if pickup time is at least 2 hours from now
        if (request.getPickUpTime().isBefore(now.plusHours(2))) {
            log.info("Pick-up time is less than 2 hours from now - PickUp: {}, Current time: {}, Minimum required: {}, accessBy {}", 
                    request.getPickUpTime(), now, now.plusHours(2), accountId);
            hasValidBookingTime = false;
        }
        
        // Check if pickup time is within allowed hours (06:00 - 22:00)
        LocalTime pickUpTime = request.getPickUpTime().toLocalTime();
        LocalTime PICKUP_START_TIME = LocalTime.of(6, 0);
        LocalTime PICKUP_END_TIME = LocalTime.of(22, 0);
        if (pickUpTime.isBefore(PICKUP_START_TIME) || pickUpTime.isAfter(PICKUP_END_TIME)) {
            log.info("Pick-up time is outside allowed hours - PickUp time: {}, Allowed range: {} - {}, accessBy {}", 
                    pickUpTime, PICKUP_START_TIME, PICKUP_END_TIME, accountId);
            hasValidBookingTime = false;
        }
        
        // Check if drop-off time is at least 2 hours after pick-up time
        if (request.getPickUpTime().isAfter(request.getDropOffTime().minusHours(2))) {
            log.info("Booking duration is less than 2 hours - PickUp: {}, DropOff: {}, Minimum duration: 2 hours, accessBy {}", 
                    request.getPickUpTime(), request.getDropOffTime(), accountId);
            hasValidBookingTime = false;
        }

        // Retrieve car details from the database
        Car car = carRepository.findById(request.getCarId())
                .orElseThrow(() -> {
                    log.info("Fail access to car's information, carId: {}, accessBy {}", request.getCarId(), accountId);
                    return new AppException(ErrorCode.CAR_NOT_FOUND_IN_DB);
                });

        double averageRating = getAverageRatingByCar(request.getCarId());

        // Check if the car is verified
        if (car.getStatus() != ECarStatus.VERIFIED) {
            log.info("Fail access to car's information, carId: {}, car is not verified, accessBy {}", request.getCarId(), accountId);
            throw new AppException(ErrorCode.CAR_NOT_VERIFIED);
        }

        //Check car is available (only if booking time is valid)
        boolean isAvailable = false;
        if (hasValidBookingTime) {
            isAvailable = isCarAvailable(request.getCarId(), request.getPickUpTime(), request.getDropOffTime());
        } else {
            // If booking time is invalid, car is not available regardless of bookings
            log.info("Car marked as unavailable due to invalid booking time requirements, carId: {}, accessBy {}", 
                    request.getCarId(), accountId);
        }

        // Map the car entity to a CarDetailResponse DTO
        CarDetailResponse response = carMapper.toCarDetailResponse(car, isAvailable);

        boolean isBooked = isCarBooked(request.getCarId(), accountId);

        if (isBooked) {
            // If the booking_status is COMPLETE, display the full address
            response.setAddress(car.getHouseNumberStreet() + ", "
                    + car.getWard() + ", "
                    + car.getDistrict() + ", "
                    + car.getCityProvince());

            // Allow viewing and downloading car-related documents
            response.setCertificateOfInspectionUrl(fileService.getFileUrl(car.getCertificateOfInspectionUri()));
            response.setInsuranceUrl(fileService.getFileUrl(car.getInsuranceUri()));
            response.setRegistrationPaperUrl(fileService.getFileUrl(car.getRegistrationPaperUri()));

        } else {
            // If the booking status is CANCELLED or PENDING_DEPOSIT, hide document URLs and provide a partial address
            response.setCertificateOfInspectionUrl(null);
            response.setInsuranceUrl(null);
            response.setRegistrationPaperUrl(null);

            // Provide a partial address with a message for unbooked cars
            response.setAddress(car.getDistrict() + ", " + car.getCityProvince()
                    + " (Full address will be available after you've paid the deposit to rent).");
        }

        // Retrieve verification status from the database instead of setting it to true
        response.setRegistrationPaperIsVerified(car.isRegistrationPaperUriIsVerified());
        response.setCertificateOfInspectionIsVerified(car.isCertificateOfInspectionUriIsVerified());
        response.setInsuranceIsVerified(car.isInsuranceUriIsVerified());

        // Retrieve and set the URLs for car-related images (these should always be visible)
        response.setCarImageFront(fileService.getFileUrl(car.getCarImageFront()));
        response.setCarImageBack(fileService.getFileUrl(car.getCarImageBack()));
        response.setCarImageLeft(fileService.getFileUrl(car.getCarImageLeft()));
        response.setCarImageRight(fileService.getFileUrl(car.getCarImageRight()));

        // Set booking status in the response
        response.setBooked(isBooked);

        // Set average rating follow by car
        response.setAverageRatingByCar(averageRating);

        // Count the number of completed bookings for this car and set it
        long noOfRides = bookingRepository.countCompletedBookingsByCar(request.getCarId());
        response.setNoOfRides(noOfRides);
        log.info("Successfully access to car's information, carId: {}, accessBy {}", request.getCarId(), accountId);

        return response;
    }

    /**
     * Checks if a car is available within the given time range.
     *
     * @param carId       the ID of the car
     * @param pickUpTime  the start time of the requested booking
     * @param dropOffTime the end time of the requested booking
     * @return true if the car is available, false otherwise
     */
    public boolean isCarAvailable(String carId, LocalDateTime pickUpTime, LocalDateTime dropOffTime) {
        // Get list booking in range (pickUp - 1 day) to (dropOff + 1 day) to catch any potential overlaps
        LocalDateTime searchStart = pickUpTime.minusDays(1);
        LocalDateTime searchEnd = dropOffTime.plusDays(1);

        List<Booking> bookings = bookingRepository.findActiveBookingsByCarIdAndTimeRange(carId, searchStart, searchEnd);
        log.info("Checking availability for Car ID: {} - PickUp: {}, DropOff: {}, Search range: {} to {}, Found {} bookings", 
                carId, pickUpTime, dropOffTime, searchStart, searchEnd, bookings.size());
        
        //check car if status is different with VERIFIED, the car is not available
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new AppException(ErrorCode.CAR_NOT_FOUND_IN_DB));
        if(car.getStatus() != ECarStatus.VERIFIED) {
            log.info("Car {} is not available - Status: {}", carId, car.getStatus());
            return false;
        }
        
        // Filter bookings to only those that actually overlap with the requested time range
        // Two time ranges overlap if: (pickUpTime < booking.dropOffTime AND dropOffTime > booking.pickUpTime)
        List<Booking> overlappingBookings = bookings.stream()
                .filter(booking -> {
                    boolean overlaps = pickUpTime.isBefore(booking.getDropOffTime()) 
                            && dropOffTime.isAfter(booking.getPickUpTime());
                    if (overlaps) {
                        log.info("Found overlapping booking for Car {} - Booking Number: {}, Status: {}, PickUp: {}, DropOff: {}", 
                                carId, booking.getBookingNumber(), booking.getStatus(), 
                                booking.getPickUpTime(), booking.getDropOffTime());
                    }
                    return overlaps;
                })
                .collect(java.util.stream.Collectors.toList());
        
        log.info("Car {} - Found {} bookings in search range, {} actually overlap with requested time", 
                carId, bookings.size(), overlappingBookings.size());
        
        // If there are no overlapping bookings -> Available
        if (overlappingBookings.isEmpty()) {
            log.info("Car {} is available - No overlapping bookings found", carId);
            return true;
        }
        
        // Check whether all overlapping bookings are CANCELED OR PENDING_DEPOSIT (inactive)
        boolean allInactive = overlappingBookings.stream()
                .allMatch(booking -> booking.getStatus() == EBookingStatus.CANCELLED
                        || booking.getStatus() == EBookingStatus.PENDING_DEPOSIT);
        
        if (allInactive) {
            log.info("Car {} is available - All {} overlapping bookings are inactive (CANCELLED or PENDING_DEPOSIT)", 
                    carId, overlappingBookings.size());
        } else {
            List<Booking> activeBookings = overlappingBookings.stream()
                    .filter(booking -> booking.getStatus() != EBookingStatus.CANCELLED 
                            && booking.getStatus() != EBookingStatus.PENDING_DEPOSIT)
                    .collect(java.util.stream.Collectors.toList());
            log.info("Car {} is not available - Found {} active bookings that conflict: {}", 
                    carId, activeBookings.size(), 
                    activeBookings.stream()
                            .map(b -> String.format("%s (Status: %s, %s to %s)", 
                                    b.getBookingNumber(), b.getStatus(), b.getPickUpTime(), b.getDropOffTime()))
                            .collect(java.util.stream.Collectors.joining(", ")));
        }
        
        return allInactive;
    }


    /**
     * Checks if a car has been booked by customer.
     *
     * @param carId     the ID of the car
     * @param accountId the ID of the customer
     * @return true if the car is booked by account ID, false otherwise
     */
    public boolean isCarBooked(String carId, String accountId) {
        List<EBookingStatus> activeStatuses = Arrays.asList(
                EBookingStatus.CONFIRMED,
                EBookingStatus.IN_PROGRESS,
                EBookingStatus.PENDING_PAYMENT,
                EBookingStatus.COMPLETED,
                EBookingStatus.WAITING_CONFIRMED,
                EBookingStatus.WAITING_CONFIRMED_RETURN_CAR
        );

        return bookingRepository.existsByCarIdAndAccountIdAndBookingStatusIn(carId, accountId, activeStatuses);
    }

    private static Pageable getPageable(int page, int size, String sort) {
        // Validate and limit size (maximum 100)
        if (size <= 0 || size > 100) {
            size = 10; // Default value if client provides an invalid input
        }

        // Ensure page number is non-negative (set to 0 if negative)
        if (page < 0) {
            page = 0;
        }

        // Define default sorting field and direction
        String sortField = FIELD_PRODUCTION_YEAR;
        Sort.Direction sortDirection = Sort.Direction.DESC;

        if (sort != null && !sort.isBlank()) {
            String[] sortParams = sort.split(",");
            if (sortParams.length == 2) {
                String requestedField = sortParams[0].trim();
                String requestedDirection = sortParams[1].trim().toUpperCase();

                // Check if requestedField valid
                if (List.of(FIELD_PRODUCTION_YEAR, FIELD_PRICE).contains(requestedField)) {
                    sortField = requestedField;
                }

                // Check if requestedDirection valid
                if (requestedDirection.equals("ASC") || requestedDirection.equals("DESC")) {
                    sortDirection = Sort.Direction.valueOf(requestedDirection);
                }
            }
        }

        return PageRequest.of(page, size, Sort.by(sortDirection, sortField));
    }

    /**
     * Search for cars based on address, time, and sorting criteria.
     *
     * @param request The search request containing address, pickup, and drop-off times.
     * @param page    The page number to retrieve.
     * @param size    The number of cars per page.
     * @param sort    The sorting string in the format "field,direction" (e.g., "productionYear,desc").
     * @return A paginated list of available cars.
     */

    public Page<CarThumbnailResponse> searchCars(SearchCarRequest request, int page, int size, String sort) {
        log.info("Search request received - Address: {}, PickUp: {}, DropOff: {}",
                request.getAddress(), request.getPickUpTime(), request.getDropOffTime());

        // Validate that pick-up date is before drop-off date
        if (request.getPickUpTime().isAfter(request.getDropOffTime())) {
            log.info("Invalid date range in search request - PickUp: {}, DropOff: {}", 
                    request.getPickUpTime(), request.getDropOffTime());
            throw new AppException(ErrorCode.INVALID_DATE_RANGE);
        }

        // Validate pick-up time: must be at least 2 hours from now (same as booking validation)
        LocalDateTime now = LocalDateTime.now();
        if (request.getPickUpTime().isBefore(now.plusHours(2))) {
            log.info("Invalid pick-up time in search request - PickUp: {}, Current time: {}, Minimum required: {}", 
                    request.getPickUpTime(), now, now.plusHours(2));
            throw new AppException(ErrorCode.INVALID_BOOKING_TIME);
        }

        // Validate that pick-up time is not more than 60 days in the future
        LocalDate today = now.toLocalDate();
        LocalDate pickUpDate = request.getPickUpTime().toLocalDate();
        if (pickUpDate.isAfter(today.plusDays(60))) {
            log.info("Invalid pick-up time in search request - PickUp date: {}, Maximum allowed: {}", 
                    pickUpDate, today.plusDays(60));
            throw new AppException(ErrorCode.INVALID_BOOKING_TIME);
        }

        // Validate pick-up time is within allowed hours (06:00 - 22:00)
        LocalTime pickUpTime = request.getPickUpTime().toLocalTime();
        LocalTime PICKUP_START_TIME = LocalTime.of(6, 0);
        LocalTime PICKUP_END_TIME = LocalTime.of(22, 0);
        if (pickUpTime.isBefore(PICKUP_START_TIME) || pickUpTime.isAfter(PICKUP_END_TIME)) {
            log.info("Invalid pick-up time in search request - PickUp time: {}, Allowed range: {} - {}", 
                    pickUpTime, PICKUP_START_TIME, PICKUP_END_TIME);
            throw new AppException(ErrorCode.INVALID_BOOKING_TIME);
        }

        // Validate that drop-off time is at least 2 hours after pick-up time
        if (request.getPickUpTime().isAfter(request.getDropOffTime().minusHours(2))) {
            log.info("Invalid booking duration in search request - PickUp: {}, DropOff: {}, Minimum duration: 2 hours", 
                    request.getPickUpTime(), request.getDropOffTime());
            throw new AppException(ErrorCode.INVALID_BOOKING_TIME);
        }

        // Get list car is VERIFIED with pagination
        List<Car> verifiedCars = carRepository.findVerifiedCarsByAddress(ECarStatus.VERIFIED, request.getAddress());

        // Filter to take car is AVAILABLE
        List<CarThumbnailResponse> availableCars = new ArrayList<>();

        for (Car car : verifiedCars) {
            log.info("Checking completed bookings for Car ID: {}", car.getId());

            boolean available = isCarAvailable(car.getId(), request.getPickUpTime(), request.getDropOffTime());
            if (!available) {
                continue;
            }

            long noOfRides = bookingRepository.countCompletedBookingsByCar(car.getId());
            log.info("Number of completed rides for Car ID {}: {}", car.getId(), noOfRides);

            CarThumbnailResponse response = carMapper.toSearchCar(car, noOfRides);
            response.setAddress(car.getCityProvince() + ", " + car.getDistrict() + ", " + car.getWard());

            // Get rating by car id
            response.setAverageRatingByCar(getAverageRatingByCar(car.getId()));

            // Get URL image car
            response.setCarImageFront(fileService.getFileUrl(car.getCarImageFront()));
            response.setCarImageBack(fileService.getFileUrl(car.getCarImageBack()));
            response.setCarImageLeft(fileService.getFileUrl(car.getCarImageLeft()));
            response.setCarImageRight(fileService.getFileUrl(car.getCarImageRight()));
            response.setNoOfRides(noOfRides);
            availableCars.add(response);
        }
        // Use pageable after filter
        Pageable pageable = getPageable(page, size, sort);
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), availableCars.size());
        List<CarThumbnailResponse> pagedCars = availableCars.subList(start, end);

        log.info("Successfully completed search request, total available cars: {}, accessBy: {}",
                availableCars.size(), SecurityUtil.getCurrentAccountId());

        return new PageImpl<>(pagedCars, pageable, availableCars.size());
    }

    /**
     * Retrieves a car by its ID and ensures that the current logged-in user owns the car.
     *
     * @param id The ID of the car to retrieve.
     * @return A CarResponse object containing the car details.
     * @throws AppException If the account is not found, the car is not found, or the user is unauthorized.
     */
    public CarResponse getCarById(String id) {
        // Retrieve the current user account ID to ensure the user is logged in
        String accountId = SecurityUtil.getCurrentAccountId();
        log.info("User {} is attempting to access car details with carId: {}", accountId, id);
        // Fetch the car details from the database, or throw an error if the car is not found
        Car car = carRepository.findById(id)
                .orElseThrow(() -> {
                    log.info("Failed to access car's information, carId: {}, accessed by: {}", id, accountId);
                    return new AppException(ErrorCode.CAR_NOT_FOUND_IN_DB);
                });

        // Check if the car belongs to the current logged-in user
        if (!car.getAccount().getId().equals(accountId)) {
            log.info("Failed to access car's information due to forbidden access, carId: {}, accessed by: {}", car.getId(), accountId);
            throw new AppException(ErrorCode.FORBIDDEN_CAR_ACCESS);
        }

        // Get average rating of car
        double averageRating = getAverageRatingByCar(car.getId());

        // Convert Car entity to CarResponse DTO
        CarResponse carResponse = carMapper.toCarResponse(car);

        carResponse.setAverageRatingByCar(averageRating);
        long noOfRides = bookingRepository.countCompletedBookingsByCar(car.getId());
        carResponse.setNoOfRides(noOfRides);

        // Concatenate address fields into a single string and set it in CarResponse
        carResponse.setAddress(car.getCityProvince() + ", " + car.getDistrict() + ", "
                + car.getWard() + ", " + car.getHouseNumberStreet());

        // Set file URLs for car images and documents
        setCarResponseUrls(carResponse, car);
        log.info("Successfully accessed car's information, carId: {}, accessed by: {}", car.getId(), accountId);
        return carResponse;
    }

    /**
     * Retrieves the average rating of a car based on user feedback.
     *
     * @param carId The ID of the car to fetch the average rating for.
     * @return The average rating if feedback exists; otherwise, returns 0.0.
     */
    public double getAverageRatingByCar(String carId) {
        Double averageRating = feedbackRepository.calculateAverageRatingByCar(carId);
        return (averageRating != null) ? averageRating : 0.0;
    }

    /**
     * Retrieves a paginated list of cars for the operator.
     * Supports filtering by status, sorting, and pagination.
     *
     * @param page   The requested page number (0-based index).
     * @param size   The number of records per page.
     * @param sort   Sorting criteria in the format "field,direction" (e.g., "updatedAt,desc").
     * @param status (Optional) The status filter for cars.
     * @return A page of CarThumbnailResponse objects containing summarized car information.
     */
    public Page<CarThumbnailResponse> getAllCarsForOperator(int page, int size, String sort, ECarStatus status) {
        log.info("Operator {} is requesting all cars with status {}", SecurityUtil.getCurrentAccountId(), status);

        // Create a pageable object with sorting based on the provided parameters
        Pageable pageable = createPageableForOperator(page, size, sort);

        // Fetch cars from the repository, filtering by status if provided
        Page<Car> cars = carRepository.findCars(status, pageable);
        log.info("Successfully retrieved {} cars for operator {}, status: {}", cars.getTotalElements(), SecurityUtil.getCurrentAccountId(), status);

        // Convert each Car entity into a CarThumbnailResponse DTO
        return cars.map(car -> {
            CarThumbnailResponse response = carMapper.toCarThumbnailResponse(car);

            // Construct address from the ward and cityProvince fields
            response.setAddress(car.getWard() + ", " + car.getCityProvince());

            // Retrieve and set car images from the file storage system
            response.setCarImageFront(fileService.getFileUrl(car.getCarImageFront()));
            response.setCarImageBack(fileService.getFileUrl(car.getCarImageBack()));
            response.setCarImageLeft(fileService.getFileUrl(car.getCarImageLeft()));
            response.setCarImageRight(fileService.getFileUrl(car.getCarImageRight()));

            // Get the average rating of the car based on user reviews
            response.setAverageRatingByCar(getAverageRatingByCar(car.getId()));

            // Retrieve the count of completed bookings for this car
            response.setNoOfRides(bookingRepository.countCompletedBookingsByCar(car.getId()));

            return response;
        });
    }

    /**
     * Creates a pageable object with sorting.
     * Ensures proper pagination and applies custom sorting criteria.
     *
     * @param page The requested page number (0-based index).
     * @param size The number of records per page.
     * @param sort Sorting criteria in the format "field,direction" (e.g., "updatedAt,desc").
     * @return Pageable object with sorting applied.
     */
    private Pageable createPageableForOperator(int page, int size, String sort) {
        // Ensure the page size is within a valid range (default 10, max 100)
        size = (size > 0 && size <= 100) ? size : 10;

        // Ensure the page index is non-negative
        page = Math.max(page, 0);

        // List of allowed fields for sorting
        List<String> allowedSortFields = List.of("updatedAt", "basePrice", "productionYear");

        Sort sortCriteria = null;

        // Check if sorting parameters are provided
        if (sort != null && !sort.isBlank()) {
            String[] sortParams = sort.split(",");
            if (sortParams.length == 2) {
                String requestedField = sortParams[0].trim();
                String requestedDirection = sortParams[1].trim().toUpperCase();
                Sort.Direction direction = "ASC".equals(requestedDirection) ? Sort.Direction.ASC : Sort.Direction.DESC;

                // Apply sorting only if the requested field is allowed
                if (allowedSortFields.contains(requestedField)) {
                    sortCriteria = Sort.by(direction, requestedField);
                }
            }
        }

        // If no valid sorting parameter is provided, default to sorting by updatedAt (descending)
        if (sortCriteria == null) {
            sortCriteria = Sort.by(Sort.Direction.DESC, "updatedAt");
        }

        return PageRequest.of(page, size, sortCriteria);
    }


    /**
     * Retrieves the document details of a specific car.
     *
     * <p>This method fetches the car entity from the database using the provided car ID.
     * If the car is not found, an exception is thrown.
     * The document URIs stored in the database are converted to URLs for access.</p>
     *
     * @param carId The unique identifier of the car whose documents need to be fetched.
     * @return A {@link CarDocumentsResponse} containing document URLs and verification statuses.
     * @throws AppException if the car is not found in the database.
     */
    @Transactional(readOnly = true)
    public CarDocumentsResponse getCarDocuments(String carId) {
        log.info("User {} is requesting documents for car {}", SecurityUtil.getCurrentAccountId(), carId);

        Car car = carRepository.findById(carId)
                .orElseThrow(() -> {
                    log.warn("Fail access to car's documents, carId: {}, accessBy: {}", carId, SecurityUtil.getCurrentAccountId());
                    return new AppException(ErrorCode.CAR_NOT_FOUND_IN_DB);
                });

        log.info("Successfully accessed car's documents, carId: {}, accessBy: {}", carId, SecurityUtil.getCurrentAccountId());

        // Map entity to DTO and update URLs
        return carMapper.toCarDocumentsResponse(car).toBuilder()
                .registrationPaperUrl(fileService.getFileUrl(car.getRegistrationPaperUri()))
                .certificateOfInspectionUrl(fileService.getFileUrl(car.getCertificateOfInspectionUri()))
                .insuranceUrl(fileService.getFileUrl(car.getInsuranceUri()))
                .build();
    }


    /**
     * Operator confirms a car verification by changing its status from NOT_VERIFIED to VERIFIED.
     *
     * @param carId The unique identifier of the car.
     * @return A {@link CarResponse} containing the updated car details.
     * @throws AppException if the car is not found or the status is invalid for verification.
     */
    @Transactional
    public String  verifyCar(String carId) {
        log.info("Operator {} is verifying car {}", SecurityUtil.getCurrentAccount().getId(), carId);

        // Retrieve car from database
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> {
                    log.warn("Fail access to car verification, carId: {}, accessBy: {}", carId, SecurityUtil.getCurrentAccountId());
                    return new AppException(ErrorCode.CAR_NOT_FOUND_IN_DB);
                });

        // Validate that the car is in NOT_VERIFIED status
        if (!ECarStatus.NOT_VERIFIED.equals(car.getStatus())) {
            log.warn("Invalid car status for verification, carId: {}, currentStatus: {}, accessBy: {}",
                    carId, car.getStatus(), SecurityUtil.getCurrentAccountId());
            throw new AppException(ErrorCode.INVALID_CAR_STATUS);
        }

        // Update status to VERIFIED
        car.setStatus(ECarStatus.VERIFIED);
        car.setInsuranceUriIsVerified(true);
        car.setRegistrationPaperUriIsVerified(true);
        car.setCertificateOfInspectionUriIsVerified(true);
        car.setUpdateBy(SecurityUtil.getCurrentAccount().getId());
        carRepository.saveAndFlush(car);

        //Send verification email to the car owner
        emailService.sendCarVerificationEmail(
                car.getAccount().getEmail(),
                car.getBrand() + " " + car.getModel(),
                carId
        );

        log.info("Car {} verified successfully by operator {}", carId, SecurityUtil.getCurrentAccount().getId());

        // Return updated car response
        return "Car has been verified successfully.";
    }
}

