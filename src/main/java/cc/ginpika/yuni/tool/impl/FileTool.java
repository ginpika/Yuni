package cc.ginpika.yuni.tool.impl;

import cc.ginpika.yuni.config.YuniConfig;
import cc.ginpika.yuni.tool.AbstractTool;
import cc.ginpika.yuni.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Slf4j
@Component
public class FileTool extends AbstractTool {

    @Resource
    private ToolRegistry toolRegistry;

    @Resource
    private YuniConfig yuniConfig;

    public FileTool() {
        super("file_operation", "在工作空间目录中读写文件。支持读取文件、写入文件、列出目录、创建目录、删除文件等操作。所有操作限制在工作空间目录内。");
    }

    @PostConstruct
    public void register() {
        toolRegistry.register(this);
    }

    @Override
    protected ObjectNode buildProperties() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("action", createProperty("string", "操作类型：read（读取文件）、write（写入文件）、list（列出目录）、mkdir（创建目录）、delete（删除文件/目录）、exists（检查是否存在）", new String[]{"read", "write", "list", "mkdir", "delete", "exists"}));
        properties.set("path", createProperty("string", "文件或目录路径（相对于工作空间）"));
        properties.set("content", createProperty("string", "文件内容（write 操作必填）"));
        properties.set("recursive", createProperty("boolean", "是否递归操作（用于 list、delete）"));
        return properties;
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{"action", "path"};
    }

    @Override
    public String execute(JsonNode arguments) {
        String action = arguments.has("action") ? arguments.get("action").asText() : null;
        String path = arguments.has("path") ? arguments.get("path").asText() : null;

        if (action == null || action.isEmpty()) {
            return "{\"success\": false, \"error\": \"操作类型不能为空\"}";
        }

        if (path == null || path.isEmpty()) {
            return "{\"success\": false, \"error\": \"路径不能为空\"}";
        }

        try {
            Path workspacePath = getWorkspacePath();
            Path targetPath = resolvePath(workspacePath, path);

            if (targetPath == null) {
                return "{\"success\": false, \"error\": \"路径不合法，超出工作空间范围\"}";
            }

            switch (action) {
                case "read":
                    return readFile(targetPath);
                case "write":
                    String content = arguments.has("content") ? arguments.get("content").asText() : "";
                    return writeFile(targetPath, content);
                case "list":
                    boolean listRecursive = arguments.has("recursive") && arguments.get("recursive").asBoolean();
                    return listDirectory(targetPath, listRecursive);
                case "mkdir":
                    return createDirectory(targetPath);
                case "delete":
                    boolean deleteRecursive = arguments.has("recursive") && arguments.get("recursive").asBoolean();
                    return deletePath(targetPath, deleteRecursive);
                case "exists":
                    return checkExists(targetPath);
                default:
                    return "{\"success\": false, \"error\": \"不支持的操作类型: " + action + "\"}";
            }
        } catch (Exception e) {
            log.error("文件操作失败: {}", action, e);
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private Path getWorkspacePath() {
        String workspace = yuniConfig.getWorkspace();
        if (workspace == null || workspace.isEmpty()) {
            workspace = System.getProperty("user.home") + "/YuniWorkspace";
        }
        if (workspace.startsWith("~")) {
            workspace = System.getProperty("user.home") + workspace.substring(1);
        }
        return Paths.get(workspace).toAbsolutePath().normalize();
    }

    private Path resolvePath(Path workspacePath, String relativePath) {
        Path resolved = workspacePath.resolve(relativePath).normalize();
        if (!resolved.startsWith(workspacePath)) {
            return null;
        }
        return resolved;
    }

    private String readFile(Path path) {
        if (!Files.exists(path)) {
            return "{\"success\": false, \"error\": \"文件不存在\"}";
        }
        if (!Files.isRegularFile(path)) {
            return "{\"success\": false, \"error\": \"不是常规文件\"}";
        }

        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            if (content.length() > 50000) {
                content = content.substring(0, 50000) + "\n... [内容已截断]";
            }
            return "{\"success\": true, \"content\": \"" + escapeJson(content) + "\", \"size\": " + Files.size(path) + "}";
        } catch (IOException e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String writeFile(Path path, String content) {
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, content, StandardCharsets.UTF_8);
            log.info("写入文件: {}", path);
            return "{\"success\": true, \"path\": \"" + escapeJson(path.getFileName().toString()) + "\", \"size\": " + content.length() + "}";
        } catch (IOException e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String listDirectory(Path path, boolean recursive) {
        if (!Files.exists(path)) {
            return "{\"success\": false, \"error\": \"目录不存在\"}";
        }
        if (!Files.isDirectory(path)) {
            return "{\"success\": false, \"error\": \"不是目录\"}";
        }

        try {
            List<String> items = new ArrayList<>();
            
            if (recursive) {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                        items.add(file.toString().substring(path.toString().length() + 1));
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                        if (!dir.equals(path)) {
                            items.add(dir.toString().substring(path.toString().length() + 1) + "/");
                        }
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
                    for (Path item : stream) {
                        String name = item.getFileName().toString();
                        if (Files.isDirectory(item)) {
                            name += "/";
                        }
                        items.add(name);
                    }
                }
            }

            StringBuilder result = new StringBuilder("{\"success\": true, \"items\": [");
            for (int i = 0; i < items.size() && i < 100; i++) {
                if (i > 0) result.append(", ");
                result.append("\"").append(escapeJson(items.get(i))).append("\"");
            }
            if (items.size() > 100) {
                result.append(", \"... 还有 ").append(items.size() - 100).append(" 项\"");
            }
            result.append("], \"total\": ").append(items.size()).append("}");

            return result.toString();
        } catch (IOException e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String createDirectory(Path path) {
        try {
            Files.createDirectories(path);
            log.info("创建目录: {}", path);
            return "{\"success\": true, \"path\": \"" + escapeJson(path.getFileName().toString()) + "\"}";
        } catch (IOException e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String deletePath(Path path, boolean recursive) {
        if (!Files.exists(path)) {
            return "{\"success\": false, \"error\": \"文件或目录不存在\"}";
        }

        try {
            if (Files.isDirectory(path) && recursive) {
                Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        Files.delete(file);
                        return FileVisitResult.CONTINUE;
                    }

                    @Override
                    public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                        Files.delete(dir);
                        return FileVisitResult.CONTINUE;
                    }
                });
            } else {
                Files.delete(path);
            }
            log.info("删除: {}", path);
            return "{\"success\": true, \"path\": \"" + escapeJson(path.getFileName().toString()) + "\"}";
        } catch (IOException e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String checkExists(Path path) {
        boolean exists = Files.exists(path);
        boolean isDirectory = Files.isDirectory(path);
        boolean isFile = Files.isRegularFile(path);
        
        return "{\"success\": true, \"exists\": " + exists + 
               ", \"isDirectory\": " + isDirectory + 
               ", \"isFile\": " + isFile + "}";
    }

    private String escapeJson(String text) {
        if (text == null) return "";
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
