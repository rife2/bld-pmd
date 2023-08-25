package rife.bld.extension;

import rife.bld.BuildCommand;
import rife.bld.Project;
import rife.bld.publish.PublishDeveloper;
import rife.bld.publish.PublishLicense;
import rife.bld.publish.PublishScm;

import java.io.IOException;
import java.util.List;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Repository.RIFE2_RELEASES;
import static rife.bld.dependencies.Scope.compile;
import static rife.bld.dependencies.Scope.runtime;
import static rife.bld.dependencies.Scope.test;
import static rife.bld.operations.JavadocOptions.DocLinkOption.NO_MISSING;

public class PmdOperationBuild extends Project {
    public PmdOperationBuild() {
        pkg = "rife.bld.extension";
        name = "bld-pmd";
        version = version(0, 9, 3, "SNAPSHOT");

        javaRelease = 17;
        downloadSources = true;
        autoDownloadPurge = true;
        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES);

        var pmd = version(7, 0, 0, "rc3");
        scope(compile)
                .include(dependency("com.uwyn.rife2", "bld", version(1, 7, 2)))
                .include(dependency("net.sourceforge.pmd", "pmd-java", pmd));
        scope(runtime)
                .include(dependency("net.sourceforge.pmd", "pmd-java", pmd))
                .include(dependency("org.slf4j", "slf4j-simple", version(2,0,7)));
        scope(test)
                .include(dependency("org.junit.jupiter", "junit-jupiter", version(5, 10, 0)))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1, 10, 0)))
                .include(dependency("org.assertj:assertj-joda-time:2.2.0"));

        javadocOperation()
                .javadocOptions()
                .docLint(NO_MISSING)
                .link("https://rife2.github.io/bld/")
                .link("https://rife2.github.io/rife2/")
                .link("https://javadoc.io/doc/net.sourceforge.pmd/pmd-core/latest/");

        publishOperation()
                // .repository(MAVEN_LOCAL)
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

    @BuildCommand(summary = "Generates JaCoCo Reports")
    public void jacoco() throws IOException {
        new JacocoReportOperation()
                .fromProject(this)
                .execute();
    }
}
