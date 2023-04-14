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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;


/**
 * PmdOperationTest class
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.0
 */
public class PmdOperationTest {
    static final PmdOperation PMD_OPERATION = new PmdOperation();

    @BeforeAll
    public static void initializeOperation() {
        PMD_OPERATION.inputPaths = List.of(Paths.get("src/main"));
        PMD_OPERATION.reportFile = Paths.get("build", "pmd", "pmd-test-report.txt");
        PMD_OPERATION.cache = Paths.get("build", "pmd", "pmd-cache");
        PMD_OPERATION.failOnViolation = false;
    }

    @Test
    void testConfigFile() {
        var pmd = PMD_OPERATION.ruleSets("src/test/resources/pmd.xml");
        assertThat(pmd.performPmdAnalysis("test", pmd.initConfiguration("pmd")))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testEmptyRuleSets() {
        var pmd = PMD_OPERATION.ruleSets("");
        assertThat(pmd.performPmdAnalysis("test", pmd.initConfiguration("pmd")))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testJavaQuickStart() {
        var pmd = PMD_OPERATION.ruleSets("rulesets/java/quickstart.xml");
        assertThat(pmd.performPmdAnalysis("test", pmd.initConfiguration("pmd")))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testJavaErrorProne() {
        var pmd = PMD_OPERATION.ruleSets("category/java/errorprone.xml");
        assertThat(pmd.performPmdAnalysis("test", pmd.initConfiguration("pmd")))
                .as("no errors").isGreaterThan(0);
    }

    @Test
    void testJavaCodeStyle() {
        var pmd = PMD_OPERATION.ruleSets("category/java/codestyle.xml");
        assertThat(pmd.performPmdAnalysis("test", pmd.initConfiguration("pmd")))
                .as("no errors").isGreaterThan(0);
    }

    @Test
    void testJavaDesign() {
        var pmd = PMD_OPERATION.ruleSets("category/java/design.xml");
        assertThat(pmd.performPmdAnalysis("test", pmd.initConfiguration("pmd")))
                .as("no errors").isGreaterThan(0);
    }

    @Test
    void testJavaDocumentation() {
        var pmd = PMD_OPERATION.ruleSets("category/java/documentation.xml");
        assertThat(pmd.performPmdAnalysis("test", pmd.initConfiguration("pmd")))
                .as("no errors").isGreaterThan(0);
    }

    @Test
    void testJavaBestPractices() {
        var pmd = PMD_OPERATION.ruleSets("category/java/bestpractices.xml");
        assertThat(pmd.performPmdAnalysis("test", pmd.initConfiguration("pmd")))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testJavaMultiThreading() {
        var pmd = PMD_OPERATION.ruleSets("category/java/multithreading");
        assertThat(pmd.performPmdAnalysis("test", pmd.initConfiguration("pmd")))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testJavaPerformance() {
        var pmd = PMD_OPERATION.ruleSets("category/java/performance.xml");
        assertThat(pmd.performPmdAnalysis("test", pmd.initConfiguration("pmd")))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testJavaSecurity() {
        var pmd = PMD_OPERATION.ruleSets("category/java/security.xml");
        assertThat(pmd.performPmdAnalysis("test", pmd.initConfiguration("pmd")))
                .as("no errors").isEqualTo(0);
    }

    @Test
    void testPmdOperation() {
        assertThat(PMD_OPERATION.performPmdAnalysis("test", PMD_OPERATION.initConfiguration("pmd")))
                .as("no errors").isEqualTo(0);
    }
}
