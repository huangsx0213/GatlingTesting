package com.qa.app.ui.util;

import javafx.scene.control.Tooltip;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.qa.app.service.util.VariableGenerator;

/**
 * Centralized factory for reusable help tooltips across the UI.
 * This class builds and caches Tooltip instances so that every ViewModel
 * can obtain consistent help messages without duplicating code.
 */
public final class HelpTooltipManager {

    private static Tooltip responseCheckTooltip;
    private static Tooltip executionFlowTooltip;
    private static Tooltip functionalTestTooltip;

    private static final String BUILT_IN_VARIABLE_DOC = "Built-in variables usage examples:\n\n" +
            "1. UUID: __UUID\n" +
            "   Example: \"__UUID\" → \"f47ac10b-58cc-4372-a567-0e02b2c3d479\"\n\n" +
            "2. Timestamp (ms): __TIMESTAMP\n" +
            "   Example: \"__TIMESTAMP\" → \"1684932156123\"\n\n" +
            "3. Date/Time: __DATETIME(format[, timezone])\n" +
            "   Examples:\n" +
            "   - \"__DATETIME(yyyy-MM-dd HH:mm:ss)\" → \"2025-07-05 12:30:45\"\n" +
            "   - \"__DATETIME(yyyyMMddHHmmss)\" → \"20250705123045\"\n" +
            "   - \"__DATETIME(yyyy-MM-dd'T'HH:mm:ssXXX, Asia/Shanghai)\" → \"2025-07-05T12:30:45+08:00\"\n\n" +
            "4. Prefix + Timestamp (ms): __PREFIX_TIMESTAMP(prefix)\n" +
            "   Example: \"__PREFIX_TIMESTAMP(ORDER_)\" → \"ORDER_1684932156123\"\n\n" +
            "5. Prefix + DateTime: __PREFIX_DATETIME(prefix,format[, timezone])\n" +
            "   Example: \"__PREFIX_DATETIME(BATCH_,yyyyMMdd,America/New_York)\" → \"BATCH_20230524\"\n\n" +
            "6. Random String: __RANDOM_STRING(length,mode)\n" +
            "   Modes available:\n" +
            "   - a: Alphanumeric (default)\n" +
            "   - u: Uppercase only\n" +
            "   - l: Lowercase only\n" +
            "   - m: Mixed case (no numbers)\n" +
            "   - n: Numeric only\n" +
            "   Example: \"__RANDOM_STRING(8,a)\" → \"A3bX71pQ\"";

    private HelpTooltipManager() {
        // Utility class – no instance.
    }

    /**
     * Returns a singleton tooltip describing how to configure response checks.
     * The content is static; therefore we cache the Tooltip instance.
     *
     * @return a Tooltip with response-check help text
     */
    public static Tooltip getResponseCheckTooltip() {
        if (responseCheckTooltip == null) {
            responseCheckTooltip = buildResponseCheckTooltip();
        }
        return responseCheckTooltip;
    }

    /**
     * Returns a singleton tooltip that explains the runtime execution order
     * (Setup → DIFF_PRE → PRE_CHECK → Main → DIFF_PST → PST_CHECK → Teardown).
     */
    public static Tooltip getExecutionFlowTooltip() {
        if (executionFlowTooltip == null) {
            executionFlowTooltip = buildExecutionFlowTooltip();
        }
        return executionFlowTooltip;
    }

    /**
     * Tooltip explaining Functional Test mode in Scenario.
     */
    public static Tooltip getFunctionalTestTooltip() {
        if (functionalTestTooltip == null) {
            functionalTestTooltip = buildFunctionalTestTooltip();
        }
        return functionalTestTooltip;
    }

    /**
     * Builds a fresh tooltip describing available dynamic variables.
     * This tooltip is rebuilt on every call so that it always reflects
     * the latest custom variable definitions.
     *
     * @return a newly constructed Tooltip for dynamic variables
     */
    public static Tooltip buildVariableTooltip() {
        String builtInDocs = BUILT_IN_VARIABLE_DOC;
        List<Map<String, String>> allVars = VariableGenerator.getInstance().getVariableDefinitions();
        String customDocs = allVars.stream()
                .filter(v -> !v.get("format").startsWith("__")) // exclude built-ins
                .map(v -> v.get("format") + "\n  " + v.get("description"))
                .collect(Collectors.joining("\n\n"));

        StringBuilder tooltipText = new StringBuilder();
        tooltipText.append(builtInDocs);

        if (!customDocs.isEmpty()) {
            tooltipText.append("\n\n------------------------------\n\n");
            tooltipText.append("Custom Variables:\n\n");
            tooltipText.append(customDocs);
        }

        Tooltip tooltip = new Tooltip(tooltipText.toString());
        tooltip.setStyle("-fx-font-size: 14px;");
        tooltip.setAutoHide(true);
        return tooltip;
    }

    public static String getBuiltInVariableDoc() {
        return BUILT_IN_VARIABLE_DOC;
    }

    // ---------------------------------------------------------------------
    // Private helper methods
    // ---------------------------------------------------------------------

    private static Tooltip buildResponseCheckTooltip() {
        String text = "How to configure response checks:\n\n" +
                "1. STATUS\n" +
                "  - Expression: (Not used)\n" +
                "  - Operator: IS\n" +
                "  - Expect: Expected HTTP status code (e.g., 200, 404).\n\n" +
                "2. JSON_PATH\n" +
                "  - Expression: JSONPath to extract value (e.g., $.data.id).\n" +
                "  - Operator: IS, CONTAINS.\n" +
                "  - Expect: The expected value.\n" +
                "  - Save As: (Optional) Variable name to save the value for later use (e.g., myToken).\n\n" +
                "3. XPATH\n" +
                "  - Expression: XPath for XML responses (e.g., //user/id).\n" +
                "  - Operator: IS, CONTAINS.\n" +
                "  - Expect: The expected value.\n" +
                "  - Save As: (Optional) Variable name.\n\n" +
                "4. REGEX\n" +
                "  - Expression: Regex with a capturing group (e.g., token=(.*?);).\n" +
                "  - Operator: IS, CONTAINS.\n" +
                "  - Expect: The expected value.\n" +
                "  - Save As: (Optional) Variable name.\n\n" +
                "5. DIFF\n" +
                "  - Expression: Reference key `TCID.JSONPath` (e.g., Position.$.balance).\n" +
                "  - Operator: IS\n" +
                "  - Expect: Expected numerical difference (after - before).\n" +
                "  - Save As: (Not used).\n\n" +
                "Note: If \"Save As\" is provided, the extracted value will be saved as a variable named \"TCID.variableName\".\n" +
                "You can use it in later test cases with ${TCID.variableName}.";

        Tooltip tooltip = new Tooltip(text);
        tooltip.setStyle("-fx-font-size: 14px;");
        tooltip.setAutoHide(true);
        return tooltip;
    }

    private static Tooltip buildExecutionFlowTooltip() {
        String text = "Execution sequence when running tests:\n\n" +
                "1. Setup script(s) – executed in listed order.\n" +
                "2. DIFF_PRE – reference API call(s) to capture \"before\" value for DIFF checks.\n" +
                "3. PRE_CHECK – reference API call(s) to validate pre-condition fields.\n" +
                "4. Main request – the test case itself, performs STATUS / JSON_PATH etc.\n" +
                "5. DIFF_PST – reference API call(s) to capture \"after\" value for DIFF checks.\n" +
                "6. PST_CHECK – reference API call(s) to validate result fields.\n" +
                "7. Teardown script(s).\n\n" +
                "Evaluation logic:\n" +
                "• DIFF = (after – before) compared with Expect.\n" +
                "• PRE_CHECK / PST_CHECK: compare field value with Expect using IS / CONTAINS / MATCHES.\n" +
                "• All checks recorded in final report under their respective groups.";

        Tooltip t = new Tooltip(text);
        t.setStyle("-fx-font-size: 14px;");
        t.setAutoHide(true);
        return t;
    }

    private static Tooltip buildFunctionalTestTooltip() {
        String txt = "Functional Test mode runs the scenario sequentially with a single virtual user.\n\n" +
                "• Ignores thread-group load model settings.\n" +
                "• Executes each step's Setup → Main → Teardown chain.\n" +
                "• Generates JSON report identical to Gatling Test report.\n" +
                "Use this mode for functional / API regression verification rather than load testing.";
        Tooltip t = new Tooltip(txt);
        t.setStyle("-fx-font-size: 14px;");
        t.setAutoHide(true);
        return t;
    }
} 