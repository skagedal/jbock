package net.jbock.examples;


import net.jbock.Command;
import net.jbock.Option;
import net.jbock.either.Optional;

import java.util.List;

@Command
abstract class NoNameArguments {

  @Option(names = "--message")
  abstract Optional<String> message();

  @Option(names = "--file")
  abstract List<String> file();

  @Option(names = {"--verbosity", "-v"})
  abstract Optional<Integer> verbosity();

  @Option(names = {"--number", "-n"})
  abstract int number();

  @Option(names = "--cmos")
  abstract boolean cmos();
}
