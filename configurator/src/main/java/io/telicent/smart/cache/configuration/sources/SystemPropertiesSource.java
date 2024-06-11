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
package io.telicent.smart.cache.configuration.sources;

/**
 * A configuration source backed by system properties.
 * <p>
 * This always reflects the current state of {@link System#getProperties()} so any changes to System Properties will be
 * immediately visible to this source.  If you prefer to capture only the current state of System Properties then you
 * should use a {@link PropertiesSource} instead taking a copy of {@link System#getProperties()}.
 */
public final class SystemPropertiesSource implements ConfigurationSource {

    /**
     * The singleton instance of the system properties source
     */
    public static final ConfigurationSource INSTANCE = new SystemPropertiesSource();

    /**
     * Private constructor to force use of the singleton instance {@link #INSTANCE}
     */
    private SystemPropertiesSource() {

    }

    @Override
    public String get(String key) {
        return PropertiesSource.getFromProperties(System.getProperties(), key);
    }
}
