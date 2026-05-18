package backend.xxx.chat.message.strategy;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import backend.xxx.chat.common.exception.ValidationException;
import backend.xxx.chat.message.model.MessageType;
import org.springframework.stereotype.Component;

@Component
public class MessageTypeStrategyRegistry {

    private final Map<MessageType, MessageTypeStrategy> strategies;

    public MessageTypeStrategyRegistry(List<MessageTypeStrategy> strategies) {
        this.strategies = new EnumMap<>(MessageType.class);
        strategies.forEach(strategy -> this.strategies.put(strategy.type(), strategy));
    }

    public MessageTypeStrategy get(MessageType messageType) {
        MessageTypeStrategy strategy = strategies.get(messageType);
        if (strategy == null) {
            throw new ValidationException("Unsupported message type: " + messageType);
        }
        return strategy;
    }
}
