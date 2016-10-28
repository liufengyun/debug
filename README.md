# Debug

Investigate debuggability of dotty


# Tools

- jdb (`jdb -help` and `help`)
- javap  (`javap -c -l -p Test.class`)

# Configure

Change the path in the `path` script to your local setting.

# Manual debug

Debug the file `test.scala` as follows:

    ./debug test.scala

# Automatic debug

Suppose you defined a file `tests/sequence.debug`, then you can run:

    ./check tests/sequence.scala

