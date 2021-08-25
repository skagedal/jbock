package net.jbock.context;

import com.squareup.javapoet.ArrayTypeName;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.WildcardTypeName;
import net.jbock.annotated.AnnotatedOption;
import net.jbock.annotated.AnnotatedParameter;
import net.jbock.annotated.AnnotatedParameters;
import net.jbock.common.SafeTypes;
import net.jbock.convert.Mapping;
import net.jbock.model.ItemType;
import net.jbock.processor.SourceElement;
import net.jbock.state.GenericParser;
import net.jbock.util.ExConvert;
import net.jbock.util.ExMissingItem;
import net.jbock.util.ExNotSuccess;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Preconditions.checkArgument;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.jbock.common.Constants.EITHERS;
import static net.jbock.common.Constants.STRING;
import static net.jbock.common.Constants.STRING_ARRAY;

@ContextScope
public class ConstructMethod extends CachedMethod {

    private final GeneratedTypes generatedTypes;
    private final SourceElement sourceElement;
    private final ParameterSpec parser;
    private final List<Mapping<AnnotatedOption>> namedOptions;
    private final List<Mapping<AnnotatedParameter>> positionalParameters;
    private final List<Mapping<AnnotatedParameters>> repeatablePositionalParameters;
    private final ContextUtil contextUtil;
    private final ParameterSpec left = ParameterSpec.builder(STRING, "left").build();

    @Inject
    ConstructMethod(
            GeneratedTypes generatedTypes,
            SourceElement sourceElement,
            List<Mapping<AnnotatedOption>> namedOptions,
            List<Mapping<AnnotatedParameter>> positionalParameters,
            List<Mapping<AnnotatedParameters>> repeatablePositionalParameters,
            SafeTypes safeTypes,
            ContextUtil contextUtil) {
        this.generatedTypes = generatedTypes;
        this.sourceElement = sourceElement;
        this.namedOptions = namedOptions;
        this.positionalParameters = positionalParameters;
        this.repeatablePositionalParameters = repeatablePositionalParameters;
        this.contextUtil = contextUtil;
        this.parser = ParameterSpec.builder(ParameterizedTypeName.get(
                ClassName.get(GenericParser.class),
                namedOptions.isEmpty() ?
                        WildcardTypeName.get(safeTypes.getWildcardType()) :
                        sourceElement.optionEnumType()),
                "parser").build();
    }

    @Override
    MethodSpec define() {
        CodeBlock constructorArguments = getConstructorArguments();
        MethodSpec.Builder spec = MethodSpec.methodBuilder("construct");
        for (int i = 0; i < namedOptions.size(); i++) {
            Mapping<AnnotatedOption> m = namedOptions.get(i);
            ParameterSpec p = m.asParam();
            spec.addStatement("$T $N = $L", p.type, p, convertExpressionOption(m, i));
        }
        for (int i = 0; i < positionalParameters.size(); i++) {
            Mapping<AnnotatedParameter> m = positionalParameters.get(i);
            ParameterSpec p = m.asParam();
            spec.addStatement("$T $N = $L", p.type, p, convertExpressionRegularParameter(m, i));
        }
        repeatablePositionalParameters.forEach(m -> {
            ParameterSpec p = m.asParam();
            spec.addStatement("$T $N = $L", p.type, p, convertExpressionRepeatableParameter(m));
        });
        generatedTypes.superResultType().ifPresentOrElse(parseResultWithRestType -> {
                    ParameterSpec result = ParameterSpec.builder(sourceElement.typeName(), "result").build();
                    ParameterSpec restArgs = ParameterSpec.builder(sourceElement.typeName(), "restArgs").build();
                    spec.addStatement("$T $N = new $T($L)", result.type, result, generatedTypes.implType(),
                            constructorArguments);
                    spec.addStatement("$T $N = $N.rest().toArray($T::new)", STRING_ARRAY, restArgs,
                            parser, ArrayTypeName.of(String.class));
                    spec.addStatement("return new $T($N, $N)", parseResultWithRestType,
                            result, restArgs);
                },
                () -> spec.addStatement("return new $T($L)", generatedTypes.implType(), constructorArguments));
        return spec.returns(generatedTypes.parseSuccessType())
                .addParameter(parser)
                .addException(ExNotSuccess.class)
                .addModifiers(PRIVATE)
                .build();
    }

    private CodeBlock getConstructorArguments() {
        List<CodeBlock> code = new ArrayList<>();
        for (Mapping<AnnotatedOption> c : namedOptions) {
            code.add(CodeBlock.of("$N", c.asParam()));
        }
        for (Mapping<AnnotatedParameter> c : positionalParameters) {
            code.add(CodeBlock.of("$N", c.asParam()));
        }
        repeatablePositionalParameters.stream()
                .map(m -> CodeBlock.of("$N", m.asParam()))
                .forEach(code::add);
        return contextUtil.joinByComma(code);
    }

    private CodeBlock convertExpressionOption(Mapping<AnnotatedOption> m, int i) {
        List<CodeBlock> code = new ArrayList<>();
        code.add(CodeBlock.of("$N.option($T.$N)", parser,
                sourceElement.optionEnumType(), m.enumName().enumConstant()));
        if (!m.modeFlag()) {
            code.add(CodeBlock.of(".map($L)", m.mapExpr()));
        }
        code.addAll(tailExpressionOption(m, i));
        m.extractExpr().ifPresent(code::add);
        return contextUtil.joinByNewline(code);
    }

    private CodeBlock convertExpressionRegularParameter(Mapping<AnnotatedParameter> m, int i) {
        List<CodeBlock> code = new ArrayList<>();
        code.add(CodeBlock.of("$N.param($L)", parser,
                m.sourceMethod().index()));
        code.add(CodeBlock.of(".map($L)", m.mapExpr()));
        code.addAll(tailExpressionParameter(m, i));
        m.extractExpr().ifPresent(code::add);
        return contextUtil.joinByNewline(code);
    }

    private CodeBlock convertExpressionRepeatableParameter(Mapping<AnnotatedParameters> m) {
        List<CodeBlock> code = new ArrayList<>();
        code.add(CodeBlock.of("$N.rest()", parser));
        code.add(CodeBlock.of(".map($L)", m.mapExpr()));
        code.add(CodeBlock.of(".collect($T.toValidList())", EITHERS));
        code.add(orElseThrowConverterError(ItemType.PARAMETER, positionalParameters.size()));
        return contextUtil.joinByNewline(code);
    }

    private List<CodeBlock> tailExpressionOption(Mapping<AnnotatedOption> m, int i) {
        if (m.modeFlag()) {
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
                        CodeBlock.of(".collect($T.toValidList())", EITHERS),
                        orElseThrowConverterError(ItemType.OPTION, i),
                        CodeBlock.of(".stream().findAny()"));
            default: {
                checkArgument(m.isRepeatable());
                return List.of(
                        CodeBlock.of(".collect($T.toValidList())", EITHERS),
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
        checkArgument(m.isOptional());
        return List.of(
                CodeBlock.of(".stream()"),
                CodeBlock.of(".collect($T.toValidList())", EITHERS),
                orElseThrowConverterError(ItemType.PARAMETER, i),
                CodeBlock.of(".stream().findAny()"));
    }

    private CodeBlock orElseThrowConverterError(ItemType itemType, int i) {
        return CodeBlock.of(".orElseThrow($1N -> new $2T($1N, $3T.$4L, $5L))",
                left, ExConvert.class, ItemType.class, itemType, i);
    }
}