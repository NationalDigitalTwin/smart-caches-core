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
package io.telicent.smart.cache.server.jaxrs.applications;

import io.telicent.smart.cache.server.jaxrs.filters.RequestIdFilter;
import io.telicent.smart.cache.server.jaxrs.init.TestInit;
import io.telicent.smart.cache.server.jaxrs.resources.StatusHealthResource;
import io.telicent.smart.cache.server.jaxrs.utils.RandomPortProvider;
import jakarta.ws.rs.client.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.testng.Assert;
import org.testng.annotations.*;

public class TestCors extends AbstractAppEntrypoint {
    private static final RandomPortProvider PORT = new RandomPortProvider(15888);
    public static final String EXAMPLE_ORIGIN = "https://example.org";
    public static final String BAD_ORIGIN = "https://bad-origin";

    private final Client client = ClientBuilder.newClient();

    @BeforeClass
    public void setupServer() {
        allowRestrictedHeaders();
        this.run(false);
    }

    @AfterClass
    public void teardownServer() {
        this.server.shutdownNow();
        this.client.close();
    }

    @BeforeMethod
    public void setup() {
        TestInit.reset();
        StatusHealthResource.reset();
    }

    @Override
    protected ServerBuilder buildServer() {
        //@formatter:off
        return ServerBuilder.create()
                            .application(MockHealthApplication.class)
                            // Use a different port for each test just in case one test is slow to teardown the server
                            .port(PORT.newPort())
                            // Enable CORS with various additional headers and origins
                            .withCors(new CorsConfigurationBuilder(false))
                            .withCors(c -> c.preflightMaxAge(60)
                                            .allowCredentials(true)
                                            .withDefaults()
                                            .chainPreflight(true)
                                            .addAllowedHeaders("CUSTOM", "X-Test")
                                            .addExposedHeaders("X-Test")
                                            .addAllowedOrigins(EXAMPLE_ORIGIN)
                                            .addAllowedTimingOrigins(EXAMPLE_ORIGIN)
                            )
                            .displayName("CORS Tests");
        //@formatter:on
    }

    private WebTarget forServer(Server server, String path) {
        return this.client.target(server.getBaseUri()).path(path);
    }

    /**
     * Sets the necessary System Property that allows setting the Origin header
     */
    private static void allowRestrictedHeaders() {
        // Needed to allow setting the Origin header so CORS requests can be made
        System.setProperty("sun.net.http.allowRestrictedHeaders", "true");
    }

    @Test
    public void test_cors_preflight_01() {
        // A CORS pre-flight request
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN)
                                              .header("Origin", EXAMPLE_ORIGIN)
                                              .header("Access-Control-Request-Method", "GET");

        try (Response response = invocation.options()) {
            Assert.assertNotNull(response.getHeaderString("Access-Control-Allow-Origin"));
            Assert.assertNotNull(response.getHeaderString("Access-Control-Allow-Methods"));
            Assert.assertNotNull(response.getHeaderString("Access-Control-Allow-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Expose-Headers"));
            Assert.assertNotNull(response.getHeaderString("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    public void test_cors_preflight_02() {
        // A CORS pre-flight request
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN)
                                              .header("Origin", BAD_ORIGIN)
                                              .header("Access-Control-Request-Method", "GET");

        try (Response response = invocation.options()) {
            // Not a permitted Origin so not CORS headers are sent in the response thus a browser would reject the request
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Origin"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Methods"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Expose-Headers"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    public void test_cors_simple_01() {
        // A simple request
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN).header("Origin", EXAMPLE_ORIGIN);

        try (Response response = invocation.get()) {
            Assert.assertNotNull(response.getHeaderString("Access-Control-Allow-Origin"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Methods"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Headers"));
            Assert.assertNotNull(response.getHeaderString("Access-Control-Expose-Headers"));
            Assert.assertNotNull(response.getHeaderString("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    public void test_cors_non_simple_01() {
        // A non-simple request
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN)
                                              .header("Origin", EXAMPLE_ORIGIN)
                                              .header(RequestIdFilter.REQUEST_ID, "cors-03");

        try (Response response = invocation.get()) {
            Assert.assertNotNull(response.getHeaderString("Access-Control-Allow-Origin"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Methods"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Headers"));
            Assert.assertNotNull(response.getHeaderString("Access-Control-Expose-Headers"));
            Assert.assertNotNull(response.getHeaderString("Access-Control-Allow-Credentials"));
        }
    }

    @Test
    public void test_cors_non_simple_02() {
        // A non-simple request
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.TEXT_PLAIN)
                                              .header("Origin", EXAMPLE_ORIGIN)
                                              .header(RequestIdFilter.REQUEST_ID, "cors-03");

        // PUT is not a simple method for CORS
        try (Response response = invocation.put(Entity.text("test"))) {
            Assert.assertNotNull(response.getHeaderString("Access-Control-Allow-Origin"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Methods"));
            Assert.assertNull(response.getHeaderString("Access-Control-Allow-Headers"));
            Assert.assertNotNull(response.getHeaderString("Access-Control-Expose-Headers"));
            Assert.assertNotNull(response.getHeaderString("Access-Control-Allow-Credentials"));
        }
    }
}
