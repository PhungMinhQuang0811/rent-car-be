package com.mp.karental.mapper;

import com.mp.karental.dto.request.user.AccountRegisterRequest;
import com.mp.karental.dto.request.user.EditProfileRequest;
import com.mp.karental.dto.response.user.EditProfileResponse;
import com.mp.karental.dto.response.user.UserResponse;
import com.mp.karental.entity.Account;
import com.mp.karental.entity.UserProfile;
import org.mapstruct.*;

/**
 * Mapper interface for converting between user-related DTOs and entities.
 *
 * @author DieuTTH4
 *
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface UserMapper {
    /**
     * Converts an {@code AccountRegisterRequest} to an {@code Account} entity.
     *
     * @param request the account registration request containing user input data
     * @return an {@code Account} entity constructed from the provided request data
     */
    Account toAccount(AccountRegisterRequest request);

    /**
     * Converts an {@code AccountRegisterRequest} to a {@code UserProfile} entity.
     *
     * @param request the account registration request containing user input data
     * @return a {@code UserProfile} entity constructed from the provided request data
     */
    UserProfile toUserProfile(AccountRegisterRequest request);

    /**
     * Maps an {@code Account} entity and its associated {@code UserProfile} to a {@code UserResponse} DTO.
     * <p>
     * The mapping is performed as follows:
     * <ul>
     *     <li>{@code fullName} is sourced from {@code userProfile.fullName}.</li>
     *     <li>{@code email} is sourced from {@code account.email}.</li>
     *     <li>{@code phoneNumber} is sourced from {@code userProfile.phoneNumber}.</li>
     *     <li>{@code role} is sourced from {@code account.role}.</li>
     * </ul>
     * </p>
     *
     * @param account the account entity containing authentication and role information
     * @param userProfile the user profile entity containing personal details
     * @return a {@code UserResponse} DTO combining information from both the account and profile
     */
    @Mapping(target = "id", source = "account.id")
    @Mapping(target = "fullName", source = "userProfile.fullName")
    @Mapping(target = "email", source = "account.email")
    @Mapping(target = "phoneNumber", source = "userProfile.phoneNumber")
    @Mapping(target = "role", source = "account.role.name")
    UserResponse toUserResponse(Account account, UserProfile userProfile);

    // Mapping for feature edit-profile
    @Mapping(target = "drivingLicenseUrl", ignore = true)
    @Mapping(target = "email", source = "account.email")
    EditProfileResponse toEditProfileResponse(UserProfile userProfile);

    // Update UserProfile from EditProfileRequest
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    @Mapping(target = "drivingLicenseUri", ignore = true)
    @Mapping(target = "fullName", source = "fullName")
    @Mapping(target = "dob", source = "dob")
    @Mapping(target = "phoneNumber", source = "phoneNumber")
    @Mapping(target = "nationalId", source = "nationalId")
    @Mapping(target = "cityProvince", source = "cityProvince")
    @Mapping(target = "district", source = "district")
    @Mapping(target = "ward", source = "ward")
    @Mapping(target = "houseNumberStreet", source = "houseNumberStreet")
    void updateUserProfileFromRequest(EditProfileRequest request, @MappingTarget UserProfile userProfile);

}
