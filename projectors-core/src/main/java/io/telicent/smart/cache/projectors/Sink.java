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
package io.telicent.smart.cache.projectors;

/**
 * Interface for sinks that process items
 * <p>
 * Used to define simple processing chains without needing to get bogged down with some higher level abstraction like
 * DAGs because we're pretty much going to be limited to simple pipelines.
 * </p>
 * <p>
 * Sinks are auto-closeable allowing callers to enclose them in try-with-resources blocks and giving them optional
 * ability to clean up after themselves.
 * </p>
 *
 * @param <T> Item type
 */
@FunctionalInterface
public interface Sink<T> extends AutoCloseable {

    /**
     * Sends an item to the sink, throwing an error if it cannot be processed
     *
     * @param item Item
     * @throws SinkException Thrown if the item cannot be processed
     */
    void send(T item);

    @Override
    default void close() {
        // No-op by default
    }
}
