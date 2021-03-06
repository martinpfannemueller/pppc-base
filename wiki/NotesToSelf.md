#Notes

##Introduction

The notes below are probably only useful for me but as I know that I will not remember the specific details of the solutions below, I rather write them down. 

##Eclipse Update Site vs. Tycho

Tycho relies on P2 repositories. Thus, it requires a proper version of artifacts.xml and contents.xml on the update site. This command line can generate the two files from a local copy of the update site:

```
java -jar C:/Progra~1/Java/Eclipse*/org.eclipse.equinox.launcher_*.jar 
  -application org.eclipse.equinox.p2.publisher.FeaturesAndBundlesPublisher 
  -metadataRepository file:///c:/sitecopy 
  -artifactRepository file:///c:/sitecopy
  -source c:/sitecopy 
  -publishArtifacts
```

Once the metadata is updated, it is *important to clear out the cache* in the local maven repository. Otherwise, the problem persists.

##Eclipse PDE vs. Target Platform

When working on the Eclipse plug-ins, PDE will not automatically chose the proper target platform definition. *It seems necessary to manually switch between definitions using the Eclipse preference dialog.* Changes to target definitions are only reflected after switching from one to another and back. The reload button seems not to work properly.

##Maven Deployment Plug-in

Instead of using WebDAV for deployment it is better to directly deploy to an SVN working copy. This can be done by specifying an alternate deployment repository that points to the releases or snapshots folder in the working copy.

```
mvn deploy -DaltDeploymentRepository=pppc-base::default::file:///path/to/working/copy/maven/releases
```

##Maven Versions Plug-in

Instead of manipulating all pom files consistently by hand, it is much easier to use the versions plugin to update the versions of a multimodule project.

```
mvn versions:set -DnewVersion=1.0
```