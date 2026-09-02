# Release readiness

This project remains intentionally unpublished. The checklist records what has been verified
locally and what must still be completed before creating a version tag or public artifact.

## Verified snapshot artifacts

The `0.1.0-SNAPSHOT` Maven publication has been generated into the local
`build/staging-deploy` repository and inspected without contacting a remote publishing service.

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

The manually triggered `Release dry run` GitHub Actions workflow performs the complete test suite,
creates a non-snapshot signed staging repository, verifies every detached signature, and uploads a
candidate Central Portal ZIP as a seven-day workflow artifact. It has read-only repository
permissions and never contacts a publishing endpoint.

Run it from the GitHub Actions page or with:

```shell
gh workflow run release-dry-run.yml -f version=0.1.0
```

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

## Required before publication

- Stabilize the public construction API after the implementation-module decision.
- Decide the final artifact IDs and JPMS module names before the first non-snapshot release.
- Add the real remote repository configuration only when a publication target has been selected.
- Verify generated signatures and all files in the final staging repository.
- Test consumption with both Gradle and Maven using the final release coordinates.
- Run the complete compatibility suite, including the official TLSH 5.0.0 corpus.
- Review the release notes and known limitations, especially the supported `T1` configuration and
  conservative 256-byte minimum input size.
- Publish the artifact before creating the matching Git tag and GitHub Release.

No token, password, private key, or other publishing secret belongs in this repository.
