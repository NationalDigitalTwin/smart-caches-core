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
package io.telicent.smart.cache.observability;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.DoubleHistogram;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.Aggregation;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricExporter;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.telicent.smart.cache.observability.metrics.MetricsCollector;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestTelicentMetrics {

    @Test
    public void test_no_configuration() {
        OpenTelemetry otel = TelicentMetrics.get();
        Assert.assertNotNull(otel);
    }

    @Test
    public void test_explicit_configuration() {
        OpenTelemetry defOtel = TelicentMetrics.get();
        OpenTelemetry explicit = OpenTelemetry.propagating(ContextPropagators.noop());
        Assert.assertNotEquals(defOtel, explicit);

        TelicentMetrics.set(explicit);
        Assert.assertEquals(TelicentMetrics.get(), explicit);
    }

    @Test
    public void test_get_meter_01() {
        // With default, you get no-op meters, so regardless of the meter name provided you always get the same instance
        Meter m = TelicentMetrics.getMeter("test", "0.1");
        Meter m2 = TelicentMetrics.getMeter("test", "0.1");
        Assert.assertEquals(m, m2);

        Meter n = TelicentMetrics.getMeter("other", "0.1");
        Assert.assertEquals(m, n);
        Assert.assertEquals(m2, n);
    }

    @Test
    public void test_get_meter_02() {
        Meter m;
        try {
            mockOpenTelemetry();

            // If we've explicitly configured a proper Open Telemetry instance then we should get the same meter each
            // time we use the same meter name
            m = TelicentMetrics.getMeter("test", "0.1");
            Meter m2 = TelicentMetrics.getMeter("test", "0.1");
            Assert.assertEquals(m, m2);

            // However a different meter name should produce a different instance
            Meter n = TelicentMetrics.getMeter("other", "0.1");
            Assert.assertNotEquals(m, n);
            Assert.assertNotEquals(m2, n);
        } finally {
            TelicentMetrics.reset();
        }

        // After a reset should get different meter instances again
        Meter m3 = TelicentMetrics.getMeter("test", "0.1");
        Assert.assertNotEquals(m, m3);

        // Calling reset() again has no effect here
        TelicentMetrics.reset();
    }

    @Test
    public void test_get_meter_03() {
        TelicentMetrics.getMeter("test");
    }

    private static void mockOpenTelemetry() {
        MetricExporter exporter = mock(MetricExporter.class);
        when(exporter.getDefaultAggregation(any())).thenReturn(Aggregation.defaultAggregation());
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                                                         .registerMetricReader(PeriodicMetricReader.builder(exporter)
                                                                                                   .setInterval(
                                                                                                           Duration.ofSeconds(
                                                                                                                   5))
                                                                                                   .build())
                                                         .build();
        OpenTelemetry otel = OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build();
        TelicentMetrics.set(otel);
    }

    @Test
    public void test_get_counter_01() {
        try {
            mockOpenTelemetry();

            Meter m = TelicentMetrics.getMeter("test", "0.1");
            LongCounter counter = m.counterBuilder("test")
                                   .setDescription("Description")
                                   .build();
            LongCounter counter2 = m.counterBuilder("test")
                                    .setDescription("Description")
                                    .build();
            // Open Telemetry SDK caching should result in the same counter being returned here
            Assert.assertEquals(counter, counter2);


            // Even if we use a different meter a counter of the same name returns the same counter instance
            Meter m2 = TelicentMetrics.getMeter("other", "0.2");
            Assert.assertNotEquals(m, m2);
            LongCounter counter2a = m2.counterBuilder("test")
                                      .setDescription("Description")
                                      .build();
            Assert.assertEquals(counter, counter2a);

            // However a different counter name will result in a different instance
            LongCounter counter3 = m2.counterBuilder("other")
                                     .setDescription("Description")
                                     .build();
            Assert.assertNotEquals(counter, counter3);
            Assert.assertNotEquals(counter2, counter3);
        } finally {
            TelicentMetrics.reset();
        }
    }

    @Test
    public void time_01() {
        verifyTimingMetrics(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });
    }

    private static void verifyTimingMetrics(Runnable runnable) {
        try {
            MetricsCollector collector = new MetricsCollector();
            MetricReader reader = prepareTimingReader(collector);

            Meter m = TelicentMetrics.getMeter("test", "0.1");
            DoubleHistogram histogram = m.histogramBuilder("timings").build();
            Attributes attributes =
                    Attributes.of(AttributeKey.stringKey(AttributeNames.INSTANCE_ID), UUID.randomUUID().toString());

            TelicentMetrics.time(histogram, attributes, runnable);
            verifyTimingMetric(collector, reader);
        } finally {
            TelicentMetrics.reset();
        }
    }

    private static <T> void verifyTimingMetrics(Callable<T> runnable, Class<?> expectedError) {
        try {
            MetricsCollector collector = new MetricsCollector();
            MetricReader reader = prepareTimingReader(collector);

            Meter m = TelicentMetrics.getMeter("test", "0.1");
            DoubleHistogram histogram = m.histogramBuilder("timings").build();
            Attributes attributes =
                    Attributes.of(AttributeKey.stringKey(AttributeNames.INSTANCE_ID), UUID.randomUUID().toString());

            try {
                TelicentMetrics.time(histogram, attributes, runnable);
            } catch (Exception e) {
                if (expectedError == null) {
                    Assert.fail("Error not expected");
                } else {
                    Assert.assertTrue(expectedError.isAssignableFrom(e.getClass()));
                }
            }
            verifyTimingMetric(collector, reader);
        } finally {
            TelicentMetrics.reset();
        }
    }

    private static void verifyTimingMetric(MetricsCollector collector, MetricReader reader) {
        reader.forceFlush();

        Map<String, Map<Attributes, Double>> metrics = collector.getAllMetrics();
        Assert.assertFalse(metrics.isEmpty());
        Double count = collector.getMetric("timings.count", Attributes.empty());
        Assert.assertEquals(count, 1.0);
    }

    private static MetricReader prepareTimingReader(MetricsCollector collector) {
        MetricReader reader = PeriodicMetricReader.builder(collector).setInterval(Duration.ofSeconds(5)).build();
        SdkMeterProvider meterProvider = SdkMeterProvider.builder()
                                                         .registerMetricReader(reader)
                                                         .build();
        OpenTelemetry otel = OpenTelemetrySdk.builder().setMeterProvider(meterProvider).build();
        TelicentMetrics.set(otel);
        return reader;
    }

    @Test
    public void time_02() {
        verifyTimingMetrics(() -> 12345, null);
    }

    @Test
    public void time_03() {
        verifyTimingMetrics(() -> {
            throw new Exception("test");
        }, Exception.class);
    }

    @Test
    public void time_04() {
        verifyTimingMetrics(() -> {
            throw new FileNotFoundException();
        }, IOException.class);
    }
}
