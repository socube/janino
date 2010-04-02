
/*
 * Janino - An embedded Java[TM] compiler
 *
 * Copyright (c) 2001-2010, Arno Unkrig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted provided that the
 * following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of conditions and the
 *       following disclaimer.
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the
 *       following disclaimer in the documentation and/or other materials provided with the distribution.
 *    3. The name of the author may not be used to endorse or promote products derived from this software without
 *       specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL
 * THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.codehaus.janino.tests;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.codehaus.janino.ExpressionEvaluator;
import org.codehaus.janino.Scanner;
import org.codehaus.janino.ScriptEvaluator;

public class EvaluatorTests extends TestCase {

    public static Test suite() {
        TestSuite s = new TestSuite(EvaluatorTests.class.getName());
//        s.addTest(new EvaluatorTests("testMultiScriptEvaluator"));
//        s.addTest(new EvaluatorTests("testExpressionEvaluator"));
//        s.addTest(new EvaluatorTests("testFastClassBodyEvaluator1"));
//        s.addTest(new EvaluatorTests("testFastClassBodyEvaluator2"));
//        s.addTest(new EvaluatorTests("testFastExpressionEvaluator"));
//        s.addTest(new EvaluatorTests("testManyEEs"));
        s.addTest(new EvaluatorTests("testGuessParameterNames"));
//        s.addTest(new EvaluatorTests("testAssertNotCooked"));
//        s.addTest(new EvaluatorTests("testAccessingCompilingClass"));
//        s.addTest(new EvaluatorTests("testProtectedAccessAcrossPackages"));
//
//        // The following three are known to fail because of JANINO-113:
////        s.addTest(new EvaluatorTests("testProtectedAccessWithinPackage"));
////        s.addTest(new EvaluatorTests("testComplicatedSyntheticAccess"));
////        s.addTest(new EvaluatorTests("testStaticInitAccessProtected"));
//        s.addTest(new EvaluatorTests("testDivByZero"));
//        s.addTest(new EvaluatorTests("test32kBranchLimit"));
//        s.addTest(new EvaluatorTests("test32kConstantPool"));
//        s.addTest(new EvaluatorTests("testHugeIntArray"));
//        s.addTest(new EvaluatorTests("testStaticFieldAccess"));
//        s.addTest(new EvaluatorTests("testWideInstructions"));
//        s.addTest(new EvaluatorTests("testHandlingNaN"));
//        s.addTest(new EvaluatorTests("testInstanceOf"));
//        s.addTest(new EvaluatorTests("testOverrideVisibility"));
//        s.addTest(new EvaluatorTests("testCovariantReturns"));
//        s.addTest(new EvaluatorTests("testNonExistentImport"));
//        s.addTest(new EvaluatorTests("testAnonymousFieldInitializedByCapture"));
//        s.addTest(new EvaluatorTests("testNamedFieldInitializedByCapture"));
//        s.addTest(new EvaluatorTests("testAbstractGrandParentsWithCovariantReturns"));
//        s.addTest(new EvaluatorTests("testStringBuilderLength"));
//        s.addTest(new EvaluatorTests("testBaseClassAccess"));
        return s;
    }

    public EvaluatorTests(String name) { super(name); }


    public void testGuessParameterNames() throws Exception {
        Set parameterNames = new HashSet(Arrays.asList(ExpressionEvaluator.guessParameterNames(new Scanner(null, new StringReader(
            "import o.p;\n" +
            "a + b.c + d.e() + f() + g.h.I.j() + k.l.M"
        )))));
        assertEquals(new HashSet(Arrays.asList(new String[] { "a", "b", "d" })), parameterNames);

        parameterNames = new HashSet(Arrays.asList(ScriptEvaluator.guessParameterNames(new Scanner(null, new StringReader(
            "import o.p;\n" +
            "int a;\n" +
            "return a + b.c + d.e() + f() + g.h.I.j() + k.l.M;"
        )))));
        assertEquals(new HashSet(Arrays.asList(new String[] { "b", "d" })), parameterNames);
    }

}
