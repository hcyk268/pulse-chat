package backend.xxx.chat.realtime.model;

import com.fasterxml.jackson.annotation.JsonValue;

public enum RealtimeEventType {
    MESSAGE_CREATED("message.created"),
    MESSAGE_UPDATED("message.updated"),
    MESSAGE_DELETED("message.deleted"),
    MESSAGE_PINNED("message.pinned"),
    MESSAGE_UNPINNED("message.unpinned"),
    MESSAGE_READ("message.read"),
    MESSAGE_STATUS_UPDATED("message.status.updated"),
    CONVERSATION_UPDATED("conversation.updated"),
    TYPING_UPDATED("typing.updated"),
    PRESENCE_UPDATED("presence.updated"),
    GROUP_CREATED("group.created"),
    GROUP_MEMBER_ADDED("group.member.added"),
    GROUP_MEMBER_REMOVED("group.member.removed"),
    GROUP_UPDATED("group.updated");

    private final String value;

    RealtimeEventType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }
}
