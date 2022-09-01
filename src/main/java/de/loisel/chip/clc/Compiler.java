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

    private final static String[] COMPILER_KEYWORDS = { "include", "test" /* does nothing */ };

    private final List<Line> inLines;
    private List<String> assembly;
    private Map<String, List<String>> comCode;

    public Compiler(List<Line> inLines) {
        this.inLines = inLines;
    }

    public List<String> compile() {
        comCode = new HashMap<>();
        assembly = new ArrayList<>();

        parseAll(inLines);

        message("Compiled successfully!");
        return assembly;
    }

    private static void parseAll(List<Line> lines) {

        balancedParenthesis(lines);

        checkSyntax(lines);

        message("All files parsed successfully!");
    }

    private static void message(String msg) {
        System.out.println("Compiler: " + msg);
    }

    private static void balancedParenthesis(List<Line> lines) {

        Deque<Character> stack = new ArrayDeque<>();

        Line oldLine = null;
        for (Line line : lines) {

            if (oldLine != null
                    && !oldLine.fName.equals(line.fName)
                    && !stack.isEmpty())
            {
                    throw new CompilerParseException(
                            oldLine.num, "'" + stack.pop() + "' was not closed!", oldLine.fName
                    );
            }
            oldLine = line;

            // every character
            for (int i = 0; i < line.length(); i++) {
                char x = line.charAt(i);

                if (!"{[()]}".contains(Character.toString(x))) {
                    // not a bracket
                    continue;
                }

                if (x == '(' || x == '[' || x == '{') {
                    // Push the element in the stack
                    stack.push(x);
                }
                // If current character is not opening
                // bracket, then it must be closing. So stack
                // cannot be empty at this point.
                else if (stack.isEmpty())
                    throw new CompilerParseException(line.num, "Unexpected " + x, line.fName);
                else {
                    char check;
                switch (x) {
                    case ')':
                        check = stack.pop();
                        if (check == '{' || check == '[')
                            throw new CompilerParseException(line.num, "Unexpected " + x, line.fName);
                        break;

                    case '}':
                        check = stack.pop();
                        if (check == '(' || check == '[')
                            throw new CompilerParseException(line.num, "Unexpected " + x, line.fName);
                        break;

                    case ']':
                        check = stack.pop();
                        if (check == '(' || check == '{')
                            throw new CompilerParseException(line.num, "Unexpected " + x, line.fName);
                        break;
                    default:
                        // skip unimportant characters
                        break;
                }
                }
            }
        }

        if(!stack.isEmpty()) {
            Line last = lines.get(lines.size() - 1);
            throw new CompilerParseException(
                    last.num, "'" + stack.pop() + "' was not closed!", last.fName
            );
        }
    }

    private static void checkSyntax(List<Line> allLines) {



    }

    private static void checkCompilerStatement(Line line) {
        String statement = line.s.substring(1);
        String command = statement.split(" ")[0];

        if(Arrays.asList(COMPILER_KEYWORDS).contains(command)) {

            if(command.equals(COMPILER_KEYWORDS[0])) { // #include

                if(statement.split(" ").length == 2) {

                    String argument = statement.split(" ")[1];
                    if (
                            argument.charAt(0) != '"'
                            || argument.charAt(argument.length() - 1) != '"'
                            || argument.length() < 3
                    ) {
                        throw new SyntaxErrorException(
                                line,
                                "Cannot read expression: " + argument + "."
                        );
                    }

                } else if(statement.split(" ").length < 2) {
                    throw new SyntaxErrorException(line, "Compiler statement \"" + statement + "\" missing arguments");
                } else {
                    throw new SyntaxErrorException(line, "Compiler statement \"" + statement + "\" to many arguments");
                }

            } else if(command.equals(COMPILER_KEYWORDS[1])) { // #test

            }

        } else {
            throw new SyntaxErrorException(line, "Compiler command \"#" + command + "\" not found");
        }

    }
}
