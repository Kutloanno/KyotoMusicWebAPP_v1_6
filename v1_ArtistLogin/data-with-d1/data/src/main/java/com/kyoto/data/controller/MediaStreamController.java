package com.kyoto.data.controller;

import com.kyoto.data.service.R2Service;
import com.kyoto.data.service.D1Service; // NEW IMPORTS
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

import java.util.List; // NEW IMPORT for SQL params

@Controller
public class MediaStreamController {

    private final R2Service musicService;
    private final D1Service d1Service; // NEW FIELD

    // Updated Constructor to include D1Service
    public MediaStreamController(R2Service musicService, D1Service d1Service) {
        this.musicService = musicService;
        this.d1Service = d1Service;
    }

    @GetMapping("/stream/{key:.+}") // Added :.+ to handle filenames with dots
    @ResponseBody
    public ResponseEntity<Resource> streamAudio(@PathVariable String key) {
        return handleStreamAudio(key);
    }

    @GetMapping("/image/{key:.+}")
    @ResponseBody
    public ResponseEntity<Resource> streamImage(@PathVariable String key) {
        return handleStreamImage(key);
    }

    @GetMapping("/download/{key:.+}")
    @ResponseBody
    public ResponseEntity<Resource> downloadAudio(@PathVariable String key) {
        return handleDownloadAudio(key);
    }

    // ============================================================
    // REUSABLE METHODS - UPDATED WITH DB COUNTERS
    // ============================================================

    public ResponseEntity<Resource> handleStreamAudio(String key) {
        try {
            // --- UPDATING PLAY COUNT ---
            String sql = "UPDATE SONG SET PlayCount = PlayCount + 1 WHERE AudioFileURL = ?";
            d1Service.executeUpdateWithParams(sql, List.of(key));

            ResponseInputStream<GetObjectResponse> s3Object = musicService.downloadSong(key);
            long contentLength = musicService.getContentLength(key);
            InputStreamResource resource = new InputStreamResource(s3Object);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .contentLength(contentLength)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + key + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    public ResponseEntity<Resource> handleDownloadAudio(String key) {
        try {
            // --- UPDATING DOWNLOAD COUNT ---
            String sql = "UPDATE SONG SET DownloadCount = DownloadCount + 1 WHERE AudioFileURL = ?";
            d1Service.executeUpdateWithParams(sql, List.of(key));

            ResponseInputStream<GetObjectResponse> s3Object = musicService.downloadSong(key);
            long contentLength = musicService.getContentLength(key);
            InputStreamResource resource = new InputStreamResource(s3Object);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("audio/mpeg"))
                    .contentLength(contentLength)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + key + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Keep your other methods (handleStreamImage, deleteFile, detectImageContentType)
    // exactly as they were below...

    public ResponseEntity<Resource> handleStreamImage(String key) {
        try {
            ResponseInputStream<GetObjectResponse> s3Object = musicService.downloadSong(key);
            long contentLength = musicService.getContentLength(key);
            InputStreamResource resource = new InputStreamResource(s3Object);
            String contentType = detectImageContentType(key);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .contentLength(contentLength)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + key + "\"")
                    .body(resource);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    private String detectImageContentType(String filename) {
        String lower = filename.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "image/jpeg";
    }
}