package backend.xxx.chat.message.service;

import java.util.List;

import backend.xxx.chat.message.dto.AttachmentResponse;
import backend.xxx.chat.message.model.MessageAttachment;
import org.springframework.stereotype.Component;

@Component
public class MessageAttachmentMapper {

    public AttachmentResponse toResponse(MessageAttachment attachment) {
        return new AttachmentResponse(
                attachment.getObjectKey(),
                attachment.getUrl(),
                attachment.getFileName(),
                attachment.getContentType(),
                attachment.getSizeBytes(),
                attachment.getWidth(),
                attachment.getHeight(),
                attachment.getDurationSeconds(),
                attachment.getThumbnailUrl()
        );
    }

    public List<AttachmentResponse> toResponses(List<MessageAttachment> attachments) {
        return attachments.stream()
                .map(this::toResponse)
                .toList();
    }
}
