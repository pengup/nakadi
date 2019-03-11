package org.zalando.nakadi.repository.kafka;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class KafkaSettings {

    // kafka client requires this property to be int
    // https://github.com/apache/kafka/blob/d9206500bf2f99ce93f6ad64c7a89483100b3b5f/clients/src/main/java/org/apache
    // /kafka/clients/producer/ProducerConfig.java#L261
    private final int requestTimeoutMs;
    // kafka client requires this property to be int
    // https://github.com/apache/kafka/blob/d9206500bf2f99ce93f6ad64c7a89483100b3b5f/clients/src/main/java/org/apache
    // /kafka/clients/producer/ProducerConfig.java#L232
    private final int batchSize;
    private final int lingerMs;
    private final boolean enableAutoCommit;
    private final int maxRequestSize;
    private final int deliveryTimeoutMs;
    private final int maxBlockMs;

    @Autowired
    public KafkaSettings(@Value("${nakadi.kafka.request.timeout.ms}") final int requestTimeoutMs,
                         @Value("${nakadi.kafka.batch.size}") final int batchSize,
                         @Value("${nakadi.kafka.linger.ms}") final int lingerMs,
                         @Value("${nakadi.kafka.enable.auto.commit}") final boolean enableAutoCommit,
                         @Value("${nakadi.kafka.max.request.size}") final int maxRequestSize,
                         @Value("${nakadi.kafka.delivery.timeout.ms}") final int deliveryTimeoutMs,
                         @Value("${nakadi.kafka.max.block.ms}") final int maxBlockMs) {
        this.requestTimeoutMs = requestTimeoutMs;
        this.batchSize = batchSize;
        this.lingerMs = lingerMs;
        this.enableAutoCommit = enableAutoCommit;
        this.maxRequestSize = maxRequestSize;
        this.deliveryTimeoutMs = deliveryTimeoutMs;
        this.maxBlockMs = maxBlockMs;
    }

    public int getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getLingerMs() {
        return lingerMs;
    }

    public boolean getEnableAutoCommit() {
        return enableAutoCommit;
    }

    public int getMaxRequestSize() {
        return maxRequestSize;
    }

    public int getDeliveryTimeoutMs() {
        return deliveryTimeoutMs;
    }

    public int getMaxBlockMs() {
        return maxBlockMs;
    }
}
