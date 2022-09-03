package de.loisel.chip.clc;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;

interface SyntaxCheck {

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
                case "#": {
                    checkSynComStat(line);
                    nextIndex = i;
                    break;
                }

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
        int closingIndex = 0;
        Deque<Character> stack = new ArrayDeque<>();
        stack.push('{');
        for (int i = index + 1; i < allLines.size(); i++) {
            if (allLines.get(i).s.equals("{"))
                stack.push('{');
            else if (allLines.get(i).s.equals("}"))
                stack.pop();
            if (stack.isEmpty()) {
                closingIndex = i;
                break;
            }
        }
        if(closingIndex == 0 || !allLines.get(index).absPath.equals(allLines.get(closingIndex).absPath)) {
            throw new SyntaxErrorException(allLines.get(index), "Code-block was not closed");
        }

        countLines = closingIndex + 1 - index;

        // check content of Code block
        int nextIndex = -1;
        for (int i = index + 1; i < closingIndex; i++) {
            if (i < nextIndex)
                continue;

            String val = allLines.get(i).s;

            if ("while".equals(val)) {                           // while loop
                nextIndex = i + checkSynWhileLoop(allLines, i);
            } else if ("return".equals(val)) {                   // return statement
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
        int countLines;

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
        if(!allLines.get(index + 3).s.equals("]")) {    // missing ']'
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected ], got :\"" + allLines.get(index + 3).s + "\".");
        }
        if(!isVariableName(allLines.get(index + 2).s)   // wrong index
                && !isNum(allLines.get(index + 2).s)) {
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected a number or variable as index, got :\"" + allLines.get(index + 2).s + "\".");
        }
        if(!allLines.get(index + 4).s.equals("=")) {    // missing '='
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected =, got :\"" + allLines.get(index + 4).s + "\".");
        }

        countLines = 5 + checkAssignMath(allLines, index + 5);

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

        int endIndex = -1;
        for (int i = index; i < allLines.size() && endIndex == -1; i++) {
            if (allLines.get(i).s.equals(";"))
                endIndex = i;
        }
        if (endIndex == -1)
            throw new SyntaxErrorException(allLines.get(index), "No ';' after variable assignment found");
        countLines = endIndex - index + 1;

        List<Line> subList = allLines.subList(index, endIndex);

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
        balancedParenthesis(lines);

        if (lines.get(0).s.equals("*") || lines.get(0).s.equals("/"))
            throw new SyntaxErrorException(lines.get(0), "Unexpected \"" + lines.get(0).s + "\"");

        for (int i = 0; i < lines.size(); i++) {
            String val = lines.get(i).s;

            if (!val.matches("^[a-zA-Z_$\\d()+\\-*/\\[\\]]*$"))       // anything unexpected that not should be here
                throw new SyntaxErrorException(lines.get(i), "What does \"" + val + "\" do there?");

            else if (
                    Arrays.asList(Compiler.MATH_OPERATORS).contains(val)
                            && i + 1 == lines.size()
            ) {                                                 // Math operators
                throw new SyntaxErrorException(lines.get(i), "What does \"" + val + "\" do there alone?");
            } else if (
                    Arrays.asList(Compiler.MATH_OPERATORS).contains(val)
                            && !isVariableName(lines.get(i + 1).s)
                            && !isNum(lines.get(i + 1).s)
                            && !lines.get(i + 1).s.equals("(")
            ) {
                throw new SyntaxErrorException(lines.get(i), "Could not parse expression");
            } else if (
                    val.equals("(")                               // '(' Bracket
                            && !Arrays.asList(Compiler.MATH_OPERATORS).contains(val)
                            && !isVariableName(lines.get(i + 1).s)
                            && !isNum(lines.get(i + 1).s)
                            && !lines.get(i + 1).s.equals("(")
            ) {
                throw new SyntaxErrorException(lines.get(i + 1), "Unexpected \"" + lines.get(i + 1).s + "\"");
            } else if (
                    i + 1 < lines.size()
                            && val.equals(")")
                            && (isNum(lines.get(i + 1).s) || isVariableName(lines.get(i + 1).s))
            ) {
                throw new SyntaxErrorException(lines.get(i + 1), "Unexpected \"" + lines.get(i + 1).s + "\"");
            }
        }
    }

    /**
     * check syntax of a variable assignment
     *
     * @param index of variable name
     * @return lines to skip
     */
    static int checkSynVarAssign(List<Line> allLines, int index) {
        int countLines = 0;

        return countLines;
    }

    /**
     * check syntax of a function call that stands alone in a line <br>
     * e.g.: "testFun(3);"
     * @param index of function call
     * @return lines to skip
     */
    static int checkSynLineFunCall(List<Line> allLines, int index) {
        int countLines = 0;

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
            throw new SyntaxErrorException(allLines.get(index), "No closing ';' found.");
        }
        if(allLines.size() < closeIndex + 1 || !allLines.get(closeIndex).s.equals(";")) {
            throw new SyntaxErrorException(allLines.get(index), "No closing ';' found.");
        }
        if(!allLines.get(closeIndex - 1).s.equals(")")) {
            throw new SyntaxErrorException(allLines.get(index), "No closing ')' found.");
        }

        countLines = closeIndex - index + 1;

        List<Line> arguments = allLines.subList(index + 2, closeIndex - 1);
        balancedParenthesis(arguments);

        // now we have to check for arguments
        // it is a little more complicated
        // at first we have to check that all brackets are matching.
        // If all are closed we can look for a ','.
        // If not, either we hit the end or the argument continues.
        // When we found a whole argument we send it to check for mathematical expression
        // After that we have to check the next argument.
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
            else if(stack.isEmpty() && i == arguments.size() - 1) {
                checkMathExp(arguments.subList(nextArgIndex, i + 1));
                nextArgIndex = i + 2;
            }

        }

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

        if(!allLines.get(index).s.equals("return")) {
            throw new SyntaxErrorException(allLines.get(index),
                    "Expected \"return\", got: \"" + allLines.get(index).s + "\".");
        }

        if(!allLines.get(index + 1).s.equals("return")) {       // got a return value
            countLines += checkAssignMath(allLines, index + 1);
        }

        return countLines;
    }

    /**
     * check syntax of an if condition
     *
     * @param index of if
     * @return lines to skip
     */
    static int checkSynIfCond(List<Line> allLines, int index) {
        int countLines = 0;

        return countLines;
    }

    /**
     * check syntax of a while loop
     *
     * @param index of while
     * @return lines to skip
     */
    static int checkSynWhileLoop(List<Line> allLines, int index) {
        int countLines = 0;

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

        if (allLines.get(index + 5).s.equals("[")) {             // check: int name = [3];

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
                                                                    // check: int name = { 3, ..., 1 };
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
            } else {

                int counter = 0;
                while (index + 6 + counter < closeIndex) {      // check if there is alternating a number and a ','
                    if (
                            ((counter & 0b1) == 0 && !isNum(allLines.get(index + 6 + counter).s))
                                    || ((counter & 0b1) == 1 && !allLines.get(index + 6 + counter).s.equals(","))
                    ) {
                        throw new SyntaxErrorException(allLines.get(index), "Cannot read value.");
                    }
                    counter++;
                }

            }

            if (!allLines.get(closeIndex + 1).s.equals(";")) {
                throw new SyntaxErrorException(allLines.get(closeIndex),
                        "Expected ';' got: '" + allLines.get(closeIndex).s + "'");
            }
            countLines = closeIndex - index + 2;
        }

        return countLines;
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

    private static boolean isNum(String number) {
        return number.matches("\\d");
    }

    private static boolean isVariableName(String name) {
        name = name.strip();
        return name.matches("^[a-zA-Z_$][a-zA-Z_$\\d]*$");
    }
}
