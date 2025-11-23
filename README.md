# [bld](https://rife2.com/bld) Extension to Perform Static Code Analysis with [PMD](https://pmd.github.io/)

[![License](https://img.shields.io/badge/license-Apache%20License%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![bld](https://img.shields.io/badge/2.3.0-FA9052?label=bld&labelColor=2392FF)](https://rife2.com/bld)
[![Release](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Freleases%2Fcom%2Fuwyn%2Frife2%2Fbld-pmd%2Fmaven-metadata.xml&color=blue)](https://repo.rife2.com/#/releases/com/uwyn/rife2/bld-pmd)
[![Snapshot](https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo.rife2.com%2Fsnapshots%2Fcom%2Fuwyn%2Frife2%2Fbld-pmd%2Fmaven-metadata.xml&label=snapshot)](https://repo.rife2.com/#/snapshots/com/uwyn/rife2/bld-pmd)
[![GitHub CI](https://github.com/rife2/bld-pmd/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-pmd/actions/workflows/bld.yml)

To install the latest version, add the following to the `lib/bld/bld-wrapper.properties` file:

```properties
bld.extension-pmd=com.uwyn.rife2:bld-pmd
```

For more information, please refer to the [extensions](https://github.com/rife2/bld/wiki/Extensions) documentation.

## Check Source with PMD

To check all source  Codecode using the [Java Quickstart](https://docs.pmd-code.org/latest/pmd_rules_java.html) configuration, add the following to your build file:

```java
@BuildCommand(summary = "Checks source code with PMD")
public void pmd() throws Exception {
    new PmdOperation()
        .fromProject(this)
        .execute();
}
```

```console
./bld pmd test
```

To check the main source directory using a custom ruleset, [Java Error Prone](https://docs.pmd-code.org/latest/pmd_rules_java.html#error-prone) configuration, and failing on any violation.

```java
@BuildCommand(value = "pmd-main", summary = "Checks main source code with PMD")
public void pmdMain() throws Exception {
    new PmdOperation()
            .fromProject(this)
            .failOnViolation(true)
            .inputPaths(srcMainDirectory())
            .ruleSets("config/pmd.xml", "category/java/errorprone.xml")
            .execute();
}
```

```console
./bld compile pmd-main
```

Please check the [PmdOperation documentation](https://rife2.github.io/bld-pmd/rife/bld/extension/PmdOperation.html#method-summary) for all available configuration options.
