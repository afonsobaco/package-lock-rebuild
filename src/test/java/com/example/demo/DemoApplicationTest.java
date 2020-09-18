package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

//TODO reimplement tests
public class DemoApplicationTest {

    private DemoApplication demo = new DemoApplication();

    @Test
    @DisplayName("Should convert deserialized to dependency list")
    public void case1() {
        Map<String, DeserializedDependency> deserialized = getDeserializedEntry();

        var result = demo.convertToDependency(deserialized);
        assertEquals(deserialized.size(), result.size());
    }

    @Test
    @DisplayName("Should clean requires")
    public void case4() {
        List<Dependency> dependencies = getListFromTestFile();

        List<Dependency> actual = demo.cleanRequires(dependencies);

        assertNotNull(actual);
        assertFalse(actual.isEmpty());

        print(actual);
    }

    private Map<String, DeserializedDependency> getDeserializedEntry() {
        Map<String, DeserializedDependency> dependencies = new HashMap<>();
        Map<String, DeserializedDependency> nestedDependecies = new HashMap<>();
        Map<String, String> requires = new HashMap<>();
        nestedDependecies.put("dep-nested", DeserializedDependency.builder()
                .dev(true)
                .version("v1")
                .build());
        requires.put("dep-nested", "v1");
        dependencies.put("dep", DeserializedDependency.builder()
                .version("v1.1")
                .dependencies(nestedDependecies)
                .requires(requires)
                .build());
        dependencies.put("dep-whitout-requires-and-dependencies", DeserializedDependency.builder()
                .version("v1.1")
                .build());
        dependencies.put("dep-whitout-requires", DeserializedDependency.builder()
                .version("v1.1")
                .dependencies(nestedDependecies)
                .build());
        dependencies.put("dep-whitout-dependecies", DeserializedDependency.builder()
                .version("v1.1")
                .requires(requires)
                .build());
        return dependencies;
    }

    private void print(Object result) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(result));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
    }

    private List<Dependency> getListFromTestFile() {
        try {
            ObjectMapper mapper = new ObjectMapper();
            List<Dependency> list = Arrays
                    .asList(mapper.readValue(getFileFromResource("test.json"), Dependency[].class));
            return list;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private File getFileFromResource(String fileName) throws URISyntaxException {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(fileName);
        if (resource == null) {
            throw new IllegalArgumentException("file not found! " + fileName);
        } else {
            return new File(resource.toURI());
        }
    }

}
