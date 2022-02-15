VuHeideltimeWrapper
============
version 1.1
Copyright: VU University Amsterdam
email: piek.vossen@vu.nl


### DESCRIPTION
This module parses KAF or NAF files and creates a data structure that can be accessed and modified.
It also serializes KAF or NAF to a file again.

SOURCE CODE:

https://github.com/cltl/vuheildetimewrapper

The binaries can be built using maven and the pom.xml

> mvn clean install

This will create an executable jar file under `./target/vuheideltimewrapper.jar`.
### REQUIREMENTS
VuHeideltimeWrapper is developed in Java 1.8 and can run on any platform that supports Java 1.8


### USAGE
The code accepts NAF input from files, folders or the input stream:

* File to file processing:
```java -jar target/vuheideltimewrapper.jar -i <input-file> -o <output-path>```
  * Note that the output path may be a file or a folder; in that case, the processed file has the same name as the input file.
  
* Folder to folder processing:
```java -jar target/vuheideltimewrapper.jar -i <input-dir> -o <output-dir>```
* From input to output stream:
```java -jar target/vuheideltimewrapper-1.1.jar -s < <input-file>```

Additionally, 
* the option `r`, or `replace`, allows to replace an existing timex layer;
* the code uses a config file located at `./conf/wrapper.props`; the option `c`, or `config`, allows to specify a different path.

#### New in Version 1.1
The code is agnostic about input and output file extensions, and input and output files may have any name and extension. On the other hand,
it is only possible to specify an output-file name when processing from file to file, as file names are copied when processing from folder 
to folder.

### LICENSE
    VuHeideltimeWrapper is free software: you can redistribute it and/or modify
    it under the terms of the The Apache License, Version 2.0:
        http://www.apache.org/licenses/LICENSE-2.0.txt.

    VuHeideltimeWrapper is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
