package de.loisel.chip.clc;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

interface SyntaxCheck {

    String RETURN = "return";

    /**
     * check for clc Syntax over all lines
     */
    static void checkSyntax(List<Line> allLines) {
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
                        nextIndex = i + checkSynArrDec(allLines, i);
                    } else if (allLines.get(i + 2).s.equals("=")) {        // variable declaration
                        nextIndex = i + checkSynVarDec(allLines, i);
                    } else if (allLines.get(i + 2).s.equals("(")) {        // function declaration
                        nextIndex = i + checkSynFunDec(allLines, i);
                    }
                    break;
                }

                case "void": {
                    nextIndex = i + checkSynFunDec(allLines, i);
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

    }

    /**
     * check syntax of function declaration
     *
     * @param index of declaration
     * @return lines to skip
     */
    private static int checkSynFunDec(List<Line> allLines, int index) {
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

        countLines += checkSynCodeBlock(allLines, roundClosingIndex + 1);

        return countLines;
    }

    /**
     * check syntax for code block
     *
     * @param index start of the code-block ('{')
     * @return lines to skip
     */
    static int checkSynCodeBlock(List<Line> allLines, int index) {
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
                nextIndex = i + checkSynArrDec(allLines, i);
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
    static int checkSynArrAssign(List<Line> allLines, int index) {
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
    static int checkSynArrAccess(List<Line> allLines, int index) {
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

        checkMathExp(allLines.subList(index + 2, closeIndex));

        return countLines;
    }

    /**
     * check math expression for assignment <br>
     * Always ends with ;
     * @param index Index where expression begins
     * @return lines to skip
     */
    static int checkAssignMath(List<Line> allLines, int index) {
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

        List<Line> subList = allLines.subList(index, closeIndex);

        if(subList.isEmpty())
            throw new SyntaxErrorException(allLines.get(index), "No value found");

        checkMathExp(subList);

        return  countLines;
    }

    /**
     * check syntax of a mathematical expression <br>
     * e.g.: 5 + 2
     *
     * @param lines List of lines that contain the expression
     */
    static void checkMathExp(List<Line> lines) {
        if(lines.isEmpty())
            throw new SyntaxErrorException(new Line("", "ERR", -1),
                    "Critical Error in checkMathExp. Empty list. No mathematical expression was found. This Could be a internal error"
                    );

        balancedParenthesis(lines);

        if (lines.get(0).s.equals("*") || lines.get(0).s.equals("/") || lines.get(0).s.equals("["))
            throw new SyntaxErrorException(lines.get(0), "Unexpected \"" + lines.get(0).s + "\"");

        String last = "START";

        int nextIndex = -1;
        for (int i = 0; i < lines.size(); i++) {
            if(i < nextIndex)
                continue;

            Line line = lines.get(i);
            boolean hasNext = i + 1  < lines.size();

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
                        nextIndex = i + checkSynMathExpInBrackets(lines, i);
                        last = "VAL";
                    }
                    else if (isVariableName(line.s) && hasNext && lines.get(i + 1).s.equals("(")) {
                        nextIndex = i + checkSynInFunCall(lines, i);
                        last = "VAL";
                    }
                    else if (isVariableName(line.s) && hasNext && lines.get(i + 1).s.equals("[")) {
                        nextIndex = i + checkSynArrAccess(lines, i);
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
                        nextIndex = i + checkSynMathExpInBrackets(lines, i);
                        last = "VAL";
                    }
                    else if (isVariableName(line.s) && hasNext && lines.get(i + 1).s.equals("(")) {
                        nextIndex = i + checkSynInFunCall(lines, i);
                        last = "VAL";
                    }
                    else if (isVariableName(line.s) && hasNext && lines.get(i + 1).s.equals("[")) {
                        nextIndex = i + checkSynArrAccess(lines, i);
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
            throw new SyntaxErrorException(lines.get(lines.size() - 1),
                    "\"" + lines.get(lines.size() - 1).s + "\" cannot stand alone.");
        }
    }

    /**
     * Check mathematical expression that is in "()" brackets
     * @param index Index where the "(" is located
     * @return lines to skip
     */
    static int checkSynMathExpInBrackets(List<Line> allLines, int index) {

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
        checkMathExp(allLines.subList(index + 1, closeIndex));
        return closeIndex - index + 1;
    }

    /**
     * check syntax of a variable assignment<br>
     * e.g.: "testVar = 5 + otherTestVar;"
     * @param index of variable name
     * @return lines to skip
     */
    static int checkSynVarAssign(List<Line> allLines, int index) {
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

        List<Line> expr = allLines.subList(index + 2, closeIndex);
        if(expr.isEmpty())
            throw new SyntaxErrorException(allLines.get(index), "Value for assignment is missing.");
        checkMathExp(expr);

        return countLines;
    }

    /**
     * Check for a function call in e.g. mathematical expressions
     * @param index Index where the function name is in allLines
     * @return how many lines the function call goes
     */
    static int checkSynInFunCall(List<Line> allLines, int index) {
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

        List<Line> arguments = allLines.subList(index + 2, closeIndex - 1);
        checkArgumentInList(arguments);

        return countLines;
    }

    /**
     * check syntax of a function call that stands alone in a line <br>
     * e.g.: "testFun(3);"
     * @param index of function call
     * @return lines to skip
     */
    static int checkSynLineFunCall(List<Line> allLines, int index) {
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
    static int checkSynReturn(List<Line> allLines, int index) {
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
    static int checkSynIfCond(List<Line> allLines, int index) {
        int countLines = 0;

        if(!allLines.get(index).s.equals("if"))
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected \"if\" at start of if-condition, got: \"" + allLines.get(index).s + "\".");
        countLines ++;

        if(!allLines.get(index + 1).s.equals("("))
            throw new SyntaxErrorException(allLines.get(index + 1),
                    "Expected \"(\" for the condition if, got: \"" + allLines.get(index + 1).s + "\".");

        countLines += checkSynMathExpInBrackets(allLines, index + 1);

        if(!allLines.get(index + countLines).s.equals("{"))
            throw new SyntaxErrorException(allLines.get(index + countLines),
                    "Expected \"{\" for the code block after if(), got: \"" + allLines.get(index + countLines).s + "\".");

        countLines += checkSynCodeBlock(allLines, index + countLines);

        int nextIndex = -1;
        for (int i = index + countLines; i < allLines.size(); i++) {
            if(i < nextIndex)
                continue;

            if(i + 1 < allLines.size() && allLines.get(i).s.equals("else") && allLines.get(i + 1).s.equals("if")) {
                countLines += 2;

                if(!allLines.get(index + countLines).s.equals("("))
                    throw new SyntaxErrorException(allLines.get(index + countLines),
                            "Expected \"(\" after else if, got: \"" + allLines.get(index + countLines).s + "\".");

                countLines += checkSynMathExpInBrackets(allLines, index + countLines);

                if(!allLines.get(index + countLines).s.equals("{"))
                    throw new SyntaxErrorException(allLines.get(index + countLines),
                            "Expected \"{\" for the code block after else if(), got: \"" + allLines.get(index + countLines).s + "\".");

                countLines += checkSynCodeBlock(allLines, index + countLines);

                nextIndex = index + countLines;
            } else if (i + 1 < allLines.size() && allLines.get(i).s.equals("else")) {
                countLines ++;

                if(!allLines.get(index + countLines).s.equals("{"))
                    throw new SyntaxErrorException(allLines.get(index + countLines),
                            "Expected \"{\" for the code block after else, got: \"" + allLines.get(index + countLines).s + "\".");

                countLines += checkSynCodeBlock(allLines, index + countLines);

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
    static int checkSynWhileLoop(List<Line> allLines, int index) {
        int countLines = 0;

        if(!allLines.get(index).s.equals("while"))
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected \"while\" at start of loop, got: \"" + allLines.get(index).s + "\".");
        countLines ++;

        if(!allLines.get(index + 1).s.equals("("))
            throw new SyntaxErrorException(allLines.get(index + 1),
                    "Expected \"(\" for the condition at while loop, got: \"" + allLines.get(index + 1).s + "\".");

        countLines += checkSynMathExpInBrackets(allLines, index + 1);

        if(!allLines.get(index + countLines).s.equals("{"))
            throw new SyntaxErrorException(allLines.get(index + countLines),
                    "Expected \"{\" for the code block after while(), got: \"" + allLines.get(index + countLines).s + "\".");

        countLines += checkSynCodeBlock(allLines, index + countLines);

        return countLines;
    }

    /**
     * check syntax of variable declaration
     *
     * @param index of declaration
     * @return lines to skip
     */
    static int checkSynVarDec(List<Line> allLines, int index) {
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
    static int checkSynArrDec(List<Line> allLines, int index) {
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
            } else if (((closeIndex - index) & 0b1) == 0 || (closeIndex - index) < 1) {
                throw new SyntaxErrorException(allLines.get(index), "Cannot read value.");
            }

            List<Line> arguments = allLines.subList(index + 6, closeIndex - 1);
            checkArgumentInList(arguments);

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
     * @param arguments Contains all lines with the arguments
     */
    static void checkArgumentInList(List<Line> arguments) {

        // now we have to check for arguments
        // it is a little more complicated
        // at first we have to check that all brackets are matching.
        // If all are closed we can look for a ','.
        // If not, either we hit the end or the argument continues.
        // When we found a whole argument we send it to check for mathematical expression
        // After that we have to check the next argument.

        balancedParenthesis(arguments);

        Deque<Character> stack = new ArrayDeque<>();
        int nextArgIndex = 0;
        for(int i = nextArgIndex; i < arguments.size(); i++) {

            if(arguments.get(i).s.equals("(") || arguments.get(i).s.equals("["))
                stack.push(arguments.get(i).s.charAt(0));
            else if(arguments.get(i).s.equals(")") || arguments.get(i).s.equals("]"))
                stack.pop();
            else if(stack.isEmpty() && arguments.get(i).s.equals(",")) {
                checkMathExp(arguments.subList(nextArgIndex, i));
                nextArgIndex = i + 1;
            }

        }
    }

    /**
     * Check if brackets are matching.
     * Supported brackets are: {}[]()
     */
    static void balancedParenthesis(List<Line> lines) {

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
    static void checkSynComStat(Line line) {
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

    private static void throwUnexpectedBracket(Line line) {
        throw new CompilerParseException(line.num, "Unexpected: " + line.s, line.fName);
    }

    private static boolean isNum(String number) {
        return number.matches("^\\d*$");
    }

    private static boolean isVariableName(String name) {
        name = name.strip();
        return name.matches("^[a-zA-Z_$][a-zA-Z_$\\d]*$");
    }
}
