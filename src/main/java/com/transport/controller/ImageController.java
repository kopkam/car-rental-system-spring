package com.transport.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Controller
@RequestMapping("/images")
public class ImageController {

    private static final String UPLOAD_DIR = "uploads/cars/";

    @GetMapping("/cars/{filename:.+}")
    public ResponseEntity<Resource> getCarImage(@PathVariable String filename) {
        try {
            System.out.println("Image request: /images/cars/" + filename);

            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename);
            System.out.println("Looking for file at: " + filePath.toAbsolutePath());

            if (!Files.exists(filePath)) {
                System.out.println("Image not found: " + filePath.toAbsolutePath());

                // Debug - pokaż zawartość katalogu
                Path parentDir = filePath.getParent();
                if (Files.exists(parentDir)) {
                    System.out.println("Directory contents:");
                    Files.list(parentDir).forEach(path ->
                            System.out.println("  - " + path.getFileName()));
                } else {
                    System.out.println("Upload directory doesn't exist: " + parentDir);
                }

                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                System.out.println("Resource not readable: " + filePath.toAbsolutePath());
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                String fileName = filename.toLowerCase();
                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    contentType = "image/jpeg";
                } else if (fileName.endsWith(".png")) {
                    contentType = "image/png";
                } else if (fileName.endsWith(".webp")) {
                    contentType = "image/webp";
                } else {
                    contentType = "application/octet-stream";
                }
            }

            System.out.println("Serving image: " + filename + " (" + contentType + ")");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache na godzinę
                    .body(resource);

        } catch (Exception e) {
            System.err.println("Error serving image: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.notFound().build();
        }
    }
}