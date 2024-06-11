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

import java.util.HashMap;
import java.util.Map;

/**
 * An in-memory offset store intended primarily for testing
 */
public class MemoryOffsetStore extends AbstractOffsetStore {

    /**
     * The in-memory cache of offsets
     */
    protected final Map<String, Object> offsets = new HashMap<>();

    @Override
    protected boolean hasOffsetInternal(String key) {
        return this.offsets.containsKey(key);
    }

    @Override
    protected <T> void saveOffsetInternal(String key, T offset) {
        this.offsets.put(key, offset);
    }

    @Override
    protected Object getRawOffset(String key) {
        return this.offsets.get(key);
    }

    @Override
    protected void deleteOffsetInternal(String key) {
        this.offsets.remove(key);
    }

    @Override
    protected void flushInternal() {
        // Not persistent so nothing to do by default!
    }

    @Override
    protected void closeInternal() {
        this.offsets.clear();
    }

    @Override
    public <T> boolean supportsOffsetType(Class<T> offsetType) {
        // All types are supported in this store
        return true;
    }
}
