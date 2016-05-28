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

public
enum Color {
    BLACK  (AnsiOutputStream.BLACK,     "BLACK"),
    RED    (AnsiOutputStream.RED,         "RED"),
    GREEN  (AnsiOutputStream.GREEN,     "GREEN"),
    YELLOW (AnsiOutputStream.YELLOW,   "YELLOW"),
    BLUE   (AnsiOutputStream.BLUE,       "BLUE"),
    MAGENTA(AnsiOutputStream.MAGENTA, "MAGENTA"),
    CYAN   (AnsiOutputStream.CYAN,       "CYAN"),
    WHITE  (AnsiOutputStream.WHITE,     "WHITE");

    private final int value;
    private final String name;

    Color(int index, String name) {
        this.value = index;
        this.name = name;

        // register code names with the ANSI renderer
        AnsiRenderer.reg(this, name, false);
        AnsiRenderer.reg(this, "FG_" + name, false);
        AnsiRenderer.reg(this, "BG_" + name, true);
    }

    @Override
    public
    String toString() {
        return name;
    }

    public
    int fg() {
        return value + 30;
    }

    public
    int bg() {
        return value + 40;
    }

    public
    int fgBright() {
        return value + 90;
    }

    public
    int bgBright() {
        return value + 100;
    }
}
