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
package io.telicent.smart.cache.sources.offsets;

/**
 * A store of read offsets
 */
public interface OffsetStore {

    /**
     * Is there an offset stored under the given key?
     * <p>
     * Note that just because an implementation returns {@code true} <strong>DOES NOT</strong> guarantee that an actual
     * offset is stored.  For implementations that are capable of storing {@code null} offsets then they
     * <strong>SHOULD</strong> return {@code true} even if the actual stored offset is {@code null}.
     * </p>
     *
     * @param key Key
     * @return True if a stored offset exists, false otherwise
     * @throws NullPointerException  Thrown if the key is null
     * @throws IllegalStateException Thrown if the offset store has been closed
     */
    boolean hasOffset(String key);

    /**
     * Saves an offset under the given key
     *
     * @param key    Key
     * @param offset Offset
     * @param <T>    Offset type
     * @throws NullPointerException  Thrown if the key is null
     * @throws IllegalStateException Thrown if the offset store has been closed
     */
    <T> void saveOffset(String key, T offset);

    /**
     * Loads an offset for the given key
     *
     * @param key Key
     * @param <T> Offset type
     * @return Offset, or {@code null} if no offset is available for the key
     * @throws NullPointerException  Thrown if the key is null
     * @throws ClassCastException    Thrown if the offset is not castable to the desired type
     * @throws IllegalStateException Thrown if the offset store has been closed
     */
    <T> T loadOffset(String key);

    /**
     * Deletes the previously stored offset (if any) for the given key
     *
     * @param key Key
     * @throws NullPointerException  Thrown if the key is null
     * @throws IllegalStateException Thrown if the offset store has been closed
     */
    void deleteOffset(String key);

    /**
     * Gets whether this store supports offsets of the given type
     *
     * @param offsetType Offset type class
     * @param <T>        Offset type
     * @return True if supported, false otherwise
     */
    <T> boolean supportsOffsetType(Class<T> offsetType);

    /**
     * Flushes the offset store
     * <p>
     * For offset stores that are persistent, i.e. offsets can persist beyond the lifetime of the instance, then this
     * should have the effect of persisting the current offsets.
     * </p>
     */
    void flush();

    /**
     * Closes the offset store releasing any held resources
     * <p>
     * Additionally for offset stores that are persistent, i.e. offsets can persist beyond the lifetime of the instance
     * then this should also have the effect of persisting the current offsets.
     * </p>
     */
    void close();
}
