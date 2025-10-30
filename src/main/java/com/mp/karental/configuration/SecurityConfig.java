package com.mp.karental.configuration;

import com.mp.karental.security.auth.AuthEntryPointJwt;
import com.mp.karental.security.auth.AuthTokenFilter;
import com.mp.karental.security.auth.CustomAccessDeniedHandler;
import com.mp.karental.security.service.UserDetailsServiceImpl;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.*;

/**
 * This class is responsible for security configuration in the application
 *
 * @author DieuTTH4
 * @version 1.0
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SecurityConfig{

    @Value("${front-end.base-url}")
    @NonFinal
    private String frontendBaseUrl;

    /**
     * Define public endpoints, the endpoint that could be accessed without needing to provide any authentication header
     */
    @Value("${application.security.public-endpoints}")
    @NonFinal
    private String[] publicEndpoints;

    private List<String> getAllowCorsUrl(){
        return List.of(frontendBaseUrl);
    }

    UserDetailsServiceImpl userDetailsService;
    AuthEntryPointJwt jwtAuthenticationEntryPoint;
    AuthTokenFilter authTokenFilter;


    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig)
            throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(corsConfig -> corsConfig.configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration config = new CorsConfiguration();
//                        config.setAllowedOrigins(getAllowCorsUrl()); //
                        config.setAllowedOriginPatterns(Collections.singletonList("*")); // only for prm
                        config.setAllowedMethods(Collections.singletonList("*"));
                        config.setAllowedHeaders(Collections.singletonList("*"));
                        config.setExposedHeaders(Collections.singletonList("Authorization"));
//                        config.setAllowCredentials(true);
                        config.setAllowCredentials(false); // only for prm
                        return config;
                    }
                }))
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint) //unauthenticated request
                        .accessDeniedHandler(new CustomAccessDeniedHandler()) //unauthorized access
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) //make each request independently
                .authorizeHttpRequests( //authorization in http url
                        request -> request
                                //open public endpoints
                                .requestMatchers(publicEndpoints).permitAll()
                                //endpoints for user has role CAR_OWNER
                                .requestMatchers("/car/car-owner/**").hasRole("CAR_OWNER")
                                .requestMatchers("/car/customer/**").hasRole("CUSTOMER")
                                .requestMatchers("/booking/car-owner/**").hasRole("CAR_OWNER")
                                .requestMatchers("/booking/customer/**").hasRole("CUSTOMER")
                                .requestMatchers("/booking/operator/**").hasRole("OPERATOR")
                                .requestMatchers("/feedback/car-owner/**").hasRole("CAR_OWNER")
                                .requestMatchers("/feedback/customer/**").hasRole("CUSTOMER")
                                .requestMatchers("/car/operator/**").hasRole("OPERATOR")
                                .anyRequest().authenticated()
                );
        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(authTokenFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }


    /**
     * Configures the password encoder for the application.
     * <p>
     * This method sets up a BCryptPasswordEncoder with a strength of 10,
     * which is used to hash user passwords for secure storage.
     * </p>
     *
     * @return A PasswordEncoder that uses BCrypt hashing algorithm.
     * @author DieuTTH4
     */
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }



}
