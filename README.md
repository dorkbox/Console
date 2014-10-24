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


This is for cross-platform use, specifically - linux 32/64, mac 32/64, and windows 32/64. Java 6+

ANSI output to console is also supported, and is required for backspace functionality to work if echo is enabled.

```
Note: If you use the attached JNA/JAnsi libraries, you **MUST** load the respective
      native libraries yourself, especially with JNA (as the loading logic has
      been removed from the jar)
```
```
Note: This project was heavily influence by the excellent JLine library, and
      includes utility classes/methods from a variety of sources.
```

```
* FastObjectPool - Apache 2.0 License
  http://ashkrit.blogspot.com/2013/05/lock-less-java-object-pool.html
  https://github.com/ashkrit/blog/tree/master/FastObjectPool
  Copyright 2013 Ashkrit

* JLine - BSD License
  http://jline.sourceforge.net/
  Copyright 2002-2006, Marc Prud'hommeaux <mwp1@cornell.edu>  All rights reserved.

* Jansi - Apache 2.0 License
  hawtjni - EPL v1.0
  https://github.com/fusesource/jansi
  Copyright Hiram Chirino

* JNA - Apache 2.0 License
  https://github.com/twall/jna
  Copyright (c) 2011 Timothy Wall
```