package net.jbock.processor;

import javax.lang.model.SourceVersion;
import java.util.List;

/**
 * This is jbock's annotation processor.
 * It defines "steps", which are like subprocessors,
 * each of which handles a subset of the jbock annotations.
 * Most validation, as well as the source code generation,
 * is handled in the {@link CommandStep}.
 * The other steps perform some additional validation.
 */
public final class JbockProcessor extends BasicAnnotationProcessor {

    @Override
    protected List<? extends Step> steps() {
        ProcessorComponent component = DaggerProcessorComponent.factory()
                .create(new ProcessorModule(processingEnv));
        return List.of(component.commandStep(),
                component.methodStep());
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }
}
