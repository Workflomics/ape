### APE 2.6.0 - Snakemake, Partial Workflows and Major Refactorings

- Maven Group change to `org.workflomics`
- Snakemake output support by @CGru21
- Added configuration for explicitly accepting or skipping `partial` outputs, i.e. outputs where the executed command is unknown by @eladrion
- Major refactoring
- Test resources improvements
- Markdown documentation updates (Releases, Maven Bundles, Security contacts, general contacts)

### APE 2.5.3 - CWL Support Improvements, Bugfixes

- Introduce a new CWL parser and related classes to support APE semantic annotations embedded into the CWL files.
- Bugfixes in CNF handling output numbering
- Bump commons-lang3 to 3.19.0
- Documentation Updates

### APE 2.4.0 - CLI Extensions, Template Enhancements

- CLI is extended. It now allows users to automatically convert tools from bioinformatics domain (specifically `bio.tools`), using methods: `pull-a-tool`, `convert-tools` and `bio.tools` (The previous interface only allowed workflow synthesis while we are now using the command synthesis).
- Included more constraint templates
- Migrate to Java 17

### APE 2.3.0 - CWL Improvements and General Code Improvements

#### CWL Support Enhancements

- Removed full CWL implementation support within the annotation file due to maintainability challenges and insufficient examples.
- Introduced a `cwl_reference` field for direct linkage to CWL implementation files of tools, facilitating easier access and usability.

#### Code and Documentation Improvements

- Conducted a comprehensive refactor of the codebase, including improvements in naming conventions to enhance code clarity and quality.
- Updated the project's security guidelines to ensure compliance with current best practices.

#### Minor Fixes and Updates

- Implemented small fixes to address issues and improve overall functionality.
- Updated the releases list to keep users informed of new versions and available features.

### APE 2.2.6 - General Enhancements

- Added `SECURITY.md`
- Build process updates
- Enhancements of Badges (e.g. `fair-software`)
- API methods improvements

### APE 2.2.0 - Code refactor and Taverna style workflow outputs

- Fix `CITATION.cff` formatting and update the file
- Refactor code and implement the Taverna style workflow outputs
- Fix JavaDoc error
- Update `README.md` - remove a badge

### APE 2.1.8 -  Improved CWL generation and code refactor

- Create `codeql.yml`
- Update constraint formats and annotation labels
- Introduce constraints APERunConfig tag & refactor
- Update `README.md`
- Fix build badge
- CWL fixes and dependencies update
- Create `CITATION.cff`

### APE 2.1.5 - Support executable CWL outputs

- Configuration files can be provided as URLs and can contain URLs
- CWL files are now executable
- CWL input file (`.yml`) is also provided
- Tests are improved
- Refactoring


### APE 2.0.3 - Improved SLTLx Templates and direct encoding

- Added new NL templates for SLTLx
- Added encoding runtime message
- Fixed issue with cache of SLTLx specified constraints on synthesis re-run

### APE 2.0.2 - New SLTLx Templates, added workflow output dependency

- Added new constraint templates
- Workflows no longer use provided inputs as outputs

### APE 2.0.0 - Support for SLTLx

APE formalism is extended to support the SLTLx logic. The solving engine is still a SAT solver.
The project is restructured and it includes a parser for the underlying logic.
Problem constraints can now be specified using constraints as well as directly in SLTLx.

### APE 1.1.12 - log4j version fix Latest
- Changed log4j version dependency to 2.17.0, due to security issues.
- Updated other mvn dependencies

### APE 1.1.11 - DOI release
- Fix needed to obtain the DOI

### APE 1.1.9 - Export abstract and executable CWL
- Export abstract and executable CWL
- Improved synthesis execution flags



### APE 1.1.8 - Improved Class structure and naming
Improved class structure and naming to support extensions with additional solving techniques. The new structure allows for easy extensions with other (non-SAT) solvers.



### APE 1.1.7 - Adding synthesis execution flags
- Added synthesis execution flag (class SATsolutionsList)
- Improved SolutionWorkflow class by adding methods for PNG retrieval of the workflows (e.g., getDataflowGraphPNG(..))



### APE 1.1.5 - Adding long predicate labels
- Added method PredicateLabel.getLongLabel()
- Improved ModuleNode.getNodeFullLabel() method and renamed to ModuleNode.getNodeLongLabel()



### APE 1.1.4 - Improving SolutionWorkflowNode (updated)
- Introduced method SolutionWorkflowNode.getNodeFullLabel()
- Improved method descriptions


### APE 1.1.3 - Improved synthesis run handling
Implemented:

- Cleaning temp files used for synthesis encoding after each SAT run
- Introduced "timeout(sec)" configuration field


### APE 1.1.2 - Constraint parsing fix
Parsing of constraints got fixed. The error occurred in case of defining constraints over concrete operations that are not part of the ontology.


### APE 1.1.1 - Strict taxonomy hiararchy
Improvements:

- Implemented stricter tool annotations and added the corresponding core configuration field (strict_tool_annotations)
- Removed message passing approach
- Improved APE API (added new methods and improved documentation)


### APE 1.0.3 - Interface improvements
- Provided interface for building constraints from JSON objects
- Tested new functionalities
- Improved constraint descriptions (ConstraintTemplate in ConstraintFactory)
- Improved constraint printouts in debug mode


### APE 1.0.2 - constraint improvements
- Improved constraint formatting
- Refactored interface for auxiliary predicated
- Improved documentation and testing


### The first stable version of APE 1.0.1
APE is a command line tool and Java API for the automated exploration of possible computational pipelines (scientific workflows) from large collections of computational tools.

The first stable version of the software includes:

- APE-1.0.1.jar (the library jar)
- APE-1.0.1-executable.jar (command line executable jar, that includes all the dependencies)
- APE-1.0.1-javadoc.jar
- APE-1.0.1-sources.jar

