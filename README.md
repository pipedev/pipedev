pipedev
=======
This is the public workflow repository for the Pipeline Development and Evaluation (PDE) Working Group at [Ontario Institute for Cancer Research](http://www.oicr.on.ca) (OICR).

The PDE has two primary mandates:

* Develop [SeqWare](http://seqware.io) workflows and pipelines for the Sequencing Production Group
* Evaluate and verify software and pipelines that will be used by Sequencing Production Bioinformatics

This repository houses the open-source workflows, deciders and utilities that are written and maintained by our group.

Binaries
________

Our utilities are available from our Maven repository. Add the appropriate repository to your pom.xml.

* Releases: (seqware-dependencies)[http://seqwaremaven.oicr.on.ca/artifactory/simple/seqware-dependencies]
* Nightly snapshots: (seqware-dependencies-snapshot)[https://seqwaremaven.oicr.on.ca/artifactory/simple/seqware-dependencies-snapshot]


Build
-------
To build our utilities, use Maven.

```bash
mvn install:install -f utility-modules-pom.xml
```
Afterwards, they will be available in your local Maven repository.

```html
<dependency>
    <groupId>ca.on.oicr.pde</groupId>
    <artifactId>common-utilities</artifactId>
    <version>1.1-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>ca.on.oicr.pde</groupId>
    <artifactId>decider-utilities</artifactId>
    <version>1.4.3-SNAPSHOT</version>
</dependency>

<dependency>
    <groupId>ca.on.oicr.pde</groupId>
    <artifactId>workflow-utilities</artifactId>
    <version>1.3-SNAPSHOT</version>
</dependency>
```


