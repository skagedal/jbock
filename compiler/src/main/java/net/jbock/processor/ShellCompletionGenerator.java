package net.jbock.processor;

import io.jbock.simple.Inject;

import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileManager;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class ShellCompletionGenerator {
    private final Filer filer;

    @Inject
    ShellCompletionGenerator(ProcessingEnvironment processingEnvironment) {
        this.filer = processingEnvironment.getFiler();
    }

    public void write() {
        try {
            var resource = filer.createResource(StandardLocation.CLASS_OUTPUT, "foo.bar", UUID.randomUUID() + ".txt");
            try (var os = resource.openOutputStream()) {
                os.write("Hello\n".getBytes(StandardCharsets.UTF_8));
            } catch (RuntimeException e) {
                System.out.println("Exception while writing: " + e);
            }
            System.out.println("Wrote to " + resource.getName());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
