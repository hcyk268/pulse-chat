package backend.xxx.chat.storage.model;

public enum UploadPurpose {
    AVATAR("avatars"),
    MESSAGE_ATTACHMENT("message-attachments"),
    COMMUNITY_AVATAR("community-avatars"),
    COMMUNITY_COVER("community-covers"),
    COMMUNITY_ATTACHMENT("community-attachments");

    private final String keyPrefix;

    UploadPurpose(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String keyPrefix() {
        return keyPrefix;
    }
}
