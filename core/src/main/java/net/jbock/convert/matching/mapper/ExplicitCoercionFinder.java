package net.jbock.convert.matching.mapper;

import com.google.common.collect.ImmutableList;
import com.squareup.javapoet.CodeBlock;
import net.jbock.Converter;
import net.jbock.convert.ConvertedParameter;
import net.jbock.convert.Util;
import net.jbock.convert.matching.Match;
import net.jbock.convert.matching.matcher.Matcher;
import net.jbock.convert.reference.FunctionType;
import net.jbock.convert.reference.ReferenceTool;
import net.jbock.compiler.ParameterContext;
import net.jbock.compiler.ParameterScoped;
import net.jbock.compiler.parameter.AbstractParameter;
import net.jbock.either.Either;

import javax.inject.Inject;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import static javax.lang.model.element.Modifier.ABSTRACT;

public class ExplicitCoercionFinder extends ParameterScoped {

  private final ImmutableList<Matcher> matchers;
  private final ReferenceTool referenceTool;
  private final Util util;

  @Inject
  ExplicitCoercionFinder(
      ParameterContext context,
      ImmutableList<Matcher> matchers,
      ReferenceTool referenceTool,
      Util util) {
    super(context);
    this.matchers = matchers;
    this.referenceTool = referenceTool;
    this.util = util;
  }

  public <P extends AbstractParameter> Either<String, ConvertedParameter<P>> findCoercion(
      P parameter,
      TypeElement converter) {
    Optional<String> maybeFailure = util.commonTypeChecks(converter).map(s -> "converter " + s);
    return Either.<String, Void>fromFailure(maybeFailure, null)
        .filter(nothing -> checkNotAbstract(converter))
        .filter(nothing -> checkNoTypevars(converter))
        .filter(nothing -> checkMapperAnnotation(converter))
        .flatMap(nothing -> referenceTool.getReferencedType(converter))
        .flatMap(functionType -> tryAllMatchers(functionType, parameter, converter));
  }

  private <P extends AbstractParameter> Either<String, ConvertedParameter<P>> tryAllMatchers(
      FunctionType functionType,
      P parameter,
      TypeElement converter) {
    List<Match> matches = new ArrayList<>();
    for (Matcher matcher : matchers) {
      Optional<Match> match = matcher.tryMatch(parameter);
      match.ifPresent(matches::add);
      match = match.filter(m -> isValidMatch(m, functionType));
      if (match.isPresent()) {
        return Either.fromSuccess("", match)
            .map(m -> {
              CodeBlock mapExpr = getMapExpr(functionType, converter);
              return m.toCoercion(mapExpr, parameter);
            });
      }
    }
    Match message = matches.stream()
        .max(Comparator.comparing(Match::skew))
        .orElseThrow(AssertionError::new); // exact matcher always matches
    return Either.left(ExplicitCoercionFinder.noMatchError(message.baseReturnType()));
  }

  private Optional<String> checkMapperAnnotation(TypeElement converter) {
    Converter converterAnnotation = converter.getAnnotation(Converter.class);
    boolean nestedMapper = util.getEnclosingElements(converter).contains(sourceElement());
    if (converterAnnotation == null && !nestedMapper) {
      return Optional.of("converter must be an inner class of the command class, or carry the @" + Converter.class.getSimpleName() + " annotation");
    }
    return Optional.empty();
  }

  private Optional<String> checkNotAbstract(TypeElement converter) {
    if (converter.getModifiers().contains(ABSTRACT)) {
      return Optional.of("non-abstract converter class");
    }
    return Optional.empty();
  }

  private Optional<String> checkNoTypevars(TypeElement converter) {
    if (!converter.getTypeParameters().isEmpty()) {
      return Optional.of("found type parameters in converter class declaration");
    }
    return Optional.empty();
  }

  private CodeBlock getMapExpr(FunctionType functionType, TypeElement converter) {
    return CodeBlock.of("new $T()$L", converter.asType(),
        functionType.isSupplier() ? ".get()" : "");
  }

  private boolean isValidMatch(Match match, FunctionType functionType) {
    return tool().isSameType(functionType.outputType(), match.baseReturnType());
  }

  private static String noMatchError(TypeMirror type) {
    return "converter should implement Function<String, " + Util.typeToString(type) + ">";
  }
}
