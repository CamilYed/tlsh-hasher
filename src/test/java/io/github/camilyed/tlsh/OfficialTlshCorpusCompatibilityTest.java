package io.github.camilyed.tlsh;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies this implementation against real files and results shipped with TLSH 5.0.0. */
final class OfficialTlshCorpusCompatibilityTest {

  private static final String FIXTURE_DIRECTORY_SYSTEM_PROPERTY = "tlsh.officialFixtureDirectory";

  @Test
  void shouldMatchOfficialFileDigestsAndDistanceScores() throws IOException {
    // given
    final String fixtureDirectory = System.getProperty(FIXTURE_DIRECTORY_SYSTEM_PROPERTY);
    assumeTrue(
        fixtureDirectory != null && !fixtureDirectory.isBlank(),
        () -> FIXTURE_DIRECTORY_SYSTEM_PROPERTY + " is not configured");
    final Path fixtureRoot = Path.of(fixtureDirectory);
    final List<OfficialFileVector> vectors =
        List.of(
            new OfficialFileVector(
                "0Alice.txt",
                "T145D18407A78523B35A030267671FA2C2F725402973629B25545EB43C3356679477F7FC"),
            new OfficialFileVector(
                "1english-only.txt",
                "T1E951784702042376169012B1BA5A76EAF36092FC3311A595B4856235278F9F973763EF"),
            new OfficialFileVector(
                "spanish_place_namesA.txt",
                "T1E481852B33C423B545639375535F5AFBB74EC694421183F0A89EC43E735698C11B9AE8"),
            new OfficialFileVector(
                "spanish_place_namesB.txt",
                "T15C12E729F30903720143429821CF67F2B75691E8D2720365B86CCA3EF693EE951F4CEA"));

    for (final OfficialFileVector vector : vectors) {
      final Path fixture = fixtureRoot.resolve(vector.fileName());
      assertThat(fixture).as("official TLSH fixture %s", vector.fileName()).isRegularFile();

      // when
      final TlshDigest digest = Tlsh.hash(fixture);

      // then
      assertThat(digest.encoded()).as(vector.fileName()).isEqualTo(vector.expectedDigest());
    }

    final TlshDigest spanishNamesA = Tlsh.hash(fixtureRoot.resolve("spanish_place_namesA.txt"));
    final TlshDigest spanishNamesB = Tlsh.hash(fixtureRoot.resolve("spanish_place_namesB.txt"));
    assertThat(spanishNamesA.distanceTo(spanishNamesB)).isEqualTo(282);
    assertThat(spanishNamesA.distanceToIgnoringLength(spanishNamesB)).isEqualTo(174);
  }

  private record OfficialFileVector(String fileName, String expectedDigest) {}
}
