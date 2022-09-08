package de.loisel.chip.clc.gfg;

import de.loisel.chip.clc.Line;

import java.util.*;

/**
 * Slightly changed Shunting Yard Algorithm
 */
public interface ShuntingYard {

    // Operator having higher precedence
    // value will be returned
    private static int getPrecedence(String op) {
        if(isFunName(op))
            return 0;
        return switch (op) {
            case "(", ")", "," -> 0;// functions
            case "==", "!=" -> 9;   // equality
            case "<", ">" -> 10;    // relational
            case "+", "-" -> 11;    // additive
            case "*", "/" -> 12;    // multiplicative
            default -> -1;
        };
    }

    private static List<String> addFunctionCallReturn(List<Line> expression, int index, int end, List<String> functions, List<String> variables, List<String> arrays) {
        List<String> output = new ArrayList<>();
        output.add("_StartFunRet_ " + expression.get(index).s);

        int argumentCounter = -1;
        int counter = 0;
        int lastComma = index + 1;  // set to the start of the expression
        for (int i = index + 2; i < end; i++) {
            String token = expression.get(i).s;
            if(token.equals("("))
                counter++;
            else if (token.equals(")"))
                counter--;
            else if (counter == 0 && token.equals(",")) {
                argumentCounter++;
                output.add("_FunArgument_ " + argumentCounter);
                output.addAll( infixToRpn(
                        expression.subList(lastComma + 1, i),
                        functions,
                        variables,
                        arrays
                ));
                output.add("_EndFunArgument_ " + argumentCounter);
                lastComma = i;
            }
        }
        argumentCounter++;
        output.add("_FunArgument_ " + argumentCounter);
        output.addAll( infixToRpn(
                expression.subList(lastComma + 1, end),
                functions,
                variables,
                arrays
        ));
        output.add("_EndFunArgument_ " + argumentCounter);

        output.add("_EndFunRet_");
        return  output;
    }

    private static List<String> addArrayCall(List<Line> expression, int index, int end, List<String> functions, List<String> variables, List<String> arrays) {
        List<String> output = new ArrayList<>();
        output.add("_StartArrayVal_ " + expression.get(index).s);
        output.addAll( infixToRpn(
                expression.subList(index + 2, end),
                functions,
                variables,
                arrays
        ));
        output.add("_EndArrayVal_");
        return  output;
    }

    /**
     * Method converts  given infix to postfix
     * to illustrate shunting yard algorithm
     * @param infixExpr The Expression to be converted
     * @param functions All occurring functions
     * @param variables All occurring variables
     * @param arrays All occurring arrays
     * @return The given input in reverse polish notation
     */
    static List<String> infixToRpn(List<Line> infixExpr, List<String> functions, List<String> variables, List<String> arrays) {
        // Initialising an empty String
        // (for output) and an empty stack
        Deque<String> stack = new ArrayDeque<>();

        // Initially empty string taken
        List<String> output = new ArrayList<>();

        // Iterating over tokens using inbuilt
        // .length() function
        for (int i = 0; i < infixExpr.size(); ++i) {
            // Finding character at index i
            String token = infixExpr.get(i).s;

            // If the scanned Token is a
            // number, function call,  add it to output
            if (isNumeric(token)) output.add("_IntVal_ " + token);

            else if (variables.contains(token)) output.add("_IntVar_ " + token);

            else if(arrays.contains(token)) {
                int counter = 0;
                int end = -1;
                for (int j = i; j < infixExpr.size(); j++) {
                    if(infixExpr.get(j).s.equals("["))
                        counter++;
                    else if (infixExpr.get(j).s.equals("]")) {
                        counter--;
                        if(counter == 0)
                            end = j;
                    }
                }
                output.addAll(addArrayCall(infixExpr, i, end, functions, variables, arrays));
                i = end;
            }

            else if (functions.contains(token)) {
                int counter = 0;
                int end = -1;
                for (int j = i; j < infixExpr.size(); j++) {
                    String tmpToken = infixExpr.get(j).s;
                    if(tmpToken.equals("("))
                        counter++;
                    else if (tmpToken.equals(")")) {
                        counter--;
                        if(counter == 0)
                            end = j;
                    }
                }
                output.addAll(addFunctionCallReturn(infixExpr, i, end, functions, variables, arrays));
                i = end;
            }

            // If the scanned Token is an '('
            // push it to the stack
            else if (token.equals("(")) stack.push(token);

                // If the scanned Token is an ')' pop and append
                // it to output from the stack until an '(' is
                // encountered
            else if (token.equals(")")) {
                while (!stack.isEmpty() && !stack.peek().equals("(")) {
                    output.add("_Operator_ " + stack.pop());
                }

                stack.pop();
            }

            // If an operator is encountered then taken the
            // further action based on the precedence of the
            // operator

            else {
                while (!stack.isEmpty() && getPrecedence(token) <= getPrecedence(stack.peek())) {
                    // peek() inbuilt stack function to
                    // fetch the top element(token)

                    output.add("_Operator_ " + stack.pop());
                }
                stack.push(token);
            }
        }

        // pop all the remaining operators from
        // the stack and append them to output
        while (!stack.isEmpty()) {
            output.add("_Operator_ " + stack.pop());
        }
        return output;
    }

    private static boolean isFunName(String name) {
        name = name.strip();
        return name.matches("^[a-zA-Z_$][a-zA-Z_$\\d]*$");
    }

    private static boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch(NumberFormatException e){
            return false;
        }
    }
}
