package net.jbock.util;

import net.jbock.model.CommandModel;

/**
 * This parsing result indicates that the user has passed the
 * {@code --help} option on the command line.
 */
public final class HelpRequested extends NotSuccess {

  public HelpRequested(CommandModel commandModel) {
    super(commandModel);
  }
}