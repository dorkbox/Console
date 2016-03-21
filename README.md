InputConsole
============

Unbuffered input on the console for java applications.

This small library is very similar to what JLine provides, however it does 4 things very differently:

1. Only provides unbuffered input.  
  - Backspace functionality for line input is preserved.  
  - Ctrl-C (SIGINT) is also preserved in windows  
2. Uses native calls via JNA (instead of shell execution) for linux & mac terminal configuration  
3. Supports unsupported teminals (for example, while in an IDE ), so in.read() will still return (a line is split into chars, then fed to consumer). The enter key must still be pressed.  
4. Multi-threaded, intelligent buffering of command input for simultaneous input readers on different threads  


- This is for cross-platform use, specifically - linux 32/64, mac 32/64, and windows 32/64. Java 6+
- ANSI output to console is also supported, and is required for backspace functionality to work if echo is enabled.


```
Note: If you use the attached JNA/JAnsi libraries, you **MUST** load the respective
      native libraries yourself, especially with JNA (as the loading logic has
      been removed from the jar)
```
```
Note: This project was inspired (and some parts heavily modified) by the excellent 
      JLine library, and includes utility classes/methods from a variety of sources.
```


<h4>We now release to maven!</h4> 

There is a hard dependency in the POM file for the utilities library, which is an extremely small subset of a much larger library; including only what is *necessary* for this particular project to function.

This project is **kept in sync** with the utilities library, so "jar hell" is not an issue. Please note that the util library (in it's entirety) is not added since there are **many** dependencies that are not *necessary* for this project. No reason to require a massive amount of dependencies for one or two classes/methods. 
```
<dependency>
  <groupId>com.dorkbox</groupId>
  <artifactId>InputConsole</artifactId>
  <version>2.5</version>
</dependency>
```

Or if you don't want to use Maven, you can access the files directly here:  
https://oss.sonatype.org/content/repositories/releases/com/dorkbox/InputConsole/  
https://oss.sonatype.org/content/repositories/releases/com/dorkbox/InputConsole-Dorkbox-Util/  

https://oss.sonatype.org/content/repositories/releases/com/dorkbox/ObjectPool/  
  

https://repo1.maven.org/maven2/net/java/dev/jna/jna/  
https://repo1.maven.org/maven2/net/java/dev/jna/jna-platform/  
https://repo1.maven.org/maven2/org/slf4j/slf4j-api/  

<h2>License</h2>

This project is distributed under the terms of the Apache v2.0 License. See file "LICENSE" for further references.

