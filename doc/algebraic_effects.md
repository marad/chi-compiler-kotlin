# Algebraic Effects

You can think of algebraic effects as exceptions with the possibility to return a value. Take a look:

```chi
effect hello(name: string): string

fun generateGreeting(): string {
  hello("Chi")
}

val greeting = handle {
  generateGreeting()
} with {
  hello(name) -> resume("Hello $name")
}
```

You can invoke an effect anywhere, and it will look "up" for `handle` block with handler defined for this effect.
Within the handler you can use effect arguments and there is a special `resume` function. The `resume` function
single argument value is what given effect returns (`string` in this example). It's return value type is identical
to the whole `handle` block type since it resumes the execution interrupted while invoking the effect.

But you can also decide not to resume at all and simply return value from the whole `handle` block dropping the
interrupted execution entirely:

```chi
effect httpGetHtml(url: string): string

val html = handle {
  val response = httpGetHtml("https://google.com")
  println(response) // this code will not run
  response
} with {
  httpGetHtml(url) -> "<h1>Surprise!</h1>"
}
```

In the above example the `response` is never going to be shown to the console since invoked effect `httpGetHtml`
never `resume`s and return a string value directly.

## Exception implementation

Chi doesn't have exceptions and returning `Result` explicitly type is the preferred way
to [handle errors](error_handling.md), but you could very easily implement exceptions using algebraic effects, sort of
like this:

```chi
effect throw(exc: any): unit

fn try[T](f: () -> T): Result[T, any] {
  handle {
    Ok(f())
  } with {
    throw(exc) -> {
      println("Exception was thrown: $exc")
      Err(exc)
    }
  }
}

try({
  // some code
  throw("This is an error!")
})
```

Of course real implementation would require quite a lot more work, but I hope you can see how powerful algebraic
expressions are.