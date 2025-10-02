package messaging;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
@RequiredArgsConstructor
public class BaseKafkaProducerService {
    private final KafkaTemplate<String, Object> kafkaTemplate;
    public final int DEFAULT_TIMEOUT_MS = 2 * 60 * 1000;

    protected void sendAsync(String key, Object data, String topic) {
        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send(topic, key, data);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Sent message successfully to topic: {}, partition: {}, offset: {}",
                        topic,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to send message to topic: {}. Error: {}",
                        topic, ex.getMessage(), ex);
            }
        });
    }

    protected boolean sendSync(String key, Object data, String topic) {
        try {

            SendResult<String, Object> result = kafkaTemplate.send(topic, key, data)
                    .get();
            log.debug("Message sent successfully to partition: {}",
                    result.getRecordMetadata().partition());
            return true;
        } catch (Exception e) {
            log.error("Failed to send message synchronously", e);
            return false;
        }
    }

    protected boolean sendWithTimeout(String key, Object data, String topic, long timeoutMs) {
        try {
            SendResult<String, Object> result = kafkaTemplate.send(topic, key, data)
                    .get(timeoutMs, java.util.concurrent.TimeUnit.MILLISECONDS);
            log.info("Message sent successfully. Key: {}, Partition: {}, Offset: {}",
                    key,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
            return true;
        } catch (Exception e) {
            log.error("Failed to send message with timeout", e);
            return false;
        }
    }

}
