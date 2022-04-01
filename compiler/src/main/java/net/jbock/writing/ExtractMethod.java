package net.jbock.writing;

import io.jbock.javapoet.ArrayTypeName;
import io.jbock.javapoet.ClassName;
import io.jbock.javapoet.CodeBlock;
import io.jbock.javapoet.MethodSpec;
import io.jbock.javapoet.ParameterSpec;
import io.jbock.javapoet.ParameterizedTypeName;
import jakarta.inject.Inject;
import net.jbock.annotated.AnnotatedOption;
import net.jbock.annotated.AnnotatedParameter;
import net.jbock.annotated.AnnotatedVarargsParameter;
import net.jbock.common.Suppliers;
import net.jbock.convert.Mapping;
import net.jbock.model.ItemType;
import net.jbock.parse.ParseResult;
import net.jbock.util.ExConvert;
import net.jbock.util.ExFailure;
import net.jbock.util.ExMissingItem;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static javax.lang.model.element.Modifier.PRIVATE;
import static net.jbock.common.Constants.EITHERS;
import static net.jbock.common.Constants.STRING;
import static net.jbock.common.Constants.STRING_ARRAY;
import static net.jbock.common.Suppliers.memoize;
import static net.jbock.writing.CodeBlocks.joinByNewline;

@WritingScope
final class ExtractMethod extends HasCommandRepresentation {

    private final GeneratedTypes generatedTypes;
    private final ParameterSpec left = ParameterSpec.builder(STRING, "left").build();

    @Inject
    ExtractMethod(
            GeneratedTypes generatedTypes,
            CommandRepresentation commandRepresentation) {
        super(commandRepresentation);
        this.generatedTypes = generatedTypes;
    }

    private final Supplier<ParameterSpec> parserSupplier = Suppliers.memoize(() -> {
        ParameterizedTypeName parserType = ParameterizedTypeName.get(ClassName.get(ParseResult.class),
                optType());
        return ParameterSpec.builder(parserType, "result").build();
    });

    private ParameterSpec parser() {
        return parserSupplier.get();
    }

    private GeneratedTypes generatedTypes() {
        return generatedTypes;
    }

    private final Supplier<MethodSpec> extractMethod = memoize(() -> {
        ParameterSpec result = ParameterSpec.builder(generatedTypes().implType(), "impl").build();
        MethodSpec.Builder spec = MethodSpec.methodBuilder("extract");
        spec.addStatement("$T $N = new $T()", result.type, result, result.type);
        for (int i = 0; i < namedOptions().size(); i++) {
            Mapping<AnnotatedOption> m = namedOptions().get(i);
            spec.addStatement("$N.$N = $L", result, m.field(), convertExpressionOption(m, i));
        }
        for (int i = 0; i < positionalParameters().size(); i++) {
            Mapping<AnnotatedParameter> m = positionalParameters().get(i);
            spec.addStatement("$N.$N = $L", result, m.field(), convertExpressionRegularParameter(m, i));
        }
        varargsParameters().forEach(m -> {
            spec.addStatement("$N.$N = $L", result, m.field(), convertExpressionRepeatableParameter(m));
        });
        generatedTypes().superResultType().ifPresentOrElse(parseResultWithRestType -> {
                    ParameterSpec restArgs = ParameterSpec.builder(sourceElement().typeName(), "restArgs").build();
                    spec.addStatement("$T $N = $N.rest().toArray($T::new)", STRING_ARRAY, restArgs,
                            parser(), ArrayTypeName.of(String.class));
                    spec.addStatement("return new $T($N, $N)", parseResultWithRestType,
                            result, restArgs);
                },
                () -> spec.addStatement("return $N", result));
        return spec.returns(generatedTypes().parseSuccessType())
                .addParameter(parser())
                .addException(ExFailure.class)
                .addModifiers(PRIVATE)
                .build();
    });

    MethodSpec get() {
        return extractMethod.get();
    }

    private CodeBlock convertExpressionOption(Mapping<AnnotatedOption> m, int i) {
        List<CodeBlock> code = new ArrayList<>();
        code.add(CodeBlock.of("$N.option($T.$N)", parser(),
                sourceElement().optionEnumType(), m.enumName()));
        if (!m.isNullary()) {
            code.add(CodeBlock.of(".map($L)", m.createConverterExpression()));
        }
        code.addAll(tailExpressionOption(m, i));
        m.extractExpr().ifPresent(code::add);
        return joinByNewline(code);
    }

    private CodeBlock convertExpressionRegularParameter(Mapping<AnnotatedParameter> m, int i) {
        List<CodeBlock> code = new ArrayList<>();
        code.add(CodeBlock.of("$N.param($L)", parser(),
                m.sourceMethod().index()));
        code.add(CodeBlock.of(".map($L)", m.createConverterExpression()));
        code.addAll(tailExpressionParameter(m, i));
        m.extractExpr().ifPresent(code::add);
        return joinByNewline(code);
    }

    private CodeBlock convertExpressionRepeatableParameter(Mapping<AnnotatedVarargsParameter> m) {
        List<CodeBlock> code = new ArrayList<>();
        code.add(CodeBlock.of("$N.rest()", parser()));
        code.add(CodeBlock.of(".map($L)", m.createConverterExpression()));
        code.add(CodeBlock.of(".collect($T.firstFailure())", EITHERS));
        code.add(orElseThrowConverterError(ItemType.PARAMETER, positionalParameters().size()));
        return joinByNewline(code);
    }

    private List<CodeBlock> tailExpressionOption(Mapping<AnnotatedOption> m, int i) {
        if (m.isNullary()) {
            return List.of(CodeBlock.of(".findAny().isPresent()"));
        }
        switch (m.multiplicity()) {
            case REQUIRED:
                return List.of(
                        CodeBlock.of(".findAny()"),
                        CodeBlock.of(".orElseThrow(() -> new $T($T.$L, $L))",
                                ExMissingItem.class, ItemType.class, ItemType.OPTION, i),
                        orElseThrowConverterError(ItemType.OPTION, i));
            case OPTIONAL:
                return List.of(
                        CodeBlock.of(".collect($T.firstFailure())", EITHERS),
                        orElseThrowConverterError(ItemType.OPTION, i),
                        CodeBlock.of(".stream().findAny()"));
            default: {
                if (!m.isRepeatable()) {
                    throw new AssertionError();
                }
                return List.of(
                        CodeBlock.of(".collect($T.firstFailure())", EITHERS),
                        orElseThrowConverterError(ItemType.OPTION, i));
            }
        }
    }

    private List<CodeBlock> tailExpressionParameter(Mapping<AnnotatedParameter> m, int i) {
        if (m.isRequired()) {
            return List.of(CodeBlock.of(".orElseThrow(() -> new $T($T.$L, $L))",
                            ExMissingItem.class, ItemType.class, ItemType.PARAMETER, i),
                    orElseThrowConverterError(ItemType.PARAMETER, i));
        }
        if (!m.isOptional()) {
            throw new AssertionError();
        }
        return List.of(
                CodeBlock.of(".stream()"),
                CodeBlock.of(".collect($T.firstFailure())", EITHERS),
                orElseThrowConverterError(ItemType.PARAMETER, i),
                CodeBlock.of(".stream().findAny()"));
    }

    private CodeBlock orElseThrowConverterError(ItemType itemType, int i) {
        return CodeBlock.of(".orElseThrow($1N -> new $2T($1N, $3T.$4L, $5L))",
                left, ExConvert.class, ItemType.class, itemType, i);
    }
}
