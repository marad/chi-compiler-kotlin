# Weave expression

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

