# Variables

You can declare mutable and immutable variables with `var` and `val` keywords. You should default to immutable ones.

```chi
val x = 5 // immutable variable
var y = 0 // mutable variable
```

You can optionally specify the expected type for defined variables:

```chi
val b: bool = true
val s: string = "hello"
```

Evaluating such expression will create a symbol within current lexical scope. Each [package](modules_and_packages.md)
has its own lexical scope and each function has new sub scope generated for it.

So naturally you can access symbols outside current scope:

```chi
val outerScopeVar = 23
fn foo(): int {
  outerScopeVar + 5
}
```

Symbol declarations are also expressions, so you can use them anywhere:

```chi
if ((val result = getResult()).isSuccess()) {
    // ... 
} else {
  println(result.error)
}
```

even when... declaring another symbol:

```chi
val y = 5 + (val x = 5)
```

# Functions

To declare a function you may use special syntax:

```chi
fn functionName(i: int, s: string): bool {
  true
}
```

And then you can call this function as you would expect: `functionName(5, "hello")`.

One thing to keep in mind that functions are just values and `functionName` is a symbol that points to that value. What
does that even mean? Look at this:

```chi
val functionName = { i: int, s: string -> true }
```

This has exactly the same effect as the previous syntax (except that in the previous one you can explicitly specify
what the function returns).

This also means that you can easily rename a function:

```chi
val otherName = functionName
otherName(5, "hello")
```

# Closures

In Chi every function is a lambda function - as you can see above. The difference between a function and a closure is
when you use variables from outside the lambda. Their reference must be then captured into lambda's scope and travel
with it.

Here is an example:

```chi
var x = 0
arrayOf(1,2,3).forEach( { it: int -> x += 1 } )
println("There are $x elements in the array")
```

The `x` variable is captured within the lambda, and then the function is passed to the `forEach` invocation. Capturing
allows the lambda to use the reference to change the `x` value.

As you can see this is also supported in Chi.

# Generic functions

While you could make functions with `any` type, you'd be giving away that sweet type safety. This is not what
you want. To make a function that can operate on many types it's better to use generic type parameters:

```chi
fn genericFunction[T](param: T): T {
  param
}

val a: int = genericFunction(1)
val b: string = genericFunction("hello")
val c: bool = genericFunction(false)
```

Compiler will make sure you don't try anything sketchy with the `param` value. Your program will be typesafe.

