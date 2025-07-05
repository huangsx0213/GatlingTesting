package com.qa.app.ui.vm.gatling;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Tooltip;
import javafx.util.Duration;
import com.qa.app.service.script.VariableGenerator;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 统一管理应用中的tooltip提示信息
 * 提供集中的tooltip文本内容和显示逻辑
 */
public class TooltipManager {
    
    private static final String TOOLTIP_STYLE = "-fx-font-size: 14px;";
    private static final Duration SHOW_DELAY = Duration.millis(200);
    private static final Duration SHOW_DURATION = Duration.seconds(30);
    
    // 单例模式
    private static TooltipManager instance;
    
    // 各种提示信息的tooltip对象
    private Tooltip variableTooltip;
    private Tooltip responseCheckTooltip;
    
    private TooltipManager() {
        initializeTooltips();
    }
    
    public static TooltipManager getInstance() {
        if (instance == null) {
            instance = new TooltipManager();
        }
        return instance;
    }
    
    /**
     * 初始化所有tooltip
     */
    private void initializeTooltips() {
        // 变量使用提示
        variableTooltip = createTooltip(getVariableHelpText());
        
        // 响应检查提示
        responseCheckTooltip = createTooltip(getResponseCheckHelpText());
    }
    
    /**
     * 创建统一样式的tooltip
     */
    private Tooltip createTooltip(String text) {
        Tooltip tooltip = new Tooltip(text);
        tooltip.setStyle(TOOLTIP_STYLE);
        tooltip.setShowDelay(SHOW_DELAY);
        tooltip.setShowDuration(SHOW_DURATION);
        tooltip.setAutoHide(true);
        return tooltip;
    }
    
    /**
     * 显示或隐藏tooltip
     * @param tooltip 要显示的tooltip
     * @param ownerNode 触发tooltip的节点
     */
    public void toggleTooltip(Tooltip tooltip, Node ownerNode) {
        if (tooltip.isShowing()) {
            tooltip.hide();
        } else {
            Point2D p = ownerNode.localToScreen(
                ownerNode.getBoundsInLocal().getMaxX(), 
                ownerNode.getBoundsInLocal().getMinY()
            );
            tooltip.show(ownerNode, p.getX(), p.getY());
        }
    }
    
    /**
     * 获取变量提示tooltip
     */
    public Tooltip getVariableTooltip() {
        return variableTooltip;
    }
    
    /**
     * 获取响应检查提示tooltip
     */
    public Tooltip getResponseCheckTooltip() {
        return responseCheckTooltip;
    }
    
    /**
     * 刷新所有tooltip内容
     * 当系统中的变量定义发生变化时调用
     */
    public void refreshTooltips() {
        variableTooltip.setText(getVariableHelpText());
        responseCheckTooltip.setText(getResponseCheckHelpText());
    }
    
    /**
     * 变量帮助文本
     * 从VariableGenerator获取实际的变量定义
     */
    private String getVariableHelpText() {
        // 获取内置文档
        String builtInDocs = VariableGenerator.getBuiltInVariablesDocumentation();
        
        // 获取自定义变量
        List<Map<String, String>> allVars = VariableGenerator.getInstance().getVariableDefinitions();
        String customDocs = allVars.stream()
                .filter(v -> !v.get("format").startsWith("__")) // 过滤掉内置变量
                .map(v -> v.get("format") + "\n  " + v.get("description"))
                .collect(Collectors.joining("\n\n"));
        
        StringBuilder tooltipTextBuilder = new StringBuilder();
        tooltipTextBuilder.append(builtInDocs);
        
        if (!customDocs.isEmpty()) {
            tooltipTextBuilder.append("\n\n------------------------------\n\n");
            tooltipTextBuilder.append("自定义变量:\n\n");
            tooltipTextBuilder.append(customDocs);
        }
        
        return tooltipTextBuilder.toString();
    }
    
    /**
     * 响应检查帮助文本
     */
    private String getResponseCheckHelpText() {
        return "响应检查配置说明:\n\n" +
               "1. STATUS\n" +
               "  - Expression: (不使用)\n" +
               "  - Operator: IS\n" +
               "  - Expect: 期望的HTTP状态码 (例如: 200, 404)\n\n" +
               "2. JSON_PATH\n" +
               "  - Expression: JSONPath表达式 (例如: $.data.id)\n" +
               "  - Operator: IS, CONTAINS\n" +
               "  - Expect: 期望的值\n" +
               "  - Save As: (可选) 保存提取值的变量名 (例如: myToken)\n\n" +
               "3. XPATH\n" +
               "  - Expression: XML的XPath表达式 (例如: //user/id)\n" +
               "  - Operator: IS, CONTAINS\n" +
               "  - Expect: 期望的值\n" +
               "  - Save As: (可选) 变量名\n\n" +
               "4. REGEX\n" +
               "  - Expression: 带捕获组的正则表达式 (例如: token=(.*?);)\n" +
               "  - Operator: IS, CONTAINS\n" +
               "  - Expect: 期望的值\n" +
               "  - Save As: (可选) 变量名\n\n" +
               "5. DIFF\n" +
               "  - Expression: 引用键 `TCID.JSONPath` (例如: GET_BALANCE.data.balance)\n" +
               "  - Operator: IS\n" +
               "  - Expect: 期望的数值差异 (after - before)\n" +
               "  - Save As: (不使用)";
    }
} 