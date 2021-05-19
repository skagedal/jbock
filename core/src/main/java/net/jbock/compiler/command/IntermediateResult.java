package net.jbock.compiler.command;

import net.jbock.compiler.ValidationFailure;
import net.jbock.compiler.parameter.PositionalParameter;
import net.jbock.convert.ConvertedParameter;
import net.jbock.either.Either;
import net.jbock.qualifier.SourceMethod;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static net.jbock.either.Either.left;
import static net.jbock.either.Either.right;

class IntermediateResult {

  private final List<SourceMethod> options;
  private final List<ConvertedParameter<PositionalParameter>> positionalParameters;

  private IntermediateResult(
      List<SourceMethod> options,
      List<ConvertedParameter<PositionalParameter>> positionalParameters) {
    this.options = options;
    this.positionalParameters = positionalParameters;
  }

  static Either<List<ValidationFailure>, IntermediateResult> create(
      List<SourceMethod> options,
      List<ConvertedParameter<PositionalParameter>> positionalParameters) {
    List<ValidationFailure> failures = validatePositions(positionalParameters);
    if (!failures.isEmpty()) {
      return left(failures);
    }
    return right(new IntermediateResult(options, positionalParameters));
  }

  private static List<ValidationFailure> validatePositions(
      List<ConvertedParameter<PositionalParameter>> params) {
    List<ConvertedParameter<PositionalParameter>> sorted = params.stream()
        .sorted(Comparator.comparing(c -> c.parameter().position()))
        .collect(Collectors.toList());
    List<ValidationFailure> failures = new ArrayList<>();
    for (int i = 0; i < sorted.size(); i++) {
      ConvertedParameter<PositionalParameter> c = sorted.get(i);
      PositionalParameter p = c.parameter();
      if (p.position() != i) {
        String message = "Position " + p.position() + " is not available. Suggested position: " + i;
        failures.add(p.fail(message));
      }
    }
    return failures;
  }

  List<SourceMethod> options() {
    return options;
  }

  List<ConvertedParameter<PositionalParameter>> positionalParameters() {
    return positionalParameters;
  }
}