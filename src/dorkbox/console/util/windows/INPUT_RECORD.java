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
import com.sun.jna.Union;

/**
 * https://msdn.microsoft.com/en-us/library/ms683499(v=VS.85).aspx
 */
public
class INPUT_RECORD extends Structure {
    public static final short KEY_EVENT = 0x0001;
    public static final short MOUSE_EVENT = 0x0002;
    public short EventType;
    public EventUnion Event;

    @Override
    public
    void read() {
        readField("EventType");
        switch (EventType) {
            case KEY_EVENT:
                Event.setType(KEY_EVENT_RECORD.class);
                break;
            case MOUSE_EVENT:
                Event.setType(MOUSE_EVENT_RECORD.class);
                break;
        }
        super.read();
    }

    @Override
    protected
    List<String> getFieldOrder() {
        return Arrays.asList("EventType", "Event");
    }


    static public
    class ByReference extends INPUT_RECORD implements Structure.ByReference {}


    public static
    class EventUnion extends Union {
        public KEY_EVENT_RECORD KeyEvent;
        public MOUSE_EVENT_RECORD MouseEvent;
    }
}
