package com.qa.app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingRunParameters;
import com.qa.app.model.GatlingTest;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class DynamicJavaSimulation extends Simulation {

    private final GatlingTest test;
    private final GatlingRunParameters params;
    private final Endpoint endpoint;

    public DynamicJavaSimulation() {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.test = objectMapper.readValue(new File(System.getProperty("gatling.test.file")), GatlingTest.class);
            this.params = objectMapper.readValue(new File(System.getProperty("gatling.params.file")), GatlingRunParameters.class);
            this.endpoint = objectMapper.readValue(new File(System.getProperty("gatling.endpoint.file")), Endpoint.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize simulation parameters from file", e);
        }

        HttpProtocolBuilder httpProtocol = http
                .baseUrl(endpoint.getUrl())
                .acceptHeader("application/json;q=0.9,*/*;q=0.8");

        // Parse headers from the generated string
        Map<String, String> headersMap = parseHeaders(test.getHeaders());

        // Automatically add Content-Type if it's a POST/PUT with a body and header is missing
        String method = endpoint.getMethod().toUpperCase();
        boolean hasBody = test.getBody() != null && !test.getBody().trim().isEmpty();

        if (hasBody && (method.equals("POST") || method.equals("PUT")) &&
            !headersMap.keySet().stream().anyMatch(key -> key.equalsIgnoreCase("Content-Type"))) {
            headersMap.put("Content-Type", "application/json");
        }

        ScenarioBuilder scn = scenario("DynamicSimulation")
                .repeat(params.getRepetitions()).on(
                        exec(http("request_1")
                                .httpRequest(endpoint.getMethod(), "")
                                .headers(headersMap)
                                .body(StringBody(test.getBody() != null ? test.getBody() : ""))
                                .check(status().is(Integer.parseInt(test.getExpStatus()))))
                );

        System.out.println("DynamicJavaSimulation: Test scene initialized");
        System.out.println("- HTTP Method: " + endpoint.getMethod());
        System.out.println("- Repetitions: " + params.getRepetitions());

        setUp(scn.injectOpen(rampUsers(params.getUsers()).during(Duration.ofSeconds(params.getRampUp()))))
                .protocols(httpProtocol);
    }
    
    private Map<String, String> parseHeaders(String headersString) {
        if (headersString == null || headersString.trim().isEmpty()) {
            return new HashMap<>();
        }
        
        return Arrays.stream(headersString.split("\\r?\\n"))
                .map(String::trim)
                .filter(line -> !line.isEmpty() && line.contains(":"))
                .map(line -> line.split(":", 2))
                .filter(parts -> parts.length == 2 && !parts[0].trim().isEmpty())
                .collect(Collectors.toMap(
                        parts -> parts[0].trim(),
                        parts -> parts[1].trim(),
                        (v1, v2) -> v2, // In case of duplicate headers, keep the last one
                        HashMap::new
                ));
    }
} 