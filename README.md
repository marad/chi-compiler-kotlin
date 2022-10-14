[![Build and Test the project](https://github.com/marad/chi-compiler-kotlin/actions/workflows/test.yaml/badge.svg)](https://github.com/marad/chi-compiler-kotlin/actions/workflows/test.yaml)

## Oh god, another language?!

Yes. How do you like your programming workflow with writing some code,
starting tests and... waiting for them to finish just to see that you
made a small mistake?

Wouldn't it be nice to have an **instant feedback** on what you are working
on?

As a matter of fact - you can already do this with languages like LISP and
Smalltalk. The thing is... their syntax is a *bit* outdated and this makes
people uncomfortable.

Chi language takes the ancient wisdom and simply dress it up in some more
modern clothes, so we all can benefit.

The key language feature is that the language is **introspective**. This means
that you can browse and modify defined modules, packages, variables and functions
**within the running application**.

Here's the list that makes it stand out:

- It's introspective (as mentioned above)
- It has simple and powerful core. Most of the latest and greatest features are just syntactic sugar.
- Supports algebraic effects (ish - project loom will unleash the full potential)
- Has powerful REPL
- Has **no OOP**, yet syntax is still suspiciously similar to OOP languages, how could it be?!
  (remember the powerful core part?)
- Algebraic data types (sum types)
- The weave expression
- Draws a LOT of inspiration from LISP and Smalltalk (fortunately not the syntax)
- Features Smalltalk inspired app images

You can read more on that features [here](doc/how_is_chi_different.md).

## How to get started?

You'd probably need to learn the syntax first. I'm working on this!

For now - here is a simple naive Fibonacci function:

```
fn fib(n: int): int {
  if (n == 0) { 0 }
  else if (n == 1) { 1 }
  else { fib(n - 1) + fib(n - 2) }
}
```

You can also explore the [examples][examples] folder. It contains some [Advent Of Code][aoc]
2015 solutions [written in Chi][chi-aoc-solutions].

## How can I use it?

Right now you need to build it yourself but in the future there will be few ways to
use it:

- Download `chi` executable and simply run scripts or app images with it
- As a scripting language in your JVM projects using GraalVM polyglot API
- Potentially in other languages through foreign function interface
  (we'll see about that)

## Building Chi yourself

One single requirement is to have GraalVM SDK 22 or newer installed. Then just make
sure it's in your `JAVA_HOME` when you invoke:

```shell
./gradlew build
```

This wil generate the `chi-launcher/build/libs/chi-launcher-1.0-all.jar` after
that you should be able to jump into REPL with:

```shell
java -jar chi-launcher/build/libs/chi-launcher-1.0-all.jar repl
```

### Building native executable

For this you'll need to have GraalVM's `native-image` installed (with
`gu install native-image`).

On Ubuntu you'd also need `build-essential` package
to have the tools required for compilation.

For Windows you'll need VS Native Tools and use their command line (it has all
the tools set up correcly on the `PATH`). [Here is how to use it][native-tools].

When you have all the things, what's left to do is to

```shell
./build_native.bat
```

Yeah - it's for Windows now, but it should be easy to convert to .sh script
(I'm open to your contributions!)

After that you can simply run some script `./chi.exe somescript.chi` or
drop straight to the REPL `./chi.exe repl`.

[examples]: https://github.com/marad/chi-compiler-kotlin/tree/main/examples

[chi-aoc-solutions]: https://github.com/marad/chi-compiler-kotlin/tree/main/examples/aoc/2015

[aoc]: https://adventofcode.com/

[native-tools]: https://learn.microsoft.com/en-us/cpp/build/building-on-the-command-line?view=msvc-170