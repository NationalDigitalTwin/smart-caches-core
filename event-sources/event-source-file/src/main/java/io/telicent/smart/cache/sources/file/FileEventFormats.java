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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Registry of supported file event formats
 */
public class FileEventFormats {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileEventFormats.class);

    private static final Map<String, FileEventFormatProvider> PROVIDERS = new HashMap<>();

    static {
        try {
            ServiceLoader<FileEventFormatProvider> loader = ServiceLoader.load(FileEventFormatProvider.class);

            for (FileEventFormatProvider format : loader) {
                PROVIDERS.put(format.name(), format);
            }
        } catch (ServiceConfigurationError e) {
            LOGGER.warn("Failed to load file event formats: {}", e.getMessage());
        }
    }

    /**
     * Gets the available file event formats
     *
     * @return Available format names
     */
    public static Set<String> available() {
        return Collections.unmodifiableSet(PROVIDERS.keySet());
    }

    /**
     * Gets the format with the given name (if supported)
     *
     * @param name Format name
     * @return Format, or {@code null} if not supported
     */
    public static FileEventFormatProvider get(String name) {
        return PROVIDERS.get(name);
    }
}
