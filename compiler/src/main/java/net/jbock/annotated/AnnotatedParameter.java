package net.jbock.annotated;

import net.jbock.common.SnakeName;

import java.util.Locale;
import java.util.function.Supplier;

import static net.jbock.common.Suppliers.memoize;

public final class AnnotatedParameter extends AnnotatedMethod<ExecutableParameter> {

    private final Supplier<String> paramLabel = memoize(() -> executable().paramLabel()
            .orElseGet(() -> SnakeName.create(executable().simpleName())
                    .snake('_')
                    .toUpperCase(Locale.ROOT)));

    private AnnotatedParameter(
            String enumName,
            ExecutableParameter parameter) {
        super(parameter, enumName);
    }

    static AnnotatedParameter createParameter(
            ExecutableParameter parameter,
            String enumName) {
        return new AnnotatedParameter(enumName, parameter);
    }

    @Override
    public boolean isParameter() {
        return true;
    }

    @Override
    public boolean isVarargsParameter() {
        return false;
    }

    @Override
    public String paramLabel() {
        return paramLabel.get();
    }

    public int index() {
        return executable().index();
    }
}
