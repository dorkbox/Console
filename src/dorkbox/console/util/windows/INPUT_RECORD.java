package dorkbox.console.util.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;
import com.sun.jna.Union;

/**
 * https://msdn.microsoft.com/en-us/library/ms683499(v=VS.85).aspx
 */
public class INPUT_RECORD extends Structure {
    static public class ByReference extends INPUT_RECORD implements Structure.ByReference {}

    public static final short KEY_EVENT = 0x0001;
    public static final short MOUSE_EVENT = 0x0002;

    public short EventType;
    public EventUnion Event;

    public static class EventUnion extends Union {
        public KEY_EVENT_RECORD KeyEvent;
        public MOUSE_EVENT_RECORD MouseEvent;
    }

    @Override
    public void read() {
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
    protected List<String> getFieldOrder() {
        return Arrays.asList("EventType", "Event");
    }
}
