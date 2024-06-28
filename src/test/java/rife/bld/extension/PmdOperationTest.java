/*
 * Copyright 2023-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package rife.bld.extension;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.rule.RulePriority;
import org.junit.jupiter.api.Test;
import rife.bld.BaseProject;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * PmdOperationTest class
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
class PmdOperationTest {
    static final String CATEGORY_FOO = "category/foo.xml";
    static final Path CODE_STYLE_SAMPLE = Path.of("src/test/resources/java/CodeStyle.java");
    static final String CODE_STYLE_XML = "category/java/codestyle.xml";
    static final String COMMAND_NAME = "pmd";
    static final String DESIGN_XML = "category/java/design.xml";
    static final String DOCUMENTATION_XML = "category/java/documentation.xml";
    static final Path ERROR_PRONE_SAMPLE = Path.of("src/test/resources/java/ErrorProne.java");
    static final String ERROR_PRONE_XML = "category/java/errorprone.xml";
    static final String PERFORMANCE_XML = "category/java/performance.xml";
    static final String SECURITY_XML = "category/java/security.xml";
    static final String TEST = "test";

    PmdOperation newPmdOperation() {
        return new PmdOperation()
                .inputPaths(Path.of("src/main"), Path.of("src/test"))
                .cache(Paths.get("build", COMMAND_NAME, "pmd-cache"))
                .failOnViolation(false)
                .reportFile(Paths.get("build", COMMAND_NAME, "pmd-test-report.txt"));
    }

    @Test
    void testAddInputPath() throws ExitStatusException {
        var project = new BaseProject();
        var pmd = new PmdOperation().fromProject(project);

        assertThat(pmd.inputPaths()).as("default").containsExactly(project.srcMainDirectory().toPath(),
                project.srcTestDirectory().toPath());

        var err = pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME));

        pmd.inputPaths().clear();
        pmd.addInputPaths(project.srcMainDirectory());

        assertThat(pmd.inputPaths()).as("main").containsExactly(project.srcMainDirectory().toPath());

        pmd.inputPaths().clear();
        pmd.addInputPaths(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());

        assertThat(pmd.inputPaths()).as("toPath(main, test)").containsExactly(project.srcMainDirectory().toPath(),
                project.srcTestDirectory().toPath());

        pmd.inputPaths().clear();
        pmd.addInputPaths(List.of(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath()));

        assertThat(pmd.inputPaths()).as("List(main, test)").containsExactly(
                project.srcMainDirectory().toPath(),
                project.srcTestDirectory().toPath());

        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .isGreaterThan(0).isEqualTo(err);
    }

    @Test
    void testAddRuleSets() throws ExitStatusException {
        var pmd = new PmdOperation().fromProject(new BaseProject());

        assertThat(pmd.ruleSets()).containsExactly(PmdOperation.RULE_SET_DEFAULT);

        pmd.addRuleSet(ERROR_PRONE_XML);

        assertThat(pmd.ruleSets()).containsExactly(PmdOperation.RULE_SET_DEFAULT, ERROR_PRONE_XML);

        var err = pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME));

        pmd.ruleSets().clear();

        pmd.addRuleSet(List.of(PmdOperation.RULE_SET_DEFAULT, ERROR_PRONE_XML));

        assertThat(pmd.ruleSets()).as("collection")
                .containsExactly(PmdOperation.RULE_SET_DEFAULT, ERROR_PRONE_XML);

        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .isGreaterThan(0).isEqualTo(err);
    }

    @Test
    void testCache() throws ExitStatusException {
        var cache = Path.of("build/pmd/temp-cache");
        var pmd = newPmdOperation().ruleSets(CODE_STYLE_XML).inputPaths(List.of(CODE_STYLE_SAMPLE)).cache(cache);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
        var f = cache.toFile();
        assertThat(f.exists()).as("file exits").isTrue();
        assertThat(f.delete()).as("delete file").isTrue();
    }

    @Test
    void testEncoding() {
        var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).encoding("UTF-16");
        PMDConfiguration config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.getSourceEncoding()).as("UTF-16").isEqualTo(StandardCharsets.UTF_16);

        pmd = pmd.encoding(StandardCharsets.ISO_8859_1);
        config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.getSourceEncoding()).as("ISO_8859").isEqualTo(StandardCharsets.ISO_8859_1);
    }

    @Test
    void testExecuteNoProject() {
        var pmd = new PmdOperation();
        assertThatCode(pmd::execute).isInstanceOf(ExitStatusException.class);
    }

    @Test
    void testExecute() throws ExitStatusException {
        var pmd = new PmdOperation().fromProject(new BaseProject());

        assertThat(pmd.inputPaths()).containsExactly(Paths.get("src/main").toAbsolutePath(),
                Paths.get("src/test").toAbsolutePath());

        pmd.inputPaths().clear();
        pmd.inputPaths("src/main/java", "src/test/java")
                .ruleSets("config/pmd.xml");

        assertThat(pmd.ruleSets()).containsExactly("config/pmd.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(0);
    }

    @Test
    void testFailOnValidation() {
        var pmd = newPmdOperation().ruleSets(DOCUMENTATION_XML)
                .inputPaths(Path.of("src/test/resources/java/Documentation.java"));
        assertThatCode(() -> pmd.failOnViolation(true).performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .isInstanceOf(ExitStatusException.class);
    }

    @Test
    void testIgnoreFile() throws ExitStatusException {
        var pmd = newPmdOperation()
                .ruleSets(ERROR_PRONE_XML, CODE_STYLE_XML)
                .ignoreFile(Path.of("src/test/resources/ignore.txt"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);

        pmd.inputPaths().clear();
        pmd.inputPaths(ERROR_PRONE_SAMPLE, CODE_STYLE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(0);

        pmd.ruleSets().clear();
        pmd.inputPaths().clear();
        assertThat(pmd.inputPaths(ERROR_PRONE_SAMPLE)
                .ignoreFile(new File("src/test/resources/ignore-single.txt"))
                .performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(0);
    }

    @Test
    void testIncrementalAnalysis() {
        var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).incrementalAnalysis(true);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.isIgnoreIncrementalAnalysis()).isFalse();
    }

    @Test
    void testInputPaths() throws ExitStatusException {
        var pmd = newPmdOperation()
                .ruleSets(PmdOperation.RULE_SET_DEFAULT, CODE_STYLE_XML)
                .inputPaths(ERROR_PRONE_SAMPLE, CODE_STYLE_SAMPLE);
        assertThat(pmd.inputPaths()).contains(ERROR_PRONE_SAMPLE, CODE_STYLE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }

    @Test
    void testJavaBestPractices() throws ExitStatusException {
        var pmd = newPmdOperation().ruleSets("category/java/bestpractices.xml")
                .inputPaths(Path.of("src/test/resources/java/BestPractices.java"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }

    @Test
    void testJavaCodeStyle() throws ExitStatusException {
        var pmd = newPmdOperation().ruleSets(CODE_STYLE_XML).inputPaths(CODE_STYLE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }

    @Test
    void testJavaCodeStyleAndErrorProne() throws ExitStatusException {
        var pmd = newPmdOperation().ruleSets(CODE_STYLE_XML).inputPaths(CODE_STYLE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("code style").isGreaterThan(0);
        pmd = pmd.ruleSets(ERROR_PRONE_XML).inputPaths(ERROR_PRONE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("code style + error prone").isGreaterThan(0);
    }

    @Test
    void testJavaDesign() throws ExitStatusException {
        var pmd = newPmdOperation()
                .ruleSets(DESIGN_XML)
                .inputPaths("src/test/resources/java/Design.java")
                .cache(Path.of("build/pmd/design-cache"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }

    @Test
    void testJavaDocumentation() throws ExitStatusException {
        var pmd = newPmdOperation()
                .ruleSets(DOCUMENTATION_XML)
                .inputPaths(Path.of("src/test/resources/java/Documentation.java"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }

    @Test
    void testJavaErrorProne() throws ExitStatusException {
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).inputPaths(ERROR_PRONE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }

    @Test
    void testJavaMultiThreading() throws ExitStatusException {
        var pmd = newPmdOperation().ruleSets("category/java/multithreading.xml")
                .inputPaths(Path.of("src/test/resources/java/MultiThreading.java"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }

    @Test
    void testJavaPerformance() throws ExitStatusException {
        var pmd = newPmdOperation()
                .ruleSets(PERFORMANCE_XML)
                .inputPaths(Path.of("src/test/resources/java/Performance.java"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }

    @Test
    void testJavaQuickStart() throws ExitStatusException {
        var pmd = newPmdOperation().ruleSets(PmdOperation.RULE_SET_DEFAULT)
                .inputPaths(new File("src/test/resources/java/"));
        assertThat(pmd.ruleSets()).containsExactly(PmdOperation.RULE_SET_DEFAULT);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }

    @Test
    void testJavaSecurity() throws ExitStatusException {
        var pmd = newPmdOperation().ruleSets(SECURITY_XML)
                .inputPaths(Path.of("src/test/resources/java/Security.java"));
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }

    @Test
    void testLanguageVersions() throws ExitStatusException {
        var language = LanguageRegistry.PMD.getLanguageById("java");
        assertThat(language).isNotNull();

        var pmd = newPmdOperation()
                .forceLanguageVersion(language.getLatestVersion())
                .defaultLanguageVersions(language.getVersions())
                .languageVersions(language.getVersion("22"))
                .ruleSets(PmdOperation.RULE_SET_DEFAULT);
        assertThat(pmd.languageVersions()).contains(language.getDefaultVersion());
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);

        assertThat(pmd.defaultLanguageVersions(language.getVersion("17"), language.getVersion("21"))
                .languageVersions(language.getVersions()).performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("17 & 21").isGreaterThan(0);

    }

    @Test
    void testMainOperation() throws ExitStatusException {
        var pmd = newPmdOperation().inputPaths(new File("src/main"))
                .performPmdAnalysis(TEST, newPmdOperation().initConfiguration(COMMAND_NAME));
        assertThat(pmd).isEqualTo(0);
    }

    @Test
    void testPriority() throws ExitStatusException {
        var pmd = newPmdOperation().inputPaths(CODE_STYLE_SAMPLE).minimumPriority(RulePriority.HIGH);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(0);
    }

    @Test
    void testRelativizeRoots() {
        var foo = Path.of("foo/bar");
        var bar = Path.of("bar/foo");
        var baz = Path.of("baz/foz");

        var pmd = newPmdOperation().ruleSets(List.of(CATEGORY_FOO)).relativizeRoots(foo).relativizeRoots(bar.toFile())
                .relativizeRoots(baz.toString()).relativizeRoots(List.of(foo, bar, baz));
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.getRelativizeRoots()).isEqualTo(pmd.relativizeRoots());
        assertThat(config.getRelativizeRoots()).containsExactly(foo, bar, baz, foo, bar, baz);
    }

    @Test
    void testReportFile() throws FileNotFoundException, ExitStatusException {
        var report = new File("build", "pmd-report-file");
        report.deleteOnExit();
        var pmd = newPmdOperation().ruleSets(List.of(ERROR_PRONE_XML, DESIGN_XML)).reportFile(report);
        pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME));
        assertThat(report).exists();

        try (var writer = new PrintWriter(report)) {
            writer.write("");
        }
        assertThat(report).isEmpty();

        pmd.reportFile(report.getAbsolutePath()).performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME));
        assertThat(report).isNotEmpty();
    }

    @Test
    void testReportFormat() throws IOException, ExitStatusException {
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).reportFormat("xml").inputPaths(ERROR_PRONE_SAMPLE);
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
        try (var br = Files.newBufferedReader(pmd.reportFile())) {
            assertThat(br.readLine()).as("xml report").startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        }
    }

    @Test
    void testReportProperties() throws ExitStatusException {
        var pmd = newPmdOperation().ruleSets(CODE_STYLE_XML, ERROR_PRONE_XML)
                .includeLineNumber(true)
                .reportProperties(new Properties());
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }

    @Test
    void testRuleSetsConfigFile() throws ExitStatusException {
        var pmd = newPmdOperation().ruleSets("src/test/resources/pmd.xml")
                .ignoreFile("src/test/resources/ignore-all.txt");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(0);
    }

    @Test
    void testRuleSetsEmpty() throws ExitStatusException {
        var pmd = newPmdOperation().ruleSets("");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isEqualTo(0);
    }

    @Test
    void testShowSuppressed() {
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).showSuppressed(true);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.isShowSuppressedViolations()).isTrue();
    }

    @Test
    void testSuppressedMarker() {
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).suppressedMarker(TEST);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.getSuppressMarker()).isEqualTo(TEST);
    }

    @Test
    void testThreads() {
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).threads(5);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.getThreads()).isEqualTo(5);
    }

    @Test
    void testUri() throws URISyntaxException {
        var uri = new URI("https://example.com");
        var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).uri(uri);
        var config = pmd.initConfiguration(COMMAND_NAME);
        assertThat(config.getUri()).isEqualTo(uri);
    }

    @Test
    void testXml() throws ExitStatusException {
        var pmd = newPmdOperation().addInputPaths("src/test/resources/pmd.xml")
                .ruleSets("src/test/resources/xml/basic.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME))).isGreaterThan(0);
    }
}
