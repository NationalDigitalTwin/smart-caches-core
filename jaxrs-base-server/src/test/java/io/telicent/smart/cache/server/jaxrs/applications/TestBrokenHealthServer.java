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

import io.telicent.smart.cache.server.jaxrs.init.TestInit;
import io.telicent.smart.cache.server.jaxrs.model.HealthStatus;
import io.telicent.smart.cache.server.jaxrs.resources.BrokenStatusHealthResource;
import io.telicent.smart.cache.server.jaxrs.resources.StatusHealthResource;
import io.telicent.smart.cache.server.jaxrs.utils.RandomPortProvider;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import jakarta.ws.rs.client.Invocation;
import jakarta.ws.rs.client.WebTarget;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.testng.Assert;
import org.testng.annotations.*;

public class TestBrokenHealthServer extends AbstractAppEntrypoint {

    private static final RandomPortProvider PORT = new RandomPortProvider(15555);

    private final Client client = ClientBuilder.newClient();

    @BeforeClass
    public void setupServer() {
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
        BrokenStatusHealthResource.reset();
    }

    @Override
    protected ServerBuilder buildServer() {
        return ServerBuilder.create()
                            .application(MockBrokenHealthApplication.class)
                            // Use a different port for each test just in case one test is slow to teardown the server
                            .port(PORT.newPort())
                            .displayName("Broken Health Status Tests");
    }

    private WebTarget forServer(Server server, String path) {
        return this.client.target(server.getBaseUri()).path(path);
    }

    @Test
    public void test_healthy_01() {
        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
        try (Response response = invocation.get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
            HealthStatus status = response.readEntity(HealthStatus.class);

            Assert.assertFalse(status.isHealthy());
            Assert.assertFalse(status.reasons().isEmpty());
            Assert.assertEquals(status.reasons().size(), 2);
            Assert.assertEquals(status.reasons().get(0), StatusHealthResource.UNEXPECTED_ERROR_REASON);
            Assert.assertEquals(status.reasons().get(1), BrokenStatusHealthResource.DEFAULT_ERROR_MESSAGE);
        }
    }

    @Test
    public void test_healthy_02() {
        BrokenStatusHealthResource.ERROR_MESSAGE = "Bar";

        WebTarget target = forServer(this.server, "/healthz");
        Invocation.Builder invocation = target.request(MediaType.APPLICATION_JSON);
        try (Response response = invocation.get()) {
            Assert.assertEquals(response.getStatus(), Response.Status.SERVICE_UNAVAILABLE.getStatusCode());
            HealthStatus status = response.readEntity(HealthStatus.class);

            Assert.assertFalse(status.isHealthy());
            Assert.assertFalse(status.reasons().isEmpty());
            Assert.assertEquals(status.reasons().size(), 2);
            Assert.assertEquals(status.reasons().get(0), StatusHealthResource.UNEXPECTED_ERROR_REASON);
            Assert.assertEquals(status.reasons().get(1), "Bar");
        }
    }
}
