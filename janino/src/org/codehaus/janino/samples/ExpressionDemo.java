
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

package org.codehaus.janino.samples;

import org.codehaus.janino.*;

/**
 * A test program that allows you to play around with the
 * {@link org.codehaus.janino.ExpressionEvaluator ExpressionEvaluator} class.
 */

public class ExpressionDemo extends DemoBase {
    public static void main(String[] args) throws Exception {
        String   expression             = "total >= 100.0 ? 0.0 : 7.95";
        Class    optionalExpressionType = null;
        String[] parameterNames         = { "total", };
        Class[]  parameterTypes         = { Double.TYPE, };
        Class[]  thrownExceptions       = new Class[0];

        int i;
        for (i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (!arg.startsWith("-")) break;
            if (arg.equals("-e")) {
                expression = args[++i];
            } else
            if (arg.equals("-et")) {
                optionalExpressionType = DemoBase.stringToType(args[++i]);
            } else
            if (arg.equals("-pn")) {
                parameterNames = DemoBase.explode(args[++i]);
            } else
            if (arg.equals("-pt")) {
                parameterTypes = DemoBase.stringToTypes(args[++i]);
            } else
            if (arg.equals("-te")) {
                thrownExceptions = DemoBase.stringToTypes(args[++i]);
            } else
            if (arg.equals("-help")) {
                usage();
                System.exit(0);
            } else
            {
                System.err.println("Invalid command line option \"" + arg + "\".");
                usage();
                System.exit(0);
            }
        }

        if (parameterTypes.length != parameterNames.length) {
            System.err.println("Parameter type count and parameter name count do not match.");
            usage();
            System.exit(1);
        }

        // One command line argument for each parameter.
        if (args.length - i != parameterNames.length) {
            System.err.println("Parameter value count and parameter name count do not match.");
            usage();
            System.exit(1);
        }

        // Convert command line arguments to parameter values.
        Object[] parameterValues = new Object[parameterNames.length];
        for (int j = 0; j < parameterNames.length; ++j) {
            parameterValues[j] = DemoBase.createObject(parameterTypes[j], args[i + j]);
        }

        // Create "ExpressionEvaluator" object.
        ExpressionEvaluator ee = new ExpressionEvaluator(
            expression,
            optionalExpressionType,
            parameterNames,
            parameterTypes,
            thrownExceptions,
            null              // optionalClassLoader
        );

        // Evaluate expression with actual parameter values.
        Object res = ee.evaluate(parameterValues);

        // Print expression result.
        System.out.println("Result = " + DemoBase.toString(res));
    }

    private ExpressionDemo() {}

    private static void usage() {
        System.err.println("Usage:  ExpressionDemo { <option> } { <parameter-value> }");
        System.err.println("Valid options are");
        System.err.println(" -e <expression>");
        System.err.println(" -et <expression-type>");
        System.err.println(" -pn <comma-separated-parameter-names>");
        System.err.println(" -pt <comma-separated-parameter-types>");
        System.err.println(" -te <comma-separated-thrown-exception-types>");
        System.err.println(" -help");
        System.err.println("The number of parameter names, types and values must be identical.");
    }
}
