/*
 * Copyright 2023-2026 the original author or authors.
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

import net.sourceforge.pmd.lang.LanguageRegistry;
import net.sourceforge.pmd.lang.rule.RulePriority;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import rife.bld.BaseProject;
import rife.bld.extension.pmd.JavaRules;
import rife.bld.extension.testing.LoggingExtension;
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

import static org.assertj.core.api.Assertions.*;

/**
 * PmdOperation Tests
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@ExtendWith(LoggingExtension.class)
class PmdOperationTests {

    private static final String ANALYSIS_FAILURE = "analysis should fail";
    private static final String ANALYSIS_SUCCESS = "analysis should succeed";
    private static final String BAR = "bar";
    private static final String CATEGORY_FOO = "category/foo.xml";
    private static final String FOO = "foo";
    private static final String PERFORMANCE_XML = "category/java/performance.xml";
    private static final String SECURITY_XML = "category/java/security.xml";
    private static final Path codeStyleSample = Path.of("src/test/resources/java/CodeStyle.java");
    private static final Path errorProneSample = Path.of("src/test/resources/java/ErrorProne.java");
    private static final File fileBar = new File(BAR);
    private static final File fileFoo = new File(FOO);
    @RegisterExtension
    @SuppressWarnings({"unused"})
    private static final LoggingExtension loggingExtension = new LoggingExtension(PmdOperation.class.getName());
    private static final Path pathBar = Path.of(BAR);
    private static final Path pathFoo = Path.of(FOO);

    PmdOperation newPmdOperation() {
        return new PmdOperation()
                .cache("build/pmd-cache")
                .failOnViolation(false)
                .reportFile(Paths.get("build", "pmd-test-report.txt"));
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {

        private static final Path bazPath = Path.of("baz/foz");

        @Test
        void cache() throws ExitStatusException {
            var cache = Path.of("build/pmd/temp-cache");
            var pmd = newPmdOperation()
                    .ruleSets(JavaRules.CODE_STYLE)
                    .inputPaths(List.of(codeStyleSample))
                    .fromProject(new BaseProject())
                    .cache(cache);

            assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).rulesChecked())
                    .isGreaterThan(0);
            var f = cache.toFile();
            assertThat(f.exists()).as("cache should exist").isTrue();
            assertThat(f.delete()).as("cache should be deleted").isTrue();
        }

        @Test
        void collectFilesRecursively() {
            var pmd = newPmdOperation()
                    .ruleSets(JavaRules.ERROR_PRONE)
                    .fromProject(new BaseProject())
                    .collectFilesRecursively(false);

            var config = pmd.initConfiguration();
            assertThat(config.collectFilesRecursively()).isFalse();
        }

        @Test
        void defaultLanguageVersions() throws ExitStatusException {
            var language = LanguageRegistry.PMD.getLanguageById("java");
            assertThat(language).isNotNull();

            var pmd = newPmdOperation()
                    .forceLanguageVersion(language.getLatestVersion())
                    .defaultLanguageVersions(language.getVersions())
                    .ruleSets(JavaRules.QUICK_START)
                    .fromProject(new BaseProject());

            assertThat(pmd.defaultLanguageVersions()).as("should contain default language version")
                    .contains(language.getDefaultVersion());
            assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).processingErrors())
                    .as("should have processing errors").isGreaterThan(0);

            assertThat(pmd.defaultLanguageVersions(language.getVersion("17"), language.getVersion("21"))
                    .excludesStrings("src/test/resources/txt", "src/test/resources/xml")
                    .performPmdAnalysis(pmd.initConfiguration()).configurationErrors())
                    .as("should have no processing errors").isEqualTo(0);
        }

        @Test
        void encoding() {
            var pmd = newPmdOperation()
                    .ruleSets(CATEGORY_FOO)
                    .fromProject(new BaseProject())
                    .encoding("UTF-16");

            var config = pmd.initConfiguration();
            assertThat(config.getSourceEncoding()).as("encoding should be UTF-16")
                    .isEqualTo(StandardCharsets.UTF_16);

            pmd = pmd.encoding(StandardCharsets.ISO_8859_1);
            config = pmd.initConfiguration();
            assertThat(config.getSourceEncoding()).as("encoding should be ISO_8859")
                    .isEqualTo(StandardCharsets.ISO_8859_1);
        }

        @Test
        void incrementalAnalysis() {
            var pmd = newPmdOperation()
                    .ruleSets(CATEGORY_FOO)
                    .fromProject(new BaseProject())
                    .incrementalAnalysis(true);

            var config = pmd.initConfiguration();
            assertThat(config.isIgnoreIncrementalAnalysis()).isFalse();
        }

        @Test
        void prependAuxClasspath() {
            var pmd = newPmdOperation()
                    .ruleSets(CATEGORY_FOO)
                    .prependAuxClasspath(FOO, BAR);

            assertThat(pmd.prependAuxClasspath()).isEqualTo(FOO + File.pathSeparator + BAR);
        }

        @Test
        void priority() throws ExitStatusException {
            var pmd = newPmdOperation()
                    .inputPaths(codeStyleSample)
                    .fromProject(new BaseProject())
                    .minimumPriority(RulePriority.MEDIUM);
            var cfg = pmd.initConfiguration();

            var medium = pmd.performPmdAnalysis(cfg).rulesChecked();
            pmd.minimumPriority(RulePriority.LOW);
            cfg = pmd.initConfiguration();
            var low = pmd.performPmdAnalysis(cfg).rulesChecked();

            assertThat(low).isNotEqualTo(medium);
        }

        @Test
        void relativizeRoots() {
            var pmd = newPmdOperation()
                    .ruleSets(List.of(CATEGORY_FOO))
                    .fromProject(new BaseProject())
                    .relativizeRoots(pathFoo, pathBar, bazPath);

            var config = pmd.initConfiguration();
            assertThat(config.getRelativizeRoots()).as("multiple roots").isEqualTo(pmd.relativizeRoots())
                    .containsExactly(pathFoo, pathBar, bazPath);
        }

        @Test
        void relativizeRootsAsFileArray() {
            var pmd = newPmdOperation()
                    .ruleSets(List.of(CATEGORY_FOO))
                    .fromProject(new BaseProject())
                    .relativizeRoots(pathFoo.toFile(), pathBar.toFile(), bazPath.toFile());

            var config = pmd.initConfiguration();
            assertThat(config.getRelativizeRoots()).as("List(File...)").isEqualTo(pmd.relativizeRoots())
                    .containsExactly(pathFoo, pathBar, bazPath);
        }

        @Test
        void relativizeRootsAsFileList() {
            var pmd = newPmdOperation()
                    .ruleSets(List.of(CATEGORY_FOO))
                    .fromProject(new BaseProject())
                    .relativizeRootsFiles(List.of(pathFoo.toFile(), pathBar.toFile(), bazPath.toFile()));

            var config = pmd.initConfiguration();
            assertThat(config.getRelativizeRoots()).as("List(File...)").isEqualTo(pmd.relativizeRoots())
                    .containsExactly(pathFoo, pathBar, bazPath);
        }

        @Test
        void relativizeRootsAsPathList() {
            var pmd = newPmdOperation()
                    .ruleSets(List.of(CATEGORY_FOO))
                    .fromProject(new BaseProject())
                    .relativizeRoots(List.of(pathFoo, pathBar, bazPath));

            var config = pmd.initConfiguration();
            assertThat(config.getRelativizeRoots()).as("multiple roots").isEqualTo(pmd.relativizeRoots())
                    .containsExactly(pathFoo, pathBar, bazPath);
        }

        @Test
        void relativizeRootsAsStringArray() {
            var pmd = newPmdOperation()
                    .ruleSets(List.of(CATEGORY_FOO))
                    .fromProject(new BaseProject())
                    .relativizeRoots(pathFoo.toString(), pathBar.toString(), bazPath.toString());

            var config = pmd.initConfiguration();
            assertThat(config.getRelativizeRoots()).as("List(String....)").isEqualTo(pmd.relativizeRoots())
                    .containsExactly(pathFoo, pathBar, bazPath);
        }

        @Test
        void relativizeRootsAsStringList() {
            var pmd = newPmdOperation()
                    .ruleSets(List.of(CATEGORY_FOO))
                    .fromProject(new BaseProject())
                    .relativizeRootsStrings(List.of(pathFoo.toString(), pathBar.toString(), bazPath.toString()));

            var config = pmd.initConfiguration();
            assertThat(config.getRelativizeRoots()).as("List(String....)").isEqualTo(pmd.relativizeRoots())
                    .containsExactly(pathFoo, pathBar, bazPath);
        }

        @Test
        void showSuppressed() {
            var pmd = newPmdOperation()
                    .ruleSets(JavaRules.ERROR_PRONE)
                    .fromProject(new BaseProject())
                    .showSuppressed(true);

            var config = pmd.initConfiguration();
            assertThat(config.isShowSuppressedViolations()).isTrue();
        }

        @Test
        void suppressedMarker() {
            var pmd = newPmdOperation()
                    .ruleSets(JavaRules.ERROR_PRONE)
                    .fromProject(new BaseProject())
                    .suppressedMarker(FOO);

            var config = pmd.initConfiguration();
            assertThat(config.getSuppressMarker()).isEqualTo(FOO);
        }

        @Test
        void threads() {
            var pmd = newPmdOperation()
                    .ruleSets(JavaRules.ERROR_PRONE)
                    .fromProject(new BaseProject())
                    .threads(5);

            var config = pmd.initConfiguration();
            assertThat(config.getThreads()).isEqualTo(5);
        }

        @Test
        void uri() throws URISyntaxException {
            var uri = new URI("https://example.com");
            var pmd = newPmdOperation()
                    .ruleSets(JavaRules.ERROR_PRONE)
                    .fromProject(new BaseProject())
                    .uri(uri);

            var config = pmd.initConfiguration();
            assertThat(config.getUri()).isEqualTo(uri);
        }

        @Nested
        @DisplayName("Exclusion Tests")
        class ExclusionTests {

            @Test
            void excludes() {
                var pmd = newPmdOperation()
                        .ruleSets(CATEGORY_FOO)
                        .fromProject(new BaseProject())
                        .excludes(pathFoo, pathBar);
                var config = pmd.initConfiguration();
                assertThat(pmd.excludes()).containsExactly(List.of(pathFoo, pathBar).toArray(new Path[0]));
                assertThat(config.getExcludes()).containsExactly(List.of(pathFoo, pathBar).toArray(new Path[0]));
            }

            @Test
            void excludesAsList() {
                var excludes = List.of(pathFoo, pathBar);
                var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).fromProject(new BaseProject());
                pmd.excludes(excludes);

                var config = pmd.initConfiguration();
                assertThat(config.getExcludes()).containsExactly(excludes.toArray(new Path[0]));
            }

            @Test
            void excludesFiles() {
                var pmd = newPmdOperation()
                        .ruleSets(CATEGORY_FOO)
                        .excludesFiles(fileFoo, fileBar)
                        .fromProject(new BaseProject());

                var config = pmd.initConfiguration();
                assertThat(config.getExcludes()).containsExactly(fileFoo.toPath(), fileBar.toPath());
            }

            @Test
            void excludesFilesAsList() {
                var excludes = List.of(fileFoo, fileBar);
                var pmd = newPmdOperation();
                pmd.ruleSets(CATEGORY_FOO)
                        .excludesFiles(excludes)
                        .fromProject(new BaseProject());

                var config = pmd.initConfiguration();
                assertThat(config.getExcludes())
                        .containsExactly(excludes.stream().map(File::toPath).toArray(Path[]::new));
            }

            @Test
            void excludesStrings() {
                var pmd = newPmdOperation()
                        .ruleSets(CATEGORY_FOO)
                        .fromProject(new BaseProject())
                        .excludesStrings(FOO, BAR);

                var config = pmd.initConfiguration();
                assertThat(pmd.excludes()).containsExactly(pathFoo, pathBar);
                assertThat(config.getExcludes()).containsExactly(pathFoo, pathBar);
            }

            @Test
            void excludesStringsAsList() {
                var excludes = List.of(FOO, BAR);
                var pmd = newPmdOperation();
                pmd.ruleSets(CATEGORY_FOO)
                        .fromProject(new BaseProject())
                        .excludesStrings(excludes);

                var config = pmd.initConfiguration();
                assertThat(config.getExcludes())
                        .containsExactly(excludes.stream().map(Paths::get).toArray(Path[]::new));
            }
        }

        @Nested
        @DisplayName("Failure Tests")
        class FailureTests {

            @Test
            void failOnError() {
                var pmd = newPmdOperation()
                        .ruleSets("src/test/resources/xml/old.xml")
                        .inputPaths(Path.of("src/test/resources/java/Documentation.java"));

                assertThatCode(() -> pmd.performPmdAnalysis(pmd.initConfiguration()))
                        .isInstanceOf(ExitStatusException.class);
                assertThatCode(() -> pmd.failOnError(false).performPmdAnalysis(
                        pmd.initConfiguration())).doesNotThrowAnyException();
            }

            @Test
            void failOnValidation() {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.DOCUMENTATION)
                        .inputPaths(Path.of("src/test/resources/java/Documentation.java"));

                assertThatCode(() -> pmd.failOnViolation(true).performPmdAnalysis(
                        pmd.initConfiguration())).isInstanceOf(ExitStatusException.class);
                assertThatCode(() -> pmd.failOnViolation(false).performPmdAnalysis(
                        pmd.initConfiguration())).doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("Ignore File Tests")
        class IgnoreFileTests {

            @Test
            void ignoreFile() throws ExitStatusException {
                var pmd = newPmdOperation();
                pmd.ruleSets(JavaRules.ERROR_PRONE, JavaRules.CODE_STYLE)
                        .ignoreFile(Path.of("src/test/resources/txt/ignore.txt"));
                pmd.inputPaths(errorProneSample, codeStyleSample)
                        .fromProject(new BaseProject());

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as("%s for error prone and code style", ANALYSIS_SUCCESS).isEqualTo(0);
            }

            @Test
            void ignoreSingleFile() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.ERROR_PRONE)
                        .ignoreFile(new File("src/test/resources/txt/ignore-single.txt"));
                pmd.inputPaths(errorProneSample)
                        .fromProject(new BaseProject());

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as("%s for error prone sample", ANALYSIS_SUCCESS).isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("Input Paths Tests")
        class InputPathsTests {

            final BaseProject project = new BaseProject();

            @Test
            void inputPathsAsArray() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.QUICK_START, JavaRules.CODE_STYLE);
                pmd.inputPaths(errorProneSample, codeStyleSample)
                        .fromProject(new BaseProject());

                assertThat(pmd.inputPaths()).containsExactly(errorProneSample, codeStyleSample);
                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void inputPathsAsFileArray() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.QUICK_START, JavaRules.CODE_STYLE);
                pmd.inputPaths(errorProneSample.toFile(), codeStyleSample.toFile())
                        .fromProject(new BaseProject());

                assertThat(pmd.inputPaths()).containsExactly(errorProneSample, codeStyleSample);
                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void inputPathsAsFileList() {
                var pmd = new PmdOperation()
                        .inputPathsFiles(List.of(project.srcMainDirectory(), project.srcTestDirectory()))
                        .fromProject(new BaseProject());

                assertThat(pmd.inputPaths()).as("List(File...)")
                        .containsExactly(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());
            }

            @Test
            void inputPathsAsList() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.QUICK_START, JavaRules.CODE_STYLE);
                pmd.inputPathsFiles(List.of(errorProneSample.toFile(), codeStyleSample.toFile()))
                        .fromProject(new BaseProject());

                assertThat(pmd.inputPaths()).containsExactly(errorProneSample, codeStyleSample);
                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void inputPathsAsPathArray() {
                var pmd = new PmdOperation()
                        .inputPaths(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath())
                        .fromProject(new BaseProject());

                assertThat(pmd.inputPaths()).as("Path...")
                        .containsExactly(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());
            }

            @Test
            void inputPathsAsPathList() {
                var pmd = new PmdOperation()
                        .inputPaths(List.of(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath()))
                        .fromProject(new BaseProject());
                assertThat(pmd.inputPaths()).as("List(Path)")
                        .containsExactly(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());
            }


            @Test
            void inputPathsAsStringArray() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.QUICK_START, JavaRules.CODE_STYLE);
                pmd.inputPaths(errorProneSample.toString(), codeStyleSample.toString())
                        .fromProject(new BaseProject());

                assertThat(pmd.inputPaths()).containsExactly(errorProneSample, codeStyleSample);
                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void inputPathsAsStringList() throws ExitStatusException {
                var pmd = newPmdOperation().ruleSets(JavaRules.QUICK_START, JavaRules.CODE_STYLE);
                pmd = pmd.inputPathsStrings(List.of(errorProneSample.toString(), codeStyleSample.toString()))
                        .fromProject(new BaseProject());
                assertThat(pmd.inputPaths()).containsExactly(errorProneSample, codeStyleSample);
                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void inputPathsAsXml() throws ExitStatusException {
                var pmd = newPmdOperation();
                pmd.inputPaths("src/test/resources/xml/pmd.xml")
                        .ruleSets("src/test/resources/xml/basic.xml")
                        .fromProject(new BaseProject());

                assertThat(pmd.inputPaths()).containsExactly(Path.of("src/test/resources/xml/pmd.xml"));

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).configurationErrors())
                        .isEqualTo(0);
            }

            @Test
            void inputPathsMainOperation() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .inputPaths(new File("src/main"))
                        .fromProject(new BaseProject());
                var cfg = pmd.initConfiguration();

                assertThat(pmd.performPmdAnalysis(cfg).violations()).as(ANALYSIS_SUCCESS).isEqualTo(0);
            }

            @Test
            void inputPathsWithDefaults() {
                var pmd = new PmdOperation().fromProject(project);
                assertThat(pmd.inputPaths()).as("default input paths")
                        .containsExactly(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());
            }
        }

        @Nested
        @DisplayName("Reporting Tests")
        @SuppressWarnings("PMD.RelianceOnDefaultCharset")
        class ReportingTests {

            @Test
            void reportFile() throws FileNotFoundException, ExitStatusException {
                var report = new File("build", "pmd-report-file");
                report.deleteOnExit();
                var pmd = newPmdOperation()
                        .ruleSetsRules(List.of(JavaRules.ERROR_PRONE, JavaRules.DESIGN))
                        .reportFile(report)
                        .fromProject(new BaseProject());

                pmd.performPmdAnalysis(pmd.initConfiguration());
                assertThat(report).as("report should exist").exists();

                try (var writer = new PrintWriter(report)) {
                    writer.write("");
                }
                assertThat(report).as("report should not be empty").isEmpty();

                pmd.reportFile(report.getAbsolutePath()).performPmdAnalysis(pmd.initConfiguration());
                assertThat(report).as("report should not be empty").isNotEmpty();
            }

            @Test
            void reportFormat() throws IOException, ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.ERROR_PRONE)
                        .reportFormat("xml")
                        .inputPaths(errorProneSample);

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
                try (var br = Files.newBufferedReader(pmd.reportFile())) {
                    assertThat(br.readLine()).as("XML report for %s", pmd.reportFile().toString())
                            .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                }
            }

            @Test
            void reportProperties() throws ExitStatusException {
                var p = new Properties();
                p.setProperty(FOO, BAR);
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.CODE_STYLE, JavaRules.ERROR_PRONE)
                        .includeLineNumber(true)
                        .reportProperties(p)
                        .fromProject(new BaseProject());

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).processingErrors())
                        .isEqualTo(0);
            }

            @Test
            void reportPropertiesIsNull() {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.CODE_STYLE, JavaRules.ERROR_PRONE)
                        .includeLineNumber(true);

                //noinspection DataFlowIssue
                assertThatThrownBy(() -> pmd.reportProperties(null)).isInstanceOf(NullPointerException.class);
            }
        }

        @Nested
        @DisplayName("RuleSets Tests")
        class RuleSetsTests {

            @Test
            void javaBestPractices() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.BEST_PRACTICES)
                        .inputPaths(Path.of("src/test/resources/java/BestPractices.java"));

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaCodeStyle() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.CODE_STYLE)
                        .inputPaths(codeStyleSample);

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaCodeStyleAndErrorProne() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.CODE_STYLE)
                        .inputPaths(codeStyleSample);

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as("analysis with code style").isGreaterThan(0);

                pmd = pmd.ruleSets(JavaRules.ERROR_PRONE).inputPaths(errorProneSample);
                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as("analysis with code style and error prone").isGreaterThan(0);
            }

            @Test
            void javaDesign() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.DESIGN)
                        .inputPaths("src/test/resources/java/Design.java")
                        .cache(new File("build/pmd/design-cache"));

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).configurationErrors())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaDocumentation() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.DOCUMENTATION)
                        .inputPaths(Path.of("src/test/resources/java/Documentation.java"));

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaErrorProne() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.ERROR_PRONE)
                        .inputPaths(errorProneSample);

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaMultiThreading() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets("category/java/multithreading.xml")
                        .inputPaths(Path.of("src/test/resources/java/MultiThreading.java"));

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaPerformance() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(PERFORMANCE_XML)
                        .inputPaths(Path.of("src/test/resources/java/Performance.java"));

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaQuickStart() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(JavaRules.QUICK_START)
                        .inputPaths(new File("src/test/resources/java/"));

                assertThat(pmd.ruleSets()).containsExactly(JavaRules.QUICK_START.getCategory());
                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaSecurity() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(SECURITY_XML)
                        .inputPaths(Path.of("src/test/resources/java/Security.java"));

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void ruleSetsConfigFile() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets("src/test/resources/xml/pmd.xml")
                        .fromProject(new BaseProject())
                        .ignoreFile("src/test/resources/txt/ignore-all.txt");

                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).hasNoErrors()).isTrue();
            }

            @Test
            void ruleSetsEmpty() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .fromProject(new BaseProject())
                        .ruleSets("")
                        .failOnError(false);
                assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).errors())
                        .isEqualTo(1);
            }

            @Test
            void ruleSetsWithDefault() {
                var pmd = new PmdOperation().fromProject(new BaseProject());

                assertThat(pmd.ruleSets()).containsExactly(JavaRules.QUICK_START.getCategory());
            }

            @Test
            void ruleSetsWithErrorProne() {
                var pmd = new PmdOperation().ruleSets(JavaRules.ERROR_PRONE);
                assertThat(pmd.ruleSets()).containsExactly(JavaRules.ERROR_PRONE.getCategory());
            }

            @Test
            void ruleSetsWithList() {
                var pmd = new PmdOperation()
                        .ruleSetsRules(List.of(JavaRules.QUICK_START, JavaRules.ERROR_PRONE));

                assertThat(pmd.ruleSets()).as("rulesets as collection")
                        .containsExactly(JavaRules.QUICK_START.getCategory(), JavaRules.ERROR_PRONE.getCategory());
            }
        }
    }

    @Nested
    @DisplayName("Execution Tests")
    class ExecutionTests {

        @Test
        void execute() throws ExitStatusException {
            var pmd = new PmdOperation();
            pmd.inputPaths("src/main/java", "src/test/java")
                    .ruleSets("config/pmd.xml")
                    .fromProject(new BaseProject());

            assertThat(pmd.ruleSets()).containsExactly("config/pmd.xml");
            assertThat(pmd.performPmdAnalysis(pmd.initConfiguration()).violations())
                    .as(ANALYSIS_SUCCESS).isEqualTo(0);
        }

        @Test
        void executeDefaultInputPaths() {
            var pmd = new PmdOperation().fromProject(new BaseProject());

            assertThat(pmd.inputPaths()).containsExactly(Paths.get("src/main").toAbsolutePath(),
                    Paths.get("src/test").toAbsolutePath());
        }

        @Test
        void executeNoInputPaths() {
            var pmd = new PmdOperation();
            assertThatCode(pmd::execute).isInstanceOf(IllegalArgumentException.class);
        }
    }
}
