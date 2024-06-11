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

/**
 * A test complex offset type
 */
public class ComplexOffset extends HashMap<String, Object> {

    /**
     * Creates a complex offset
     *
     * @param topic     Topic
     * @param partition Partition
     * @param offset    Offset
     */
    public ComplexOffset(String topic, long partition, long offset) {
        super();
        this.put("topic", topic);
        this.put("partition", partition);
        this.put("offset", offset);
    }
}
