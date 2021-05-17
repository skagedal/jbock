package net.jbock.compiler.view;

import com.squareup.javapoet.CodeBlock;
import dagger.Reusable;

import javax.inject.Inject;

@Reusable
public class ClassJavadoc {

  private static final String PROJECT_URL = "https://github.com/h908714124/jbock";

  @Inject
  ClassJavadoc() {
  }

  CodeBlock create() {
    String version = getClass().getPackage().getImplementationVersion();
    return CodeBlock.builder()
        .add("<h3>Generated by <a href=$S>jbock $L</a></h3>\n", PROJECT_URL, version)
        .build();
  }
}
