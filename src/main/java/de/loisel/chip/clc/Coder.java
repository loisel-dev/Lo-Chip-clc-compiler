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

import de.loisel.chip.clc.gfg.ShuntingYard;

import java.util.*;

import static de.loisel.chip.clc.SyntaxCheck.isVariableName;

/**
 * Class to create a temporary code that is
 * easily converted to assembly.
 * Normally done during syntax check
 */
class Coder {

    private List<String> clcCode;

    private Map<String, String> functions = new HashMap<>();
    private Deque<List<String>> lvlVariables = new ArrayDeque<>();
    private Deque<List<String>> lvlArrays = new ArrayDeque<>();

    public Coder() {
        clcCode = new ArrayList<>();
        lvlVariables.push(new ArrayList<>());
    }

    /**
     * Defines a variable, needs to be in clean clc format
     * @param variable The definition of a Variable including the ';'
     */
    public void variableDefinition(List<Line> variable) {

        if(isOnStack(variable.get(1).s))
            throw new ClcCoderException(variable.get(1).num,
                    "Variable name \"" + variable.get(1).s + "\" already exists", variable.get(1).fName);

        lvlVariables.peek().add(variable.get(1).s);
        addDescription(variable);

        if(variable.get(0).s.equals("int")) {
            clcCode.add("_NewInt_ " + variable.get(1).s);
            intVal(variable.subList(3, variable.size() - 1));
            clcCode.add("_EndNewInt_ " + variable.get(1).s);
        }

    }

    private void intVal(List<Line> expression) {
        for (Line line : expression) {
            if(isVariableName(line.s) && !isOnStack(line.s) && functions.containsKey(line.s)) {
                throw new ClcCoderException(line.num, "Name not found: \"" + line.s + "\"", line.fName);
            }
            if(functions.containsKey(line.s) && !functions.get(line.s).equals("int"))
                throw new ClcCoderException(line.num, "Function \"" + line.s + "\" does not return int.", line.fName);
        }

        // shunting yard

        clcCode.add("_IntValue_ ");
        clcCode.addAll(ShuntingYard.infixToRpn(expression, functions.keySet().stream().toList(), stackToList(lvlVariables), stackToList(lvlArrays)));
        clcCode.add("_EndIntValue_ ");
    }

    private void addDescription(List<Line> information) {
        clcCode.add("# " + concat(information));
    }

    private boolean isOnStack(String name) {
        if(functions.containsKey(name))
            return true;

        for (List<String> names : lvlVariables.stream().toList()) {
            if(names.contains(name))
                return true;
        }

        return false;
    }

    private List<String> stackToList(Deque<List<String>> stack) {
        List<String> output = new ArrayList<>();
        for (List<String> list : stack) {
            output.addAll(list);
        }
        return  output;
    }

    private String concat(List<Line> values) {
        StringBuilder value = new StringBuilder();
        for (Line line : values) {
            value.append(line.s).append(" ");
        }
        return value.toString();
    }

}
