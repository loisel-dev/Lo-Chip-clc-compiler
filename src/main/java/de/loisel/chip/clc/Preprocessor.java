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
        Map<String, List<Line>> clearedFiles = new HashMap<>();
        files.forEach((name, file) -> clearedFiles.put(name, clearCode(name, file)));

        List<Line> allLines = execPreprocessorCommands(clearedFiles);

        // Splitting lines until there is only one statement per line

        allLines = splitLines(allLines);

        return allLines;
    }

    private static List<Line> execPreprocessorCommands(Map<String, List<Line>> clearedFiles) {
        List<Line> allLines = new ArrayList<>();

        String startName = "";

        for(Map.Entry<String, List<Line>> entry: clearedFiles.entrySet()) {
            for (Line line : entry.getValue()) {
                if(line.s.contains("int main()") && startName.isEmpty()) {
                    startName = entry.getKey();
                } else if(line.s.contains("int main()") && !startName.isEmpty()) {
                    throw new PreprocessorException(line,
                            "Found multiple entry points: \"" + startName + "\" and \"" + entry.getKey() + "\".");
                }
            }
        }

        if(startName.isEmpty())
            throw new PreprocessorException(new Line("N.A.", "N.A.", -1), "No entry point was found.");

        includeFile(clearedFiles, allLines, startName);



        return allLines;
    }

    private static void includeFile(Map<String, List<Line>> clearedFiles, List<Line> allLines, String includeFile) {
        for (Line line : clearedFiles.get(includeFile)) {
            if(!line.s.contains("#include")) {
                allLines.add(line);
                continue;
            }

            for (Map.Entry<String, List<Line>> entry: clearedFiles.entrySet()) {
                if(entry.getKey().contains(
                        line.s.substring(line.s.indexOf('"') + 1, line.s.lastIndexOf('"'))
                )) {
                    includeFile(clearedFiles, allLines, entry.getKey());
                }
            }

        }
    }

    private static List<Line> splitLines(List<Line> lines) {
        List<Line> splitLines = new ArrayList<>();

        List<String> keys = new ArrayList<>();
        keys.addAll(Arrays.asList(Compiler.LANG_SIGNS));
        keys.addAll(Arrays.asList(Compiler.LANG_KEYWORDS));

        for (Line line : lines) {
            String value = line.s;

            if(value.charAt(0) == '#') {       // dont split preprocessor commands
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
                if(line.indexOf(key, i) - i == 0)
                    return i;
            }

        }

        return index;
    }

    private static List<Line> clearCode(String name, List<String> file) {
        List<Line> lines = new ArrayList<>();

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

        return lines;
    }
}
