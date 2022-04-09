# Vox

Minimalistic, interpreted programming language with a very regular syntax.

## Introduction

Vox is a language I created mainly for two purposes.
First, because creating my own programming language is something I wanted to do for years.
In order to make it not too difficult, I decided on certain parts of the language design specifically, so it would be relatively easy to parse.
For example the first token in each statement already indicates which kind of statement it is.

The second reason is that I wanted to create a language mainly intended for educational purposes.
Its grammar and syntax are very regular (e.g. typical, arithmetic operators are just regular functions).
The distinction between statements and expressions is essential for the understanding of many programming languages.
This is why Vox makes a relatively strict distinction between them, with the exception of the function call, which can be both.
In general, the language is quite strict (if-statements require boolean values, values of different types are never equal to each other)
without being overly restrictive (the "concat" function does not require you to convert every value to a string).

I also wanted to include a couple of more sophisticated features that I think are quite important to understand early on when learning how to program.
Therefore, Vox supports some typical concepts of functional programming languages like higher-order-functions and closures.

## Language specification

### Data types

Vox supports the following data types:

| Type   | Description                                                             |
|--------|-------------------------------------------------------------------------|
| Int    | 32 bit integer                                                          |
| Float  | 64 bit, double precision                                                |
| Bool   | either "true" or "false"                                                |
| String | and immutable string of unicode characters                              |
| List   | mutable zero-indexed collection                                         |
| Dict   | hash map with keys of any type                                          |
| Range  | inclusive integer range                                                 |
| Func   | function pointer                                                        |
| Nil    | type with the single instance "nil" that represents the absence of data |

### Grammar

#### Statements
Vox makes a clear distinction between statements and expressions.
A Vox program is defined as a list of statements.
Statements are the parts of the language that get things done and are supposed to have an effect on the rest of the program.

##### Variable declaration
Declares a variable in the current scope and initializes it with the value `nil`.
```
var test
```
You can also define multiple variables at once
```
var [a b c]
```

##### Variable Assignment
Assigns to an already existing variable. Assigning to a variable that doesn't exist causes a runtime error.
```
var test
as test 42
```

There is a shorthand syntax for declaring and defining a variable in one line.
```
varas name "Vox"

# is equivalent to
var name
as name "Vox"
```

##### Constant definition
Creates a constant and assigns to it a given value.
```
const PI 3.1415
```

Constants can not be reassigned.
```
as PI 5   # Will throw a runtime error
```

##### Function definition (statement shorthand)
```
function helloWorld []
    (print "Hello World")
end
```
This statement is equivalent to
```
const helloWorld func []
    (print "Hello World")
end
```
Please look at function definition expressions for more details.

##### Function call
Calls a function. The first expression inside the pair of parentheses is the actual function.
The following expressions are its arguments
```
(print "Hello World")
(print "The answer is: " answer)
(print)
```
Function calls are the only language elements that can be both statements and expressions depending on context.

##### Block
Blocks group a list of statements into a single unit. They are mainly used to create sub-scopes.
```
do
    var test
    as test (add 1 2)
    (print test)
end
```

##### If condition
Execute a block of code only if the condition yields `true`.
An *else* branch and several *else if* branches are also permitted.
```
if (eq (add 1 2) 3)
    (print "Hurray")
end

if this
    # do something
elif that
    # do something else
else
    # do something entirely different
end

if true
    (print false)
else
    (print true)
end
```
If conditions are required to be `Bool`s. Using a value other than a `Bool` causes a runtime exception.

##### While loop
```
varas i 0
while (lt i 10)
    (print i)
    as i (inc i)
end
```
While conditions are required to be `Bool`s. Using a value other than a `Bool` causes a runtime exception.

##### For loop
Values of types `List`, `Dict` and `Range` can be iterated over.
```
const l (list 1 2 3 4)
const d (dict "a" 1 "b" 2 "c" 3)
const r (range 0 4)

var i
for i l
    (print i)   # 1, 2, 3, 4
end

for i d
    (print i)   # a, b, c
end

for i r
    (print i)   # 0, 1, 2, 3, 4
end
```
You can also declare a variable directly inside a for loop. This variable will only exist for the duration of the loop.
```
for var i (range 4 1 -1)
    (print i)  # 4, 3, 2, 1
end
```

##### Return statement
Returns a value from a function and terminates its execution.
```
function test []
    return 1
end

(print (test))  # 1
```
Return statements require a return value. Omitting this value is not allowed.
If you just want to exit a function without returning a specific value, use `exit`, which is a shorthand of `return nil`.
```
function test []
    exit
end
```


##### Break and Continue
`break` allows you to exit from a loop early while `continue` skips the rest of the current iteration.
```
for var i (range 1 10)
    if (eq i 4)
        continue
    end
    if (eq i 8)
        break
    end
    (print i)
end

# Prints 1, 2, 3, 5, 6, 7
```

#### Expressions
Expressions are the parts of Vox that represent values.
They cannot stand alone but are always a part of statement.

##### Constant Literals
Literals represent constant values that do not have to be computed.
```
# Nil
nil

# Int
1
-42

# Float
3.5
-17.74

# Bool
true
false

# String
"Hello World"
```
String literals allow the following escape sequences: `\"`, `\\`, `\n`, `\r`, `\t`.

##### Variables
Variables can temporarily store values that will be used at a later point in the program and are referred to by name.
Their names are allowed to consist of the following symbols.
```
abcdefghijklmnopqrstuvwxyz
ABCDEFGHIJKLMNOPQRSTUVWXYZ
0123456789
_-+*/%><&|'!?$=~
```
There are two exceptions:
* A variable name cannot start with a digit
* If a variable starts with `+` or `-`, the second character cannot be a digit.

###### Variable Scopes
Functions, blocks, if conditions, while loops, and for loops all define their own scopes
Each variable is only visible in the scope where it was created or in one of its sub-scopes.
```
varas a 1
do
    varas b 2
    if true
        varas c 3
        # a, b and c are visible here
    end
    # a and b are visible here
end
# only a is visible here
```
Inside functions you have always access to variables in the scope where the function was created,
even if the function is called from outside this scope.
```
var test
do
    varas a 123
    function f []
        as a (inc a)
        (print a)
    end
    as test f
end

(test)   # prints 124
```
You can create a variable even if there already exists a variable with the same name in one of the outer scopes.
If you do that, the inner variable so called "shadows" the outer one.
Note that these two variables then have nothing to do with each other, except that they happen to have the same name.
```
varas test 42
do
    # redeclaring a variable test
    varas test 3
    (print test)   # prints 3
end
(print test)       # prints 42
```

##### Function call
Where other kinds of expressions represent a value directly, a function call is an expression that has to be evaluated first before it results in a value.
When a function call is used as an expression, the function will get executed and the value returned by the function becomes the value of the expression.

The syntax is identical to function call statements.
```
const a (add 1 2)
const b (div 9 b)

(print (inc b))
```

##### Function definition
In Vox a function is considered a value like any other.
This means, it can be assigned to variables, passed to other functions, etc.
As functions are considered values, a function definition is considered an expression.

A function definition consists of the keyword `func`, followed by a parameter list in brackets, followed by a block of code, which is also called the function body.
```
var test
as test func [a b]
    return (add (mul a 2) b)
end
```

If a function has exactly one parameter, the brackets can be omitted.
```
func x
    return (mul x 2)
end
```
When calling a function, the given arguments are assigned to variables inside the newly created function scope with the names stated in the parameter list.

Execution of a function ends when encountering a return/exit statement or when reaching the end of its body.
In the latter case, the function implicitly returns `nil`.

###### Closures
In Vox, function can access and even change the environment, in which they were created.
A function that does this is called a closure. Some examples:
```
function makeCounter []
    varas n 0
    function count []
        as n (inc n)
        return n
    end
    return count
end

const counter (makeCounter)
const counter2 (makeCounter)

(print (counter))   # prints 1
(print (counter2))  # prints 1
(print (counter))   # prints 2
(print (counter2))  # prints 2


function makeAdder x
    return func y
        return (add x y)
    end
end

const add2 (makeAdder 2)

(print (add2 5))  # prints 7
(print (add2 6))  # prints 8
```

###### Shorthand lambda syntax
Very short function definitions that have the form `func [args...] return expression end` can be expressed in a more concise manner.

```
\a (mul a 2)
# is equivalent to
func a return (mul a 2) end

\[a b] (add (mul a 2) b)
# is equivalent to
func [a b] return (add (mul a 2) b) end
```
This is especially useful when these so called "lambda expressions" are passed to another function:
```
const l (list 1 2 3 4 5 6)
const squares (map l \x (mul x x))
(print squares)   # [1, 4, 9, 16, 25]
```

### Standard library

#### Special functions

Special functions are functions that when called have different semantics (like lazy evaluation of its arguments).
They can be called, assigned to variables, etc. just like any other function, but internally they are called in a different way.

##### and, or
They take a variable number of boolean values as arguments and return the logical conjunction/disjunction of these values.
In other words: `and` returns `true` if all input values are `true`, `or` returns `true` if at least one of its inputs is `true`.
They both perform lazy evaluation. For example, if the first argument to `or` is true, the rest of its arguments is not even evaluated.

```
const value (int (read))

# Checks if conversion was successful and divisible by 2.
if (and (neq value nil) (eq (mod value 2) 0))
    # do something
end
```
Without lazy evaluation, this check could throw an error if the conversion to int in the first line failed, because "mod" throws an error if one of its arguments is `nil`.

##### choice
`choice` expects exacty three arguments. It evaluates the first.
If it yields `true`, it evaluates the second and returns its result.
Otherwise, it returns the third.
This can be used as a handy shorthand for a more verbose if condition.

```
const maybeValue (int (read))
const value (choice (eq maybeValue nil) 0 maybeValue)

# is equivalent to

const maybeValue (int (read))
var tempValue
if (eq maybeValue nil)
    as tempValue 0
else
    as tempValue maybeValue
end
const value tempValue
```

#### Native functions
Native functions are part of the runtime and written in Kotlin. They could not be have been written just using Vox.

##### add, mul
Take a variable number of arguments or just one argument that is list of numbers and calculate the sum/product of them.
If one of the inputs is a `Float`, the result will be a `Float`, otherwise an `Int`.

```
(add 2 3)               # 5
(add 2 3 4 5)           # 9
(mul (list 1 2 3 4 5))  # 10
```
Aliases: `+`, `*`

##### sub
Expects two numbers as inputs and subtracts the second from the first.
If both inputs ar `Int`s, it returns an `Int`, otherwise a `Float`.

```
(sub 3 -5)   # 8
(sub 4.2 1)  # 5.2
```
Alias: `-`

##### mod
Expects two numbers as inputs and returns the remainder of the first number divided by the second.
If both inputs ar `Int`s, it returns an `Int`, otherwise a `Float`.

```
(mod 7 3)   # 1
(mod 7 -3)  # -1
```

##### div
Divides two given numbers.
Unlike other arithmetic functions like `add`, `div` always returns a `Float`.
```
(div 2.2 11)  # 0.2
(div 1 2)     # 0.5
```
Alias: `/`

##### intdiv
Requires two `Int`s.
Returns the integer quotient of these numbers.
```
(intdiv 3 2)   # 1
(intdiv -3 2)  # -1
```

##### pow
Requires two `Float`s.
Returns the first to the power of the second.
```
(pow 5.0 2.0)  # 25.0
(pow 4.0 0.5)  # 2.0
```

##### eq
Expects two values of any type and returns `true` *iff* they are equal.
Note that two values of different types can never be equal, even if they represent the same value (e.g. `2` and `2.0`)
```
(eq (concat "a" "b") "ab")     # true
(eq (add 1 2) 3.0)             # false
(eq (list 1 2 3) (list 1 2 3)) # true
```
Alias: `=`

##### id
Expects two values of any type and returns `true` *iff* they are identical.
Two variables are identical if they refer to the exact same data in memory.
This means that when you modify one, you also modify the other as both represent the exact same value.

The notion of identity only makes sense for values that can be modified, namely `List`s and `Dict`s.
For all other types (like `Int` or `String`) `id` always returns the same value as `eq`
```
const a (list 1 2 3)
const b (list 1 2 3)
const a2 a

(print (id "abc" "abc")) # true

(print (id a b))   # false
(print (eq a b))   # true
(print (id a a2))  # true
(print (eq a a2))  # true
```

##### lt
Expects two numbers (which both can be of type `Int` or `Float`) or two `String`s.
For two numbers, it returns `true` *iff* the first one is less than the second.
For two `String`s, it returns `true` *iff* the first one would come before the second in an alphabetically sorted list.
```
(lt 1 2)       # true
(lt 2 1)       # false
(lt 1.0 2)     # true
(lt "ab "ac")  # true
```

##### min, max
Take a variable number of arguments or just one argument that is list of numbers.
They return the maximum or minimum number of the given numbers.
The result is an `Int` if all inputs are `Int`s. Otherwise it is a `Float`.
```
(min -4 3.0)           # -4.0
(max (list 1 2 3 4 5)) # 5
```

##### print
Takes a variable number of arguments, converts them to strings, concatenates them, and prints them to the console with a new line character at the end.
```
(print "The answer is " (add 40 2)) # Prints "The answer is 42"
(print (list 1 2 3))                # Prints "[1, 2, 3]"
```

##### read
Takes no arguments. The function reads a single line from the console and returns it as a `String`.


##### panic
Takes a `String` as an argument and throws a runtime exception with the message `[panic] <arg>` where `<arg>` is the provided argument.


##### random
Returns a random `Float` in the range `[0, 1)`.


##### charList
Takes a `String` and returns a `List` that includes all characters of that string.
```
(charList "Hello")   # (list "H" "e" "l" "l" "o")
```


##### concat
Takes a variable number of arguments or exactly one argument that is a `List`, converts all given values to `String`s, concatenates them and returns the concatenation as a `String`

```
(concat 1 2 3)                      # "123"
(concat (list "one" "two" "three")) # "onetwothree"
```


##### int
Takes a `Float`, `Int` or `String` and converts it two an `Int`.
If the argument is an `Int`, the function simply returns it.
If the argument is a `Float`, the function rounds it to the nearest representable `Int` towards zero.
If the argument is a `String` and it cannot be converted to an `Int`, the function returns `nil`
```
(int -3.5)           # -3
(int (pow 2.0 256.0) # 2,147,483,647 (maximum possible int)
(int nan)            # 0
(int "42")           # 42
(int "test42")       # nil
```


##### float
Takes a `Float`, `Int` or `String` and converts it two an `Float`.
A `Float` or `Int` can always be converted accurately.
If the argument is a `String` and it cannot be converted to an `Float`, the function returns `nil`
```
(float 3.5)     # 3.5
(float 1)       # 1.0
(float "42")    # 42.0
(float "three") # nil
```


##### get
Takes a collection (`List` or `Dict`) and an index (or a key for a dictionary) and returns the value at that index or key.
If the collection is a `List` and the index is not an `Int` or not a valid index, the function throws an exception.
If the collection is a `Dict` and the key does not exist in it, the function simply returns `nil`.
```
(get (list 1 2 3) 1)       # 2
(get (list 1 2 3) "1")     # throws "invalid type exception"
(get (list 1 2 3) 3)       # throws "index out of bounds exception"
(get (dict 1 "a" 2 "b") 1) # a
(get (dict 1 "a" 2 "b") 3) # nil
```
Elements of a nested collection can be received using just one `get` call:
```
const points (list
    (dict "x" 1 "y" 2)
    (dict "x" 3 "y" 4)
)
(print (get points 1 "y")) # 4
```


##### set
Similar to get. It takes three arguments: the collection (`List` or `Dict`), the index (or key), and the new value.
For a `List`, the index is expected to be an `Int` and not out of bounds.
If the index is valid, the respective element is set to the new value.
Otherwise, the function throws an exception.
For a `Dict`, the function changes the value if the key already exists (based on equality, not identity). Otherwise, a new key-value-pair is created.
```
const l (list 1 2 3)
const d (dict 1 "a")
const nested (list (list 1 2) (list 3 4))

(set l 2 4)   # l is now [1, 2, 4]
(set l 3 4)   # throws "index out of bounds exception"
(set d 2 "b") # d is now {1: "a", 2: "b"}
(set d 1 "c") # d is now {1: "c", 2: "b"}
(set nested 0 1 1) # nested is now [[1, 0], [3, 4]]
```


##### size
Takes a collection (`List` or `Dict`) and returns the number of values (or key-value-pairs for a `Dict`) in it.
```
(size (list 1 2 3))       # 3
(size (dict 1 "a" 2 "b")) # 2
```


##### in
Takes a collection (`List` or `Dict`) or a `Range `and a value.
For a `List`, it returns `true` *iff* the value is in the list (based on equality, not identity).
For a `Dict`, it returns `true` *iff* the the key exists in the dictionary.
For a `Range`, it requires the value to be an `Int` or a `Float`.
If it is, the function returns `true` *iff* `rangeStart <= value <= rangeEnd`
```
(in (list 1 2 3) 3)         # true
(in (list 1 2 3) 0)         # false
(in (dict 1 "a" 2 "b") 1)   # true
(in (dict 1 "a" 2 "b") "a") # false
(in (range 1 10 2) 10)      # true
(in (range 1 10) 10.5)      # false
```


##### remove
Takes a collection (`List` or `Dict`) and a value.
For a `List`, the function removes the first element that is equal to that value from it.
For a `Dict`, the function looks for the key and removes the key-value-pair if it finds it.
In both cases, the function returns `true` *iff* it removed a value
```
const l (list 1 2 3 2)
const d (dict 1 "a" 2 "b")

(remove l 2)    # true, l is now [1, 2, 3]
(remove d 1)    # true, d is now {2: "b"}
(remove l 4)    # false, l does not change
(remove d "b")  # false, d does not change
```


##### list
Takes a variable number of arguments and returns a `List` including all of them.
```
(list 1 2 3)   # [1, 2, 3]
(list "value") # ["value"]
(list)         # []
```


##### push
Takes a `List` and any value and appends that value to the list.
```
const l (list 1 2 3)
(push l 4)  # list is now [1, 2, 3, 4]
```


##### pop
Takes a `List`, removes the last element of it and returns it.
If the list is empty, it returns `nil`.
```
const l (list 1 2)
(pop l)  # 2, l is now [1]
(pop l)  # 1, l is now []
(pop l)  # nil, l did not change
```


##### dict
Takes an even number of arguments.
Returns a `Dict`, which includes all given key-value-pairs.
```
(dict 1 "a" 2 "b")  # {1: "a", 2: "b"}
(dict 1)            # throws runtime exception
(dict)              # {} (empty dictionary)
```


##### range
Takes three `Int`s (a start value, an end value, and a step size) and returns a `Range`.
The third argument can be omitted and then defaults to `1`.
```
(range 1 5)    # range (1, 2, 3, 4, 5)
(range 1 8 2)  # range (1, 3, 5, 7)
(range 5 1 -1) # range (5, 4, 3, 2, 1)
(range 5 1 1)  # empty range
```


##### rangeProps
Takes a `Range` and returns a `List` containing its three parameters (start, end, step).
```
(rangeProps (range 1 10))   # [1, 10, 1]
(rangeProps (range 1 9 2))  # [1, 9, 2]
```


##### type
Takes any value and returns its type as a `String`.
```
(type nil)        # "Nil"
(type true)       # "Bool"
(type 1)          # "Int"
(type 2.0)        # "Float"
(type "a")        # "String"
(type (list))     # "List"
(type (dict))     # "Dict"
(type (range 1 5) # "Range"
(type add)        # "Func"
```



#### stdlib.vox
More functions, constants and function aliases that are also available globally can be found in [stdlib.vox](/src/runtime/stdlib/stdlib.vox).

















<!--
## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes. See deployment for notes on how to deploy the project on a live system.

### Prerequisites

What things you need to install the software and how to install them

```
Give examples
```

### Installing

A step by step series of examples that tell you how to get a development env running

Say what the step will be

```
Give the example
```

And repeat

```
until finished
```

End with an example of getting some data out of the system or using it for a little demo

## Running the tests

Explain how to run the automated tests for this system

### Break down into end to end tests

Explain what these tests test and why

```
Give an example
```

### And coding style tests

Explain what these tests test and why

```
Give an example
```

## Deployment

Add additional notes about how to deploy this on a live system

## Built With

* [Dropwizard](http://www.dropwizard.io/1.0.2/docs/) - The web framework used
* [Maven](https://maven.apache.org/) - Dependency Management
* [ROME](https://rometools.github.io/rome/) - Used to generate RSS Feeds

## Contributing

Please read [CONTRIBUTING.md](https://gist.github.com/PurpleBooth/b24679402957c63ec426) for details on our code of conduct, and the process for submitting pull requests to us.

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/your/project/tags).

## Authors

* **Billie Thompson** - *Initial work* - [PurpleBooth](https://github.com/PurpleBooth)

See also the list of [contributors](https://github.com/your/project/contributors) who participated in this project.

## License

This project is licensed under the MIT License - see the [LICENSE.md](LICENSE.md) file for details

## Acknowledgments

* Hat tip to anyone whose code was used
* Inspiration
* etc

-->
