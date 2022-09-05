/*
 * Copyright 2022 Elias Taufer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.loisel.chip.clc;

import java.util.*;

class Compiler {

    protected static final String[] COMPILER_KEYWORDS = { "include", "test" /* does nothing */ };
    protected static final String[] LANG_KEYWORDS = {
            "int",
            "void",
            "return",
            "while",
            "if",
            "else",
    };
    protected static final String[] FUN_TYPES = {
            "int", "void",
    };
    protected static final String[] VAR_TYPES = {   // string does yet have to be implemented
            "int", "string",
    };
    protected static final String[] LANG_SIGNS = {
            "{", "}", "[", "]", "(", ")",
            ",", ";",
            "+", "-", "*", "/",
            "==", "!=", "<=", ">=", "<", ">",
            "=",
    };
    protected static final String[] MATH_OPERATORS = {
            "+", "-", "*", "/",
    };
    protected static final String[] BOOL_OPERATORS = {
            "<", ">", "==", "!=",
    };

    private final List<Line> inLines;
    private List<Line> assembly;
    private List<Line> clcCode;
    private Map<String, List<String>> comCode;

    public Compiler(List<Line> inLines) {
        this.inLines = inLines;
    }

    public List<Line> compile() {
        message(inLines.size() + " words and signs to compile.");
        comCode = new HashMap<>();
        assembly = new ArrayList<>();

        // Syntax Check
        SyntaxCheck synChecker = new SyntaxCheck(inLines);
        clcCode = synChecker.checkSyntax();
        message("All files parsed successfully!");

        assembly = AssemblyGenerator.generateAssembly(clcCode);

        message("Compiled successfully!");
        return assembly;
    }

    private static void message(String msg) {
        System.out.println("Compiler: " + msg);
    }

}
