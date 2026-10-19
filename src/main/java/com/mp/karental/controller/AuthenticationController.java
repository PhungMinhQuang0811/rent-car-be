package com.mp.karental.controller;

import com.mp.karental.dto.request.auth.ChangePasswordRequest;
import com.mp.karental.dto.request.auth.LoginRequest;
import com.mp.karental.dto.response.ApiResponse;
import com.mp.karental.dto.response.auth.LoginResponse;
import com.mp.karental.service.AuthenticationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Handle request relating to authentication and authorization
 * eg: login, logout, refresh token
 *
 * @author DieuTTH4
 *
 * @version 1.0
 */
@RestController
@RequestMapping(value = "/auth", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Validated
@Slf4j
@Tag(name = "Authentication", description = "API for authentication's operations")
public class AuthenticationController {

    AuthenticationService authenticationService;

    @Operation(
            summary = "Login",
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
                                                    schema = @Schema(type = "object", implementation = LoginResponse.class)
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
                                    | 2006 | Password must contain at least one number, one numeral, and seven characters. |
                                    | 3003 | The account is not exist in the system.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = """
                                    Unauthorized
                                    |code  | message |
                                    |------|-------------|
                                    | 4002 | Either email address or password is incorrect. Please try again.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 3005 | Your account is inactive.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody LoginRequest loginRequest) {
        return authenticationService.login(loginRequest);
    }

    @Operation(
            summary = "Refresh access token",
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
                                                    schema = @Schema(type = "string", example = "Successfully refresh token")
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
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "401",
                            description = """
                                    Unauthorized
                                    |code  | message |
                                    |------|-------------|
                                    | 4009 | Invalid refresh token. Please try again.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 3005 | Your account is inactive.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @PostMapping("/refresh-token")
    public ResponseEntity<ApiResponse<String>> refreshToken(HttpServletRequest request) {
        return authenticationService.refreshToken(request);
    }

    @Operation(
            summary = "Logout",
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
                                                    schema = @Schema(type = "string", example = "Successfully logged out")
                                            )
                                    }
                            )
                    )
            }
    )
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        return authenticationService.logout(request);
    }


    @Operation(
            summary = "Request to change password when forgot the password",
            description = "This api would send an email to user's email address to verify that the user are making change password request",
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
                                                    schema = @Schema(type = "string", example = "An email has been send " +
                                                            "to your email address. Please click to the link in the email " +
                                                            "to change password.")
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
                                    | 2002 | Please enter a valid email address. |
                                    | 3022 | The email address you’ve entered does not exist. Please try again.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 3005 | Your account is inactive.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "503",
                            description = """
                                    Service unavailable
                                    |code  | message |
                                    |------|-------------|
                                    | 3016 | There was error during sending forgot password email fail, please try again.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    //the user forgot password, send request to change the password
    @GetMapping("/forgot-password/{email}")
    public ApiResponse<String> sendForgotPasswordEmail(@PathVariable("email")
                                                         @Email(message = "INVALID_EMAIL")
                                                         String email) {
        authenticationService.sendForgotPasswordEmail(email);
        return ApiResponse.<String>builder()
                .message("An email has been send to your email address. Please click to the link in the email to change password.")
                .build();

    }

    @Operation(
            summary = "Verify change password request",
            description = "This api would verify that user really forgot password and making change password request.",
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
                                                    schema = @Schema(type = "string", example = "Verify change password request successfully!")
                                            ),
                                            @SchemaProperty(
                                                    name = "data",
                                                    schema = @Schema(type = "string", example = "{verify forgot password token}")
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
                                    | 4012 | This link has expired. Please go back to Homepage and try again. |
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 3005 | Your account is inactive.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @GetMapping("/forgot-password/verify")
    public ApiResponse<String> verifyForgotPassword(@RequestParam("t") String forgotPasswordToken) {
        return ApiResponse.<String>builder()
                .message("Verify change password request successfully!")
                .data(authenticationService.verifyForgotPassword(forgotPasswordToken))
                .build();
    }

    @Operation(
            summary = "Change password",
            description = "This api would verify that user really forgot password and change the forgot password to the new password.",
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
                                                    schema = @Schema(type = "string", example = "Change password successfully!")
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
                                    | 2006 | Password must contain at least one number, one numeral, and seven characters. |
                                    | 3003 | The account is not exist in the system.|
                                    | 4012 | This link has expired. Please go back to Homepage and try again. |
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "403",
                            description = """
                                    Forbidden
                                    |code  | message |
                                    |------|-------------|
                                    | 3005 | Your account is inactive.|
                                    """,
                            content = @Content(schema = @Schema(implementation = ApiResponse.class))
                    )
            }
    )
    @PutMapping("/forgot-password/change")
    public ApiResponse<String> changePassword(@RequestBody @Valid ChangePasswordRequest request){
        authenticationService.changePassword(request);
        return ApiResponse.<String>builder()
                .message("Change password successfully!")
                .build();
    }
}
