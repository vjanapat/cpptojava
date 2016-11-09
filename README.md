# cpptojava
Prerequisite to execute the project:
Extract project cpptojava.
1)	Make sure to set right values in config.properties
a)	HOME_PATH: Is the directory under which sub-directories for cppfiles(input) and javafiles(output) exists.
b)	INPUT: Is the directory where input cpp files exists it should be HOME_PATH+”cppfiles”.
Copy all files from cpptojava/test folder to INPUT folder.
c)	OUTPUT: Is the directory where output java files exists it should be HOME_PATH+”javafiles”

2)	Make sure to input jar’s under lib folder to classpath. If its eclipse right click on project -> buid path -> add external jar’s
3)	Make sure java version is > 1.7

How Does the Tranformation happens:
It’s a two step process
1)	Parse C++ Code to an abstract Syntax tree using eclipse.cdt.dom.parser class.
This provides a tree of declarations ( of various types functions, classes, namespaces ..etc) with nested declarations.

2)	Then we recursively parse each declaration making use of different user defined manger classes( eg: FunctionManager,EnumManager etc.) to handle different types of declarations.
Each declaration can have statements or expression which get evaluated from c++ code to java using corresponding evaluator classes( eg: ExpressionEvaluator,StmtEvaluator)
