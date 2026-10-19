package com.mp.karental.validation.validator;

import com.mp.karental.constant.EDocumentFile;
import com.mp.karental.validation.ValidDocument;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import org.springframework.web.multipart.MultipartFile;

/**
 * Validator for checking if an uploaded document file has a valid extension.
 * The valid file extensions are defined in the {@link EDocumentFile} enum.
 * QuangPM20
 * @version 1.0
 */
@RequiredArgsConstructor
public class FileDocumentValidator implements ConstraintValidator<ValidDocument, MultipartFile> {

    /**
     * Validates whether the uploaded file has a valid document extension.
     *
     * @param file    The uploaded file to validate.
     * @param context The validation context.
     * @return true if the file is valid or empty (assuming required validation is handled separately), false otherwise.
     */
    @Override
    public boolean isValid(MultipartFile file, ConstraintValidatorContext context) {
        if (file == null || file.isEmpty()) {
            return true;  // Skip validation if the file is null or empty (handled separately with @NotNull)
        }

        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            return false; // Invalid if the file has no name
        }

        // Extract the file extension
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        String fileExtension = fileName.substring(dot + 1).toLowerCase();

        // Validate the file extension using the isValidExtension method
        return isValidExtension(fileExtension) && UploadedFileSignature.isAllowedDocument(file, fileExtension);
    }

    /**
     * Checks whether the given file extension is valid based on the {@link EDocumentFile} enum.
     *
     * @param extension The file extension to validate.
     * @return true if the extension is valid, false otherwise.
     */
    private boolean isValidExtension(String extension) {
        for (EDocumentFile fileType : EDocumentFile.values()) {
            if (fileType.getExtension().equalsIgnoreCase(extension)) {
                return true;  // Extension is valid
            }
        }
        return false;  // Extension is not valid
    }
}
