#Licenses

##Introduction

If there are legal issues with the contents of the repository or the web page, please contact me via <a href="mailto:marcus.handte@googlemail.com">email</a> before contacting your lawyer. Without further ado, we will gladly remove whatever is necessary to make you happy. 

We have tried to remove all code and as many resources as possible that were not created by someone from the team but chances are that we have missed something and if there is some artifact created by you, please let us know, so we can remove it from the repository or add the corresponding license to this page.

##The Intention

Until recently, the BASE code that you find in this repository was solely used internally within a couple of research projects. Since the code has become more mature over time, we would like to share the results with everyone for free without strings attached. 

##Our License

Consequently, we have decided to use a BSD license for everything developed by us. By using our code, you accept the following:

```
Copyright (c) 2006-2009, Universität Stuttgart, IPVS
Copyright (c) 2010, Universität Duisburg-Essen, NES

All rights reserved.

Redistribution and use in source and binary forms, 
with or without modification, are permitted provided 
that the following conditions are met:

Redistributions of source code must retain the above 
copyright notice, this list of conditions and the 
following disclaimer.

Redistributions in binary form must reproduce the 
above copyright notice, this list of conditions and 
the following disclaimer in the documentation and/or 
other materials provided with the distribution.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS 
AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED 
WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A 
PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL 
THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY 
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR 
CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF 
USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) 
HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING 
NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF 
THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE 
POSSIBILITY OF SUCH DAMAGE.
```

In general, this license applies to all code that you can find within the packages under *info.pppc*. Note that this does not include the image resources as they are - for the most part - taken from Eclipse and adapted to display nicely on a WM6 device.

Furthermore, this also does not apply to the code that you can find in the vendor subfolder which - at the moment - contains a copy of the Bouncycastle 1.46 lightweight API. The reason for including this code in the repository at all is the lack of a Maven build for this specific distribution of Bouncycastle.

##Other Licenses

As stated previously, the image resources contained in the swt and lcdui packages are adapted icons from the Eclipse distribution which is - to the best of our knowledge - distributed under the [Eclipse Public License](http://www.eclipse.org/legal/epl-v10.html).

The bouncycastle code is - to the best of our knowledge - distributed under an [MIT X11 License](http://www.bouncycastle.org/licence.html) but apparently, there are also patents on some of the algortihms in case they are used in commercial products.

The SunSPOT libraries - to which the SunSPOT related subprojects including the fastecc extension are linking - are - to the best of our knowledge - licensed under the [GNU Public License V2](http://java.net/projects/spots-network-library/). Consequently, you might not be able to use them in a commerical closed source application.

The CLDC and MIDP stubs - that are used to compile most of the code in such a way that it executes everywhere - are - to the best of our knowledge - licensed under the [GNU Lesser Public License V3.0](http://mcpat.github.com/java-microedition-libraries/).

The Bluecove library - that is used to compile the JSR-82 Bluetooth plug-in and included in the Bluetooth example projects - is - to the best of our knowledge - licensed under [Apache License, V2](http://bluecove.org/).

##Disclaimer

The above description may not be correct, so please re-check this to be sure. We are trying to be exact here but we cannot take responsibility for whatever happens if there is a flaw.