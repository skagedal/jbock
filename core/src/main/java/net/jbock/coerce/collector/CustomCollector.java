package net.jbock.coerce.collector;

import com.squareup.javapoet.CodeBlock;
import net.jbock.compiler.TypeTool;

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import java.util.List;

import static net.jbock.compiler.Util.getTypeParameterList;

/**
 * Custom collector class specified.
 * This class implements either {@link java.util.stream.Collector Collector} directly, is
 * a {@link java.util.function.Supplier Supplier} that returns {@code Collector}.
 */
public class CustomCollector extends AbstractCollector {

  private final TypeElement collectorClass; // the specified collector class

  private final boolean supplier; // true if wrapped in Supplier

  private final List<TypeMirror> solution; // solution to the typevars of collector class, if any

  public CustomCollector(TypeMirror inputType, TypeElement collectorClass, boolean supplier, List<TypeMirror> solution) {
    super(inputType);
    this.collectorClass = collectorClass;
    this.supplier = supplier;
    this.solution = solution;
  }

  public TypeMirror collectorType() {
    return collectorClass.asType();
  }

  public boolean supplier() {
    return supplier;
  }

  public List<TypeMirror> solution() {
    return solution;
  }

  @Override
  public CodeBlock createCollector() {
    return CodeBlock.of("new $T$L()$L",
        TypeTool.get().erasure(collectorType()),
        getTypeParameterList(solution()),
        supplier() ? ".get()" : "");
  }
}