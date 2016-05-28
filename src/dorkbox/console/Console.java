package dorkbox.console;

/**
 *
 */
public
class Console {
    public static volatile boolean ENABLE_ECHO = true;

    public static boolean PASSWORD_ECHO = true;
    public static char PASSWORD_ECHO_CHAR = '*';

    // how many threads can read from this input at the same time
    public static int NUMBER_OF_READERS = 32;


    public final static boolean ENABLE_BACKSPACE = true;
//    enableBackspace = Boolean.parseBoolean(System.getProperty(Console.ENABLE_BACKSPACE, "true"));


    public static String ENABLE_BACKSPACEs = "input.enableBackspace";


    // OS types supported by the input console. Default is AUTO
    public static final String AUTO = "auto";
    public static final String UNIX = "unix";
    public static final String WIN = "win";
    public static final String WINDOWS = "windows";

    public static final String NONE = "none";  // this is the same as unsupported
    public static final String OFF = "off"; // this is the same as unsupported
    public static final String FALSE = "false"; // this is the same as unsupported
    public static final String INPUT_CONSOLE_OSTYPE = "AUTO";


    /**
     * Gets the version number.
     */
    public static
    String getVersion() {
        return "2.9";
    }

    public static Input in = new Input();
    public static String out;
    public static String err;
}
