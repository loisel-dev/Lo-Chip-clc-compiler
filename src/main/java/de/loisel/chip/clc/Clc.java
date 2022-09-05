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

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Clc {

    Map<String, List<String>> files = new HashMap<>();
    List<Line> processedLines;
    List<String> assemblyProgram;

    public Clc(File[] srcFiles) {
        loadFiles(srcFiles);
    }

    public void compile() {
        long startTime = System.currentTimeMillis();
        
        Preprocessor processor;
        Compiler compiler;

        processor= new Preprocessor(files);
        processedLines = processor.process();

        compiler = new Compiler(processedLines);
        assemblyProgram = compiler.compile();
        saveFile(assemblyProgram);

        message("Took "
                + (((double)System.currentTimeMillis() - (double)startTime) / 1000)
                + " seconds to compile.");
    }

    private void saveFile(List<String> file) {

    }

    private void loadFiles(File[] srcFiles) {

        for (File file : srcFiles) {
            try(BufferedReader br = new BufferedReader(new FileReader(file))) {

                List<String> content = new ArrayList<>();

                for(String line; (line = br.readLine()) != null; ) {
                    content.add(line);
                }

                files.put(file.getAbsolutePath(), content);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void message(String msg) {
        System.out.println("clc-Compiler: " + msg);
    }
}