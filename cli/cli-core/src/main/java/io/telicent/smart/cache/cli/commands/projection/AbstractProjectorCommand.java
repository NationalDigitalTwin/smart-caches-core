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

import com.github.rvesse.airline.annotations.AirlineModule;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.ranges.LongRange;
import io.telicent.smart.cache.cli.commands.SmartCacheCommand;
import io.telicent.smart.cache.cli.options.FileSourceOptions;
import io.telicent.smart.cache.projectors.Projector;
import io.telicent.smart.cache.projectors.Sink;
import io.telicent.smart.cache.projectors.driver.ProjectorDriver;
import io.telicent.smart.cache.projectors.sinks.Sinks;
import io.telicent.smart.cache.projectors.sinks.ThroughputSink;
import io.telicent.smart.cache.sources.CapturingEventSource;
import io.telicent.smart.cache.sources.Event;
import io.telicent.smart.cache.sources.EventSource;
import io.telicent.smart.cache.sources.Header;
import org.apache.kafka.common.serialization.BytesDeserializer;
import org.apache.kafka.common.serialization.BytesSerializer;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.kafka.common.serialization.Serializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Function;

/**
 * Abstract base class for commands that create and run a Projector
 *
 * @param <TKey>    Event key type
 * @param <TValue>  Event value type
 * @param <TOutput> Output type
 */
public abstract class AbstractProjectorCommand<TKey, TValue, TOutput> extends SmartCacheCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractProjectorCommand.class);

    /** The bytes serialiser, which is cached here, thread-safe and does not have implementation for or need to be {@link java.io.Closeable}. */
    protected static final Serializer<?> BYTES_SERIALIZER = new BytesSerializer();
    /** The bytes deserialiser, which is cached here, thread-safe and does not have implementation for or need to be {@link java.io.Closeable}. */
    protected static final Deserializer<?> BYTES_DESERIALIZER = new BytesDeserializer();

    /**
     * Limits the number of events to be projected
     */
    @Option(name = {
            "-l",
            "--limit"
    }, title = "EventLimit", description = "Sets the maximum number of events to process, a value less than zero means unlimited.  Defaults to -1 i.e. unlimited.  Once this limit is reached projection will abort.")
    protected long limit = -1;

    /**
     * Controls whether projection should abort when sufficient
     */
    @Option(name = { "--max-stalls" }, title = "MaxStalls", description = "Specifies the maximum number of consecutive stalls, i.e. instances where the event source returns no new events, after which projection is aborted.  Defaults to -1 i.e. unlimited, this means that the projection will effectively run forever by default waiting indefinitely for new events.")
    protected long maxStalls = -1;

    /**
     * Controls how frequently event throughput is reported
     */
    @Option(name = { "--report-batch-size" }, title = "ReportBatchSize", description = "Specifies how often event throughput should be reported.")
    protected long reportBatchSize = 10_000L;

    /**
     * Controls how long a timeout we use for our {@link EventSource#poll(Duration)} calls
     */
    @Option(name = { "--poll-timeout" }, title = "PollTimeoutSeconds", description = "Specifies how long a timeout to use for each poll call against the configured event source.  A longer timeout means fewer poll calls are made against the event source but the command may be blocked from responding to termination signals for long periods while waiting for an active poll operation to complete.  Default is 30 seconds and a minimum of 1 seconds may be specified.")
    @LongRange(min = 1)
    protected long pollTimeout = 30L;

    /**
     * Provides file source options
     */
    @AirlineModule
    protected FileSourceOptions<TKey, TValue> fileSourceOptions = new FileSourceOptions();

    /**
     * Gets the key serializer, needed for event capture. Defaults to {@link BytesSerializer} in this implementation ,
     * as most use cases require this.
     *
     * @return Key serializer
     */
    @SuppressWarnings("unchecked")
    protected Serializer<TKey> keySerializer() {
        return (Serializer<TKey>)BYTES_SERIALIZER;
    }

    /**
     * Gets the key deserializer, needed for event replay. Defaults to {@link BytesDeserializer} in this implementation ,
     * as most use cases require this.
     *
     * @return Key deserializer
     */
    @SuppressWarnings("unchecked")
    protected Deserializer<TKey> keyDeserializer() {
        return (Deserializer<TKey>)BYTES_DESERIALIZER;
    }

    /**
     * Gets the value serializer, needed for event capture
     *
     * @return Value serializer
     */
    protected abstract Serializer<TValue> valueSerializer();

    /**
     * Gets the value deserializer, needed for event replay
     *
     * @return Value deserializer
     */
    protected abstract Deserializer<TValue> valueDeserializer();

    /**
     * Gets the additional headers that should be added to captured events when event capture is enabled (if any).
     * <p>
     * Defaults to returning {@code null} which means no additional headers are added to captured events
     * </p>
     *
     * @return Additional capture headers
     */
    protected List<Header> additionalCaptureHeaders() {
        return null;
    }

    /**
     * Gets the additional header generators that should be added to captured events when event capture is enabled (if
     * any).
     * <p>
     * Defaults to returning {@code null} which means no additional headers are added to captured events
     * </p>
     *
     * @return Additional capture header generators
     */
    protected List<Function<Event<TKey, TValue>, Header>> additionalCaptureHeaderGenerators() {
        return null;
    }

    /**
     * Prepares a work sink that will be wrapped with {@link ThroughputSink} for throughput reporting
     *
     * @return Throughput sink
     */
    protected final Sink<TOutput> prepareSink() {
        Sink<TOutput> work = prepareWorkSink();
        return Sinks.<TOutput>throughput()
                    .tracker(t -> t.reportBatchSize(this.reportBatchSize)
                                   .inSeconds()
                                   .action("Projected")
                                   .itemsName(getThroughputItemsName())
                                   .metricsLabel(getThroughputItemsName().toLowerCase(Locale.ROOT)))
                    .destination(work)
                    .build();
    }

    /**
     * Gets the name for the items being projected that should be used by the {@link ThroughputSink} to report on the
     * progress
     *
     * @return Throughput Items Name
     */
    protected abstract String getThroughputItemsName();

    @Override
    public final int run() {
        EventSource<TKey, TValue> source = this.fileSourceOptions.useFileSource() ?
                                           this.fileSourceOptions.getFileSource(this.keyDeserializer(),
                                                                                this.valueDeserializer()) : getSource();

        // If event capture is enabled wrap the real source in a capturing event source
        Sink<Event<TKey, TValue>> capture =
                this.fileSourceOptions.getCaptureSink(keySerializer(), valueSerializer(), additionalCaptureHeaders(),
                                                      additionalCaptureHeaderGenerators());
        if (capture != null) {
            source = new CapturingEventSource<>(source, capture);
        }

        Projector<Event<TKey, TValue>, TOutput> projector = getProjector();
        //@formatter:off
        ProjectorDriver<TKey, TValue, TOutput> driver
                = ProjectorDriver.<TKey, TValue, TOutput>create()
                                 .source(source)
                                 .projector(projector)
                                 .destination(this::prepareSink)
                                 .pollTimeout(Duration.ofSeconds(this.pollTimeout))
                                 .limit(this.limit)
                                 .maxStalls(this.maxStalls)
                                 .reportBatchSize(this.reportBatchSize)
                                 .build();
        //@formatter:on

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Future<?> future = executor.submit(driver);
        Runtime.getRuntime().addShutdownHook(new Thread(new CancelDriver(driver, future)));
        try {
            future.get();
        } catch (InterruptedException e) {
            LOGGER.warn("Interrupted while waiting for projection to finish");
            return 1;
        } catch (ExecutionException e) {
            LOGGER.error("Unexpected error in projection: {}", e.getMessage());
            Throwable t = e.getCause();
            while (t != null) {
                LOGGER.error("  Caused by: {}", t.getMessage());
                t = t.getCause();
            }

            // Dump the stack trace
            e.printStackTrace();
            return 1;
        }

        return 0;
    }

    /**
     * Gets the event source that is in use
     *
     * @return Event Source
     */
    protected abstract EventSource<TKey, TValue> getSource();

    /**
     * Gets the projector to use for this command. In this implementation, the projector simply passes on its
     * upstream input to its downstream output, assuming the types are compatible.
     *
     * @return Projector
     */
    @SuppressWarnings("unchecked")
    protected Projector<Event<TKey, TValue>, TOutput> getProjector() {
        return (event, sink) -> sink.send((TOutput)event);
    }

    /**
     * Prepares the actual sink that performs the processing of the output from the projector used by this command
     *
     * @return Actual sink
     */
    protected abstract Sink<TOutput> prepareWorkSink();

    /**
     * Prepares the dead letter sink, if any, where erroneous output from the projector is written.
     *
     * @return a dead letter sink, which may be null to indicate none configured.
     * @param <K> Key type
     * @param <V> Value type
     */
    protected <K, V> Sink<Event<K, V>> prepareDeadLetterSink() {
        return null;
    }

    private class CancelDriver implements Runnable {
        private final ProjectorDriver<TKey, TValue, TOutput> driver;
        private final Future<?> future;

        public CancelDriver(ProjectorDriver<TKey, TValue, TOutput> driver, Future<?> future) {
            this.driver = driver;
            this.future = future;
        }

        @Override
        public void run() {
            driver.cancel();
            try {
                future.get();
            } catch (Throwable e) {
                // Ignored, just trying to ensure that the driver has finished before we allow the JVM to exit
            }
        }
    }
}
