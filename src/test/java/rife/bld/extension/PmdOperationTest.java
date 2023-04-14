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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * PmdOperationTest class
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class PmdOperationTest {
    public static final String COMMAND_NAME = "pmd";
    public static final String TEST = "test";
    static final PmdOperation pmdOperation = new PmdOperation();

    @BeforeEach
    void initializeOperation() {
        pmdOperation.inputPaths = List.of(Paths.get("src/main"));
        pmdOperation.reportFile = Paths.get("build", COMMAND_NAME, "pmd-test-report.txt");
        pmdOperation.cache = Paths.get("build", COMMAND_NAME, "pmd-cache");
        pmdOperation.failOnViolation = false;
    }

    @Test
    void testConfigFile() {
        var pmd = pmdOperation.ruleSets("src/test/resources/pmd.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testEmptyRuleSets() {
        var pmd = pmdOperation.ruleSets("");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testJavaQuickStart() {
        var pmd = pmdOperation.ruleSets("rulesets/java/quickstart.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testJavaErrorProne() {
        var pmd = pmdOperation.ruleSets("category/java/errorprone.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("many errors").isGreaterThan(0);
    }

    @Test
    void testJavaCodeStyleAndErrorProne() {
        var pmd = pmdOperation.addRuleSet("category/java/codestyle.xml", "category/java/errorprone.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("many errors").isGreaterThan(0);
    }

    @Test
    void testJavaCodeStyle() {
        var pmd = pmdOperation.ruleSets("category/java/codestyle.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("many errors").isGreaterThan(0);
    }

    @Test
    void testJavaDesign() {
        var pmd = pmdOperation.ruleSets("category/java/design.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("many errors").isGreaterThan(0);
    }

    @Test
    void testJavaDocumentation() {
        var pmd = pmdOperation.ruleSets("category/java/documentation.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("many errors").isGreaterThan(0);
    }

    @Test
    void testJavaBestPractices() {
        var pmd = pmdOperation.ruleSets("category/java/bestpractices.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testJavaMultiThreading() {
        var pmd = pmdOperation.ruleSets("category/java/multithreading");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testJavaPerformance() {
        var pmd = pmdOperation.ruleSets("category/java/performance.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testJavaSecurity() {
        var pmd = pmdOperation.ruleSets("category/java/security.xml");
        assertThat(pmd.performPmdAnalysis(TEST, pmd.initConfiguration(COMMAND_NAME)))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testPmdOperation() {
        assertThat(pmdOperation.performPmdAnalysis(TEST, pmdOperation.initConfiguration(COMMAND_NAME)))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testFailOnValidation() {
        assertThatCode(() -> pmdOperation.ruleSets("category/java/documentation.xml").failOnViolation(true)
                .performPmdAnalysis(TEST, pmdOperation.initConfiguration(COMMAND_NAME))
        ).isInstanceOf(RuntimeException.class).hasMessageContaining('[' + TEST + ']');
    }
}
