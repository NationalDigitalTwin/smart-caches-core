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
package io.telicent.smart.cache.sources.file;

/**
 * Possible operation modes for file event IO
 */
public enum FileEventAccessMode {
    /**
     * Read-only
     */
    ReadOnly(true, false),
    /**
     * Write only
     */
    WriteOnly(false, true),
    /**
     * Read/write
     */
    ReadWrite(true, true);

    private final boolean requiresSerializers, requiresDeserializers;

    /**
     * Creates a new access mode
     *
     * @param requiresDeserializers Whether deserializers are required for this mode
     * @param requiresSerializers   Whether serializers are required for this mode
     */
    FileEventAccessMode(boolean requiresDeserializers, boolean requiresSerializers) {
        this.requiresSerializers = requiresSerializers;
        this.requiresDeserializers = requiresDeserializers;
    }

    /**
     * Gets whether this access mode requires serializers to be configured
     *
     * @return True if serializers are required, false otherwise
     */
    public boolean requiresSerializers() {
        return this.requiresSerializers;
    }

    /**
     * Gets whether this access mode requires deserializers to be configured
     *
     * @return True if deserializers are required, false otherwise
     */
    public boolean requiresDeserializers() {
        return this.requiresDeserializers;
    }
}
