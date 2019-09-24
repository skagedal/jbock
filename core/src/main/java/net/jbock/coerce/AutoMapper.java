package net.jbock.coerce;

import com.squareup.javapoet.CodeBlock;
import net.jbock.compiler.TypeTool;

import javax.lang.model.type.TypeMirror;
import java.io.File;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

class AutoMapper {

  private static final String NEW = "new";
  private static final String CREATE = "create";
  private static final String VALUE_OF = "valueOf";
  private static final String FOR_NAME = "forName";
  private static final String COMPILE = "compile";
  private static final String PARSE = "parse";

  private static Map.Entry<Class<?>, CodeBlock> create(Class<?> clasz, String createFromString) {
    return new AbstractMap.SimpleImmutableEntry<>(clasz, CodeBlock.of("$T::" + createFromString, clasz));
  }

  private static Map.Entry<Class<?>, CodeBlock> create(Class<?> clasz, CodeBlock mapExpr) {
    return new AbstractMap.SimpleImmutableEntry<>(clasz, mapExpr);
  }

  private static final List<Map.Entry<Class<?>, CodeBlock>> MAPPERS = Arrays.asList(
      create(String.class, CodeBlock.of("$T.identity()", Function.class)),
      create(Integer.class, VALUE_OF),
      create(Long.class, VALUE_OF),
      create(File.class, NEW),
      create(Character.class, CodeBlock.of("Helper::parseCharacter")),
      create(Path.class, CodeBlock.of("$T::get", Paths.class)),
      create(URI.class, CREATE),
      create(BigDecimal.class, NEW),
      create(BigInteger.class, NEW),
      create(Charset.class, FOR_NAME),
      create(Pattern.class, COMPILE),
      create(LocalDate.class, PARSE),
      create(Short.class, VALUE_OF),
      create(Byte.class, VALUE_OF),
      create(Double.class, VALUE_OF),
      create(Float.class, VALUE_OF),
      create(OffsetDateTime.class, PARSE),
      create(LocalDateTime.class, PARSE),
      create(ZonedDateTime.class, PARSE),
      create(Instant.class, PARSE));

  static Optional<CodeBlock> findAutoMapper(TypeTool tool, TypeMirror innerType) {
    for (Map.Entry<Class<?>, CodeBlock> coercion : MAPPERS) {
      if (tool.isSameType(innerType, coercion.getKey())) {
        return Optional.of(coercion.getValue());
      }
    }
    return Optional.empty();
  }
}