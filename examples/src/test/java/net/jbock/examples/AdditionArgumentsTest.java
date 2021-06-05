package net.jbock.examples;

import net.jbock.examples.fixture.ParserTestFixture;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdditionArgumentsTest {

  private final AdditionArgumentsParser parser = new AdditionArgumentsParser();

  private final ParserTestFixture<AdditionArguments> f =
      ParserTestFixture.create(parser);

  @Test
  void optionalAbsent() {
    f.assertThat("1", "2").succeeds(
        "a", 1,
        "b", 2,
        "c", Optional.empty());
  }

  @Test
  void optionalPresent() {
    f.assertThat("1", "2", "3").succeeds(
        "a", 1,
        "b", 2,
        "c", Optional.of(3));
  }

  @Test
  void wrongNumber() {
    assertTrue(parser.parse("--", "-a", "2").getLeft().map(f::castToError)
        .orElseThrow().message()
        .contains("while converting parameter A: For input string: \"-a\""));
  }

  @Test
  void dashesIgnored() {
    f.assertThat("--", "1", "-2", "3").satisfies(e -> e.sum() == 2);
    f.assertThat("--", "-1", "-2", "-3").satisfies(e -> e.sum() == -6);
    f.assertThat("--", "-1", "-2", "3").satisfies(e -> e.sum() == 0);
    f.assertThat("--", "-1", "-2").satisfies(e -> e.sum() == -3);
  }

  @Test
  void testPrint() {
    f.assertPrintsHelp(
        "USAGE",
        "  addition-arguments A B [C]",
        "",
        "PARAMETERS",
        "  A  First argument",
        "  B  Second argument",
        "  C  Optional third argument",
        "");
  }
}
