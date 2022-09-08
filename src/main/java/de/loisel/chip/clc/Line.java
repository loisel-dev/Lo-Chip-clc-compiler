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

import java.io.File;

public class Line {
    public final String fName;
    public final String path;
    public final String absPath;
    public final int num;

    /**
     * Contains the content of the line.
     */
    public final String s;

    public Line(String absPath, String line, int lineNum) {
        File helper = new File(absPath);
        this.fName = helper.getName();
        this.path = helper.getPath();
        this.absPath = absPath;
        this.s = line;
        this.num = lineNum;
    }

    public Line(String content, Line line) {
        this.fName = line.fName;
        this.path = line.path;
        this.absPath = line.absPath;
        this.s = content;
        this.num = line.num;
    }

    public int length() {
        return s.length();
    }

    public char charAt(int i) {
        return s.charAt(i);
    }
}
