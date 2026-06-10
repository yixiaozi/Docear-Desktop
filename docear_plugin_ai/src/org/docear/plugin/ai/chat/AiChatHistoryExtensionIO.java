package org.docear.plugin.ai.chat;

import java.io.IOException;
import java.util.Vector;

import org.freeplane.core.extension.IExtension;
import org.freeplane.core.io.IElementDOMHandler;
import org.freeplane.core.io.IExtensionElementWriter;
import org.freeplane.core.io.ITreeWriter;
import org.freeplane.features.map.MapController;
import org.freeplane.features.map.MapModel;
import org.freeplane.features.mode.ModeController;
import org.freeplane.n3.nanoxml.XMLElement;

/**
 * 聊天历史 Extension 的 XML 读写，随 .mm 文件持久化。
 */
public final class AiChatHistoryExtensionIO {

    private static final String XML_ELEMENT = "ai_chat_history";

    private AiChatHistoryExtensionIO() {
    }

    public static void install(ModeController modeController) {
        final MapController mapController = modeController.getMapController();
        mapController.getWriteManager().addExtensionElementWriter(
                AiChatHistoryExtension.class, new AiChatHistoryExtensionWriter());
        mapController.getReadManager().addElementHandler(XML_ELEMENT, new AiChatHistoryExtensionReader());
    }

    private static final class AiChatHistoryExtensionWriter implements IExtensionElementWriter {
        public void writeContent(ITreeWriter writer, Object element, IExtension extension) throws IOException {
            AiChatHistoryExtension chatExt = (AiChatHistoryExtension) extension;
            AiChatSession session = chatExt.getSession();
            if (session == null || session.getMessages().isEmpty()) {
                return;
            }

            XMLElement xml = new XMLElement(XML_ELEMENT);
            xml.setAttribute("mapId", session.getMapId());
            xml.setAttribute("lastUpdated", String.valueOf(session.getLastUpdated()));

            for (int i = 0; i < session.getMessages().size(); i++) {
                AiChatMessage msg = session.getMessages().get(i);
                XMLElement msgXml = new XMLElement("message");
                msgXml.setAttribute("role", msg.getRole().name());
                msgXml.setAttribute("timestamp", String.valueOf(msg.getTimestamp()));
                msgXml.setContent(escapeXml(msg.getContent()));
                xml.addChild(msgXml);
            }
            writer.addElement(element, xml);
        }
    }

    private static final class AiChatHistoryExtensionReader implements IElementDOMHandler {
        public Object createElement(Object parent, String tag, XMLElement attributes) {
            return parent;
        }

        public void endElement(Object parent, String tag, Object element, XMLElement dom) {
            MapModel map = resolveMap(parent);
            if (map == null || dom == null) {
                return;
            }

            String mapId = dom.getAttribute("mapId", AiChatSessionManager.resolveMapKey(map));
            AiChatSession session = new AiChatSession(mapId);
            Vector<XMLElement> children = dom.getChildren();
            for (int childIndex = 0; childIndex < children.size(); childIndex++) {
                XMLElement msgXml = children.elementAt(childIndex);
                if (!"message".equals(msgXml.getName())) {
                    continue;
                }
                String roleName = msgXml.getAttribute("role", "USER");
                long timestamp = parseLong(msgXml.getAttribute("timestamp", "0"), 0L);
                String content = unescapeXml(msgXml.getContent());
                AiChatMessage.Role role = "ASSISTANT".equalsIgnoreCase(roleName)
                        ? AiChatMessage.Role.ASSISTANT : AiChatMessage.Role.USER;
                session.addMessage(new AiChatMessage(role, content, timestamp));
            }

            AiChatHistoryExtension extension = AiChatHistoryExtension.getOrCreate(map);
            extension.setSession(session);
        }

        private MapModel resolveMap(Object parent) {
            if (parent instanceof MapModel) {
                return (MapModel) parent;
            }
            return null;
        }

        private long parseLong(String value, long defaultValue) {
            try {
                return Long.parseLong(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
    }

    private static String escapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private static String unescapeXml(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }
}
