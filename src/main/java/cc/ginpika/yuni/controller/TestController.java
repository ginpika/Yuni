package cc.ginpika.yuni.controller;

import cc.ginpika.yuni.core.ChatResponse;
import cc.ginpika.yuni.service.AgentContext;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@Slf4j
@RestController
public class TestController {
    @Resource
    AgentContext agentContext;

    @RequestMapping("/chat")
    public ChatResponse chat(@RequestParam String input) throws IOException {
        return agentContext.call(input);
    }
}
