---
title: 'APE: A Java Library for Automated Exploration of Computational Pipelines'
tags:
  - automated workflow exploration
  - automated workflow composition
  - scientific workflows
  - computational pipelines
  - workflow synthesis
authors:
  - given-names: Vedran 
    surname: Vedran Kasalica
    orcid: 0000-0002-0097-1056
    corresponding: true # Do you want to be the corresponding author, Vedran? Otherwise I would take this over
    affiliation: 3
  - given-names: Mario
    surname: Frank
    orcid: 0000-0001-8888-7475
    corresponding: true
    affiliation: 1
  - given-names: Charlotte
    surname: Grunert
    orcid: 0009-0007-1871-5409
    affiliation: 1
  - given-names: Koen
    surname: Haverkort
    orcid: 0009-0005-4904-0868
    affiliation: 2
  - given-names: Maurin
    surname: Voshol
    affiliation: 2
  - given-names: Anna-Lena
    surname: Lamprecht
    orcid: 0000-0003-1953-5606
    affiliation: 1

affiliations:
 - name: Department of Computer Science, University of Potsdam, 14476 Potsdam, Germany
   index: 1
 - name: Department of Information and Computing Sciences, Utrecht University, 3584 CC Utrecht, Netherlands
   index: 2
 - name: Stichting Health-RI, 3521 AL Utrecht, Netherlands
   index: 3
date: 13 February 2026
bibliography: paper.bib

---
# Summary
<!-- A description of the high-level functionality and purpose of the software for a diverse, non-specialist audience. -->

Scientists across all disciplines frequently combine diverse computational tools into computational pipelines (a.k.a.
scientific workflows) to solve specific data analysis problems.
Software registries provide large collections of computational tools, often with rich metadata that captures their
functional and non-functional properties.
However, finding the right (combinations of) tools for building a problem-specific workflow remains a challenge.

Therefore, we have developed the Automated Pipeline Explorer (APE) [@ape_github; @kasalica2020]
to support workflow developers in systematically exploring the possible pipelines of tools for a given computational
problem. Like a route planner, it takes a starting point (input data), destination (desired outputs) and possibly
additional constraints (such as kinds of operations to use), and returns a set of possible routes (pipelines) that meet
the request. As described in detail in [@kasalica2019], internally the framework uses an extension of the well-known
Linear Temporal Logic (LTL) to encode the workflow specification. This specification is translated into a propositional
logic formula and handed over to a SAT solver, with the resulting solutions representing possible workflows.

Working with APE has three main phases:
1) Modeling of the *domain knowledge*, which provides the base on which APE relies for the exploration of workflows. It comprises lightweight ontologies, used to annotate operations and data from the domain in form of semantic hierarchies, and tool annotations, i.e. input and output dependencies of the tools. Ideally, the modelling is performed by a small group of domain experts.
2) *Problem specification*, where the user describes the data analysis problem to solve using the vocabulary provided by the domain model. A problem specification for APE comprises the workflow inputs, the desired outputs, and constraints that the workflow has to fulfill. The constraints are exposed in the form of fill-in-the-blank templates (such as "Use operation X" or "If tool X is used, tool Y cannot be used subsequently", etc.).
3) Performing the actual *workflow exploration*. Finally, the obtained candidate solutions that satisfy the specification are returned. The solutions can be presented as workflow structures, executable implementations (such as shell scripts) and/or exported into standard workflow formats (such as CWL) [@amstutz_common_2016].


# Statement of need
<!-- A section that clearly illustrates the research purpose of the software and places it in the context of related
     work. This should clearly state what problems the software is designed to solve, who the target audience is, and
     its relation to other work.-->

# State of the field
<!-- A description of how this software compares to other commonly-used packages in the research area. If related tools
     exist, provide a clear “build vs. contribute” justification explaining your unique scholarly contribution and why
     existing alternatives are insufficient. -->

# Software design
<!-- An explanation of the trade-offs you weighed, the design/architecture you chose, and why it matters for your
     research application. This should demonstrate meaningful design thinking beyond a superficial code structure
     description. -->

APE has been inspired by the ideas behind the PROPHETS framework for synthesis-based loose
programming [@Lampre2013; @LaNaMS2010; @StMaFr1993], the automated chaining of scientific web services in
jORCA/Magallanes [@KaMaRT2010], and the AI planning-based automated workflow instantiation in WINGS [@gil_wings:_2011].
However, it sets itself apart from these systems by a clear focus on workflow/pipeline exploration in a stand-alone
library that is independent from any concrete workflow system. Thus, workflow management
systems [@martin_high-level_2009; @cwl_computational_ws] can integrate APE functionality and make automated workflow
exploration available to their users.


# Research impact statement
<!-- Evidence of realized impact (publications, external use, integrations) or credible near-term significance
     (benchmarks, reproducible materials, community-readiness signals). The evidence should be compelling and specific,
     not aspirational. -->

APE has successfully been used for the automated composition of workflows for data analysis in mass spectrometry-based
proteomics [@palmblad_automated_2018] (based on the EDAM ontology [@ison2013edam] and the bio.tools [@ison_tools_2016]
registry as domain models), creation of thematic maps depicting bird movement patterns in the
Netherlands [@kasalicaLamprecht2019] (based on the GMT collection of mapping tools [@wessel_generic_2013]), and creation
of a liveability atlas of Amsterdam [@scheiderCCD2020] (based on standard GIS operations as provided by ArcGIS).
Further applications, in particular in bioinformatics and geosciences, are currently being developed.


# AI usage disclosure

GitHub Copilot was used for

- generating a review and Pull Request Overview for Pull Request [120](https://github.com/Workflomics/ape/pull/120)
- finding and resolving a string formatting [issue](https://github.com/Workflomics/ape/commit/7681cdca3e0356c29770434a4db38a5562f2ba74) (octal versus decimal)
- generating an Exeption class [description](https://github.com/Workflomics/ape/commit/95f704712798474a1a6edeb50921e8ec790b03cc).

No generative AI tools were used in the writing of this manuscript, or the preparation of supporting materials.


# Acknowledgements

The development of APE has been supported by the last author's Westerdijk Fellowship (Utrecht University, Faculty of Science)
and Open Science Community Utrecht Faculty Ambassadorship (Utrecht University, Open Science Program).
We are grateful to our users and collaborators for their continuous feedback that helps us improving APE.

Also, we are grateful for contributions by Michael R. Crusoe, especially the support in [integrating](https://github.com/Workflomics/ape/pull/138) the `cwl2java`
package in APE.

# References
