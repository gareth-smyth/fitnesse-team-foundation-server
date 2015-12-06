# Fitnesse Team Foundation Server plugin

This is a plugin for the Fitnesse testing engine to enable all changes made in the fitnesse wiki to be checked in to
a Team Foundation Server.  

Based on <https://github.com/amolenaar/fitnesse-git-plugin> and the samples in [Microsoft Visual Studio Team Foundation Server 2012 Software Development Kit for Java](http://www.microsoft.com/en-us/download/details.aspx?id=22616)

## Status

Very very very alpha - just a proof of concept at the moment.  Tests are lacking and pointless.  **Do not use anywhere**

## Build
Build uses gradle so install that and then run `gradle jar` , which will create a build folder and put the new
jar into it.

## Testing
**None**

## Pre-installation
You need to have a TFS based project created on your system which is mapped to your fitnesse folder.  It shouldn't have
to be mapped to the root as the plugin tries to find the project using the mapped local folder.  Any folders that aren't 
mapped won't be checked in.

## Installation
Add the following lines to your `plugins.properties` file in your fitnesse folder  **NOTE THE FORWARD SLASHES IN THE NATIVE_LIB_FOLDER PROPERTY**

    VersionsController=fitnesse.wiki.fs.TfsFileVersionController
    TfsVersionsController.NativeLibFolder=plugins/redist/native
    TfsVersionsController.TfsServerUri=http://localhost:8080/tfs/DefaultCollection/

Copy  
    `./team-foundation-server-sdk-redist/lib/com.microsoft.tfs.sdk-11.0.0.jar`
and  
    `./build/libs/fitnesse-team-foundation-server-1.0-SNAPSHOT.jar`  
into the "plugins" folder in the root of your fitnesse install.  You may need to create the "plugins" folder. 

Copy the
`./team-foundation-server-sdk-redist` folder into the plugins folder in your fitnesse root, and rename
it to `redist`.

So in the end your fitnesse folder should look something like

    FitNesseRoot/   
        All your fitnesse wikis   
    lib/  
        All the normal libs  
    plugins/  
        com.microsoft.tfs.sdk-11.0.0.jar  
        fitnesse-team-foundation-server-1.0-SNAPSHOT.jar  
        redist/  
            lib/  
                com.microsoft.tfs.sdk-11.0.0.jar   
            native/  
                lots of stuff
            redist.txt  
            ThirdPartyNotices.html  
    encrypt.bat  
    encrypt.sh  
    LICENSE  
    plugins.properties  
    README.md  
    startFitnesse.bat   
    startFitnesse.sh  
    
## TODO
* Everything important
* Everything else 
