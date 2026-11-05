# Java with GraalVM 25 LTS native image template

* Written in `Java 25 LTS`. Version specified inside `.sdkmanrc` file using [sdkman](https://sdkman.io/usage)
* Maven `v3.9.9` with the [wrapper](https://maven.apache.org/wrapper/)
* Compiled to native executable using [GraalVM](https://www.graalvm.org/)
* Uses [virtual threads](https://docs.oracle.com/en/java/javase/25/core/virtual-threads.html)
  and [structured concurrency](https://docs.oracle.com/en/java/javase/24/core/structured-concurrency.html)
* Uses [Error Prone](https://errorprone.info/) with [NullAway](https://github.com/uber/NullAway) during compilation.
  Nullness violations fail the build. All project packages use JSpecify `@NullMarked`; use
  `org.jspecify.annotations.Nullable` for values that can be null (including generic arguments and array elements).
* Uses [Spotless](https://github.com/diffplug/spotless/) for automatic code formatting
  in [Android Open Source Project](https://source.android.com/docs/setup/contribute/code-style) style.
* Uses [grype](https://github.com/anchore/grype) to detect CVEs inside dependencies.

## Build & run

### Standard maven

* Build self-executable jar file

```bash
./mvnw clean package
```

* Run application
  Pay attention that we also need to provide `--enable-preview` during runtime because we have used
  [Structured Concurrency](https://docs.oracle.com/en/java/javase/25/core/structured-concurrency.html) which is in a
  preview mode for java 25.

```bash
./run.sh
```

### Quality gates

```bash
./mvnw clean verify
```

The default build enforces:

* Java 25 and Maven 3.9.9 or newer.
* Convergent dependency versions, release dependencies, and no duplicate dependency declarations.
* Compiler lint warnings as errors (except intentional preview API use), alongside Error Prone and NullAway.
* SpotBugs at maximum effort and low threshold, including Find Security Bugs.
* JaCoCo coverage of at least 92% of lines and 73% of branches across production code.
* PMD violations and CPD duplicate blocks of 100 tokens or more.

Coverage, PMD, and CPD checks run in the `test` phase, so `test`, `package`, and
`verify` enforce them. Reports are written to `target/site/jacoco/index.html`,
`target/pmd.xml`, and `target/cpd.xml`. Violations fail the build.

### Native image

* Build native image using maven `native` profile

If you're using Windows make sure you have [Visual Studio 2022](https://visualstudio.microsoft.com/downloads/)
installed.
It's necessary for the native image compilation.

```bash
./mvnw clean package -Pnative -DskipTests
```

* Run native executable (Windows or Unix)

```bash
./target/java-graalvm-template.exe

./target/java-graalvm-template
```

## Vulnerability Scan

We will use [grype](https://github.com/anchore/grype) as our vulnerability scanner for CI and locally.

```bash
grype . --name java-graalvm-template
```

Expected output:

```
 ✔ Indexed file system                                                                                                                  .
 ✔ Cataloged contents 47be346d20a8cd65a0cf1689c66413d0193e7de65cde1e63a8f12efcc7b7e02f
   ├── ✔ Packages                        [9 packages]
   ├── ✔ File digests                    [0 files]
   ├── ✔ Executables                     [0 executables]
   └── ✔ File metadata                   [0 locations]
 ✔ Scanned for vulnerabilities     [0 vulnerability matches]
   ├── by severity: 0 critical, 0 high, 0 medium, 0 low, 0 negligible
   └── by status:   0 fixed, 0 not-fixed, 0 ignored
No vulnerabilities found
```