# ASMPlugModifier

A little toy project to explore how ObjectWeb ASM works and how to change jvm code that have already been compiled into a jar file.
I had a specific use case when I wrote it, but because of the impracticality of maintaining a program that modifies bytecode, this really is more of a toy project.

``/src/main/kotlin/patch/patch.kt`` - Contains the general mechanism to patch a jar file, based on a set of patches. The Patch interface is also defined here.

``/src/main/kotlin/patch/AddSimpleEnumValue.kt`` - This file contains a patch to add an enum constant to an enum that doesn't have any parameters or other complex initialization logic. It's designed to fail fast, in case the structure of the enum doesn't match what we expect.

``/src/main/kotlin/Main.kt`` - The main entry point to the program. The patch data specific to my use case is also stored here.
