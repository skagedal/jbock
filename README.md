[skagedal/jbock: a fork of jbock-java/jbock with the intention of playing around with shell completion script generation]

----

[![jbock-compiler](https://maven-badges.herokuapp.com/maven-central/io.github.jbock-java/jbock-compiler/badge.svg?color=grey&subject=jbock-compiler)](https://maven-badges.herokuapp.com/maven-central/io.github.jbock-java/jbock-compiler)
[![jbock](https://maven-badges.herokuapp.com/maven-central/io.github.jbock-java/jbock/badge.svg?subject=jbock)](https://maven-badges.herokuapp.com/maven-central/io.github.jbock-java/jbock)

jbock is a command line parser, which uses the same well-known annotation names as [JCommander](https://jcommander.org/)
and [picocli](https://github.com/remkop/picocli).
It is an
[annotation processor](https://openjdk.java.net/groups/compiler/processing-code.html)
so it doesn't use runtime reflection, but generates a custom parser at compile time instead.

### Quick rundown

Create an abstract class, or alternatively a Java interface,
and add the `@Command` annotation.
In this so-called *command class*, each abstract method

* must return *something* (not `void`),
* must have *no* arguments, and
* must be annotated with either `@Option`, `@Parameter` or `@VarargsParameter`.

The *multiplicity* of options and parameters is determined by the *return type* of their declaring method.
The types `boolean`, `List` and `Optional` (including `OptionalInt`, yada yada) have a special meaning.
See example below.

````java
@Command
abstract class DeleteCommand {

  @Option(names = {"-v", "--verbosity"},
          description = {"A named option. The return type reflects optionality.",
                         "Could use Optional<Integer> too, but using int or Integer",
                         "would make it a 'required option'."})
  abstract OptionalInt verbosity();

  @Parameter(
          index = 0,
          description = {"A required positional parameter. Return type is non-optional.",
                         "Path is a standard type, so no custom converter is needed."})
  abstract Path path();

  @Parameter(
          index = 1,
          description = "An optional positional parameter.")
  abstract Optional<Path> anotherPath();

  @VarargsParameter(
          description = {"A varargs parameter. There can only be one of these.",
                         "The return type must be List-of-something."})
  abstract List<Path> morePaths();
  
  @Option(names = "--dry-run",
          description = "A nullary option, a.k.a. mode flag. Return type is boolean.")
  abstract boolean dryRun();
  
  @Option(names = "-h",
          description = "A repeatable option. Return type is List.")
  abstract List<String> headers(); 
  
  @Option(names = "--charset",
          description = "Named option with a custom converter",
          converter = CharsetConverter.class)
  abstract Optional<Charset> charset();
  
  // sample converter class
  static class CharsetConverter extends StringConverter<Charset> {
    @Override
    protected Charset convert(String token) { return Charset.forName(token); }
  }
}
````

The generated class is called `DeleteCommandParser`. It converts a string array to an instance of `DeleteCommand`:

````java
public static void main(String[] args) {
  DeleteCommand command = new DeleteCommandParser().parseOrExit(args);
  // ...
}

````

In addition to `parseOrExit`, the generated parser has a basic and side-effect free `parse` method.
This can be used to fine-tune the help and error messages for your users.

### Standard types

Some types don't need a custom converter. See [StandardConverters.java](https://github.com/jbock-java/jbock/blob/master/jbock/src/main/java/net/jbock/contrib/StandardConverters.java).

### Subcommands

The `@SuperCommand` annotation can be used to define a git-like subcommand structure. See [javadoc](https://github.com/jbock-java/jbock/blob/master/jbock/src/main/java/net/jbock/SuperCommand.java).

### Sample projects

* [jbock-maven-example](https://github.com/jbock-java/jbock-maven-example)
* [jbock-gradle-example](https://github.com/jbock-java/jbock-gradle-example)

### Alternatives

* [Tim's list](https://github.com/timtiemens/javacommandlineparser)
