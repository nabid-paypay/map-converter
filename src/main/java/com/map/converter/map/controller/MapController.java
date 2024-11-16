package com.map.converter.map.controller;

import com.map.converter.map.request.MapRequest;
import com.map.converter.map.response.MapConvertResponse;
import com.map.converter.map.service.MapUrlConverter;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.ResponseEntity;

@RestController
@RequestMapping("/map")
public class MapController {

    @PostMapping("/convert")
    public ResponseEntity<MapConvertResponse> convertMapLink(@RequestBody MapRequest mapRequest) {
        MapConvertResponse response = MapUrlConverter.convertMapUrl(mapRequest.getUrl());
        return ResponseEntity.ok(response);
    }
}
