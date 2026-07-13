package backend.xxx.chat.message.dto;

import java.util.List;

public record MessageReadReceiptsResponse(
        Long messageId,
        List<MessageReadReceiptResponse> items
) {
}