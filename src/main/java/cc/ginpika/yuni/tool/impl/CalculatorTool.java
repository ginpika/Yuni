package cc.ginpika.yuni.tool.impl;

import cc.ginpika.yuni.tool.AbstractTool;
import cc.ginpika.yuni.tool.ToolRegistry;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTool extends AbstractTool {

    @Resource
    private ToolRegistry toolRegistry;

    public CalculatorTool() {
        super("calculator", "执行数学计算，支持加减乘除运算");
    }

    @PostConstruct
    public void register() {
        toolRegistry.register(this);
    }

    @Override
    protected ObjectNode buildProperties() {
        ObjectNode properties = objectMapper.createObjectNode();
        properties.set("expression", createProperty("string", "数学表达式，如：2+3*4, (10-5)/2"));
        return properties;
    }

    @Override
    protected String[] getRequiredParameters() {
        return new String[]{"expression"};
    }

    @Override
    public String execute(JsonNode arguments) {
        String expression = arguments.has("expression") ? arguments.get("expression").asText() : "";
        
        try {
            expression = expression.replaceAll("\\s+", "");
            
            if (!expression.matches("[0-9+\\-*/().]+")) {
                return "{\"error\": \"表达式包含非法字符\"}";
            }
            
            double result = evaluateExpression(expression);
            
            if (result == (long) result) {
                return String.format("{\"expression\": \"%s\", \"result\": %d}", expression, (long) result);
            } else {
                return String.format("{\"expression\": \"%s\", \"result\": %.2f}", expression, result);
            }
        } catch (Exception e) {
            return String.format("{\"error\": \"计算失败: %s\"}", e.getMessage());
        }
    }

    private double evaluateExpression(String expression) {
        return new Object() {
            int pos = -1, ch;

            void nextChar() {
                ch = (++pos < expression.length()) ? expression.charAt(pos) : -1;
            }

            boolean eat(int charToEat) {
                while (ch == ' ') nextChar();
                if (ch == charToEat) {
                    nextChar();
                    return true;
                }
                return false;
            }

            double parse() {
                nextChar();
                double x = parseExpression();
                if (pos < expression.length()) throw new RuntimeException("Unexpected: " + (char) ch);
                return x;
            }

            double parseExpression() {
                double x = parseTerm();
                for (; ; ) {
                    if (eat('+')) x += parseTerm();
                    else if (eat('-')) x -= parseTerm();
                    else return x;
                }
            }

            double parseTerm() {
                double x = parseFactor();
                for (; ; ) {
                    if (eat('*')) x *= parseFactor();
                    else if (eat('/')) x /= parseFactor();
                    else return x;
                }
            }

            double parseFactor() {
                if (eat('+')) return parseFactor();
                if (eat('-')) return -parseFactor();

                double x;
                int startPos = this.pos;
                if (eat('(')) {
                    x = parseExpression();
                    eat(')');
                } else if ((ch >= '0' && ch <= '9') || ch == '.') {
                    while ((ch >= '0' && ch <= '9') || ch == '.') nextChar();
                    x = Double.parseDouble(expression.substring(startPos, this.pos));
                } else {
                    throw new RuntimeException("Unexpected: " + (char) ch);
                }

                return x;
            }
        }.parse();
    }
}
