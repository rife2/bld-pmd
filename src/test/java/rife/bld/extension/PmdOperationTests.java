/*
 * Copyright 2023-2025 the original author or authors.
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import rife.bld.BaseProject;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * PmdOperationTests class
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
    private static final Path CODE_STYLE_SAMPLE = Path.of("src/test/resources/java/CodeStyle.java");
    private static final String CODE_STYLE_XML = "category/java/codestyle.xml";
    private static final String COMMAND_NAME = "pmd";
    private static final String DESIGN_XML = "category/java/design.xml";
    private static final String DOCUMENTATION_XML = "category/java/documentation.xml";
    private static final Path ERROR_PRONE_SAMPLE = Path.of("src/test/resources/java/ErrorProne.java");
    private static final String ERROR_PRONE_XML = "category/java/errorprone.xml";
    private static final File FILE_BAR = new File(BAR);
    private static final String FOO = "foo";
    private static final File FILE_FOO = new File(FOO);

    @RegisterExtension
    @SuppressWarnings({"unused"})
    private static final LoggingExtension LOGGING_EXTENSION = new LoggingExtension(PmdOperation.class.getName());

    private static final Path PATH_BAR = Path.of(BAR);
    private static final Path PATH_FOO = Path.of(FOO);
    private static final String PERFORMANCE_XML = "category/java/performance.xml";
    private static final String SECURITY_XML = "category/java/security.xml";
    private static final String TEST = "test";

    PmdOperation newPmdOperation() {
        return new PmdOperation()
                .inputPaths(Path.of("src/main"), Path.of("src/test"))
                .cache("build/" + COMMAND_NAME + "/pmd-cache")
                .failOnViolation(false)
                .reportFile(Paths.get("build", COMMAND_NAME, "pmd-test-report.txt"));
    }

    @Nested
    @DisplayName("Configuration Tests")
    class ConfigurationTests {
        @Test
        void cache() throws ExitStatusException {
            var cache = Path.of("build/pmd/temp-cache");
            var pmd = newPmdOperation()
                    .ruleSets(CODE_STYLE_XML)
                    .inputPaths(List.of(CODE_STYLE_SAMPLE))
                    .cache(cache);

            assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).rulesChecked())
                    .isGreaterThan(0);
            var f = cache.toFile();
            assertThat(f.exists()).as("cache should exist").isTrue();
            assertThat(f.delete()).as("cache should be deleted").isTrue();
        }

        @Test
        void collectFilesRecursively() {
            var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).collectFilesRecursively(false);

            var config = pmd.initConfiguration(COMMAND_NAME);
            assertThat(config.collectFilesRecursively()).isFalse();
        }

        @Test
        void encoding() {
            var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).encoding("UTF-16");

            PMDConfiguration config = pmd.initConfiguration(COMMAND_NAME);
            assertThat(config.getSourceEncoding()).as("encoding should be UTF-16")
                    .isEqualTo(StandardCharsets.UTF_16);

            pmd = pmd.encoding(StandardCharsets.ISO_8859_1);
            config = pmd.initConfiguration(COMMAND_NAME);
            assertThat(config.getSourceEncoding()).as("encoding should be ISO_8859")
                    .isEqualTo(StandardCharsets.ISO_8859_1);
        }

        @Test
        void incrementalAnalysis() {
            var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).incrementalAnalysis(true);

            var config = pmd.initConfiguration(COMMAND_NAME);
            assertThat(config.isIgnoreIncrementalAnalysis()).isFalse();
        }

        @Test
        void languageVersions() throws ExitStatusException {
            var language = LanguageRegistry.PMD.getLanguageById("java");
            assertThat(language).isNotNull();

            var pmd = newPmdOperation()
                    .forceLanguageVersion(language.getLatestVersion())
                    .defaultLanguageVersions(language.getVersions())
                    .languageVersions(language.getDefaultVersion())
                    .ruleSets(PmdOperation.RULE_SET_DEFAULT);

            assertThat(pmd.languageVersions()).as("should contain default language version")
                    .contains(language.getDefaultVersion());
            assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).processingErrors())
                    .as("should have processing errors").isGreaterThan(0);

            assertThat(pmd.defaultLanguageVersions(language.getVersion("17"), language.getVersion("21"))
                    .languageVersions(language.getVersions())
                    .excludesStrings("src/test/resources/txt", "src/test/resources/xml")
                    .performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).configurationErrors())
                    .as("should have no processing errors").isEqualTo(0);
        }

        @Test
        void prependAuxClasspath() {
            var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).prependAuxClasspath(FOO, BAR);
            assertThat(pmd.prependAuxClasspath()).isEqualTo(FOO + File.pathSeparator + BAR);
        }

        @Test
        void priority() throws ExitStatusException {
            var pmd = newPmdOperation().inputPaths(CODE_STYLE_SAMPLE).minimumPriority(RulePriority.HIGH);
            assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).rulesChecked())
                    .as(ANALYSIS_SUCCESS).isEqualTo(0);
        }

        @Test
        void relativizeRoots() {
            var baz = Path.of("baz/foz");

            var pmd = newPmdOperation()
                    .ruleSets(List.of(CATEGORY_FOO))
                    .relativizeRoots(PATH_FOO)
                    .relativizeRoots(PATH_BAR.toFile()).relativizeRoots(baz.toString())
                    .relativizeRoots(List.of(PATH_FOO, PATH_BAR, baz));

            var config = pmd.initConfiguration(COMMAND_NAME);
            assertThat(config.getRelativizeRoots()).as("multiple roots").isEqualTo(pmd.relativizeRoots())
                    .containsExactly(PATH_FOO, PATH_BAR, baz, PATH_FOO, PATH_BAR, baz);

            pmd = newPmdOperation().ruleSets(List.of(CATEGORY_FOO))
                    .relativizeRootsFiles(List.of(PATH_FOO.toFile(), PATH_BAR.toFile(), baz.toFile()));
            config = pmd.initConfiguration(COMMAND_NAME);
            assertThat(config.getRelativizeRoots()).as("List(File...)").isEqualTo(pmd.relativizeRoots())
                    .containsExactly(PATH_FOO, PATH_BAR, baz);

            pmd = newPmdOperation().ruleSets(List.of(CATEGORY_FOO))
                    .relativizeRootsStrings(List.of(PATH_FOO.toString(), PATH_BAR.toString(), baz.toString()));
            config = pmd.initConfiguration(COMMAND_NAME);
            assertThat(config.getRelativizeRoots()).as("List(String....)").isEqualTo(pmd.relativizeRoots())
                    .containsExactly(PATH_FOO, PATH_BAR, baz);
        }

        @Test
        void showSuppressed() {
            var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).showSuppressed(true);
            var config = pmd.initConfiguration(COMMAND_NAME);
            assertThat(config.isShowSuppressedViolations()).isTrue();
        }

        @Test
        void suppressedMarker() {
            var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).suppressedMarker(TEST);
            var config = pmd.initConfiguration(COMMAND_NAME);
            assertThat(config.getSuppressMarker()).isEqualTo(TEST);
        }

        @Test
        void threads() {
            var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).threads(5);
            var config = pmd.initConfiguration(COMMAND_NAME);
            assertThat(config.getThreads()).isEqualTo(5);
        }

        @Test
        void uri() throws URISyntaxException {
            var uri = new URI("https://example.com");
            var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).uri(uri);
            var config = pmd.initConfiguration(COMMAND_NAME);
            assertThat(config.getUri()).isEqualTo(uri);
        }

        @Nested
        @DisplayName("Exclusion Tests")
        class ExclusionTests {
            @Test
            void addExcludes() {
                var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).addExcludes(PATH_FOO);
                var config = pmd.initConfiguration(COMMAND_NAME);
                assertThat(config.getExcludes()).containsExactly(PATH_FOO);

                pmd = pmd.addExcludes(PATH_BAR);
                config = pmd.initConfiguration(COMMAND_NAME);
                assertThat(config.getExcludes()).containsExactly(PATH_FOO, PATH_BAR);
            }

            @Test
            void addExcludesFiles() {
                var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).addExcludesFiles(FILE_FOO);
                var config = pmd.initConfiguration(COMMAND_NAME);
                assertThat(config.getExcludes()).containsExactly(FILE_FOO.toPath());

                pmd = pmd.addExcludesFiles(FILE_BAR);
                config = pmd.initConfiguration(COMMAND_NAME);
                assertThat(config.getExcludes()).containsExactly(FILE_FOO.toPath(), FILE_BAR.toPath());
            }

            @Test
            void addExcludesStrings() {
                var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).addExcludesStrings(FOO);
                var config = pmd.initConfiguration(COMMAND_NAME);
                assertThat(config.getExcludes()).containsExactly(PATH_FOO);

                pmd = pmd.addExcludesStrings(BAR);
                config = pmd.initConfiguration(COMMAND_NAME);
                assertThat(config.getExcludes()).containsExactly(PATH_FOO, PATH_BAR);
            }

            @Test
            void excludes() {
                var foz = Path.of("foz/baz");
                var baz = Path.of("baz/foz");

                var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).excludes(PATH_FOO, PATH_BAR);
                var config = pmd.initConfiguration(COMMAND_NAME);
                assertThat(pmd.excludes()).containsExactly(List.of(PATH_FOO, PATH_BAR).toArray(new Path[0]));
                assertThat(config.getExcludes()).containsExactly(List.of(PATH_FOO, PATH_BAR).toArray(new Path[0]));

                var excludes = List.of(List.of(PATH_FOO, PATH_BAR), List.of(foz, baz));
                for (var exclude : excludes) {
                    pmd = newPmdOperation().ruleSets(CATEGORY_FOO).excludes(exclude);
                    config = pmd.initConfiguration(COMMAND_NAME);
                    assertThat(config.getExcludes()).containsExactly(exclude.toArray(new Path[0]));
                }
            }

            @Test
            void excludesFiles() {
                var foz = new File("foz");
                var baz = new File("baz");

                var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).excludesFiles(FILE_FOO, FILE_BAR);
                var config = pmd.initConfiguration(COMMAND_NAME);
                assertThat(config.getExcludes()).containsExactly(FILE_FOO.toPath(), FILE_BAR.toPath());

                var excludes = List.of(List.of(FILE_FOO, FILE_BAR), List.of(foz, baz));
                for (var exclude : excludes) {
                    pmd = newPmdOperation().ruleSets(CATEGORY_FOO).excludesFiles(exclude);
                    config = pmd.initConfiguration(COMMAND_NAME);
                    assertThat(config.getExcludes())
                            .containsExactly(exclude.stream().map(File::toPath).toArray(Path[]::new));
                }
            }

            @Test
            void excludesStrings() {
                var foz = "foz";
                var baz = "baz";

                var pmd = newPmdOperation().ruleSets(CATEGORY_FOO).excludesStrings(FOO, BAR);
                var config = pmd.initConfiguration(COMMAND_NAME);
                assertThat(pmd.excludes()).containsExactly(PATH_FOO, PATH_BAR);
                assertThat(config.getExcludes()).containsExactly(PATH_FOO, PATH_BAR);

                var excludes = List.of(List.of(FOO, BAR), List.of(foz, baz));
                for (var exclude : excludes) {
                    pmd = newPmdOperation().ruleSets(CATEGORY_FOO).excludesStrings(exclude);
                    config = pmd.initConfiguration(COMMAND_NAME);
                    assertThat(config.getExcludes())
                            .containsExactly(exclude.stream().map(Paths::get).toArray(Path[]::new));
                }
            }
        }

        @Nested
        @DisplayName("Failure Tests")
        class FailureTests {
            @Test
            void failOnError() {
                var pmd = newPmdOperation().ruleSets("src/test/resources/xml/old.xml")
                        .inputPaths(Path.of("src/test/resources/java/Documentation.java"));
                assertThatCode(() -> pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                        .isInstanceOf(ExitStatusException.class);
                assertThatCode(() -> pmd.failOnError(false).performPmdAnalysis(TEST,
                        pmd.initConfiguration(COMMAND_NAME))).doesNotThrowAnyException();
            }

            @Test
            void failOnValidation() {
                var pmd = newPmdOperation().ruleSets(DOCUMENTATION_XML)
                        .inputPaths(Path.of("src/test/resources/java/Documentation.java"));
                assertThatCode(() -> pmd.failOnViolation(true).performPmdAnalysis(TEST,
                        pmd.initConfiguration(COMMAND_NAME))).isInstanceOf(ExitStatusException.class);
                assertThatCode(() -> pmd.failOnViolation(false).performPmdAnalysis(TEST,
                        pmd.initConfiguration(COMMAND_NAME))).doesNotThrowAnyException();
            }
        }

        @Nested
        @DisplayName("Ignore File Tests")
        class IgnoreFileTests {
            @Test
            void ignoreFile() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(ERROR_PRONE_XML, CODE_STYLE_XML)
                        .inputPaths(ERROR_PRONE_SAMPLE, CODE_STYLE_SAMPLE)
                        .ignoreFile(Path.of("src/test/resources/txt/ignore.txt"));

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as("%s for error prone and code style", ANALYSIS_SUCCESS).isEqualTo(0);
            }

            @Test
            void ignoreSingleFile() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(ERROR_PRONE_XML)
                        .inputPaths(ERROR_PRONE_SAMPLE)
                        .ignoreFile(new File("src/test/resources/txt/ignore-single.txt"));

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as("%s for error prone sample", ANALYSIS_SUCCESS).isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("Input Paths Tests")
        class InputPathsTests {
            @Test
            void fileArray() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(PmdOperation.RULE_SET_DEFAULT, CODE_STYLE_XML)
                        .inputPaths(ERROR_PRONE_SAMPLE.toFile(), CODE_STYLE_SAMPLE.toFile());

                assertThat(pmd.inputPaths()).containsExactly(ERROR_PRONE_SAMPLE, CODE_STYLE_SAMPLE);
                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void mainOperation() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .inputPaths(new File("src/main"))
                        .performPmdAnalysis(TEST, newPmdOperation().initConfiguration(COMMAND_NAME))
                        .violations();

                assertThat(pmd).as(ANALYSIS_SUCCESS).isEqualTo(0);
            }

            @Test
            void pathArray() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(PmdOperation.RULE_SET_DEFAULT, CODE_STYLE_XML)
                        .inputPaths(ERROR_PRONE_SAMPLE, CODE_STYLE_SAMPLE);

                assertThat(pmd.inputPaths()).containsExactly(ERROR_PRONE_SAMPLE, CODE_STYLE_SAMPLE);
                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void pathList() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(PmdOperation.RULE_SET_DEFAULT, CODE_STYLE_XML)
                        .inputPathsFiles(List.of(ERROR_PRONE_SAMPLE.toFile(), CODE_STYLE_SAMPLE.toFile()));

                assertThat(pmd.inputPaths()).containsExactly(ERROR_PRONE_SAMPLE, CODE_STYLE_SAMPLE);
                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void stringArray() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(PmdOperation.RULE_SET_DEFAULT, CODE_STYLE_XML)
                        .inputPaths(ERROR_PRONE_SAMPLE.toString(), CODE_STYLE_SAMPLE.toString());

                assertThat(pmd.inputPaths()).containsExactly(ERROR_PRONE_SAMPLE, CODE_STYLE_SAMPLE);
                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void stringList() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(PmdOperation.RULE_SET_DEFAULT, CODE_STYLE_XML)
                        .inputPathsStrings(List.of(ERROR_PRONE_SAMPLE.toString(), CODE_STYLE_SAMPLE.toString()));

                assertThat(pmd.inputPaths()).containsExactly(ERROR_PRONE_SAMPLE, CODE_STYLE_SAMPLE);
                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void xml() throws ExitStatusException {
                var pmd = newPmdOperation().addInputPaths("src/test/resources/xml/pmd.xml")
                        .ruleSets("src/test/resources/xml/basic.xml");

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).configurationErrors())
                        .isEqualTo(0);
            }

            @Nested
            @DisplayName("Add Input Paths Tests")
            class AddInputPathsTests {
                final BaseProject project = new BaseProject();

                @Test
                void addInputPathsAsFileArray() {
                    var pmd = new PmdOperation().fromProject(project);
                    pmd.inputPaths().clear();
                    pmd = pmd.addInputPaths(project.srcMainDirectory(), project.srcTestDirectory());

                    assertThat(pmd.inputPaths()).as("File...").containsExactly(project.srcMainDirectory().toPath(),
                            project.srcTestDirectory().toPath());
                }

                @Test
                void addInputPathsAsFileList() {
                    var pmd = new PmdOperation().fromProject(project);
                    pmd.inputPaths().clear();
                    pmd = pmd.addInputPathsFiles(List.of(project.srcMainDirectory(), project.srcTestDirectory()));

                    assertThat(pmd.inputPaths()).as("List(File...)")
                            .containsExactly(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());
                }

                @Test
                void addInputPathsAsPathArray() {
                    var pmd = new PmdOperation().fromProject(project);
                    pmd.inputPaths().clear();
                    pmd = pmd.addInputPaths(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());

                    assertThat(pmd.inputPaths()).as("Path...")
                            .containsExactly(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());
                }

                @Test
                void addInputPathsAsPathList() {
                    var pmd = new PmdOperation().fromProject(project);
                    pmd.inputPaths().clear();
                    pmd = pmd.addInputPaths(List.of(project.srcMainDirectory().toPath(),
                            project.srcTestDirectory().toPath()));

                    assertThat(pmd.inputPaths()).as("List(Path)")
                            .containsExactly(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());
                }

                @Test
                void addInputPathsAsStringArray() {
                    var pmd = new PmdOperation().fromProject(project);
                    pmd.inputPaths().clear();
                    pmd = pmd.addInputPaths(project.srcMainDirectory().getAbsolutePath(),
                            project.srcTestDirectory().getAbsolutePath());

                    assertThat(pmd.inputPaths()).as("String...")
                            .containsExactly(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());
                }

                @Test
                void addInputPathsAsStringList() {
                    var pmd = new PmdOperation().fromProject(project);
                    pmd.inputPaths().clear();
                    pmd = pmd.addInputPathsStrings(
                            List.of(project.srcMainDirectory().getAbsolutePath(),
                                    project.srcTestDirectory().getAbsolutePath())
                    );

                    assertThat(pmd.inputPaths()).as("List(String...)")
                            .containsExactly(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());
                }

                @Test
                void addInputPathsWithDefaults() {
                    var pmd = new PmdOperation().fromProject(project);
                    assertThat(pmd.inputPaths()).as("default input paths")
                            .containsExactly(project.srcMainDirectory().toPath(), project.srcTestDirectory().toPath());
                }
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
                        .ruleSets(List.of(ERROR_PRONE_XML, DESIGN_XML))
                        .reportFile(report);

                pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME));
                assertThat(report).as("report should exist").exists();

                try (var writer = new PrintWriter(report)) {
                    writer.write("");
                }
                assertThat(report).as("report should not be empty").isEmpty();

                pmd.reportFile(report.getAbsolutePath()).performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME));
                assertThat(report).as("report should not be empty").isNotEmpty();
            }

            @Test
            void reportFormat() throws IOException, ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(ERROR_PRONE_XML)
                        .reportFormat("xml")
                        .inputPaths(ERROR_PRONE_SAMPLE);

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
                try (var br = Files.newBufferedReader(pmd.reportFile())) {
                    assertThat(br.readLine()).as("XML report for %s", pmd.reportFile().toString())
                            .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                }
            }

            @Test
            void reportProperties() throws ExitStatusException {
                var pmd = newPmdOperation().ruleSets(CODE_STYLE_XML, ERROR_PRONE_XML)
                        .includeLineNumber(true)
                        .reportProperties(new Properties());

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).processingErrors())
                        .isEqualTo(0);
            }
        }

        @Nested
        @DisplayName("RuleSets Tests")
        class RuleSetsTests {
            @Test
            void javaBestPractices() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets("category/java/bestpractices.xml")
                        .inputPaths(Path.of("src/test/resources/java/BestPractices.java"));

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaCodeStyle() throws ExitStatusException {
                var pmd = newPmdOperation().ruleSets(CODE_STYLE_XML).inputPaths(CODE_STYLE_SAMPLE);

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaCodeStyleAndErrorProne() throws ExitStatusException {
                var pmd = newPmdOperation().ruleSets(CODE_STYLE_XML).inputPaths(CODE_STYLE_SAMPLE);

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as("analysis with code style").isGreaterThan(0);

                pmd = pmd.ruleSets(ERROR_PRONE_XML).inputPaths(ERROR_PRONE_SAMPLE);
                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as("analysis with code style and error prone").isGreaterThan(0);
            }

            @Test
            void javaDesign() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(DESIGN_XML)
                        .inputPaths("src/test/resources/java/Design.java")
                        .cache(new File("build/pmd/design-cache"));

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).configurationErrors())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaDocumentation() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(DOCUMENTATION_XML)
                        .inputPaths(Path.of("src/test/resources/java/Documentation.java"));

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaErrorProne() throws ExitStatusException {
                var pmd = newPmdOperation().ruleSets(ERROR_PRONE_XML).inputPaths(ERROR_PRONE_SAMPLE);

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaMultiThreading() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets("category/java/multithreading.xml")
                        .inputPaths(Path.of("src/test/resources/java/MultiThreading.java"));

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaPerformance() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(PERFORMANCE_XML)
                        .inputPaths(Path.of("src/test/resources/java/Performance.java"));

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaQuickStart() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(PmdOperation.RULE_SET_DEFAULT)
                        .inputPaths(new File("src/test/resources/java/"));

                assertThat(pmd.ruleSets()).containsExactly(PmdOperation.RULE_SET_DEFAULT);
                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void javaSecurity() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets(SECURITY_XML)
                        .inputPaths(Path.of("src/test/resources/java/Security.java"));

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                        .as(ANALYSIS_FAILURE).isGreaterThan(0);
            }

            @Test
            void ruleSetsConfigFile() throws ExitStatusException {
                var pmd = newPmdOperation()
                        .ruleSets("src/test/resources/xml/pmd.xml")
                        .ignoreFile("src/test/resources/txt/ignore-all.txt");

                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).hasNoErrors()).isTrue();
            }

            @Test
            void ruleSetsEmpty() throws ExitStatusException {
                var pmd = newPmdOperation().ruleSets("").failOnError(false);
                assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).errors())
                        .isEqualTo(1);
            }

            @Nested
            @DisplayName("Add RuleSets Tests")
            class AddRuleSetsTests {
                @Test
                void addRuleSetsWithDefault() {
                    var pmd = new PmdOperation().fromProject(new BaseProject());

                    assertThat(pmd.ruleSets()).containsExactly(PmdOperation.RULE_SET_DEFAULT);
                }

                @Test
                void addRuleSetsWithErrorProne() {
                    var pmd = new PmdOperation().fromProject(new BaseProject());

                    pmd.addRuleSet(ERROR_PRONE_XML);

                    assertThat(pmd.ruleSets()).containsExactly(PmdOperation.RULE_SET_DEFAULT, ERROR_PRONE_XML);
                }

                @Test
                void addRuleSetsWithList() {
                    var pmd = new PmdOperation().fromProject(new BaseProject());
                    pmd.ruleSets().clear();
                    pmd.addRuleSet(List.of(PmdOperation.RULE_SET_DEFAULT, ERROR_PRONE_XML));

                    assertThat(pmd.ruleSets()).as("rulesets as collection")
                            .containsExactly(PmdOperation.RULE_SET_DEFAULT, ERROR_PRONE_XML);
                }
            }
        }
    }

    @Nested
    @DisplayName("Execution Tests")
    class ExecutionTests {
        @Test
        void execute() throws ExitStatusException {
            var pmd = new PmdOperation().fromProject(new BaseProject());

            pmd.inputPaths("src/main/java", "src/test/java").ruleSets("config/pmd.xml");

            assertThat(pmd.ruleSets()).containsExactly("config/pmd.xml");
            assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)).violations())
                    .as(ANALYSIS_SUCCESS).isEqualTo(0);
        }

        @Test
        void executeDefaultInputPaths() {
            var pmd = new PmdOperation().fromProject(new BaseProject());

            assertThat(pmd.inputPaths()).containsExactly(Paths.get("src/main").toAbsolutePath(),
                    Paths.get("src/test").toAbsolutePath());
        }

        @Test
        void executeNoProject() {
            var pmd = new PmdOperation();
            assertThatCode(pmd::execute).isInstanceOf(ExitStatusException.class);
        }
    }
}
