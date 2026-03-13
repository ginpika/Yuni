package cc.ginpika.yuni.tool.impl;

import cc.ginpika.yuni.config.YuniConfig;
import cc.ginpika.yuni.service.PlaywrightPermissionService;
import cc.ginpika.yuni.tool.AbstractTool;
import cc.ginpika.yuni.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.LoadState;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PlaywrightTool extends AbstractTool {

    @Resource
    private ToolRegistry toolRegistry;

    @Resource
    private PlaywrightPermissionService permissionService;

    @Resource
    private YuniConfig yuniConfig;

    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    public PlaywrightTool() {
        super("browser_control", "使用 Playwright 控制浏览器访问网页、截图、点击元素、填写表单等操作。支持导航、截图、点击、输入文本、获取内容等操作。");
    }

    @PostConstruct
    public void register() {
        toolRegistry.register(this);
        initPlaywright();
    }

    private void initPlaywright() {
        try {
            playwright = Playwright.create();
            browser = playwright.chromium().launch(new BrowserType.LaunchOptions()
                    .setHeadless(true));
            context = browser.newContext(new Browser.NewContextOptions()
                    .setViewportSize(1280, 720));
            page = context.newPage();
            log.info("Playwright 浏览器初始化成功");
        } catch (Exception e) {
            log.error("Playwright 初始化失败", e);
        }
    }

    @PreDestroy
    public void cleanup() {
        try {
            if (page != null) page.close();
            if (context != null) context.close();
            if (browser != null) browser.close();
            if (playwright != null) playwright.close();
            log.info("Playwright 资源已释放");
        } catch (Exception e) {
            log.error("Playwright 清理失败", e);
        }
    }

    @Override
    protected ObjectNode buildProperties() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("action", createProperty("string", "操作类型：navigate（导航）、screenshot（截图）、click（点击）、fill（填写）、content（获取内容）、title（获取标题）", new String[]{"navigate", "screenshot", "click", "fill", "content", "title"}));
        properties.set("url", createProperty("string", "目标 URL（navigate 操作必填）"));
        properties.set("selector", createProperty("string", "CSS 选择器（click、fill 操作必填）"));
        properties.set("value", createProperty("string", "输入值（fill 操作必填）"));
        properties.set("timeout", createProperty("integer", "超时时间（毫秒），默认 30000"));
        return properties;
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{"action"};
    }

    @Override
    public String execute(JsonNode arguments) {
        String action = arguments.has("action") ? arguments.get("action").asText() : null;
        
        if (action == null || action.isEmpty()) {
            return "{\"success\": false, \"error\": \"操作类型不能为空\"}";
        }

        if (browser == null || page == null) {
            initPlaywright();
            if (browser == null || page == null) {
                return "{\"success\": false, \"error\": \"浏览器未初始化\"}";
            }
        }

        int timeout = arguments.has("timeout") ? arguments.get("timeout").asInt() : 30000;

        try {
            switch (action) {
                case "navigate":
                    return navigate(arguments, timeout);
                case "screenshot":
                    return screenshot(timeout);
                case "click":
                    return click(arguments, timeout);
                case "fill":
                    return fill(arguments, timeout);
                case "content":
                    return getContent();
                case "title":
                    return getTitle();
                default:
                    return "{\"success\": false, \"error\": \"不支持的操作类型: " + action + "\"}";
            }
        } catch (Exception e) {
            log.error("浏览器操作失败: {}", action, e);
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String navigate(JsonNode arguments, int timeout) {
        String url = arguments.has("url") ? arguments.get("url").asText() : null;
        
        if (url == null || url.isEmpty()) {
            return "{\"success\": false, \"error\": \"URL 不能为空\"}";
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://" + url;
        }

        if (!permissionService.isDomainAllowed(url)) {
            String requestId = permissionService.requestDomainPermission(url, "AI 请求访问此网站");
            
            PlaywrightPermissionService.PermissionRequest request = permissionService.getPendingRequest(requestId);
            if (request != null) {
                synchronized (request) {
                    try {
                        request.wait(60000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                
                if (request.getApproved() == null || !request.getApproved()) {
                    permissionService.removePendingRequest(requestId);
                    return "{\"success\": false, \"error\": \"用户拒绝访问此网站\"}";
                }
                
                permissionService.removePendingRequest(requestId);
            } else {
                return "{\"success\": false, \"error\": \"权限请求超时\"}";
            }
        }

        try {
            page.navigate(url, new Page.NavigateOptions().setTimeout(timeout));
            page.waitForLoadState(LoadState.DOMCONTENTLOADED, new Page.WaitForLoadStateOptions().setTimeout(timeout));
            
            String title = page.title();
            String currentUrl = page.url();
            
            return "{\"success\": true, \"title\": \"" + escapeJson(title) + "\", \"url\": \"" + escapeJson(currentUrl) + "\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String screenshot(int timeout) {
        try {
            String workspace = yuniConfig.getWorkspace();
            if (workspace == null || workspace.isEmpty()) {
                workspace = System.getProperty("user.home") + "/YuniWorkspace";
            }
            if (workspace.startsWith("~")) {
                workspace = System.getProperty("user.home") + workspace.substring(1);
            }
            
            Path screenshotDir = Paths.get(workspace, "screenshots");
            Files.createDirectories(screenshotDir);
            
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String filename = "screenshot_" + timestamp + ".png";
            Path screenshotPath = screenshotDir.resolve(filename);
            
            page.screenshot(new Page.ScreenshotOptions()
                    .setPath(screenshotPath)
                    .setFullPage(false)
                    .setTimeout(timeout));
            
            log.info("截图已保存: {}", screenshotPath);
            return "{\"success\": true, \"file\": \"screenshots/" + filename + "\", \"message\": \"截图已保存到 screenshots/" + filename + "\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String click(JsonNode arguments, int timeout) {
        String selector = arguments.has("selector") ? arguments.get("selector").asText() : null;
        
        if (selector == null || selector.isEmpty()) {
            return "{\"success\": false, \"error\": \"选择器不能为空\"}";
        }

        try {
            page.click(selector, new Page.ClickOptions().setTimeout(timeout));
            return "{\"success\": true, \"action\": \"click\", \"selector\": \"" + escapeJson(selector) + "\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String fill(JsonNode arguments, int timeout) {
        String selector = arguments.has("selector") ? arguments.get("selector").asText() : null;
        String value = arguments.has("value") ? arguments.get("value").asText() : null;
        
        if (selector == null || selector.isEmpty()) {
            return "{\"success\": false, \"error\": \"选择器不能为空\"}";
        }
        if (value == null) {
            return "{\"success\": false, \"error\": \"输入值不能为空\"}";
        }

        try {
            page.fill(selector, value, new Page.FillOptions().setTimeout(timeout));
            return "{\"success\": true, \"action\": \"fill\", \"selector\": \"" + escapeJson(selector) + "\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String getContent() {
        try {
            String content = page.content();
            if (content.length() > 5000) {
                content = content.substring(0, 5000) + "...";
            }
            return "{\"success\": true, \"content\": \"" + escapeJson(content) + "\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
    }

    private String getTitle() {
        try {
            String title = page.title();
            return "{\"success\": true, \"title\": \"" + escapeJson(title) + "\"}";
        } catch (Exception e) {
            return "{\"success\": false, \"error\": \"" + escapeJson(e.getMessage()) + "\"}";
        }
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
