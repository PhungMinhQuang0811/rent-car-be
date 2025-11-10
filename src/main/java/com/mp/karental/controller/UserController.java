package com.mp.karental.controller;

import com.mp.karental.dto.request.user.AccountRegisterRequest;
import com.mp.karental.dto.request.user.CheckUniqueEmailRequest;
import com.mp.karental.dto.request.user.EditPasswordRequest;
import com.mp.karental.dto.request.user.EditProfileRequest;
import com.mp.karental.dto.response.ApiResponse;
import com.mp.karental.dto.response.user.EditProfileResponse;
import com.mp.karental.dto.response.user.UserResponse;
import com.mp.karental.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperties;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for handling user-related operations.
 * <p>
 * This controller provides endpoints for user management functionalities,
 * including user registration.
 * </p>
 *
 * @author DieuTTH4
 * @version 1.0
 */
@RestController
@RequestMapping(value = "/user", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Validated
@Slf4j
@Tag(name = "User", description = "API for managing user")
public class UserController {

    UserService userService;
    private final NamedParameterJdbcTemplate namedParameterJdbcTemplate;

    /**
     * Registers a new user account.
     * <p>
     * This method accepts a validated {@code AccountRegisterRequest} containing
     * the registration details, delegates the account creation to the {@code UserService},
     * and wraps the resulting {@code UserResponse} in a standardized {@code ApiResponse}.
     * </p>
     *
     * @param request the registration details for the new account
     * @return an {@code ApiResponse} containing the created user information
     * @author DieuTTH4
     */
    @Operation(
            summary = "Create a new account",
            description = "User create a new account with role Customer or Car Owner",
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
                                                    schema = @Schema(type = "string", example = "Create account successfully. Please check your email inbox to verify your email address.")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object", implementation = UserResponse.class)
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
                                    | 2000 | {fieldName} is required.|
                                    | 2001 | The full name can only contain alphabet characters.|
                                    | 2002 | Please enter a valid email address. |
                                    | 2003 | Email already existed. Please try another email. |
                                    | 2004 | Invalid phone number. |
                                    | 2005 | The phone number already existed. Please try another phone number. |
                                    | 2006 | Password must contain at least one number, one numeral, and seven characters. |
                                    | 3002 | The entity role requested is not found in the database. |
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3005 | There was error during sending verify email fail, please try again.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )

    @PostMapping("/register")
    public ApiResponse<UserResponse> registerAccount(@RequestBody @Valid AccountRegisterRequest request) {
        log.info("Registering account {}", request);
        return ApiResponse.<UserResponse>builder()
                .message("Create account successfully. Please check your email inbox to verify your email address.")
                .data(userService.addNewAccount(request))
                .build();
    }

    /**
     * this method check whether the email exist in the db or not
     *
     * @param request Object contain email
     * @return ApiResponse Object
     */
    @Operation(
            summary = "Check whether the email has existed in the database",
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
                                                    schema = @Schema(type = "string", example = "Email is unique.")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object")
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
                                    | 2000 | {fieldName} is required.|
                                    | 2002 | Please enter a valid email address. |
                                    | 2003 | Email already existed. Please try another email. |
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @PostMapping("/check-unique-email")
    public ApiResponse<String> checkUniqueEmail(@RequestBody @Valid CheckUniqueEmailRequest request) {
        return ApiResponse.<String>builder()
                .message("Email is unique.")
                .build();
    }

    @Operation(
            summary = "Resend verify email",
            description = "Resend verify email to the email address in the api",
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
                                                    schema = @Schema(type = "string", example = "The verify email " +
                                                            "is sent successfully. Please check your inbox again and " +
                                                            "follow instructions to verify your email.")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object")
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
                                    | 3022 | The email address you’ve entered does not exist. Please try again.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3005 | There was error during sending verify email fail, please try again.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @GetMapping("/resend-verify-email/{email}")
    public ApiResponse<String> resendVerifyEmail(@PathVariable("email")
                                                 @Email(message = "INVALID_EMAIL")
                                                 String email) {
        return ApiResponse.<String>builder()
                .message(userService.resendVerifyEmail(email))
                .build();
    }

    @Operation(
            summary = "Verify email",
            description = "Verify whether the email that user used to register account is valid",
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
                                                    schema = @Schema(type = "string", example = "Verify email successfully!" +
                                                            " Now you can use your account to login.")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "object")
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
                                    | 3022 | The email address you’ve entered does not exist. Please try again.|
                                    | 4011 | The token is invalid or this link has expired or has been used.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @GetMapping("/verify-email")
    public ApiResponse<String> verifyEmail(@RequestParam("t") String verifyEmailToken) {
        userService.verifyEmail(verifyEmailToken);
        return ApiResponse.<String>builder()
                .message("Verify email successfully! Now you can use your account to login.")
                .build();
    }

    /**
     * API to edit user profile
     *
     * @param request the new profile information
     * @return an ApiResponse containing updated user information
     */
    @PutMapping(value = "/edit-profile", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<EditProfileResponse> editProfile(@ModelAttribute @Valid EditProfileRequest request) {
        log.info("Editing profile for user: {}", request);
        return ApiResponse.<EditProfileResponse>builder()
                .data(userService.editProfile(request))
                .build();

    }

    /**
     * API to get user profile.
     *
     * @return an ApiResponse containing user profile information
     */
    @GetMapping("/edit-profile")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CAR_OWNER')")
    public ResponseEntity<ApiResponse<EditProfileResponse>> getUserProfile() {
        log.info("Fetching user profile");
        return ResponseEntity.ok(
                ApiResponse.<EditProfileResponse>builder()
                        .data(userService.getUserProfile())
                        .build()
        );
    }


    /**
     * Changes the password of the current user.
     *
     * @param request the request containing current, new, and confirm passwords
     * @return a response indicating success or failure
     */
    @PutMapping("/edit-password")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'CAR_OWNER')")
    public ApiResponse<String> editPassword(@RequestBody @Valid EditPasswordRequest request) {
        log.info("Changing password for user");
        userService.editPassword(request);
        return ApiResponse.<String>builder()
                .message("Password updated successfully")
                .build();
    }

    /**
     * API to get all users with pagination for operator.
     *
     * @param page   The page number (0-based index).
     * @param size   The number of records per page.
     * @param sort   Sorting criteria in the format "field,direction".
     * @param role   Optional role filter (CUSTOMER, CAR_OWNER, OPERATOR).
     * @return An ApiResponse containing paginated user list.
     */
    @Operation(
            summary = "Get all users with pagination",
            description = "Operator can retrieve all users with pagination, sorting, and role filtering",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @GetMapping("/operator/list")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<Page<UserResponse>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String role) {
        log.info("Operator requesting all users - page: {}, size: {}, sort: {}, role: {}", page, size, sort, role);
        Page<UserResponse> users = userService.getAllUsersForOperator(page, size, sort, role);
        return ApiResponse.<Page<UserResponse>>builder()
                .data(users)
                .build();
    }

    /**
     * API to get a user by ID for operator.
     *
     * @param userId The ID of the user to retrieve.
     * @return An ApiResponse containing user details.
     */
    @Operation(
            summary = "Get user by ID",
            description = "Operator can retrieve a specific user's details by ID",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "User not found",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @GetMapping("/operator/{userId}")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<UserResponse> getUserById(@PathVariable String userId) {
        log.info("Operator requesting user with id: {}", userId);
        UserResponse user = userService.getUserByIdForOperator(userId);
        return ApiResponse.<UserResponse>builder()
                .data(user)
                .build();
    }

    /**
     * API to update a user by operator.
     *
     * @param userId  The ID of the user to update.
     * @param request The updated user information.
     * @return An ApiResponse containing updated user information.
     */
    @Operation(
            summary = "Update user by operator",
            description = "Operator can update a user's profile information",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Bad request - validation errors",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @PutMapping(value = "/operator/{userId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<EditProfileResponse> updateUser(
            @PathVariable String userId,
            @ModelAttribute @Valid EditProfileRequest request) {
        log.info("Operator updating user with id: {}", userId);
        EditProfileResponse response = userService.updateUserByOperator(userId, request);
        return ApiResponse.<EditProfileResponse>builder()
                .data(response)
                .message("User updated successfully")
                .build();
    }

    /**
     * API to deactivate a user by operator.
     *
     * @param userId The ID of the user to deactivate.
     * @return An ApiResponse indicating success.
     */
    @Operation(
            summary = "Deactivate user",
            description = "Operator can deactivate a user account",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "User not found",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @PutMapping("/operator/{userId}/deactivate")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<String> deactivateUser(@PathVariable String userId) {
        log.info("Operator deactivating user with id: {}", userId);
        userService.deactivateUser(userId);
        return ApiResponse.<String>builder()
                .message("User deactivated successfully")
                .build();
    }

    /**
     * API to activate a user by operator.
     *
     * @param userId The ID of the user to activate.
     * @return An ApiResponse indicating success.
     */
    @Operation(
            summary = "Activate user",
            description = "Operator can activate a user account",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Success",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "User not found",
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @PutMapping("/operator/{userId}/activate")
    @PreAuthorize("hasRole('OPERATOR')")
    public ApiResponse<String> activateUser(@PathVariable String userId) {
        log.info("Operator activating user with id: {}", userId);
        userService.activateUser(userId);
        return ApiResponse.<String>builder()
                .message("User activated successfully")
                .build();
    }

}
