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
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.lang.LanguageVersion;
import rife.bld.BaseProject;
import rife.bld.operations.AbstractOperation;

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
     * The default rule set.
     */
    public static final String RULE_SET_DEFAULT = "rulesets/java/quickstart.xml";
    private static final Logger LOGGER = Logger.getLogger(PmdOperation.class.getName());
    private static final String PMD_DIR = "pmd";
    /**
     * The input paths (source) list.
     */
    final List<Path> inputPaths_ = new ArrayList<>();
    /**
     * The relative roots paths.
     */
    final List<Path> relativizeRoots_ = new ArrayList<>();
    /**
     * The rule priority.
     */
    final RulePriority rulePriority_ = RulePriority.LOW;
    /**
     * The rule sets list.
     */
    final List<String> ruleSets_ = new ArrayList<>();
    /**
     * The cache location.
     */
    Path cache_;
    /**
     * The encoding.
     */
    Charset encoding_ = StandardCharsets.UTF_8;
    /**
     * The fail on violation toggle.
     */
    boolean failOnViolation_;
    /**
     * The forced language.
     */
    LanguageVersion forcedLanguageVersion_;
    /**
     * The path of the ignore file
     */
    Path ignoreFile_;
    /**
     * The incremental analysis toggle.
     */
    boolean incrementalAnalysis_ = true;
    /**
     * The input URI.
     */
    URI inputUri_;
    /**
     * The default language version(s).
     */
    List<LanguageVersion> languageVersions_;
    /**
     * The path to the report page.
     */
    Path reportFile_;
    /**
     * The report format.
     */
    String reportFormat_ = "text";
    /**
     * The show suppressed flag.
     */
    boolean showSuppressed_;
    /**
     * THe suppressed marker.
     */
    String suppressedMarker_ = "NOPMD";
    /**
     * The number of threads.
     */
    int threads_ = 1;
    /**
     * The project reference.
     */
    private BaseProject project_;

    /**
     * Adds paths to source files, or directories containing source files to analyze.
     */
    public PmdOperation addInputPath(Path... inputPath) {
        inputPaths_.addAll(List.of(inputPath));
        return this;
    }

    /**
     * Adds paths to source files, or directories containing source files to analyze.
     */
    public PmdOperation addInputPath(Collection<Path> inputPath) {
        inputPaths_.addAll(inputPath);
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     */
    public PmdOperation addRelativizeRoot(Path... relativeRoot) {
        relativizeRoots_.addAll(List.of(relativeRoot));
        return this;
    }

    /**
     * Adds several paths to shorten paths that are output in the report.
     */
    public PmdOperation addRelativizeRoot(Collection<Path> relativeRoot) {
        relativizeRoots_.addAll(relativeRoot);
        return this;
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
     */
    public PmdOperation addRuleSet(String... ruleSet) {
        ruleSets_.addAll(List.of(ruleSet));
        return this;
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
     */
    public PmdOperation addRuleSet(Collection<String> ruleSets) {
        ruleSets_.addAll(ruleSets);
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
     * Sets the default language to be used for all input files.
     */
    public PmdOperation defaultLanguage(LanguageVersion... languageVersion) {
        languageVersions_.addAll(List.of(languageVersion));
        return this;
    }

    /**
     * Sets the default language to be used for all input files.
     */
    public PmdOperation defaultLanguage(Collection<LanguageVersion> languageVersion) {
        languageVersions_.addAll(languageVersion);
        return this;
    }

    /**
     * <p>Specifies the character set encoding of the source code files. The default is {@code UTF-8}.</p>
     *
     * <p>The valid values are the standard character sets of {@link java.nio.charset.Charset Charset}.</p>
     */
    public PmdOperation encoding(String encoding) {
        encoding_ = Charset.forName(encoding);
        return this;
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
     * Performs the PMD code analysis operation.
     */
    @Override
    public void execute() {
        if (project_ == null) {
            throw new IllegalArgumentException("ERROR: project required.");
        }

        var commandName = project_.getCurrentCommandName();
        performPmdAnalysis(commandName, initConfiguration(commandName));
    }

    /**
     * Sets whether the build will continue on warnings.
     */
    public PmdOperation failOnViolation(boolean failOnViolation) {
        failOnViolation_ = failOnViolation;
        return this;
    }

    /**
     * Forces a language to be used for all input files, irrespective of file names.
     */
    public PmdOperation forceVersion(LanguageVersion languageVersion) {
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
     */
    public PmdOperation fromProject(BaseProject project) {
        project_ = project;

        inputPaths_.add(project.srcMainDirectory().toPath());
        inputPaths_.add(project.srcTestDirectory().toPath());
        ruleSets_.add(RULE_SET_DEFAULT);
        return this;
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     */
    public PmdOperation ignoreFile(Path ignoreFile) {
        ignoreFile_ = ignoreFile;
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
     */
    public PMDConfiguration initConfiguration(String commandName) {
        PMDConfiguration config = new PMDConfiguration();

        if (cache_ == null && project_ != null && incrementalAnalysis_) {
            config.setAnalysisCacheLocation(
                    Paths.get(project_.buildDirectory().getPath(), PMD_DIR, PMD_DIR + "-cache").toFile().getAbsolutePath());
        } else if (cache_ != null) {
            config.setAnalysisCacheLocation(cache_.toFile().getAbsolutePath());
        }

        config.setFailOnViolation(failOnViolation_);

        if (languageVersions_ != null) {
            config.setDefaultLanguageVersions(languageVersions_);
        }

        if (forcedLanguageVersion_ != null) {
            config.setForceLanguageVersion(forcedLanguageVersion_);
        }

        if (ignoreFile_ != null) {
            config.setIgnoreFilePath(ignoreFile_);
        }

        config.setIgnoreIncrementalAnalysis(!incrementalAnalysis_);

        if (inputPaths_.isEmpty()) {
            throw new IllegalArgumentException(commandName + ": InputPaths required.");
        } else {
            config.setInputPathList(inputPaths_);
        }

        if (inputUri_ != null) {
            config.setInputUri(inputUri_);
        }

        config.setMinimumPriority(rulePriority_);

        if (project_ != null) {
            config.setReportFile(Objects.requireNonNullElseGet(reportFile_,
                    () -> Paths.get(project_.buildDirectory().getPath(), PMD_DIR, PMD_DIR + "-report." + reportFormat_)));
        } else {
            config.setReportFile(reportFile_);
        }

        config.addRelativizeRoots(relativizeRoots_);
        config.setReportFormat(reportFormat_);
        config.setRuleSets(ruleSets_);
        config.setShowSuppressedViolations(showSuppressed_);
        config.setSourceEncoding(encoding_);
        config.setSuppressMarker(suppressedMarker_);
        config.setThreads(threads_);

        return config;
    }

    /**
     * Sets the to source files, or directories containing source files to analyze.
     * Previously set paths will be disregarded.
     */
    public PmdOperation inputPaths(Path... inputPath) {
        inputPaths_.clear();
        inputPaths_.addAll(List.of(inputPath));
        return this;
    }

    /**
     * Sets the to source files, or directories containing source files to analyze.
     * Previously set paths will be disregarded.
     */
    public PmdOperation inputPaths(Collection<Path> inputPath) {
        inputPaths_.clear();
        inputPaths_.addAll(inputPath);
        return this;
    }

    /**
     * Performs the PMD analysis with the given config.
     */
    public int performPmdAnalysis(String commandName, PMDConfiguration config) throws RuntimeException {
        var pmd = PmdAnalysis.create(config);
        var report = pmd.performAnalysisAndCollectReport();
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.log(Level.INFO, "[{0}] inputPaths{1}", new Object[]{commandName, inputPaths_});
            LOGGER.log(Level.INFO, "[{0}] ruleSets{1}", new Object[]{commandName, ruleSets_});
        }
        var numErrors = report.getViolations().size();
        if (numErrors > 0) {
            var msg = String.format(
                    "[%s] %d rule violations were found. See the report at: %s", commandName, numErrors,
                    config.getReportFilePath().toUri());
            for (var v : report.getViolations()) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "[{0}] {1}:{2}:\n\t{3} ({4})\n\t\t--> {5}",
                            new Object[]{commandName,
                                    v.getFileId().getAbsolutePath(),
                                    v.getBeginLine(),
                                    v.getRule().getName(),
                                    v.getRule().getExternalInfoUrl() //TODO bug in PMD?
                                            .replace("${pmd.website.baseurl}",
                                            "https://docs.pmd-code.org/pmd-doc-7.0.0-rc4"),
                                    v.getDescription()});
                }
            }
            if (config.isFailOnViolation()) {
                throw new RuntimeException(msg); // NOPMD
            } else {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning(msg);
                }
            }
        } else {
            var rules = pmd.getRulesets();
            if (!rules.isEmpty()) {
                int count = 0;
                for (var rule : rules) {
                    count += rule.getRules().size();
                }
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(String.format("[%s] %d rules were checked.", commandName, count));
                }
            }
        }
        return numErrors;
    }

    /**
     * Sets several paths to shorten paths that are output in the report. Previous relative paths will be disregarded.
     */
    public PmdOperation relativizeRoots(Path... relativeRoot) {
        relativizeRoots_.clear();
        relativizeRoots_.addAll(List.of(relativeRoot));
        return this;
    }

    /**
     * Sets several paths to shorten paths that are output in the report. Previous relative paths will be disregarded.
     */
    public PmdOperation relativizeRoots(Collection<Path> relativeRoot) {
        relativizeRoots_.clear();
        relativizeRoots_.addAll(relativeRoot);
        return this;
    }

    /**
     * Sets the output format of the analysis report. The default is {@code text}.
     */
    public PmdOperation reportFormat(String reportFormat) {
        reportFormat_ = reportFormat;
        return this;
    }

    /**
     * Sets the rule set path(s), disregarding any previously set paths.
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
     */
    public PmdOperation ruleSets(String... ruleSet) {
        ruleSets_.clear();
        ruleSets_.addAll(Arrays.asList(ruleSet));
        return this;
    }

    /**
     * Sets the rule set path(s), disregarding any previously set paths.
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
     */
    public PmdOperation ruleSets(Collection<String> ruleSets) {
        ruleSets_.clear();
        ruleSets_.addAll(ruleSets);
        return this;
    }

    /**
     * Enables or disables adding the suppressed rule violations to the report.
     */
    public PmdOperation showSuppressed(boolean showSuppressed) {
        showSuppressed_ = showSuppressed;
        return this;
    }

    /**
     * Specifies the comment token that marks lines which should be ignored. The default is {@code NOPMD}.
     */
    public PmdOperation suppressedMarker(String suppressedMarker) {
        suppressedMarker_ = suppressedMarker;
        return this;
    }

    /**
     * Sets the number of threads to be used. The default is {code 1}.
     */
    public PmdOperation threads(int threads) {
        threads_ = threads;
        return this;
    }

    /**
     * Sets the input URI to process for source code objects.
     */
    public PmdOperation uri(URI inputUri) {
        inputUri_ = inputUri;
        return this;
    }
}
