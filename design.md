# Design of Test Framework

A DSL proposed to automate the debuggability tests.

## Assumptions

Assumption:

1. Each test only involves one source file and one debug file

Conventions (defaults):

1. Debug file and source file are located in the same directory
1. Source files have the extension `.scala` in file name
1. Check files have the extension `.debug` in file name

## Example

An example test file `Test.debug`:

```
---
break: 10, 13, 15
---
step
4 step

# this is a comment

cont
list: 17 =>

list: "17 =>"
```

In the example above, the lines between the separator `---` is the configurations,
which is also called *front matter*.


The body holds the interactions with the debugger.
In the body, each line is a command to the debugger. To add a check for the
expected output of the command, just append the command with a colon (`:`).

The expected output must be a line string, which can be immediately after the
colon or in the next line. The spaces around the text is ignored.

In most cases, you can just use literal strings after the colon, which is
matched if it's part of the command output. Wildcards like `*` can be used.

User can also specify a regular expression as the expected output. To do this,
the regular expression should be wrapped inside a quote.

Comments and blank lines are ignored.

## Implementation

From a debug file, we first generate a test file based on `expect`, then execute the generated test file.

## Usage

A command `debug` will be provided. To automatically debug a file, run the command:

   debug test.scala

The command will do the following:

- compile the source code
- generate test file from `test.debug` under the same folder
- run the test file

Following options are supported

- `-c`: specify a debug file different from the default
- `-m`: specify the main class different from `Test`

