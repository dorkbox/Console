package dorkbox.console.util.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

public class MOUSE_EVENT_RECORD extends Structure {
    public COORD mousePosition;
    public int buttonState;
    public int controlKeyState;
    public int eventFlags;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("mousePosition", "buttonState", "controlKeyState", "eventFlags");
    }
}
