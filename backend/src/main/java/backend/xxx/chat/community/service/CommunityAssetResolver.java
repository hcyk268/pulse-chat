package backend.xxx.chat.community.service;

import java.time.Instant;

import backend.xxx.chat.common.exception.ConflictException;
import backend.xxx.chat.common.exception.ForbiddenException;
import backend.xxx.chat.common.exception.NotFoundException;
import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.storage.model.UploadPurpose;
import backend.xxx.chat.storage.model.UploadedAsset;
import backend.xxx.chat.storage.repository.UploadedAssetRepository;
import backend.xxx.chat.user.model.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommunityAssetResolver {

    private final UploadedAssetRepository uploadedAssetRepository;

    public UploadedAsset resolveOwnedReadyAsset(
            User owner,
            Long assetId,
            UploadPurpose expectedPurpose,
            UploadedAsset currentAsset
    ) {
        if (assetId == null) {
            return null;
        }

        UploadedAsset asset = uploadedAssetRepository.findByIdForUpdate(assetId)
                .orElseThrow(() -> new NotFoundException("upload.asset.not.found"));
        if (!asset.belongsTo(owner.getId())) {
            throw new ForbiddenException("upload.asset.forbidden");
        }
        if (asset.getPurpose() != expectedPurpose) {
            throw new ValidationException("upload.asset.purpose.invalid");
        }
        if (currentAsset != null && currentAsset.getId().equals(asset.getId())) {
            return asset;
        }
        if (!asset.isReady()) {
            throw new ConflictException("upload.asset.not.ready");
        }

        asset.markAttached(Instant.now());
        return asset;
    }
}
