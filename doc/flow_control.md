# Flow control

## Conditionals

Simple `if`/`else`. Works mostly as you would expect:

```chi
val x = 3
if (x > 0) { 
  println("yes")
} else {
  println("no"
}
```

Maybe except that it's also an expression:

```chi
val x = if (true) 5 else 10
```

One additional note is that paired with `is` expression it also fills in the algebraic type variant, so you could access
its fields:

```chi
data Option[T] = Just(value: T) | Nothing

fun foo(opt: Option[string]) {
  // you couldn't use `opt.value` outside the `if` since we don't know which variant it is
  if (opt is Just) {
    println(opt.value) // this is only possible thanks to `is` expression
  }
}
```

## When

This expression is mostly syntactic sugar, so you don't have to look at `if`/`else` ladder:

```chi
data Foo = Less | Zero | More
val foo: Foo = when {
  x < 0 -> Less
  x == 0 -> Zero
  else -> More
}
```

## While loop

While loop is also pretty straightforward:

```chi
var i = 0
while (i < 10) {
  i += 1
}
```

## Weave expression

This is inspired by Clojure's weave macro. This simply takes value of an expression
and weaves it into another expression - possibly multiple times:

```chi
10 ~> _ + _ + _
```

You can even chain the weave expressions to create a whole pipeline of execution:

```chi
10 ~> _ + 5 ~> IntWrapper(_) ~> sendThisOverTheInternet(targetAddress, _)
```

You can think of it as more general form of [method invocation syntax](method_syntax.md) since you can use an arbitrary
expression, use your input value multiple times and in different locations. Method invocation syntax is limited to
using only functions and requires that first function argument type matches with the value on which method is invoked.

### Note on side effects

Each consecutive expression is evaluated, and only it's value is passed further. This ensures that side effects happen
predictably only once:

```chi
fn imaginaryHttpRequest(): int {
	println("Doing imaginary request")
	42
}

imaginaryHttpRequest() ~> _ + _
```

In the above example message `Doing imaginary request` would show only once and the result of the whole weave expression
is going to be `84`.