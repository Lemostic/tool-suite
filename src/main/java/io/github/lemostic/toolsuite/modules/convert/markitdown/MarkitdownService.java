package io.github.lemostic.toolsuite.modules.convert.markitdown;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for calling Python markitdown to convert documents to markdown.
 */
public class MarkitdownService {

    private static final Pattern BASE64_IMAGE_PATTERN =
            Pattern.compile("!\\[([^\\]]*)\\]\\(data:image/(\\w+);base64,([^\\)]+)\\)");

    private static String customPythonPath = null;

    /**
     * Set custom Python path for markitdown.
     */
    public static void setCustomPythonPath(String path) {
        customPythonPath = path;
    }

    /**
     * Configuration for a markitdown conversion.
     */
    public static class ConvertConfig {
        private File inputFile;
        private File outputFile;
        private String imageOutputDir;
        private int timeoutSeconds = 120;

        public File getInputFile() { return inputFile; }
        public void setInputFile(File inputFile) { this.inputFile = inputFile; }
        public File getOutputFile() { return outputFile; }
        public void setOutputFile(File outputFile) { this.outputFile = outputFile; }
        public String getImageOutputDir() { return imageOutputDir; }
        public void setImageOutputDir(String imageOutputDir) { this.imageOutputDir = imageOutputDir; }
        public int getTimeoutSeconds() { return timeoutSeconds; }
        public void setTimeoutSeconds(int timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    }

    /**
     * Result for a markitdown conversion.
     */
    public static class ConvertResult {
        private final boolean success;
        private final String message;
        private final String markdownContent;
        private final File outputFile;
        private final int extractedImages;
        private final long fileSizeBytes;

        public ConvertResult(boolean success, String message, String markdownContent,
                             File outputFile, int extractedImages, long fileSizeBytes) {
            this.success = success;
            this.message = message;
            this.markdownContent = markdownContent;
            this.outputFile = outputFile;
            this.extractedImages = extractedImages;
            this.fileSizeBytes = fileSizeBytes;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public String getMarkdownContent() { return markdownContent; }
        public File getOutputFile() { return outputFile; }
        public int getExtractedImages() { return extractedImages; }
        public long getFileSizeBytes() { return fileSizeBytes; }

        public static ConvertResult success(String message, String content, File outputFile,
                                             int extractedImages, long fileSizeBytes) {
            return new ConvertResult(true, message, content, outputFile, extractedImages, fileSizeBytes);
        }

        public static ConvertResult failure(String message) {
            return new ConvertResult(false, message, null, null, 0, 0);
        }
    }

    /**
     * Check whether Python is available on the system.
     */
    public static boolean isPythonAvailable() {
        return isPythonAvailable(null);
    }

    /**
     * Check whether Python is available at the specified path.
     */
    public static boolean isPythonAvailable(String pythonPath) {
        String cmd = pythonPath != null && !pythonPath.isEmpty() ? pythonPath : "python";
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd, "--version");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            if (pythonPath == null || pythonPath.isEmpty()) {
                try {
                    ProcessBuilder pb = new ProcessBuilder("python3", "--version");
                    pb.redirectErrorStream(true);
                    Process process = pb.start();
                    boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                    return finished && process.exitValue() == 0;
                } catch (Exception e2) {
                    return false;
                }
            }
            return false;
        }
    }

    /**
     * Check whether markitdown is installed.
     */
    public static boolean isMarkitdownInstalled() {
        return isMarkitdownInstalled(null);
    }

    /**
     * Check whether markitdown is installed for the specified Python path.
     */
    public static boolean isMarkitdownInstalled(String pythonPath) {
        String cmd = pythonPath != null && !pythonPath.isEmpty() ? pythonPath : "python";
        try {
            Process process = runCommand(cmd, "-m", "markitdown", "--version");
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            if (pythonPath == null || pythonPath.isEmpty()) {
                try {
                    Process process = runCommand("python3", "-m", "markitdown", "--version");
                    boolean finished = process.waitFor(10, TimeUnit.SECONDS);
                    return finished && process.exitValue() == 0;
                } catch (Exception e2) {
                    return false;
                }
            }
            return false;
        }
    }

    /**
     * Get the Python executable command that has markitdown installed.
     */
    public static String getPythonCommand() {
        if (customPythonPath != null && !customPythonPath.isEmpty()) {
            if (isMarkitdownInstalledForCommand(customPythonPath)) {
                return customPythonPath;
            }
        }
        if (isMarkitdownInstalledForCommand("python")) {
            return "python";
        }
        if (isMarkitdownInstalledForCommand("python3")) {
            return "python3";
        }
        return null;
    }

    private static boolean isMarkitdownInstalledForCommand(String pythonCmd) {
        try {
            Process process = runCommand(pythonCmd, "-m", "markitdown", "--version");
            boolean finished = process.waitFor(10, TimeUnit.SECONDS);
            return finished && process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Run a markitdown conversion.
     */
    public ConvertResult convert(ConvertConfig config) {
        if (!isPythonAvailable(customPythonPath)) {
            return ConvertResult.failure("未检测到 Python 环境，请先安装 Python 3.8+ 或手动指定路径");
        }

        String pythonCmd = getPythonCommand();
        if (pythonCmd == null) {
            return ConvertResult.failure(
                "未安装 markitdown 库，请运行: pip install 'markitdown[all]'\n" +
                "该命令会安装 markitdown 及其所有可选依赖（支持更多文档格式）"
            );
        }

        try {
            List<String> command = buildCommand(config, pythonCmd);
            Process process = runCommand(command.toArray(new String[0]));
            String stdout = readStream(process.getInputStream());
            String stderr = readStream(process.getErrorStream());
            boolean finished = process.waitFor(config.getTimeoutSeconds(), TimeUnit.SECONDS);

            if (!finished) {
                process.destroyForcibly();
                return ConvertResult.failure("转换超时（" + config.getTimeoutSeconds() + "秒），请检查文件大小或增加超时时间");
            }

            int exitCode = process.exitValue();
            if (exitCode != 0) {
                String errorMsg = stderr.isEmpty() ? "退出码: " + exitCode : stderr;
                return ConvertResult.failure("转换失败: " + errorMsg.trim());
            }

            String markdownContent;
            if (config.getOutputFile() != null) {
                markdownContent = Files.readString(config.getOutputFile().toPath(), StandardCharsets.UTF_8);
            } else {
                markdownContent = stdout;
            }

            int extractedImages = 0;
            if (config.getImageOutputDir() != null && !config.getImageOutputDir().isBlank()) {
                ImageExtractionResult imgResult = extractBase64Images(markdownContent, config);
                if (imgResult != null) {
                    markdownContent = imgResult.markdown;
                    if (config.getOutputFile() != null) {
                        Files.writeString(config.getOutputFile().toPath(), imgResult.markdown, StandardCharsets.UTF_8);
                    }
                    extractedImages = imgResult.count;
                }
            }

            long fileSize = 0;
            if (config.getOutputFile() != null && config.getOutputFile().exists()) {
                fileSize = config.getOutputFile().length();
            } else {
                fileSize = markdownContent.getBytes(StandardCharsets.UTF_8).length;
            }

            String msg = String.format("转换成功！输出: %s",
                    config.getOutputFile() != null ? config.getOutputFile().getAbsolutePath() : "stdout");

            return ConvertResult.success(msg, markdownContent, config.getOutputFile(), extractedImages, fileSize);

        } catch (IOException e) {
            return ConvertResult.failure("IO 错误: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ConvertResult.failure("转换被中断");
        } catch (Exception e) {
            return ConvertResult.failure("未知错误: " + e.getMessage());
        }
    }

    private List<String> buildCommand(ConvertConfig config, String pythonCmd) {
        List<String> cmd = new ArrayList<>();
        cmd.add(pythonCmd);
        cmd.add("-m");
        cmd.add("markitdown");

        if (config.getOutputFile() != null) {
            cmd.add("-o");
            cmd.add(config.getOutputFile().getAbsolutePath());
        }

        cmd.add(config.getInputFile().getAbsolutePath());

        return cmd;
    }

    private static class ImageExtractionResult {
        final String markdown;
        final int count;
        ImageExtractionResult(String markdown, int count) {
            this.markdown = markdown;
            this.count = count;
        }
    }

    private ImageExtractionResult extractBase64Images(String markdown, ConvertConfig config) throws IOException {
        if (markdown == null || markdown.isEmpty()) {
            return null;
        }

        Path imageDir = Paths.get(config.getImageOutputDir());
        Files.createDirectories(imageDir);

        Matcher matcher = BASE64_IMAGE_PATTERN.matcher(markdown);
        int count = 0;

        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String altText = matcher.group(1);
            String ext = matcher.group(2);
            String base64Data = matcher.group(3);

            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            String filename = "image_" + (count + 1) + "." + normalizeExtension(ext);
            Path imagePath = imageDir.resolve(filename);
            Files.write(imagePath, imageBytes);

            String relativePath = imageDir.relativize(imagePath).toString().replace('\\', '/');

            matcher.appendReplacement(result,
                    "![" + Matcher.quoteReplacement(altText) + "](" + Matcher.quoteReplacement(relativePath) + ")");
            count++;
        }
        matcher.appendTail(result);

        return new ImageExtractionResult(result.toString(), count);
    }

    private String normalizeExtension(String ext) {
        switch (ext.toLowerCase()) {
            case "svg+xml": return "svg";
            case "jpeg": return "jpg";
            default: return ext;
        }
    }

    private static Process runCommand(String... command) throws IOException {
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(false);
        return pb.start();
    }

    private String readStream(java.io.InputStream stream) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (sb.length() > 0) sb.append("\n");
                sb.append(line);
            }
        }
        return sb.toString();
    }
}