
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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.janino.util.ClassFile;


/**
 * Utilities for the various "...Evaluator" classes.
 */

public class EvaluatorBase {
    private final static boolean DEBUG = false;

    /**
     * Construct with the given {@link ClassLoader}.
     *  
     * @param optionalParentClassLoader null == use current thread's context class loader
     */
    protected EvaluatorBase(ClassLoader optionalParentClassLoader) {
        this.classLoaderIClassLoader = new ClassLoaderIClassLoader(
            optionalParentClassLoader != null ?
            optionalParentClassLoader :
            Thread.currentThread().getContextClassLoader()
        );
    }

    /**
     * Parse as many import declarations as possible for the given
     * {@link Java.CompilationUnit}.
     * @param compilationUnit 
     * @param scanner Source of tokens
     * @throws Scanner.ScanException
     * @throws Parser.ParseException
     * @throws IOException
     */
    protected void parseImportDeclarations(
        Java.CompilationUnit compilationUnit,
        Scanner              scanner
    ) throws Scanner.ScanException, Parser.ParseException, IOException {
        Parser parser = new Parser(scanner);
        while (scanner.peek().isKeyword("import")) parser.parseImportDeclaration(compilationUnit);
    }

    /**
     * To the given {@link Java.CompilationUnit}, add
     * <ul>
     *   <li>A class declaration with the given name, superclass and interfaces
     *   <li>A method declaration with the given return type, name, parameter
     *       names and values and thrown exceptions
     * </ul> 
     * @param location
     * @param compilationUnit
     * @param className
     * @param optionalExtendedType (null == {@link Object})
     * @param implementedTypes
     * @return The created {@link Java.ClassDeclaration} object
     * @throws Parser.ParseException
     * @throws Scanner.ScanException
     * @throws IOException
     */
    protected Java.PackageMemberClassDeclaration addPackageMemberClassDeclaration(
        Scanner.Location     location,
        Java.CompilationUnit compilationUnit,
        String               className,
        Class                optionalExtendedType,
        Class[]              implementedTypes
    ) throws Parser.ParseException {
        Java.PackageMemberClassDeclaration tlcd = new Java.PackageMemberClassDeclaration(
            location,                                         // location
            compilationUnit,                                  // declaringCompilationUnit
            Mod.PUBLIC,                                       // modifiers
            className,                                        // name
            this.classToType(location, optionalExtendedType), // optionalExtendedType
            this.classesToTypes(location, implementedTypes)   // implementedTypes
        );
        compilationUnit.addPackageMemberTypeDeclaration(tlcd);
        return tlcd;
    }

    /**
     * To the given {@link Java.CompilationUnit}, add
     * <ul>
     *   <li>A package member class declaration with the given name, superclass and interfaces
     *   <li>A public method declaration with the given return type, name, parameter
     *       names and values and thrown exceptions
     *   <li>A block 
     * </ul> 
     * @param location
     * @param compilationUnit
     * @param className
     * @param optionalExtendedType (null == {@link Object})
     * @param implementedTypes
     * @param staticMethod Whether the method should be declared "static"
     * @param returnType Return type of the declared method
     * @param methodName
     * @param parameterNames
     * @param parameterTypes
     * @param thrownExceptions
     * @return The created {@link Java.Block} object
     * @throws Parser.ParseException
     * @throws Scanner.ScanException
     * @throws IOException
     */
    protected Java.Block addClassMethodBlockDeclaration(
        Scanner.Location     location,
        Java.CompilationUnit compilationUnit,
        String               className,
        Class                optionalExtendedType,
        Class[]              implementedTypes,
        boolean              staticMethod,
        Class                returnType,
        String               methodName,
        String[]             parameterNames,
        Class[]              parameterTypes,
        Class[]              thrownExceptions
    ) throws Parser.ParseException {
        if (parameterNames.length != parameterTypes.length) throw new RuntimeException("Lengths of \"parameterNames\" and \"parameterTypes\" do not match");

        // Add class declaration.
        Java.ClassDeclaration cd = this.addPackageMemberClassDeclaration(
            location,
            compilationUnit,
            className, optionalExtendedType, implementedTypes
        );

        // Add method declaration.
        Java.MethodDeclarator md = new Java.MethodDeclarator(
            location,                                        // location
            cd,                                              // declaringClassOrInterface
            (                                                // modifiers
                staticMethod ?
                (short) (Mod.PUBLIC | Mod.STATIC) :
                (short) Mod.PUBLIC
            ),
            this.classToType(location, returnType),         // type
            methodName,                                      // name
            this.makeFormalParameters(                      // formalParameters
                location,
                parameterNames, parameterTypes
            ),
            this.classesToTypes(location, thrownExceptions) // thrownExceptions
        );
        cd.addDeclaredMethod(md);

        // Add block as method body.
        Java.Block b = new Java.Block(location, (Java.Scope) md);
        md.setBody(b);

        return b;
    }

    /**
     * Wrap a reflection {@link Class} in a {@link Java.Type} object.
     */
    protected Java.Type classToType(
        Scanner.Location location,
        final Class      optionalClass
    ) {
        if (optionalClass == null) return null;

        return new Java.SimpleType(
            location,
            this.classLoaderIClassLoader.loadIClass(Descriptor.fromClassName(optionalClass.getName()))
        );
    }

    /**
     * Convert an array of {@link Class}es into an array of{@link Java.Type}s.
     */
    protected Java.Type[] classesToTypes(
        Scanner.Location location,
        Class[]          classes
    ) {
        Java.Type[] types = new Java.Type[classes.length];
        for (int i = 0; i < classes.length; ++i) {
            types[i] = this.classToType(location, classes[i]);
        }
        return types;
    }

    /**
     * Convert name and {@link Class}-base parameters into an array of
     * {@link Java.FormalParameter}s.
     */
    protected Java.FormalParameter[] makeFormalParameters(
        Scanner.Location location,
        String[]         parameterNames,
        Class[]          parameterTypes
    ) {
        Java.FormalParameter[] res = new Java.FormalParameter[parameterNames.length];
        for (int i = 0; i < res.length; ++i) {
            res[i] = new Java.FormalParameter(
                true,                                          // finaL
                this.classToType(location, parameterTypes[i]), // type
                parameterNames[i]                              // name
            );
        }
        return res;
    }

    /**
     * Compile the given compilation unit. (A "compilation unit" is typically the contents
     * of a Java<sup>TM</sup> source file.)
     * 
     * @param compilationUnit The parsed compilation unit
     * @param debuggingInformation What kind of debugging information to generate in the class file
     * @return The {@link ClassLoader} into which the compiled classes were defined
     * @throws Java.CompileException
     */
    protected ClassLoader compileAndLoad(
        Java.CompilationUnit compilationUnit,
        DebuggingInformation debuggingInformation
    ) throws Java.CompileException {
        if (EvaluatorBase.DEBUG) {
            UnparseVisitor.unparse(compilationUnit, new OutputStreamWriter(System.out));
        }

        // Compile compilation unit to class files.
        ClassFile[] classFiles = compilationUnit.compile(
            this.classLoaderIClassLoader, // iClassLoader
            debuggingInformation          // debuggingInformation
        );

        // Convert the class files to bytes and store them in a Map.
        Map classes = new HashMap(); // String className => byte[] data
        for (int i = 0; i < classFiles.length; ++i) {
            ClassFile cf = classFiles[i];
            classes.put(cf.getThisClassName(), cf.toByteArray());
        }

        // Create a ClassLoader that loads the generated classes.
        return new ByteArrayClassLoader(classes);
    }

    /**
     * Compile the given compilation unit, load all generated classes, and
     * return the class with the given name. 
     * @param compilationUnit
     * @param debuggingInformation TODO
     * @param newClassName The fully qualified class name
     * @return The loaded class
     * @throws Java.CompileException
     * @throws ClassNotFoundException A class with the given name was not declared in the compilation unit
     */
    protected Class compileAndLoad(
        Java.CompilationUnit compilationUnit,
        DebuggingInformation debuggingInformation,
        String               newClassName
    ) throws Java.CompileException, ClassNotFoundException {
        return this.compileAndLoad(compilationUnit, debuggingInformation).loadClass(newClassName);
    }

    private final ClassLoaderIClassLoader classLoaderIClassLoader;
}
