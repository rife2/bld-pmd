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

    private static final String EXCLUDES_NON_NULL = "The excludes values must all be non-null";
    private static final String INPUT_PATHS_NON_NULL = "The input paths values must all be non-null";
    private static final String MSG_FORMAT_NO_LINE_IN_LINK =
            "%s (line: %d)\n\t%s (%s)\n\t\t--> %s";
    private static final String MSG_FORMAT_WITH_LINE =
            "%s:%d\n\t%s (%s)\n\t\t--> %s";
    private static final String PMD_DIR = "pmd";
    private static final String RELATIVIZE_ROOTS_NON_NULL = "The relativize roots values must all be non-null";
    private static final String RULE_SETS_NON_NULL = "The rule sets values must all be non-null and non-empty";
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
     */
    @Override
    public void execute() throws Exception {
        performPmdAnalysis(initConfiguration());
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     *
     * @param cache the cache file path.
     * @return this operation
     * @see #cache(File)
     * @see #cache(String)
     */
    public PmdOperation cache(@NonNull Path cache) {
        Objects.requireNonNull(cache, "The cache file must not be null");
        cache_ = cache;
        return this;
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     *
     * @param cache the cache file
     * @return this operation
     * @see #cache(Path)
     * @see #cache(String)
     */
    public PmdOperation cache(@NonNull File cache) {
        Objects.requireNonNull(cache, "The cache file must not be null");
        cache_ = cache.toPath();
        return this;
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     *
     * @param cache the cache file path
     * @return this operation
     * @see #cache(Path)
     * @see #cache(File)
     */
    public PmdOperation cache(@NonNull String cache) {
        ObjectTools.requireNotEmpty(cache, "The cache file must not be null or empty");
        cache_ = Path.of(cache);
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
     * @param languageVersion one or more language versions
     * @return this operation
     * @see #defaultLanguageVersions(Collection)
     * @see #defaultLanguageVersions()
     */
    public PmdOperation defaultLanguageVersions(@NonNull LanguageVersion... languageVersion) {
        ObjectTools.requireNotEmpty(languageVersion, "The language version values must all be non-null");
        defaultLanguageVersions_.clear();
        defaultLanguageVersions_.addAll(List.of(languageVersion));
        return this;
    }

    /**
     * Sets the default language version to be used for all input files.
     *
     * @param languageVersion a collection of language versions
     * @return this operation
     * @see #defaultLanguageVersions(LanguageVersion...)
     * @see #defaultLanguageVersions()
     */
    public final PmdOperation defaultLanguageVersions(@NonNull Collection<LanguageVersion> languageVersion) {
        ObjectTools.requireNotEmpty(languageVersion, "The language version values must all be non-null");
        defaultLanguageVersions_.addAll(languageVersion);
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
     * @see #encoding(Charset)
     */
    public PmdOperation encoding(@NonNull String encoding) {
        Objects.requireNonNull(encoding, "The encoding must not be null");
        encoding_ = Charset.forName(encoding);
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
     * @see #encoding(String)
     */
    public PmdOperation encoding(@NonNull Charset encoding) {
        Objects.requireNonNull(encoding, "The encoding must not be null");
        encoding_ = encoding;
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
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludes(Path...)
     * @see #excludes(Collection)
     * @see #excludes()
     */
    public PmdOperation excludes(@NonNull Path... excludes) {
        ObjectTools.requireNotEmpty(excludes, EXCLUDES_NON_NULL);
        excludes_.addAll(List.of(excludes));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes paths to exclude
     * @return this operation
     * @see #excludes(Path...)
     * @see #excludes()
     */
    public final PmdOperation excludes(@NonNull Collection<Path> excludes) {
        ObjectTools.requireNotEmpty(excludes, EXCLUDES_NON_NULL);
        excludes_.addAll(excludes);
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesFiles(File...)
     * @see #excludes(Path...)
     * @since 1.2
     */
    public PmdOperation excludesFiles(@NonNull File... excludes) {
        ObjectTools.requireNotEmpty(excludes, EXCLUDES_NON_NULL);
        excludes_.addAll(CollectionTools.combineFilesToPaths(excludes));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes a collection of paths to exclude
     * @return this operation
     * @see #excludesFiles(File...)
     * @see #excludes(Path...)
     * @since 1.2
     */
    public final PmdOperation excludesFiles(@NonNull Collection<File> excludes) {
        ObjectTools.requireNotEmpty(excludes, EXCLUDES_NON_NULL);
        excludes_.addAll(CollectionTools.combineFilesToPaths(excludes));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesStrings(Collection)
     * @see #excludes(Path...)
     * @since 1.2
     */
    public PmdOperation excludesStrings(@NonNull String... excludes) {
        ObjectTools.requireNotEmpty(excludes, EXCLUDES_NON_NULL);
        excludes_.addAll(CollectionTools.combineStringsToPaths(excludes));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes a collection of paths to exclude
     * @return this operation
     * @see #excludesStrings(String...)
     * @see #excludes(Path...)
     * @since 1.2
     */
    public final PmdOperation excludesStrings(@NonNull Collection<String> excludes) {
        ObjectTools.requireNotEmpty(excludes, EXCLUDES_NON_NULL);
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
     */
    public PmdOperation forceLanguageVersion(@NonNull LanguageVersion languageVersion) {
        Objects.requireNonNull(languageVersion, "The force language version must not be null");
        forcedLanguageVersion_ = languageVersion;
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
     * <li>ruleSets={@link JavaRules#QUICK_START}, if not already sst</li>
     * <li>suppressedMarker={@code NOPMD}</li>
     * <li>threads=1</li>
     * </ul>
     *
     * @param project the project
     * @return this operation
     */
    public PmdOperation fromProject(@NonNull BaseProject project) {
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
     * @param ignoreFile the ignore file path
     * @return this operation
     * @see #ignoreFile(File)
     * @see #ignoreFile(String)
     */
    public PmdOperation ignoreFile(@NonNull Path ignoreFile) {
        Objects.requireNonNull(ignoreFile, "The ignore file must not be null");
        ignoreFile_ = ignoreFile;
        return this;
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     *
     * @param ignoreFile the ignore file
     * @return this operation
     * @see #ignoreFile(Path)
     * @see #ignoreFile(String)
     */
    public PmdOperation ignoreFile(@NonNull File ignoreFile) {
        Objects.requireNonNull(ignoreFile, "The ignore file must not be null");
        ignoreFile_ = ignoreFile.toPath();
        return this;
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     *
     * @param ignoreFile the ignore file path
     * @return this operation
     * @see #ignoreFile(Path)
     * @see #ignoreFile(File)
     */
    public PmdOperation ignoreFile(@NonNull String ignoreFile) {
        ObjectTools.requireNotEmpty(ignoreFile, "The ignore file must not be null or empty");
        ignoreFile_ = Path.of(ignoreFile);
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
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(Path...)
     * @see #inputPaths(Collection)
     * @see #inputPaths()
     */
    public PmdOperation inputPaths(@NonNull Path... inputPath) {
        ObjectTools.requireNotEmpty(inputPath, INPUT_PATHS_NON_NULL);
        inputPaths_.addAll(List.of(inputPath));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(File...)
     * @see #inputPathsFiles(Collection)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     */
    public PmdOperation inputPaths(@NonNull File... inputPath) {
        ObjectTools.requireNotEmpty(inputPath, INPUT_PATHS_NON_NULL);
        inputPaths_.addAll(CollectionTools.combineFilesToPaths(inputPath));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(String...)
     * @see #inputPathsStrings(Collection)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     */
    public PmdOperation inputPaths(@NonNull String... inputPath) {
        ObjectTools.requireNotEmpty(inputPath, INPUT_PATHS_NON_NULL);
        inputPaths_.addAll(CollectionTools.combineStringsToPaths(inputPath));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     */
    public final PmdOperation inputPaths(@NonNull Collection<Path> inputPath) {
        ObjectTools.requireNotEmpty(inputPath, INPUT_PATHS_NON_NULL);
        inputPaths_.addAll(inputPath);
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPaths(File...)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     */
    public final PmdOperation inputPathsFiles(@NonNull Collection<File> inputPath) {
        ObjectTools.requireNotEmpty(inputPath, INPUT_PATHS_NON_NULL);
        inputPaths_.addAll(CollectionTools.combineFilesToPaths(inputPath));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPaths(String...)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     */
    public PmdOperation inputPathsStrings(@NonNull Collection<String> inputPath) {
        ObjectTools.requireNotEmpty(inputPath, INPUT_PATHS_NON_NULL);
        inputPaths_.addAll(CollectionTools.combineStringsToPaths(inputPath));
        return this;
    }

    /**
     * Sets the minimum priority threshold when loading Rules from RuleSets.
     *
     * @param priority the minimum rule priority
     * @return this operation
     */
    public PmdOperation minimumPriority(@NonNull RulePriority priority) {
        Objects.requireNonNull(priority, "The priority must not be null");
        rulePriority_ = priority;
        return this;
    }

    /**
     * Performs the PMD analysis with the given config.
     *
     * @param config the configuration
     * @return the analysis results
     * @throws ExitStatusException if an error occurs
     */
    public PmdAnalysisResults performPmdAnalysis(PMDConfiguration config) throws ExitStatusException {
        try (var pmd = PmdAnalysis.create(config)) {
            var report = pmd.performAnalysisAndCollectReport();

            if (canLog(Level.INFO)) {
                logger.info(() -> "inputPaths" + inputPaths_);
                logger.info(() -> "ruleSets" + ruleSets_);
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
     * Prepend the specified classpath-like string to the current ClassLoader of the configuration.
     * If no ClassLoader is currently configured, the ClassLoader used to load the PMDConfiguration
     * class will be used as the parent ClassLoader of the created ClassLoader.
     * <p>
     * If the classpath String looks like a URL to a file (i.e., starts with {@code file://}) the
     * file will be read with each line representing an entry on the classpath.
     *
     * @param classpath one or more classpath entries
     * @return this operation
     * @see #prependAuxClasspath()
     */
    public PmdOperation prependAuxClasspath(@NonNull String... classpath) {
        ObjectTools.requireNotEmpty(classpath, "The classpath values must all be non-null and non-empty");
        prependAuxClasspath_ = String.join(File.pathSeparator, classpath);
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
     * @param relativeRoot one or more relative root paths
     * @return this operation
     * @see #relativizeRoots(Collection)
     * @see #relativizeRoots()
     */
    public PmdOperation relativizeRoots(@NonNull Path... relativeRoot) {
        ObjectTools.requireNotEmpty(relativeRoot, RELATIVIZE_ROOTS_NON_NULL);
        relativizeRoots_.addAll(CollectionTools.combine(relativeRoot));
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot one or more relative root paths
     * @return this operation
     * @see #relativizeRootsFiles(Collection)
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     */
    public PmdOperation relativizeRoots(@NonNull File... relativeRoot) {
        ObjectTools.requireNotEmpty(relativeRoot, RELATIVIZE_ROOTS_NON_NULL);
        relativizeRoots_.addAll(CollectionTools.combineFilesToPaths(relativeRoot));
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot one or more relative root paths
     * @return this operation
     * @see #relativizeRootsStrings(Collection)
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     */
    public PmdOperation relativizeRoots(@NonNull String... relativeRoot) {
        ObjectTools.requireNotEmpty(relativeRoot,
                "The relativize roots values must all be non-null and non-empty");
        relativizeRoots_.addAll(CollectionTools.combineStringsToPaths(relativeRoot));
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot a collection of relative root paths
     * @return this operation
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     */
    public final PmdOperation relativizeRoots(@NonNull Collection<Path> relativeRoot) {
        ObjectTools.requireNotEmpty(relativeRoot, RELATIVIZE_ROOTS_NON_NULL);
        relativizeRoots_.addAll(relativeRoot);
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
     * @param relativeRoot a collection of relative root paths
     * @return this operation
     * @see #relativizeRoots(File...)
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     */
    public final PmdOperation relativizeRootsFiles(@NonNull Collection<File> relativeRoot) {
        ObjectTools.requireNotEmpty(relativeRoot, RELATIVIZE_ROOTS_NON_NULL);
        relativizeRoots_.addAll(CollectionTools.combineFilesToPaths(relativeRoot));
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot a collection of relative root paths
     * @return this operation
     * @see #relativizeRoots(String...)
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     */
    public final PmdOperation relativizeRootsStrings(@NonNull Collection<String> relativeRoot) {
        ObjectTools.requireNotEmpty(relativeRoot, RELATIVIZE_ROOTS_NON_NULL);
        relativizeRoots_.addAll(CollectionTools.combineStringsToPaths(relativeRoot));
        return this;
    }

    /**
     * Sets the path to the report page.
     *
     * @param reportFile the report file path
     * @return this operation
     * @see #reportFile(File)
     * @see #reportFile(String)
     * @see #reportFile()
     */
    public PmdOperation reportFile(@NonNull Path reportFile) {
        reportFile_ = Objects.requireNonNull(reportFile, "The report file must not be null");
        return this;
    }

    /**
     * Sets the path to the report page.
     *
     * @param reportFile the report file
     * @return this operation
     * @see #reportFile(Path)
     * @see #reportFile(String)
     * @see #reportFile()
     */
    public PmdOperation reportFile(@NonNull File reportFile) {
        Objects.requireNonNull(reportFile, "The report file must not be null");
        reportFile_ = reportFile.toPath();
        return this;
    }

    /**
     * Sets the path to the report page.
     *
     * @param reportFile the report file path
     * @return this operation
     * @see #reportFile(Path)
     * @see #reportFile(File)
     * @see #reportFile()
     */
    public PmdOperation reportFile(@NonNull String reportFile) {
        ObjectTools.requireNotEmpty(reportFile, "The report file must not be null or empty");
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
     * @param reportFormat the report format
     * @return this operation
     */
    public PmdOperation reportFormat(@NonNull String reportFormat) {
        ObjectTools.requireNotEmpty(reportFormat, "The report format must not be null or empty");
        reportFormat_ = reportFormat;
        return this;
    }

    /**
     * Sets the Report properties. These are used to create the Renderer.
     *
     * @param reportProperties the report properties
     * @return this operation
     */
    public PmdOperation reportProperties(@NonNull Properties reportProperties) {
        Objects.requireNonNull(reportProperties, "The report properties must not be null");
        reportProperties_.putAll(reportProperties);
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
     * @param ruleSet one or more rule set paths
     * @return this operation
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSetsRules(Collection)
     * @see #ruleSets()
     */
    public PmdOperation ruleSets(@NonNull String... ruleSet) {
        ObjectTools.requireNotEmpty(ruleSet, RULE_SETS_NON_NULL);
        ruleSets_.addAll(List.of(ruleSet));
        return this;
    }

    /**
     * Sets rule set paths.
     *
     * @param ruleSet one or more rule set paths
     * @return this operation
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSetsRules(Collection)
     * @see #ruleSets()
     */
    public final PmdOperation ruleSets(@NonNull Collection<String> ruleSet) {
        ObjectTools.requireNotEmpty(ruleSet, RULE_SETS_NON_NULL);
        ruleSets_.addAll(ruleSet);
        return this;
    }

    /**
     * Sets rule set paths.
     *
     * @param ruleSet a collection of rule set paths
     * @return this operation
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection)
     * @see #ruleSetsRules(Collection)
     * @see #ruleSets()
     * @since 1.5
     */
    public final PmdOperation ruleSets(@NonNull JavaRules... ruleSet) {
        ObjectTools.requireNotEmpty(ruleSet, RULE_SETS_NON_NULL);
        ruleSets_.addAll(Arrays.stream(ruleSet).map(JavaRules::getCategory).toList());
        return this;
    }

    /**
     * Sets rule set paths.
     *
     * @param ruleSet a collection of rule set paths
     * @return this operation
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSets()
     * @since 1.5
     */
    public final PmdOperation ruleSetsRules(@NonNull Collection<JavaRules> ruleSet) {
        ObjectTools.requireNotEmpty(ruleSet, RULE_SETS_NON_NULL);
        ruleSets_.addAll(CollectionTools.combine(ruleSet).stream().map(JavaRules::getCategory).toList());
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
     * @param suppressedMarker the suppressed marker token
     * @return this operation
     */
    public PmdOperation suppressedMarker(@NonNull String suppressedMarker) {
        ObjectTools.requireNotEmpty(suppressedMarker, "The suppressed marker must not be null or empty");
        suppressedMarker_ = suppressedMarker;
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
     */
    public PmdOperation uri(@NonNull URI inputUri) {
        Objects.requireNonNull(inputUri, "The input URI must not be null");
        inputUri_ = inputUri;
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
     * @throws NullPointerException if the project has not been set via {@link #fromProject}
     */
    @TestOnly
    PMDConfiguration initConfiguration() {
        ObjectTools.requireNotEmpty(inputPaths_, INPUT_PATHS_NON_NULL);

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