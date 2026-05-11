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

import edu.umd.cs.findbugs.annotations.NonNull;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.reporting.RuleViolation;
import org.jetbrains.annotations.TestOnly;
import rife.bld.BaseProject;
import rife.bld.extension.pmd.JavaRules;
import rife.bld.extension.pmd.PmdAnalysisResults;
import rife.bld.extension.tools.CollectionTools;
import rife.bld.extension.tools.IOTools;
import rife.bld.extension.tools.ObjectTools;
import rife.bld.operations.AbstractOperation;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs static code analysis with <a href="https://pmd.github.io/">PMD</a>.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
@SuppressWarnings("PMD.CouplingBetweenObjects")
@SuppressFBWarnings(
        value = "EI_EXPOSE_REP",
        justification = "Builder pattern intentionally exposes mutable collections"
)
public class PmdOperation extends AbstractOperation<PmdOperation> {

    private static final String INPUT_PATHS = "inputPaths";
    private static final String MSG_FORMAT_NO_LINE_IN_LINK =
            "%s (line: %d)\n\t%s (%s)\n\t\t--> %s";
    private static final String MSG_FORMAT_WITH_LINE =
            "%s:%d\n\t%s (%s)\n\t\t--> %s";
    private static final String PMD_DIR = "pmd";
    private static final String RELATIVIZE_ROOTS = "relativizeRoots";
    private static final String RULE_SETS = "ruleSets";
    private static final Logger logger = Logger.getLogger(PmdOperation.class.getName());
    private final List<LanguageVersion> defaultLanguageVersions_ = new ArrayList<>();
    private final List<Path> excludes_ = new ArrayList<>();
    private final List<Path> inputPaths_ = new ArrayList<>();
    private final List<Path> relativizeRoots_ = new ArrayList<>();
    private final Properties reportProperties_ = new Properties();
    private final Set<String> ruleSets_ = new HashSet<>(); // Keep the order when logging
    private Path cache_;
    private boolean collectFilesRecursively_ = true;
    private Charset encoding_ = StandardCharsets.UTF_8;
    private boolean failOnError_ = true;
    private boolean failOnViolation_;
    private LanguageVersion forcedLanguageVersion_;
    private Path ignoreFile_;
    private boolean includeLineNumber_ = true;
    private boolean incrementalAnalysis_ = true;
    private URI inputUri_;
    private String prependAuxClasspath_;
    private Path reportFile_;
    private String reportFormat_ = "text";
    private RulePriority rulePriority_ = RulePriority.LOW;
    private boolean showSuppressed_;
    private String suppressedMarker_ = "NOPMD";
    private int threads_ = 1;

    /**
     * Performs the PMD code analysis operation.
     *
     * @throws Exception           if the analysis fails to execute
     * @throws ExitStatusException if violations or errors occur and {@code failOnViolation}
     *                             or {@code failOnError} is true
     */
    @Override
    public void execute() throws Exception {
        performAnalysis(initConfiguration());
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     *
     * @param path the cache file path
     * @return this operation
     * @throws NullPointerException if {@code path} is null
     * @see #cache(File)
     * @see #cache(String)
     */
    public PmdOperation cache(@NonNull Path path) {
        cache_ = ObjectTools.requireNonNull(path, "path");
        return this;
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     *
     * @param file the cache file
     * @return this operation
     * @throws NullPointerException if {@code file} is null
     * @see #cache(Path)
     * @see #cache(String)
     */
    public PmdOperation cache(@NonNull File file) {
        ObjectTools.requireNonNull(file, "file");
        cache_ = file.toPath();
        return this;
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     *
     * @param cache the cache file path
     * @return this operation
     * @throws NullPointerException     if {@code cache} is null
     * @throws IllegalArgumentException if {@code cache} is empty
     * @see #cache(Path)
     * @see #cache(File)
     */
    public PmdOperation cache(@NonNull String cache) {
        cache_ = Path.of(ObjectTools.requireNotEmpty(cache, "cache"));
        return this;
    }

    /**
     * When specified, any directory mentioned with {@link #inputPaths()} will only be searched
     * for files that are direct children. By default, subdirectories are recursively included.
     *
     * @param collectFilesRecursively whether to collect files recursively or not
     * @return this operation
     * @since 1.3.0
     */
    public PmdOperation collectFilesRecursively(boolean collectFilesRecursively) {
        this.collectFilesRecursively_ = collectFilesRecursively;
        return this;
    }

    /**
     * Sets the default language version to be used for all input files.
     *
     * @param languageVersions one or more language versions
     * @return this operation
     * @throws NullPointerException     if {@code languageVersions} is null
     * @throws IllegalArgumentException if {@code languageVersions} elements are null or empty
     * @see #defaultLanguageVersions(Collection)
     * @see #defaultLanguageVersions()
     */
    public PmdOperation defaultLanguageVersions(@NonNull LanguageVersion... languageVersions) {
        ObjectTools.requireNotEmpty(languageVersions, "defaultLanguageVersion");
        defaultLanguageVersions_.addAll(List.of(languageVersions));
        return this;
    }

    /**
     * Sets the default language version to be used for all input files.
     *
     * @param languageVersions a collection of language versions
     * @return this operation
     * @throws NullPointerException     if {@code languageVersions} is null
     * @throws IllegalArgumentException if {@code languageVersions} elements are null or empty
     * @see #defaultLanguageVersions(LanguageVersion...)
     * @see #defaultLanguageVersions()
     */
    public final PmdOperation defaultLanguageVersions(@NonNull Collection<LanguageVersion> languageVersions) {
        ObjectTools.requireNotEmpty(languageVersions, "defaultLanguageVersions");
        defaultLanguageVersions_.addAll(languageVersions);
        return this;
    }

    /**
     * Returns the default language versions.
     *
     * @return the language versions
     * @see #defaultLanguageVersions(LanguageVersion...)
     * @see #defaultLanguageVersions(Collection)
     */
    public List<LanguageVersion> defaultLanguageVersions() {
        return defaultLanguageVersions_;
    }

    /**
     * Specifies the character set encoding of the source code files. The default is
     * {@link StandardCharsets#UTF_8 UTF-8}.
     *
     * <p>The valid values are the standard character sets of {@link java.nio.charset.Charset Charset}.</p>
     *
     * @param encoding the encoding name
     * @return this operation
     * @throws NullPointerException     if {@code encoding} is null
     * @throws IllegalArgumentException if {@code encoding} is empty
     * @see #encoding(Charset)
     */
    public PmdOperation encoding(@NonNull String encoding) {
        encoding_ = Charset.forName(ObjectTools.requireNotEmpty(encoding, "encoding"));
        return this;
    }

    /**
     * Specifies the character set encoding of the source code files. The default is
     * {@link StandardCharsets#UTF_8 UTF-8}.
     *
     * <p>The valid values are the standard character sets of {@link java.nio.charset.Charset Charset}.</p>
     *
     * @param encoding the charset
     * @return this operation
     * @throws NullPointerException if {@code encoding} is null
     * @see #encoding(String)
     */
    public PmdOperation encoding(@NonNull Charset encoding) {
        encoding_ = ObjectTools.requireNonNull(encoding, "encoding");
        return this;
    }

    /**
     * Returns the paths to exclude from the analysis.
     *
     * @return the exclude paths
     * @see #excludes(Path...)
     * @see #excludes(Collection)
     */
    public List<Path> excludes() {
        return excludes_;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param paths one or more paths to exclude
     * @return this operation
     * @throws NullPointerException     if {@code paths} is null
     * @throws IllegalArgumentException if {@code paths} elements are null or empty
     * @see #excludes(Collection)
     * @see #excludes()
     */
    public PmdOperation excludes(@NonNull Path... paths) {
        ObjectTools.requireNotEmpty(paths, "excludes");
        excludes_.addAll(List.of(paths));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param paths paths to exclude
     * @return this operation
     * @throws NullPointerException     if {@code paths} is null
     * @throws IllegalArgumentException if {@code paths} elements are null or empty
     * @see #excludes(Path...)
     * @see #excludes()
     */
    public final PmdOperation excludes(@NonNull Collection<Path> paths) {
        ObjectTools.requireNotEmpty(paths, "excludes");
        excludes_.addAll(paths);
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param files one or more paths to exclude
     * @return this operation
     * @throws NullPointerException     if {@code files} is null
     * @throws IllegalArgumentException if {@code files} elements are null or empty
     * @see #excludesFiles(Collection)
     * @see #excludes(Path...)
     * @since 1.2
     */
    public PmdOperation excludesFiles(@NonNull File... files) {
        ObjectTools.requireNotEmpty(files, "excludesFiles");
        excludes_.addAll(CollectionTools.combineFilesToPaths(files));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param files a collection of paths to exclude
     * @return this operation
     * @throws NullPointerException     if {@code files} is null
     * @throws IllegalArgumentException if {@code files} elements are null or empty
     * @see #excludesFiles(File...)
     * @see #excludes(Path...)
     * @since 1.2
     */
    public final PmdOperation excludesFiles(@NonNull Collection<File> files) {
        ObjectTools.requireNotEmpty(files, "excludesFiles");
        excludes_.addAll(CollectionTools.combineFilesToPaths(files));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @throws NullPointerException     if {@code excludes} is null
     * @throws IllegalArgumentException if {@code excludes} elements are null or empty
     * @see #excludesStrings(Collection)
     * @see #excludes(Path...)
     * @since 1.2
     */
    public PmdOperation excludesStrings(@NonNull String... excludes) {
        ObjectTools.requireNotEmpty(excludes, "excludeStrings");
        excludes_.addAll(CollectionTools.combineStringsToPaths(excludes));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes a collection of paths to exclude
     * @return this operation
     * @throws NullPointerException     if {@code excludes} is null
     * @throws IllegalArgumentException if {@code excludes} elements are null or empty
     * @see #excludesStrings(String...)
     * @see #excludes(Path...)
     * @since 1.2
     */
    public final PmdOperation excludesStrings(@NonNull Collection<String> excludes) {
        ObjectTools.requireNotEmpty(excludes, "excludeStrings");
        excludes_.addAll(CollectionTools.combineStringsToPaths(excludes));
        return this;
    }

    /**
     * Sets whether the build will exit on recoverable errors.
     * <p>
     * Default is: {@code true}
     * <p>
     * Note: If only violations are found, see {@link #failOnViolation(boolean) failOnViolation}
     *
     * @param failOnError whether to exit and fail the build if recoverable errors occurred
     * @return this operation
     * @see #failOnViolation(boolean)
     */
    public PmdOperation failOnError(boolean failOnError) {
        failOnError_ = failOnError;
        return this;
    }

    /**
     * Sets whether the build will continue on violations.
     * <p>
     * Note: If additionally recoverable errors occurred, see {@link #failOnError(boolean) failOnError}
     *
     * @param failOnViolation whether to exit and fail the build if violations are found
     * @return this operation
     * @see #failOnError(boolean)
     */
    public PmdOperation failOnViolation(boolean failOnViolation) {
        failOnViolation_ = failOnViolation;
        return this;
    }

    /**
     * Forces a language to be used for all input files, irrespective of file names.
     *
     * @param languageVersion the language version
     * @return this operation
     * @throws NullPointerException if {@code languageVersion} is null
     */
    public PmdOperation forceLanguageVersion(@NonNull LanguageVersion languageVersion) {
        forcedLanguageVersion_ = ObjectTools.requireNonNull(languageVersion, "forceLanguageVersion");
        return this;
    }

    /**
     * Configures a PMD operation from a {@link BaseProject}.
     *
     * <p>
     * The defaults are:
     * <ul>
     * <li>cache={@code build/pmd/pmd-cache}, if not already set</li>
     * <li>encoding={@code UTF-8}</li>
     * <li>incrementalAnalysis={@code true}</li>
     * <li>inputPaths={@code [src/main, src/test]}, if not already set</li>
     * <li>reportFile={@code build/pmd/pmd-report.txt}, if not already set</li>
     * <li>reportFormat={@code text}</li>
     * <li>rulePriority={@code LOW}</li>
     * <li>ruleSets={@link JavaRules#QUICK_START}, if not already set</li>
     * <li>suppressedMarker={@code NOPMD}</li>
     * <li>threads=1</li>
     * </ul>
     *
     * @param project the project
     * @return this operation
     * @throws NullPointerException if {@code project} is null
     */
    public PmdOperation fromProject(@NonNull BaseProject project) {
        ObjectTools.requireNonNull(project, "fromProject");
        if (inputPaths_.isEmpty()) {
            inputPaths(project.srcMainDirectory(), project.srcTestDirectory());
        }
        if (ruleSets_.isEmpty()) {
            ruleSets(JavaRules.QUICK_START);
        }
        if (cache_ == null) {
            cache_ = IOTools.resolveFile(project.buildDirectory(), PMD_DIR, PMD_DIR + "-cache").toPath();
        }
        if (reportFile_ == null) {
            reportFile_ =
                    IOTools.resolveFile(project.buildDirectory(), PMD_DIR, PMD_DIR + "-report.txt").toPath();
        }
        return this;
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     *
     * @param path the ignore file path
     * @return this operation
     * @throws NullPointerException if {@code path} is null
     * @see #ignoreFile(File)
     * @see #ignoreFile(String)
     */
    public PmdOperation ignoreFile(@NonNull Path path) {
        ignoreFile_ = ObjectTools.requireNonNull(path, "ignoreFile");
        return this;
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     *
     * @param file the ignore file
     * @return this operation
     * @throws NullPointerException if {@code file} is null
     * @see #ignoreFile(Path)
     * @see #ignoreFile(String)
     */
    public PmdOperation ignoreFile(@NonNull File file) {
        ObjectTools.requireNonNull(file, "ignoreFile");
        ignoreFile_ = file.toPath();
        return this;
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     *
     * @param ignoreFile the ignore file path
     * @return this operation
     * @throws NullPointerException     if {@code ignoreFile} is null
     * @throws IllegalArgumentException if {@code ignoreFile} is empty
     * @see #ignoreFile(Path)
     * @see #ignoreFile(File)
     */
    public PmdOperation ignoreFile(@NonNull String ignoreFile) {
        ignoreFile_ = Path.of(ObjectTools.requireNotEmpty(ignoreFile, "ignoreFile"));
        return this;
    }

    /**
     * Enables or disables line number in source file URIs.
     * <p>
     * While clicking on the URI works in IntelliJ IDEA, Visual Studio Code, etc.; it might
     * not in terminal emulators.
     * <p>
     * Default: {@code true}
     *
     * @param includeLineNumber whether to include the line number in source file URIs
     * @return this operation
     */
    public PmdOperation includeLineNumber(boolean includeLineNumber) {
        includeLineNumber_ = includeLineNumber;
        return this;
    }

    /**
     * Enables or disables incremental analysis.
     *
     * @param incrementalAnalysis whether to enable incremental analysis
     * @return this operation
     */
    public PmdOperation incrementalAnalysis(boolean incrementalAnalysis) {
        incrementalAnalysis_ = incrementalAnalysis;
        return this;
    }

    /**
     * Returns paths to source files or directories containing source files to analyze.
     *
     * @return the input paths
     * @see #inputPaths(Path...)
     * @see #inputPaths(Collection)
     */
    public List<Path> inputPaths() {
        return inputPaths_;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param paths one or more paths
     * @return this operation
     * @throws NullPointerException     if {@code paths} is null
     * @throws IllegalArgumentException if {@code paths} elements are null or empty
     * @see #inputPaths(Collection)
     * @see #inputPaths()
     */
    public PmdOperation inputPaths(@NonNull Path... paths) {
        ObjectTools.requireNotEmpty(paths, INPUT_PATHS);
        inputPaths_.addAll(List.of(paths));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param files one or more paths
     * @return this operation
     * @throws NullPointerException     if {@code files} is null
     * @throws IllegalArgumentException if {@code files} elements are null or empty
     * @see #inputPathsFiles(Collection)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     */
    public PmdOperation inputPaths(@NonNull File... files) {
        ObjectTools.requireNotEmpty(files, INPUT_PATHS);
        inputPaths_.addAll(CollectionTools.combineFilesToPaths(files));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param inputPaths one or more paths
     * @return this operation
     * @throws NullPointerException     if {@code inputPaths} is null
     * @throws IllegalArgumentException if {@code inputPaths} elements are null or empty
     * @see #inputPathsStrings(Collection)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     */
    public PmdOperation inputPaths(@NonNull String... inputPaths) {
        ObjectTools.requireNotEmpty(inputPaths, INPUT_PATHS);
        inputPaths_.addAll(CollectionTools.combineStringsToPaths(inputPaths));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param paths a collection of input paths
     * @return this operation
     * @throws NullPointerException     if {@code paths} is null
     * @throws IllegalArgumentException if {@code paths} elements are null or empty
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     */
    public final PmdOperation inputPaths(@NonNull Collection<Path> paths) {
        ObjectTools.requireNotEmpty(paths, INPUT_PATHS);
        inputPaths_.addAll(paths);
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param files a collection of input paths
     * @return this operation
     * @throws NullPointerException     if {@code files} is null
     * @throws IllegalArgumentException if {@code files} elements are null or empty
     * @see #inputPaths(File...)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     */
    public final PmdOperation inputPathsFiles(@NonNull Collection<File> files) {
        ObjectTools.requireNotEmpty(files, "inputPathFiles");
        inputPaths_.addAll(CollectionTools.combineFilesToPaths(files));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param inputPaths a collection of input paths
     * @return this operation
     * @throws NullPointerException     if {@code inputPaths} is null
     * @throws IllegalArgumentException if {@code inputPaths} elements are null or empty
     * @see #inputPaths(String...)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     */
    public PmdOperation inputPathsStrings(@NonNull Collection<String> inputPaths) {
        ObjectTools.requireNotEmpty(inputPaths, "inputPathsStrings");
        inputPaths_.addAll(CollectionTools.combineStringsToPaths(inputPaths));
        return this;
    }

    /**
     * Sets the minimum priority threshold when loading Rules from RuleSets.
     *
     * @param priority the minimum rule priority
     * @return this operation
     * @throws NullPointerException if {@code priority} is null
     */
    public PmdOperation minimumPriority(@NonNull RulePriority priority) {
        rulePriority_ = ObjectTools.requireNonNull(priority, "minimumPriority");
        return this;
    }

    /**
     * Performs the PMD analysis with the given config.
     *
     * @param config the configuration
     * @return the analysis results
     * @throws NullPointerException if {@code config} is null
     * @throws ExitStatusException  if violations or errors occur and {@code failOnViolation}
     *                              or {@code failOnError} is true
     */
    public PmdAnalysisResults performAnalysis(@NonNull PMDConfiguration config) throws ExitStatusException {
        ObjectTools.requireNonNull(config, "performAnalysis");
        try (var pmd = PmdAnalysis.create(config)) {
            var report = pmd.performAnalysisAndCollectReport();

            if (canLog(Level.INFO)) {

                logger.info(() -> INPUT_PATHS + inputPaths_);
                logger.info(() -> RULE_SETS + ruleSets_);
            }

            var violations = report.getViolations();
            var numViolations = violations.size();

            if (numViolations > 0) {
                printViolations(config, violations);
            }

            if (pmd.getReporter().numErrors() > 0 && config.isFailOnError()) {
                throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
            }

            int rulesChecked = pmd.getRulesets().stream()
                    .mapToInt(rule -> rule.getRules().size())
                    .sum();

            var result = new PmdAnalysisResults(
                    numViolations,
                    report.getSuppressedViolations().size(),
                    pmd.getReporter().numErrors(),
                    report.getProcessingErrors().size(),
                    report.getConfigurationErrors().size(),
                    rulesChecked
            );

            if (canLog(Level.INFO)) {
                logger.info(() -> result.rulesChecked() + " rules were checked.");
            }

            if (canLog(Level.WARNING)) {
                if (result.processingErrors() > 0) {
                    for (var err : report.getProcessingErrors()) {
                        logger.warning(err::getMsg);
                    }
                }

                if (result.configurationErrors() > 0) {
                    for (var err : report.getConfigurationErrors()) {
                        logger.warning(err::issue);
                    }
                }
            }

            if (canLog(Level.FINEST)) {
                logger.finest(result::toString);
            }

            return result;
        }
    }

    /**
     * Prepend the specified classpaths-like string to the current ClassLoader of the configuration.
     * If no ClassLoader is currently configured, the ClassLoader used to load the PMDConfiguration
     * class will be used as the parent ClassLoader of the created ClassLoader.
     * <p>
     * If the classpaths String looks like a URL to a file (i.e., starts with {@code file://}) the
     * file will be read with each line representing an entry on the classpaths.
     *
     * @param classpaths one or more classpaths entries
     * @return this operation
     * @throws NullPointerException     if {@code classpaths} is null
     * @throws IllegalArgumentException if {@code classpaths} elements are null or empty
     * @see #prependAuxClasspath()
     */
    public PmdOperation prependAuxClasspath(@NonNull String... classpaths) {
        ObjectTools.requireNotEmpty(classpaths, "prependAuxClasspath");
        prependAuxClasspath_ = String.join(File.pathSeparator, classpaths);
        return this;
    }

    /**
     * Returns the prepended classpath.
     *
     * @return the classpath
     * @see #prependAuxClasspath(String...)
     */
    public String prependAuxClasspath() {
        return prependAuxClasspath_;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param roots one or more relative root paths
     * @return this operation
     * @throws NullPointerException     if {@code roots} is null
     * @throws IllegalArgumentException if {@code roots} elements are null or empty
     * @see #relativizeRoots(Collection)
     * @see #relativizeRoots()
     */
    public PmdOperation relativizeRoots(@NonNull Path... roots) {
        ObjectTools.requireNotEmpty(roots, RELATIVIZE_ROOTS);
        relativizeRoots_.addAll(List.of(roots));
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param roots one or more relative root paths
     * @return this operation
     * @throws NullPointerException     if {@code roots} is null
     * @throws IllegalArgumentException if {@code roots} elements are null or empty
     * @see #relativizeRootsFiles(Collection)
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     */
    public PmdOperation relativizeRoots(@NonNull File... roots) {
        ObjectTools.requireNotEmpty(roots, RELATIVIZE_ROOTS);
        relativizeRoots_.addAll(CollectionTools.combineFilesToPaths(roots));
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param roots one or more relative root paths
     * @return this operation
     * @throws NullPointerException     if {@code roots} is null
     * @throws IllegalArgumentException if {@code roots} elements are null or empty
     * @see #relativizeRootsStrings(Collection)
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     */
    public PmdOperation relativizeRoots(@NonNull String... roots) {
        ObjectTools.requireNotEmpty(roots, RELATIVIZE_ROOTS);
        relativizeRoots_.addAll(CollectionTools.combineStringsToPaths(roots));
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param roots a collection of relative root paths
     * @return this operation
     * @throws NullPointerException     if {@code roots} is null
     * @throws IllegalArgumentException if {@code roots} elements are null or empty
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     */
    public final PmdOperation relativizeRoots(@NonNull Collection<Path> roots) {
        ObjectTools.requireNotEmpty(roots, RELATIVIZE_ROOTS);
        relativizeRoots_.addAll(roots);
        return this;
    }

    /**
     * Returns paths to shorten paths that are output in the report.
     *
     * @return the relative root paths
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots(Collection)
     */
    public List<Path> relativizeRoots() {
        return relativizeRoots_;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param roots a collection of relative root paths
     * @return this operation
     * @throws NullPointerException     if {@code roots} is null
     * @throws IllegalArgumentException if {@code roots} elements are null or empty
     * @see #relativizeRoots(File...)
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     */
    public final PmdOperation relativizeRootsFiles(@NonNull Collection<File> roots) {
        ObjectTools.requireNotEmpty(roots, "relativizeRootsFiles");
        relativizeRoots_.addAll(CollectionTools.combineFilesToPaths(roots));
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param roots a collection of relative root paths
     * @return this operation
     * @throws NullPointerException     if {@code roots} is null
     * @throws IllegalArgumentException if {@code roots} elements are null or empty
     * @see #relativizeRoots(String...)
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     */
    public final PmdOperation relativizeRootsStrings(@NonNull Collection<String> roots) {
        ObjectTools.requireNotEmpty(roots, "relativizeRootsStrings");
        relativizeRoots_.addAll(CollectionTools.combineStringsToPaths(roots));
        return this;
    }

    /**
     * Sets the path to the report page.
     *
     * @param path the report file path
     * @return this operation
     * @throws NullPointerException if {@code path} is null
     * @see #reportFile(File)
     * @see #reportFile(String)
     * @see #reportFile()
     */
    public PmdOperation reportFile(@NonNull Path path) {
        reportFile_ = ObjectTools.requireNonNull(path, "reportFile");
        return this;
    }

    /**
     * Sets the path to the report page.
     *
     * @param file the report file
     * @return this operation
     * @throws NullPointerException if {@code file} is null
     * @see #reportFile(Path)
     * @see #reportFile(String)
     * @see #reportFile()
     */
    public PmdOperation reportFile(@NonNull File file) {
        ObjectTools.requireNonNull(file, "reportFile");
        reportFile_ = file.toPath();
        return this;
    }

    /**
     * Sets the path to the report page.
     *
     * @param reportFile the report file path
     * @return this operation
     * @throws NullPointerException     if {@code reportFile} is null
     * @throws IllegalArgumentException if {@code reportFile} is empty
     * @see #reportFile(Path)
     * @see #reportFile(File)
     * @see #reportFile()
     */
    public PmdOperation reportFile(@NonNull String reportFile) {
        ObjectTools.requireNotEmpty(reportFile, "reportFile");
        reportFile_ = Path.of(reportFile);
        return this;
    }

    /**
     * Returns the path to the report file.
     *
     * @return the report file path
     * @see #reportFile(Path)
     * @see #reportFile(File)
     * @see #reportFile(String)
     */
    public Path reportFile() {
        return reportFile_;
    }

    /**
     * Sets the output format of the analysis report. The default is {@code text}.
     *
     * @param format the report format
     * @return this operation
     * @throws NullPointerException     if {@code format} is null
     * @throws IllegalArgumentException if {@code format} is empty
     */
    public PmdOperation reportFormat(@NonNull String format) {
        reportFormat_ = ObjectTools.requireNotEmpty(format, "reportFormat");
        return this;
    }

    /**
     * Sets the Report properties. These are used to create the Renderer.
     *
     * @param properties the report properties
     * @return this operation
     * @throws NullPointerException if {@code properties} is null
     */
    public PmdOperation reportProperties(@NonNull Properties properties) {
        ObjectTools.requireNonNull(properties, "reportProperties");
        reportProperties_.putAll(properties);
        return this;
    }

    /**
     * Returns the rule set paths.
     *
     * @return the rule sets
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSetsRules(Collection)
     */
    public Set<String> ruleSets() {
        return ruleSets_;
    }

    /**
     * Sets rule set paths.
     *
     * @param ruleSets one or more rule set paths
     * @return this operation
     * @throws NullPointerException     if {@code ruleSets} is null
     * @throws IllegalArgumentException if {@code ruleSets} elements are null or empty
     * @see #ruleSets(Collection)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSetsRules(Collection)
     * @see #ruleSets()
     */
    public PmdOperation ruleSets(@NonNull String... ruleSets) {
        ObjectTools.requireNotEmpty(ruleSets, RULE_SETS);
        ruleSets_.addAll(List.of(ruleSets));
        return this;
    }

    /**
     * Sets rule set paths.
     *
     * @param ruleSets a collection of rule set paths
     * @return this operation
     * @throws NullPointerException     if {@code ruleSets} is null
     * @throws IllegalArgumentException if {@code ruleSets} elements are null or empty
     * @see #ruleSets(String...)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSetsRules(Collection)
     * @see #ruleSets()
     */
    public final PmdOperation ruleSets(@NonNull Collection<String> ruleSets) {
        ObjectTools.requireNotEmpty(ruleSets, RULE_SETS);
        ruleSets_.addAll(ruleSets);
        return this;
    }

    /**
     * Sets rule set paths.
     *
     * @param ruleSets one or more rule sets
     * @return this operation
     * @throws NullPointerException     if {@code ruleSets} is null
     * @throws IllegalArgumentException if {@code ruleSets} elements are null or empty
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection)
     * @see #ruleSetsRules(Collection)
     * @see #ruleSets()
     * @since 1.5
     */
    public final PmdOperation ruleSets(@NonNull JavaRules... ruleSets) {
        ObjectTools.requireNotEmpty(ruleSets, RULE_SETS);
        ruleSets_.addAll(Arrays.stream(ruleSets).map(JavaRules::getCategory).toList());
        return this;
    }

    /**
     * Sets rule set paths.
     *
     * @param ruleSets a collection of rule sets
     * @return this operation
     * @throws NullPointerException     if {@code ruleSets} is null
     * @throws IllegalArgumentException if {@code ruleSets} elements are null or empty
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSets()
     * @since 1.5
     */
    public final PmdOperation ruleSetsRules(@NonNull Collection<JavaRules> ruleSets) {
        ObjectTools.requireNotEmpty(ruleSets, "ruleSetsRules");
        ruleSets_.addAll(CollectionTools.combine(ruleSets).stream().map(JavaRules::getCategory).toList());
        return this;
    }

    /**
     * Enables or disables adding the suppressed rule violations to the report.
     *
     * @param showSuppressed whether to add suppressed violations to the report
     * @return this operation
     */
    public PmdOperation showSuppressed(boolean showSuppressed) {
        showSuppressed_ = showSuppressed;
        return this;
    }

    /**
     * Specifies the comment token that marks lines which should be ignored. The default is {@code NOPMD}.
     *
     * @param marker the suppressed marker token
     * @return this operation
     * @throws NullPointerException     if {@code marker} is null
     * @throws IllegalArgumentException if {@code marker} is empty
     */
    public PmdOperation suppressedMarker(@NonNull String marker) {
        suppressedMarker_ = ObjectTools.requireNotEmpty(marker, "suppressedMarker");
        return this;
    }

    /**
     * Sets the number of threads to be used. The default is {@code 1}.
     *
     * @param threads the number of threads
     * @return this operation
     */
    public PmdOperation threads(int threads) {
        threads_ = threads;
        return this;
    }

    /**
     * Sets the input URI to process for source code objects.
     *
     * @param inputUri the input URI
     * @return this operation
     * @throws NullPointerException if {@code inputUri} is null
     */
    public PmdOperation uri(@NonNull URI inputUri) {
        inputUri_ = ObjectTools.requireNonNull(inputUri, "uri");
        return this;
    }

    /**
     * Returns {@code true} when logging at {@code level} is both enabled and
     * not suppressed by {@link #silent()}.
     */
    private boolean canLog(Level level) {
        return !silent() && logger.isLoggable(level);
    }

    /**
     * Creates a new initialized configuration.
     *
     * @return a fully configured {@link PMDConfiguration}
     * @throws NullPointerException     if the project has not been set via {@link #fromProject}
     * @throws IllegalArgumentException if {@link #inputPaths()} is empty
     */
    @TestOnly
    PMDConfiguration initConfiguration() {
        ObjectTools.requireNotEmpty(inputPaths_, INPUT_PATHS);

        var config = new PMDConfiguration();

        config.addRelativizeRoots(relativizeRoots_);
        config.collectFilesRecursively(collectFilesRecursively_);

        if (prependAuxClasspath_ != null) {
            config.prependAuxClasspath(prependAuxClasspath_);
        }
        if (forcedLanguageVersion_ != null) {
            config.setForceLanguageVersion(forcedLanguageVersion_);
        }
        if (ignoreFile_ != null) {
            config.setIgnoreFilePath(ignoreFile_);
        }
        if (inputUri_ != null) {
            config.setInputUri(inputUri_);
        }

        if (incrementalAnalysis_ && cache_ != null) {
            config.setAnalysisCacheLocation(cache_.toAbsolutePath().toString());
        }

        if (ObjectTools.isNotEmpty(defaultLanguageVersions_)) {
            config.setDefaultLanguageVersions(defaultLanguageVersions_);
        }

        if (ObjectTools.isNotEmpty(excludes_)) {
            config.setExcludes(excludes_);
        }

        config.setFailOnError(failOnError_);
        config.setFailOnViolation(failOnViolation_);
        config.setIgnoreIncrementalAnalysis(!incrementalAnalysis_);

        config.setInputPathList(inputPaths_);

        config.setMinimumPriority(rulePriority_);

        if (reportFile_ != null) {
            config.setReportFile(reportFile_);
        }

        config.setReportFormat(reportFormat_);
        config.setReportProperties(reportProperties_);

        config.setRuleSets(List.copyOf(ruleSets_));

        config.setShowSuppressedViolations(showSuppressed_);
        config.setSourceEncoding(encoding_);
        config.setSuppressMarker(suppressedMarker_);
        config.setThreads(threads_);

        return config;
    }

    /**
     * Logs each violation and throws or warns depending on {@link #failOnViolation_}.
     * Accepts the pre-fetched violations list to avoid calling {@code report.getViolations()} twice.
     *
     * @param config     the PMD configuration
     * @param violations the violations to print
     * @throws ExitStatusException if {@code failOnViolation} is true and violations were found
     */
    private void printViolations(PMDConfiguration config,
                                 List<RuleViolation> violations) throws ExitStatusException {
        if (canLog(Level.WARNING)) {
            final String msgFormat = includeLineNumber_ ? MSG_FORMAT_WITH_LINE : MSG_FORMAT_NO_LINE_IN_LINK;
            for (var v : violations) {
                logger.warning(() -> msgFormat.formatted(
                        v.getFileId().getUriString(),
                        v.getBeginLine(),
                        v.getRule().getName(),
                        v.getRule().getExternalInfoUrl(),
                        v.getDescription()));
            }
        }

        var reportFilePath = config.getReportFilePath();
        var suffix = reportFilePath != null ? " See the report at: " + reportFilePath.toUri() : "";
        var summary = "%d rule violations were found.%s".formatted(violations.size(), suffix);

        if (config.isFailOnViolation()) {
            if (canLog(Level.SEVERE)) {
                logger.severe(summary);
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } else if (canLog(Level.WARNING)) {
            logger.warning(summary);
        }
    }
}