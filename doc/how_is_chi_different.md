# How is Chi different?

# It's introspective

🏗️ stay tuned, work in progress...

# It has simple and powerful core

Mental model for Chi runtime is pretty simple. **Modules** contain **packages** and packages contain **
symbols** ([more here](modules_and_packages.md)). Symbol is just a name for a value. Value is `5` or `"string"`
or `true`. Functions are also values: `{ i: int -> i + 1 }`.

This is it. Seriously. Of course there are some control flow operations like `if-else`, `while`, but the core concept is
simple.

So when you write

```chi 
val foo = { i: int -> i + 1 }
```

You have effectively defined a function `foo` with type `(int) -> int` in current package (default module/package
is `user/default`). So how is this different from defining function using other syntax:

```chi
fn foo(i: int): int { i + 1 }
```

It's not. The effect is identical. You have symbol `foo` defined in current package and that symbol `foo` is of
type `(int) -> int` and it's value is a function that increases value by 1.

This simplicity gives a lot of flexibility and allows adding new language features mostly on syntactic level without
touching type checking or inference that operate on simpler syntax tree.

# Algebraic effects

So this is pretty cool. Basically you can define an effect, then invoke it like you would with a function, but its
implementation may be dynamically dependent on the context:

```chi
effect hello(name: string): string

handle {
  hello("Chi")
} with {
  hello(name) -> resume("Hello $name!")
}
```

You can read more [here](algebraic_effects.md).

# No OOP, yet syntax is similar to OOP

Invoking methods on objects is pretty convenient. But should this require defining a class and it's methods? Can't we
deal with simple functions?

Well, we can. In most languages method invocation even compiles to function invocation with first argument filled in by
the compiler to `this`.

Chi takes things the other way. It lets you invoke any function using method syntax as long as the first parameter is
the same type as the value you are using:

```chi
fn inc(i: int): int { i + 1 }
10.inc().inc().inc()
```

Compiler will actually translate it to `inc(inc(inc(10)))`. It's far easier to write, read and understand the method
syntax.

You can read more [here](method_syntax.md).

# The weave expression

This is inspired by Clojure's weave macro. This simply takes value of an expression and weaves it into another
expression - possibly multiple times:

```chi
10 ~> _ + _ + _
```

You can read more [here](flow_control.md#weave-expression).

# Algebraic data types

Most languages lack proper support for this. They try to emulate it with sealed classes or enums. In Chi it's pretty
straightforward:

```chi
data Maybe[T] = Some(value: T) | None
```

Then you can use `Maybe` as a type and `Some`, `None` as functions to create instances of that type.

You can read more [here](algebraic_data_types.md).

# Features Smalltalk inspired app images

🏗️ stay tuned, work in progress...
