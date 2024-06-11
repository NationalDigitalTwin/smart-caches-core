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
package io.telicent.smart.cache.server.jaxrs.resources;

import io.telicent.smart.cache.observability.LibraryVersion;
import io.telicent.smart.cache.server.jaxrs.model.VersionInfo;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

/**
 * Resource that provides a {@code /version-info} endpoint
 */
@Path("/")
public class VersionInfoResource {

    /**
     * Returns version information based on the library versions that have been loaded during the lifetime of the
     * server.
     * <p>
     * You can use {@link io.telicent.smart.cache.server.jaxrs.applications.ServerBuilder#withVersionInfo(String)} to
     * force a given libraries version information (if any) to be included in this endpoints output.
     * </p>
     *
     * @return Version information response
     */
    @GET
    @Produces({ MediaType.APPLICATION_JSON })
    @Path("version-info")
    public Response getVersionInfo() {
        VersionInfo info = new VersionInfo();
        for (String library : LibraryVersion.cachedLibraries()) {
            info.addLibraryVersion(library, LibraryVersion.getProperties(library));
        }

        return Response.ok().entity(info).build();
    }
}
