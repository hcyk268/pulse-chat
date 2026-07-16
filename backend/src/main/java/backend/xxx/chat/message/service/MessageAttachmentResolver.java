package backend.xxx.chat.message.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.web.Translator;
import backend.xxx.chat.common.exception.ForbiddenException;
import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.message.dto.AttachmentRequest;
import backend.xxx.chat.message.model.MessageAttachment;
import backend.xxx.chat.storage.model.UploadPurpose;
import backend.xxx.chat.storage.model.UploadedAsset;
import backend.xxx.chat.storage.repository.UploadedAssetRepository;
import backend.xxx.chat.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageAttachmentResolver {

    private static final int MAX_ATTACHMENTS_PER_MESSAGE = 10;

    private final UploadedAssetRepository uploadedAssetRepository;

    public List<MessageAttachment> resolve(User sender, List<AttachmentRequest> requests) {
        if (requests == null || requests.isEmpty()) {
            throw new ValidationException("message.media.attachments.required");
        }
        if (requests.size() > MAX_ATTACHMENTS_PER_MESSAGE) {
            throw new ValidationException(Translator.toLocale("message.media.attachments.limit.exceeded", MAX_ATTACHMENTS_PER_MESSAGE));
        }

        return IntStream.range(0, requests.size())
                .mapToObj(index -> resolveOne(sender, requests.get(index), index))
                .toList();
    }

    private MessageAttachment resolveOne(User sender, AttachmentRequest request, int sortOrder) {
        if (request == null) {
            throw new ValidationException("message.attachment.required");
        }
        if (request.assetId() == null) {
            throw new ValidationException("message.attachment.asset.id.required");
        }

        return resolveUploadedAsset(sender, request.assetId(), sortOrder);
    }

    private MessageAttachment resolveUploadedAsset(User sender, Long assetId, int sortOrder) {
        UploadedAsset asset = uploadedAssetRepository.findByIdForUpdate(assetId)
                .orElseThrow(() -> new NotFoundException("upload.asset.not.found"));

        if (!asset.belongsTo(sender.getId())) {
            throw new ForbiddenException("upload.asset.forbidden");
        }
        if (asset.getPurpose() != UploadPurpose.MESSAGE_ATTACHMENT) {
            throw new ValidationException("upload.asset.purpose.invalid");
        }
        if (!asset.isReady()) {
            throw new ConflictException("upload.asset.not.ready");
        }

        asset.markAttached(Instant.now());
        return MessageAttachment.createFromAsset(asset, sortOrder);
    }
}
