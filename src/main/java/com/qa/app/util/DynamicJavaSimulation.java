package com.qa.app.util;

import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingRunParameters;
import com.qa.app.model.GatlingTest;
import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import org.json.JSONObject;

import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class DynamicJavaSimulation extends Simulation {

    private static final ThreadLocal<GatlingTest> testThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<GatlingRunParameters> paramsThreadLocal = new ThreadLocal<>();
    private static final ThreadLocal<Endpoint> endpointThreadLocal = new ThreadLocal<>();

    public static void setParameters(GatlingTest test, GatlingRunParameters params, Endpoint endpoint) {
        testThreadLocal.set(test);
        paramsThreadLocal.set(params);
        endpointThreadLocal.set(endpoint);
        
        System.out.println("DynamicJavaSimulation: 参数已设置");
        System.out.println("- 测试TCID: " + test.getTcid());
        System.out.println("- 端点URL: " + endpoint.getUrl());
        System.out.println("- 并发用户数: " + params.getUsers());
        System.out.println("- 重复次数: " + params.getRepetitions());
    }

    private GatlingTest getTest() { 
        GatlingTest test = testThreadLocal.get();
        if (test == null) {
            System.err.println("警告: 测试参数未设置，使用默认值");
            return new GatlingTest();
        }
        return test; 
    }
    
    private GatlingRunParameters getParams() { 
        GatlingRunParameters params = paramsThreadLocal.get();
        if (params == null) {
            System.err.println("警告: 运行参数未设置，使用默认值");
            return new GatlingRunParameters(1, 0, 1);
        }
        return params; 
    }
    
    private Endpoint getEndpoint() { 
        Endpoint endpoint = endpointThreadLocal.get();
        if (endpoint == null) {
            System.err.println("警告: 端点参数未设置，使用默认值");
            Endpoint defaultEndpoint = new Endpoint();
            defaultEndpoint.setUrl("http://localhost");
            defaultEndpoint.setMethod("GET");
            return defaultEndpoint;
        }
        return endpoint; 
    }

    HttpProtocolBuilder httpProtocol = http
            .baseUrl(getEndpoint().getUrl())
            .acceptHeader("application/json;q=0.9,*/*;q=0.8");
            // 默认情况下，Gatling会自动跟随重定向，无需额外配置

    ScenarioBuilder scn = scenario("DynamicSimulation")
            .repeat(getParams().getRepetitions()).on(
                exec(http("request_1")
                    .httpRequest(getEndpoint().getMethod(), "")
                    .headers(createHeaders(getTest().getHeaders()))
                    .body(StringBody(getTest().getBody() != null ? getTest().getBody() : ""))
                    .check(status().is(getExpectedStatus())))
            );

    {
        System.out.println("DynamicJavaSimulation: 初始化测试场景");
        System.out.println("- HTTP方法: " + getEndpoint().getMethod());
        System.out.println("- 重复次数: " + getParams().getRepetitions());
        System.out.println("- 已启用自动重定向跟踪（默认）");
        
        setUp(scn.injectOpen(rampUsers(getParams().getUsers()).during(Duration.ofSeconds(getParams().getRampUp()))))
                .protocols(httpProtocol);
    }

    private int getExpectedStatus() {
        try {
            String expStatus = getTest().getExpStatus();
            if (expStatus != null && !expStatus.isEmpty()) {
                return Integer.parseInt(expStatus);
            }
        } catch (Exception e) {
            System.err.println("解析期望状态码失败，使用默认值200: " + e.getMessage());
        }
        return 200;
    }

    private static Map<String, String> createHeaders(String headersText) {
        if (headersText == null || headersText.trim().isEmpty()) {
            return Collections.emptyMap();
        }
        
        Map<String, String> headers = new HashMap<>();
        
        try {
            // 首先尝试解析为JSON格式
            if (headersText.trim().startsWith("{")) {
                JSONObject json = new JSONObject(headersText);
                for (String key : json.keySet()) {
                    headers.put(key, json.getString(key));
                }
            } else {
                // 如果不是JSON格式，则尝试按行解析
                String[] lines = headersText.split("\\r?\\n");
                for (String line : lines) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        int colonIndex = line.indexOf(':');
                        if (colonIndex > 0) {
                            String key = line.substring(0, colonIndex).trim();
                            String value = line.substring(colonIndex + 1).trim();
                            headers.put(key, value);
                        }
                    }
                }
            }
            
            System.out.println("成功解析请求头，共 " + headers.size() + " 个");
            for (Map.Entry<String, String> entry : headers.entrySet()) {
                System.out.println("  " + entry.getKey() + ": " + entry.getValue());
            }
            
            return headers;
        } catch (Exception e) {
            System.err.println("解析请求头失败，使用空Map: " + e.getMessage());
            return Collections.emptyMap();
        }
    }
} 