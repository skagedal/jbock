package net.jbock.validate;

import io.jbock.util.Either;
import net.jbock.annotated.AnnotatedOption;
import net.jbock.common.ValidationFailure;
import net.jbock.convert.Mapping;
import net.jbock.convert.MappingFinder;
import net.jbock.convert.match.MatchFinder;

import javax.inject.Inject;
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

@ValidateScope
public class SourceOptionValidator {

    private final MappingFinder mappingFinder;
    private final MatchFinder matchFinder;

    @Inject
    SourceOptionValidator(
            MappingFinder mappingFinder,
            MatchFinder matchFinder) {
        this.mappingFinder = mappingFinder;
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
            AnnotatedOption option) {
        return checkFlag(option)
                .map(m -> matchFinder.findFlagMatch(m)
                        .map(Mapping::createFlag))
                .orElseGet(() -> mappingFinder.findMapping(option));
    }

    private Optional<AnnotatedOption> checkFlag(
            AnnotatedOption option) {
        if (option.converter().isPresent()) {
            return Optional.empty();
        }
        if (option.returnType().getKind() != BOOLEAN) {
            return Optional.empty();
        }
        return Optional.of(option);
    }

    private Either<ValidationFailure, AnnotatedOption> checkOptionNames(
            AnnotatedOption option) {
        if (option.names().isEmpty()) {
            return left(option.fail("define at least one option name"));
        }
        return option.names().stream()
                .map(name -> checkName(option, name))
                .flatMap(Optional::stream)
                .map(s -> s.prepend("invalid name: "))
                .findFirst()
                .<Either<ValidationFailure, AnnotatedOption>>map(Either::left)
                .orElseGet(() -> right(option));
    }

    /* Left-Optional
     */
    private Optional<ValidationFailure> checkName(AnnotatedOption option, String name) {
        if (Objects.toString(name, "").length() <= 1 || "--".equals(name)) {
            return Optional.of(option.fail(name));
        }
        if (!name.startsWith("-")) {
            return Optional.of(option.fail("must start with a dash character: " + name));
        }
        if (name.startsWith("---")) {
            return Optional.of(option.fail("cannot start with three dashes: " + name));
        }
        if (!name.startsWith("--") && name.length() > 2) {
            return Optional.of(option.fail("single-dash name must be single-character: " + name));
        }
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (isWhitespace(c)) {
                return Optional.of(option.fail("whitespace characters: " + name));
            }
            if (c == '=') {
                return Optional.of(option.fail("invalid character '=': " + name));
            }
        }
        return Optional.empty();
    }

    /* Left-Optional
     */
    private Optional<List<ValidationFailure>> validateUniqueOptionNames(
            List<AnnotatedOption> allOptions) {
        Set<String> allNames = new HashSet<>();
        return allOptions.stream()
                .flatMap(option -> option.names().stream()
                        .filter(name -> !allNames.add(name))
                        .map(name -> "duplicate option name: " + name)
                        .map(option::fail))
                .collect(toOptionalList());
    }
}
