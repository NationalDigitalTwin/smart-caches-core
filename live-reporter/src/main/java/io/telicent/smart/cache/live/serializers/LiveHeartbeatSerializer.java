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
package io.telicent.smart.cache.live.serializers;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import io.telicent.smart.cache.live.model.LiveHeartbeat;
import io.telicent.smart.cache.sources.kafka.serializers.AbstractJacksonSerializer;

/**
 * A Kafka serializer that handles {@link LiveHeartbeat} values
 */
public class LiveHeartbeatSerializer extends AbstractJacksonSerializer<LiveHeartbeat> {

    /**
     * Creates a new {@link LiveHeartbeat} serializer
     */
    public LiveHeartbeatSerializer() {
        super();
        this.mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        this.mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));
    }
}
