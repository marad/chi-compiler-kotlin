# Algebraic Data Types

You can declare and use new data type:

```chi
data MyType(i: int, s: string)
val x = MyType(10, "hello")
x.i + 5
```

Using [method syntax](method_syntax.md) you can also implement class like behavior:

```chi
data Person(name: string)

fn greet(person: Person) {
  println("Hello ${person.name}!")
}

Person("John").greet()
```

## Variants

The data type declaration syntax is in fact simplified version of the full syntax. This has exactly the same effect:

```chi
data Person = Person(name: string)
```

... but it also allows for declaring multiple variants of the same type:

```chi
data IntOption = Value(value: int) | Nothing
val a: IntOption = Value(10)
val b: IntOption = Nothing

if (a is Value) {
  println("Value is ${a.value}")
}
```

## Generic types

Declared types can also have generic type parameters:

```chi
data Result[T, E] = Ok(value: T) | Err(error: E)
val iopt: Result[int, string] = Ok(5)
val sopt: Result[string, string] = Ok("hello!")
val bopt: Result[bool, string] = Err("this is an error!")
```