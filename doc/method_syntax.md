# The Method Syntax

Invoking methods on objects is pretty convenient. But should this require defining
a class and it's methods? Can't we deal with simple functions?

Well, we can. In most languages method invocation even compiles to function
invocation with first argument filled in by the compiler to `this`.

Chi takes things the other way. It lets you invoke any function using method syntax
as long as the first parameter is the same type as the value you are using:

```chi
fn inc(i: int): int { i + 1 }
10.inc().inc().inc()
```

Compiler will actually translate it to `inc(inc(inc(10)))`. It's far easier
to write, read and understand the method syntax.

## How are the functions resolved?

While looking for a function to call it must obviously match with called function name and the first argument must be
the same type that the expression you are invoking it on has. The *methods* are **searched in the package** that the
type is defined in **first**. Then functions defined within current package and then imported ones.

Here is an example. Let's say that we have our data type:

```chi
package test.mod/some.pkg
data Test(i: int)

fn inc(t: Test): Test {
  Test(t.i + 1)
}
```

and then we import this into another package:

```chi
package test.mod/another.pkg
import test.mod/some.pkg { Test }

val t = Test(1)
t.inc()
```

The called function is actually `test.mod/some.pkg.inc()`. This has the advantage that you don't have to import
all the functions that you want to invoke on your type. You can also somewhat simulate what you are already familiar
with from OOP world.
