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
 * All built-in rules available for Java.
 *
 * @author <a href="https://erik.thauvin.net/">Erik C. Thauvin</a>
 * @since 1.5.0
 */
public enum JavaRules {
    /**
     * Best Practices
     */
    BEST_PRACTICES("category/java/bestpractices.xml"),
    /**
     * Code Style
     */
    CODE_STYLE("category/java/codestyle.xml"),
    /**
     * Design
     */
    DESIGN("category/java/design.xml"),
    /**
     * Documentation
     */
    DOCUMENTATION("category/java/documentation.xml"),
    /**
     * Error Prone
     */
    ERROR_PRONE("category/java/errorprone.xml"),
    /**
     * Multithreading
     */
    MULTITHREADING("category/java/multithreading.xml"),
    /**
     * Performance
     */
    PERFORMANCE("category/java/performance.xml"),
    /**
     * Quick Start (Default RuleSet)
     */
    QUICK_START("rulesets/java/quickstart.xml"),
    /**
     * Security
     */
    SECURITY("category/java/security.xml");

    private final String category;

    JavaRules(String category) {
        this.category = category;
    }

    public String getCategory() {
        return category;
    }
}
