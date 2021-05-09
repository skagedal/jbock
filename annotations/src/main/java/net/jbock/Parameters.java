package net.jbock;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * <p>Marker annotation for a repeatable positional parameter.
 * This parameter will capture the remaining tokens,
 * after all non-repeatable positional parameters have been captured.</p>
 *
 * <ul>
 *   <li>The annotated method must be {@code abstract} and have an empty argument list.</li>
 *   <li>It must return {@link java.util.List List&lt;E&gt;}, where {@code E} is a converted type.</li>
 *   <li>There cannot be more than one such method per class.</li>
 * </ul>
 */
@Target(METHOD)
@Retention(SOURCE)
public @interface Parameters {

  /**
   * Declare a custom converter that will be applied to each
   * individual token that's captured by this parameter.
   * This is either a
   * {@link java.util.function.Function Function}
   * accepting strings,
   * or a {@link java.util.function.Supplier Supplier} of such a function.
   *
   * @return converter class or {@code Void.class}
   */
  Class<?> converter() default Void.class;

  /**
   * The key that is used to find the parameter
   * description in the internationalization resource bundle.
   * If no {@code bundleKey} is defined,
   * or no message bundle is supplied at runtime,
   * or a bundle is supplied but does not contain the bundle key,
   * then the {@code description} attribute will be used.
   * If that is also empty, the method's javadoc will be used.
   *
   * @return bundle key or empty string
   */
  String bundleKey() default "";

  /**
   * Parameter description, used when generating the usage documentation.
   * If empty, the method's javadoc will be used as a fallback.
   * The {@code bundleKey} overrides this,
   * if the key is present in the resource bundle at runtime.
   *
   * @return description text
   */
  String[] description() default {};
}
