package com.kyoto.data.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Spring Service that centralises every R2/S3 operation.
 * Replaces the original @Stateless EJB.
 */
@Service
public class R2Service {

    // Removed the FOLDER variable. Using the bucket name directly.
    private static final String BUCKET = "my-music-storage";

    private final S3Client s3Client;

    public R2Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    // ------------------------------------------------------------------
    // LIST all objects in the bucket
    // ------------------------------------------------------------------
    public List<String> listSongs() {
        ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder().bucket(BUCKET).build());

        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    // ------------------------------------------------------------------
    // UPLOAD – receives a raw InputStream from the multipart form
    // ------------------------------------------------------------------
    public void uploadSong(String fileName, InputStream inputStream, long fileSize) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(fileName) // Just the filename, no slash prefix
                        .contentType("audio/mpeg")
                        .build(),
                RequestBody.fromInputStream(inputStream, fileSize));
    }

    // ------------------------------------------------------------------
    // DOWNLOAD – returns a streaming S3 response (controller pipes it out)
    // ------------------------------------------------------------------
    public ResponseInputStream<GetObjectResponse> downloadSong(String key) {
        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .build());
    }

    // ------------------------------------------------------------------
    // DELETE a song from the bucket
    // ------------------------------------------------------------------
    public void deleteSong(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .build());
    }


    // ------------------------------------------------------------------
    // GET content length for a key (used by stream/download endpoints)
    // ------------------------------------------------------------------
    public long getContentLength(String key) {
        HeadObjectResponse head = s3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .build());
        return head.contentLength();
    }


    public String getFileUrl(String key) {
        return "/stream/" + key; // Local URL
    }
}


