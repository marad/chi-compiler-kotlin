# Modules and packages

## Structure

Chi program is built from modules. Each module contains packages and each package contains functions and variables.

Functions and packages are internally available in module (are module-public). For functions to be publicly visible
outside the module they must be preceeded with `pub` keyword:

```chi
pub fn myFunc() { ... }
```

Module contains a list of packages without any structure. Virtual structure may be reflected by using commas in package
names:

```chi
package std/my.pkg.foo
package std/my.pkg.bar
```

But still - there is no hierarchical dependency between the packages named similarily. Module `std` here contains two
packages one named `my.pkg.foo` and second named `my.pkg.bar`. It's mostly for the developer.

The `package` informs that what follows will be evaluated within the context of given module and package. It's possible
to define multiple packages within one source code file.

# No fully qualified names

Functions and variables from other packages can only be called by their imported alias. There is no possibility to use
the fully qualified path to function or variable with module and package specified inline.

!! Importing

Developer can use `import` keyword to import function from another package. Every function from current module can be
imported (doesn't have to be public) but if you want to import functions from another module they need to be made
publicly available with `pub` keyword.

For example importing `myFunc` function from package `system` in module `std` looks like this:

```chi
import std/my.pkg { myFunc }
import std/my.pkg { otherFunc }
```

For convinience you can import multiple functions at once and provide an alias for them (in case you'd have name
collission):

```chi
import std/my.pkg { myFunc as foo, otherFunc as bar }
```

You can alias the package:

```chi
import std/my.pkg as baz
```

...or everything:

```chi
import std/my.pkg as baz { myFunc as foo, otherFunc as bar }
```

## Visibility

Every variable, function and variant field is visible internally inside the module it's defined in (so in all packages
in the module). By default - they are not visible outside the module. You can make things visible outside the module
with `pub` keyword. It can be used for variables...

```chi
val foo = 5 // this iss visible only inside current module
pub val bar = 10 // this can be accessed from other modules as well
```

...and functions and effects:

```chi
fn foo() {}

// module internal
pub fn bar() {} // public
pub effect baz()
```

### Variant constructor visibility

Variant types are visible everywhere, but their constructors can be individually made public:

```chi
// declare type SomeType with public constructor SomeType
data pub SomeType(field: int)
// the same thing with basic variant type declaration syntax
data SomeType = pub SomeType(field: int)
```

Note that you can only control constructor visibility:

```chi
// this is ok
data SomeType = pub SomeType(field: int)
// this is not
data pub SomeType = SomeType(field: int)
```

Some constructors may be public and other can be left internal:

```chi
data SomeType = pub Foo(i: int) | Bar(f: float)
```

### Variant field visibility

Variant fields are also visible only within the module they are declared by default. To change that user can use `pub`
keyword to allow field acces from other module:

```chi
// make `i` visible everywhere, `f` is still only visible within the module
data MyType( pub i: int, f: float)
```

