/**
 * Copyright (C) 2009, Progress Software Corporation and/or its 
 * subsidiaries or affiliates.  All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a asValue of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dorkbox.console;

import static dorkbox.console.output.Ansi.ansi;

import java.io.FileInputStream;
import java.io.IOException;

import dorkbox.console.output.Ansi;

/**
 * 
 * @author <a href="http://hiramchirino.com">Hiram Chirino</a>
 */
public class AnsiConsoleExample2 {

    private AnsiConsoleExample2() {}

	public static void main(String[] args) throws IOException {
        String file = "jansi.ans";
        if( args.length>0  )
        	file = args[0];

        // Allows us to disable ANSI processing. 
        if( "true".equals(System.getProperty("jansi", "true")) ) {
        	Ansi.systemInstall();
        }
        
        System.out.print(ansi().reset().eraseScreen().cursor(1, 1));
        System.out.print("=======================================================================");
		FileInputStream f = new FileInputStream(file);
        int c;
        while( (c=f.read())>=0 ) {
        	System.out.write(c);
        }
        f.close();
        System.out.println("=======================================================================");
	}
	
}
