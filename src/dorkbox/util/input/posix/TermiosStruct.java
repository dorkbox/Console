/*
 * Copyright 2010 dorkbox, llc
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
package dorkbox.util.input.posix;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

public class TermiosStruct extends Structure {
    /** input mode flags */
    public int c_iflag;
    /** output mode flags */
    public int c_oflag;
    /** control mode flags */
    public int c_cflag;
    /** local mode flags */
    public int c_lflag;
    /** line discipline */
    public byte c_line;

    /** control characters */
    public byte[] c_cc = new byte[32];

    /** input speed */
    public int c_ispeed;
    /** output speed */
    public int c_ospeed;

    public TermiosStruct() {
    }

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList(
             "c_iflag",
             "c_oflag",
             "c_cflag",
             "c_lflag",
             "c_line",
             "c_cc",
             "c_ispeed",
             "c_ospeed"
                );
    }
}