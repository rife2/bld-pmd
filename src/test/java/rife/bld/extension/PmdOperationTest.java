/*
 *  Copyright 2023 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package rife.bld.extension;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * PmdOperationTest class
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
class PmdOperationTest {
    static final String CODE_STYLE = "category/java/codestyle.xml";
    static final Path CODE_STYLE_SAMPLE = Path.of("src/test/resources/java/CodeStyle.java");
    static final int CODING_STYLE_ERRORS = 16;
    static final String COMMAND_NAME = "pmd";
    static final String ERROR_PRONE = "category/java/errorprone.xml";
    static final int ERROR_PRONE_ERRORS = 8;
    static final Path ERROR_PRONE_SAMPLE = Path.of("src/test/resources/java/ErrorProne.java");
    static final String TEST = "test";
    static final String CATEGORY_FOO = "category/foo.xml";

    PmdOperation newPmdOperation() {
        final PmdOperation pmdOperation = new PmdOperation();
        pmdOperation.inputPaths(Path.of("src/main"), Path.of("src/test"));
        pmdOperation.reportFile = Paths.get("build", COMMAND_NAME, "pmd-test-report.txt");
        pmdOperation.cache = Paths.get("build", COMMAND_NAME, "pmd-cache");
        pmdOperation.failOnViolation = false;
        return pmdOperation;
    }

    @Test
    void testAddInputPaths() {
        var pmd = newPmdOperation().ruleSets(PmdOperation.RULE_SET_DEFAULT, CODE_STYLE).inputPaths(ERROR_PRONE_SAMPLE)
                .addInputPath(CODE_STYLE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(42);
    }

    @Test
    void testCache() {
        var cache = Path.of("build/pmd/temp-cache");
        var pmd = newPmdOperation().ruleSets(CODE_STYLE).inputPaths(CODE_STYLE_SAMPLE).cache(cache);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(CODING_STYLE_ERRORS);
        var f = cache.toFile();
        assertThat(f.exists()).as("file exits").isTrue();
        assertThat(f.delete()).as("delete file").isTrue();
    }

    @Test
    void testDebug() {
        var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).debug(true);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.isDebug()).isTrue();
    }

    @Test
    void testEncoding() {
        var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).encoding("UTF-16");
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.getSourceEncoding()).isEqualTo(StandardCharsets.UTF_16);
    }

    @Test
    void testFailOnValidation() {
        var pmd = newPmdOperation().ruleSets("category/java/documentation.xml")
                .inputPaths(Path.of("src/test/resources/java/Documentation.java"));
        assertThatCode(() -> pmd.failOnViolation(true).performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))
        ).isInstanceOf(RuntimeException.class).hasMessageContaining('[' + TEST + ']');
    }

    @Test
    void testIgnoreFile() {
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE, CODE_STYLE).inputPaths(ERROR_PRONE_SAMPLE, CODE_STYLE_SAMPLE)
                .ignoreFile(Path.of("src/test/resources/ignore.txt"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(0);
    }

    @Test
    void testIncrementalAnalysis() {
        var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).incrementalAnalysis(true);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.isIgnoreIncrementalAnalysis()).isFalse();
    }

    @Test
    void testJavaBestPractices() {
        var pmd = newPmdOperation().ruleSets("category/java/bestpractices.xml")
                .inputPaths(Path.of("src/test/resources/java/BestPractices.java"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(11);
    }

    @Test
    void testJavaCodeStyle() {
        var pmd = newPmdOperation().ruleSets(CODE_STYLE).inputPaths(CODE_STYLE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(CODING_STYLE_ERRORS);
    }

    @Test
    void testJavaCodeStyleAndErrorProne() {
        var pmd = newPmdOperation().ruleSets(CODE_STYLE).inputPaths(CODE_STYLE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("code style").isEqualTo(CODING_STYLE_ERRORS);
        pmd = pmd.addRuleSet(ERROR_PRONE).addInputPath(ERROR_PRONE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("code style + error prone").isEqualTo(40);
    }

    @Test
    void testJavaDesign() {
        var pmd = newPmdOperation().ruleSets("category/java/design.xml")
                .inputPaths(Path.of("src/test/resources/java/Design.java"))
                .cache(Path.of("build/pmd/design-cache"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(4);
    }

    @Test
    void testJavaDocumentation() {
        var pmd = newPmdOperation().ruleSets("category/java/documentation.xml")
                .inputPaths(Path.of("src/test/resources/java/Documentation.java"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(4);
    }

    @Test
    void testJavaErrorProne() {
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE).inputPaths(ERROR_PRONE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(ERROR_PRONE_ERRORS);
    }

    @Test
    void testJavaMultiThreading() {
        var pmd = newPmdOperation().ruleSets("category/java/multithreading.xml")
                .inputPaths(Path.of("src/test/resources/java/MultiThreading.java"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(4);
    }

    @Test
    void testJavaPerformance() {
        var pmd = newPmdOperation().ruleSets("category/java/performance.xml")
                .inputPaths(Path.of("src/test/resources/java/Performance.java"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(9);
    }

    @Test
    void testJavaQuickStart() {
        var pmd = newPmdOperation().ruleSets("rulesets/java/quickstart.xml")
                .inputPaths(Path.of("src/test/resources/java"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(35);
    }

    @Test
    void testJavaSecurity() {
        var pmd = newPmdOperation().ruleSets("category/java/security.xml")
                .inputPaths(Path.of("src/test/resources/java/Security.java"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(1);
    }

    @Test
    void testMainOperation() {
        var pmd = newPmdOperation().inputPaths(Path.of("src/main"))
                .performPmdAnalysis(TEST, newPmdOperation().initConfiguration(COMMAND_NAME));
        assertThat(pmd).isEqualTo(0);
    }

    @Test
    void testRelativizeRoots() {
        var foo = Path.of("foo/bar");
        var bar = Path.of("bar/foo");

        var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).relativizeRoots(foo).addRelativizeRoot(bar);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.getRelativizeRoots()).contains(foo).contains(bar);
    }

    @Test
    void testReportFormat() throws IOException {
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE).reportFormat("xml").inputPaths(ERROR_PRONE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(ERROR_PRONE_ERRORS);
        try (var br = Files.newBufferedReader(pmd.reportFile)) {
            assertThat(br.readLine()).as("xml report").startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        }
    }

    @Test
    void testRuleSetsConfigFile() {
        var pmd = newPmdOperation().ruleSets("src/test/resources/pmd.xml")
                .ignoreFile(Path.of("src/test/resources/ignore-all.txt"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(0);
    }

    @Test
    void testRuleSetsEmpty() {
        var pmd = newPmdOperation().ruleSets("");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(0);
    }

    @Test
    void testShowSuppressed() {
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE).showSuppressed(true);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.isShowSuppressedViolations()).isTrue();
    }

    @Test
    void testSuppressedMarker() {
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE).suppressedMarker(TEST);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.getSuppressMarker()).isEqualTo(TEST);
    }

    @Test
    void testThreads() {
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE).threads(5);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.getThreads()).isEqualTo(5);
    }

    @Test
    void testUri() throws URISyntaxException {
        var uri = new URI("https://example.com");
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE).uri(uri);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.getUri()).isEqualTo(uri);
    }

    @Test
    void testXml() {
        var pmd = newPmdOperation().inputPaths(Path.of("src/test/resources/pmd.xml"))
                .ruleSets("src/test/resources/xml/basic.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(0);
    }
}
