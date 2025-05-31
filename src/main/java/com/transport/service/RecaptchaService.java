package com.transport.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Map;

@Service
public class RecaptchaService {

    @Value("${google.recaptcha.secret-key}")
    private String recaptchaSecretKey;

    @Value("${google.recaptcha.verify-url}")
    private String recaptchaVerifyUrl;

    @Autowired
    private RestTemplate restTemplate;

    public boolean verifyRecaptcha(String recaptchaResponse, String clientIp) {
        try {
            System.out.println("Verifying reCAPTCHA...");
            System.out.println("Response: " + recaptchaResponse);
            System.out.println("Secret Key: " + (recaptchaSecretKey != null ? "Present" : "Missing"));

            // Prepare form data
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("secret", recaptchaSecretKey);
            formData.add("response", recaptchaResponse);
            formData.add("remoteip", clientIp);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            // Create request entity
            HttpEntity<MultiValueMap<String, String>> requestEntity =
                    new HttpEntity<>(formData, headers);

            // Make the request
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(
                    recaptchaVerifyUrl,
                    requestEntity,
                    Map.class
            );

            Map<String, Object> response = responseEntity.getBody();
            System.out.println("Google response: " + response);

            if (response != null && response.containsKey("success")) {
                Boolean success = (Boolean) response.get("success");
                System.out.println("reCAPTCHA verification result: " + success);

                // Log error codes if verification failed
                if (!success && response.containsKey("error-codes")) {
                    System.err.println("reCAPTCHA error codes: " + response.get("error-codes"));
                }

                return success;
            }

            System.err.println("Invalid response from Google reCAPTCHA");
            return false;

        } catch (Exception e) {
            System.err.println("reCAPTCHA verification failed: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
}