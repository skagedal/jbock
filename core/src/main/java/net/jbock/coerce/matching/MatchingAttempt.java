package net.jbock.coerce.matching;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import net.jbock.coerce.Coercion;
import net.jbock.coerce.MapperClassValidator;
import net.jbock.coerce.NonFlagCoercion;
import net.jbock.coerce.NonFlagSkew;
import net.jbock.coerce.either.Either;
import net.jbock.compiler.EnumName;
import net.jbock.compiler.ParameterScoped;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.function.Function;
import java.util.stream.Collectors;

class MatchingAttempt {

  private final CodeBlock extractExpr;
  private final ParameterSpec constructorParam;
  private final NonFlagSkew skew;
  private final TypeMirror testType;
  private final TypeElement mapperClass;

  MatchingAttempt(TypeMirror testType, CodeBlock extractExpr, ParameterSpec constructorParam, NonFlagSkew skew, TypeElement mapperClass) {
    this.testType = testType;
    this.extractExpr = extractExpr;
    this.constructorParam = constructorParam;
    this.skew = skew;
    this.mapperClass = mapperClass;
  }

  static CodeBlock autoCollectExpr(ClassName optionType, EnumName enumName, NonFlagSkew skew) {
    switch (skew) {
      case OPTIONAL:
        return CodeBlock.of(".findAny()");
      case REQUIRED:
        return CodeBlock.of(".findAny().orElseThrow($T.$L::missingRequired)", optionType,
            enumName.enumConstant());
      case REPEATABLE:
        return CodeBlock.of(".collect($T.toList())", Collectors.class);
      default:
        throw new AssertionError("unknown skew: " + skew);
    }
  }

  Either<String, Coercion> findCoercion(ParameterScoped basicInfo) {
    MapperClassValidator validator = new MapperClassValidator(basicInfo::failure, basicInfo.tool(), testType, mapperClass);
    return validator.getMapExpr().map(Function.identity(), mapExpr -> {
      CodeBlock expr = autoCollectExpr(basicInfo.optionType(), basicInfo.enumName(), skew);
      return new NonFlagCoercion(basicInfo.enumName(), mapExpr, expr, extractExpr, skew, constructorParam);
    });
  }
}
