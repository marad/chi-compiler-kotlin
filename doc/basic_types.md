# Basic types

Right now Chi has just a few basic types:

* `int` - for integer numbers. It's actually 64 bit value. Chi doesn't have 32 bit value type. It might have in the
  future.
* `float` - for decimal numbers. This one is 32bit. In the future maybe double, we'll see.
* `bool` - has two values `true` and `false`
* `string` - for all the character processing purposes. There is no character type, you can convert string to unicode
  codepoints, which are of type `int`.
* `any` - for whenever you are feeling confident enough to give up static type checking, use with caution
* `unit` - this type has single value, used mostly when you don't want to return anything from the function

### Arrays

Arrays can be declared using `array[T](capacity, defaultElement)` constructor or `arrayOf[T](elements: T*)` constructor:

```kotlin
// create 10 element array filled with zeroes
val a = array[int](10, 0)
// create 3 element array with elements 1, 2, and 3
val b = arrayOf[int](1, 2, 3)
```

To access array element use the `arr[index]` operator:

```kotlin
// get element at index 5
val elem = a[5]

// set element at index 1
b[1] = 10
```

As you can see, array elements are mutable right now, but that may change in the future.

### String interpolation

The `string` type supports interpolation, so you can eval an arbitrary expression:

```chi
val x = 0
"Value of x is $x"
"Value of x+5 is ${x + 5}"
"${ "${ "${ x }" }" }"  // yes, even another interpolated string
```

### User defined data types

You can also define your own types using the `data` keyword. See [here](algebraic_data_types.md) for more information.
