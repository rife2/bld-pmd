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

package rife.bld.extension.pmd;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class PmdAnalysisResultsTests {
    @Nested
    @DisplayName("Errors Tests")
    class ErrorsTests {
        @Test
        void hasConfigurationErrors() {
            var results = new PmdAnalysisResults(
                    0, 0, 0, 0, 1, 0);
            assertThat(results.hasNoErrors()).isFalse();
        }

        @Test
        void hasErrors() {
            var results = new PmdAnalysisResults(
                    0, 0, 1, 0, 0, 0);
            assertThat(results.hasNoErrors()).isFalse();
        }

        @Test
        void hasNoErrors() {
            var results = new PmdAnalysisResults(
                    0, 0, 0, 0, 0, 0);
            assertThat(results.hasNoErrors()).isTrue();
        }

        @Test
        void hasNoErrorsWithViolations() {
            var results = new PmdAnalysisResults(
                    10, 5, 0, 0, 0, 50);
            assertThat(results.hasNoErrors()).isTrue();
        }


        @Test
        void hasProcessingErrors() {
            var results = new PmdAnalysisResults(
                    0, 0, 0, 1, 0, 0);
            assertThat(results.hasNoErrors()).isFalse();
        }
    }

    @Nested
    @DisplayName("Violations Tests")
    class ViolationsTests {
        @Test
        void hasNoViolations() {
            var results = new PmdAnalysisResults(
                    0, 0, 0, 0, 0, 0);
            assertThat(results.hasNoViolations()).isTrue();
        }

        @Test
        void hasSuppressedViolations() {
            var results = new PmdAnalysisResults(
                    0, 1, 0, 0, 0, 0);
            assertThat(results.hasNoViolations()).isFalse();
        }

        @Test
        void hasViolations() {
            var results = new PmdAnalysisResults(
                    1, 0, 0, 0, 0, 0);
            assertThat(results.hasNoViolations()).isFalse();
        }
    }
}
