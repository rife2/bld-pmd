package rife.bld.extension;

import rife.bld.Project;

import java.util.List;

import static rife.bld.dependencies.Repository.MAVEN_CENTRAL;
import static rife.bld.dependencies.Repository.RIFE2_RELEASES;
import static rife.bld.dependencies.Scope.*;

public class PmdOperationBuild extends Project {
    public PmdOperationBuild() {
        pkg = "rife.bld.extension";
        name = "PmdOperation";
        mainClass = "rife.bld.extension.PmdOperationMain";
        version = version(0, 9, 0, "SNAPSHOT");

        downloadSources = true;
        repositories = List.of(MAVEN_CENTRAL, RIFE2_RELEASES);
        scope(compile)
                .include(dependency("com.uwyn.rife2", "rife2", version(1, 5, 19)))
                .include(dependency("net.sourceforge.pmd:pmd-java:6.55.0"));
        scope(runtime)
                .include(dependency("net.sourceforge.pmd:pmd:6.55.0"));
        scope(test)
                .include(dependency("org.junit.jupiter", "junit-jupiter", version(5, 9, 2)))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1, 9, 2)))
                .include(dependency("org.assertj:assertj-joda-time:2.2.0"));
    }

    public static void main(String[] args) {
        new PmdOperationBuild().start(args);
    }
}