package net.jbock.model;

import java.util.List;

/**
 * Abstract superclass of {@link Option} and {@link Parameter}.
 */
public abstract class Item {

  private final String paramLabel;
  private final String descriptionKey;
  private final List<String> description;
  private final Skew skew;

  Item(String paramLabel, String descriptionKey, List<String> description, Skew skew) {
    this.paramLabel = paramLabel;
    this.descriptionKey = descriptionKey;
    this.description = description;
    this.skew = skew;
  }

  /**
   * A string, not empty.
   *
   * @return item name
   */
  public abstract String name();

  /**
   * A string, not empty.
   *
   * @return param label
   */
  public final String paramLabel() {
    return paramLabel;
  }

  /**
   * Description, possibly empty.
   *
   * @return description lines
   */
  public final List<String> description() {
    return description;
  }

  /**
   * A string, possibly empty.
   *
   * @return description key
   */
  public final String descriptionKey() {
    return descriptionKey;
  }

  /**
   * The skew of this item.
   *
   * @return item skew
   */
  public final Skew skew() {
    return skew;
  }
}