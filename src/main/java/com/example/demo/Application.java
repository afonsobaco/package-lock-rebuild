package com.example.demo;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
@AllArgsConstructor
@RequiredArgsConstructor
public class Application {
    private String name;
    private String version;
    private float lockfileVersion;
    private boolean requires;
    private Map<String, DeserializedDependency> dependencies;

}