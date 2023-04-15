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

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.RulePriority;
import net.sourceforge.pmd.lang.LanguageVersion;
import rife.bld.BaseProject;
import rife.bld.operations.AbstractOperation;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
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
     * The cache location.
     */
    Path cache;
    /**
     * The debug toggle.
     */
    boolean debug;
    /**
     * The encoding.
     */
    String encoding = "UTF-8";
    /**
     * The fail on violation toggle.
     */
    boolean failOnViolation;
    /**
     * The forced language.
     */
    LanguageVersion forcedLanguageVersion;
    /**
     * The path of the ignore file
     */
    Path ignoreFile;
    /**
     * The incremental analysis toggle.
     */
    boolean incrementalAnalysis = true;
    /**
     * The input paths (source) list.
     */
    List<Path> inputPaths = new ArrayList<>();
    /**
     * The input URI.
     */
    URI inputUri;
    /**
     * The default language version(s).
     */
    List<LanguageVersion> languageVersions;
    /**
     * The relative roots paths.
     */
    List<Path> relativizeRoots = new ArrayList<>();
    /**
     * The path to the report page.
     */
    Path reportFile;
    /**
     * The report format.
     */
    String reportFormat = "text";
    /**
     * The rule priority.
     */
    RulePriority rulePriority = RulePriority.LOW;
    /**
     * The rule sets list.
     */
    List<String> ruleSets = new ArrayList<>();
    /**
     * The show suppressed flag.
     */
    boolean showSuppressed;
    /**
     * THe suppressed marker.
     */
    String suppressedMarker = "NOPMD";
    /**
     * The number of threads.
     */
    int threads = 1;
    /**
     * The project reference.
     */
    private BaseProject project;

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
        this.project = project;

        inputPaths.add(project.srcMainDirectory().toPath());
        inputPaths.add(project.srcTestDirectory().toPath());
        ruleSets.add(RULE_SET_DEFAULT);
        return this;
    }

    /**
     * Adds paths to source files, or directories containing source files to analyze.
     *
     * @see #inputPaths(Path...)
     */
    public PmdOperation addInputPath(Path... inputPath) {
        inputPaths.addAll(List.of(inputPath));
        return this;
    }

    /**
     * Add several paths to shorten paths that are output in the report.
     *
     * @see #addRelativizeRoot(Path...)
     */
    public PmdOperation addRelativizeRoot(Path... relativeRoot) {
        this.relativizeRoots.addAll(List.of(relativeRoot));
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
     *
     * @see #ruleSets(String...)
     */
    public PmdOperation addRuleSet(String... ruleSet) {
        ruleSets.addAll(List.of(ruleSet));
        return this;
    }

    /**
     * Sets the location of the cache file for incremental analysis.
     */
    public PmdOperation cache(Path cache) {
        this.cache = cache;
        return this;
    }

    /**
     * Enables or disables debug logging mode.
     */
    public PmdOperation debug(boolean debug) {
        this.debug = debug;
        return this;
    }

    /**
     * Sets the default language to be used for all input files.
     */
    public PmdOperation defaultLanguage(LanguageVersion... languageVersion) {
        this.languageVersions.addAll(List.of(languageVersion));
        return this;
    }

    /**
     * <p>Specifies the character set encoding of the source code files. The default is {@code UTF-8}.</p>
     *
     * <p>The valid values are the standard character sets of {@link java.nio.charset.Charset Charset}.</p>
     */
    public PmdOperation encoding(String encoding) {
        this.encoding = encoding;
        return this;
    }

    /**
     * Performs the PMD code analysis operation.
     *
     * @throws Exception when an exception occurs during the execution
     */
    @Override
    public void execute() throws Exception {
        if (project == null) {
            throw new IllegalArgumentException("ERROR: project required.");
        }

        var commandName = project.getCurrentCommandName();
        performPmdAnalysis(commandName, initConfiguration(commandName));
    }

    /**
     * Sets whether the build will continue on warnings.
     */
    public PmdOperation failOnViolation(boolean failOnViolation) {
        this.failOnViolation = failOnViolation;
        return this;
    }

    /**
     * Forces a language to be used for all input files, irrespective of file names.
     */
    public PmdOperation forceVersion(LanguageVersion languageVersion) {
        this.forcedLanguageVersion = languageVersion;
        return this;
    }

    /**
     * Sets the path to the file containing a list of files to ignore, one path per line.
     */
    public PmdOperation ignoreFile(Path ignoreFile) {
        this.ignoreFile = ignoreFile;
        return this;
    }

    /**
     * Enables or disables incremental analysis.
     */
    public PmdOperation incrementalAnalysis(boolean incrementalAnalysis) {
        this.incrementalAnalysis = incrementalAnalysis;
        return this;
    }

    /**
     * Creates a new initialized configuration.
     */
    public PMDConfiguration initConfiguration(String commandName) {
        PMDConfiguration config = new PMDConfiguration();

        if (cache == null && project != null && incrementalAnalysis) {
            config.setAnalysisCacheLocation(
                    Paths.get(project.buildDirectory().getPath(), PMD_DIR, PMD_DIR + "-cache").toFile().getAbsolutePath());
        } else if (cache != null) {
            config.setAnalysisCacheLocation(cache.toFile().getAbsolutePath());
        }

        config.setDebug(debug);
        config.setFailOnViolation(failOnViolation);

        if (languageVersions != null) {
            config.setDefaultLanguageVersions(languageVersions);
        }

        if (forcedLanguageVersion != null) {
            config.setForceLanguageVersion(forcedLanguageVersion);
        }

        if (ignoreFile != null) {
            config.setIgnoreFilePath(ignoreFile);
        }

        config.setIgnoreIncrementalAnalysis(!incrementalAnalysis);

        if (inputPaths.isEmpty()) {
            throw new IllegalArgumentException(commandName + ": InputPaths required.");
        } else {
            config.setInputPathList(inputPaths);
        }

        if (inputUri != null) {
            config.setInputUri(inputUri);
        }

        config.setMinimumPriority(rulePriority);

        if (project != null) {
            config.setReportFile(Objects.requireNonNullElseGet(reportFile,
                    () -> Paths.get(project.buildDirectory().getPath(), PMD_DIR, PMD_DIR + "-report." + reportFormat)));
        } else {
            config.setReportFile(reportFile);
        }

        config.addRelativizeRoots(relativizeRoots);
        config.setReportFormat(reportFormat);
        config.setRuleSets(ruleSets);
        config.setShowSuppressedViolations(showSuppressed);
        config.setSourceEncoding(encoding);
        config.setSuppressMarker(suppressedMarker);
        config.setThreads(threads);

        return config;
    }

    /**
     * Sets the to source files, or directories containing source files to analyze.
     * Previously set paths will be disregarded.
     *
     * @see #addInputPath(Path...)
     */
    public PmdOperation inputPaths(Path... inputPath) {
        inputPaths.clear();
        inputPaths.addAll(List.of(inputPath));
        return this;
    }

    /**
     * Performs the PMD analysis with the given config.
     */
    public int performPmdAnalysis(String commandName, PMDConfiguration config) throws RuntimeException {
        var pmd = PmdAnalysis.create(config);
        var report = pmd.performAnalysisAndCollectReport();
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(String.format("[%s] ruleSets%s", commandName, ruleSets));
        }
        var numErrors = report.getViolations().size();
        if (numErrors > 0) {
            var msg = String.format(
                    "[%s] %d rule violations were found. See the report at: %s", commandName, numErrors,
                    config.getReportFilePath().toUri());
            for (var v : report.getViolations()) {
                if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.warning(String.format("[%s] %s:%d\n\t%s (%s)\n\t\t--> %s", commandName,
                            Paths.get(v.getFilename()).toUri(), v.getBeginLine(), v.getRule().getName(), v.getRule().getExternalInfoUrl(), v.getDescription()));
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
            if (rules.size() > 0) {
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
     *
     * @see #addRelativizeRoot(Path...)
     */
    public PmdOperation relativizeRoots(Path... relativeRoot) {
        this.relativizeRoots.clear();
        this.relativizeRoots.addAll(List.of(relativeRoot));
        return this;
    }

    /**
     * Sets the output format of the analysis report. The default is {@code text}.
     */
    public PmdOperation reportFormat(String reportFormat) {
        this.reportFormat = reportFormat;
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
     *
     * @see #addRuleSet(String...)
     */
    public PmdOperation ruleSets(String... ruleSet) {
        ruleSets.clear();
        ruleSets.addAll(Arrays.asList(ruleSet));
        return this;
    }

    /**
     * Enables or disables adding the suppressed rule violations to the report.
     */
    public PmdOperation showSuppressed(boolean showSuppressed) {
        this.showSuppressed = showSuppressed;
        return this;
    }

    /**
     * Specifies the comment token that marks lines which should be ignored. The default is {@code NOPMD}.
     */
    public PmdOperation suppressedMarker(String suppressedMarker) {
        this.suppressedMarker = suppressedMarker;
        return this;
    }

    /**
     * Sets the number of threads to be used. The default is {code 1}.
     */
    public PmdOperation threads(int threads) {
        this.threads = threads;
        return this;
    }

    /**
     * Sets the input URI to process for source code objects.
     */
    public PmdOperation uri(URI inputUri) {
        this.inputUri = inputUri;
        return this;
    }
}