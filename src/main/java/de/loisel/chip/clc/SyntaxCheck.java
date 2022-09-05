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

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

class SyntaxCheck {

    private static final String RETURN = "return";

    List<Line> allLines;
    List<Line> clcCode;

    public SyntaxCheck(List<Line> lines) {
        this.allLines = lines;
    }

    /**
     * check for clc Syntax over all lines
     */
    List<Line> checkSyntax() {
        balancedParenthesis(allLines);

        int nextIndex = -1;
        for (int i = 0; i < allLines.size(); i++) {
            if (i < nextIndex)
                continue;


            Line line = allLines.get(i);

            switch (line.s) {
                case "int": {
                    if (allLines.size() <= i + 4)
                        throw new SyntaxErrorException(line, "Statement not complete");

                    else if (allLines.get(i + 1).s.equals("[")
                            && allLines.get(i + 2).s.equals("]")) {     // array declaration
                        nextIndex = i + checkSynArrDec(i);
                    } else if (allLines.get(i + 2).s.equals("=")) {        // variable declaration
                        nextIndex = i + checkSynVarDec(allLines, i);
                    } else if (allLines.get(i + 2).s.equals("(")) {        // function declaration
                        nextIndex = i + checkSynFunDec(i);
                    }
                    break;
                }

                case "void": {
                    nextIndex = i + checkSynFunDec(i);
                    break;
                }

                default: {
                    if (line.s.charAt(0) == '#') {
                        checkSynComStat(line);
                        nextIndex = i;
                    } else
                        throw new SyntaxErrorException(line, "Unexpected expression: \"" + line.s + "\"");
                }
            }

        }

        return this.clcCode;
    }

    /**
     * check syntax of function declaration
     *
     * @param index of declaration
     * @return lines to skip
     */
    private int checkSynFunDec(int index) {
        int countLines = 0;

        if (!Arrays.asList(Compiler.FUN_TYPES).contains(allLines.get(index).s))               // wrong return type
            throw new SyntaxErrorException(allLines.get(index),
                    "Unknown type \"" + allLines.get(index).s + "\" for function.");
        else if (!isVariableName(allLines.get(index + 1).s))                        // not a valid name
            throw new SyntaxErrorException(allLines.get(index),
                    "Not a valid function name: \"" + allLines.get(index + 1).s + "\".");
        else if (!allLines.get(index + 2).s.equals("("))                             // missing '(' bracket for arguments
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected \"(\" for function declaration, got: \"" + allLines.get(index + 1).s + "\".");

        // find ')'
        int roundClosingIndex = -1;
        for (int i = index + 3; i < allLines.size(); i++) {
            if (allLines.get(i).s.equals(")")) {
                roundClosingIndex = i;
                break;
            }
        }
        if (roundClosingIndex == -1)                                                 // no ')' was found
            throw new SyntaxErrorException(allLines.get(index), "Missing ')' for function definition");
        countLines += roundClosingIndex - index + 1;

        // if true, there are arguments
        if (roundClosingIndex > index + 3) {
            List<Line> subList = allLines.subList(index + 3, roundClosingIndex);

            if (!Arrays.asList(Compiler.VAR_TYPES).contains(subList.get(0).s))
                throw new SyntaxErrorException(allLines.get(index),
                        "Expected variable type, got: \"" + subList.get(0).s + "\"");

            String lastThing = "COM";
            for (Line line : subList) {
                String val = line.s;

                switch (lastThing) {
                    case "TYPE": {
                        if (!isVariableName(val))
                            throw new SyntaxErrorException(allLines.get(index),
                                    "Expected variable name, got: \"" + line.s + "\"");
                        lastThing = "NAME";
                        break;
                    }
                    case "NAME": {
                        if (!val.equals(","))
                            throw new SyntaxErrorException(allLines.get(index),
                                    "Expected ')' or ',' after variable name, got: \"" + line.s + "\"");
                        lastThing = "COM";
                        break;
                    }
                    case "COM": {
                        if (!Arrays.asList(Compiler.VAR_TYPES).contains(val))
                            throw new SyntaxErrorException(allLines.get(index),
                                    "Expected variable type after ',', got: \"" + line.s + "\"");
                        lastThing = "TYPE";
                        break;
                    }
                    default: {
                        break;
                    }
                }

            }
        }


        if (allLines.size() > roundClosingIndex + 1 && !allLines.get(roundClosingIndex + 1).s.equals("{"))
            throw new SyntaxErrorException(allLines.get(index), "Expected \"{\" after function definition");

        countLines += checkSynCodeBlock(roundClosingIndex + 1);

        return countLines;
    }

    /**
     * check syntax for code block
     *
     * @param index start of the code-block ('{')
     * @return lines to skip
     */
    private int checkSynCodeBlock(int index) {
        int countLines;

        // find '}'
        int closeIndex = 0;
        Deque<Character> stack = new ArrayDeque<>();
        stack.push('{');
        for (int i = index + 1; i < allLines.size(); i++) {
            if (allLines.get(i).s.equals("{"))
                stack.push('{');
            else if (allLines.get(i).s.equals("}"))
                stack.pop();
            if (stack.isEmpty()) {
                closeIndex = i;
                break;
            }
        }
        if(closeIndex == 0 || !allLines.get(index).absPath.equals(allLines.get(closeIndex).absPath)) {
            throw new SyntaxErrorException(allLines.get(index), "Code-block was not closed");
        }

        countLines = closeIndex + 1 - index;

        // check content of Code block
        int nextIndex = -1;
        for (int i = index + 1; i < closeIndex; i++) {
            if (i < nextIndex)
                continue;

            String val = allLines.get(i).s;

            if ("while".equals(val)) {                           // while loop
                nextIndex = i + checkSynWhileLoop(allLines, i);
            } else if (RETURN.equals(val)) {                   // return statement
                nextIndex = i + checkSynReturn(allLines, i);
            } else if ("if".equals(val)) {                       // if condition
                nextIndex = i + checkSynIfCond(allLines, i);
            } else if (                                          // array declaration
                    Arrays.asList(Compiler.VAR_TYPES).contains(val)
                            && allLines.size() > i + 1
                            && allLines.get(i + 1).s.equals("[")
            ) {
                nextIndex = i + checkSynArrDec(i);
            } else if (Arrays.asList(Compiler.VAR_TYPES).contains(val)) { // variable declaration
                nextIndex = i + checkSynVarDec(allLines, i);
            } else if (                                          // function call
                    isVariableName(val)
                            && allLines.size() > i + 1
                            && allLines.get(i + 1).s.equals("(")
            ) {
                nextIndex = i + checkSynLineFunCall(allLines, i);
            } else if (                                          // variable assignment
                    isVariableName(val)
                            && allLines.size() > i + 1
                            && allLines.get(i + 1).s.equals("=")
            ) {
                nextIndex = i + checkSynVarAssign(allLines, i);
            } else if (                                          // array assignment
                    isVariableName(val)
                            && allLines.size() > i + 1
                            && allLines.get(i + 1).s.equals("[")
            ) {
                nextIndex = i + checkSynArrAssign(allLines, i);
            } else {
                throw new SyntaxErrorException(allLines.get(i), "Cannot parse statement: \"" + val + "\"");
            }
        }

        return countLines;
    }

    /**
     * check syntax of an array assignment <br>
     * e.g.: "arrName[2] = 5;"
     *
     * @param index of array name
     * @return lines to skip
     */
    private int checkSynArrAssign(List<Line> allLines, int index) {
        int countLines = 0;

        // always

        if(allLines.size() < index + 7                  // not long enough
                || !allLines.get(index).absPath.equals(allLines.get(index + 7).absPath)
        ) {
            throw new SyntaxErrorException(allLines.get(index), "Not a complete assignment");
        }

        if (!isVariableName(allLines.get(index).s)) {   // wrong identifier
            throw new SyntaxErrorException(allLines.get(index), "Not a variable name");
        }
        if(!allLines.get(index + 1).s.equals("[")) {    // missing '['
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected [, got :\"" + allLines.get(index + 1).s + "\".");
        }

        countLines += checkSynArrAccess(allLines, index);

        countLines += checkAssignMath(allLines, index + countLines + 1); // +1 for the '='

        countLines++; // for ';'

        return countLines;
    }

    /**
     * Check the access of an array <br>
     * e.g.: "arrName[i + 1]"
     * @param index Index of array name in allLines
     * @return lines to skip
     */
    private int checkSynArrAccess(List<Line> allLines, int index) {
        int countLines = 0;

        if(!isVariableName(allLines.get(index).s))
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected array name instead got: \"" + allLines.get(index).s + "\"."
                    );
        if(index >= allLines.size() || !allLines.get(index + 1).s.equals("["))
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected \"[\" for array access, got:" + allLines.get(index).s + "\".");

        // check for closing ']'
        Deque<Character> stack = new ArrayDeque<>();
        int closeIndex = -1;
        for (int i = index + 1; i < allLines.size(); i++) {
            if (allLines.get(i).s.equals("[")) {
                stack.push('[');
            } else if (allLines.get(i).s.equals("]")) {
                stack.pop();
                if(stack.isEmpty()) {
                    closeIndex = i;
                    break;
                }
            }
        }

        if (closeIndex < 0 || !allLines.get(index).absPath.equals(allLines.get(closeIndex).absPath)) {
            throw new SyntaxErrorException(allLines.get(index), "No closing ']' found for array access.");
        }

        countLines += closeIndex - index + 1;

        checkMathExp(index + 2, closeIndex);

        return countLines;
    }

    /**
     * check math expression for assignment <br>
     * Always ends with ;
     * @param index Index where expression begins
     * @return lines to skip
     */
    private int checkAssignMath(List<Line> allLines, int index) {
        int countLines;

        int closeIndex = -1;
        for (int i = index; i < allLines.size(); i++) {
            if (allLines.get(i).s.equals(";")) {
                closeIndex = i;
                break;
            }
        }
        if (closeIndex == -1)
            throw new SyntaxErrorException(allLines.get(index), "No ';' after variable assignment found");
        countLines = closeIndex - index + 1;

        checkMathExp(index, closeIndex);

        return  countLines;
    }

    /**
     * check syntax of a mathematical expression <br>
     * e.g.: 5 + 2
     *
     * @param start where the expression begins. inclusive
     * @param end where the expression ends. exclusive
     */
    private void checkMathExp(int start, int end) {
        if(end <= start)
            throw new SyntaxErrorException(allLines.get(start), "No mathematical expression found");

        {
            List<Line> lines = allLines.subList(start, end);
            if (lines.isEmpty())
                throw new SyntaxErrorException(new Line("", "ERR", -1),
                        "Critical Error in checkMathExp. Empty list. No mathematical expression was found. This Could be a internal error"
                );

            balancedParenthesis(lines);
        }

        if (allLines.get(start).s.equals("*") || allLines.get(start).s.equals("/") || allLines.get(start).s.equals("["))
            throw new SyntaxErrorException(allLines.get(start), "Unexpected \"" + allLines.get(start).s + "\"");

        String last = "START";

        int nextIndex = -1;
        for (int i = start; i < end; i++) {
            if(i < nextIndex)
                continue;

            Line line = allLines.get(i);
            boolean hasNext = i + 1  < allLines.size();

            switch (last) {
                case "START": {
                if (
                           line.s.equals("*")
                        || line.s.equals("/")
                        || line.s.equals("[")
                        || Arrays.asList(Compiler.BOOL_OPERATORS).contains(line.s)
                )
                        throw new SyntaxErrorException(line, "Unexpected \"" + line.s + "\"");
                    if (line.s.equals("(")) {
                        // start new checkMathExp recursively
                        nextIndex = i + checkSynMathExpInBrackets(i);
                        last = "VAL";
                    }
                    else if (isVariableName(line.s) && hasNext && allLines.get(i + 1).s.equals("(")) {
                        nextIndex = i + checkSynInFunCall(allLines, i);
                        last = "VAL";
                    }
                    else if (isVariableName(line.s) && hasNext && allLines.get(i + 1).s.equals("[")) {
                        nextIndex = i + checkSynArrAccess(allLines, i);
                        last = "VAL";
                    }
                    else if (isVariableName(line.s) || isNum(line.s))
                        last = "VAL";
                    else if (line.s.equals("-") || line.s.equals("+"))
                        last = "SIGN";
                    else
                        throw new SyntaxErrorException(line,
                                "Unexpected sign at begin of math. expression: \"" + line.s + "\".");
                    break;
                }
                case "VAL": {
                    if (Arrays.asList(Compiler.MATH_OPERATORS).contains(line.s)
                            || Arrays.asList(Compiler.BOOL_OPERATORS).contains(line.s)) {
                        last = "SIGN";
                    }
                    else
                        throw new SyntaxErrorException(line, "After value got unexpected: " + line.s + "\".");
                    break;
                }
                case "SIGN": {
                    if (line.s.equals("(")) {
                        // start new checkMathExp recursively
                        nextIndex = i + checkSynMathExpInBrackets(i);
                        last = "VAL";
                    }
                    else if (isVariableName(line.s) && hasNext && allLines.get(i + 1).s.equals("(")) {
                        nextIndex = i + checkSynInFunCall(allLines, i);
                        last = "VAL";
                    }
                    else if (isVariableName(line.s) && hasNext && allLines.get(i + 1).s.equals("[")) {
                        nextIndex = i + checkSynArrAccess(allLines, i);
                        last = "VAL";
                    }
                    else if (isVariableName(line.s) || isNum(line.s))
                        last = "VAL";
                    else
                        throw new SyntaxErrorException(line, "After operator got unexpected: " + line.s + "\".");
                    break;
                }
                default:
                    break;
            }
        }

        if(last.equals("SIGN")) {
            throw new SyntaxErrorException(allLines.get(end - 1),
                    "\"" + allLines.get(end - 1).s + "\" cannot stand alone.");
        }
    }

    /**
     * Check mathematical expression that is in "()" brackets
     * @param index Index where the "(" is located
     * @return lines to skip
     */
    private int checkSynMathExpInBrackets(int index) {

        // find closing ')'
        Deque<Character> stack = new ArrayDeque<>();
        int closeIndex = -1;
        for (int i = index; i < allLines.size(); i++) {
            if(allLines.get(i).s.equals("("))
                stack.push('(');
            else if(allLines.get(i).s.equals(")")) {
                stack.pop();
                if(stack.isEmpty()) {
                    closeIndex = i;
                    break;
                }
            }
        }
        if (closeIndex < 0 || !allLines.get(index).absPath.equals(allLines.get(closeIndex).absPath)) {
            throw new SyntaxErrorException(allLines.get(index), "No closing ')' found for function call.");
        }
        checkMathExp(index + 1, closeIndex);
        return closeIndex - index + 1;
    }

    /**
     * check syntax of a variable assignment<br>
     * e.g.: "testVar = 5 + otherTestVar;"
     * @param index of variable name
     * @return lines to skip
     */
    private int checkSynVarAssign(List<Line> allLines, int index) {
        int countLines;

        if(!isVariableName(allLines.get(index).s)) {
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected variable name, got: \"" + allLines.get(index).s + "\"");
        }
        if(!allLines.get(index + 1).s.equals("=")) {
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected \"=\" for assignment, got: \"" + allLines.get(index).s + "\"");
        }

        // check for closing ';'
        int closeIndex = -1;
        for (int i = index + 3; i < allLines.size(); i++) {
            if (allLines.get(i).s.equals(";")) {
                closeIndex = i;
                break;
            }
        }
        if (closeIndex < 0 || !allLines.get(index).absPath.equals(allLines.get(closeIndex).absPath)) {
            throw new SyntaxErrorException(allLines.get(index), "No closing ';' found for assignment.");
        }

        countLines = closeIndex - index + 1;

        checkMathExp(index + 2, closeIndex);

        return countLines;
    }

    /**
     * Check for a function call in e.g. mathematical expressions
     * @param index Index where the function name is in allLines
     * @return how many lines the function call goes
     */
    private int checkSynInFunCall(List<Line> allLines, int index) {
        int countLines = 0;

        if(!isVariableName(allLines.get(index).s))
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected function name, got:\"" + allLines.get(index).s + "\".");
        if(allLines.size() <= index + 1 && !allLines.get(index + 1).s.equals("("))
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected \"(\" bracket at function call, got:\"" + allLines.get(index).s + "\".");

        // find closing ')'
        Deque<Character> stack = new ArrayDeque<>();
        int closeIndex = -1;
        for (int i = index + 1; i < allLines.size(); i++) {
            if(allLines.get(i).s.equals("("))
                stack.push('(');
            else if(allLines.get(i).s.equals(")")) {
                stack.pop();
                if(stack.isEmpty()) {
                    closeIndex = i;
                    break;
                }
            }
        }
        if (closeIndex < 0 || !allLines.get(index).absPath.equals(allLines.get(closeIndex).absPath)) {
            throw new SyntaxErrorException(allLines.get(index), "No closing ')' found for function call.");
        }

        countLines += closeIndex - index + 1;

        int startBracket = index + 2;
        int endBracket = closeIndex - 1;
        if(startBracket < endBracket)
            checkArgumentInList(startBracket, endBracket);

        return countLines;
    }

    /**
     * check syntax of a function call that stands alone in a line <br>
     * e.g.: "testFun(3);"
     * @param index of function call
     * @return lines to skip
     */
    private int checkSynLineFunCall(List<Line> allLines, int index) {
        int countLines;

        if(!isVariableName(allLines.get(index).s)) {
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected function name, got: \"" + allLines.get(index).s + "\"");
        }
        if(!allLines.get(index + 1).s.equals("(")) {
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected \"(\" for function call, got: \"" + allLines.get(index).s + "\"");
        }

        // check for closing ';'
        int closeIndex = -1;
        for (int i = index + 6; i < allLines.size(); i++) {
            if (allLines.get(i).s.equals(";")) {
                closeIndex = i;
                break;
            }
        }

        if (closeIndex < 0 || !allLines.get(index).absPath.equals(allLines.get(closeIndex).absPath)) {
            throw new SyntaxErrorException(allLines.get(index), "No closing ';' found for function call.");
        }
        if(allLines.size() < closeIndex + 1 || !allLines.get(closeIndex).s.equals(";")) {
            throw new SyntaxErrorException(allLines.get(index), "No closing ';' found.");
        }
        if(!allLines.get(closeIndex - 1).s.equals(")")) {
            throw new SyntaxErrorException(allLines.get(index), "No closing ')' found.");
        }

        countLines = checkSynInFunCall(allLines, index) + 1;

        return countLines;
    }

    /**
     * check syntax of a return statement
     *
     * @param index of if
     * @return lines to skip
     */
    private int checkSynReturn(List<Line> allLines, int index) {
        int countLines = 2;

        if(!allLines.get(index).s.equals(RETURN)) {
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected \"return\", got: \"" + allLines.get(index).s + "\".");
        }

        if(!allLines.get(index + 1).s.equals(RETURN)) {       // got a return value
            countLines += checkAssignMath(allLines, index + 1);
        }

        return countLines;
    }

    /**
     * check syntax of an if condition
     *
     * @param index Index of if in allLines
     * @return lines to skip
     */
    private int checkSynIfCond(List<Line> allLines, int index) {
        int countLines = 0;

        if(!allLines.get(index).s.equals("if"))
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected \"if\" at start of if-condition, got: \"" + allLines.get(index).s + "\".");
        countLines ++;

        if(!allLines.get(index + 1).s.equals("("))
            throw new SyntaxErrorException(allLines.get(index + 1),
                    "Expected \"(\" for the condition if, got: \"" + allLines.get(index + 1).s + "\".");

        countLines += checkSynMathExpInBrackets(index + 1);

        if(!allLines.get(index + countLines).s.equals("{"))
            throw new SyntaxErrorException(allLines.get(index + countLines),
                    "Expected \"{\" for the code block after if(), got: \"" + allLines.get(index + countLines).s + "\".");

        countLines += checkSynCodeBlock(index + countLines);

        int nextIndex = -1;
        for (int i = index + countLines; i < allLines.size(); i++) {
            if(i < nextIndex)
                continue;

            if(i + 1 < allLines.size() && allLines.get(i).s.equals("else") && allLines.get(i + 1).s.equals("if")) {
                countLines += 2;

                if(!allLines.get(index + countLines).s.equals("("))
                    throw new SyntaxErrorException(allLines.get(index + countLines),
                            "Expected \"(\" after else if, got: \"" + allLines.get(index + countLines).s + "\".");

                countLines += checkSynMathExpInBrackets(index + countLines);

                if(!allLines.get(index + countLines).s.equals("{"))
                    throw new SyntaxErrorException(allLines.get(index + countLines),
                            "Expected \"{\" for the code block after else if(), got: \"" + allLines.get(index + countLines).s + "\".");

                countLines += checkSynCodeBlock(index + countLines);

                nextIndex = index + countLines;
            } else if (i + 1 < allLines.size() && allLines.get(i).s.equals("else")) {
                countLines ++;

                if(!allLines.get(index + countLines).s.equals("{"))
                    throw new SyntaxErrorException(allLines.get(index + countLines),
                            "Expected \"{\" for the code block after else, got: \"" + allLines.get(index + countLines).s + "\".");

                countLines += checkSynCodeBlock(index + countLines);

                nextIndex = index + countLines;
            } else {
                nextIndex = allLines.size(); // no "else if" or "else" found
            }
        }

        return countLines;
    }

    /**
     * check syntax of a while loop
     *
     * @param index Index of while in allLines
     * @return lines to skip
     */
    private int checkSynWhileLoop(List<Line> allLines, int index) {
        int countLines = 0;

        if(!allLines.get(index).s.equals("while"))
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected \"while\" at start of loop, got: \"" + allLines.get(index).s + "\".");
        countLines ++;

        if(!allLines.get(index + 1).s.equals("("))
            throw new SyntaxErrorException(allLines.get(index + 1),
                    "Expected \"(\" for the condition at while loop, got: \"" + allLines.get(index + 1).s + "\".");

        countLines += checkSynMathExpInBrackets(index + 1);

        if(!allLines.get(index + countLines).s.equals("{"))
            throw new SyntaxErrorException(allLines.get(index + countLines),
                    "Expected \"{\" for the code block after while(), got: \"" + allLines.get(index + countLines).s + "\".");

        countLines += checkSynCodeBlock(index + countLines);

        return countLines;
    }

    /**
     * check syntax of variable declaration
     *
     * @param index of declaration
     * @return lines to skip
     */
    private int checkSynVarDec(List<Line> allLines, int index) {
        int countLines;

        if (!isVariableName(allLines.get(index + 1).s)) {
            throw new SyntaxErrorException(allLines.get(index + 3), "'"
                    + allLines.get(index + 1).s + "' is not a variable name.");
        } else if (!allLines.get(index + 2).s.equals("=")) {
            throw new SyntaxErrorException(allLines.get(index + 2), "Expected: '=' got: '"
                    + allLines.get(index + 2).s + "'.");
        }

        countLines = 3 + checkAssignMath(allLines, index + 3);

        return countLines;
    }

    /**
     * check syntax of an array declaration
     *
     * @param index of declaration
     * @return lines to skip
     */
    private int checkSynArrDec(int index) {
        int countLines;
        String file = allLines.get(index).absPath;

        String varName = allLines.get(index + 3).s;


        if (!isVariableName(varName)) {
            throw new SyntaxErrorException(allLines.get(index + 3), "'" + varName + "' is not a variable name.");
        } else if (!allLines.get(index + 4).s.equals("=")) {
            throw new SyntaxErrorException(allLines.get(index + 4), "Expected: '=' got: '"
                    + allLines.get(index + 4).s + "'.");
        } else if (!allLines.get(index + 5).s.equals("{") && !allLines.get(index + 5).s.equals("[")) {
            throw new SyntaxErrorException(allLines.get(index + 3), "Expected: '{' or '[' after '='.");
        }

        if (allLines.get(index + 5).s.equals("[")) {             // check if : "int name = [3];"

            if (allLines.size() <= index + 8 || !file.equals(allLines.get(index + 8).absPath)) {
                throw new SyntaxErrorException(allLines.get(index + 6),
                        "Statement not complete.");
            } else if (!isNum(allLines.get(index + 6).s)) {
                throw new SyntaxErrorException(allLines.get(index + 6),
                        "Expected number got: '" + allLines.get(index + 6).s + "'");
            } else if (!allLines.get(index + 7).s.equals("]")) {
                throw new SyntaxErrorException(allLines.get(index + 7),
                        "Expected ']' got: '" + allLines.get(index + 7).s + "'");
            } else if (!allLines.get(index + 8).s.equals(";")) {
                throw new SyntaxErrorException(allLines.get(index + 8),
                        "Expected ';' got: '" + allLines.get(index + 8).s + "'");
            }
            countLines = 9;


        }
                                                                    // check if: "int name = { 3, ..., 1 };"
        else {

            // check for closing '}'
            int closeIndex = -1;
            for (int i = index + 6; i < allLines.size(); i++) {
                if (allLines.get(i).s.equals("}")) {
                    closeIndex = i;
                    break;
                }
            }

            if (closeIndex < 0 || !allLines.get(index).absPath.equals(allLines.get(closeIndex).absPath)) {
                throw new SyntaxErrorException(allLines.get(index), "No closing '}' found.");
            } else if ((closeIndex - index) < 1) {
                throw new SyntaxErrorException(allLines.get(index), "Array declaration values missing.");
            }

            checkArgumentInList(index + 6, closeIndex);

            if (!allLines.get(closeIndex + 1).s.equals(";")) {
                throw new SyntaxErrorException(allLines.get(closeIndex),
                        "Expected ';' got: '" + allLines.get(closeIndex).s + "'");
            }
            countLines = closeIndex - index + 2;
        }

        return countLines;
    }

    /**
     * Checks a list of arguments seperated by ',' <br>
     * e.g.: "arg1, 5 + 6, 233"
     * @param start where the expression begins. inclusive
     * @param end where the expression ends. exclusive
     */
    private void checkArgumentInList(int start, int end) {
        if(end <= start)
            throw new SyntaxErrorException(allLines.get(start), "No arguments found");

        // now we have to check for arguments
        // it is a little more complicated
        // at first we have to check that all brackets are matching.
        // If all are closed we can look for a ','.
        // If not, either we hit the end or the argument continues.
        // When we found a whole argument we send it to check for mathematical expression
        // After that we have to check the next argument.

        {
            List<Line> arguments = allLines.subList(start, end);
            balancedParenthesis(arguments);
        }

        Deque<Character> stack = new ArrayDeque<>();
        int nextArgIndex = start;
        for(int i = nextArgIndex; i < end; i++) {

            if(allLines.get(i).s.equals("(") || allLines.get(i).s.equals("["))
                stack.push(allLines.get(i).s.charAt(0));
            else if(allLines.get(i).s.equals(")") || allLines.get(i).s.equals("]"))
                stack.pop();
            else if(stack.isEmpty() && allLines.get(i).s.equals(",")) {
                checkMathExp(nextArgIndex, i);
                nextArgIndex = i + 1;
            }

        }
    }

    /**
     * Check if brackets are matching.
     * Supported brackets are: {}[]()
     */
    private void balancedParenthesis(List<Line> lines) {

        Deque<Character> stack = new ArrayDeque<>();

        Line oldLine = null;
        for (Line line : lines) {

            if (oldLine != null
                    && !oldLine.fName.equals(line.fName)
                    && !stack.isEmpty()) {
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
                    throwUnexpectedBracket(line);
                else {
                    char check;
                    switch (x) {
                        case ')':
                            check = stack.pop();
                            if (check == '{' || check == '[')
                                throwUnexpectedBracket(line);
                            break;

                        case '}':
                            check = stack.pop();
                            if (check == '(' || check == '[')
                                throwUnexpectedBracket(line);
                            break;

                        case ']':
                            check = stack.pop();
                            if (check == '(' || check == '{')
                                throwUnexpectedBracket(line);
                            break;
                        default:
                            // skip unimportant characters
                            break;
                    }
                }
            }
        }

        if (!stack.isEmpty()) {
            Line last = lines.get(lines.size() - 1);
            throw new CompilerParseException(
                    last.num, "'" + stack.pop() + "' was not closed!", last.fName
            );
        }
    }

    // check syntax of Compiler statement (Statements starting with '#')
    private void checkSynComStat(Line line) {
        String statement = line.s.substring(1);
        String command = statement.split(" ")[0];

        if (Arrays.asList(Compiler.COMPILER_KEYWORDS).contains(command)) {

            if (command.equals(Compiler.COMPILER_KEYWORDS[0])) { // #include

                if (statement.split(" ").length == 2) {

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

                } else if (statement.split(" ").length < 2) {
                    throw new SyntaxErrorException(line, "Compiler statement \"" + statement + "\" missing arguments");
                } else {
                    throw new SyntaxErrorException(line, "Compiler statement \"" + statement + "\" to many arguments");
                }

            }

        } else {
            throw new SyntaxErrorException(line, "Compiler command \"#" + command + "\" not found");
        }

    }



    /*
        ===========================
        =========  TOOLS  =========
        ===========================
     */

    private void throwUnexpectedBracket(Line line) {
        throw new CompilerParseException(line.num, "Unexpected: " + line.s, line.fName);
    }

    private boolean isNum(String number) {
        return number.matches("^\\d*$");
    }

    private boolean isVariableName(String name) {
        name = name.strip();
        return name.matches("^[a-zA-Z_$][a-zA-Z_$\\d]*$");
    }
}
