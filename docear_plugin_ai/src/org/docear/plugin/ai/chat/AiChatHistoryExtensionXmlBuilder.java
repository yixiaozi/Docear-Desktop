package org.docear.plugin.ai.chat;

import java.io.IOException;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.IExtensionAttributeWriter;
import org.freeplane.core.io.IExtensionElementWriter;
import org.freeplane.core.io.ITreeWriter;
import org.freeplane.core.io.xml.TreeXmlReader;
import org.freeplane.n3.nanoxml.XMLElement;

/**
 * 聊天历史 Extension 的 XML 读写器。
 * 负责把聊天记录序列化到 .mm 文件，以及从 .mm 文件恢复。
 */
public class AiChatHistoryExtensionXmlBuilder implements IExtensionElementWriter, IExtensionAttributeWriter {

    private static final String XML_ELEMENT = "ai_chat_history";

    // TODO: 实现从 XML 读取聊天历史
    // public static void registerAttributeHandlers(TreeXmlReader reader) { ... }

    @Override
    public void writeAttributes(ITreeWriter writer, Object userObject, IExtension extension) {
        // 暂时不写属性，由 writeContent 统一处理
    }

    @Override
    public void writeContent(ITreeWriter writer, Object element, IExtension extension) throws IOException {
        final AiChatHistoryExtension chatExt = (AiChatHistoryExtension) extension;
        final AiChatSession session = chatExt.getSession();

        if (session == null || session.getMessages().isEmpty()) {
            return;
        }

        XMLElement xml = new XMLElement(XML_ELEMENT);
        xml.setAttribute("mapId", session.getMapId());
        xml.setAttribute("lastUpdated", String.valueOf(session.getLastUpdated()));

        for (AiChatMessage msg : session.getMessages()) {
            XMLElement msgXml = new XMLElement("message");
            msgXml.setAttribute("role", msg.getRole().name());
            msgXml.setAttribute("timestamp", String.valueOf(msg.getTimestamp()));
            msgXml.setContent(escapeXml(msg.getContent()));
            xml.addChild(msgXml);
        }

        writer.addElement(element, xml);
    }

    private String escapeXml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
}
