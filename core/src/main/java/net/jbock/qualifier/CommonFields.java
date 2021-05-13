package net.jbock.qualifier;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import net.jbock.compiler.GeneratedTypes;

import java.io.PrintStream;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import static com.squareup.javapoet.TypeName.INT;
import static javax.lang.model.element.Modifier.FINAL;
import static javax.lang.model.element.Modifier.PRIVATE;
import static javax.lang.model.element.Modifier.STATIC;
import static net.jbock.compiler.Constants.STRING;
import static net.jbock.compiler.Constants.STRING_TO_STRING_MAP;
import static net.jbock.compiler.Constants.mapOf;

public class CommonFields {

  private static final int DEFAULT_WRAP_AFTER = 80;

  private final FieldSpec exitHookField;
  private final FieldSpec programName;
  private final FieldSpec optionsByName;

  private final FieldSpec err = FieldSpec.builder(PrintStream.class, "err", PRIVATE)
      .initializer("$T.err", System.class).build();

  private final FieldSpec terminalWidth = FieldSpec.builder(INT, "terminalWidth", PRIVATE)
      .initializer("$L", DEFAULT_WRAP_AFTER).build();

  private final FieldSpec messages = FieldSpec.builder(STRING_TO_STRING_MAP, "messages", PRIVATE)
      .initializer("$T.emptyMap()", Collections.class).build();

  private final FieldSpec suspiciousPattern = FieldSpec.builder(Pattern.class, "SUSPICIOUS")
      .initializer("$T.compile($S)", Pattern.class, "-[a-zA-Z0-9]+|--[a-zA-Z0-9-]+")
      .addModifiers(PRIVATE, STATIC, FINAL)
      .build();

  private CommonFields(FieldSpec exitHookField, FieldSpec programName, FieldSpec optionsByName) {
    this.exitHookField = exitHookField;
    this.programName = programName;
    this.optionsByName = optionsByName;
  }

  public static CommonFields create(
      GeneratedTypes generatedTypes,
      GeneratedType generatedType,
      SourceElement sourceElement) {
    ParameterizedTypeName consumer = ParameterizedTypeName.get(ClassName.get(Consumer.class),
        generatedTypes.parseResultType());
    ParameterSpec result = ParameterSpec.builder(generatedTypes.parseResultType(), "result").build();
    CodeBlock.Builder code = CodeBlock.builder();
    code.add(generatedTypes.helpRequestedType()
        .map(helpRequestedType -> CodeBlock.builder()
            .add("$N ->\n", result).indent()
            .add("$T.exit($N instanceof $T ? 0 : 1)", System.class, result, helpRequestedType)
            .unindent().build())
        .orElseGet(() -> CodeBlock.of("$N -> $T.exit(1)", result, System.class)));
    FieldSpec exitHookField = FieldSpec.builder(consumer, "exitHook")
        .addModifiers(PRIVATE)
        .initializer(code.build())
        .build();
    FieldSpec programName = FieldSpec.builder(STRING, "programName", PRIVATE, FINAL)
        .initializer("$S", sourceElement.programName()).build();
    FieldSpec optionsByName = FieldSpec.builder(mapOf(STRING, generatedType.optionType()), "OPTIONS_BY_NAME")
        .initializer("optionsByName()")
        .addModifiers(PRIVATE, STATIC, FINAL)
        .build();
    return new CommonFields(exitHookField, programName, optionsByName);
  }

  public FieldSpec exitHook() {
    return exitHookField;
  }

  public FieldSpec programName() {
    return programName;
  }

  public FieldSpec err() {
    return err;
  }

  public FieldSpec terminalWidth() {
    return terminalWidth;
  }

  public FieldSpec messages() {
    return messages;
  }

  public FieldSpec suspiciousPattern() {
    return suspiciousPattern;
  }

  public FieldSpec optionsByName() {
    return optionsByName;
  }
}
