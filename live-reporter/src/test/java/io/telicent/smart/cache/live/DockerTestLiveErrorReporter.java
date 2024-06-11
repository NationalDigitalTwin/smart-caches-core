/**
 * Copyright (C) Telicent Ltd
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.telicent.smart.cache.live;

import io.telicent.smart.cache.live.model.LiveError;
import io.telicent.smart.cache.live.serializers.LiveErrorDeserializer;
import io.telicent.smart.cache.live.serializers.LiveErrorSerializer;
import io.telicent.smart.cache.sources.Event;
import io.telicent.smart.cache.sources.kafka.KafkaEventSource;
import io.telicent.smart.cache.sources.kafka.KafkaTestCluster;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.utils.Bytes;
import org.slf4j.event.Level;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

public class DockerTestLiveErrorReporter extends AbstractDockerLiveTests<LiveError> {

    @Override
    protected Class<? extends Serializer<LiveError>> getSerializerClass() {
        return LiveErrorSerializer.class;
    }

    private final AtomicLong id = new AtomicLong();

    private void verifyErrors(long expected) {
        //@formatter:off
        KafkaEventSource<Bytes, LiveError> source
                = KafkaEventSource.<Bytes, LiveError>create()
                                  .bootstrapServers(this.kafka.getBootstrapServers())
                                  .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                  .consumerGroup("error-reporter-" + id.incrementAndGet())
                                  .autoCommit()
                                  .keyDeserializer(BytesDeserializer.class)
                                  .valueDeserializer(LiveErrorDeserializer.class)
                                  .build();
        //@formatter:on

        long actual = 0;
        do {
            Event<Bytes, LiveError> event = source.poll(Duration.ofSeconds(1));
            if (event == null) {
                break;
            }
            actual++;
        } while (source.remaining() != null && source.remaining() > 0);

        Assert.assertEquals(actual, expected);
    }

    @Test
    public void live_errors_kafka_01() {
        LiveErrorReporter reporter = LiveErrorReporter.create().destination(this.createSink()).build();
        reporter.close();

        verifyErrors(0);
    }

    @Test
    public void live_errors_kafka_02() {
        LiveErrorReporter reporter = LiveErrorReporter.create().destination(this.createSink()).build();
        for (int i = 1; i <= 100; i++) {
            LiveError error = LiveError.create()
                                       .id("errors-02")
                                       .now()
                                       .type("TestError")
                                       .message(String.format("Error %,d", i))
                                       .level(Level.ERROR)
                                       .recordCounter((long) i)
                                       .build();
            reporter.reportError(error);
        }
        reporter.close();
        verifyErrors(100);
    }

    @Test
    public void live_errors_kafka_03() {
        LiveErrorReporter reporter = LiveErrorReporter.create().destination(this.createSink()).build();
        for (int i = 1; i <= 100; i++) {
            Throwable t = new RuntimeException("test");
            LiveError error =
                    LiveError.create().id("errors-03").error(t).level(Level.ERROR).recordCounter((long) i).build();
            reporter.reportError(error);
        }
        reporter.close();
        verifyErrors(100);
    }
}
