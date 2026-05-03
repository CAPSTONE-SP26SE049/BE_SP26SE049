package org.fsa_2026.company_fsa_captone_2026.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Bucket;
import com.google.firebase.cloud.StorageClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Slf4j
@Service
public class FirebaseStorageService {

    @Value("${firebase.bucket-name}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String folder) throws IOException {
        String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
        String fullPath = (folder != null && !folder.isEmpty()) ? folder + "/" + fileName : fileName;

        Bucket bucket = StorageClient.getInstance().bucket();
        Blob blob = bucket.create(fullPath, file.getBytes(), file.getContentType());

        log.info("File uploaded to Firebase: {}", fullPath);

        // Return a public-like URL (Firebase Storage media link)
        // Format: https://firebasestorage.googleapis.com/v0/b/[BUCKET]/o/[PATH]?alt=media
        return String.format("https://firebasestorage.googleapis.com/v0/b/%s/o/%s?alt=media",
                bucketName,
                URLEncoder.encode(fullPath, StandardCharsets.UTF_8));
    }
}
