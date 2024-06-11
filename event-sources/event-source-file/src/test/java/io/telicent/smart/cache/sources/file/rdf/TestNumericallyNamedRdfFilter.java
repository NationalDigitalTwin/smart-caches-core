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
package io.telicent.smart.cache.sources.file.rdf;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class TestNumericallyNamedRdfFilter {

    private final NumericallyNamedRdfFilter rdfFilter = new NumericallyNamedRdfFilter();

    @Test
    public void numerically_named_rdf_filter_01() throws IOException {
        File test = Files.createTempFile("test", ".ttl").toFile();
        test.deleteOnExit();
        Assert.assertTrue(rdfFilter.accept(test));
    }

    @Test
    public void numerically_named_rdf_filter_02() throws IOException {
        File test = Files.createTempFile("test", ".rq").toFile();
        test.deleteOnExit();
        Assert.assertFalse(rdfFilter.accept(test));
    }

    @Test
    public void numerically_named_rdf_filter_03() throws IOException {
        File test = Files.createTempFile("test", ".txt").toFile();
        Assert.assertFalse(rdfFilter.accept(test));
    }

    @Test
    public void numerically_named_rdf_filter_04() {
        File test = new File(new File("test-data", "bad-dir"), "test");
        Assert.assertFalse(rdfFilter.accept(test));
    }

    @Test
    public void numerically_named_rdf_filter_05() {
        File test = new File("test-data");
        Assert.assertFalse(rdfFilter.accept(test));
    }

    @Test
    public void numerically_named_rdf_filter_06() {
        File test = new File("test-data", "no-key.yaml");
        Assert.assertFalse(rdfFilter.accept(test));
    }
}
