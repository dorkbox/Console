Console
=======

Unbuffered input and ANSI output support for linux, mac, windows. Java 6+

This library is a optimized combination of [JLine](https://github.com/jline/jline2) and [JAnsi](https://github.com/fusesource/jansi). While it is very similar in functionality to what these libraries provide, there are several things that are significantly different.

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
  
  
- This is for cross-platform use, specifically - linux arm/32/64, mac 32/64, and windows 32/64. Java 6+
  
  
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
     - AUTO - automatically determine which OS/console type to use
     - UNIX - try to control a UNIX console
     - WINDOWS - try to control a WINDOWS console
     - NONE - do not try to control anything, only line input is supported 

        
Ansi.restoreSystemStreams()
 - Restores System.err/out PrintStreams to their ORIGINAL configuration. Useful when using ANSI functionality but do not want to hook into the system.
```



```
Note: This project was inspired (and some parts heavily modified) by the excellent 
      JLine and JAnsi libraries. Many thanks to their hard work.
```


<h4>We now release to maven!</h4> 

There is a hard dependency in the POM file for the utilities library, which is an extremely small subset of a much larger library; including only what is *necessary* for this particular project to function.

This project is **kept in sync** with the utilities library, so "jar hell" is not an issue. Please note that the util library (in it's entirety) is not added since there are **many** dependencies that are not *necessary* for this project. No reason to require a massive amount of dependencies for one or two classes/methods. 
```
<dependency>
  <groupId>com.dorkbox</groupId>
  <artifactId>Console</artifactId>
  <version>2.9</version>
</dependency>
```

Or if you don't want to use Maven, you can access the files directly here:  
https://oss.sonatype.org/content/repositories/releases/com/dorkbox/Console/  
https://oss.sonatype.org/content/repositories/releases/com/dorkbox/Console-Dorkbox-Util/  

https://oss.sonatype.org/content/repositories/releases/com/dorkbox/ObjectPool/  
  

https://repo1.maven.org/maven2/net/java/dev/jna/jna/  
https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/  
https://repo1.maven.org/maven2/org/slf4j/slf4j-api/  

<h2>License</h2>

This project is © 2010 dorkbox llc, and is distributed under the terms of the Apache v2.0 License. See file "LICENSE" for further references.

