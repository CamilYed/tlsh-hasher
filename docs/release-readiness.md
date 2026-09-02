# Release readiness

Version `0.1.0` has been published to Maven Central. This document records the checks performed for
that release and the safeguards retained for future releases.

## Verified release artifacts

The `0.1.0` Maven publication was generated into the local `build/staging-deploy` repository,
signed, validated by the Central Publisher Portal, and published under the coordinates
`io.github.camilyed:tlsh-hasher:0.1.0`.

- The binary JAR contains `module-info.class` and exports only `io.github.camilyed.tlsh`.
- The module has no runtime dependency beyond the mandated `java.base` module.
- Binary and source JARs contain the Apache 2.0 license under `META-INF/LICENSE`.
- Source and Javadoc JARs are generated alongside the binary artifact.
- The POM contains project name, description, URL, license, developer, and SCM metadata.
- Gradle module metadata describes Java 25 API and runtime variants plus source and Javadoc
  variants.
- Archive entries use reproducible timestamps rather than local source-file modification times.
- A non-published smoke-test project compiles and runs as the separate named module
  `io.github.camilyed.tlsh.smoke` during every root `check`.
- The binary, source, Javadoc, POM, and Gradle module metadata artifacts have been signed and their
  detached ASCII-armored signatures have been verified locally with GnuPG.

The local staging command is:

```shell
./gradlew clean publishMavenJavaPublicationToLocalBuildRepository
```

This command writes only under `build`; it does not publish externally. Signing is intentionally
skipped for this local repository.

The published POM, binary JAR, source JAR, and Javadoc JAR were retrieved successfully from the
public Maven Central repository. A clean external Gradle project then resolved version `0.1.0`
through `mavenCentral()`, compiled as a separate named JPMS module, and calculated a digest at
runtime. The binary JAR rebuilt from the tagged sources was byte-for-byte identical to the public
artifact; both had SHA-256
`6d3e5284deb4e22ab579f1aa27553295c10d8368cd86b98b42333a40fc045f0d`.

The manually triggered `Release dry run` GitHub Actions workflow performs the complete test suite,
creates a non-snapshot signed staging repository, verifies every detached signature, and uploads a
candidate Central Portal ZIP as a seven-day workflow artifact. It has read-only repository
permissions and never contacts a publishing endpoint.

Run it from the GitHub Actions page or with:

```shell
gh workflow run release-dry-run.yml -f version=0.1.0
```

## Central Portal staging

The manually triggered `Stage Maven Central release` workflow repeats the dry-run checks and then
uploads the verified ZIP to the Central Publisher Portal with the `USER_MANAGED` publishing type.
It waits for the portal to validate the candidate, but it cannot publish the candidate, create a Git
tag, or create a GitHub Release. Those irreversible release steps require a separate decision after
the validated deployment has been inspected in the portal.

Run it only from `main`, using the intended immutable release version:

```shell
gh workflow run release.yml --ref main -f version=0.1.0
```

The completed workflow summary contains the Central Portal deployment ID. A `VALIDATED` deployment
is still private and can either be tested and published or dropped from the portal.

## Release signing identity

TLSH Hasher releases use a dedicated RSA 3072-bit primary signing key. The public identity is
`CamilYed (TLSH releases) <kamil.jedrzejuk@gmail.com>` and its fingerprint is:

```text
A297 CD5F 1C68 59C3 E88C  3868 8726 4607 2BBE 5586
```

The key expires on 2028-09-01 and must be extended or replaced before that date. Its public part is
distributed through `keyserver.ubuntu.com` and is verified as downloadable by fingerprint from
`keys.openpgp.org`. The encrypted private key and passphrase are supplied to GitHub Actions through
the `SIGNING_KEY` and `SIGNING_PASSWORD` repository secrets; neither value is stored in Git. An
encrypted private-key backup and the revocation certificate must remain outside the repository.

## Required for future releases

- Stabilize the public construction API before `1.0.0` after the implementation-module decision.
- Decide artifact IDs and JPMS module names before publishing any additional modules.
- Verify generated signatures and all files in the final staging repository.
- Test consumption with both Gradle and Maven using the final release coordinates.
- Run the complete compatibility suite, including the official TLSH 5.0.0 corpus.
- Review the release notes and known limitations, especially the supported `T1` configuration and
  conservative 256-byte minimum input size.
- Publish the artifact before creating the matching Git tag and GitHub Release.

No token, password, private key, or other publishing secret belongs in this repository.
