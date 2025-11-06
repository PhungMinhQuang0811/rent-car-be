package com.mp.karental.service;

import com.mp.karental.dto.request.auth.ChangePasswordRequest;
import com.mp.karental.dto.request.auth.LoginRequest;
import com.mp.karental.dto.response.ApiResponse;
import com.mp.karental.dto.response.auth.LoginResponse;
import com.mp.karental.entity.Account;
import com.mp.karental.repository.UserProfileRepository;
import com.mp.karental.exception.AppException;
import com.mp.karental.exception.ErrorCode;
import com.mp.karental.repository.AccountRepository;
import com.mp.karental.security.JwtUtils;
import com.mp.karental.security.entity.UserDetailsImpl;
import com.mp.karental.security.service.TokenService;
import com.mp.karental.util.RedisUtil;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.InternalAuthenticationServiceException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.WebUtils;

/**
 * Service class for handling authentication operations.
 *
 * @author DieuTTH4
 * @version 1.0
 */
@Service
@Transactional
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class AuthenticationService {

    @Value("${front-end.base-url}")
    @NonFinal
    private String frontEndBaseUrl;

    //=======================================
    @Value("${server.servlet.context-path}")
    @NonFinal
    private String contextPath;

    @Value("${application.security.jwt.access-token-cookie-name}")
    @NonFinal
    private String accessTokenCookieName;

    @Value("${application.security.jwt.access-token-expiration}")
    @NonFinal
    private long accessTokenExpiration;
    //=======================================
    @NonFinal
    private String refreshTokenUrl = "/karental/auth/refresh-token";

    @Value("${application.security.jwt.refresh-token-cookie-name}")
    @NonFinal
    private String refreshTokenCookieName;

    @Value("${application.security.jwt.refresh-token-expiration}")
    @NonFinal
    private long refreshTokenExpiration;
    //=======================================
    @Value("${application.security.jwt.csrf-token-cookie-name}")
    @NonFinal
    private String csrfTokenCookieName;

    @Value("${application.security.jwt.csrf-token-header-name}")
    @NonFinal
    private String csrfTokenHeaderName;

    @NonFinal
    private String logoutUrl = "/karental/auth/logout";

    @Value("${application.domain-name}")
    @NonFinal
    private String domain;


    AuthenticationManager authenticationManager;

    JwtUtils jwtUtils;
    RedisUtil redisUtil;
    PasswordEncoder passwordEncoder;

    TokenService tokenService;
    EmailService emailService;

    AccountRepository accountRepository;
    UserProfileRepository userProfileRepository;

    public ResponseEntity<ApiResponse<LoginResponse>> login(LoginRequest request) {
        log.info("Processing login request, email={}", request.getEmail());
        //authenticate user's login information
        Authentication authentication = null;

        try {
            authentication = authenticationManager
                    .authenticate(
                            new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
                    );
        } catch (InternalAuthenticationServiceException e) {
            log.info("Login fail, account is inactive - email={}", request.getEmail());
            throw new AppException(ErrorCode.ACCOUNT_IS_INACTIVE);
        } catch (BadCredentialsException e) {
            log.info("Login fail, invalid login information - email={}", request.getEmail());
            throw new AppException(ErrorCode.INVALID_LOGIN_INFORMATION);
        }

        //set Authentication in security context
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();



        //put user's role and fullname in response
        String role = userDetails.getRole().getName().toString();
        String fullName = userProfileRepository.findById(userDetails.getAccoutnId()).get().getFullName();
        String csrfToken = jwtUtils.generateCsrfTokenFromUserEmail(userDetails.getEmail());
        ApiResponse<LoginResponse> apiResponse = ApiResponse.<LoginResponse>builder()
                .data(new LoginResponse(role, fullName, csrfToken))
                .build();
        log.info("Account with email={} logged in successfully", request.getEmail());
        return sendApiResponseResponseEntity(userDetails.getEmail(), userDetails.getAccoutnId(), apiResponse);
    }

    private <T> ResponseEntity<ApiResponse<T>> sendApiResponseResponseEntity(String email, String accountId, ApiResponse<T> apiResponse) {
        //generate tokens
        String accessToken = jwtUtils.generateAccessTokenFromUserEmail(email);
        String refreshToken = jwtUtils.generateRefreshTokenFromAccountId(accountId);

        //Generate token cookie
        ResponseCookie accessTokenCookie = generateCookie(accessTokenCookieName, accessToken, contextPath, accessTokenExpiration, true);
        ResponseCookie refreshTokenCookie = generateCookie(refreshTokenCookieName, refreshToken, refreshTokenUrl, refreshTokenExpiration, true);
        ResponseCookie refreshTokenCookieLogout = generateCookie(refreshTokenCookieName, refreshToken, logoutUrl, refreshTokenExpiration, true);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE,refreshTokenCookieLogout.toString())
                .body(apiResponse);
    }

    public ResponseEntity<ApiResponse<String>> refreshToken(HttpServletRequest request) {
        log.info("Processing refresh token request");

        //get the refresh token out from cookies
        String refreshToken = getCookieValueByName(request, refreshTokenCookieName);
        if (refreshToken == null || refreshToken.trim().isEmpty()){
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        jwtUtils.validateJwtRefreshToken(refreshToken);

        if(tokenService.isRefreshTokenInvalidated(refreshToken)){
            throw new AppException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        getTokensAndInvalidateTokens(request);

        //get user account's id from refresh token to generate new access token
        String accountId = jwtUtils.getUserAccountIdFromRefreshToken(refreshToken);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND_IN_DB));
        //the account in the token is inactive (banned)
        if (!account.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_IS_INACTIVE);
        }
        String csrfToken = jwtUtils.generateCsrfTokenFromUserEmail(account.getEmail());

        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .data(csrfToken)
                .build();
        log.info("New refresh token is generated for account with email={}", account.getEmail());
        return sendApiResponseResponseEntity(account.getEmail(), account.getId(), apiResponse);
    }

    public ResponseEntity<ApiResponse<String>> logout(HttpServletRequest request) {
        log.info("Processing logout request");

        getTokensAndInvalidateTokens(request);

        ApiResponse<String> apiResponse = ApiResponse.<String>builder()
                .data("Successfully logged out")
                .build();
        log.info("Logged out successfully");
        return sendLogoutApiResponseResponseEntity(apiResponse);
    }

    private <T> ResponseEntity<ApiResponse<T>> sendLogoutApiResponseResponseEntity(ApiResponse<T> apiResponse) {
        //Generate token cookie
        ResponseCookie accessTokenCookie = generateCookie(accessTokenCookieName, null, contextPath, 0, true);
        ResponseCookie refreshTokenCookie = generateCookie(refreshTokenCookieName, null, refreshTokenUrl, 0, true);
        ResponseCookie refreshTokenCookieLogout = generateCookie(refreshTokenCookieName, null, logoutUrl, 0, true);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, accessTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookieLogout.toString())
                .body(apiResponse);
    }

    private void getTokensAndInvalidateTokens(HttpServletRequest request) {
        //get tokens out from cookies
        String accessToken = getCookieValueByName(request, accessTokenCookieName);
        String refreshToken = getCookieValueByName(request, refreshTokenCookieName);

        //refresh token exist in cookie
        if (refreshToken != null && !refreshToken.isEmpty()) {
            try {
                jwtUtils.validateJwtRefreshToken(refreshToken);
                //the refresh token still not expire, invalidate it by saving to redis
                tokenService.invalidateRefreshToken(refreshToken, jwtUtils.getExpirationAtFromRefreshToken(refreshToken));
            } catch (Exception e) {
                log.info("Invalid refresh token, user can not refresh access token with this refresh token");
            }
        }

        //access token exist in cookie
        if (accessToken != null && !accessToken.isEmpty()) {
            try {
                jwtUtils.validateJwtAccessToken(accessToken);
                //the access token still not expire, invalidate it
                tokenService.invalidateAccessToken(accessToken, jwtUtils.getExpirationAtFromAccessToken(accessToken));

            } catch (Exception e) {
                log.info("Invalid access token, user can not be authenticated with this access token");
            }
        }

        String csrfToken = request.getHeader(csrfTokenHeaderName);
        if (csrfToken != null && !csrfToken.isEmpty()) {
            try {
                jwtUtils.validateJwtCsrfToken(csrfToken);
                //the csrf token still not expire, invalidate it by saving to redis
                tokenService.invalidateCsrfToken(csrfToken, jwtUtils.getExpirationAtFromCsrfToken(csrfToken));
            } catch (Exception e) {
                log.info("Invalid csrf token, user can not access with this csrf token");
            }
        }

    }
    /**
     * Create cookie to return to the client
     *
     * @param cookieName  the name of the cookie
     * @param cookieValue the value of the cookie
     * @param path        the domain that this cookie sent along
     * @return ResponseCookie object
     */
    private ResponseCookie generateCookie(String cookieName, String cookieValue, String path, long maxAgeMiliseconds, boolean isHttpOnly) {
        return ResponseCookie
                .from(cookieName, cookieValue)
//                .path(path)
                .path("/")
//                .domain(domain)
                .maxAge(maxAgeMiliseconds / 1000) // seconds ~ 1days
                .httpOnly(isHttpOnly)
//                .secure(true)
                .secure(false)
//                .sameSite("None")
                .sameSite("Lax")
                .build();
    }

    /**
     * get the value of the cookie using its' name (key-value)
     *
     * @param request
     * @param name
     * @return
     */
    private String getCookieValueByName(HttpServletRequest request, String name) {
        Cookie cookie = WebUtils.getCookie(request, name);
        if (cookie != null) {
            return cookie.getValue();
        } else {
            return null;
        }
    }

    //=====================================================
    //FORGOT PASSWORD

    public void sendForgotPasswordEmail(String email) {
        log.info("user with email={} request to change password.", email);
        //is the email used by one account in the system
        Account account = accountRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.EMAIL_NOT_USED_BY_ANY_ACCOUNT));

        //The email in the account is not verified or the account is inactivated
        if (!account.isEmailVerified() || !account.isActive()) {
            throw new AppException(ErrorCode.ACCOUNT_IS_INACTIVE);
        }

        //send email
        String changePasswordToken = redisUtil.generateForgotPasswordToken(account.getId());
        String forgotPasswordUrl = frontEndBaseUrl + "/#/auth/forgot-password/verify?t=" + changePasswordToken;
        log.info("Verify email url: {}", forgotPasswordUrl);
        emailService.sendForgotPasswordEmail(email, forgotPasswordUrl);
    }

    /**
     * verify that the user really forgot the password
     *
     * @param forgotPasswordToken
     * @return change password token if the token is valid
     */
    public String verifyForgotPassword(String forgotPasswordToken) {
        log.info("user with token={} request to change password.", forgotPasswordToken);
        //verify forgot password token
        verifyForgotPasswordToken(forgotPasswordToken);

        //return change password token
        return forgotPasswordToken;
    }

    private Account verifyForgotPasswordToken(String forgotPasswordToken) {
        String accountId = redisUtil.getValueOfForgotPasswordToken(forgotPasswordToken);
        //token exist and not expired
        if (accountId != null && !accountId.isEmpty()) {
            Account account = accountRepository.findById(accountId)
                    .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND_IN_DB));
            //The email in the account is not verified or the account is inactivated
            if (!account.isEmailVerified() || !account.isActive()) {
                throw new AppException(ErrorCode.ACCOUNT_IS_INACTIVE);
            }
            log.info("Forgot password token is valid");
            return account;
        } else {
            log.info("Forgot password token is invalid!");
            throw new AppException(ErrorCode.INVALID_FORGOT_PASSWORD_TOKEN);
        }
    }

    public void changePassword(ChangePasswordRequest request) {
        log.info("change password ");
        //Verify the forgot password token
        Account account = verifyForgotPasswordToken(request.getForgotPasswordToken());
        //update password
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);
        log.info("Changed password successfully");
        //delete the forgot password token
        redisUtil.deleteForgotPasswordToken(request.getForgotPasswordToken());
    }

}

