package backend.xxx.chat.outbox.model;

public enum OutboxStatus {
    PENDING,
    PROCESSING,
    DONE,
    FAILED
}
