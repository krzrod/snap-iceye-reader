Project name: SNAP ICEYE reader
Type: Plugin  
Main contact: ahmad.hamouda (at) iceye.fi

* Introduction

This plugin is used inside snap desktop application to map our image format to snap format.  
This plugin build as per snap development extension module documentation "https://senbox.atlassian.net/wiki/spaces/SNAP/pages/10879037/How+to+develop+an+extension+module".

* Compilation & dependencies

In order to compile and execute the current code for development  
purposes [debug], execute:

    - Change TESTING_IMAGE_PATH variable at TestIceyeReader
    - Set application enviroments as per "https://senbox.atlassian.net/wiki/spaces/SNAP/pages/10879037/How+to+develop+an+extension+module"
    - Run test case in debug mode

In order to attach code for debugger mode:

    - Create Jar configuration with the following parameters: 
        - Jar path:	<your snap desktop installation directory>/snap/modules/ext/org.esa.snap.snap-rcp/org-esa-snap/snap-main.jar
        - VM options:	-Dsun.java2d.noddraw=true -Dsun.awt.nopixfmt=true -Dsun.java2d.dpiaware=false -Dorg.netbeans.level=INFO -Xmx8G
        - Program arguments:	--userdir "<your .snap path usualy at home>/.snap/system"
        - Working directory:	<your snap desktop installation directory>
        - JRE:	1.8

* Unit Testing

To run unit tests, execute:

    - change TESTING_IMAGE_PATH variable at TestIceyeReader

    mvn clean install

To build without testing

    mvn clean install -DskipTests=true;

* Integration Testing

    - Download SNAP desktop version 7.0.0 "http://step.esa.int/main/download/"
    - You can update the application if you want
    - Add plugin:
        - Tools -> Plugins -> Downloaded -> add plugin -> slect pulgin from your target file [.pem]
    - Open image:
        - File -> Import -> SAR sensors -> Iceye-Product


* Useful urls
    - Snap git hub:https://github.com/senbox-org/s1tbx

# Plugin update for COG TIffs
* Installation

SNAP's plugins are NetBeans modules (.nbm file) which are built using "nbm-maven-plugin" in maven


Installation of the plugin did not change since previous version but new plugin needs one additional prerequisite.  
Since it uses the new "s1tbx" dependencies with version 10.0-SNAPSHOT, we need to build them too and install together with plugin:

1. Clone s1tbx repository from https://github.com/senbox-org/s1tbx
2. Build it with maven
3. Copy below three artifacts to same folder as the plugin (this step is not needed but convenient)
    4. s1tbx-commons
    5. s1tbx-io
    6. s1tbx-cloud

When everything is prepared, steps to installation are:

       - Open SNAP software
       - (Optional) - if reinstalling the new plugin, first delete current version (Menu > Tools > Installed > Select "New Iceye Plugin" > Uninstall
       - Menu Tools > Plugin > Downloaded
       - Add Plugins > Select the s1tbx dependencies and your built plugin file
       - Click "Install"

* Opening files
    * GRD
        * Menu > Open Product > Select file > Select plugin to use
    * SLC
        * Menu > Import > SAR Sensors > ICEYE-NEW > Select file > Select reader plugin again


### Known issue with opening COG files using new plugin:
Sometimes, after installing new plugin and using it to open a product, for some reason the old plugin is called and fails for new format so the product is not added to SNAP.  
This is shown as an error in the bottom right corner of SNAP, with error saying "cannot read rasterData.getWidth() cause product is null".
Several things were changed in order to force SNAP to use new plugin including:
- Changing artifact Id in maven
- Changing version
- Changing classes packages
- Chaning the reader package in "META-INF.services"
- Changing the manfiest version in "manifest.mf"

Unfortunately, it did not help. Workaround solution for that is
- Uninstall plugin (explained above)
- Restart SNAP
- Install plugin again
- Restart SNAP
- Close SNAP
- Delete folders in %AppData%/SNAP/var
- Start SNAP again
  Please note, this did not work 100% of times so sometimes, another "var" folder removal, SNAP restart and/or another re-installation was required