package net.jbock.annotated;

import net.jbock.Parameter;
import net.jbock.common.EnumName;
import net.jbock.processor.SourceElement;

import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Optional;

import static net.jbock.annotated.AnnotatedParameter.createParameter;
import static net.jbock.common.Constants.optionalString;

final class ExecutableParameter extends Executable {

    private final Parameter parameter;

    ExecutableParameter(
            SourceElement sourceElement,
            ExecutableElement method,
            Parameter parameter) {
        super(sourceElement, method);
        this.parameter = parameter;
    }

    @Override
    AnnotatedMethod annotatedMethod(
            SourceElement sourceElement,
            EnumName enumName) {
        return createParameter(this, enumName);
    }

    @Override
    Optional<String> descriptionKey() {
        return optionalString(parameter.descriptionKey());
    }

    @Override
    List<String> description() {
        return List.of(parameter.description());
    }

    Optional<String> paramLabel() {
        return optionalString(parameter.paramLabel());
    }

    int index() {
        return parameter.index();
    }
}
