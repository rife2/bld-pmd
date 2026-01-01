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

package rife.bld.extension.pmd;

/**
 * Represents the results of a PMD analysis, containing various counts
 * related to violations, errors, and rules.
 *
 * @param violations           The number of violations found during the analysis
 * @param suppressedViolations The number of suppressed violations found during the analysis
 * @param errors               The number of errors returned during the analysis
 * @param processingErrors     The number of processing errors returned during the analysis
 * @param configurationErrors  The number of processing errors returning during the analysis
 * @param rulesChecked         The number of rules checked during the analysis
 * @since 1.3.0
 */
public record PmdAnalysisResults(
        int violations,
        int suppressedViolations,
        int errors,
        int processingErrors,
        int configurationErrors,
        int rulesChecked) {

    /**
     * Checks if the analysis results indicate no errors of any type.
     *
     * @return {@code true} if there are no errors, {@code false} otherwise
     */
    public boolean hasNoErrors() {
        return errors == 0 && processingErrors == 0 && configurationErrors == 0;
    }

    /**
     * Determines whether the analysis results indicate no violations of any type.
     *
     * @return {@code true} if there are no violations, {@code false} otherwise
     */
    public boolean hasNoViolations() {
        return violations == 0 && suppressedViolations == 0;
    }
}
