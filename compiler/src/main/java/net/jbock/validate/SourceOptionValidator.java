package net.jbock.validate;

import com.squareup.javapoet.CodeBlock;
import io.jbock.util.Either;
import net.jbock.annotated.AnnotatedOption;
import net.jbock.common.ValidationFailure;
import net.jbock.convert.Mapping;
import net.jbock.convert.MappingFinder;
import net.jbock.convert.matching.MatchFinder;
import net.jbock.source.SourceOption;
import net.jbock.util.StringConverter;

import javax.inject.Inject;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static io.jbock.util.Either.left;
import static io.jbock.util.Either.right;
import static io.jbock.util.Eithers.toOptionalList;
import static io.jbock.util.Eithers.toValidListAll;
import static java.lang.Character.isWhitespace;
import static javax.lang.model.type.TypeKind.BOOLEAN;

@ValidateScope
public class SourceOptionValidator {

    private final MappingFinder converterFinder;
    private final MatchFinder matchFinder;

    @Inject
    SourceOptionValidator(
            MappingFinder converterFinder,
            MatchFinder matchFinder) {
        this.converterFinder = converterFinder;
        this.matchFinder = matchFinder;
    }

    Either<List<ValidationFailure>, ContextBuilder> wrapOptions(
            ContextBuilder.Step3 step) {
        return step.namedOptions().stream()
                .map(this::checkOptionNames)
                .collect(toValidListAll())
                .filter(this::validateUniqueOptionNames)
                .flatMap(sourceOptions -> sourceOptions.stream()
                        .map(this::wrapOption)
                        .collect(toValidListAll()))
                .map(step::accept);
    }

    private Either<ValidationFailure, Mapping<AnnotatedOption>> wrapOption(
            SourceOption sourceMethod) {
        return checkFlag(sourceMethod)
                .orElseGet(() -> converterFinder.findMapping(sourceMethod));
    }

    private Optional<Either<ValidationFailure, Mapping<AnnotatedOption>>> checkFlag(
            SourceOption sourceOption) {
        if (sourceOption.annotatedMethod().converter().isPresent()) {
            return Optional.empty();
        }
        if (sourceOption.returnType().getKind() != BOOLEAN) {
            return Optional.empty();
        }
        CodeBlock code = CodeBlock.of("$T.create($T.identity())",
                StringConverter.class, Function.class);
        return Optional.of(matchFinder.findFlagMatch(sourceOption).map(m -> Mapping.createFlag(code, m)));
    }

    private Either<ValidationFailure, SourceOption> checkOptionNames(
            SourceOption sourceOption) {
        if (sourceOption.annotatedMethod().names().isEmpty()) {
            return left(sourceOption.fail("define at least one option name"));
        }
        for (String name : sourceOption.names()) {
            Optional<String> check = checkName(name);
            if (check.isPresent()) {
                return left(sourceOption.fail(check.map(s -> "invalid name: " + s)
                        .orElseThrow()));
            }
        }
        return right(sourceOption);
    }

    /* Left-Optional
     */
    private Optional<String> checkName(String name) {
        if (Objects.toString(name, "").length() <= 1 || "--".equals(name)) {
            return Optional.of(name);
        }
        if (!name.startsWith("-")) {
            return Optional.of("must start with a dash character: " + name);
        }
        if (name.startsWith("---")) {
            return Optional.of("cannot start with three dashes: " + name);
        }
        if (!name.startsWith("--") && name.length() > 2) {
            return Optional.of("single-dash name must be single-character: " + name);
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (isWhitespace(c)) {
                return Optional.of("whitespace characters: " + name);
            }
            if (c == '=') {
                return Optional.of("invalid character '=': " + name);
            }
        }
        return Optional.empty();
    }

    /* Left-Optional
     */
    private Optional<List<ValidationFailure>> validateUniqueOptionNames(
            List<SourceOption> allOptions) {
        Set<String> allNames = new HashSet<>();
        return allOptions.stream()
                .flatMap(item -> item.names().stream()
                        .filter(name -> !allNames.add(name))
                        .map(name -> "duplicate option name: " + name)
                        .map(item::fail))
                .collect(toOptionalList());
    }
}