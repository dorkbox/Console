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