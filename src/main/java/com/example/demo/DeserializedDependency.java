package com.example.demo;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@AllArgsConstructor
@RequiredArgsConstructor
public class DeserializedDependency {
    private String name;
    private String version;
    private String resolved;
    private String integrity;
    private boolean dev;
    private boolean nested;
    private boolean optional;
    private boolean bundled;
    private Map<String, String> requires;
    private Map<String, DeserializedDependency> dependencies;
}
