package net.jbock.validate;

import io.jbock.util.Either;
import net.jbock.annotated.AnnotatedOption;
import net.jbock.common.ValidationFailure;
import net.jbock.convert.ConverterFinder;
import net.jbock.convert.Mapped;
import net.jbock.source.SourceOption;

import javax.inject.Inject;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.util.Types;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import static io.jbock.util.Either.left;
import static io.jbock.util.Either.right;
import static io.jbock.util.Eithers.toOptionalList;
import static io.jbock.util.Eithers.toValidListAll;
import static java.lang.Character.isWhitespace;
import static javax.lang.model.type.TypeKind.BOOLEAN;
import static net.jbock.convert.Mapped.createFlag;

@ValidateScope
public class SourceOptionValidator {

    private final ConverterFinder converterFinder;
    private final Types types;

    @Inject
    SourceOptionValidator(ConverterFinder converterFinder, Types types) {
        this.converterFinder = converterFinder;
        this.types = types;
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

    private Either<ValidationFailure, Mapped<AnnotatedOption>> wrapOption(SourceOption sourceMethod) {
        return checkFlag(sourceMethod)
                .<Either<ValidationFailure, Mapped<AnnotatedOption>>>map(Either::right)
                .orElseGet(() -> converterFinder.findConverter(sourceMethod).mapLeft(sourceMethod::fail));
    }

    /* Right-Optional
     */
    private Optional<Mapped<AnnotatedOption>> checkFlag(SourceOption sourceMethod) {
        if (sourceMethod.annotatedMethod().converter().isPresent()) {
            return Optional.empty();
        }
        if (sourceMethod.returnType().getKind() != BOOLEAN) {
            return Optional.empty();
        }
        PrimitiveType primitiveBoolean = types.getPrimitiveType(BOOLEAN);
        return Optional.of(createFlag(sourceMethod, primitiveBoolean));
    }

    private Either<ValidationFailure, SourceOption> checkOptionNames(SourceOption sourceMethod) {
        if (sourceMethod.annotatedMethod().names().isEmpty()) {
            return left(sourceMethod.fail("define at least one option name"));
        }
        for (String name : sourceMethod.names()) {
            Optional<String> check = checkName(name);
            if (check.isPresent()) {
                return left(sourceMethod.fail(check.orElseThrow()));
            }
        }
        return right(sourceMethod);
    }

    /* Left-Optional
     */
    private Optional<String> checkName(String name) {
        if (Objects.toString(name, "").length() <= 1 || "--".equals(name)) {
            return Optional.of("invalid name: " + name);
        }
        if (!name.startsWith("-")) {
            return Optional.of("the name must start with a dash character: " + name);
        }
        if (name.startsWith("---")) {
            return Optional.of("the name must start with one or two dashes, not three:" + name);
        }
        if (!name.startsWith("--") && name.length() > 2) {
            return Optional.of("single-dash names must be single-character names: " + name);
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (isWhitespace(c)) {
                return Optional.of("the name contains whitespace characters: " + name);
            }
            if (c == '=') {
                return Optional.of("the name contains '=': " + name);
            }
        }
        return Optional.empty();
    }

    /* Left-Optional
     */
    private Optional<List<ValidationFailure>> validateUniqueOptionNames(List<SourceOption> allOptions) {
        Set<String> allNames = new HashSet<>();
        return allOptions.stream()
                .flatMap(item -> item.names().stream()
                        .filter(name -> !allNames.add(name))
                        .map(name -> "duplicate option name: " + name)
                        .map(item::fail))
                .collect(toOptionalList());
    }
}
