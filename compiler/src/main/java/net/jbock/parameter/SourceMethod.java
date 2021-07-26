package net.jbock.parameter;

import net.jbock.common.AnnotatedMethod;
import net.jbock.common.EnumName;
import net.jbock.common.ValidationFailure;
import net.jbock.validate.ParameterStyle;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.type.TypeMirror;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import static net.jbock.common.Constants.ACCESS_MODIFIERS;

public class SourceMethod {

    private final AnnotatedMethod annotatedMethod;
    private final EnumName enumName;
    private final ParameterStyle parameterStyle;
    private final List<Modifier> accessModifiers;

    private SourceMethod(
            AnnotatedMethod annotatedMethod,
            EnumName enumName,
            ParameterStyle parameterStyle,
            List<Modifier> accessModifiers) {
        this.annotatedMethod = annotatedMethod;
        this.enumName = enumName;
        this.parameterStyle = parameterStyle;
        this.accessModifiers = accessModifiers;
    }

    public static SourceMethod create(AnnotatedMethod annotatedMethod, EnumName enumName) {
        List<Modifier> accessModifiers = annotatedMethod.sourceMethod().getModifiers().stream()
                .filter(ACCESS_MODIFIERS::contains)
                .collect(Collectors.toUnmodifiableList());
        ParameterStyle parameterStyle = ParameterStyle.getStyle(annotatedMethod.sourceMethod());
        return new SourceMethod(annotatedMethod, enumName, parameterStyle, accessModifiers);
    }

    public ExecutableElement method() {
        return annotatedMethod.sourceMethod();
    }

    public TypeMirror returnType() {
        return annotatedMethod.sourceMethod().getReturnType();
    }

    public ParameterStyle style() {
        return parameterStyle;
    }

    public OptionalInt index() {
        return parameterStyle.index(annotatedMethod);
    }

    public Optional<String> descriptionKey() {
        return parameterStyle.descriptionKey(annotatedMethod);
    }

    public ValidationFailure fail(String message) {
        return new ValidationFailure(message, annotatedMethod.sourceMethod());
    }

    public List<String> names() {
        return parameterStyle.names(annotatedMethod);
    }

    public List<String> description() {
        return parameterStyle.description(annotatedMethod);
    }

    public Optional<String> paramLabel() {
        return parameterStyle.paramLabel(annotatedMethod);
    }

    public List<Modifier> accessModifiers() {
        return accessModifiers;
    }

    public EnumName enumName() {
        return enumName;
    }
}
