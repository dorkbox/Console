/*
 * Copyright 2016 dorkbox, llc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dorkbox.console.output;

import static dorkbox.console.output.AnsiOutputStream.ERASE_ALL;
import static dorkbox.console.output.AnsiOutputStream.ERASE_TO_BEGINNING;
import static dorkbox.console.output.AnsiOutputStream.ERASE_TO_END;

public
enum Erase {
    FORWARD(ERASE_TO_END, "FORWARD"),
    BACKWARD(ERASE_TO_BEGINNING, "BACKWARD"),
    ALL(ERASE_ALL, "ALL");

    private final int value;
    private final String name;

    Erase(int index, String name) {
        this.value = index;
        this.name = name;
    }

    @Override
    public
    String toString() {
        return name;
    }

    public
    int value() {
        return value;
    }
}
