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

package de.loise.chip.clc;

import de.loisel.chip.clc.Clc;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import java.io.File;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class ExampleTest {

    static final String FILE_1 = "example.clc";
    static final String FILE_2 = "example-include.clc";
    final File FOLDER = new File("src/test/resources" + File.separator + "example-project");

    Clc compiler;

    @Test
    @Order(1)
    void loadExampleFilesTest() {

        File[] files= {
                new File(FOLDER.getAbsoluteFile() + File.separator + FILE_1),
                new File(FOLDER.getAbsoluteFile() + File.separator + FILE_2)
        };

        compiler = new Clc(files);

    }

    @Test
    @Order(2)
    void compileExampleTest() {

        loadExampleFilesTest();

        compiler.compile();
    }
}
