module dorkbox.console {
    exports dorkbox.console;
    exports dorkbox.console.input;
    exports dorkbox.console.output;

    requires transitive dorkbox.byteUtils;
    requires transitive dorkbox.propertyLoader;
    requires transitive dorkbox.jna;
    requires transitive dorkbox.updates;

    requires transitive kotlin.stdlib;

    requires transitive com.sun.jna;
    requires transitive com.sun.jna.platform;

    requires transitive org.slf4j;
}
