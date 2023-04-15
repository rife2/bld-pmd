package rife.bld.extension;

import rife.bld.Project;
import rife.bld.publish.PublishDeveloper;
import rife.bld.publish.PublishLicense;
import rife.bld.publish.PublishScm;

import java.util.List;

import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.*;
import static rife.bld.operations.JavadocOptions.DocLinkOption.NO_MISSING;


public class PmdOperationBuild extends Project {
    public PmdOperationBuild() {
        pkg = "rife.bld.extension";
        name = "bld-pmd";
        version = version(0, 9, 0, "SNAPSHOT");

        javaRelease = 17;
        downloadSources = true;
        autoDownloadPurge = true;
        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES);

        scope(compile)
                .include(dependency("com.uwyn.rife2", "rife2", version(1, 5, 20)))
                .include(dependency("net.sourceforge.pmd:pmd-java:6.55.0"));
        scope(runtime)
                .include(dependency("net.sourceforge.pmd:pmd:6.55.0"));
        scope(test)
                .include(dependency("org.junit.jupiter", "junit-jupiter", version(5, 9, 2)))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1, 9, 2)))
                .include(dependency("org.assertj:assertj-joda-time:2.2.0"));

        javadocOperation()
                .javadocOptions()
                .docLint(NO_MISSING)
                .link("https://rife2.github.io/rife2/")
                .link("https://javadoc.io/doc/net.sourceforge.pmd/pmd-core/latest/");

        publishOperation()
//                .repository(MAVEN_LOCAL)
                .repository(version.isSnapshot() ? repository("rife2-snapshot") : repository("rife2"))
                .info()
                .groupId("com.uwyn.rife2")
                .artifactId("bld-pmd")
                .description("bld Extension to Perform Static Code Analysis with PMD")
                .url("https://github.com/rife2/bld-pmd")
                .developer(new PublishDeveloper().id("ethauvin").name("Erik C. Thauvin").email("erik@thauvin.net")
                        .url("https://erik.thauvin.net/"))
                .license(new PublishLicense().name("The Apache License, Version 2.0")
                        .url("http://www.apache.org/licenses/LICENSE-2.0.txt"))
                .scm(new PublishScm().connection("scm:git:https://github.com/rife2/bld-pmd.git")
                        .developerConnection("scm:git:git@github.com:rife2/bld-pmd.git")
                        .url("https://github.com/rife2/bld-pmd"))
                .signKey(property("sign.key"))
                .signPassphrase(property("sign.passphrase"));
    }

    public static void main(String[] args) {
        new PmdOperationBuild().start(args);
    }
}