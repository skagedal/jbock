Jbock differs from most other command line parsers
in its handling of optional parameters.
Let's take a look at
[jcommander](http://jcommander.org/)
for comparison.

In jcommander, the parameter annotations go on fields.
Like this:

````java
class Args {

  @Parameter(names = "-v")
  int verbosity;
}
````

and then parsing happens:

````java
Args args = new Args();
String[] argv = {};
JCommander.newBuilder().addObject(args).build().parse(argv);
assertEquals(0, args.verbosity);
````

Because of the implicit default value of `0`,
we can't say whether `argv` was
`{"-v", "0"}` or `{}`. This means we cannot actually
be sure whether the value `0` was supplied by the user
or not.

The situation is different if we use an
`Integer` instead:

````java
class Args {

  @Parameter(names = "-v")
  Integer verbosity;
}
````

````java
Args args = new Args();
String[] argv = {};
JCommander.newBuilder().addObject(args).build().parse(argv);
assertNull(args.verbosity);
````

Now we can tell the cases apart, but there's a cost:
a source of `null` values
has been added to our program.
The `argv` array can never contain `null`; it doesn't feel
right to convert it into something that can.

#### A farewell to null

`verbosity` is an optional parameter, and
 jbock will not allow using the types `Integer`
or `int` for this. The correct type in this case would be
`Optional<Integer>` or `OptionalInt`.

If however either `int` or `Integer` are used as the parameter type,
then jbock will treat this parameter as required.
In any case, jbock will not return `null`
as a parameter value.