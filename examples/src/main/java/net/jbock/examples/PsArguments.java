package net.jbock.examples;

import net.jbock.CommandLineArguments;
import net.jbock.Parameter;

import java.util.Optional;

@CommandLineArguments
abstract class PsArguments {

  @Parameter(
      flag = true,
      longName = "all",
      shortName = 'a')
  abstract boolean all();

  /**
   * This is the description.
   */
  @Parameter(
      optional = true,
      longName = "width",
      shortName = 'w',
      descriptionArgumentName = "number")
  abstract Optional<Integer> width();
}
