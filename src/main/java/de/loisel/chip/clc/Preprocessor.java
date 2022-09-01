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
import java.util.List;
import java.util.Map;

/**
 * the preprocessor "cleans" the files
 */
class Preprocessor {

    private final Map<String, List<String>> files;
    private final List<Line> allLines = new ArrayList<>();

    public Preprocessor(Map<String, List<String>> files) {
        this.files = files;
    }

    /**
     * Processes the file. Remove comments, multiple spaces,
     * empty lines.
     * @return Files<Filename, LineMap<Linenumber, Line-content>>
     */
    public List<Line> process() {

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
                    allLines.add(new Line(name, line, lineNum));
                lineNum++;
            }
        });

        return  allLines;
    }

}
