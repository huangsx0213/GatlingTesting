package com.qa.app.service.reports;

import com.qa.app.model.reports.CaseReport;
import com.qa.app.model.reports.FunctionalTestReport;
import com.qa.app.model.reports.ModeGroup;
import com.qa.app.model.reports.RequestReport;
import com.qa.app.model.reports.RequestInfo;
import com.qa.app.model.reports.ResponseInfo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.io.InputStream;
import java.nio.file.StandardCopyOption;


/**
 * Utility class responsible for generating a lightweight HTML summary page for {@link FunctionalTestReport} data.
 * <p>
 * The generated page contains:
 * <ul>
 *     <li>Summary table for suite & test totals (total / passed / failed / pass-rate).</li>
 *     <li>Two pie charts (Chart.js) visualising pass / fail statistics for suites & tests.</li>
 * </ul>
 */
public final class HtmlSummaryReportGenerator {

    private HtmlSummaryReportGenerator() {
        // utility class – hide constructor
    }

    /**
     * Generates an HTML summary file at the provided {@code targetPath}.
     * Directories are created automatically when missing.
     *
     * @param reports    one or more functional test reports to aggregate
     * @param targetPath target file location (e.g. {@code Path.of("target/gatling/summary.html")})
     * @throws IOException if writing the file fails
     */
    public static void generateHtml(List<FunctionalTestReport> reports, Path targetPath) throws IOException {
        if (reports == null || reports.isEmpty()) {
            throw new IllegalArgumentException("Report list must not be null/empty");
        }

        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }

        copyResourceTo(targetPath.getParent(), "static/js/chart.js");
        copyResourceTo(targetPath.getParent(), "static/js/chartjs-plugin-datalabels.js");

        // Group by suite name
        Map<String, java.util.List<FunctionalTestReport>> suiteMap = new HashMap<>();
        for (FunctionalTestReport r : reports) {
            String suiteName = r.getSuite() != null ? r.getSuite() : "DEFAULT";
            suiteMap.computeIfAbsent(suiteName, k -> new java.util.ArrayList<>()).add(r);
        }

        int suiteTotal = suiteMap.size();
        int suitePassed = 0;
        int testTotal = 0;
        int testPassed = 0;

        for (java.util.Map.Entry<String, java.util.List<FunctionalTestReport>> entry : suiteMap.entrySet()) {
            boolean suiteAllPassed = true;
            for (FunctionalTestReport r : entry.getValue()) {
                for (ModeGroup g : r.getGroups()) {
                    if (g.getMode() == com.qa.app.model.reports.TestMode.MAIN) {
                        for (CaseReport c : g.getCases()) {
                            testTotal++;
                            if (c.isPassed()) {
                                testPassed++;
                            } else {
                                suiteAllPassed = false;
                            }
                        }
                    }
                }
            }
            if (suiteAllPassed) suitePassed++;
        }
        int suiteFailed = suiteTotal - suitePassed;
        int testFailed = testTotal - testPassed;

        double suitePassRate = suiteTotal == 0 ? 0 : ((double) suitePassed / suiteTotal) * 100.0;
        double testPassRate  = testTotal  == 0 ? 0 : ((double) testPassed  / testTotal ) * 100.0;

        String detailsSection = generateDetailsSectionHtml(reports);

        // Build HTML ------------------------------------------------------
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8"/>
                    <title>Gatling Functional Test Summary</title>
                    <script src="chart.js"></script>
                    <script src="chartjs-plugin-datalabels.js"></script>
                    <style>
                        :root {
                            --pass-color: #28a745;
                            --fail-color: #dc3545;
                            --bg-color: #f8f9fa;
                            --card-bg: #ffffff;
                            --text-color: #212529;
                            --text-muted: #6c757d;
                            --border-color: #dee2e6;
                            --hover-bg: #f1f3f5;
                            --font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
                        }
                        * { box-sizing: border-box; }
                        html, body { margin: 0; padding: 0; font-family: var(--font-family); background: var(--bg-color); color: var(--text-color); line-height: 1.5; }
                        
                        main { width: 100%%; max-width: 1600px; margin: 0 auto; padding: 24px; }

                        .main-header { text-align: center; margin-bottom: 24px; }
                        .main-header h1 { margin: 0 0 4px; font-size: 2rem; }
                        .main-header p { margin: 0; color: var(--text-muted); }

                        .summary-wrapper {
                            display: flex;
                            gap: 24px;
                            margin-bottom: 24px;
                        }

                        .summary-box {
                            background: var(--card-bg);
                            border-radius: 8px;
                            border: 1px solid var(--border-color);
                            box-shadow: 0 2px 8px rgba(0,0,0,.05);
                            padding: 20px;
                        }
                        .summary-box.stats { flex: 1; }
                        .summary-box.charts { flex: 2; }
                        .summary-box-header { font-size: 1.25rem; font-weight: 600; margin: 0 0 16px; padding-bottom: 12px; border-bottom: 1px solid var(--border-color); }

                        .summary-table { width: 100%%; border-collapse: collapse; text-align: center; font-size: 0.9rem; }
                        .summary-table th, .summary-table td { padding: 12px; border: 1px solid var(--border-color); }
                        .summary-table thead th { background: #f8f9fa; font-weight: 600; }
                        .summary-table tbody th { text-align: left; font-weight: 600; }

                        .charts-container { display: flex; gap: 20px; align-items: center; justify-content: space-around; }
                        .chart-container { flex: 1; max-width: 300px; height: 260px; }
                        
                        .details-card { background: var(--card-bg); border-radius: 8px; box-shadow: 0 2px 8px rgba(0,0,0,.05); border: 1px solid var(--border-color); overflow: hidden; }
                        .details-header { padding: 16px 20px; border-bottom: 1px solid var(--border-color); display: flex; justify-content: space-between; align-items: center; }
                        .details-header h3 { margin: 0; font-size: 1.25rem; }
                        .details-controls { display: flex; gap: 8px; }
                        .details-controls button { padding: 8px 16px; border: 1px solid var(--border-color); border-radius: 6px; cursor: pointer; background: var(--card-bg); font-weight: 500; transition: background-color 0.2s; }
                        .details-controls button:hover { background: var(--hover-bg); }
                        .details-body { padding: 20px; }

                        .case-card { margin-bottom: 16px; border-radius: 8px; background: #fff; box-shadow: 0 1px 4px rgba(0,0,0,.06); overflow: hidden; border: 1px solid var(--border-color); }
                        .case-card:last-child { margin-bottom: 0; }
                        .case-header { display: flex; align-items: center; gap: 16px; padding: 12px 16px; cursor: pointer; background: #fafbfc; border-bottom: 1px solid var(--border-color); transition: background-color 0.2s; }
                        .case-header:hover { background: var(--hover-bg); }
                        .case-header .case-tcid { flex-grow: 1; font-size: 1.1rem; font-weight: 600; }
                        .case-header .case-result-badge { padding: 4px 10px; border-radius: 12px; color: #fff; font-size: 0.8rem; font-weight: bold; }
                        .case-header .case-result-badge.pass { background-color: var(--pass-color); }
                        .case-header .case-result-badge.fail { background-color: var(--fail-color); }
                        .chevron { width: 10px; height: 10px; border-style: solid; border-width: 0 2px 2px 0; transform: rotate(45deg); transition: transform 0.3s ease; }
                        .case-header.open .chevron { transform: rotate(-135deg); }

                        .case-body { padding: 0; max-height: 0; overflow: hidden; transition: max-height 0.5s ease-in-out; background: #fff; }
                        .case-body.open { padding: 16px; max-height: 5000px; }

                        .request-table { width: 100%%; border-collapse: collapse; font-size: .9rem; border: 1px solid var(--border-color); border-radius: 6px; overflow: hidden; }
                        .request-table th, .request-table td { padding: 10px 14px; border-bottom: 1px solid var(--border-color); text-align: left; }
                        .request-table th { background: #f8f9fa; }
                        .request-table tbody tr:last-child td { border-bottom: none; }
                        
                        .request-summary-row { cursor: pointer; }
                        .request-summary-row:hover { background-color: var(--hover-bg); }
                        .request-summary-row .phase-cell { font-weight: 600; color: var(--text-muted); }
                        .request-summary-row .result-cell { font-weight: 600; }
                        .request-summary-row .result-cell.pass { color: var(--pass-color); }
                        .request-summary-row .result-cell.fail { color: var(--fail-color); }

                        .request-details-row { display: none; }
                        .request-details-row td { padding: 0 !important; background: #fcfdff; }

                        .details-tabs { display: flex; border-bottom: 1px solid var(--border-color); background: #f8f9fa; padding-left: 12px; }
                        .tab-link { background-color: transparent; border: none; outline: none; cursor: pointer; padding: 12px 16px; transition: 0.3s; font-size: 0.95rem; border-bottom: 3px solid transparent; }
                        .tab-link.active { border-bottom-color: var(--pass-color); font-weight: 600; }

                        .tab-content { padding: 16px; }
                        
                        .checks-table { width: 100%%; border-collapse: collapse; }
                        .checks-table th, .checks-table td { border: 1px solid var(--border-color); padding: 12px; text-align: left; vertical-align: middle; }
                        .checks-table tr.fail { background-color: #fff5f5; }
                        .checks-table td.pre-cell { padding: 0; }
                        .checks-table td.pre-cell pre { margin: 0; padding: 12px; background-color: transparent; border-radius: 0; font-size: 0.85rem; white-space: pre-wrap; word-break: break-all; font-family: 'SF Mono', 'Courier New', monospace; }
                        .checks-table td.pre-cell pre.pass { color: var(--pass-color); font-weight: 600; }
                        .checks-table td.pre-cell pre.fail { color: var(--fail-color); font-weight: 600; }
                        
                        .key-value-grid { display: grid; grid-template-columns: auto 1fr; gap: 8px 16px; align-items: center; padding-left: 8px; }
                        .key-value-grid strong { font-weight: 600; }
                        
                        .pre-wrapper { position: relative; }
                        .pre-wrapper pre { background: #212529; color: #f8f9fa; padding: 12px; border-radius: 4px; white-space: pre-wrap; word-break: break-all; font-family: 'SF Mono', 'Courier New', monospace; font-size: 0.85rem; margin-top: 4px;}
                        .copy-btn { position: absolute; top: 8px; right: 8px; background: #6c757d; color: white; border: none; border-radius: 4px; padding: 4px 8px; cursor: pointer; opacity: 0.7; transition: opacity 0.2s; }
                        .pre-wrapper:hover .copy-btn { opacity: 1; }
                        .copy-btn:hover { background: #495057; }

                    </style>
                </head>
                <body>
                    <main>
                        <div class="main-header">
                            <h1>Gatling Functional Test Report</h1>
                            <p>This document serves as a detailed record of the test execution, suitable for result analysis and as testing evidence.</p>
                            <p>Generated on: %s</p>
                        </div>

                        <div class="summary-wrapper">
                            <div class="summary-box stats">
                                <div class="summary-box-header">Aggregated Statistics</div>
                        <table class="summary-table">
                                    <thead>
                                        <tr>
                                            <th>Test Summary</th>
                                            <th>Total</th>
                                            <th class="pass">Passed</th>
                                            <th class="fail">Failed</th>
                                            <th>Pass Rate</th>
                                        </tr>
                                    </thead>
                                    <tbody>
                                        <tr>
                                            <th>Suites</th>
                                            <td>%d</td>
                                            <td class="pass">%d</td>
                                            <td class="fail">%d</td>
                                            <td>%.2f%%</td>
                                        </tr>
                                        <tr>
                                            <th>Tests</th>
                                            <td>%d</td>
                                            <td class="pass">%d</td>
                                            <td class="fail">%d</td>
                                            <td>%.2f%%</td>
                                        </tr>
                                    </tbody>
                        </table>
                            </div>
                            <div class="summary-box charts">
                                <div class="summary-box-header">Visual Breakdown</div>
                                <div class="charts-container">
                            <div class="chart-container"><canvas id="suiteChart"></canvas></div>
                            <div class="chart-container"><canvas id="testChart"></canvas></div>
                                </div>
                            </div>
                        </div>

                        %s
                    </main>
                    <script>
                        Chart.register(ChartDataLabels);

                        const suiteCtx = document.getElementById('suiteChart').getContext('2d');
                        new Chart(suiteCtx, {
                            type: 'doughnut',
                            data: { labels: ['Passed', 'Failed'], datasets: [{ data: [%d, %d], backgroundColor: ['#28a745', '#dc3545'], borderWidth: 0 }] },
                            options: { responsive: true, maintainAspectRatio: false, plugins: { title:{display:true, text:'Suites'}, legend: { display:false }, datalabels: { color:'#fff', font:{weight:'bold', size: 14}, formatter:(v,ctx)=>{const d=ctx.chart.data.datasets[0].data;const sum=d.reduce((a,b)=>a+b,0); return sum > 0 ? (v/sum*100).toFixed(1)+'%%' : '';} } } }
                        });

                        const testCtx = document.getElementById('testChart').getContext('2d');
                        new Chart(testCtx, {
                            type: 'doughnut',
                            data: { labels: ['Passed', 'Failed'], datasets: [{ data: [%d, %d], backgroundColor: ['#28a745', '#dc3545'], borderWidth: 0 }] },
                            options: { responsive: true, maintainAspectRatio: false, plugins: { title:{display:true, text:'Tests'}, legend: { display:false }, datalabels: { color:'#fff', font:{weight:'bold', size: 14}, formatter:(v,ctx)=>{const d=ctx.chart.data.datasets[0].data;const sum=d.reduce((a,b)=>a+b,0); return sum > 0 ? (v/sum*100).toFixed(1)+'%%' : '';} } } }
                        });
                        
                        function setupEventListeners() {
                            // Case expand/collapse
                            document.querySelectorAll('.case-header').forEach(header => {
                                header.addEventListener('click', () => {
                                    header.classList.toggle('open');
                                    header.nextElementSibling.classList.toggle('open');
                                });
                            });
                            
                            // Request expand/collapse
                            document.querySelectorAll('.request-summary-row').forEach(row => {
                                row.addEventListener('click', (e) => {
                                    row.nextElementSibling.style.display = row.nextElementSibling.style.display === 'table-row' ? 'none' : 'table-row';
                                });
                            });

                            // Copy button
                            document.querySelectorAll('.copy-btn').forEach(button => {
                                button.addEventListener('click', (e) => {
                                    e.stopPropagation();
                                    const pre = button.previousElementSibling;
                                    navigator.clipboard.writeText(pre.innerText);
                                    button.innerText = 'Copied!';
                                    setTimeout(() => { button.innerText = 'Copy'; }, 2000);
                                });
                            });
                        }
                        
                        document.addEventListener('DOMContentLoaded', setupEventListeners);

                        function openTab(evt, tabName) {
                            const tabContainer = evt.target.closest('.request-details-row');
                            tabContainer.querySelectorAll('.tab-content').forEach(tc => tc.style.display = 'none');
                            tabContainer.querySelectorAll('.tab-link').forEach(tl => tl.classList.remove('active'));
                            document.getElementById(tabName).style.display = 'block';
                            evt.currentTarget.classList.add('active');
                        }

                        function toggleAll(expand) {
                            document.querySelectorAll('.case-header').forEach(header => {
                                const body = header.nextElementSibling;
                                if (expand && !header.classList.contains('open')) {
                                    header.classList.add('open');
                                    body.classList.add('open');
                                } else if (!expand && header.classList.contains('open')) {
                                    header.classList.remove('open');
                                    body.classList.remove('open');
                                }
                            });
                        }
                    </script>
                </body>
                </html>
                """.formatted(
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                suiteTotal, suitePassed, suiteFailed, suitePassRate,
                testTotal, testPassed, testFailed, testPassRate,
                detailsSection,
                suitePassed, suiteFailed,
                testPassed, testFailed
        );

        // Write file ------------------------------------------------------
        if (targetPath.getParent() != null) {
            Files.createDirectories(targetPath.getParent());
        }
        Files.writeString(targetPath, html, StandardCharsets.UTF_8);
    }

    /**
     * Convenience wrapper for a single {@link FunctionalTestReport}.
     */
    public static void generateHtml(FunctionalTestReport report, Path targetPath) throws IOException {
        generateHtml(java.util.List.of(report), targetPath);
    }

    private static String generateDetailsSectionHtml(List<FunctionalTestReport> reports) {
        if (reports == null || reports.isEmpty()) {
            return "";
        }
        StringBuilder detailsHtml = new StringBuilder();
        detailsHtml.append("""
            <div class="details-card">
                <div class="details-header">
                    <h3>Test Case Details</h3>
                    <div class="details-controls">
                        <button onclick="toggleAll(true)">Expand All Cases</button>
                        <button onclick="toggleAll(false)">Collapse All Cases</button>
                    </div>
                </div>
                <div class="details-body">
        """);

        int requestCounter = 0;
        for (FunctionalTestReport report : reports) {
            String caseStatus = report.isPassed() ? "pass" : "fail";
            detailsHtml.append(String.format("""
                <div class="case-card">
                    <div class="case-header">
                        <span class="case-tcid">Test Case: %s</span>
                        <span class="case-result-badge %s">%s</span>
                        <span class="chevron"></span>
                    </div>
                    <div class="case-body">
            """, escapeHtml(report.getOriginTcid()), caseStatus, report.isPassed() ? "PASS" : "FAIL"));

            detailsHtml.append("""
                <table class="request-table">
                    <thead>
                        <tr>
                            <th>Phase</th>
                            <th>Name</th>
                            <th>Status</th>
                            <th>Latency (ms)</th>
                            <th>Result</th>
                        </tr>
                    </thead>
                    <tbody>
            """);

            for (ModeGroup group : report.getGroups()) {
                if (group.getCases() == null || group.getCases().isEmpty()) continue;
                
                final String phaseName = group.getMode().name();

                for (CaseReport caseReport : group.getCases()) {
                    if (caseReport.getItems() != null) {
                        for (com.qa.app.model.reports.RequestReport item : caseReport.getItems()) {
                            detailsHtml.append(generateRequestRowHtml(item, requestCounter, phaseName));
                            requestCounter++;
                        }
                    }
                }
            }
            detailsHtml.append("</tbody></table>");
            detailsHtml.append("</div></div>");
        }

        detailsHtml.append("</div></div>"); // Close the details-body and details-card
        return detailsHtml.toString();
    }
    
    private static String generateRequestRowHtml(com.qa.app.model.reports.RequestReport item, int counter, String phaseName) {
        StringBuilder sb = new StringBuilder();
        String itemStatusClass = item.isPassed() ? "pass" : "fail";
        
        // Summary Row
        sb.append(String.format("""
            <tr class="request-summary-row">
                <td class="phase-cell">%s</td>
                <td>%s</td>
                <td>%s</td>
                <td>%d</td>
                <td class="result-cell %s">%s</td>
            </tr>
        """, 
                escapeHtml(phaseName),
                escapeHtml(item.getRequestName()),
                item.getResponse() != null ? String.valueOf(item.getResponse().getStatus()) : "N/A",
                item.getResponse() != null ? item.getResponse().getLatencyMs() : 0,
                itemStatusClass,
                item.isPassed() ? "Passed" : "Failed"
        ));

        // Details Row
        String reqTabId = "req-tab-" + counter;
        String respTabId = "resp-tab-" + counter;
        String checksTabId = "checks-tab-" + counter;

        sb.append(String.format("""
            <tr class="request-details-row">
                <td colspan="5">
                    <div class="details-tabs">
                        <button class="tab-link active" onclick="openTab(event, '%s')">Request</button>
                        <button class="tab-link" onclick="openTab(event, '%s')">Response</button>
                        <button class="tab-link" onclick="openTab(event, '%s')">Checks</button>
                    </div>
        """, reqTabId, respTabId, checksTabId));

        // Request Tab
        com.qa.app.model.reports.RequestInfo req = item.getRequest();
        sb.append(String.format("""
            <div id="%s" class="tab-content" style="display:block;">
                <h4>URL & Method</h4>
                <div class="key-value-grid">
                    <strong>Method:</strong> <span>%s</span>
                    <strong>URL:</strong> <span>%s</span>
                </div>
                
                <h4>Headers</h4>
                <div class="pre-wrapper"><pre>%s</pre><button class="copy-btn">Copy</button></div>

                <h4>Request Body</h4>
                <div class="pre-wrapper"><pre>%s</pre><button class="copy-btn">Copy</button></div>
            </div>
        """, reqTabId, escapeHtml(req.getMethod()), escapeHtml(req.getUrl()), escapeHtml(mapToString(req.getHeaders())), escapeHtml(req.getBody())));

        // Response Tab
        com.qa.app.model.reports.ResponseInfo resp = item.getResponse();
        sb.append(String.format("""
            <div id="%s" class="tab-content" style="display:none;">
                <h4>Headers</h4>
                <div class="pre-wrapper"><pre>%s</pre><button class="copy-btn">Copy</button></div>

                <h4>Response Body Sample</h4>
                <div class="pre-wrapper"><pre>%s</pre><button class="copy-btn">Copy</button></div>
            </div>
        """, respTabId, escapeHtml(mapToString(resp.getHeaders())), escapeHtml(resp.getBodySample())));

        // Checks Tab
        sb.append(String.format("<div id=\"%s\" class=\"tab-content\" style=\"display:none;\">", checksTabId));
        sb.append("""
            <table class="checks-table">
                <thead><tr><th>Type</th><th>Expression</th><th>Operator</th><th>Expected</th><th>Actual</th><th>Result</th></tr></thead>
                <tbody>
        """);
        if (item.getChecks() != null) {
            for (com.qa.app.model.reports.CheckReport check : item.getChecks()) {
                sb.append(String.format("""
                    <tr class="%s">
                        <td class="pre-cell"><pre>%s</pre></td>
                        <td class="pre-cell"><pre>%s</pre></td>
                        <td class="pre-cell"><pre>%s</pre></td>
                        <td class="pre-cell"><pre>%s</pre></td>
                        <td class="pre-cell"><pre class="%s">%s</pre></td>
                        <td class="pre-cell"><pre class="%s">%s</pre></td>
                    </tr>
                """, check.isPassed() ? "pass" : "fail",
                        check.getType(), 
                        escapeHtml(check.getExpression()), 
                        check.getOperator(),
                        escapeHtml(check.getExpect()),
                        check.isPassed() ? "pass" : "fail", escapeHtml(check.getActual()),
                        check.isPassed() ? "pass" : "fail", check.isPassed() ? "PASS" : "FAIL"
                ));
            }
        }
        sb.append("</tbody></table></div>");
        
        sb.append("</td></tr>");
        return sb.toString();
    }
    
    private static String mapToString(java.util.Map<String, String> map) {
        if (map == null || map.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        map.forEach((k, v) -> sb.append(k).append(": ").append(v).append("\n"));
        return sb.toString().trim();
    }

    private static String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static void copyResourceTo(Path targetDirectory, String resourceName) throws IOException {
        try (InputStream stream = HtmlSummaryReportGenerator.class.getClassLoader().getResourceAsStream(resourceName)) {
            if (stream == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            Path destination = targetDirectory.resolve(Path.of(resourceName).getFileName());
            Files.copy(stream, destination, StandardCopyOption.REPLACE_EXISTING);
        }
    }
} 