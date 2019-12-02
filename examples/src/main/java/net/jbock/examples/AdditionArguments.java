package net.jbock.examples;

import net.jbock.CLI;
import net.jbock.Param;

import java.util.Optional;

// Allow positional arguments to start with a dash, so negative numbers can be passed.
@CLI
abstract class AdditionArguments {

  /**
   * First argument
   */
  @Param(value = 1)
  abstract int a();

  /**
   * Second argument
   */
  @Param(value = 2)
  abstract int b();

  /**
   * Optional third argument
   */
  @Param(value = 3)
  abstract Optional<Integer> c();

  final int sum() {
    return a() + b() + c().orElse(0);
  }
}
