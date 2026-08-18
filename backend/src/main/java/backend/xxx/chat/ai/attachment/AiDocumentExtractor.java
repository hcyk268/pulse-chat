package backend.xxx.chat.ai.attachment;

import backend.xxx.chat.config.properties.AIProperties;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.xml.sax.ContentHandler;

@Component
@RequiredArgsConstructor
public class AiDocumentExtractor {

    private final AIProperties properties;

    public String extract(LoadedAiAttachment attachment) {
        if (attachment == null || attachment.image()) {
            return "";
        }
        String contentType = attachment.contentType() == null ? "" : attachment.contentType();
        if (contentType.startsWith("text/") || contentType.equals("application/json")) {
            return truncate(new String(attachment.data(), StandardCharsets.UTF_8));
        }
        try (ByteArrayInputStream input = new ByteArrayInputStream(attachment.data())) {
            ContentHandler handler = new BodyContentHandler(properties.getAttachment().getMaxExtractedChars());
            Metadata metadata = new Metadata();
            metadata.set(Metadata.CONTENT_TYPE, attachment.contentType());
            metadata.set(TikaCoreProperties.RESOURCE_NAME_KEY, attachment.fileName());
            new AutoDetectParser().parse(input, handler, metadata, new ParseContext());
            return truncate(handler.toString());
        } catch (Exception ex) {
            return "";
        }
    }

    private String truncate(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        int max = properties.getAttachment().getMaxExtractedChars();
        String normalized = value.trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max).trim();
    }
}