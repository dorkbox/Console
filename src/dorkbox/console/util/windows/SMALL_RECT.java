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
 * https://msdn.microsoft.com/en-us/library/ms686311%28VS.85%29.aspx
 */
@SuppressWarnings("NumericCastThatLosesPrecision")
public
class SMALL_RECT extends Structure {
    public short left;
    public short top;
    public short right;
    public short bottom;

    public
    short width() {
        return (short) (this.right - this.left);
    }

    public
    short height() {
        return (short) (this.bottom - this.top);
    }

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("left", "top", "right", "bottom");
    }

    @Override
    public
    String toString() {
        return "LTRB: " + left + "," + top + "," + right + "," + bottom;
    }


    static public
    class ByReference extends SMALL_RECT implements Structure.ByReference {}
}
