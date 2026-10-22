package com.mp.karental.dto.response.file;

import com.mp.karental.constant.EUploadPurpose;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class UploadAssetResponse {
    String assetId;
    EUploadPurpose purpose;
}
