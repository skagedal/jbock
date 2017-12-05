package net.jbock.compiler;

import static net.jbock.com.squareup.javapoet.TypeName.BOOLEAN;
import static net.jbock.compiler.Constants.LIST_OF_STRING;
import static net.jbock.compiler.Constants.OPTIONAL_STRING;
import static net.jbock.compiler.Constants.STRING;

import java.util.Collections;
import java.util.Optional;
import java.util.OptionalInt;
import net.jbock.com.squareup.javapoet.ClassName;
import net.jbock.com.squareup.javapoet.CodeBlock;
import net.jbock.com.squareup.javapoet.TypeName;

/**
 * An enumeration of the enum constants that {@link OptionType} defines.
 */
enum Type {

  FLAG(BOOLEAN, false, false, false) {
    @Override
    CodeBlock extractExpression(Option option, int j) {
      return CodeBlock.builder().add(
          "$N.contains($T.$N)",
          option.flagsParameter, option.type, option.enumConstant(j))
          .build();
    }
  },

  OPTIONAL(OPTIONAL_STRING, false, true, false) {
    @Override
    CodeBlock extractExpression(Option option, int j) {
      return CodeBlock.builder().add(
          "$T.ofNullable($N.get($T.$L))",
          Optional.class, option.sMapParameter, option.type, option.enumConstant(j))
          .build();
    }
  },

  OPTIONAL_INT(ClassName.get(OptionalInt.class), false, true, false) {
    @Override
    CodeBlock extractExpression(Option option, int j) {
      return CodeBlock.builder().add(
          "$N($N, $T.$L)",
          option.extractOptionalIntMethod, option.sMapParameter,
          option.type, option.enumConstant(j))
          .build();
    }
  },

  REQUIRED(STRING, false, true, true) {
    @Override
    CodeBlock extractExpression(Option option, int j) {
      return CodeBlock.builder().add(
          "$N.get($T.$L)",
          option.sMapParameter, option.type, option.enumConstant(j))
          .build();
    }
  },

  REQUIRED_INT(TypeName.INT, false, true, true) {
    @Override
    CodeBlock extractExpression(Option option, int j) {
      return CodeBlock.builder().add(
          "$T.parseInt($N.get($T.$L))",
          Integer.class, option.sMapParameter, option.type, option.enumConstant(j))
          .build();
    }
  },

  REPEATABLE(LIST_OF_STRING, false, true, false) {
    @Override
    CodeBlock extractExpression(Option option, int j) {
      return CodeBlock.builder().add(
          "$N.getOrDefault($T.$L, $T.emptyList())",
          option.optMapParameter, option.type, option.enumConstant(j), Collections.class)
          .build();
    }
  },

  OTHER_TOKENS(LIST_OF_STRING, true, false, false) {
    @Override
    CodeBlock extractExpression(Option option, int j) {
      return CodeBlock.builder().add(
          "$N", option.otherTokensParameter).build();
    }
  },

  EVERYTHING_AFTER(LIST_OF_STRING, true, false, false) {
    @Override
    CodeBlock extractExpression(Option option, int j) {
      return CodeBlock.builder().add(
          "$N", option.restParameter).build();
    }
  };

  final TypeName returnType;
  final boolean special;
  final boolean binding;
  final boolean required;

  abstract CodeBlock extractExpression(Option option, int j);

  Type(
      TypeName returnType,
      boolean special,
      boolean binding, boolean required) {
    this.returnType = returnType;
    this.special = special;
    this.binding = binding;
    this.required = required;
  }
}
