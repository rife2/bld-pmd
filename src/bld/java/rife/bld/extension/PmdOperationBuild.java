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

package rife.bld.extension;

import rife.bld.Project;
import rife.bld.publish.PublishDeveloper;
import rife.bld.publish.PublishLicense;
import rife.bld.publish.PublishScm;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static rife.bld.dependencies.Repository.*;
import static rife.bld.dependencies.Scope.*;
import static rife.bld.operations.JavadocOptions.DocLinkOption.NO_MISSING;

public class PmdOperationBuild extends Project {
    public PmdOperationBuild() {
        pkg = "rife.bld.extension";
        name = "bld-pmd";
        version = version(1, 3, 2);

        javaRelease = 17;

        downloadSources = true;
        autoDownloadPurge = true;

        repositories = List.of(MAVEN_LOCAL, MAVEN_CENTRAL, RIFE2_RELEASES, RIFE2_SNAPSHOTS);

        var pmd = version(7, 16, 0);
        scope(compile)
                .include(dependency("com.uwyn.rife2", "bld", version(2, 3, 0)))
                .include(dependency("net.sourceforge.pmd", "pmd-java", pmd));
        scope(runtime)
                .include(dependency("org.slf4j", "slf4j-simple", version(2, 0, 17)));
        scope(test)
                .include(dependency("org.junit.jupiter", "junit-jupiter", version(5, 13, 4)))
                .include(dependency("org.junit.platform", "junit-platform-console-standalone", version(1, 13, 4)))
                .include(dependency("org.assertj", "assertj-core", version(3, 27, 3)));

        javadocOperation()
                .javadocOptions()
                .author()
                .docLint(NO_MISSING)
                .link("https://rife2.github.io/bld/")
                .link("https://rife2.github.io/rife2/")
                .link("https://javadoc.io/doc/net.sourceforge.pmd/pmd-core/latest/");

        publishOperation()
                .repository(version.isSnapshot() ? repository("rife2-snapshot") : repository("rife2"))
                .repository(repository("github"))
                .info()
                .groupId("com.uwyn.rife2")
                .artifactId("bld-pmd")
                .description("bld Extension to Perform Static Code Analysis with PMD")
                .url("https://github.com/rife2/bld-pmd")
                .developer(new PublishDeveloper()
                        .id("ethauvin").name("Erik C. Thauvin")
                        .email("erik@thauvin.net")
                        .url("https://erik.thauvin.net/")
                )
                .license(new PublishLicense()
                        .name("The Apache License, Version 2.0")
                        .url("https://www.apache.org/licenses/LICENSE-2.0.txt")
                )
                .scm(new PublishScm()
                        .connection("scm:git:https://github.com/rife2/bld-pmd.git")
                        .developerConnection("scm:git:git@github.com:rife2/bld-pmd.git")
                        .url("https://github.com/rife2/bld-pmd")
                )
                .signKey(property("sign.key"))
                .signPassphrase(property("sign.passphrase"));
    }

    public static void main(String[] args) {
        new PmdOperationBuild().start(args);
    }

    @Override
    public void test() throws Exception {
        var testResultsDir = "build/test-results/test/";

        var op = testOperation().fromProject(this);
        op.testToolOptions().reportsDir(new File(testResultsDir));

        Exception ex = null;
        try {
            op.execute();
        } catch (Exception e) {
            ex = e;
        }

        var xunitViewer = new File("/usr/bin/xunit-viewer");
        if (xunitViewer.exists() && xunitViewer.canExecute()) {
            var reportsDir = "build/reports/tests/test/";

            Files.createDirectories(Path.of(reportsDir));

            new ExecOperation()
                    .fromProject(this)
                    .command(xunitViewer.getPath(), "-r", testResultsDir, "-o", reportsDir + "index.html")
                    .execute();
        }

        if (ex != null) {
            throw ex;
        }
    }
}
