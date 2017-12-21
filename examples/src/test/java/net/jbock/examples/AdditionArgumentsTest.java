package net.jbock.examples;

import static net.jbock.examples.fixture.PrintFixture.printFixture;

import net.jbock.examples.fixture.ParserFixture;
import org.junit.Test;

public class AdditionArgumentsTest {

  private final ParserFixture<AdditionArguments> f =
      ParserFixture.create(AdditionArguments_Parser::parse);

  @Test
  public void optionalAbsent() {
    f.assertThat("1", "2").isParsedAs(
        "a", 1,
        "b", 2);
  }

  @Test
  public void optionalPresent() {
    f.assertThat("1", "2", "3").isParsedAs(
        "a", 1,
        "b", 2,
        "c", 3);
  }

  @Test
  public void wrongNumber() {
    f.assertThat("-a", "2").throwsException(NumberFormatException.class,
        "For input string: \"-a\"");
  }

  @Test
  public void dashesIgnored() {
    f.assertThat("1", "-2", "3").satisfies(e -> e.sum() == 2);
    f.assertThat("-1", "-2", "-3").satisfies(e -> e.sum() == -6);
    f.assertThat("-1", "-2", "3").satisfies(e -> e.sum() == 0);
    f.assertThat("-1", "-2").satisfies(e -> e.sum() == -3);
  }


  @Test
  public void testPrint() {
    printFixture(AdditionArguments_Parser::printUsage).assertPrints(
        "SYNOPSIS",
        "  A B [C]",
        "",
        "DESCRIPTION",
        "",
        "");
  }
}
