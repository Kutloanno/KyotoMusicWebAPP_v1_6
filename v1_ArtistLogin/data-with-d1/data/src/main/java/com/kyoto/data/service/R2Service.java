package com.kyoto.data.service;

import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class R2Service {

    private static final String BUCKET = "my-music-storage";

    private final S3Client s3Client;

    public R2Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    public List<String> listSongs() {
        ListObjectsV2Response response = s3Client.listObjectsV2(
                ListObjectsV2Request.builder().bucket(BUCKET).build());

        return response.contents().stream()
                .map(S3Object::key)
                .collect(Collectors.toList());
    }

    public void uploadSong(String fileName, InputStream inputStream, long fileSize) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(fileName)
                        .contentType("audio/mpeg")
                        .build(),
                RequestBody.fromInputStream(inputStream, fileSize));
    }

    public ResponseInputStream<GetObjectResponse> downloadSong(String key) {
        return s3Client.getObject(
                GetObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .build());
    }

    public void deleteSong(String key) {
        s3Client.deleteObject(
                DeleteObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .build());
    }

    public long getContentLength(String key) {
        HeadObjectResponse head = s3Client.headObject(
                HeadObjectRequest.builder()
                        .bucket(BUCKET)
                        .key(key)
                        .build());
        return head.contentLength();
    }

    public String getFileUrl(String key) {
        return "/stream/" + key;
    }
}


