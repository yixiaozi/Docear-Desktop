package org.docear.plugin.ai;

import org.freeplane.core.resources.ResourceController;

/**
 * Docear AI 插件配置管理类。
 * 统一管理 AI 后端的选择、参数等设置。
 * 未来切换到 OpenAI 等模型时，只需在此类中扩展配置项即可。
 */
public class DocearAiConfig {

    private static final String PROPERTY_AI_ENABLED = "ai.enabled";
    private static final String PROPERTY_AI_BACKEND = "ai.backend"; // copilot_cli, openai, ollama
    private static final String PROPERTY_AI_MODEL = "ai.model";
    private static final String PROPERTY_AI_TEMPERATURE = "ai.temperature";

    public DocearAiConfig() {
    }

    public boolean isAiEnabled() {
        return ResourceController.getResourceController().getBooleanProperty(PROPERTY_AI_ENABLED);
    }

    public String getBackendType() {
        return ResourceController.getResourceController().getProperty(PROPERTY_AI_BACKEND, "copilot_cli");
    }

    public String getModel() {
        return ResourceController.getResourceController().getProperty(PROPERTY_AI_MODEL, "default");
    }

    public double getTemperature() {
        try {
            return Double.parseDouble(ResourceController.getResourceController().getProperty(PROPERTY_AI_TEMPERATURE, "0.7"));
        } catch (NumberFormatException e) {
            return 0.7;
        }
    }

    // TODO: 未来在这里添加 OpenAI API Key、Base URL 等配置读取方法
}
