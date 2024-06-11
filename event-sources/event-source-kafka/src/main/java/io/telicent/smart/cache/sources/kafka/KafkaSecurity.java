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
package io.telicent.smart.cache.sources.kafka;

/**
 * Providers helper functions related to Kafka security, in particular the ability to generate JAAS configuration values
 * for use in authenticating to a secured Kafka cluster
 */
public class KafkaSecurity {

    /**
     * Private constructor prevents instantiation
     */
    private KafkaSecurity() {

    }

    /**
     * Generates the Kafka {@link org.apache.kafka.common.config.SaslConfigs#SASL_JAAS_CONFIG} value for making a
     * plaintext login to a Kafka cluster
     *
     * @param username Username
     * @param password Password
     * @return SASL JAAS Config for plaintext login with the provided credentials
     */
    public static String plainLogin(String username, String password) {
        String builder = "org.apache.kafka.common.security.plain.PlainLoginModule required username=\"" +
                username +
                "\" password=\"" +
                password +
                "\";";
        return builder;
    }

    /**
     * Generates the Kafka {@link org.apache.kafka.common.config.SaslConfigs#SASL_JAAS_CONFIG} value for making an
     * encrypted login to a Kafka cluster
     *
     * @param username Username
     * @param password Password
     * @return SASL JAAS Config for encrypted login with the provided credentials
     */
    public static String scramLogin(String username, String password) {
        String builder = "org.apache.kafka.common.security.scram.ScramLoginModule required username=\"" +
                username +
                "\" password=\"" +
                password +
                "\";";
        return builder;
    }
}
