
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright 2004 Arno Unkrig
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.codehaus.janino;

import java.io.*;

/**
 * Parses a class body and returns it as a <tt>java.lang.Class</tt> object
 * ready for use with <tt>java.lang.reflect</tt>.
 * <p>
 * Example:
 * <pre>
 *   import java.util.*;
 *
 *   static private int a = 1;
 *   private int b = 2;
 *
 *   public void func(int c, int d) {
 *       return func2(c, d);
 *   }
 *
 *   private static void func2(int e, int f) {
 *       return e * f;
 *   }
 * </pre>
 */

public class ClassBodyEvaluator extends EvaluatorBase {
    private static final String DEFAULT_CLASS_NAME = "SC";

    /**
     * See {@link #ClassBodyEvaluator(Scanner, String, Class, Class[], ClassLoader)}.
     */
    public ClassBodyEvaluator(
        String classBody
    ) throws Java.CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this(
            new Scanner(null, new java.io.StringReader(classBody)), // scanner
            null                                                    // optionalParentClassLoader
        );
    }

    /**
     * See {@link #ClassBodyEvaluator(Scanner, String, Class, Class[], ClassLoader)}.
     */
    public ClassBodyEvaluator(
        String      optionalFileName,
        InputStream is
    ) throws Java.CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this(
            new Scanner(optionalFileName, is), // scanner
            null                               // optionalParentClassLoader
        );
    }

    /**
     * See {@link #ClassBodyEvaluator(Scanner, String, Class, Class[], ClassLoader)}.
     */
    public ClassBodyEvaluator(
        String   optionalFileName,
        Reader   reader
    ) throws Java.CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this(
            new Scanner(optionalFileName, reader), // scanner
            null                                   // optionalParentClassLoader
        );
    }

    /**
     * See {@link #ClassBodyEvaluator(Scanner, String, Class, Class[], ClassLoader)}.
     */
    public ClassBodyEvaluator(
        Scanner     scanner,
        ClassLoader optionalParentClassLoader
    ) throws Java.CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this(
            scanner,                  // scanner
            (Class) null,             // optionalExtendedType
            new Class[0],             // implementedTypes
            optionalParentClassLoader // optionalParentClassLoader
        );
    }

    /**
     * See {@link #ClassBodyEvaluator(Scanner, String, Class, Class[], ClassLoader)}.
     */
    public ClassBodyEvaluator(
        Scanner     scanner,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        ClassLoader optionalParentClassLoader
    ) throws Java.CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        this(
            scanner,                               // scanner
            ClassBodyEvaluator.DEFAULT_CLASS_NAME, // className
            optionalExtendedType,                  // optionalExtendedType
            implementedTypes,                      // implementedTypes
            optionalParentClassLoader              // optionalParentClassLoader
        );
    }

    /**
     * Parse and compile a class body, i.e. a series of member definitions.
     * <p>
     * The <code>optionalClassLoader</code> serves two purposes:
     * <ul>
     *   <li>It is used to look for classes referenced by the class body.
     *   <li>It is used to load the generated Java<sup>TM</sup> class
     *   into the JVM; directly if it is a subclass of {@link
     *   ByteArrayClassLoader}, or by creation of a temporary
     *   {@link ByteArrayClassLoader} if not.
     * </ul>
     * A number of constructors exist that provide useful default values for
     * the various parameters, or parse the class body from a
     * {@link String}, an {@link InputStream} or a {@link Reader}
     * instead of a {@link Scanner}.
     *
     * @param scanner Source of tokens
     * @param className The name of the temporary class (uncritical)
     * @param optionalExtendedType Class to extend or <tt>null</tt>
     * @param implementedTypes Interfaces to implement
     * @param optionalParentClassLoader Loads referenced classes
     */
    public ClassBodyEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        ClassLoader optionalParentClassLoader
    ) throws Java.CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        super(optionalParentClassLoader);
        Java.CompilationUnit compilationUnit = new Java.CompilationUnit(scanner.peek().getLocation().getFileName());
        
        // Parse import declarations.
        Parser parser = new Parser(scanner);
        this.parseImportDeclarations(compilationUnit, scanner);
        
        // Add class declaration.
        Java.ClassDeclaration cd = this.addPackageMemberClassDeclaration(
            scanner.peek().getLocation(),
            compilationUnit,
            className, optionalExtendedType, implementedTypes
        );
        
        // Parse class body declarations (member declarations) until EOF.
        while (!scanner.peek().isEOF()) {
            parser.parseClassBodyDeclaration(cd);
        }

        // Compile and load it.
        try {
            this.clazz = this.compileAndLoad(
                compilationUnit,                                             // compilationUnit
                DebuggingInformation.SOURCE.add(DebuggingInformation.LINES), // debuggingInformation
                className                                                    // className
            );
        } catch (ClassNotFoundException e) {
            throw new RuntimeException();
        }
    }

    /**
     * Compiles a class body and instantiates it. The generated class may optionally
     * extend/implement a given type. The returned instance can safely be type-casted
     * to that <code>optionalBaseType</code>.
     * <p>
     * Example:
     * <pre>
     * public interface Foo {
     *     int bar(int a, int b);
     * }
     * ...
     * Foo f = (Foo) ClassBodyEvaluator.createFastClassBodyEvaluator(
     *     new Scanner(null, new StringReader("public int bar(int a, int b) { return a + b; }")),
     *     Foo.class,
     *     (ClassLoader) null          // Use current thread's context class loader
     * );
     * System.out.println("1 + 2 = " + f.bar(1, 2));
     * </pre>
     * Notice: The <code>optionalBaseType</code> must either be declared <code>public</code>,
     * or with package scope in the root package (i.e. "no" package).
     * 
     * @param scanner Source of class body tokens
     * @param optionalBaseType Base type to extend/implement (see above)
     * @param optionalClassLoader
     * @return an object that extends/implements the given <code>optionalBaseType</code>
     */
    public static Object createFastClassBodyEvaluator(
        Scanner     scanner,
        Class       optionalBaseType,
        ClassLoader optionalClassLoader
    ) throws Java.CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        return ClassBodyEvaluator.createFastClassBodyEvaluator(
            scanner,                               // scanner
            ClassBodyEvaluator.DEFAULT_CLASS_NAME, // className
            (                                      // optionalExtendedType
                optionalBaseType != null && !optionalBaseType.isInterface() ?
                optionalBaseType : null
            ),
            (                                      // implementedTypes
                optionalBaseType != null && optionalBaseType.isInterface() ?
                new Class[] { optionalBaseType } : new Class[0]
            ),
            optionalClassLoader                    // optionalClassLoader
        );
    }

    /**
     * Like {@link #createFastClassBodyEvaluator(Scanner, Class, ClassLoader)},
     * but gives you more control over the generated class (rarely needed in practice).
     * <p> 
     * Notice: The <code>optionalExtendedType</code> and the <code>implementedTypes</code>
     * must either be declared <code>public</code>, or with package scope in the same
     * package as <code>className</code>.
     * 
     * @param scanner Source of class body tokens
     * @param className Name of generated class
     * @param optionalExtendedType Class to extend
     * @param implementedTypes Interfaces to implement
     * @param optionalParentClassLoader Loads referenced classes
     * @return an object that extends the <code>optionalExtendedType</code> and implements the given <code>implementedTypes</code>
     */
    public static Object createFastClassBodyEvaluator(
        Scanner     scanner,
        String      className,
        Class       optionalExtendedType,
        Class[]     implementedTypes,
        ClassLoader optionalParentClassLoader
    ) throws Java.CompileException, Parser.ParseException, Scanner.ScanException, IOException {
        Class c = new ClassBodyEvaluator(
            scanner,                  // scanner
            className,                // className
            optionalExtendedType,     // optionalExtendedType
            implementedTypes,         // implementedTypes
            optionalParentClassLoader // optionalParentClassLoader
        ).evaluate();
        try {
            return c.newInstance();
        } catch (InstantiationException e) {
            throw new Java.CompileException("Cannot instantiate abstract class -- one or more method implementations are missing", null);
        } catch (IllegalAccessException e) {
            // SNO - type and default constructor of generated class are PUBLIC.
            throw new RuntimeException(e.toString());
        }
    }

    /**
     * Returns the <tt>java.lang.Class</tt> object compiled from the class
     * body.
     */
    public Class evaluate() {
        return this.clazz;
    }

    private final Class clazz;
}
