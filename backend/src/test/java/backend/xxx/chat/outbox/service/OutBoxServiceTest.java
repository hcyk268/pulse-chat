package backend.xxx.chat.outbox.service;

import backend.xxx.chat.outbox.model.OutboxEvent;
import backend.xxx.chat.outbox.model.OutboxStatus;
import backend.xxx.chat.outbox.repository.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class OutBoxServiceTest {

    @Autowired
    private OutBoxService outBoxService;

    @Autowired
    private OutboxEventRepository outboxEventRepository;

    @Test
    void pushEventKeepsEventNamesButTrimsAndValidatesPayloadBeforePersisting() {
        outBoxService.pushEvent(
                " message ",
                123L,
                " Message.Created ",
                " { \"messageId\" : 123, \"conversationId\" : 10 } "
        );

        OutboxEvent savedEvent = outboxEventRepository.findAll().get(0);

        assertThat(savedEvent.getAggregateType()).isEqualTo("message");
        assertThat(savedEvent.getAggregateId()).isEqualTo(123L);
        assertThat(savedEvent.getEventType()).isEqualTo("Message.Created");
        assertThat(savedEvent.getPayload()).isEqualTo("{ \"messageId\" : 123, \"conversationId\" : 10 }");
        assertThat(savedEvent.getStatus()).isEqualTo(OutboxStatus.PENDING);
    }

    @Test
    void pushEventRejectsInvalidJsonPayload() {
        assertThatThrownBy(() -> outBoxService.pushEvent(
                "MESSAGE",
                123L,
                "message.created",
                "not-json"
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("payload must be valid JSON");
    }
}
