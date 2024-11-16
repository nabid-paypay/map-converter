package com.map.converter.map.service;

import com.map.converter.map.response.MapConvertResponse;
import java.io.*;
import java.net.*;
import java.util.*;
import javax.net.ssl.*;
import java.security.cert.X509Certificate;

public class MapUrlConverter {

    public static MapConvertResponse convertMapUrl(String inputUrl) {
        try {
            // Parse the input URL
            URL url = new URL(inputUrl);
            String host = url.getHost();

            boolean isGoogleMaps = false;
            boolean isAppleMaps = false;

            if (host.contains("google.com") || host.contains("goo.gl") || host.contains("maps.app.goo.gl")) {
                isGoogleMaps = true;
            } else if (host.contains("apple.com")) {
                isAppleMaps = true;
            } else {
                throw new IllegalArgumentException("Unsupported URL format");
            }

            String googleMapsUrl = null;
            String appleMapsUrl = null;

            if (isGoogleMaps) {
                // Convert to Apple Maps URL with enriched information
                appleMapsUrl = convertGoogleMapsToAppleMaps(inputUrl);
                googleMapsUrl = inputUrl;
            } else if (isAppleMaps) {
                // Convert to Google Maps URL with only latitude and longitude
                googleMapsUrl = convertAppleMapsToGoogleMaps(inputUrl);
                appleMapsUrl = inputUrl;
            } else {
                throw new IllegalArgumentException("Unsupported URL host");
            }

            return new MapConvertResponse(googleMapsUrl, appleMapsUrl);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String convertGoogleMapsToAppleMaps(String inputUrl) throws Exception {
        // Resolve short URLs
        if (inputUrl.contains("goo.gl") || inputUrl.contains("maps.app.goo.gl")) {
            inputUrl = resolveShortUrl(inputUrl);
        }

        // Parse the URL
        URL url = new URL(inputUrl);
        String path = url.getPath();
        String query = url.getQuery();

        String latitude = null;
        String longitude = null;
        String placeName = null;

        // Extract place name and coordinates
        if (path.contains("/place/")) {
            // URL format: https://www.google.com/maps/place/Place+Name/@lat,lng,...
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
            // URL format: https://www.google.com/maps/@lat,lng,...
            String[] parts = path.split("@");
            if (parts.length > 1) {
                String[] coords = parts[1].split(",");
                if (coords.length >= 2) {
                    latitude = coords[0];
                    longitude = coords[1];
                }
            }
        } else if (query != null) {
            // URL might have query parameters
            Map<String, String> params = splitQuery(query);
            if (params.containsKey("q")) {
                placeName = params.get("q");
            }
            if (params.containsKey("ll")) {
                String ll = params.get("ll");
                String[] coords = ll.split(",");
                if (coords.length >= 2) {
                    latitude = coords[0];
                    longitude = coords[1];
                }
            }
        }

        // If coordinates are still null, try extracting from query parameters
        if ((latitude == null || longitude == null) && query != null) {
            Map<String, String> params = splitQuery(query);
            String queryParam = params.get("query");
            if (queryParam != null && queryParam.contains(",")) {
                String[] coords = queryParam.split(",");
                if (coords.length >= 2) {
                    latitude = coords[0];
                    longitude = coords[1];
                }
            }
        }

        // Build Apple Maps URL with enriched information
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
        // Parse the URL
        URL url = new URL(inputUrl);
        String query = url.getQuery();

        Map<String, String> params = splitQuery(query);

        String latitude = null;
        String longitude = null;

        if (params.containsKey("ll")) {
            String ll = params.get("ll");
            String[] latLng = ll.split(",");
            if (latLng.length >= 2) {
                latitude = latLng[0];
                longitude = latLng[1];
            }
        }

        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("Cannot extract coordinates from Apple Maps URL");
        }

        // Build Google Maps URL with only latitude and longitude
        String googleMapsUrl = String.format("https://www.google.com/maps/search/?api=1&query=%s,%s", latitude, longitude);

        return googleMapsUrl;
    }

    private static String resolveShortUrl(String shortUrl) throws Exception {
        HttpURLConnection conn = null;
        try {
            // Bypass SSL certificate validation
            if (shortUrl.startsWith("https")) {
                trustAllHosts();
            }

            URL url = new URL(shortUrl);
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false); // We handle redirects ourselves
            conn.setRequestMethod("GET");
            conn.connect();
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                responseCode == 307 || responseCode == 308) {
                String redirectUrl = conn.getHeaderField("Location");
                if (redirectUrl == null) {
                    throw new IOException("No 'Location' header in redirect");
                }
                return resolveShortUrl(redirectUrl); // Recursive call in case of multiple redirects
            } else if (responseCode == HttpURLConnection.HTTP_OK) {
                // No redirect, return the original URL
                return shortUrl;
            } else {
                throw new IOException("Unexpected response code: " + responseCode);
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
    }

    private static void trustAllHosts() {
        TrustManager[] trustAllCerts = new TrustManager[]{
            new X509TrustManager() {
                public X509Certificate[] getAcceptedIssuers() {return new X509Certificate[0];}
                public void checkClientTrusted(X509Certificate[] chain, String authType) {}
                public void checkServerTrusted(X509Certificate[] chain, String authType) {}
            }
        };

        try {
            SSLContext sc = SSLContext.getInstance("TLS");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HostnameVerifier allHostsValid = (hostname, session) -> true;
            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> splitQuery(String query) throws UnsupportedEncodingException {
        Map<String, String> queryPairs = new LinkedHashMap<>();
        if (query == null) return queryPairs;
        String[] pairs = query.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf("=");
            String key;
            String value;
            if (idx > 0) {
                key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
                value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
            } else {
                key = URLDecoder.decode(pair, "UTF-8");
                value = "";
            }
            queryPairs.put(key, value);
        }
        return queryPairs;
    }

    // For testing purposes
    public static void main(String[] args) {
        String googleUrl = "https://maps.app.goo.gl/rSsZyv1eRHzqGKre6";
        String appleUrl = "https://maps.apple.com/?address=%E3%83%A1%E3%82%BE%E3%83%B3%E9%9D%92%E5%92%8C,%2026-1,%20Machiya%201-Ch%C5%8Dme,%20Arakawa,%20Tokyo,%20Japan%20116-0001&auid=13732938358045998444&ll=35.744410,139.784896&lsp=9902&q=Ai%20Visiting%20Care%20Station%20Machiya&t=m";

        MapConvertResponse convertedFromGoogle = convertMapUrl(googleUrl);
        System.out.println("Converted from Google Maps URL to Apple Maps URL:");
        System.out.println(convertedFromGoogle);

        MapConvertResponse convertedFromApple = convertMapUrl(appleUrl);
        System.out.println("Converted from Apple Maps URL to Google Maps URL:");
        System.out.println(convertedFromApple);
    }
}



