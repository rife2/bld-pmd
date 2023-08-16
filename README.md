# [Bld](https://rife2.com/bld) Extension to Perform Static Code Analysis with [PMD](https://pmd.github.io/)


[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![Release](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/releases/com/uwyn/rife2/bld-pmd/maven-metadata.xml?color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-pmd)
[![Snapshot](https://flat.badgen.net/maven/v/metadata-url/repo.rife2.com/snapshots/com/uwyn/rife2/bld-pmd/maven-metadata.xml?label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-pmd)
[![GitHub CI](https://github.com/rife2/bld-pmd/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-pmd/actions/workflows/bld.yml)

To install, please refer to the [extensions documentation](https://github.com/rife2/bld/wiki/Extensions).

To check all source code using the [java quickstart rule](https://pmd.github.io/pmd/pmd_rules_java.html), add the following to your build file

```java
@BuildCommand
public void pmd() throws Exception {
    new PmdOperation()
        .fromProject(this)
        .execute();
}
```

```text
./bld pmd test
```

To check the main source directory using a custom rule, [java error prone rule](https://pmd.github.io/pmd/pmd_rules_java.html) and failing on any violation.

```java
@BuildCommand
public void pmdMain() throws Exception {
    new PmdOperation()
            .fromProject(this)
            .failOnViolation(true)
            .inputPaths(this.srcMainDirectory().toPath())
            .ruleSets("config/pmd.xml", "category/java/errorprone.xml")
            .execute();
}
```

```text
./bld compile pmdMain
```

Please check the [PmdOperation documentation](https://rife2.github.io/bld-pmd/rife/bld/extension/PmdOperation.html#method-summary) for all available configuration options.
