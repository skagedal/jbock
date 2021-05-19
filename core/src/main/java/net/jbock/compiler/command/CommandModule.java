package net.jbock.compiler.command;

import dagger.Module;
import dagger.Provides;
import net.jbock.compiler.TypeTool;

import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;

@Module
public interface CommandModule {

  @Provides
  static Types types(TypeTool tool) {
    return tool.types();
  }

  @Provides
  static Elements elements(TypeTool tool) {
    return tool.elements();
  }
}