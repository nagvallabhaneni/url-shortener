package com.urlshortener.validator;

import java.net.URL;

public class UrlValidator {
    private static final int MAX_URL_LENGTH = 2000;

    /**
     * Validates if the provided URL is a valid HTTP/HTTPS URL
     */
    public boolean isValid(String urlString) {
        if (urlString == null || urlString.isBlank()) {
            return false;
        }

        if (urlString.length() > MAX_URL_LENGTH) {
            return false;
        }

        try {
            URL url = new URL(urlString);
            String protocol = url.getProtocol();
            return "http".equals(protocol) || "https".equals(protocol);
        } catch (Exception e) {
            return false;
        }
    }
}
