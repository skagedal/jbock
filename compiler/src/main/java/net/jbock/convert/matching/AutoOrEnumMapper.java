package net.jbock.convert.matching;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.ParameterSpec;
import io.jbock.util.Either;
import net.jbock.annotated.AnnotatedMethod;
import net.jbock.common.TypeTool;
import net.jbock.common.Util;
import net.jbock.common.ValidationFailure;
import net.jbock.convert.Mapping;
import net.jbock.validate.ValidateScope;

import javax.inject.Inject;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import java.util.Arrays;
import java.util.stream.Collectors;

import static io.jbock.util.Either.left;
import static net.jbock.common.Constants.STRING;

@ValidateScope
public class AutoOrEnumMapper {

    private final AutoMapper autoMapper;
    private final Util util;
    private final MatchFinder matchFinder;

    @Inject
    AutoOrEnumMapper(
            AutoMapper autoMapper,
            Util util,
            MatchFinder matchFinder) {
        this.autoMapper = autoMapper;
        this.util = util;
        this.matchFinder = matchFinder;
    }

    public <M extends AnnotatedMethod>
    Either<ValidationFailure, Mapping<M>> findMapping(
            M sourceMethod) {
        return matchFinder.findMatch(sourceMethod)
                .flatMap(match -> autoMapper.findAutoMapping(match)
                        .<Either<ValidationFailure, Mapping<M>>>map(Either::right)
                        .orElseGet(() -> findEnumMapping(sourceMethod, match)));
    }

    private <M extends AnnotatedMethod>
    Either<ValidationFailure, Mapping<M>> findEnumMapping(
            M sourceMethod,
            ValidMatch<M> match) {
        TypeMirror baseType = match.baseType();
        return TypeTool.AS_DECLARED.visit(baseType)
                .map(DeclaredType::asElement)
                .flatMap(TypeTool.AS_TYPE_ELEMENT::visit)
                .filter(element -> element.getKind() == ElementKind.ENUM)
                .map(TypeElement::asType)
                .<Either<ValidationFailure, TypeMirror>>map(Either::right)
                .orElseGet(() -> left(sourceMethod.fail(util.noMatchError(baseType))))
                .map(enumType -> {
                    CodeBlock code = enumConversionCode(enumType);
                    return Mapping.create(code, match, true);
                });
    }

    private CodeBlock enumConversionCode(TypeMirror enumType) {
        ParameterSpec token = ParameterSpec.builder(STRING, "token").build();
        ParameterSpec e = ParameterSpec.builder(IllegalArgumentException.class, "e").build();
        ParameterSpec values = ParameterSpec.builder(STRING, "values").build();
        ParameterSpec message = ParameterSpec.builder(STRING, "message").build();
        CodeBlock.Builder code = CodeBlock.builder();
        code.add("try {\n").indent()
                .addStatement("return $T.valueOf($N)", enumType, token)
                .unindent()
                .add("} catch ($T $N) {\n", IllegalArgumentException.class, e).indent()
                .add("$T $N = $T.stream($T.values())\n", STRING, values, Arrays.class, enumType).indent()
                .add(".map($T::name)\n", enumType)
                .addStatement(".collect($T.joining($S, $S, $S))", Collectors.class, ", ", "[", "]")
                .unindent()
                .addStatement("$T $N = $N.getMessage() + $S + $N", STRING, message, e, " ", values)
                .addStatement("throw new $T($N)", IllegalArgumentException.class, message)
                .unindent()
                .add("}\n");
        return code.build();
    }
}
