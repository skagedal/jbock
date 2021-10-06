package net.jbock.annotated;

import net.jbock.Parameters;
import net.jbock.processor.SourceElement;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Optional;

import static net.jbock.annotated.AnnotatedParameters.createParameters;
import static net.jbock.common.Constants.optionalString;

final class ExecutableParameters extends Executable {

    private final Parameters parameters;

    ExecutableParameters(
            ExecutableElement method,
            Parameters parameters,
            AnnotationMirror annotationMirror) {
        super(method, annotationMirror);
        this.parameters = parameters;
    }

    @Override
    AnnotatedMethod annotatedMethod(
            SourceElement sourceElement,
            String enumName) {
        return createParameters(this, enumName);
    }

    @Override
    Optional<String> descriptionKey() {
        return optionalString(parameters.descriptionKey());
    }

    @Override
    List<String> description() {
        return List.of(parameters.description());
    }

    Optional<String> paramLabel() {
        return optionalString(parameters.paramLabel());
    }
}
