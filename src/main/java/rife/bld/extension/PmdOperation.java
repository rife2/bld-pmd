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
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.reporting.Report;
import rife.bld.BaseProject;
import rife.bld.extension.pmd.PmdAnalysisResults;
import rife.bld.operations.AbstractOperation;
import rife.bld.operations.exceptions.ExitStatusException;

import java.io.File;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Performs static code analysis with <a href="https://pmd.github.io/">PMD</a>.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class PmdOperation extends AbstractOperation<PmdOperation> {
    /**
     * The default logger.
     */
    public static final Logger LOGGER = Logger.getLogger(PmdOperation.class.getName());
    /**
     * The default rule set.
     * <p>
     * Set to: {@code rulesets/java/quickstart.xml}
     */
    public static final String RULE_SET_DEFAULT = "rulesets/java/quickstart.xml";

    private static final String PMD_DIR = "pmd";
    private final Collection<Path> excludes_ = new ArrayList<>();
    private final Collection<Path> inputPaths_ = new ArrayList<>();
    private final Collection<Path> relativizeRoots_ = new ArrayList<>();
    private final Collection<String> ruleSets_ = new ArrayList<>();
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
    private Collection<LanguageVersion> languageVersions_ = new ArrayList<>();
    private String prependClasspath_;
    private BaseProject project_;
    private Path reportFile_;
    private String reportFormat_ = "text";
    private Properties reportProperties_;
    private RulePriority rulePriority_ = RulePriority.LOW;
    private boolean showSuppressed_;
    private String suppressedMarker_ = "NOPMD";
    private int threads_ = 1;

    /**
     * Adds paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludes(Path...)
     * @since 1.2.0
     */
    public PmdOperation addExcludes(Path... excludes) {
        return addExcludes(List.of(excludes));
    }

    /**
     * Adds paths to exclude from the analysis.
     *
     * @param excludes paths to exclude
     * @return this operation
     * @see #excludes(Collection)
     * @since 1.2.0
     */
    public PmdOperation addExcludes(Collection<Path> excludes) {
        excludes_.addAll(excludes);
        return this;
    }

    /**
     * Adds paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesFiles(Collection)
     * @since 1.2.0
     */
    public PmdOperation addExcludesFiles(Collection<File> excludes) {
        return addExcludes(excludes.stream().map(File::toPath).toList());
    }

    /**
     * Adds paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesFiles(File...)
     * @since 1.2.0
     */
    public PmdOperation addExcludesFiles(File... excludes) {
        return addExcludesFiles(List.of(excludes));
    }

    /**
     * Adds paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesStrings(Collection)
     * @since 1.2.0
     */
    public PmdOperation addExcludesStrings(Collection<String> excludes) {
        return addExcludes(excludes.stream().map(Paths::get).toList());
    }

    /**
     * Adds paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesStrings(String...)
     * @since 1.2.0
     */
    public PmdOperation addExcludesStrings(String... excludes) {
        return addExcludesStrings(List.of(excludes));
    }

    /**
     * Adds paths to source files or directories containing source files to analyze.\
     *
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(Path...)
     */
    public PmdOperation addInputPaths(Path... inputPath) {
        return addInputPaths(List.of(inputPath));
    }

    /**
     * Adds paths to source files or directories containing source files to analyze.
     *
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(File...)
     */
    public PmdOperation addInputPaths(File... inputPath) {
        return addInputPathsFiles(List.of(inputPath));
    }

    /**
     * Adds paths to source files or directories containing source files to analyze.
     *
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(String...)
     */
    public PmdOperation addInputPaths(String... inputPath) {
        return addInputPathsStrings(List.of(inputPath));
    }

    /**
     * Adds paths to source files or directories containing source files to analyze.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPaths(Collection)
     */
    public PmdOperation addInputPaths(Collection<Path> inputPath) {
        inputPaths_.addAll(inputPath);
        return this;
    }

    /**
     * Adds paths to source files or directories containing source files to analyze.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPathsFiles(Collection)
     */
    public PmdOperation addInputPathsFiles(Collection<File> inputPath) {
        return addInputPaths(inputPath.stream().map(File::toPath).toList());
    }

    /**
     * Adds paths to source files or directories containing source files to analyze.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPathsStrings(Collection)
     */
    public PmdOperation addInputPathsStrings(Collection<String> inputPath) {
        return addInputPaths(inputPath.stream().map(Paths::get).toList());
    }

    /**
     * Adds new rule set paths.
     * <p>
     * The built-in rule set paths are:
     * <ul>
     *     <li>{@code rulesets/java/quickstart.xml}</li>
     *     <li>{@code category/java/bestpractices.xml}</li>
     *     <li>{@code category/java/codestyle.xml}</li>
     *     <li>{@code category/java/design.xml}</li>
     *     <li>{@code category/java/documentation.xml}</li>
     *     <li>{@code category/java/errorprone.xml}</li>
     *     <li>{@code category/java/multithreading.xml}</li>
     *     <li>{@code category/java/performance.xml}</li>
     *     <li>{@code category/java/security.xml}</li>
     * </ul>
     *
     * @param ruleSet one or more rule set
     * @return this operation
     * @see #ruleSets(String...)
     */
    public PmdOperation addRuleSet(String... ruleSet) {
        return addRuleSet(List.of(ruleSet));
    }

    /**
     * Adds new rule set paths.
     * <p>
     * The built-in rule set paths are:
     * <ul>
     *     <li>{@code rulesets/java/quickstart.xml}</li>
     *     <li>{@code category/java/bestpractices.xml}</li>
     *     <li>{@code category/java/codestyle.xml}</li>
     *     <li>{@code category/java/design.xml}</li>
     *     <li>{@code category/java/documentation.xml}</li>
     *     <li>{@code category/java/errorprone.xml}</li>
     *     <li>{@code category/java/multithreading.xml}</li>
     *     <li>{@code category/java/performance.xml}</li>
     *     <li>{@code category/java/security.xml}</li>
     * </ul>
     *
     * @param ruleSet a collection of rule set paths
     * @return this operation
     * @see #ruleSets(Collection)
     */
    public PmdOperation addRuleSet(Collection<String> ruleSet) {
        ruleSets_.addAll(ruleSet);
        return this;
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     */
    public PmdOperation cache(Path cache) {
        cache_ = cache;
        return this;
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     */
    public PmdOperation cache(File cache) {
        return cache(cache.toPath());
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     */
    public PmdOperation cache(String cache) {
        return cache(Path.of(cache));
    }

    /**
     * When specified, any directory mentioned with {@link #inputPaths()} will only be searched for files that are
     * direct children. By default, subdirectories are recursively included.
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
     * @param languageVersion one or more language version
     * @return this operation
     */
    public PmdOperation defaultLanguageVersions(LanguageVersion... languageVersion) {
        return languageVersions(List.of(languageVersion));
    }

    /**
     * Sets the default language version to be used for all input files.
     *
     * @param languageVersion a collection language version
     * @return this operation
     */
    public PmdOperation defaultLanguageVersions(Collection<LanguageVersion> languageVersion) {
        languageVersions_.addAll(languageVersion);
        return this;
    }

    /**
     * <p>Specifies the character set encoding of the source code files. The default is {@code UTF-8}.</p>
     *
     * <p>The valid values are the standard character sets of {@link java.nio.charset.Charset Charset}.</p>
     */
    public PmdOperation encoding(String encoding) {
        return encoding(Charset.forName(encoding));
    }

    /**
     * <p>Specifies the character set encoding of the source code files. The default is
     * {@link StandardCharsets#UTF_8 UTF-8}.</p>
     *
     * <p>The valid values are the standard character sets of {@link java.nio.charset.Charset Charset}.</p>
     */
    public PmdOperation encoding(Charset encoding) {
        encoding_ = encoding;
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #addExcludes(Path...)
     */
    public PmdOperation excludes(Path... excludes) {
        excludes(List.of(excludes));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes paths to exclude
     * @return this operation
     * @see #addExcludes(Collection)
     */
    public PmdOperation excludes(Collection<Path> excludes) {
        excludes_.clear();
        excludes_.addAll(excludes);
        return this;
    }

    /**
     * Returns the paths to exclude from the analysis.
     *
     * @return the exclude paths
     */
    public Collection<Path> excludes() {
        return excludes_;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesFiles(Collection)
     * @since 1.2.0
     */
    public PmdOperation excludesFiles(Collection<File> excludes) {
        excludes(excludes.stream().map(File::toPath).toList());
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesFiles(File...)
     * @since 1.2.0
     */
    public PmdOperation excludesFiles(File... excludes) {
        return excludesFiles(List.of(excludes));
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesStrings(Collection)
     * @since 1.2.0
     */
    public PmdOperation excludesStrings(Collection<String> excludes) {
        excludes(excludes.stream().map(Paths::get).toList());
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesStrings(String...)
     * @since 1.2.0
     */
    public PmdOperation excludesStrings(String... excludes) {
        return excludesStrings(List.of(excludes));
    }

    /**
     * Performs the PMD code analysis operation.
     */
    @Override
    public void execute() throws Exception {
        if (project_ == null) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.log(Level.SEVERE, "A project is required to run this operation.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }

        var commandName = project_.getCurrentCommandName();
        performPmdAnalysis(commandName, initConfiguration(commandName));
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
    public PmdOperation forceLanguageVersion(LanguageVersion languageVersion) {
        forcedLanguageVersion_ = languageVersion;
        return this;
    }

    /**
     * Configures a PMD operation from a {@link BaseProject}.
     *
     * <p>
     * The defaults are:
     * <ul>
     * <li>cache={@code build/pmd/pmd-cache}</li>
     * <li>encoding={@code UTF-9}</li>
     * <li>incrementAnalysis={@code true}</li>
     * <li>inputPaths={@code [src/main, src/test]}</li>
     * <li>reportFile={@code build/pmd/pmd-report-txt}</li>
     * <li>reportFormat={@code text}</li>
     * <li>rulePriority={@code LOW}</li>
     * <li>ruleSets={@code [rulesets/java/quickstart.xml]}</li>
     * <li>suppressedMarker={@code NOPMD}</li>
     * </ul>
     *
     * @param project the project
     * @return this operation
     */
    public PmdOperation fromProject(BaseProject project) {
        project_ = project;

        inputPaths(project.srcMainDirectory(), project.srcTestDirectory());
        ruleSets_.add(RULE_SET_DEFAULT);
        return this;
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     *
     * @param ignoreFile the ignore file path
     * @return this operation
     */
    public PmdOperation ignoreFile(Path ignoreFile) {
        ignoreFile_ = ignoreFile;
        return this;
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     *
     * @param ignoreFile the ignore file path
     * @return this operation
     */
    public PmdOperation ignoreFile(File ignoreFile) {
        return ignoreFile(ignoreFile.toPath());
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     *
     * @param ignoreFile the ignore file path
     * @return this operation
     */
    public PmdOperation ignoreFile(String ignoreFile) {
        return ignoreFile(Path.of(ignoreFile));
    }

    /**
     * Enables or disables including the line number for the beginning of the violation in the analyzed source file URI.
     * <p>
     * While clicking on the URI works in IntelliJ IDEA, Visual Studio Code, etc.; it might not in terminal emulators.
     * <p>
     * Default: {@code TRUE}
     */
    public PmdOperation includeLineNumber(boolean includeLineNumber) {
        includeLineNumber_ = includeLineNumber;
        return this;
    }

    /**
     * Enables or disables incremental analysis.
     */
    public PmdOperation incrementalAnalysis(boolean incrementalAnalysis) {
        incrementalAnalysis_ = incrementalAnalysis;
        return this;
    }

    /**
     * Creates a new initialized configuration.
     *
     * @param commandName the command name
     * @return this operation
     */
    public PMDConfiguration initConfiguration(String commandName) {
        var config = new PMDConfiguration();

        // addRelativizeRoots
        config.addRelativizeRoots(relativizeRoots_.stream().toList());

        // collectFilesRecursively
        if (!collectFilesRecursively_) {
            config.collectFilesRecursively(false);
        }

        // prependAuxClasspath
        if (prependClasspath_ != null) {
            config.prependAuxClasspath(prependClasspath_);
        }

        // setAnalysisCacheLocation
        if (cache_ == null && project_ != null && incrementalAnalysis_) {
            config.setAnalysisCacheLocation(
                    Paths.get(project_.buildDirectory().getPath(), PMD_DIR, PMD_DIR + "-cache").toFile().getAbsolutePath());
        } else if (cache_ != null) {
            config.setAnalysisCacheLocation(cache_.toFile().getAbsolutePath());
        }

        // setDefaultLanguageVersions
        if (languageVersions_ != null) {
            config.setDefaultLanguageVersions(languageVersions_.stream().toList());
        }

        // setExcludes
        if (!excludes_.isEmpty()) {
            config.setExcludes(excludes_.stream().toList());
        }

        // setFailOnError
        config.setFailOnError(failOnError_);
        // setFailOnViolation
        config.setFailOnViolation(failOnViolation_);

        // setForceLanguageVersion
        if (forcedLanguageVersion_ != null) {
            config.setForceLanguageVersion(forcedLanguageVersion_);
        }

        // setIgnoreFilePath
        if (ignoreFile_ != null) {
            config.setIgnoreFilePath(ignoreFile_);
        }

        // setIgnoreIncrementalAnalysis
        config.setIgnoreIncrementalAnalysis(!incrementalAnalysis_);

        // setInputPathList
        if (inputPaths_.isEmpty()) {
            throw new IllegalArgumentException(commandName + ": InputPaths required.");
        } else {
            config.setInputPathList(inputPaths_.stream().toList());
        }

        // setInputUri
        if (inputUri_ != null) {
            config.setInputUri(inputUri_);
        }

        // setMinimumPriority
        config.setMinimumPriority(rulePriority_);

        // setReportFile
        if (project_ != null) {
            config.setReportFile(Objects.requireNonNullElseGet(reportFile_,
                    () -> Paths.get(project_.buildDirectory().getPath(),
                            PMD_DIR, PMD_DIR + "-report." + reportFormat_)));
        } else {
            config.setReportFile(reportFile_);
        }

        // setReportFormat
        config.setReportFormat(reportFormat_);

        // setReportProperties
        if (reportProperties_ != null) {
            config.setReportProperties(reportProperties_);
        }

        // setRuleSets
        config.setRuleSets(ruleSets_.stream().toList());

        // setShowSuppressedViolations
        config.setShowSuppressedViolations(showSuppressed_);
        // setSourceEncoding
        config.setSourceEncoding(encoding_);
        // setSuppressMarker
        config.setSuppressMarker(suppressedMarker_);

        // setThreads
        config.setThreads(threads_);

        return config;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param inputPath one or more paths
     * @return this operation
     * @see #addInputPaths(Path...)
     */
    public PmdOperation inputPaths(Path... inputPath) {
        return inputPaths(List.of(inputPath));
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     * <p>
     * Previous entries are disregarded.
     *
     * @param inputPath one or more paths
     * @return this operation
     * @see #addInputPaths(File...)
     */
    public PmdOperation inputPaths(File... inputPath) {
        return inputPathsFiles(List.of(inputPath));
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     * <p>
     * Previous entries are disregarded.
     *
     * @param inputPath one or more paths
     * @return this operation
     * @see #addInputPaths(String...)
     */
    public PmdOperation inputPaths(String... inputPath) {
        return inputPathsStrings(List.of(inputPath));
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     * <p>
     * Previous entries are disregarded.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #addInputPaths(Collection)
     */
    public PmdOperation inputPaths(Collection<Path> inputPath) {
        inputPaths_.clear();
        inputPaths_.addAll(inputPath);
        return this;
    }

    /**
     * Returns paths to source files or directories containing source files to analyze.
     *
     * @return the input paths
     */
    public Collection<Path> inputPaths() {
        return inputPaths_;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     * <p>
     * Previous entries are disregarded.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #addInputPathsFiles(Collection)
     */
    public PmdOperation inputPathsFiles(Collection<File> inputPath) {
        return inputPaths(inputPath.stream().map(File::toPath).toList());
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     * <p>
     * Previous entries are disregarded.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #addInputPathsStrings(Collection)
     */
    public PmdOperation inputPathsStrings(Collection<String> inputPath) {
        return inputPaths(inputPath.stream().map(Paths::get).toList());
    }

    /**
     * Sets the default language versions.
     *
     * @param languageVersion one or more language versions
     * @return this operation
     */
    public PmdOperation languageVersions(LanguageVersion... languageVersion) {
        return languageVersions(List.of(languageVersion));
    }

    /**
     * Sets the default language versions.
     *
     * @param languageVersions a collection language version
     * @return this operation
     */
    public PmdOperation languageVersions(Collection<LanguageVersion> languageVersions) {
        languageVersions_ = languageVersions;
        return this;
    }

    /**
     * Returns the default language versions.
     *
     * @return the language versions
     */
    public Collection<LanguageVersion> languageVersions() {
        return languageVersions_;
    }

    /**
     * Sets the minimum priority threshold when loading Rules from RuleSets.
     *
     * @return this operation
     */
    public PmdOperation minimumPriority(RulePriority priority) {
        rulePriority_ = priority;
        return this;
    }

    /**
     * Performs the PMD analysis with the given config.
     *
     * @param commandName the command name
     * @param config      the configuration
     * @return the number of violations
     * @throws ExitStatusException if an error occurs
     */
    public PmdAnalysisResults performPmdAnalysis(String commandName, PMDConfiguration config)
            throws ExitStatusException {
        try (var pmd = PmdAnalysis.create(config)) {
            var report = pmd.performAnalysisAndCollectReport();

            if (LOGGER.isLoggable(Level.INFO) && !silent()) {
                LOGGER.log(Level.INFO, "[{0}] inputPaths{1}", new Object[]{commandName, inputPaths_});
                LOGGER.log(Level.INFO, "[{0}] ruleSets{1}", new Object[]{commandName, ruleSets_});
            }

            var numViolations = report.getViolations().size();
            if (numViolations > 0) {
                printViolations(commandName, config, report);
            } else if (pmd.getReporter().numErrors() > 0 && failOnError_) {
                throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
            }

            var rulesChecked = 0;
            var rules = pmd.getRulesets();
            if (!rules.isEmpty()) {
                for (var rule : rules) {
                    rulesChecked += rule.getRules().size();
                }
            }

            var result = new PmdAnalysisResults(
                    numViolations,
                    report.getSuppressedViolations().size(),
                    pmd.getReporter().numErrors(),
                    report.getProcessingErrors().size(),
                    report.getConfigurationErrors().size(),
                    rulesChecked
            );

            if (!silent()) {
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(String.format("[%s] %d rules were checked.", commandName, result.rulesChecked()));
                }

                if (LOGGER.isLoggable(Level.WARNING)) {
                    if (result.processingErrors() > 0) {
                        for (var err : report.getProcessingErrors()) {
                            LOGGER.warning(String.format("[%s] %s", commandName, err.getMsg()));
                        }
                    }

                    if (result.configurationErrors() > 0) {
                        for (var err : report.getConfigurationErrors()) {
                            LOGGER.warning(String.format("[%s] %s", commandName, err.issue()));
                        }
                    }
                }

                if (LOGGER.isLoggable(Level.FINEST)) {
                    LOGGER.finest(result.toString());
                }
            }

            return result;
        }
    }

    /**
     * Prepend the specified classpath like string to the current ClassLoader of the configuration. If no ClassLoader
     * is currently configured, the ClassLoader used to load the PMDConfiguration class will be used as the parent
     * ClassLoader of the created ClassLoader.
     * <p>
     * If the classpath String looks like a URL to a file (i.e., starts with {@code file://}) the file will be read with
     * each line representing an entry on the classpath.
     *
     * @param classpath one or more classpath
     * @return this operation
     */
    public PmdOperation prependAuxClasspath(String... classpath) {
        prependClasspath_ = String.join(File.pathSeparator, classpath);
        return this;
    }

    /**
     * Returns the prepended classpath.
     *
     * @return the classpath
     */
    public String prependAuxClasspath() {
        return prependClasspath_;
    }

    private void printViolations(String commandName, PMDConfiguration config, Report report)
            throws ExitStatusException {
        for (var v : report.getViolations()) {
            if (LOGGER.isLoggable(Level.WARNING) && !silent()) {
                final String msg;
                if (includeLineNumber_) {
                    msg = "[%s] %s:%d\n\t%s (%s)\n\t\t--> %s";
                } else {
                    msg = "[%s] %s (line: %d)\n\t%s (%s)\n\t\t--> %s";
                }
                LOGGER.log(Level.WARNING,
                        String.format(msg,
                                commandName,
                                v.getFileId().getUriString(),
                                v.getBeginLine(),
                                v.getRule().getName(),
                                v.getRule().getExternalInfoUrl(),
                                v.getDescription()));
            }
        }

        var violations = new StringBuilder(
                String.format("[%s] %d rule violations were found.", commandName, report.getViolations().size()));

        if (config.getReportFilePath() != null) {
            violations.append(" See the report at: ").append(config.getReportFilePath().toUri());
        }

        if (config.isFailOnViolation()) {
            if (LOGGER.isLoggable(Level.SEVERE) && !silent()) {
                LOGGER.log(Level.SEVERE, violations.toString());
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } else if (LOGGER.isLoggable(Level.WARNING) && !silent()) {
            LOGGER.warning(violations.toString());
        }
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot one or more relative root paths
     * @return this operation
     * @see #relativizeRoots(Collection)
     */
    public PmdOperation relativizeRoots(Path... relativeRoot) {
        return relativizeRoots(List.of(relativeRoot));
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot one or more relative root paths
     * @return this operation
     * @see #relativizeRootsFiles(Collection)
     */
    public PmdOperation relativizeRoots(File... relativeRoot) {
        return relativizeRootsFiles(List.of(relativeRoot));
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot one or more relative root paths
     * @return this operation
     * @see #relativizeRootsStrings(Collection)
     */
    public PmdOperation relativizeRoots(String... relativeRoot) {
        return relativizeRootsStrings(List.of(relativeRoot));
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot a collection of relative root paths
     * @return this operation
     * @see #relativizeRoots(Path...)
     */
    public PmdOperation relativizeRoots(Collection<Path> relativeRoot) {
        relativizeRoots_.addAll(relativeRoot);
        return this;
    }

    /**
     * Returns paths to shorten paths that are output in the report.
     *
     * @return the relative root paths
     */
    public Collection<Path> relativizeRoots() {
        return relativizeRoots_;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot a collection of relative root paths
     * @return this operation
     * @see #relativizeRoots(File...)
     */
    public PmdOperation relativizeRootsFiles(Collection<File> relativeRoot) {
        return relativizeRoots(relativeRoot.stream().map(File::toPath).toList());
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot a collection of relative root paths
     * @return this operation
     * @see #relativizeRoots(String...)
     */
    public PmdOperation relativizeRootsStrings(Collection<String> relativeRoot) {
        return relativizeRoots(relativeRoot.stream().map(Paths::get).toList());
    }

    /**
     * Sets the path to the report page.
     *
     * @param reportFile the report file path
     * @return this operation
     */
    public PmdOperation reportFile(Path reportFile) {
        reportFile_ = reportFile;
        return this;
    }

    /**
     * Sets the path to the report page.
     *
     * @param reportFile the report file path
     * @return this operation
     */
    public PmdOperation reportFile(File reportFile) {
        return reportFile(reportFile.toPath());
    }

    /**
     * Sets the path to the report page.
     *
     * @param reportFile the report file path
     * @return this operation
     */
    public PmdOperation reportFile(String reportFile) {
        return reportFile(Paths.get(reportFile));
    }

    /**
     * Returns the path to the report page.
     *
     * @return the path
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
    public PmdOperation reportFormat(String reportFormat) {
        reportFormat_ = reportFormat;
        return this;
    }

    /**
     * Sets the Report properties. These are used to create the Renderer.
     *
     * @param reportProperties the report properties
     * @return this operation
     */
    public PmdOperation reportProperties(Properties reportProperties) {
        reportProperties_ = reportProperties;
        return this;
    }

    /**
     * Sets new rule set paths, disregarding any previous entries.
     * <p>
     * The built-in rule set paths are:
     * <ul>
     *     <li>{@code rulesets/java/quickstart.xml}</li>
     *     <li>{@code category/java/bestpractices.xml}</li>
     *     <li>{@code category/java/codestyle.xml}</li>
     *     <li>{@code category/java/design.xml}</li>
     *     <li>{@code category/java/documentation.xml}</li>
     *     <li>{@code category/java/errorprone.xml}</li>
     *     <li>{@code category/java/multithreading.xml}</li>
     *     <li>{@code category/java/performance.xml}</li>
     *     <li>{@code category/java/security.xml}</li>
     * </ul>
     *
     * @param ruleSet one or more rule set
     * @return this operation
     * @see #addRuleSet(String...)
     */
    public PmdOperation ruleSets(String... ruleSet) {
        return ruleSets(List.of(ruleSet));
    }

    /**
     * Sets new rule set paths, disregarding any previous entries.
     * <p>
     * The built-in rule set paths are:
     * <ul>
     *     <li>{@code rulesets/java/quickstart.xml}</li>
     *     <li>{@code category/java/bestpractices.xml}</li>
     *     <li>{@code category/java/codestyle.xml}</li>
     *     <li>{@code category/java/design.xml}</li>
     *     <li>{@code category/java/documentation.xml}</li>
     *     <li>{@code category/java/errorprone.xml}</li>
     *     <li>{@code category/java/multithreading.xml}</li>
     *     <li>{@code category/java/performance.xml}</li>
     *     <li>{@code category/java/security.xml}</li>
     * </ul>
     *
     * @param ruleSet a collection of rule set paths
     * @return this operation
     * @see #addRuleSet(Collection)
     */
    public PmdOperation ruleSets(Collection<String> ruleSet) {
        ruleSets_.clear();
        ruleSets_.addAll(ruleSet);
        return this;
    }

    /**
     * Returns the rule set paths.
     *
     * @return the rule sets
     */
    public Collection<String> ruleSets() {
        return ruleSets_;
    }

    /**
     * Enables or disables adding the suppressed rule violations to the report.
     *
     * @param showSuppressed {@code true} or {@code false}
     * @return this operation
     */
    public PmdOperation showSuppressed(boolean showSuppressed) {
        showSuppressed_ = showSuppressed;
        return this;
    }

    /**
     * Specifies the comment token that marks lines which should be ignored. The default is {@code NOPMD}.
     *
     * @param suppressedMarker the suppressed marker
     * @return this operation
     */
    public PmdOperation suppressedMarker(String suppressedMarker) {
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
    public PmdOperation uri(URI inputUri) {
        inputUri_ = inputUri;
        return this;
    }
}
