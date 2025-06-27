package com.qa.app.util;

import com.qa.app.model.reports.CaseReport;
import com.qa.app.model.reports.FunctionalTestReport;
import com.qa.app.model.reports.ModeGroup;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

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

        // Build HTML ------------------------------------------------------
        String html = """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8"/>
                    <title>Gatling Functional Test Summary</title>
                    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
                    <script src="https://cdn.jsdelivr.net/npm/chartjs-plugin-datalabels@2"></script>
                    <style>
                        :root {
                            --pass-color: #4caf50;
                            --fail-color: #f44336;
                            --bg-color: #f7f9fc;
                            --card-bg: #ffffff;
                            --text-color: #2c3e50;
                        }
                        * { box-sizing: border-box; }
                        html, body { margin: 0; padding: 0; font-family: "Segoe UI", Arial, sans-serif; background: var(--bg-color); color: var(--text-color); }
                        header { background: var(--card-bg); padding: 10px 16px; box-shadow: 0 1px 3px rgba(0,0,0,.05); }
                        header h1 { margin: 0; font-size: 1.4rem; }

                        main { width: 100%%; max-width: 1200px; margin: 0 auto; padding: 6px 12px; }

                        .card { background: var(--card-bg); border-radius: 8px; padding: 12px 16px; box-shadow: 0 1px 4px rgba(0,0,0,.04); margin-bottom: 12px; width: 100%%; }

                        .summary-table { width: 100%%; margin-top: 6px; border-collapse: separate; border-spacing:0; border: 1px solid #dee2e6; border-radius:6px; overflow:hidden; }
                        .summary-table th, .summary-table td { padding: 10px 14px; text-align: center; border:1px solid #c0c4c8; }
                        .summary-table th { background: #f5f7fa; font-weight:600; color: var(--text-color); }
                        .summary-table td { background:#f9fbfd; }
                        .summary-table tr:nth-child(odd) td { background:#f9fbfd; }
                        .pass { color: var(--pass-color); font-weight:600; }
                        .fail { color: var(--fail-color); font-weight:600; }
                        .summary-table tr:last-child td { border-bottom:none; }
                        .summary-table tr td:last-child, .summary-table tr th:last-child { border-right:none; }

                        .charts { display: flex; flex-wrap: wrap; gap: 24px; justify-content: center; }
                        .chart-container { flex: 1 1 400px; max-width: 520px; height: 260px; display:flex; align-items:center; justify-content:center; }
                        .chart-container canvas { height: 260px !important; }

                        @media (max-width: 600px) {
                            .chart-container { flex: 1 1 100%%; height: 180px; }
                            .chart-container canvas { height: 180px !important; }
                        }
                    </style>
                </head>
                <body>
                    <main>
                    <section class="card">
                        <h3 style="margin:4px 0 8px 0;">Aggregated Statistics</h3>
                        <table class="summary-table">
                        <tr><th>Test Summary</th><th>Total</th><th class="pass">Passed</th><th class="fail">Failed</th><th>Pass Rate</th></tr>
                        <tr><th>Suites</th><td>%d</td><td class="pass">%d</td><td class="fail">%d</td><td>%.2f%%</td></tr>
                        <tr><th>Tests</th><td>%d</td><td class="pass">%d</td><td class="fail">%d</td><td>%.2f%%</td></tr>
                        </table>
                    </section>

                    <section class="card">
                        <h3 style="margin:4px 0 8px 0;">Visual Breakdown</h3>
                        <div class="charts">
                            <div class="chart-container"><canvas id="suiteChart"></canvas></div>
                            <div class="chart-container"><canvas id="testChart"></canvas></div>
                        </div>
                    </section>

                    <section class="card" style="text-align:justify;">
                        <p style="font-size:0.82rem; color:#555; margin:4px 2px; line-height:1.4;">This report consolidates execution results generated by the Gatling Functional Test Runner. It provides at-a-glance statistics and visual representations so that teams can quickly identify success rate and failure distribution.</p>
                    </section>

                    <footer style="text-align:center; font-size:0.75rem; color:#888; padding:4px 0;">Generated — %s</footer>
                    </main>
                    <script>
                        Chart.register(ChartDataLabels);

                        const suiteCtx = document.getElementById('suiteChart').getContext('2d');
                        new Chart(suiteCtx, {
                            type: 'pie',
                            data: {
                                labels: ['Passed', 'Failed'],
                                datasets: [{
                                    data: [%d, %d],
                                    backgroundColor: ['#4caf50', '#f44336']
                                }]
                            },
                            options: { plugins: { title:{display:true, text:'Suites'}, legend: { display:false }, datalabels: { color:'#fff', font:{weight:'bold'}, formatter:(v,ctx)=>{const d=ctx.chart.data.datasets[0].data;const sum=d.reduce((a,b)=>a+b,0);return (v/sum*100).toFixed(1)+'%%';} } } }
                        });

                        const testCtx = document.getElementById('testChart').getContext('2d');
                        new Chart(testCtx, {
                            type: 'pie',
                            data: {
                                labels: ['Passed', 'Failed'],
                                datasets: [{
                                    data: [%d, %d],
                                    backgroundColor: ['#4caf50', '#f44336']
                                }]
                            },
                            options: { plugins: { title:{display:true, text:'Tests'}, legend: { display:false }, datalabels: { color:'#fff', font:{weight:'bold'}, formatter:(v,ctx)=>{const d=ctx.chart.data.datasets[0].data;const sum=d.reduce((a,b)=>a+b,0);return (v/sum*100).toFixed(1)+'%%';} } } }
                        });
                    </script>
                </body>
                </html>
                """.formatted(
                suiteTotal, suitePassed, suiteFailed, suitePassRate,
                testTotal, testPassed, testFailed, testPassRate,
                java.time.LocalDateTime.now().withNano(0),
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
} 