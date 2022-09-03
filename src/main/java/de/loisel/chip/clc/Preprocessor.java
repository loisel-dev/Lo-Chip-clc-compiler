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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * the preprocessor "cleans" the files
 */
class Preprocessor {

    private final Map<String, List<String>> files;

    public Preprocessor(Map<String, List<String>> files) {
        this.files = files;
    }

    /**
     * Processes the file. Remove comments, multiple spaces,
     * empty lines.
     * @return Files<Filename, LineMap<Linenumber, Line-content>>
     */
    public List<Line> process() {

        // Remove unnecessary stuff
        List<Line> allLines = clearCode(files);

        // Splitting lines until there is only one statement per line

        allLines = splitLines(allLines);

        return allLines;
    }

    private static List<Line> splitLines(List<Line> lines) {
        List<Line> splitLines = new ArrayList<>();

        List<String> keys = new ArrayList<>();
        keys.addAll(Arrays.asList(Compiler.LANG_SIGNS));
        keys.addAll(Arrays.asList(Compiler.LANG_KEYWORDS));

        for (Line line : lines) {
            String value = line.s;

            if(value.charAt(0) == '#') {       // dont split compiler commands
                splitLines.add(line);
                continue;
            }

            boolean lineEnd = false;
            while(!lineEnd) {

                boolean foundKey = false;
                for (String key : keys) {

                    if(value.indexOf(key) == 0) {       // line contains sign or keyword

                        splitLines.add(new Line(key, line));
                        value = value.substring(key.length()).strip();
                        foundKey = true;

                        break;
                    }

                }

                if(!foundKey) {                 // line contains name
                    int nextKey = findNextKey(value, Arrays.asList(Compiler.LANG_SIGNS));

                    String name = (nextKey > -1) ? value.substring(0, nextKey).strip() : value;

                    splitLines.add(new Line(name, line));
                    value = value.substring(name.length()).strip();
                }

                if(value.isEmpty())
                    lineEnd = true;
            }

        }

        return splitLines;
    }

    /**
     * returns -1 if no key was found
     * @return index of the next key
     */
    private static int findNextKey(String line, List<String> keys) {
        int index = -1;

        for (int i = 0; i < line.length(); i++) {

            for (String key : keys) {
                //if(line.substring(i).indexOf(key) == 0)
                if(line.indexOf(key, i) - i == 0)
                    return i;
            }

        }

        return index;
    }

    private static List<Line> clearCode(Map<String, List<String>> files) {
        List<Line> lines = new ArrayList<>();

        files.forEach((name, file) -> {
            int lineNum = 1;
            for (String line : file) {
                if(line.contains("//"))
                    line = line.substring(0, line.indexOf("//"));
                line = line.strip();

                line = line.replace('\t', ' ');
                line = line.replaceAll("\s{2,}", " ");

                // remove empty lines
                if(!line.isEmpty())
                    // add line-numbers to generate error messages later in the process
                    lines.add(new Line(name, line, lineNum));
                lineNum++;
            }
        });

        return lines;
    }
}
