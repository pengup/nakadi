package org.zalando.nakadi.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.zalando.nakadi.annotations.DB;
import org.zalando.nakadi.repository.EventTypeRepository;
import org.zalando.nakadi.repository.db.EventTypeCache;
import org.zalando.nakadi.repository.db.TimelineDbRepository;
import org.zalando.nakadi.repository.kafka.KafkaConfig;
import org.zalando.nakadi.repository.zookeeper.ZooKeeperHolder;
import org.zalando.nakadi.repository.zookeeper.ZookeeperConfig;
import org.zalando.nakadi.service.FeatureToggleService;
import org.zalando.nakadi.service.FeatureToggleService.Feature;
import org.zalando.nakadi.service.FeatureToggleService.FeatureWrapper;
import org.zalando.nakadi.service.FeatureToggleServiceZk;
import org.zalando.nakadi.service.timeline.TimelineSync;
import org.zalando.nakadi.validation.EventBodyMustRespectSchema;
import org.zalando.nakadi.validation.EventMetadataValidationStrategy;
import org.zalando.nakadi.validation.JsonSchemaEnrichment;
import org.zalando.nakadi.validation.ValidationStrategy;

import java.util.Set;

@Configuration
@Profile("!test")
@Import({KafkaConfig.class, ZookeeperConfig.class})
@EnableConfigurationProperties(FeaturesConfig.class)
public class RepositoriesConfig {

    private static final Logger LOG = LoggerFactory.getLogger(RepositoriesConfig.class);

    @Profile({"acceptanceTest", "local"})
    @Bean
    public FeatureToggleService featureToggleServiceLocal(final ZooKeeperHolder zooKeeperHolder,
                                                          final FeaturesConfig featuresConfig) {
        final FeatureToggleService featureToggleService = new FeatureToggleServiceZk(zooKeeperHolder);
        if (featuresConfig.containsDefaults()) {
            final Set<String> features = featuresConfig.getFeaturesWithDefaultState();
            for (final String featureStr : features) {
                final boolean defaultState = featuresConfig.getDefaultState(featureStr);
                LOG.info("Setting feature {} to {}", featureStr, defaultState);
                final FeatureWrapper featureWrapper = new FeatureWrapper(Feature.valueOf(featureStr), defaultState);
                featureToggleService.setFeature(featureWrapper);
            }
        }
        return featureToggleService;
    }

    @Profile("default")
    @Bean
    public FeatureToggleService featureToggleService(final ZooKeeperHolder zooKeeperHolder) {
        return new FeatureToggleServiceZk(zooKeeperHolder);
    }

    @Bean
    public EventTypeCache eventTypeCache(final ZooKeeperHolder zooKeeperHolder,
                                         @DB final EventTypeRepository eventTypeRepository,
                                         @DB final TimelineDbRepository timelineRepository,
                                         final TimelineSync timelineSync) {
        ValidationStrategy.register(EventBodyMustRespectSchema.NAME, new EventBodyMustRespectSchema(
                new JsonSchemaEnrichment()
        ));
        ValidationStrategy.register(EventMetadataValidationStrategy.NAME, new EventMetadataValidationStrategy());

        try {
            return new EventTypeCache(eventTypeRepository, timelineRepository, zooKeeperHolder, timelineSync);
        } catch (final Exception e) {
            throw new IllegalStateException("failed to create event type cache", e);
        }
    }

}
