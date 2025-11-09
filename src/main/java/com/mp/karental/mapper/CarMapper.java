package com.mp.karental.mapper;

import com.mp.karental.dto.request.car.AddCarRequest;
import com.mp.karental.dto.request.car.EditCarRequest;
import com.mp.karental.dto.response.car.CarDetailResponse;
import com.mp.karental.dto.response.car.CarDocumentsResponse;
import com.mp.karental.dto.response.car.CarResponse;
import com.mp.karental.dto.response.car.CarThumbnailResponse;
import com.mp.karental.entity.Car;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

/**
 * Mapper interface for converting between car-related DTOs and entities.
 *
 * @author QuangPM20, AnhPH9
 *
 * @version 1.0
 */
@Mapper(componentModel = "spring")
public interface CarMapper {
    /**
     * Maps an AddCarRequest DTO to a Car entity.
     * Some fields related to images and address details are ignored.
     *
     * @param addCarRequest The request DTO containing car details.
     * @return A Car entity with mapped fields.
     */
    @Mapping(target = "registrationPaperUri", ignore = true)
    @Mapping(target = "certificateOfInspectionUri", ignore = true)
    @Mapping(target = "insuranceUri", ignore = true)
    @Mapping(target = "carImageFront", ignore = true)
    @Mapping(target = "carImageBack", ignore = true)
    @Mapping(target = "carImageLeft", ignore = true)
    @Mapping(target = "carImageRight", ignore = true)
    @Mapping(target = "cityProvince", ignore = true)
    @Mapping(target = "district", ignore = true)
    @Mapping(target = "ward", ignore = true)
    @Mapping(target = "houseNumberStreet", ignore = true)
    Car toCar(AddCarRequest addCarRequest);

    /**
     * Updates an existing Car entity with values from an EditCarRequest DTO.
     * Fields related to images and address details are ignored.
     *
     * @param car The existing Car entity to update.
     * @param editCarRequest The request DTO containing updated car details.
     */
    @Mapping(target = "registrationPaperUri", ignore = true)
    @Mapping(target = "certificateOfInspectionUri", ignore = true)
    @Mapping(target = "insuranceUri", ignore = true)
    @Mapping(target = "carImageFront", ignore = true)
    @Mapping(target = "carImageBack", ignore = true)
    @Mapping(target = "carImageLeft", ignore = true)
    @Mapping(target = "carImageRight", ignore = true)
    @Mapping(target = "cityProvince", ignore = true)
    @Mapping(target = "district", ignore = true)
    @Mapping(target = "ward", ignore = true)
    @Mapping(target = "houseNumberStreet", ignore = true)
    void editCar(@MappingTarget Car car, EditCarRequest editCarRequest);

    /**
     * Converts a Car entity into a CarResponse DTO.
     *
     * @param car The Car entity to be converted.
     * @return A CarResponse DTO containing mapped car details.
     */
    @Mapping(target = "isAutomatic", source = "automatic")
    @Mapping(target = "isGasoline", source = "gasoline")
    CarResponse toCarResponse(Car car);

    /**
     * Converts a Car entity to CarThumbnailResponse.
     * Ignores the address field in the response.
     *
     * @param car The car entity to convert.
     * @return CarThumbnailResponse containing car details.
     */
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "isAutomatic", source = "automatic")
    @Mapping(target = "isGasoline", source = "gasoline")
    CarThumbnailResponse toCarThumbnailResponse(Car car);

    /**
     * Converts a Car entity to CarDetailResponse.
     * Ignores sensitive document URLs such as registration papers,
     * certificate of inspection, and insurance details.
     *
     * @param car The car entity to convert.
     * @param isAvailable The availability status of the car.
     * @return CarDetailResponse containing detailed car information.
     */
    @Mapping(target = "address", ignore = true)
    @Mapping(target = "registrationPaperUrl", ignore = true)
    @Mapping(target = "certificateOfInspectionUrl", ignore = true)
    @Mapping(target = "insuranceUrl", ignore = true)
    @Mapping(target = "isAutomatic", source = "car.automatic")
    @Mapping(target = "isGasoline", source = "car.gasoline")
    CarDetailResponse toCarDetailResponse(Car car, boolean isAvailable);

    /**
     * Converts a Car entity to CarThumbnailResponse for search results.
     * Ignores images and the number of completed rides.
     *
     * @param car The car entity to convert.
     * @param noOfRides The number of completed rides for this car.
     * @return CarThumbnailResponse with summarized car details.
     */
    @Mapping(target = "noOfRides", ignore = true)
    @Mapping(target = "carImageFront", ignore = true)
    @Mapping(target = "carImageBack", ignore = true)
    @Mapping(target = "carImageLeft", ignore = true)
    @Mapping(target = "carImageRight", ignore = true)
    @Mapping(target = "isAutomatic", source = "car.automatic")
    @Mapping(target = "isGasoline", source = "car.gasoline")
    CarThumbnailResponse toSearchCar(Car car, long noOfRides);

    /**
     * Converts a {@link Car} entity to a {@link CarDocumentsResponse} DTO.
     *
     * <p>The returned object contains details about the car's documents, including their verification status.</p>
     *
     * @param car The {@link Car} entity to be converted.
     * @return A {@link CarDocumentsResponse} containing document-related details.
     */
    CarDocumentsResponse toCarDocumentsResponse(Car car);

}
