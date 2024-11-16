package com.map.converter.map.request;

public class MapRequest {
    private String url;

    // Default constructor (required for deserialization)
    public MapRequest() {}

    public MapRequest(String url) {
        this.url = url;
    }

    // Getter and Setter
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
