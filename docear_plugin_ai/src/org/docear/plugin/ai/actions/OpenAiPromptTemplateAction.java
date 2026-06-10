package org.docear.plugin.ai.actions;

import java.awt.event.ActionEvent;

import org.docear.plugin.ai.DocearAiController;
import org.freeplane.core.ui.AFreeplaneAction;

/**
 * 打开 AI 提示词模板文件供用户编辑。
 */
public class OpenAiPromptTemplateAction extends AFreeplaneAction {

    private static final long serialVersionUID = 1L;
    public static final String KEY = "OpenAiPromptTemplateAction";

    public OpenAiPromptTemplateAction() {
        super(KEY, "AI: \u7f16\u8f91\u63d0\u793a\u8bcd", null);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        DocearAiController controller = DocearAiController.getController();
        if (controller != null) {
            controller.getPromptBuilder().getTemplateStore().openTemplateFile();
        }
    }
}
