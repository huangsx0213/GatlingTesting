package com.qa.app.util;

import com.qa.app.model.Endpoint;
import com.qa.app.model.GatlingRunParameters;
import com.qa.app.model.GatlingTest;
import io.gatling.app.Gatling;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.json.JSONObject;

public class GatlingTestExecutor {

    public static void execute(GatlingTest test, GatlingRunParameters params, Endpoint endpoint) {
        try {
            // 打印测试详细信息
            System.out.println("========== Gatling测试详情 ==========");
            System.out.println("测试ID: " + test.getId());
            System.out.println("测试TCID: " + test.getTcid());
            System.out.println("测试Suite: " + test.getSuite());
            
            System.out.println("\n----- 端点信息 -----");
            System.out.println("端点名称: " + endpoint.getName());
            System.out.println("URL: " + endpoint.getUrl());
            System.out.println("方法: " + endpoint.getMethod());
            
            System.out.println("\n----- 请求头 -----");
            try {
                if (test.getHeaders() != null && !test.getHeaders().trim().isEmpty()) {
                    String headersText = test.getHeaders().trim();
                    if (headersText.startsWith("{")) {
                        // JSON格式
                        JSONObject headers = new JSONObject(headersText);
                        for (String key : headers.keySet()) {
                            System.out.println(key + ": " + headers.getString(key));
                        }
                    } else {
                        // 多行文本格式
                        String[] lines = headersText.split("\\r?\\n");
                        for (String line : lines) {
                            line = line.trim();
                            if (!line.isEmpty()) {
                                System.out.println(line);
                            }
                        }
                    }
                } else {
                    System.out.println("(无请求头)");
                }
            } catch (Exception e) {
                System.out.println("请求头解析失败: " + e.getMessage());
                System.out.println("原始请求头: " + test.getHeaders());
            }
            
            System.out.println("\n----- 请求体 -----");
            if (test.getBody() != null && !test.getBody().trim().isEmpty()) {
                try {
                    // 尝试格式化JSON
                    JSONObject json = new JSONObject(test.getBody());
                    System.out.println(json.toString(2)); // 缩进2个空格
                } catch (Exception e) {
                    // 如果不是有效的JSON，直接打印原始内容
                    System.out.println(test.getBody());
                }
            } else {
                System.out.println("(无请求体)");
            }
            
            System.out.println("\n----- 测试参数 -----");
            System.out.println("并发用户数: " + params.getUsers());
            System.out.println("预热时间(秒): " + params.getRampUp());
            System.out.println("重复次数: " + params.getRepetitions());
            System.out.println("期望状态码: " + test.getExpStatus());
            System.out.println("等待时间(秒): " + test.getWaitTime());
            System.out.println("===================================");

            // 设置动态参数
            DynamicJavaSimulation.setParameters(test, params, endpoint);

            // 构建Gatling命令行参数
            List<String> args = new ArrayList<>();
            args.add("-s");
            args.add(DynamicJavaSimulation.class.getName());
            args.add("-rf");
            String resultsPath = Paths.get(System.getProperty("user.dir"), "target", "gatling").toString();
            args.add(resultsPath);

            // 执行Gatling测试
            System.out.println("开始执行Gatling测试...");
            Gatling.main(args.toArray(new String[0]));
            System.out.println("Gatling测试执行完成。");

        } catch (Exception e) {
            System.err.println("执行Gatling测试失败: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Failed to run Gatling test via Java API", e);
        }
    }
} 