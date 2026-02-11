<img src="https://raw.githubusercontent.com/sanctuuary/.github/main/logos/APE-logo.png" alt="logo" width=20% />

# APE (Automated Pipeline Explorer)

| Badges |                                                                                                                                                                                                                                                                                                                                                                                                                                     |
|:----:|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| **Fairness** | [![fair-software.eu](https://img.shields.io/badge/fair--software.eu-%E2%97%8F%20%20%E2%97%8F%20%20%E2%97%8F%20%20%E2%97%8F%20%20%E2%97%8F-green)](https://fair-software.eu) [![OpenSSF Best Practices](https://bestpractices.coreinfrastructure.org/projects/5749/badge)](https://www.bestpractices.dev/projects/5749)                                                                                                              |
| **Packages and Releases** | [![Latest release](https://img.shields.io/github/release/workflomics/ape.svg)](https://github.com/workflomics/ape/releases/latest) [![Maven Central](https://badgen.net/maven/v/maven-central/org.workflomics/APE)](https://mvnrepository.com/artifact/org.workflomics/APE) [![Static Badge](https://img.shields.io/badge/RSD-APE-ape)](https://research-software-directory.org/software/ape-automated-pipeline-explorer) 
| **Build Status** | [![Build](https://github.com/workflomics/ape/actions/workflows/mvnbuild.yml/badge.svg)](https://github.com/workflomics/ape/actions/workflows/mvnbuild.yml) [![CodeQL Analysis](https://github.com/workflomics/ape/actions/workflows/codeql.yml/badge.svg)](https://github.com/workflomics/ape/actions/workflows/codeql.yml)                                                                                                         |
| **Documentation** | [![Documentation Status](https://readthedocs.org/projects/ape-framework/badge/?version=latest)](https://ape-framework.readthedocs.io/en/latest/?badge=latest)                                                                                                                                                                                                                                                                       |
| **DOI** | [![DOI](https://zenodo.org/badge/227861653.svg)](https://zenodo.org/badge/latestdoi/227861653)                                                                                                                                                                                                                                                                                                                                      |
| **License** | [![GitHub license](https://img.shields.io/github/license/workflomics/ape)](https://github.com/workflomics/ape/blob/master/LICENSE)                                                                                                                                                                                                                                                                                                  |



APE is a command line tool and Java API for the automated exploration of possible computational pipelines (scientific workflows) from large collections of computational tools. Generated workflows can be exported in [CWL](https://www.commonwl.org/) or [Snakemake](https://snakemake.readthedocs.io/en/stable/) format, as well as in graphical (PNG, SVG) formats or plain shell scripts.

APE relies on a semantic domain model that includes tool and type taxonomies as controlled vocabularies for the description of computational tools, and functional tool annotations (inputs, outputs, operations performed) using terms from these taxonomies. Based on this domain model and a specification of the available workflow inputs, the intended workflow outputs and possibly additional constraints, APE then computes possible workflows. 

Internally, APE uses a component-based program synthesis approach. It translates the domain knowledge and workflow specification into logical formulas that are then fed to a SAT solver to compute satisfying instances. These solutions are then translated into the actual candidate workflows. For a detailed description, we refer to [[1]](#1).

For our paper at [ICCS 2020](https://www.iccs-meeting.org/iccs2020/) [[2]](#2) we created a video that explains APE in 5 minutes:
<div align="left">
  <a href="https://www.youtube.com/watch?v=CzecqRJXmoM" target="_blank"><img src="https://github.com/Workflomics/workflomics-QCmetric/assets/11068408/6c699a2b-7fce-44a4-bf1f-ad11a865c6c3" alt="APE - Youtube video" width=40%></a>
</div>

**For detailed information please visit [our page](https://ape-framework.readthedocs.io).**

## Requirements

To [run](https://github.com/workflomics/ape#how-to-run-ape-from-the-command-line) APE locally you need to have [Java 1.8](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html) (or higher) installed on your system (use the command `$ java -version` to check your local version).

To [build](https://github.com/workflomics/ape#build-ape-from-source-using-maven) APE from source, [Maven 3.3+](https://maven.apache.org/download.cgi) has to be installed as well (use the command `$ mvn -version` to check your local version).


> **_Note:_**  Building APE from source is not required to run it, as the latest stable version is available at [maven repository](https://mvnrepository.com/artifact/org.workflomics/APE/latest).

### Releases 

- [Latest](https://github.com/workflomics/ape/releases/latest)
- [All releases](https://github.com/workflomics/ape/releases)


### Add APE to your Maven project

To add a dependency on APE using Maven, use the following:

```xml
<!-- https://mvnrepository.com/artifact/org.workflomics/APE -->
<dependency>
    <groupId>org.workflomics</groupId>
    <artifactId>APE</artifactId>
    <version>2.6.x</version>
</dependency>
```

For information regarding **Gradle**, **Ivy**, etc. we refer to the [APE mvn repository](https://mvnrepository.com/artifact/org.workflomics/APE/latest).

To add a dependency on older versions of APE using Maven, use the following:

```xml
<!-- https://mvnrepository.com/artifact/io.github.sanctuuary/APE -->
<dependency>
    <groupId>io.github.sanctuuary</groupId>
    <artifactId>APE</artifactId>
    <version>2.x.x</version>
</dependency>
```

For information regarding **Gradle**, **Ivy**, etc. we refer to the [APE mvn repository](https://mvnrepository.com/artifact/io.github.sanctuuary/APE/latest).

### Manually download releases

| Date       | Version | Download                                                                                                                                                                                                                                                                                                                                                                                                                 |
|------------|---------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 10-02-2026 | 2.6.0   | [jar](https://repo1.maven.org/maven2/org/workflomics/APE/2.6.0/APE-2.6.0.jar), [executable](https://repo1.maven.org/maven2/org/workflomics/APE/2.6.0/APE-2.6.0-executable.jar), [javadoc](https://repo1.maven.org/maven2/org/workflomics/APE/2.6.0/APE-2.6.0-javadoc.jar), [sources](https://repo1.maven.org/maven2/org/workflomics/APE/2.6.0/APE-2.6.0-sources.jar)                                                     |
| 29-09-2025 | 2.5.3   | [jar](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.3/APE-2.5.3.jar), [executable](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.3/APE-2.5.3-executable.jar), [javadoc](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.3/APE-2.5.3-javadoc.jar), [sources](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.3/APE-2.5.3-sources.jar)         |
| 10-06-2025 | 2.5.2   | [jar](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.2/APE-2.5.2.jar), [executable](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.2/APE-2.5.2-executable.jar), [javadoc](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.2/APE-2.5.2-javadoc.jar), [sources](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.2/APE-2.5.2-sources.jar)         |
| 09-06-2025 | 2.5.0   | [jar](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.0/APE-2.5.0.jar), [executable](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.0/APE-2.5.0-executable.jar), [javadoc](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.0/APE-2.5.0-javadoc.jar), [sources](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.5.0/APE-2.5.0-sources.jar)         |
| 19-02-2024 | 2.3.0   | [jar](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.3.0/APE-2.3.0.jar), [executable](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.3.0/APE-2.3.0-executable.jar), [javadoc](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.3.0/APE-2.3.0-javadoc.jar), [sources](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.3.0/APE-2.3.0-sources.jar)         |
| 17-05-2022 | 2.0.0   | [jar](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.0.0/APE-2.0.0.jar), [executable](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.0.0/APE-2.0.0-executable.jar), [javadoc](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.0.0/APE-2.0.0-javadoc.jar), [sources](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/2.0.0/APE-2.0.0-sources.jar)         |
| 20-12-2021 | 1.1.12  | [jar](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.1.12/APE-1.1.12.jar), [executable](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.1.12/APE-1.1.12-executable.jar), [javadoc](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.1.12/APE-1.1.12-javadoc.jar), [sources](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.1.12/APE-1.1.12-sources.jar) |
| 02-05-2021 | 1.1.7   | [jar](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.1.7/APE-1.1.7.jar), [executable](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.1.7/APE-1.1.7-executable.jar), [javadoc](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.1.7/APE-1.1.7-javadoc.jar), [sources](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.1.7/APE-1.1.7-sources.jar)         |
| 15-07-2020 | 1.0.1   | [jar](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.0.1/APE-1.0.1.jar), [executable](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.0.1/APE-1.0.1-executable.jar), [javadoc](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.0.1/APE-1.0.1-javadoc.jar), [sources](https://repo.maven.apache.org/maven2/io/github/sanctuuary/APE/1.0.1/APE-1.0.1-sources.jar)         |

The complete list of releases can be found on the [MVN Repository](https://mvnrepository.com/artifact/org.workflomics/APE).

Older releases (`< 2.6`), can be found in the previous [MVN Repository](https://mvnrepository.com/artifact/io.github.sanctuuary/APE).

### Build APE from source (using Maven)

From the project root, simply launch

```shell
$ mvn -DskipTests=true install
```

to build the APE modules from the source tree and the built files will be generated under the `/target` directory. All the dependencies will be gathered by Maven and the following stand-alone module will be generated: `APE-[latest]-executable.jar`

## Using APE

Automated workflow composition with APE can be performed through its command line interface (CLI) or its application programming interface (API). While the CLI provides a simple means to interact and experiment with the system, the API provides more flexibility and control over the synthesis process. It can be used to integrate APE’s functionality into other systems.

### How to run APE from the command line

**APE-[latest]-executable.jar** is available in [maven repository](https://mvnrepository.com/artifact/org.workflomics/APE/latest).

When running APE-[latest]-executable.jar from the command line, it requires a JSON configuration file given as a parameter and executes the automated workflow composition process accordingly:

```shell
java -jar APE-[latest]-executable.jar [path-to-ape-configuration]
```

The configuration file (see [APE cofiguration example](https://github.com/sanctuuary/APE_UseCases/blob/master/ImageMagick/Example1/config.json) and [APE configuration documentation](https://ape-framework.readthedocs.io/en/latest/docs/specifications/domain.html#configuration-file)) provides references to all therefor required information:
1. *Domain model* - classification of the types and operations in the domain in form of an **ontology** (see [ontology example](https://github.com/sanctuuary/APE_UseCases/blob/master/ImageMagick/imagemagick_taxonomy.owl) in OWL) and a **tool annotation file** (see [tool annotations example](https://github.com/sanctuuary/APE_UseCases/blob/master/ImageMagick/tool_annotations.json) in JSON).
2. *Workflow specification* - including a list of **workflow inputs/outputs** and template-based (see [constraint templates](https://ape-framework.readthedocs.io/en/latest/docs/specifications/constraints.html#constraint-templates)) **workflow constraints** (see [workflow constraints example](https://github.com/sanctuuary/APE_UseCases/blob/master/ImageMagick/Example1/constraints.json))
3. *Parameters* for the synthesis execution, such as the number of desired solutions, output directory, system configurations, etc. (see [APE configuration documentation](https://ape-framework.readthedocs.io/en/latest/docs/specifications/domain.html#configuration-file)).

#### My first APE run

```shell
git clone git@github.com:sanctuuary/APE_UseCases.git
```

or

```shell
git clone https://github.com/sanctuuary/APE_UseCases.git
```

Download the latest version of [`APE-[latest]-executable.jar`](https://repo.maven.apache.org/maven2/org/workflomics/APE/) and add it to the APE_UseCases directory (`~/git/APE_UseCases`)

```shell
cd ~/git/APE_UseCases
java -jar APE-[latest]-executable.jar ImageMagick/Example1/config.json
```

See [ImageMagick: Example 1](https://github.com/sanctuuary/APE_UseCases/tree/master/ImageMagick/Example1) for more information about the results and on how to execute the composed workflow.

#### More examples

[Use cases](https://github.com/sanctuuary/APE_UseCases/) contains all the details and **examples** regarding the composition setup and the composition execution using the existing use cases (such as [composition of ImageMagick operations](https://github.com/sanctuuary/APE_UseCases/tree/master/ImageMagick)).

### How to use the APE API

Like the CLI, the APE API relies on a configuration file that references the domain ontology, tool annotations, workflow specification and execution parameters:

```java
// set up the framework
APE ape = new APE("path/to/setup-configuration.json");

// run the synthesis
SATsolutionsList solutions = ape.runSynthesis("path/to/run-configuration.json");
// write the solutions for the file system
APE.writeSolutionToFile(solutions);
APE.writeDataFlowGraphs(solutions, RankDir.TOP_TO_BOTTOM);
APE.writeExecutableWorkflows(solutions);
```

However, the API allows to generate and edit the configuration file programmatically:

```java
// set up the framework
APECoreConfig coreConfig = new APECoreConfig(...);
APE ape = new APE(coreConfig);

// run the synthesis
APERunConfig runConfig = APERunConfig.builder().withSolutionMinLength(1).withSolutionMaxLength(10)
                                                .withMaxNoSolutions(100).withApeDomainSetup(ape.getDomainSetup())
                                                .build();
SATsolutionsList solutions1 = ape.runSynthesis(runConfig);

// run the synthesis again with altered parameters
runConfig.setUseWorkflowInput(ConfigEnum.ONE);
SATsolutionsList solutions2 = ape.runSynthesis(runConfig);
```

For more information see [APE javadoc.io](https://javadoc.io/doc/org.workflomics/APE/latest/nl/uu/cs/ape/sat/APE.html) or
the [APE (< 2.6) javadoc.io](https://javadoc.io/doc/io.github.sanctuuary/APE/latest/nl/uu/cs/ape/sat/APE.html) page.


## APE v2 architecture

The architecture of the APE v2 library is presented in the following figure. Components coloured light blue extend existing components in the APE v1 framework; dark blue components are new modules.

<img src="https://user-images.githubusercontent.com/11068408/164122545-8fda998d-ca5a-4b4d-a662-5f37a9129314.png" alt="APE 2.0 Architecture" width=50%>

## APE Web

Graphical Web Interface for the APE library is available at [APE Web](https://github.com/Workflomics/APE-Web).

## Use Cases and Demos

Our use cases are motivated by practical problems in various domains (e.g. bioinformatics, GIS [[3]](#3)). Different examples are available at the [APE Use Cases Repository](https://github.com/sanctuuary/APE_UseCases).

## Contact

For any questions concerning APE please get in touch with Anna-Lena Lamprecht (anna-lena.lamprecht[at]uni-potsdam.de).

## Contributions

We welcome all contributions (bug reports, bug fixes, feature requests, extensions, use cases, etc.) to APE.
Please get in touch with Anna-Lena Lamprecht (anna-lena.lamprecht[at]uni-potsdam.de) to coordinate your contribution.
We expect all contributors to follow our [Code of Conduct](https://github.com/workflomics/ape/blob/master/CODE_OF_CONDUCT.md).

In case you have a specific request, want to report a bug or suggest a new constraint template please make an issue [here](https://github.com/workflomics/ape/issues/new).

## Credits

APE has been inspired by the [Loose Programming framework PROPHETS](http://ls5-www.cs.tu-dortmund.de/projects/prophets/index.php). It uses similar mechanisms for semantic domain modelling, workflow specification and synthesis, but strives to provide the automated exploration and composition functionality independent from a concrete workflow system.

We thank our brave first-generation users for their patience and constructive feedback that helped us to get APE into shape. 

## License

APE is licensed under the [Apache 2.0](https://github.com/workflomics/ape/blob/master/LICENSE) license.

### Maven dependencies


#### Apache dependencies

1. [**Commons Lang**](https://mvnrepository.com/artifact/org.apache.commons/commons-lang3) - Apache 2.0
2. [**Log4j Core**](https://mvnrepository.com/artifact/org.apache.logging.log4j/log4j-core) - Apache 2.0
3. [**Commons IO**](https://mvnrepository.com/artifact/commons-io/commons-io) - Apache 2.0

#### Logging dependencies

1. [**SLF4J API**](https://mvnrepository.com/artifact/org.slf4j/slf4j-api) - MIT
2. [**SLF4J Simple**](https://mvnrepository.com/artifact/org.slf4j/slf4j-simple) - MIT

#### Code Generation dependencies

1. [**Project Lombok**](https://mvnrepository.com/artifact/org.projectlombok/lombok) - MIT
2. [**ANTLR 4 Runtime**](https://mvnrepository.com/artifact/org.antlr/antlr4-runtime) - BSD 3-clause

#### Other dependencies

1. [**OkHttp**](https://mvnrepository.com/artifact/com.squareup.okhttp3/okhttp) - Apache 2.0
2. [**Graphviz Java**](https://mvnrepository.com/artifact/guru.nidi/graphviz-java) - Apache 2.0
3. [**OWL API**](https://mvnrepository.com/artifact/net.sourceforge.owlapi/owlapi-distribution) - LGPL 3.0 or Apache 2.0
4. [**CWL SDK**](https://mvnrepository.com/artifact/org.commonwl/cwlsdk) - Apache 2.0
5. [**JSON In Java**](https://mvnrepository.com/artifact/org.json/json) - Public Domain
6. [**SAT4J Core**](https://mvnrepository.com/artifact/org.sat4j/org.sat4j.core) - EPL 1.0 or LGPL 2.1

#### Test dependencies

1. [**JUnit Jupiter API**](https://mvnrepository.com/artifact/org.junit.jupiter/junit-jupiter-api) - EPL 2.0


## References
<a id="1">[1]</a> 
Kasalica, V., & Lamprecht, A.-L. (2020). 
Workflow Discovery with Semantic Constraints:
The SAT-Based Implementation of APE. Electronic Communications of the EASST, 78(0).
https://doi.org/10.14279/tuj.eceasst.78.1092

<a id="2">[2]</a> 
Kasalica V., Lamprecht AL. (2020) 
APE: A Command-Line Tool and API for Automated Workflow Composition. 
ICCS 2020. ICCS 2020. Lecture Notes in Computer Science, vol 12143. Springer,
https://doi.org/10.1007/978-3-030-50436-6_34

<a id="3">[3]</a> 
Kasalica, V., & Lamprecht, A.-L. (2019). 
Workflow discovery through semantic constraints: A geovisualization case study. 
In Computational science and its applications – ICCSA 2019
(pp. 473–488), Springer International Publishing,
https://doi.org/10.1007/978-3-030-24302-9_53
