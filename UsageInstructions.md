# Introduction #

To simplify the usage of BASE, we are using [Maven](http://maven.apache.org/) to distribute a pre-compiled version of all of its libraries. Since BASE is not part of the public Maven repository, we have created a Maven repository in the Subversion repository. This allows you to either download the libraries manually or to use Maven's repository configuration to have them downloaded automatically when needed.

# Usage with Maven #

If you are already using Maven to develop your software, the only thing that you need to do is to tell Maven where to get the BASE binaries. To do this, just add the following definitions to your `pom.xml` file.

```
<project xmlns="http://maven.apache.org/POM/4.0.0" 
    xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" 
    xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
    http://maven.apache.org/xsd/maven-4.0.0.xsd">
  ...
  <repositories>
    <repository>
      <id>releases.pppc-base.googlecode.com</id>
      <name>3PC BASE Releases</name>
      <url>http://pppc-base.googlecode.com/svn/maven/releases</url>
      <releases>
        <enabled>true</enabled>
        <updatePolicy>never</updatePolicy>
      </releases>
      <snapshots>
        <enabled>false</enabled>
      </snapshots>
    </repository>
  </repositories>
</project>
```

By replacing `releases` with `snapshots` and enabling the snapshots, you can use the latest snapshot.


# Usage without Maven #

If you do not want to use Maven, you can either get the source code from the Subversion repository or alternatively, you can manually download the binaries from the [Maven repository](http://pppc-base.googlecode.com/svn/maven). Note that all projects also contain the source jars which you might want to download as well to be able to read the documentation directly from within your IDE.