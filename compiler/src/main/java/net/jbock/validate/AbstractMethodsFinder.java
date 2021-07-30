package net.jbock.validate;

import io.jbock.util.Either;
import net.jbock.common.ValidationFailure;
import net.jbock.processor.SourceElement;

import javax.inject.Inject;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static io.jbock.util.Either.right;
import static io.jbock.util.Eithers.toOptionalList;
import static java.util.stream.Collectors.partitioningBy;
import static java.util.stream.Collectors.toList;
import static javax.lang.model.element.ElementKind.INTERFACE;
import static javax.lang.model.element.Modifier.ABSTRACT;
import static net.jbock.common.Annotations.methodLevelAnnotations;
import static net.jbock.common.TypeTool.AS_DECLARED;
import static net.jbock.common.TypeTool.AS_TYPE_ELEMENT;

@ValidateScope
public class AbstractMethodsFinder {

    private final SourceElement sourceElement;

    @Inject
    AbstractMethodsFinder(SourceElement sourceElement) {
        this.sourceElement = sourceElement;
    }

    Either<List<ValidationFailure>, List<ExecutableElement>> findAbstractMethods() {
        Map<Boolean, List<ExecutableElement>> partitions = findMethodsIn(sourceElement.element().asType()).stream()
                .collect(partitioningBy(m -> m.getModifiers().contains(ABSTRACT)));
        Set<Name> nonAbstractNames = partitions.get(false)
                .stream()
                .map(ExecutableElement::getSimpleName)
                .collect(Collectors.toSet());
        return findRelevantAbstractMethods(partitions.get(true), nonAbstractNames);
    }

    private List<ExecutableElement> findMethodsIn(TypeMirror mirror) {
        Map<Name, List<ExecutableElement>> methodsInInterfaces = findMethodsInInterfaces(mirror);
        List<ExecutableElement> acc = new ArrayList<>();
        methodsInInterfaces.values().forEach(acc::addAll);
        while (true) {
            Optional<TypeElement> element = asAbstractTypeElement(mirror);
            if (element.isEmpty()) {
                return acc;
            }
            TypeElement el = element.orElseThrow();
            List<ExecutableElement> methods = parameterlessMethodsIn(el.getEnclosedElements());
            acc.addAll(methods);
            mirror = el.getSuperclass();
        }
    }

    private Optional<TypeElement> asAbstractTypeElement(TypeMirror mirror) {
        return AS_DECLARED.visit(mirror)
                .map(DeclaredType::asElement)
                .flatMap(AS_TYPE_ELEMENT::visit)
                // interfaces are handled separately
                .filter(typeElement -> typeElement.getKind() != INTERFACE)
                // not abstract -> no relevant methods
                .filter(typeElement -> typeElement.getModifiers().contains(ABSTRACT));
    }

    private Map<Name, List<ExecutableElement>> findMethodsInInterfaces(TypeMirror mirror) {
        return AS_DECLARED.visit(mirror)
                .map(DeclaredType::asElement)
                .flatMap(AS_TYPE_ELEMENT::visit)
                .map(typeElement -> {
                    List<ExecutableElement> methods = typeElement.getKind() == INTERFACE ?
                            parameterlessMethodsIn(typeElement.getEnclosedElements()) :
                            List.of();
                    Map<Name, List<ExecutableElement>> acc = new HashMap<>();
                    acc.put(typeElement.getQualifiedName(), methods);
                    for (TypeMirror superInterface : typeElement.getInterfaces()) {
                        acc.putAll(findMethodsInInterfaces(superInterface)); // recursion
                    }
                    return acc;
                }).orElse(Map.of());
    }

    private List<ExecutableElement> parameterlessMethodsIn(List<? extends Element> elements) {
        return ElementFilter.methodsIn(elements).stream()
                .filter(m -> m.getParameters().isEmpty())
                .collect(toList());
    }

    /**
     * Returns a Right-Either containing those abstract methods which
     * are not overridden by a non-abstract method.
     * If one of the annotated methods is overridden, a Left either is returned.
     *
     * @param allAbstract the set of all abstract methods in the source elements hierarchy
     * @return the n
     */
    private Either<List<ValidationFailure>, List<ExecutableElement>> findRelevantAbstractMethods(
            List<ExecutableElement> allAbstract,
            Set<Name> nonAbstractNames) {
        Map<Boolean, List<ExecutableElement>> partition = allAbstract.stream()
                .collect(partitioningBy(m -> nonAbstractNames.contains(m.getSimpleName())));
        return partition.get(true).stream()
                .filter(this::hasAnnotation)
                .map(m -> new ValidationFailure("annotated method is overridden", m))
                .collect(toOptionalList())
                .<Either<List<ValidationFailure>, List<ExecutableElement>>>map(Either::left)
                .orElseGet(() -> right(partition.get(false)));
    }

    private boolean hasAnnotation(ExecutableElement overridden) {
        return methodLevelAnnotations().stream()
                .anyMatch(a -> overridden.getAnnotation(a) != null);
    }
}
