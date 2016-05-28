package dorkbox.console.util.windows;

import java.util.Arrays;
import java.util.List;

import com.sun.jna.Structure;

/**
 * https://msdn.microsoft.com/en-us/library/ms684166(v=VS.85).aspx
 */
public class KEY_EVENT_RECORD extends Structure {
    public boolean keyDown;
    public short repeatCount;
    public short virtualKeyCode;
    public short virtualScanCode;
    public CharUnion uChar;
    public int controlKeyState;

    @Override
    protected List<String> getFieldOrder() {
        return Arrays.asList("keyDown", "repeatCount", "virtualKeyCode", "virtualScanCode", "uChar", "controlKeyState");
    }
}
