package com.mp.karental.controller;

import com.mp.karental.constant.EUploadPurpose;
import com.mp.karental.dto.response.ApiResponse;
import com.mp.karental.dto.response.file.UploadAssetResponse;
import com.mp.karental.service.UploadAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping(value = "/file", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class UploadController {
    private final UploadAssetService uploadAssetService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<UploadAssetResponse> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam("purpose") EUploadPurpose purpose) {
        return ApiResponse.<UploadAssetResponse>builder()
                .data(uploadAssetService.upload(file, purpose))
                .build();
    }
}
