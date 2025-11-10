package com.mp.karental.dto.response.user;

import com.mp.karental.entity.Role;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

/**
 * Represents the response payload for user details.
 * <p>
 * This class encapsulates user information that is returned to the client,
 * including personal details and assigned role.
 * </p>
 *
 * @author DieuTTH4
 *
 * @version 1.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@Builder
@Schema(name = "response.user.UserResponse", description = "Data of an user")
public class UserResponse {
    @Schema(description = "User's ID", example = "123e4567-e89b-12d3-a456-426614174000")
    String id;

    @Schema(description = "User's full name", example = "Nguyễn Thị Bích")
    String fullName;

    @Schema(description = "User's email", example = "bich@example.com")
    String email;

    @Schema(description = "User's phone number", example = "0123456789")
    String phoneNumber;

    @Schema(description = "User's role", example = "CUSTOMER")
    String role;

}
