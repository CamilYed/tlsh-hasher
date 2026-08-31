# Contributing

Issues, design discussions, documentation improvements, and pull requests are welcome. This is an
educational implementation, so readable reasoning and focused tests are as important as a working
change.

## Before opening a pull request

Use JDK 25 and the committed Gradle wrapper. Run the same verification as CI:

```shell
./gradlew spotlessCheck clean build
```

Apply the repository's Google Java Format rules with:

```shell
./gradlew spotlessApply
```

New behavior should normally begin with a focused JUnit test and use AssertJ assertions. Keep
algorithm Javadoc understandable without requiring prior TLSH knowledge. Prefer descriptive TLSH
domain names and use `final` for values that are not reassigned.

## Cutting a release

1. Confirm that `main` is clean and CI passes.
2. Move the release notes under a `## [X.Y.Z] — YYYY-MM-DD` heading in `CHANGELOG.md` and commit it.
3. Make the Central Portal signing key available as `SIGNING_KEY` and its password as
   `SIGNING_PASSWORD`, then build the exact release artifacts locally:

   ```shell
   ./gradlew -PreleaseVersion=X.Y.Z spotlessCheck clean build \
     publishAllPublicationsToLocalBuildRepository
   ```

4. Inspect the JARs, POM, Gradle metadata, checksums, and signatures under `build/staging-deploy`.
5. Publish the artifacts before creating the Git tag, so a tag never advertises a version that
   users cannot obtain.
6. Create one annotated tag pointing at the published commit:

   ```shell
   git tag -a vX.Y.Z -m "Release vX.Y.Z"
   git push origin vX.Y.Z
   ```

7. Create a GitHub Release from that tag and use the matching `CHANGELOG.md` section as its notes.

The first public release should be performed manually and reviewed at every stage. A release
workflow can automate these exact steps after the GitHub repository and Central Portal secrets are
configured.
