package com.mp.karental.scheduler;

import com.mp.karental.service.UploadAssetService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UploadAssetCleanupScheduler {
    private final UploadAssetService uploadAssetService;

    @Scheduled(cron = "0 30 3 * * *")
    public void cleanExpiredUploads() {
        uploadAssetService.markExpiredUploadsForDeletion();
        uploadAssetService.cleanupPendingUploads();
    }
}
