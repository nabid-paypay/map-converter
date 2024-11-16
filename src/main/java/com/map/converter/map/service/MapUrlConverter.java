package com.map.converter.map.service;

import com.map.converter.map.response.MapConvertResponse;

import java.io.*;
import java.net.*;
import java.util.*;

public class MapUrlConverter {

    public static MapConvertResponse convertMapUrl(String inputUrl) {
        try {
            // Parse the input URL
            URL url = new URL(inputUrl);
            String host = url.getHost();

            boolean isGoogleMaps = host.contains("google.com") || host.contains("goo.gl") || host.contains("maps.app.goo.gl");
            boolean isAppleMaps = host.contains("apple.com");

            if (!isGoogleMaps && !isAppleMaps) {
                throw new IllegalArgumentException("Unsupported URL format");
            }

            String googleMapsUrl = null;
            String appleMapsUrl = null;

            if (isGoogleMaps) {
                appleMapsUrl = convertGoogleMapsToAppleMaps(inputUrl);
                googleMapsUrl = inputUrl;
            } else if (isAppleMaps) {
                googleMapsUrl = convertAppleMapsToGoogleMaps(inputUrl);
                appleMapsUrl = inputUrl;
            }

            return new MapConvertResponse(googleMapsUrl, appleMapsUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String convertGoogleMapsToAppleMaps(String inputUrl) throws Exception {
        if (inputUrl.contains("goo.gl") || inputUrl.contains("maps.app.goo.gl")) {
            inputUrl = resolveShortUrl(inputUrl);
        }

        URL url = new URL(inputUrl);
        String path = url.getPath();
        String query = url.getQuery();

        String latitude = null;
        String longitude = null;
        String placeName = null;

        if (path.contains("/place/")) {
            String[] pathSegments = path.split("/");
            for (int i = 0; i < pathSegments.length; i++) {
                if (pathSegments[i].equals("place")) {
                    placeName = URLDecoder.decode(pathSegments[i + 1], "UTF-8");
                    break;
                }
            }
            if (path.contains("@")) {
                String[] parts = path.split("@");
                if (parts.length > 1) {
                    String[] coords = parts[1].split(",");
                    if (coords.length >= 2) {
                        latitude = coords[0];
                        longitude = coords[1];
                    }
                }
            }
        } else if (path.contains("@")) {
            String[] parts = path.split("@");
            if (parts.length > 1) {
                String[] coords = parts[1].split(",");
                if (coords.length >= 2) {
                    latitude = coords[0];
                    longitude = coords[1];
                }
            }
        }

        Map<String, String> params = query != null ? splitQuery(query) : new HashMap<>();
        if (params.containsKey("q")) {
            placeName = params.get("q");
        }
        if (params.containsKey("ll")) {
            String[] coords = params.get("ll").split(",");
            if (coords.length >= 2) {
                latitude = coords[0];
                longitude = coords[1];
            }
        }

        StringBuilder appleMapsUrl = new StringBuilder("https://maps.apple.com/?");
        if (placeName != null) {
            appleMapsUrl.append("q=").append(URLEncoder.encode(placeName, "UTF-8")).append("&");
        }
        if (latitude != null && longitude != null) {
            appleMapsUrl.append("ll=").append(latitude).append(",").append(longitude);
        }

        return appleMapsUrl.toString();
    }

    private static String convertAppleMapsToGoogleMaps(String inputUrl) throws Exception {
        URL url = new URL(inputUrl);
        Map<String, String> params = splitQuery(url.getQuery());

        String latitude = null;
        String longitude = null;

        if (params.containsKey("ll")) {
            String[] coords = params.get("ll").split(",");
            if (coords.length >= 2) {
                latitude = coords[0];
                longitude = coords[1];
            }
        }

        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Cannot extract coordinates from Apple Maps URL");
        }

        return String.format("https://www.google.com/maps/search/?api=1&query=%s,%s", latitude, longitude);
    }

    private static String resolveShortUrl(String shortUrl) throws Exception {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(shortUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestMethod("GET");
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == 307 || responseCode == 308) {
                String redirectUrl = conn.getHeaderField("Location");
                if (redirectUrl == null) {
                    throw new IOException("No 'Location' header found");
                }
                return resolveShortUrl(redirectUrl); // Recursive for multiple redirects
            }

            return shortUrl;
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        if (query == null) return queryPairs;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key = URLDecoder.decode(pair.substring(0, idx > 0 ? idx : pair.length()), "UTF-8");
            String value = idx > 0 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : "";
            queryPairs.put(key, value);
        }
        return queryPairs;
    }
}
