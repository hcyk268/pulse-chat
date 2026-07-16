package backend.xxx.chat.message.service;

import java.time.Instant;
import java.util.List;
import java.util.stream.IntStream;

import backend.xxx.chat.common.exception.ConflictException;
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
            throw new ValidationException("Media message must contain at least one attachment");
        }
        if (requests.size() > MAX_ATTACHMENTS_PER_MESSAGE) {
            throw new ValidationException("Media message cannot contain more than " + MAX_ATTACHMENTS_PER_MESSAGE + " attachments");
        }

        return IntStream.range(0, requests.size())
                .mapToObj(index -> resolveOne(sender, requests.get(index), index))
                .toList();
    }

    private MessageAttachment resolveOne(User sender, AttachmentRequest request, int sortOrder) {
        if (request == null) {
            throw new ValidationException("attachment must not be null");
        }
        if (request.assetId() == null) {
            throw new ValidationException("attachment assetId must not be null");
        }

        return resolveUploadedAsset(sender, request.assetId(), sortOrder);
    }

    private MessageAttachment resolveUploadedAsset(User sender, Long assetId, int sortOrder) {
        UploadedAsset asset = uploadedAssetRepository.findByIdForUpdate(assetId)
                .orElseThrow(() -> new NotFoundException("Uploaded asset not found"));

        if (!asset.belongsTo(sender.getId())) {
            throw new ForbiddenException("You are not allowed to use this uploaded asset");
        }
        if (asset.getPurpose() != UploadPurpose.MESSAGE_ATTACHMENT) {
            throw new ValidationException("Uploaded asset purpose is not MESSAGE_ATTACHMENT");
        }
        if (!asset.isReady()) {
            throw new ConflictException("Uploaded asset is not ready to attach");
        }

        asset.markAttached(Instant.now());
        return MessageAttachment.createFromAsset(asset, sortOrder);
    }
}
