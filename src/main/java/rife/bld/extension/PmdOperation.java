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

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.lang.LanguageVersion;
import net.sourceforge.pmd.lang.rule.RulePriority;
import net.sourceforge.pmd.reporting.RuleViolation;
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
@SuppressWarnings("PMD.CouplingBetweenObjects")
@SuppressFBWarnings("PATH_TRAVERSAL_IN")
public class PmdOperation extends AbstractOperation<PmdOperation> {

    /**
     * The default logger.
     */
    public static final Logger LOGGER = Logger.getLogger(PmdOperation.class.getName());

    private static final String MSG_FORMAT_NO_LINE =
            "[%s] %s (line: %d)\n\t%s (%s)\n\t\t--> %s";
    private static final String MSG_FORMAT_WITH_LINE =
            "[%s] %s:%d\n\t%s (%s)\n\t\t--> %s";
    private static final String PMD_DIR = "pmd";
    private final List<LanguageVersion> defaultLanguageVersions_ = new ArrayList<>();
    private final List<Path> excludes_ = new ArrayList<>();
    private final List<Path> inputPaths_ = new ArrayList<>();
    private final List<Path> relativizeRoots_ = new ArrayList<>();
    private final Properties reportProperties_ = new Properties();
    private final Set<String> ruleSets_ = new LinkedHashSet<>();

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
    private BaseProject project_;
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
        if (project_ == null) {
            if (canLog(Level.SEVERE)) {
                LOGGER.severe("A project is required to run this operation.");
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        }

        var commandName = project_.getCurrentCommandName();
        performPmdAnalysis(commandName, initConfiguration(commandName));
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     *
     * @param cache the cache file path
     * @return this operation
     * @see #cache(File)
     * @see #cache(String)
     * @since 1.0
     */
    public PmdOperation cache(Path cache) {
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
     * @since 1.0
     */
    public PmdOperation cache(File cache) {
        return cache(cache.toPath());
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     *
     * @param cache the cache file path
     * @return this operation
     * @see #cache(Path)
     * @see #cache(File)
     * @since 1.0
     */
    public PmdOperation cache(String cache) {
        return cache(Path.of(cache));
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
     * @see #defaultLanguageVersions(Collection...)
     * @see #defaultLanguageVersions()
     * @since 1.0
     */
    public PmdOperation defaultLanguageVersions(LanguageVersion... languageVersion) {
        if (ObjectTools.isNotEmpty(languageVersion)) {
            defaultLanguageVersions(List.of(languageVersion));
        }
        return this;
    }

    /**
     * Sets the default language version to be used for all input files.
     *
     * @param languageVersion a collection of language versions
     * @return this operation
     * @see #defaultLanguageVersions(LanguageVersion...)
     * @see #defaultLanguageVersions()
     * @since 1.0
     */
    @SafeVarargs
    public final PmdOperation defaultLanguageVersions(Collection<LanguageVersion>... languageVersion) {
        defaultLanguageVersions_.addAll(CollectionTools.combine(languageVersion));
        return this;
    }

    /**
     * Returns the default language versions.
     *
     * @return the language versions
     * @see #defaultLanguageVersions(LanguageVersion...)
     * @see #defaultLanguageVersions(Collection...)
     * @since 1.0
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
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
     * @since 1.0
     */
    public PmdOperation encoding(String encoding) {
        return encoding(Charset.forName(encoding));
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
     * @since 1.0
     */
    public PmdOperation encoding(Charset encoding) {
        encoding_ = encoding;
        return this;
    }

    /**
     * Sets paths to exclude from the analysis, replacing any previous entries.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludes(boolean, Path...)
     * @see #excludes(Collection...)
     * @see #excludes()
     * @since 1.0
     */
    public PmdOperation excludes(Path... excludes) {
        return excludes(true, excludes);
    }

    /**
     * Sets paths to exclude from the analysis, replacing any previous entries.
     *
     * @param excludes paths to exclude
     * @return this operation
     * @see #excludes(boolean, Collection...)
     * @see #excludes(Path...)
     * @see #excludes()
     * @since 1.0
     */
    @SafeVarargs
    public final PmdOperation excludes(Collection<Path>... excludes) {
        return excludes(true, excludes);
    }

    /**
     * Returns the paths to exclude from the analysis.
     *
     * @return the exclude paths
     * @see #excludes(Path...)
     * @see #excludes(Collection...)
     * @since 1.0
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<Path> excludes() {
        return excludes_;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param clear    whether to clear existing entries before adding the new ones
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludes(Path...)
     * @see #excludes(Collection...)
     * @see #excludes()
     * @since 1.5.0
     */
    public PmdOperation excludes(boolean clear, Path... excludes) {
        if (clear) {
            excludes_.clear();
        }
        excludes_.addAll(CollectionTools.combine(excludes));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param clear    whether to clear existing entries before adding the new ones
     * @param excludes paths to exclude
     * @return this operation
     * @see #excludes(Path...)
     * @see #excludes()
     * @since 1.5.0
     */
    @SafeVarargs
    public final PmdOperation excludes(boolean clear, Collection<Path>... excludes) {
        if (clear) {
            excludes_.clear();
        }
        excludes_.addAll(CollectionTools.combine(excludes));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis, replacing any previous entries.
     *
     * @param excludes a collection of paths to exclude
     * @return this operation
     * @see #excludesFiles(boolean, Collection...)
     * @see #excludesFiles(File...)
     * @see #excludes(Path...)
     * @since 1.2.0
     */
    @SafeVarargs
    public final PmdOperation excludesFiles(Collection<File>... excludes) {
        return excludesFiles(true, excludes);
    }

    /**
     * Sets paths to exclude from the analysis, replacing any previous entries.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesFiles(boolean, File...)
     * @see #excludesFiles(Collection...)
     * @see #excludes(Path...)
     * @since 1.2.0
     */
    public PmdOperation excludesFiles(File... excludes) {
        return excludesFiles(true, excludes);
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param clear    whether to clear existing entries before adding the new ones
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesFiles(File...)
     * @see #excludes(Path...)
     * @since 1.5.0
     */
    public PmdOperation excludesFiles(boolean clear, File... excludes) {
        if (clear) {
            excludes_.clear();
        }
        excludes_.addAll(CollectionTools.combineFilesToPaths(excludes));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param clear    whether to clear existing entries before adding the new ones
     * @param excludes a collection of paths to exclude
     * @return this operation
     * @see #excludesFiles(File...)
     * @see #excludes(Path...)
     * @since 1.5.0
     */
    @SafeVarargs
    public final PmdOperation excludesFiles(boolean clear, Collection<File>... excludes) {
        if (clear) {
            excludes_.clear();
        }
        excludes_.addAll(CollectionTools.combineFilesToPaths(excludes));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis, replacing any previous entries.
     *
     * @param excludes a collection of paths to exclude
     * @return this operation
     * @see #excludesStrings(boolean, Collection...)
     * @see #excludesStrings(String...)
     * @see #excludes(Path...)
     * @since 1.2.0
     */
    @SafeVarargs
    public final PmdOperation excludesStrings(Collection<String>... excludes) {
        return excludesStrings(true, excludes);
    }

    /**
     * Sets paths to exclude from the analysis, replacing any previous entries.
     *
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesStrings(boolean, String...)
     * @see #excludesStrings(Collection...)
     * @see #excludes(Path...)
     * @since 1.2.0
     */
    public PmdOperation excludesStrings(String... excludes) {
        return excludesStrings(true, excludes);
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param clear    whether to clear existing entries before adding the new ones
     * @param excludes one or more paths to exclude
     * @return this operation
     * @see #excludesStrings(Collection...)
     * @see #excludes(Path...)
     * @since 1.5.0
     */
    public PmdOperation excludesStrings(boolean clear, String... excludes) {
        if (clear) {
            excludes_.clear();
        }
        excludes_.addAll(CollectionTools.combineStringsToPaths(excludes));
        return this;
    }

    /**
     * Sets paths to exclude from the analysis.
     *
     * @param clear    whether to clear existing entries before adding the new ones
     * @param excludes a collection of paths to exclude
     * @return this operation
     * @see #excludesStrings(String...)
     * @see #excludes(Path...)
     * @since 1.5.0
     */
    @SafeVarargs
    public final PmdOperation excludesStrings(boolean clear, Collection<String>... excludes) {
        if (clear) {
            excludes_.clear();
        }
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * <li>encoding={@code UTF-8}</li>
     * <li>incrementalAnalysis={@code true}</li>
     * <li>inputPaths={@code [src/main, src/test]}</li>
     * <li>reportFile={@code build/pmd/pmd-report.txt}</li>
     * <li>reportFormat={@code text}</li>
     * <li>rulePriority={@code LOW}</li>
     * <li>ruleSets={@link JavaRules#QUICK_START}</li>
     * <li>suppressedMarker={@code NOPMD}</li>
     * </ul>
     *
     * @param project the project
     * @return this operation
     * @since 1.0
     */
    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public PmdOperation fromProject(BaseProject project) {
        project_ = project;
        inputPaths(project.srcMainDirectory(), project.srcTestDirectory());
        ruleSets(JavaRules.QUICK_START);
        return this;
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     *
     * @param ignoreFile the ignore file path
     * @return this operation
     * @see #ignoreFile(File)
     * @see #ignoreFile(String)
     * @since 1.0
     */
    public PmdOperation ignoreFile(Path ignoreFile) {
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
     * @since 1.0
     */
    public PmdOperation ignoreFile(File ignoreFile) {
        return ignoreFile(ignoreFile.toPath());
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     *
     * @param ignoreFile the ignore file path
     * @return this operation
     * @see #ignoreFile(Path)
     * @see #ignoreFile(File)
     * @since 1.0
     */
    public PmdOperation ignoreFile(String ignoreFile) {
        return ignoreFile(Path.of(ignoreFile));
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
     * @since 1.0
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
     * @since 1.0
     */
    public PmdOperation incrementalAnalysis(boolean incrementalAnalysis) {
        incrementalAnalysis_ = incrementalAnalysis;
        return this;
    }

    /**
     * Creates a new initialized configuration.
     *
     * @param commandName the command name
     * @return a fully configured {@link PMDConfiguration}
     * @since 1.0
     */
    public PMDConfiguration initConfiguration(String commandName) {
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

        if (incrementalAnalysis_) {
            if (cache_ != null) {
                config.setAnalysisCacheLocation(cache_.toFile().getAbsolutePath());
            } else if (project_ != null) {
                config.setAnalysisCacheLocation(
                        IOTools.resolveFile(project_.buildDirectory(), PMD_DIR, PMD_DIR + "-cache")
                                .getAbsolutePath());
            }
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

        if (ObjectTools.isEmpty(inputPaths_)) {
            throw new IllegalArgumentException(commandName + ": InputPaths required.");
        }
        config.setInputPathList(inputPaths_);

        config.setMinimumPriority(rulePriority_);

        config.setReportFile(reportFile_ != null
                ? reportFile_
                : Paths.get(project_.buildDirectory().getPath(), PMD_DIR, PMD_DIR + "-report." + reportFormat_));

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
     * Sets paths to source files or directories containing source files to analyze,
     * replacing any previous entries.
     *
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(boolean, Path...)
     * @see #inputPaths(Collection...)
     * @see #inputPaths()
     * @since 1.0
     */
    public PmdOperation inputPaths(Path... inputPath) {
        return inputPaths(true, inputPath);
    }

    /**
     * Sets paths to source files or directories containing source files to analyze,
     * replacing any previous entries.
     *
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(boolean, File...)
     * @see #inputPathsFiles(Collection...)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     * @since 1.0
     */
    public PmdOperation inputPaths(File... inputPath) {
        return inputPaths(true, inputPath);
    }

    /**
     * Sets paths to source files or directories containing source files to analyze,
     * replacing any previous entries.
     *
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(boolean, String...)
     * @see #inputPathsStrings(Collection)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     * @since 1.0
     */
    public PmdOperation inputPaths(String... inputPath) {
        return inputPaths(true, inputPath);
    }

    /**
     * Sets paths to source files or directories containing source files to analyze,
     * replacing any previous entries.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPaths(boolean, Collection...)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     * @since 1.0
     */
    @SafeVarargs
    public final PmdOperation inputPaths(Collection<Path>... inputPath) {
        return inputPaths(true, inputPath);
    }

    /**
     * Returns paths to source files or directories containing source files to analyze.
     *
     * @return the input paths
     * @see #inputPaths(Path...)
     * @see #inputPaths(Collection...)
     * @since 1.0
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public List<Path> inputPaths() {
        return inputPaths_;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param clear     whether to clear existing entries before adding the new ones
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(Path...)
     * @see #inputPaths(Collection...)
     * @see #inputPaths()
     * @since 1.5.0
     */
    public PmdOperation inputPaths(boolean clear, Path... inputPath) {
        if (clear) {
            inputPaths_.clear();
        }
        inputPaths_.addAll(CollectionTools.combine(inputPath));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param clear     whether to clear existing entries before adding the new ones
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(File...)
     * @see #inputPathsFiles(Collection...)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     * @since 1.5.0
     */
    public PmdOperation inputPaths(boolean clear, File... inputPath) {
        if (clear) {
            inputPaths_.clear();
        }
        inputPaths_.addAll(CollectionTools.combineFilesToPaths(inputPath));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param clear     whether to clear existing entries before adding the new ones
     * @param inputPath one or more paths
     * @return this operation
     * @see #inputPaths(String...)
     * @see #inputPathsStrings(Collection)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     * @since 1.5.0
     */
    public PmdOperation inputPaths(boolean clear, String... inputPath) {
        if (clear) {
            inputPaths_.clear();
        }
        inputPaths_.addAll(CollectionTools.combineStringsToPaths(inputPath));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param clear     whether to clear existing entries before adding the new ones
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     * @since 1.5.0
     */
    @SafeVarargs
    public final PmdOperation inputPaths(boolean clear, Collection<Path>... inputPath) {
        if (clear) {
            inputPaths_.clear();
        }
        inputPaths_.addAll(CollectionTools.combine(inputPath));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze,
     * replacing any previous entries.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPathsFiles(boolean, Collection...)
     * @see #inputPaths(File...)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     * @since 1.0
     */
    @SafeVarargs
    public final PmdOperation inputPathsFiles(Collection<File>... inputPath) {
        return inputPathsFiles(true, inputPath);
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param clear     whether to clear existing entries before adding the new ones
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPaths(File...)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     * @since 1.5.0
     */
    @SafeVarargs
    public final PmdOperation inputPathsFiles(boolean clear, Collection<File>... inputPath) {
        if (clear) {
            inputPaths_.clear();
        }
        inputPaths_.addAll(CollectionTools.combineFilesToPaths(inputPath));
        return this;
    }

    /**
     * Sets paths to source files or directories containing source files to analyze,
     * replacing any previous entries.
     *
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPathsStrings(boolean, Collection)
     * @see #inputPaths(String...)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     * @since 1.0
     */
    public PmdOperation inputPathsStrings(Collection<String> inputPath) {
        return inputPathsStrings(true, inputPath);
    }

    /**
     * Sets paths to source files or directories containing source files to analyze.
     *
     * @param clear     whether to clear existing entries before adding the new ones
     * @param inputPath a collection of input paths
     * @return this operation
     * @see #inputPaths(String...)
     * @see #inputPaths(Path...)
     * @see #inputPaths()
     * @since 1.5.0
     */
    public PmdOperation inputPathsStrings(boolean clear, Collection<String> inputPath) {
        if (clear) {
            inputPaths_.clear();
        }
        inputPaths_.addAll(CollectionTools.combineStringsToPaths(inputPath));
        return this;
    }

    /**
     * Sets the minimum priority threshold when loading Rules from RuleSets.
     *
     * @param priority the minimum rule priority
     * @return this operation
     * @since 1.0
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
     * @return the analysis results
     * @throws ExitStatusException if an error occurs
     * @since 1.0
     */
    public PmdAnalysisResults performPmdAnalysis(String commandName,
                                                 PMDConfiguration config) throws ExitStatusException {
        try (var pmd = PmdAnalysis.create(config)) {
            var report = pmd.performAnalysisAndCollectReport();

            if (canLog(Level.INFO)) {
                LOGGER.info(() -> "[%s] inputPaths%s".formatted(commandName, inputPaths_));
                LOGGER.info(() -> "[%s] ruleSets%s".formatted(commandName, ruleSets_));
            }

            var violations = report.getViolations();
            var numViolations = violations.size();

            if (numViolations > 0) {
                printViolations(commandName, config, violations);
            }

            if (pmd.getReporter().numErrors() > 0 && failOnError_) {
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
                LOGGER.info(() -> "[%s] %d rules were checked.".formatted(commandName, result.rulesChecked()));
            }

            if (canLog(Level.WARNING)) {
                if (result.processingErrors() > 0) {
                    for (var err : report.getProcessingErrors()) {
                        LOGGER.warning(() -> "[%s] %s".formatted(commandName, err.getMsg()));
                    }
                }

                if (result.configurationErrors() > 0) {
                    for (var err : report.getConfigurationErrors()) {
                        LOGGER.warning(() -> "[%s] %s".formatted(commandName, err.issue()));
                    }
                }
            }

            if (canLog(Level.FINEST)) {
                LOGGER.finest(result::toString);
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
     * @since 1.0
     */
    public PmdOperation prependAuxClasspath(String... classpath) {
        if (ObjectTools.isNotEmpty(classpath)) {
            prependAuxClasspath_ = String.join(File.pathSeparator, classpath);
        }
        return this;
    }

    /**
     * Returns the prepended classpath.
     *
     * @return the classpath
     * @see #prependAuxClasspath(String...)
     * @since 1.0
     */
    public String prependAuxClasspath() {
        return prependAuxClasspath_;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot one or more relative root paths
     * @return this operation
     * @see #relativizeRoots(Collection...)
     * @see #relativizeRoots()
     * @since 1.0
     */
    public PmdOperation relativizeRoots(Path... relativeRoot) {
        relativizeRoots_.addAll(CollectionTools.combine(relativeRoot));
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot one or more relative root paths
     * @return this operation
     * @see #relativizeRootsFiles(Collection...)
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     * @since 1.0
     */
    public PmdOperation relativizeRoots(File... relativeRoot) {
        relativizeRoots_.addAll(CollectionTools.combineFilesToPaths(relativeRoot));
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     *
     * @param relativeRoot one or more relative root paths
     * @return this operation
     * @see #relativizeRootsStrings(Collection...)
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots()
     * @since 1.0
     */
    public PmdOperation relativizeRoots(String... relativeRoot) {
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
     * @since 1.0
     */
    @SafeVarargs
    public final PmdOperation relativizeRoots(Collection<Path>... relativeRoot) {
        relativizeRoots_.addAll(CollectionTools.combine(relativeRoot));
        return this;
    }

    /**
     * Returns paths to shorten paths that are output in the report.
     *
     * @return the relative root paths
     * @see #relativizeRoots(Path...)
     * @see #relativizeRoots(Collection...)
     * @since 1.0
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
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
     * @since 1.0
     */
    @SafeVarargs
    public final PmdOperation relativizeRootsFiles(Collection<File>... relativeRoot) {
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
     * @since 1.0
     */
    @SafeVarargs
    public final PmdOperation relativizeRootsStrings(Collection<String>... relativeRoot) {
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
     * @since 1.0
     */
    public PmdOperation reportFile(Path reportFile) {
        reportFile_ = reportFile;
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
     * @since 1.0
     */
    public PmdOperation reportFile(File reportFile) {
        return reportFile(reportFile.toPath());
    }

    /**
     * Sets the path to the report page.
     *
     * @param reportFile the report file path
     * @return this operation
     * @see #reportFile(Path)
     * @see #reportFile(File)
     * @see #reportFile()
     * @since 1.0
     */
    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public PmdOperation reportFile(String reportFile) {
        return reportFile(Paths.get(reportFile));
    }

    /**
     * Returns the path to the report file.
     *
     * @return the report file path
     * @see #reportFile(Path)
     * @see #reportFile(File)
     * @see #reportFile(String)
     * @since 1.0
     */
    public Path reportFile() {
        return reportFile_;
    }

    /**
     * Sets the output format of the analysis report. The default is {@code text}.
     *
     * @param reportFormat the report format
     * @return this operation
     * @since 1.0
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
     * @since 1.0
     */
    public PmdOperation reportProperties(Properties reportProperties) {
        if (ObjectTools.isNotEmpty(reportProperties)) {
            reportProperties_.putAll(reportProperties);
        }
        return this;
    }

    /**
     * Sets new rule set paths, replacing any previous entries.
     *
     * @param ruleSet one or more rule set paths
     * @return this operation
     * @see #ruleSets(boolean, String...)
     * @see #ruleSets(Collection...)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSetsRules(Collection...)
     * @see #ruleSets()
     * @since 1.0
     */
    public PmdOperation ruleSets(String... ruleSet) {
        return ruleSets(true, ruleSet);
    }

    /**
     * Sets new rule set paths, replacing any previous entries.
     *
     * @param ruleSet one or more rule set paths
     * @return this operation
     * @see #ruleSets(boolean, Collection...)
     * @see #ruleSets(String...)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSetsRules(Collection...)
     * @see #ruleSets()
     * @since 1.0
     */
    @SafeVarargs
    public final PmdOperation ruleSets(Collection<String>... ruleSet) {
        return ruleSets(true, ruleSet);
    }

    /**
     * Sets new rule set paths, replacing any previous entries.
     *
     * @param ruleSet a collection of rule set paths
     * @return this operation
     * @see #ruleSets(boolean, JavaRules...)
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection...)
     * @see #ruleSetsRules(Collection...)
     * @see #ruleSets()
     * @since 1.5.0
     */
    public final PmdOperation ruleSets(JavaRules... ruleSet) {
        return ruleSets(true, ruleSet);
    }

    /**
     * Returns the rule set paths.
     *
     * @return the rule sets
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection...)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSetsRules(Collection...)
     * @since 1.0
     */
    @SuppressFBWarnings("EI_EXPOSE_REP")
    public Set<String> ruleSets() {
        return ruleSets_;
    }

    /**
     * Sets rule set paths.
     *
     * @param clear   whether to clear existing entries before adding the new ones
     * @param ruleSet one or more rule set paths
     * @return this operation
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection...)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSetsRules(Collection...)
     * @see #ruleSets()
     * @since 1.5.0
     */
    public PmdOperation ruleSets(boolean clear, String... ruleSet) {
        if (clear) {
            ruleSets_.clear();
        }
        ruleSets_.addAll(List.of(ruleSet));
        return this;
    }

    /**
     * Sets rule set paths.
     *
     * @param clear   whether to clear existing entries before adding the new ones
     * @param ruleSet one or more rule set paths
     * @return this operation
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection...)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSetsRules(Collection...)
     * @see #ruleSets()
     * @since 1.5.0
     */
    @SafeVarargs
    public final PmdOperation ruleSets(boolean clear, Collection<String>... ruleSet) {
        if (clear) {
            ruleSets_.clear();
        }
        ruleSets_.addAll(CollectionTools.combine(ruleSet));
        return this;
    }

    /**
     * Sets rule set paths.
     *
     * @param clear   whether to clear existing entries before adding the new ones
     * @param ruleSet a collection of rule set paths
     * @return this operation
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection...)
     * @see #ruleSetsRules(Collection...)
     * @see #ruleSets()
     * @since 1.5.0
     */
    public final PmdOperation ruleSets(boolean clear, JavaRules... ruleSet) {
        if (clear) {
            ruleSets_.clear();
        }
        ruleSets_.addAll(Arrays.stream(ruleSet).map(JavaRules::getCategory).toList());
        return this;
    }

    /**
     * Sets new rule set paths, replacing any previous entries.
     *
     * @param ruleSet a collection of rule set paths
     * @return this operation
     * @see #ruleSetsRules(boolean, Collection...)
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection...)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSets()
     * @since 1.5.0
     */
    @SafeVarargs
    public final PmdOperation ruleSetsRules(Collection<JavaRules>... ruleSet) {
        return ruleSetsRules(true, ruleSet);
    }

    /**
     * Sets rule set paths.
     *
     * @param clear   whether to clear existing entries before adding the new ones
     * @param ruleSet a collection of rule set paths
     * @return this operation
     * @see #ruleSets(String...)
     * @see #ruleSets(Collection...)
     * @see #ruleSets(JavaRules...)
     * @see #ruleSets()
     * @since 1.5.0
     */
    @SafeVarargs
    public final PmdOperation ruleSetsRules(boolean clear, Collection<JavaRules>... ruleSet) {
        if (clear) {
            ruleSets_.clear();
        }
        ruleSets_.addAll(CollectionTools.combine(ruleSet).stream().map(JavaRules::getCategory).toList());
        return this;
    }

    /**
     * Enables or disables adding the suppressed rule violations to the report.
     *
     * @param showSuppressed whether to add suppressed violations to the report
     * @return this operation
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
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
     * @since 1.0
     */
    public PmdOperation uri(URI inputUri) {
        inputUri_ = inputUri;
        return this;
    }

    /**
     * Returns {@code true} when logging at {@code level} is both enabled and
     * not suppressed by {@link #silent()}.
     */
    private boolean canLog(Level level) {
        return !silent() && LOGGER.isLoggable(level);
    }

    /**
     * Logs each violation and throws or warns depending on {@link #failOnViolation_}.
     * Accepts the pre-fetched violations list to avoid calling {@code report.getViolations()} twice.
     */
    private void printViolations(String commandName,
                                 PMDConfiguration config,
                                 List<RuleViolation> violations) throws ExitStatusException {
        if (canLog(Level.WARNING)) {
            final String msgFormat = includeLineNumber_ ? MSG_FORMAT_WITH_LINE : MSG_FORMAT_NO_LINE;
            for (var v : violations) {
                LOGGER.warning(() -> msgFormat.formatted(
                        commandName,
                        v.getFileId().getUriString(),
                        v.getBeginLine(),
                        v.getRule().getName(),
                        v.getRule().getExternalInfoUrl(),
                        v.getDescription()));
            }
        }

        var reportFilePath = config.getReportFilePath();
        var suffix = reportFilePath != null ? " See the report at: " + reportFilePath.toUri() : "";
        var summary = "[%s] %d rule violations were found.%s"
                .formatted(commandName, violations.size(), suffix);

        if (config.isFailOnViolation()) {
            if (canLog(Level.SEVERE)) {
                LOGGER.severe(summary);
            }
            throw new ExitStatusException(ExitStatusException.EXIT_FAILURE);
        } else if (canLog(Level.WARNING)) {
            LOGGER.warning(summary);
        }
    }
}