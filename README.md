# [Bld](https://rife2.com/bld) Extension to Perform Static Code Analysis with [PMD](https://pmd.github.io/)


[![License (3-Clause BSD)](https://img.shields.io/badge/license-BSD%203--Clause-blue.svg?style=flat-square)](http://opensource.org/licenses/BSD-3-Clause)
[![Java](https://img.shields.io/badge/java-17%2B-blue)](https://www.oracle.com/java/technologies/javase/jdk17-archive-downloads.html)
[![GitHub CI](https://github.com/rife2/bld-pmd/actions/workflows/bld.yml/badge.svg)](https://github.com/rife2/bld-pmd/actions/workflows/bld.yml)

To check all source code using the [java quickstart rule](https://pmd.github.io/pmd/pmd_rules_java.html).

```java
@BuildCommand
public void pmd() throws Exception {
    new PmdOperation(this).execute();
}
```
```text
./bld pmd test
```

To check the main source code using a custom rule, [java error prone rule](https://pmd.github.io/pmd/pmd_rules_java.html) and failing on any violation.

```java
@BuildCommand
public void pmdMain() throws Exception {
    new PmdOperation(this)
        .failOnValidation(true)
        .addInputPath(project.srcMainDirectory().toPath())
        .addRuletSet("config/pmd.xml", "category/java/errorprone.xml");
        .execute();
}
```

```text
./dld compile pmdMain
```

Please check the [PmdOperation documentation](https://rife2.github.io/bld-pmd/rife/bld/extension/PmdOperation.html#method-summary) for all available configuration options.