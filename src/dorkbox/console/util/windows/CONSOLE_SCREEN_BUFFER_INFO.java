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
package dorkbox.console.util.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

/**
 * https://msdn.microsoft.com/en-us/library/ms682093%28VS.85%29.aspx
 */
public
class CONSOLE_SCREEN_BUFFER_INFO extends Structure {

    public COORD size = new COORD();
    public COORD cursorPosition = new COORD();
    public short attributes = (short) 0;
    public SMALL_RECT window = new SMALL_RECT();
    public COORD maximumWindowSize = new COORD();

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("size", "cursorPosition", "attributes", "window", "maximumWindowSize");
    }

    @Override
    public
    String toString() {
        return "Size: " + size + " CursorPos: " + cursorPosition + " Attribs: " + attributes + " Window: " + window + " MaxWindowSize: " +
               maximumWindowSize;
    }
}
