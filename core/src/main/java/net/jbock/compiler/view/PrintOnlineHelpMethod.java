package net.jbock.compiler.view;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import net.jbock.compiler.Description;
import net.jbock.compiler.parameter.NamedOption;
import net.jbock.compiler.parameter.PositionalParameter;
import net.jbock.convert.ConvertedParameter;
import net.jbock.qualifier.AllParameters;
import net.jbock.qualifier.GeneratedType;
import net.jbock.qualifier.NamedOptions;
import net.jbock.qualifier.PositionalParameters;
import net.jbock.qualifier.SourceElement;

import javax.inject.Inject;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;

import static com.squareup.javapoet.MethodSpec.methodBuilder;
import static com.squareup.javapoet.ParameterSpec.builder;
import static javax.lang.model.element.Modifier.PRIVATE;
import static net.jbock.compiler.Constants.LIST_OF_STRING;
import static net.jbock.compiler.Constants.STRING;
import static net.jbock.compiler.view.GeneratedClass.CONTINUATION_INDENT_USAGE;

class PrintOnlineHelpMethod {

  private final Description description;
  private final SourceElement sourceElement;
  private final AllParameters allParameters;
  private final PositionalParameters positionalParameters;
  private final NamedOptions namedOptions;
  private final GeneratedType generatedType;

  private final FieldSpec err = FieldSpec.builder(PrintStream.class, "err", PRIVATE).build();

  @Inject
  PrintOnlineHelpMethod(
      Description description,
      SourceElement sourceElement,
      AllParameters allParameters,
      PositionalParameters positionalParameters,
      NamedOptions namedOptions,
      GeneratedType generatedType) {
    this.description = description;
    this.sourceElement = sourceElement;
    this.allParameters = allParameters;
    this.positionalParameters = positionalParameters;
    this.namedOptions = namedOptions;
    this.generatedType = generatedType;
  }


  MethodSpec printOnlineHelpMethod() {
    CodeBlock.Builder code = CodeBlock.builder();
    String continuationIndent = String.join("", Collections.nCopies(CONTINUATION_INDENT_USAGE, " "));

    if (!description.lines().isEmpty()) {
      ParameterSpec descriptionBuilder = builder(LIST_OF_STRING, "description").build();
      code.addStatement("$T $N = new $T<>()", descriptionBuilder.type, descriptionBuilder, ArrayList.class);
      CodeBlock descriptionBlock = sourceElement.descriptionKey()
          .map(key -> {
            CodeBlock.Builder result = CodeBlock.builder();
            ParameterSpec descriptionMessage = builder(STRING, "descriptionMessage").build();
            result.addStatement("$T $N = messages.get($S)", STRING, descriptionMessage, key);
            result.beginControlFlow("if ($N != null)", descriptionMessage)
                .addStatement("$T.addAll($N, $N.split($S, $L))",
                    Collections.class, descriptionBuilder, descriptionMessage, "\\s+", -1);
            result.endControlFlow();
            result.beginControlFlow("else");
            for (String line : description.lines()) {
              result.addStatement("$T.addAll($N, $S.split($S, $L))",
                  Collections.class, descriptionBuilder, line, "\\s+", -1);
            }
            result.endControlFlow();
            return result.build();
          })
          .orElseGet(() -> {
            CodeBlock.Builder result = CodeBlock.builder();
            for (String line : description.lines()) {
              result.addStatement("$T.addAll($N, $S.split($S, $L))",
                  Collections.class, descriptionBuilder, line, "\\s+", -1);
            }
            return result.build();
          });
      code.add(descriptionBlock);
      code.addStatement("printTokens($S, $N)", "", descriptionBuilder);
      code.addStatement("$N.println()", err);
    }

    code.addStatement("$N.println($S)", err, "USAGE");
    code.addStatement("printTokens($S, usage())", continuationIndent);

    String paramsFormat = "  %1$-" + positionalParameters.maxWidth() + "s ";

    if (!positionalParameters.none()) {
      code.addStatement("$N.println()", err);
      code.addStatement("$N.println($S)", err, "PARAMETERS");
    }
    positionalParameters.forEachRegular(p -> code.add(printPositionalCode(paramsFormat, p)));
    positionalParameters.repeatable().ifPresent(p -> code.add(printPositionalCode(paramsFormat, p)));
    if (!namedOptions.isEmpty()) {
      code.addStatement("$N.println()", err);
      code.addStatement("$N.println($S)", err, "OPTIONS");
    }

    String optionsFormat = "  %1$-" + namedOptions.maxWidth() + "s ";

    namedOptions.forEach(c -> code.add(printNamedOptionCode(optionsFormat, c)));
    return methodBuilder("printOnlineHelp")
        .addModifiers(sourceElement.accessModifiers())
        .addCode(code.build())
        .build();
  }

  private CodeBlock printNamedOptionCode(String optionsFormat, ConvertedParameter<NamedOption> c) {
    if (allParameters.anyDescriptionKeys()) {
      return CodeBlock.builder().addStatement("printOption($T.$L, $S, $S)",
          generatedType.optionType(), c.enumConstant(),
          String.format(optionsFormat, c.parameter().namesWithLabel(c.isFlag())),
          c.parameter().descriptionKey().orElse("")).build();
    } else {
      return CodeBlock.builder().addStatement("printOption($T.$L, $S)",
          generatedType.optionType(), c.enumConstant(),
          String.format(optionsFormat, c.parameter().namesWithLabel(c.isFlag()))).build();
    }
  }

  private CodeBlock printPositionalCode(String paramsFormat, ConvertedParameter<PositionalParameter> p) {
    if (allParameters.anyDescriptionKeys()) {
      return CodeBlock.builder().addStatement("printOption($T.$L, $S, $S)",
          generatedType.optionType(), p.enumConstant(),
          String.format(paramsFormat, p.parameter().paramLabel()),
          p.parameter().descriptionKey().orElse("")).build();
    } else {
      return CodeBlock.builder().addStatement("printOption($T.$L, $S)",
          generatedType.optionType(), p.enumConstant(),
          String.format(paramsFormat, p.parameter().paramLabel())).build();
    }
  }


}
