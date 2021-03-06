#Basic Deployment

##Introduction

This page provides howtos for deploying BASE on different devices.

##Personal Computer

Running the middleware on a PC is as simple as running any other Java program. That means that you need to install a virtual machine which you can get for free. Please note that some of the libraries that are used by some extensions of BASE require native libraries. For example, the bluetooth transceiver requires the JSR-82 specification which is implemented by the Bluecove Sourceforge project. This library requires an appropriate dll. In order to enable the virtual machine to find this DLL, you can either add the directory with the dll to the PATH variable or you must set the "java.libaray.path" variable via the "-D" switch, e.g. "-Djava.library.path="`<DIRECTORY>`".

##Windows Mobile PDA

Running the middleware on a PDA requires a virtual machine such as J9, for example. In order to use J9, you need unzip the file on the PDA. I would recommend using the CDC with foundation profile. Next, you need to create a JAR with the middleware and copy it to the PDA. Finally, you need to create a launcher link that you place, for example, in the "\Windows\Programs" directory. The link file should end with the extension '''.lnk''' and it should contain plain text that looks as follows: '''237#"\`<PATH_TO_J9>`\j9.exe" "-cp" "\`<PATH_TO_JAR>`\yourcode.jar" "-jcl:foun10" "`<CLASSNAME_TO_START>`"'''. Please note that there is a limit on the number of characters that you can put in a '''.lnk''' file. I believe it was 256 characters or something like that. So it might make sense not to use a very deep directory structure.

##SunSPOT

Running the middleware on a sunspot requires a sunspot development kit. This can be installed automatically via the sunspot manager which is a Java webstart application. The sunspot manager will also install apache ant and netbeans. After installing the development kit, simply connect the sunspot and the driver should install automatically (on windows x86, on windows x64 you need to modify the sunspot.inf file in \windows\inf). Once the sunspot is connected, you can simply deploy the bundle consisting of your application and the middleware using netbeans. The details of this process are described within the developers guide that can be found in the doc directory of the sdk.
= Note on deploying on Sunspot =
Use following command to deploy the jar on sunpot:
ant jar-deploy -Dfrom.jar.file="filename.jar" -Dmanifest.name="name of the Manifest file".
Use following command to execute the jar on router:
jamvm -Xbootclasspath:/mnt/usb/usr/share/jamvm/classes.zip:/mnt/usb/usr/share/classpath/glibj.zip -jar filename.jar


##J2ME Phone (e.g. Symbian, ...)

Running the middleware on a phone with J2ME support requires the application to be packaged according to the Midlet specification. This can be done using the Mobile Tools for Java Eclipse plug-ins. To test the midlet on your development workstation, you should install the J2ME Toolkit from Sun. The Mobile Tools for Java integrate with the emulator provided by the toolkit which greatly simplfies debugging. After installing the plug-ins, create a new midlet project, add a new midlet and load the BASE code within the start method of the midlet. In addition, you may want to load the LCD ui to get a console and some simple tools for connectivity testing. The deployment of the package on the device is usually a device-specific process that should be documented by the manufacturer. For Nokia mobile phones, you usually have to install the "phone suite application" that is provided by Nokia to synchronize the contents of the phone with a pc. Once installed, you can typically click on a Java Application Descriptor (.jad) file in order to install a midlet suite. With the Mobile Tools for Java, you can create the JAD file and the assoicated JAR file by clicking on the "Create Package" option. This will create a new folder in the project called "deploy" (folder name may be changed in the preference window). Once again, the actual process of how you can get the JAD and the JAR on the phone are phone-specific. 

##Android Phone (>= 1.6)

The actual deployment of code on Android devices can be done by a click of a button using the Android SDK and the Eclipse ADT plug-ins. Running the middleware on Android is not straight forward due to the Android application model. Android differenciates between Activities (something like a UI element) and services. The lifecycle of activites is tightly controlled by the OS and activities are frequently instanciated and destroyed (e.g. whenever the screen rotates). As a consequence, they are not suitable to actually execute the middleware (unless you want it to be started and stopped frequently). Instead, it is necessary to wrap the middleware as an Android service whose lifecylce can then be controlled by the activites. The '''BASE Android''' project which is available in the code repository shows how this can be done. Once the middleware is wrapped as a service, it can be built and deployed using the ADP plug-ins. 