package com.example.demo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;

@Data
@Builder(toBuilder = true)
@JsonInclude(JsonInclude.Include.NON_DEFAULT)
@EqualsAndHashCode
@RequiredArgsConstructor
@AllArgsConstructor
public class Dependency {
    private String name;
    private String version;
    private String resolved;
    private String integrity;
    private boolean dev;
    private boolean nested;
    private boolean optional;
    private boolean bundled;
    private List<Dependency> requires;
    private List<Dependency> dependencies;
}
