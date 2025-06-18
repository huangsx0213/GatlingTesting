package com.qa.app.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingRunParameters;
import com.qa.app.model.GatlingTest;
import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
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

    {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            this.test = objectMapper.readValue(new File(System.getProperty("gatling.test.file")), GatlingTest.class);
            this.params = objectMapper.readValue(new File(System.getProperty("gatling.params.file")), GatlingRunParameters.class);
            this.endpoint = objectMapper.readValue(new File(System.getProperty("gatling.endpoint.file")), Endpoint.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to deserialize simulation parameters from file", e);
        }

        HttpProtocolBuilder httpProtocol = createHttpProtocol();
        ScenarioBuilder scn = createScenario();

        System.out.println("DynamicJavaSimulation: Test scene initialized for TCID: " + test.getTcid());
        System.out.println("- Base URL: " + endpoint.getUrl());
        System.out.println("- HTTP Method: " + endpoint.getMethod());
        System.out.println("- Users: " + params.getUsers());
        System.out.println("- Ramp-up (s): " + params.getRampUp());
        System.out.println("- Repetitions per user: " + params.getRepetitions());

        setUp(scn.injectOpen(rampUsers(params.getUsers()).during(Duration.ofSeconds(params.getRampUp()))))
                .protocols(httpProtocol);
    }

    private HttpProtocolBuilder createHttpProtocol() {
        // The endpoint.getUrl() is used as the full base URL for the requests.
        // The path in the httpRequest is empty, meaning requests hit the baseUrl directly.
        return http
                .baseUrl(endpoint.getUrl())
                .acceptHeader("application/json, text/plain, */*")
                .acceptEncodingHeader("gzip, deflate, br")
                .acceptLanguageHeader("en-US,en;q=0.9");
    }

    private ScenarioBuilder createScenario() {
        Map<String, String> headersMap = parseHeaders(test.getHeaders());
        String method = endpoint.getMethod().toUpperCase();
        boolean hasBody = test.getBody() != null && !test.getBody().trim().isEmpty();

        // Automatically add Content-Type if it's a POST/PUT with a body and header is missing
        if (hasBody && (method.equals("POST") || method.equals("PUT")) &&
            !headersMap.keySet().stream().anyMatch(key -> key.equalsIgnoreCase("Content-Type"))) {
            headersMap.put("Content-Type", "application/json");
        }

        int expectedStatus = 200; // Default to 200 OK
        try {
            expectedStatus = Integer.parseInt(test.getExpStatus());
        } catch (NumberFormatException e) {
            System.err.println("Warning: Could not parse expected status '" + test.getExpStatus() + "'. Defaulting to 200.");
        }

        String requestName = test.getTcid() != null && !test.getTcid().isEmpty() ? test.getTcid() : "request_1";

        return scenario("DynamicSimulation - " + test.getSuite())
                .repeat(params.getRepetitions()).on(
                        exec(http(requestName)
                                .httpRequest(endpoint.getMethod(), "") // Path is empty as baseUrl is the full URL
                                .headers(headersMap)
                                .body(StringBody(test.getBody() != null ? test.getBody() : ""))
                                .check(status().is(expectedStatus))
                        ).pause(test.getWaitTime())
                );
    }

    private Map<String, String> parseHeaders(String headersString) {
        if (headersString == null || headersString.trim().isEmpty()) {
            return Collections.emptyMap();
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