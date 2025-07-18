package com.qa.app.util;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Combined utility: export project code into self-contained HTML (gatling.code.html)
 * and import (restore) code from that HTML.
 *
 * 1. Export (default):
 *    java GatlingCodeArchiver export [<projectDir>] [<outputHtml>]
 *
 * 2. Import:
 *    java GatlingCodeArchiver import <inputHtml> <targetDir>
 *
 * HTML title: "gatling testing code".
 * HTML embeds raw packaged txt inside a <script id="code-data" type="text/plain"> tag to avoid escaping.
 */
public class GatlingCodeArchiver {

    /* ---------- Config ---------- */
    private static final Set<String> TEXT_EXTENSIONS = Set.of(
            "java", "xml", "properties", "conf", "config", "txt", "fxml", "css", "md", "json", "yml", "yaml","png","js");
    private static final List<String> EXCLUDE_DIRS = List.of(
            ".git", "target", ".idea", "out", "logs", ".cursor", ".vscode");

    /* ---------- Entry ---------- */
    public static void main(String[] args) throws Exception {
        if (args.length == 0 || "export".equalsIgnoreCase(args[0])) {
            // 默认项目目录为当前目录
            String projectPath = ".";
            String outputPath = "gatling.code.html";
            
            if (args.length > 1) {
                projectPath = args[1];
            }
            if (args.length > 2) {
                outputPath = args[2];
            }
            
            Path project = Paths.get(projectPath);
            Path out = Paths.get(outputPath);
            exportHtml(project, out);
        } else if ("import".equalsIgnoreCase(args[0])) {
            if (args.length < 2) {
                System.out.println("Usage: import <inputHtml> [<targetDir>]");
                return;
            }
            Path html = Paths.get(args[1]);
            
            // 默认目标目录为当前目录下的 import 文件夹
            String targetPath = "./import";
            if (args.length > 2) {
                targetPath = args[2];
            }
            
            Path target = Paths.get(targetPath);
            importFromHtml(html, target);
        } else {
            System.out.println("Unknown command. Usage:\n  export [projectDir] [outputHtml]\n  import <inputHtml> [targetDir]");
        }
    }

    /* ---------- Export ---------- */
    private static void exportHtml(Path root, Path outputHtml) throws IOException {
        if (!Files.isDirectory(root)) {
            throw new IllegalArgumentException("Project directory not found: " + root);
        }

        // Build packaged text (NDJSON-like)
        StringBuilder pkgBuilder = new StringBuilder();
        Files.walk(root)
                .filter(Files::isRegularFile)
                .filter(p -> shouldInclude(p))
                .forEach(p -> appendFileEntry(root, p, pkgBuilder));
        String packagedText = pkgBuilder.toString();

        // Read this class's source (best effort)
        Path selfSrcPath = root.resolve("src/main/java/com/qa/app/util/GatlingCodeArchiver.java");
        String selfSource = "";
        if (Files.exists(selfSrcPath)) {
            selfSource = Files.readString(selfSrcPath, StandardCharsets.UTF_8);
        } else {
            // Try alternative paths
            Path alt1 = root.resolve("GatlingCodeArchiver.java");
            Path alt2 = root.resolve("src/GatlingCodeArchiver.java");
            if (Files.exists(alt1)) {
                selfSource = Files.readString(alt1, StandardCharsets.UTF_8);
            } else if (Files.exists(alt2)) {
                selfSource = Files.readString(alt2, StandardCharsets.UTF_8);
            }
        }

        // Build HTML
        String html = buildHtml(packagedText, selfSource);

        // Ensure dir exists
        if (outputHtml.getParent() != null) {
            Files.createDirectories(outputHtml.getParent());
        }
        Files.writeString(outputHtml, html, StandardCharsets.UTF_8);
        System.out.println("Exported to " + outputHtml.toAbsolutePath());
    }

    private static boolean shouldInclude(Path file) {
        for (Path part : file) {
            if (EXCLUDE_DIRS.contains(part.toString())) return false;
        }
        String name = file.getFileName().toString();
        int idx = name.lastIndexOf('.');
        if (idx == -1) return false;
        return TEXT_EXTENSIONS.contains(name.substring(idx + 1).toLowerCase());
    }

    private static void appendFileEntry(Path root, Path file, StringBuilder sb) {
        try {
            Path rel = root.relativize(file);
            String b64 = Base64.getEncoder().encodeToString(Files.readAllBytes(file));
            sb.append("PATH:").append(rel.toString().replace('\\', '/')).append('\n');
            sb.append(b64).append('\n');
            sb.append("END\n");
        } catch (IOException ex) {
            System.err.println("Skip file " + file + " due: " + ex.getMessage());
        }
    }

    /* ---------- Import ---------- */
    private static void importFromHtml(Path htmlFile, Path targetDir) throws IOException {
        if (!Files.exists(htmlFile)) {
            throw new IllegalArgumentException("HTML file not found: " + htmlFile);
        }
        // Read html as string
        String html = Files.readString(htmlFile, StandardCharsets.UTF_8);
        // Extract between <script id="code-data" type="text/plain"> ... </script>
        Pattern pattern = Pattern.compile("<script[^>]*id=\"code-data\"[^>]*>([\\s\\S]*?)</script>", Pattern.CASE_INSENSITIVE);
        Matcher m = pattern.matcher(html);
        if (!m.find()) {
            throw new IllegalStateException("No code-data section found in HTML");
        }
        String pkg = m.group(1).trim();
        // Ensure target dir exists
        Files.createDirectories(targetDir);
        parseAndWrite(pkg, targetDir);
        System.out.println("Import completed to " + targetDir.toAbsolutePath());
    }

    private static void parseAndWrite(String pkg, Path targetDir) throws IOException {
        try (BufferedReader br = new BufferedReader(new StringReader(pkg))) {
            String line;
            String currentPath = null;
            StringBuilder b64 = new StringBuilder();
            while ((line = br.readLine()) != null) {
                if (line.startsWith("PATH:")) {
                    currentPath = line.substring(5);
                    b64.setLength(0);
                } else if (line.equals("END")) {
                    if (currentPath != null) {
                        Path outFile = targetDir.resolve(currentPath);
                        Files.createDirectories(outFile.getParent());
                        byte[] data = Base64.getDecoder().decode(b64.toString());
                        Files.write(outFile, data);
                    }
                    currentPath = null;
                } else {
                    b64.append(line);
                }
            }
        }
    }

    /* ---------- HTML template ---------- */
    private static String buildHtml(String packagedText, String archiverSource) {
        String usage = "Export Examples:\n" +
                       "  java GatlingCodeArchiver.java export\n" +
                       "  java GatlingCodeArchiver.java export /path/to/project\n" +
                       "  java GatlingCodeArchiver.java export /path/to/project /path/to/output.html\n" +
                       "\n" +
                       "Import Examples:\n" +
                       "  java GatlingCodeArchiver.java import input.html\n" +
                       "  java GatlingCodeArchiver.java import input.html /path/to/restored/dir\n" +
                       "\n" +
                       "Defaults:\n" +
                       "  Export: current directory -> gatling.code.html\n" +
                       "  Import: -> ./import directory";

        String safePkg = packagedText.replace("</script>", "<\\/script>");
        String safeSrc = archiverSource.replace("</script>", "<\\/script>");

        return """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="utf-8" />
                    <title>gatling testing code</title>
                    <style>
                        body { font-family: Arial, sans-serif; margin: 20px; }
                        pre { background:#f5f5f5; border:1px solid #ddd; padding:10px; overflow:auto; }
                        button { margin-bottom:10px; padding:6px 12px; }
                    </style>
                </head>
                <body>
                    <h2>gatling testing code</h2>
                    <h3>Usage</h3>
                    <pre>%s</pre>
                    <button onclick="togglePackage()">Show / Hide Package</button>
                    <button onclick="copyPackage()">Copy Package To Clipboard</button>
                    <pre id="display" style="display:none;"></pre>
                    <h3>GatlingCodeArchiver.java</h3>
                    <button onclick="toggleSrc()">Show / Hide Source</button>
                    <button onclick="copySource()">Copy Source Code</button>
                    <pre id="src" style="display:none;"></pre>
                    <script id="code-data" type="text/plain">%s</script>
                    <script id="source-data" type="text/plain">%s</script>
                    <script>
                        const raw = document.getElementById('code-data').textContent.trim();
                        const srcRaw = document.getElementById('source-data').textContent.trim();
                        document.getElementById('display').textContent = raw;
                        document.getElementById('src').textContent = srcRaw;
                        function copyPackage() {
                            navigator.clipboard.writeText(raw).then(()=>alert('Package copied!')).catch(e=>alert('Copy failed: '+e));
                        }
                        function togglePackage(){
                            const el=document.getElementById('display');
                            el.style.display = el.style.display==='none'? 'block':'none';
                        }
                        function toggleSrc(){
                            const el=document.getElementById('src');
                            el.style.display = el.style.display==='none'? 'block':'none';
                        }
                        function copySource() {
                            navigator.clipboard.writeText(srcRaw).then(()=>alert('Source copied!')).catch(e=>alert('Copy failed: '+e));
                        }
                    </script>
                </body>
                </html>
                """.formatted(usage, safePkg, safeSrc);
    }
}