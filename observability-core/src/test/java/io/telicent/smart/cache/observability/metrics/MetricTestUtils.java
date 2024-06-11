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
package io.telicent.smart.cache.observability.metrics;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.metrics.SdkMeterProvider;
import io.opentelemetry.sdk.metrics.export.MetricReader;
import io.opentelemetry.sdk.metrics.export.PeriodicMetricReader;
import io.telicent.smart.cache.observability.TelicentMetrics;

import java.time.Duration;

/**
 * Metrics testing utility.
 */
public class MetricTestUtils {

    private static MetricsCollector METRICS;
    private static MetricReader READER;

    /**
     * Enables Metrics capture
     */
    public static void enableMetricsCapture() {
        TelicentMetrics.reset();
        METRICS = new MetricsCollector();
        READER = PeriodicMetricReader.builder(METRICS).setInterval(Duration.ofSeconds(5)).build();
        OpenTelemetrySdk otel = OpenTelemetrySdk.builder()
                                                .setMeterProvider(
                                                        SdkMeterProvider.builder().registerMetricReader(READER).build())
                                                .build();
        TelicentMetrics.set(otel);
    }

    /**
     * Disables metric capture
     */
    public static void disableMetricsCapture() {
        TelicentMetrics.reset();
        if (METRICS != null) {
            METRICS.close();
            METRICS = null;
        }
    }

    /**
     * Gets a labelled metric that has been reported to Open Telemetry
     *
     * @param metricsName Metric name
     * @param labelName   Label name
     * @param labelValue  Label Value
     * @return Metric, or {@code null} if no such metric
     * @throws IllegalStateException Thrown if you haven't called {@link #enableMetricsCapture()} prior to calling this
     *                               or no such metric can be found
     */
    public static Double getReportedMetric(String metricsName, String labelName, String labelValue) {
        requireMetricsCapture();
        READER.forceFlush();

        Double value = METRICS.getMetric(metricsName, Attributes.of(AttributeKey.stringKey(labelName), labelValue));
        ensureMetricWasReported(metricsName, value);
        return value;
    }

    private static void ensureMetricWasReported(String metricsName, Double value) {
        if (value == null) {
            throw new IllegalStateException("Metric " + metricsName + " was not found");
        }
    }

    private static void requireMetricsCapture() {
        if (METRICS == null) {
            throw new IllegalStateException("Must call TestUtils.enableMetricsCapture() as part of your test setup");
        }
    }

    /**
     * Gets a labelled metric that has been reported to Open Telemetry
     *
     * @param metricsName      Metric name
     * @param metricAttributes Attributes used for labelling
     * @return Metric, or {@code null} if no such metric
     * @throws IllegalStateException Thrown if you haven't called {@link #enableMetricsCapture()} prior to calling this
     *                               or no such metric can be found
     */
    public static Double getReportedMetric(String metricsName, Attributes metricAttributes) {
        requireMetricsCapture();
        READER.forceFlush();

        Double value = METRICS.getMetric(metricsName, MetricsCollector.attributesForStorage(metricAttributes));
        ensureMetricWasReported(metricsName, value);
        return value;
    }

    /**
     * Gets an unlabelled metric that has been reported
     *
     * @param metricsName Metrics name
     * @return Metric
     * @throws IllegalStateException Thrown if you haven't called {@link #enableMetricsCapture()} prior to calling this
     *                               or no such metric can be found
     */
    public static Double getReportedMetric(String metricsName) {
        requireMetricsCapture();
        READER.forceFlush();

        Double value = METRICS.getMetric(metricsName, Attributes.empty());
        ensureMetricWasReported(metricsName, value);
        return value;
    }
}
