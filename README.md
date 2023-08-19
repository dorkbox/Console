Console
=======

###### [![Dorkbox](https://badge.dorkbox.com/dorkbox.svg "Dorkbox")](https://git.dorkbox.com/dorkbox/Console) [![Github](https://badge.dorkbox.com/github.svg "Github")](https://github.com/dorkbox/Console) [![Gitlab](https://badge.dorkbox.com/gitlab.svg "Gitlab")](https://gitlab.com/dorkbox/Console)



Unbuffered input and ANSI output support for linux, mac, windows. Java 8+

This library is the evolution of what [JLine](https://github.com/jline/jline2) should be, and the optimization of [JAnsi](https://github.com/fusesource/jansi). While it is very similar in functionality to what these libraries provide, there are several things that are significantly different.

 1. JNA *direct-mapping* instead of custom JNI/shell execution which is [slightly slower than JNI](https://github.com/java-native-access/jna/blob/master/www/DirectMapping.md) but significantly easier to read, modify, debug, and provide support for non-intel architectures.
 1. Complete implementation of common [ANSI escape codes](https://en.wikipedia.org/wiki/ANSI_escape_code)
 1. Automatically hooks into `System.err/out` for seamless integration in Java environments
 1. Automatically detects when an `IDE` is in use
   - Prevents issues with console corruption
   - Provides simulated single character input via `in.read()`, which still requires the enter key to flush the buffer, but feeds single characters at a time
 1. Backspace functionality for line input is preserved (if ANSI is enabled, the console is updated as well).
 1. Controls `ECHO` on/off in the console
 1. Controls `Ctrl-C` (SIGINT) on/off in the console
 1. Multi-threaded, intelligent buffering of command input for simultaneous input readers on different threads
 1. Solves un-interruptable blocking reads from System.in when in an "unsupported" terminal (ie: anything other than a *nix/windows shell) so one can successfully stop reading from the input stream,
  
  
- This is for cross-platform use, specifically - linux arm/32/64, mac 64, and windows 32/64. Java 8+
  
Windows  
![Windows](https://git.dorkbox.com/dorkbox/Console/raw/branch/master/windows%20console.png)  

Linux/Mac  
![*nix](https://git.dorkbox.com/dorkbox/Console/raw/branch/master/linux%20console.png)  

  
```
Customization parameters:
Console.ENABLE_ANSI   (type boolean, default value 'true')
 - If true, allows an ANSI output stream to be created on System.out/err, otherwise it will provide an ANSI aware PrintStream which strips out the ANSI escape sequences.


Console.FORCE_ENABLE_ANSI   (type boolean, default value 'false')
 - If true, then we always force the raw ANSI output stream to be enabled (even if the output stream is not aware of ANSI commands).   
   This can be used to obtain the raw ANSI escape codes for other color aware programs (ie: less -r)
        
        
Console.ENABLE_ECHO   (type boolean, default value 'true')
 - Enables or disables character echo to stdout in the console, should call Console.setEchoEnabled(boolean) after initialization.
        
        
Console.ENABLE_INTERRUPT   (type boolean, default value 'false')
 - Enables or disables CTRL-C behavior in the console, should call Console.setInterruptEnabled(boolean) after initialization.
        
        
Console.ENABLE_BACKSPACE   (type boolean, default value 'true')
 - Enables the backspace key to delete characters in the line buffer and (if ANSI is enabled) from the screen.
        
        
Console.INPUT_CONSOLE_TYPE   (type String, default value 'AUTO')
 - Used to determine what console to use/hook when AUTO is not correctly working.  
   Valid options are:
     AUTO - automatically determine which OS/console type to use
     MACOS - try to control a MACOS console
     WINDOWS - try to control a WINDOWS console
     UNIX - try to control a UNIX console
     NONE - do not try to control anything, only line input is supported


Console.AUTO_FLUSH   (type boolean, default value 'true')
 - Enables the output printstream to automatically flush after every write. NOTE: This is DANGEROUS, as it removes the usefulness of
     * the backing BufferWriter!
        
        
Ansi.restoreSystemStreams()
 - Restores System.err/out PrintStreams to their ORIGINAL configuration. Useful when using ANSI functionality but do not want to hook into the system.
```



```
Note: This project was inspired by the excellent JLine and JAnsi libraries. Many thanks to their hard work.
```

&nbsp; 
&nbsp; 

Maven Info
---------
```
<dependencies>
    ...
    <dependency>
      <groupId>com.dorkbox</groupId>
      <artifactId>Console</artifactId>
      <version>4.0</version>
    </dependency>
</dependencies>
```

Gradle Info
---------
```
dependencies {
    ...
    implementation("com.dorkbox:Console:4.0")
}
```

License
---------
This project is © 2021 dorkbox llc, and is distributed under the terms of the Apache v2.0 License. See file "LICENSE" for further 
references.

