package org.zalando.nakadi.service;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.zalando.nakadi.config.JsonConfig;
import org.zalando.nakadi.domain.EventType;
import org.zalando.nakadi.plugin.api.authz.AuthorizationService;
import org.zalando.nakadi.plugin.api.authz.Subject;
import org.zalando.nakadi.security.UsernameHasher;

import java.util.Optional;

import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zalando.nakadi.utils.TestUtils.buildDefaultEventType;
import static uk.co.datumedge.hamcrest.json.SameJSONAs.sameJSONAs;

public class NakadiAuditLogPublisherTest {

    public class TestSubject implements Subject {
        String name;

        public TestSubject(final String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }

    @Test
    public void testPublishAuditLog() {
        final EventsProcessor processor = mock(EventsProcessor.class);
        final FeatureToggleService toggle = mock(FeatureToggleService.class);
        final AuthorizationService authorizationService = mock(AuthorizationService.class);

        when(toggle.isFeatureEnabled(FeatureToggleService.Feature.AUDIT_LOG_COLLECTION)).thenReturn(true);

        final NakadiAuditLogPublisher publisher = new NakadiAuditLogPublisher(
                toggle,
                processor,
                new JsonConfig().jacksonObjectMapper(),
                new UsernameHasher("salt"),
                authorizationService,
                "audit-event-type");
        when(authorizationService.getSubject()).thenReturn(Optional.of(new TestSubject("user-name")));
        final DateTime now = DateTime.parse("2019-01-16T13:44:16.819Z");
        final EventType et = buildDefaultEventType();
        et.setName("new-et-name");
        et.setCreatedAt(now);
        et.setUpdatedAt(now);
        et.getSchema().setCreatedAt(now);
        publisher.publish(Optional.empty(), Optional.of(et),
                NakadiAuditLogPublisher.ResourceType.EVENT_TYPE,
                NakadiAuditLogPublisher.ActionType.CREATED, "et-name");

        final ArgumentCaptor<JSONObject> supplierCaptor = ArgumentCaptor.forClass(JSONObject.class);
        verify(processor, times(1)).enrichAndSubmit(eq("audit-event-type"), supplierCaptor.capture());

        assertThat(new JSONObject("{" +
                        "\"data_op\":\"C\"," +
                        "\"data\":{" +
                        "\"new_object\":{" +
                        "\"schema\":{" +
                        "\"schema\":\"{ \\\"properties\\\": { \\\"foo\\\": { \\\"type\\\": \\\"string\\\"" +
                        " } } }\"," +
                        "\"created_at\":\"2019-01-16T13:44:16.819Z\"," +
                        "\"type\":\"json_schema\"," +
                        "\"version\":\"1.0.0\"" +
                        "}," +
                        "\"compatibility_mode\":\"compatible\"," +
                        "\"ordering_key_fields\":[]," +
                        "\"created_at\":\"2019-01-16T13:44:16.819Z\"," +
                        "\"cleanup_policy\":\"delete\"," +
                        "\"ordering_instance_ids\":[]," +
                        "\"authorization\":null," +
                        "\"partition_key_fields\":[]," +
                        "\"updated_at\":\"2019-01-16T13:44:16.819Z\"," +
                        "\"default_statistic\":null," +
                        "\"name\":\"new-et-name\"," +
                        "\"options\":{\"retention_time\":172800000}," +
                        "\"partition_strategy\":\"random\"," +
                        "\"owning_application\":\"event-producer-application\"," +
                        "\"enrichment_strategies\":[]," +
                        "\"category\":\"undefined\"" +
                        "}," +
                        "\"new_text\":\"{\\\"name\\\":\\\"new-et-name\\\",\\\"owning_application\\\":\\\"event" +
                        "-producer-application\\\",\\\"category\\\":\\\"undefined\\\",\\\"enrichment_strategies\\\"" +
                        ":[],\\\"partition_strategy\\\":\\\"random\\\",\\\"partition_key_fields\\\":[],\\\"cleanup" +
                        "_policy\\\":\\\"delete\\\",\\\"ordering_key_fields\\\":[],\\\"ordering_instance_ids\\\":[" +
                        "],\\\"schema\\\":{\\\"type\\\":\\\"json_schema\\\",\\\"schema\\\":\\\"{ \\\\\\\"propertie" +
                        "s\\\\\\\": { \\\\\\\"foo\\\\\\\": { \\\\\\\"type\\\\\\\": \\\\\\\"string\\\\\\\" } } }\\\"" +
                        ",\\\"version\\\":\\\"1.0.0\\\",\\\"created_at\\\":\\\"2019-01-16T13:44:16.819Z\\\"},\\\"d" +
                        "efault_statistic\\\":null,\\\"options\\\":{\\\"retention_time\\\":172800000},\\\"authoriz" +
                        "ation\\\":null,\\\"compatibility_mode\\\":\\\"compatible\\\",\\\"updated_at\\\":\\\"2019-" +
                        "01-16T13:44:16.819Z\\\",\\\"created_at\\\":\\\"2019-01-16T13:44:16.819Z\\\"}\"," +
                        "\"resource_type\":\"event_type\"," +
                        "\"resource_id\":\"et-name\"," +
                        "\"user\":\"user-name\"," +
                        "\"user_hash\":\"89bc5f7398509d3ce86c013c138e11357ff7f589fca9d58cfce443c27f81956c\"" +
                        "}," +
                        "\"data_type\":\"event_type\"}").toString(),
                sameJSONAs(supplierCaptor.getValue().toString()));
    }

}