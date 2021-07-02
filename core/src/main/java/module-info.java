/**
 * <p>jbock runtime package: annotations and API.
 * The generated parser is annotated with
 * {@code javax.annotation.processing.Generated},
 * so {@code java.compiler} is also required.</p>
 */
module net.jbock {

  requires transitive java.compiler;

  exports net.jbock;
  exports net.jbock.either;
  exports net.jbock.util;
  exports net.jbock.model;
  exports net.jbock.contrib;
}
