package de.loisel.chip.clc.gfg;

import de.loisel.chip.clc.Line;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class ShuntingYardTest {

    @Test
    void SimpleGfgTest() {
        List<String> fun = new ArrayList<>();
        List<String> arrVar = new ArrayList<>();
        
        List<Line> infix = new ArrayList<Line>() {{         // 5 + ( 3 * 10 - 8 )
            add(new Line("", "5", 1));
            add(new Line("", "+", 1));
            add(new Line("", "(", 1));
            add(new Line("", "3", 1));
            add(new Line("", "*", 1));
            add(new Line("", "10", 1));
            add(new Line("", "-", 1));
            add(new Line("", "8", 1));
            add(new Line("", ")", 1));
        }};
        
        List<String> output = ShuntingYard.infixToRpn(infix, fun, arrVar, arrVar);

        assertEquals("_IntVal_ 5", output.get(0));
        assertEquals("_Operator_ +", output.get(6));
    }

    @Test
    void ArrayTest() {
        List<String> fun = new ArrayList<>();
        List<String> vars = new ArrayList<>();
        List<String> arrays = new ArrayList<>(){{
            add("testArray");
        }};

        List<Line> infix = new ArrayList<Line>() {{         // 3 * (testArray[0])
            add(new Line("", "3", 1));
            add(new Line("", "*", 1));
            add(new Line("", "(", 1));
            add(new Line("", "testArray", 1));
            add(new Line("", "[", 1));
            add(new Line("", "3", 1));
            add(new Line("", "]", 1));
            add(new Line("", ")", 1));
        }};

        List<String> output = ShuntingYard.infixToRpn(infix, fun, vars, arrays);

        assertEquals("_IntVal_ 3", output.get(2));
    }

    @Test
    void FunctionTest() {
        List<String> fun = new ArrayList<>(){{
            add("testFunc");
        }};
        List<String> vars = new ArrayList<>();
        List<String> arrays = new ArrayList<>(){{
            add("testArray");
        }};

        List<Line> infix = new ArrayList<Line>() {{         // 3 * (5 - testFunc(3, 5 + 1))
            add(new Line("", "3", 1));
            add(new Line("", "*", 1));
            add(new Line("", "(", 1));
            add(new Line("", "5", 1));
            add(new Line("", "-", 1));
            add(new Line("", "testFunc", 1));
            add(new Line("", "(", 1));
            add(new Line("", "3", 1));
            add(new Line("", ",", 1));
            add(new Line("", "5", 1));
            add(new Line("", "+", 1));
            add(new Line("", "1", 1));
            add(new Line("", ")", 1));
            add(new Line("", ")", 1));
        }};

        List<String> output = ShuntingYard.infixToRpn(infix, fun, vars, arrays);

        assertEquals("_IntVal_ 3", output.get(2));
    }

    @Test
    void VariableTest() {
        List<String> fun = new ArrayList<>();
        List<String> vars = new ArrayList<>(){{
            add("testVar");
        }};
        List<String> arrays = new ArrayList<>();

        List<Line> infix = new ArrayList<Line>() {{         // 3 * (5 - testVar)
            add(new Line("", "3", 1));
            add(new Line("", "*", 1));
            add(new Line("", "(", 1));
            add(new Line("", "5", 1));
            add(new Line("", "-", 1));
            add(new Line("", "testVar", 1));
            add(new Line("", ")", 1));
        }};

        List<String> output = ShuntingYard.infixToRpn(infix, fun, vars, arrays);

        assertEquals("_IntVal_ 3", output.get(2));
    }
}
