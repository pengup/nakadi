package org.zalando.nakadi.service.subscription;

import org.zalando.nakadi.exceptions.UnprocessableEntityException;
import org.zalando.nakadi.security.Client;
import org.zalando.nakadi.service.EventStreamConfig;

import javax.annotation.Nullable;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;

public class StreamParameters {
    /**
     * Maximum amount of events in batch. If it's reached, batch is sent immediately and stream is flushed.
     */
    public final int batchLimitEvents;
    /**
     * Maximum number of events that could be sent in session
     */
    private final Optional<Long> streamLimitEvents;
    /**
     * Timeout for collecting {@code batchLimitEvents} events. If not collected - send either not full batch
     * or keep alive message.
     */
    public final long batchTimeoutMillis;
    /**
     * Stream time to live
     */
    public final long streamTimeoutMillis;
    /**
     * If count of keepAliveIterations in a row for each batch is reached - stream is closed.
     * Works only if set.
     */
    private final Optional<Integer> batchKeepAliveIterations;

    // Applies to stream, number of messages to send to clients
    public final int maxUncommittedMessages;

    // Applies to stream. Timeout without commits.
    public final long commitTimeoutMillis;

    private final Client consumingClient;

    private StreamParameters(
            final int batchLimitEvents, @Nullable final Long streamLimitEvents, final long batchTimeoutMillis,
            @Nullable final Long streamTimeoutSeconds, @Nullable final Integer batchKeepAliveIterations,
            final int maxUncommittedMessages, final long commitTimeoutMillis, final Client consumingClient)
            throws UnprocessableEntityException {
        if (batchLimitEvents > 0) {
            this.batchLimitEvents = batchLimitEvents;
        } else {
            throw new UnprocessableEntityException("batch_limit can't be lower than 1");
        }
        this.streamLimitEvents = Optional.ofNullable(streamLimitEvents).filter(v -> v != 0);
        this.batchTimeoutMillis = batchTimeoutMillis;
        this.streamTimeoutMillis = TimeUnit.SECONDS.toMillis(
                Optional.ofNullable(streamTimeoutSeconds)
                        .filter(timeout -> timeout > 0 && timeout <= EventStreamConfig.MAX_STREAM_TIMEOUT)
                        .orElse((long) EventStreamConfig.generateDefaultStreamTimeout()));
        this.batchKeepAliveIterations = Optional.ofNullable(batchKeepAliveIterations);
        this.maxUncommittedMessages = maxUncommittedMessages;
        this.commitTimeoutMillis = commitTimeoutMillis;
        this.consumingClient = consumingClient;
    }

    public long getMessagesAllowedToSend(final long limit, final long sentSoFar) {
        return streamLimitEvents.map(v -> Math.max(0, Math.min(limit, v - sentSoFar))).orElse(limit);
    }

    public boolean isStreamLimitReached(final long commitedEvents) {
        return streamLimitEvents.map(v -> v <= commitedEvents).orElse(false);
    }

    public boolean isKeepAliveLimitReached(final IntStream keepAlive) {
        return batchKeepAliveIterations.map(it -> keepAlive.allMatch(v -> v >= it)).orElse(false);
    }

    public Client getConsumingClient() {
        return consumingClient;
    }

    public static StreamParameters of(
            final int batchLimitEvents,
            @Nullable final Long streamLimitEvents,
            final long batchTimeoutSeconds,
            @Nullable final Long streamTimeoutSeconds,
            @Nullable final Integer batchKeepAliveIterations,
            final int maxUncommittedMessages,
            final long commitTimeoutSeconds,
            final Client client) throws UnprocessableEntityException {
        return new StreamParameters(
                batchLimitEvents,
                streamLimitEvents,
                TimeUnit.SECONDS.toMillis(batchTimeoutSeconds),
                streamTimeoutSeconds,
                batchKeepAliveIterations,
                maxUncommittedMessages,
                TimeUnit.SECONDS.toMillis(commitTimeoutSeconds),
                client);
    }
}
