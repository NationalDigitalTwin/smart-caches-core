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
package io.telicent.smart.cache.cli.commands.projection;

import io.telicent.smart.cache.cli.commands.AbstractCommandTests;
import io.telicent.smart.cache.cli.commands.SmartCacheCommand;
import io.telicent.smart.cache.cli.commands.SmartCacheCommandTester;
import io.telicent.smart.cache.sources.Event;
import io.telicent.smart.cache.sources.Header;
import io.telicent.smart.cache.sources.kafka.KafkaEventSource;
import io.telicent.smart.cache.sources.kafka.KafkaTestCluster;
import io.telicent.smart.cache.sources.kafka.policies.KafkaReadPolicies;
import io.telicent.smart.cache.sources.kafka.sinks.KafkaSink;
import io.telicent.smart.cache.sources.memory.SimpleEvent;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.testcontainers.shaded.org.apache.commons.lang3.StringUtils;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class DockerTestDeadLetterQueue extends AbstractCommandTests {

    public static final String DEAD_LETTER_TOPIC = "dead-letters";
    private static final int TEST_DATA_SIZE = 1_000;

    private final KafkaTestCluster kafka = new KafkaTestCluster();

    @BeforeClass
    @Override
    public void setup() {
        this.kafka.setup();
        this.kafka.resetTopic(DEAD_LETTER_TOPIC);
        generateKafkaEvents(Collections.emptyList(), "Example message %,d");

        super.setup();
    }

    @AfterMethod
    @Override
    public void testCleanup() throws InterruptedException {
        super.testCleanup();

        this.kafka.resetTopic(DEAD_LETTER_TOPIC);
    }

    @AfterClass
    @Override
    public void teardown() {
        this.kafka.teardown();

        super.teardown();
    }

    private void generateKafkaEvents(Collection<Header> headers, String format) {
        try (KafkaSink<String, String> sink = KafkaSink.<String, String>create()
                                                       .keySerializer(StringSerializer.class)
                                                       .valueSerializer(StringSerializer.class)
                                                       .bootstrapServers(this.kafka.getBootstrapServers())
                                                       .topic(KafkaTestCluster.DEFAULT_TOPIC)
                                                       .lingerMs(5)
                                                       .build()) {
            for (int i = 1; i <= TEST_DATA_SIZE; i++) {
                sink.send(new SimpleEvent<>(headers, Integer.toString(i), String.format(format, i)));
            }
        }
    }

    private void verifyDeadLetters(String topic, int expected) {
        KafkaEventSource<Bytes, Bytes> deadLetters = KafkaEventSource.<Bytes, Bytes>create()
                                                                     .bootstrapServers(this.kafka.getBootstrapServers())
                                                                     .topic(DEAD_LETTER_TOPIC)
                                                                     .consumerGroup(DEAD_LETTER_TOPIC)
                                                                     .readPolicy(KafkaReadPolicies.fromBeginning())
                                                                     .keyDeserializer(BytesDeserializer.class)
                                                                     .valueDeserializer(BytesDeserializer.class)
                                                                     .build();
        try {
            int found = 0;
            while (true) {
                Event<Bytes, Bytes> event = deadLetters.poll(Duration.ofSeconds(3));
                if (event == null) {
                    break;
                }
                found++;
            }

            Assert.assertEquals(found, expected);
        } finally {
            deadLetters.close();
        }
    }

    private void runCommand(Class<? extends SmartCacheCommand> commandClass, String deadLetterTopic,
                            Integer deadLetterFrequency) {
        List<String> args = new ArrayList<>();
        //@formatter:off
        CollectionUtils.addAll(args,
                               "--bootstrap-servers",
                               this.kafka.getBootstrapServers(),
                               "--topic",
                               KafkaTestCluster.DEFAULT_TOPIC,
                               "--max-stalls",
                               "1",
                               "--poll-timeout",
                               "3",
                               "--read-policy",
                               "BEGINNING");
        if (StringUtils.isNotBlank(deadLetterTopic)) {
            CollectionUtils.addAll(args, "--dlq-topic", deadLetterTopic);
        }
        if (deadLetterFrequency != null) {
            CollectionUtils.addAll(args, "--dead-letter-frequency", Integer.toString(deadLetterFrequency));
        }

        SmartCacheCommand.runAsSingleCommand(commandClass, args.toArray(new String[0]));
    }


    @Test
    public void givenCommandWithNoDLQ_whenProjecting_thenNoDeadLetters() {
        // Given
        // Data generated once in setup()

        // When
        runCommand(AsIsProjectionCommand.class, null, null);

        // Then
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), 0);
        verifyDeadLetters(DEAD_LETTER_TOPIC, 0);
    }

    @Test
    public void givenCommandWithDLQConfigured_whenProjecting_thenDeadLettersAreCreated() {
        // Given
        // Data generated once in setup()

        // When
        runCommand(AsIsProjectionCommand.class, DEAD_LETTER_TOPIC, null);

        // Then
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), 0);
        verifyDeadLetters(DEAD_LETTER_TOPIC, TEST_DATA_SIZE / 10);
    }

    @Test
    public void givenTransformingCommandWithDLQConfigured_whenProjecting_thenDeadLettersAreCreated() {
        // Given
        // Data generated once in setup()

        // When
        runCommand(TransformingProjectorCommand.class, DEAD_LETTER_TOPIC, null);

        // Then
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), 0);
        verifyDeadLetters(DEAD_LETTER_TOPIC, TEST_DATA_SIZE / 10);
    }

    @Test
    public void givenBadTransformingCommandWithDLQConfigured_whenProjecting_thenFails() {
        // Given
        // Data generated once in setup()

        // When
        runCommand(BadTransformingProjectorCommand.class, DEAD_LETTER_TOPIC, null);

        // Then
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), 1);
        String stdErr = SmartCacheCommandTester.getLastStdErr();
        Assert.assertTrue(StringUtils.contains(stdErr, "ClassCastException"));
    }

    @Test
    public void givenCommandWithDLQForEverything_whenProjecting_thenEverythingIsDeadLettered() {
        // Given
        // Data generated once in setup()

        // When
        runCommand(AsIsProjectionCommand.class, DEAD_LETTER_TOPIC, 1);

        // Then
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), 0);
        verifyDeadLetters(DEAD_LETTER_TOPIC, TEST_DATA_SIZE);
    }

    @Test
    public void givenCommandWithDLQForNothing_whenProjecting_thenNothingIsDeadLettered() {
        // Given
        // Data generated once in setup()

        // When
        runCommand(AsIsProjectionCommand.class, DEAD_LETTER_TOPIC, 100_000);

        // Then
        Assert.assertEquals(SmartCacheCommandTester.getLastExitStatus(), 0);
        verifyDeadLetters(DEAD_LETTER_TOPIC, 0);
    }
}
