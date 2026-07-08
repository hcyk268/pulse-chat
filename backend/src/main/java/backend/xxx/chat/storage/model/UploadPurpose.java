package backend.xxx.chat.storage.model;

public enum UploadPurpose {
    AVATAR("avatars"),
    MESSAGE_ATTACHMENT("message-attachments");

    private final String keyPrefix;

    UploadPurpose(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public String keyPrefix() {
        return keyPrefix;
    }
}
