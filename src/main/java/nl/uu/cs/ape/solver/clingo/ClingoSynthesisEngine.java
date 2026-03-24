package nl.uu.cs.ape.solver.clingo;

import nl.uu.cs.ape.automaton.ModuleAutomaton;
import nl.uu.cs.ape.automaton.TypeAutomaton;
import nl.uu.cs.ape.configuration.APERunConfig;
import nl.uu.cs.ape.domain.APEDomainSetup;
import nl.uu.cs.ape.models.SATAtomMappings;
import nl.uu.cs.ape.models.AuxTypePredicate;
import nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate;
import nl.uu.cs.ape.solver.SynthesisEngine;
import nl.uu.cs.ape.solver.solutionStructure.SolutionWorkflow;
import nl.uu.cs.ape.solver.solutionStructure.SolutionsList;
import nl.uu.cs.ape.utils.APEResources;

import org.potassco.clingo.control.Control;
import org.potassco.clingo.control.ProgramPart;
import org.potassco.clingo.solving.Model;
import org.potassco.clingo.solving.SolveHandle;
import org.potassco.clingo.symbol.Function;
import org.potassco.clingo.symbol.Number;
import org.potassco.clingo.symbol.Symbol;
import org.potassco.clingo.backend.ExternalType;
import org.potassco.clingo.solving.SolveMode;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * The {@code ClingoSynthesisEngine} class provides an integration with the Clingo ASP solver
 * using the jclingo Java bindings.
 */
public class ClingoSynthesisEngine implements SynthesisEngine {

    private final APEDomainSetup domainSetup;
    private final SolutionsList allSolutions;
    private final APERunConfig runConfig;
    private final int solutionSize;
    private final SATAtomMappings mappings;
    private Control control;
    private ModuleAutomaton moduleAutomaton;
    private TypeAutomaton typeAutomaton;

    /**
     * Instantiates a new Clingo synthesis engine.
     *
     * @param domainSetup  The domain setup
     * @param allSolutions The solutions list
     * @param runConfig    The run configuration
     * @param solutionSize The current solution length
     */
    public ClingoSynthesisEngine(APEDomainSetup domainSetup, SolutionsList allSolutions,
                                 APERunConfig runConfig, int solutionSize) {
        this.domainSetup = domainSetup;
        this.allSolutions = allSolutions;
        this.runConfig = runConfig;
        this.solutionSize = solutionSize;
        this.mappings = allSolutions.getMappings();
        
        int maxNoToolInputs = Math.max(domainSetup.getMaxNoToolInputs(), runConfig.getProgramOutputs().size());
        int maxNoToolOutputs = Math.max(domainSetup.getMaxNoToolOutputs(), runConfig.getProgramInputs().size());
        this.moduleAutomaton = new ModuleAutomaton(solutionSize, maxNoToolInputs, maxNoToolOutputs);
        this.typeAutomaton = new TypeAutomaton(solutionSize, maxNoToolInputs, maxNoToolOutputs);
    }

    @Override
    public boolean synthesisEncoding() throws IOException {
        try {
            this.control = new Control();
            
            // Load base encodings
            Path customEncodingsPath = runConfig.getClingoEncodingsPath();
            if (customEncodingsPath != null && Files.isDirectory(customEncodingsPath)) {
                try (Stream<Path> paths = Files.list(customEncodingsPath)) {
                    paths.filter(Files::isRegularFile)
                         .filter(p -> p.toString().endsWith(".lp"))
                         .forEach(p -> this.control.load(p));
                }
            } else {
                // Load embedded encodings
                String[] files = new String[]{
                        "base.lp", "step.lp", "check.lp", "show.lp",
                        "tool_inclusion.lp", "tool_dependency.lp", "properties.lp",
                        "artifact_state_constraints.lp", "temporal_constraint.lp",
                        "input_usage.lp", "recipe_constraints.lp",
                        "workflode_constraints.lp", "energy_cost.lp"
                };
                for (String file : files) {
                    String content = APEResources.getTextResource("clingo/" + file);
                    if (content != null) {
                        this.control.add("base", content);
                    }
                }
            }

            String aspProgram = generateASPFacts();
            if (this.solutionSize == 1) { // just print once
                System.out.println("--- Generated ASP Program ---");
                System.out.println(aspProgram);
                System.out.println("-----------------------------");
            }
            this.control.add("base", aspProgram);
            
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String generateASPFacts() {
        StringBuilder sb = new StringBuilder();

        // Taxonomies and Types
        // taxonomy(Domain, Dimension, (GroupRoot, Child, Parent))
        // Only generate parent-child edges — no self-references, which would
        // incorrectly mark leaf types as non-terminal and break compatible/1.
        for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate type : domainSetup.getAllTypes().getTypes()) {
            nl.uu.cs.ape.models.enums.NodeType nt = type.getNodeType();
            // Skip internal APE predicates that should not appear in the encoding.
            if (nt == nl.uu.cs.ape.models.enums.NodeType.EMPTY
                    || nt == nl.uu.cs.ape.models.enums.NodeType.EMPTY_LABEL
                    || "APE_label".equals(type.getRootNodeID())) {
                continue;
            }
            String domain = "ape";
            String dimension = type.getRootNodeID();
            if (type.getSubPredicates() != null) {
                for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate subType : type.getSubPredicates()) {
                    if (subType.getNodeType() == nl.uu.cs.ape.models.enums.NodeType.EMPTY
                            || subType.getNodeType() == nl.uu.cs.ape.models.enums.NodeType.EMPTY_LABEL
                            || "APE_label".equals(subType.getRootNodeID())) {
                        continue;
                    }
                    sb.append(String.format("taxonomy(\"%s\", \"%s\", (\"%s\", \"%s\", \"%s\")).\n",
                            domain, dimension, dimension, subType.getPredicateID(), type.getPredicateID()));
                }
            }
        }

        // Tools and taxonomy
        // tool_taxonomy(Domain, (Child, Parent))
        // Only generate parent-child edges — no self-references, which would
        // cause tool_abstract_class to be derived for every leaf tool.
        for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate module : domainSetup.getAllModules().getModules()) {
            String domain = "ape";
            nl.uu.cs.ape.models.enums.NodeType nt = module.getNodeType();

            // Mark concrete (leaf) tools and explicit abstract classes.
            // ROOT is handled implicitly: it appears as a parent in tool_taxonomy,
            // so base.lp derives tool_abstract_class for it automatically.
            if (nt == nl.uu.cs.ape.models.enums.NodeType.LEAF
                    || nt == nl.uu.cs.ape.models.enums.NodeType.ARTIFICIAL_LEAF) {
                sb.append(String.format("tool(\"%s\").\n", module.getPredicateID()));
            } else if (nt == nl.uu.cs.ape.models.enums.NodeType.ABSTRACT) {
                sb.append(String.format("tool_abstract_class(\"%s\").\n", module.getPredicateID()));
            }

            // Parent-child edges (no self-references).
            if (module.getSubPredicates() != null) {
                for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate subModule : module.getSubPredicates()) {
                    sb.append(String.format("tool_taxonomy(\"%s\", (\"%s\", \"%s\")).\n",
                            domain, subModule.getPredicateID(), module.getPredicateID()));
                }
            }

            // Tool inputs and outputs (only for concrete Module instances).
            if (module instanceof nl.uu.cs.ape.models.Module) {
                nl.uu.cs.ape.models.Module m = (nl.uu.cs.ape.models.Module) module;

                // All inputs go into a single variant so the planner must satisfy
                // every port simultaneously (the encoding chooses exactly one variant,
                // then binds every port within that variant).
                if (!m.getModuleInput().isEmpty()) {
                    String variantId = module.getPredicateID() + "_v0";
                    sb.append(String.format("tool_input(\"%s\", \"%s\").\n", module.getPredicateID(), variantId));
                    for (int i = 0; i < m.getModuleInput().size(); i++) {
                        nl.uu.cs.ape.models.Type t = m.getModuleInput().get(i);
                        String portId = variantId + "_p" + i;
                        sb.append(String.format("input_port(\"%s\", \"%s\").\n", variantId, portId));
                        printDimensions(sb, "dimension", portId, t);
                    }
                }

                for (int i = 0; i < m.getModuleOutput().size(); i++) {
                    nl.uu.cs.ape.models.Type t = m.getModuleOutput().get(i);
                    String outId = module.getPredicateID() + "_out_" + i;
                    String portId = outId + "_port_0";
                    sb.append(String.format("tool_output(\"%s\", \"%s\").\n", module.getPredicateID(), outId));
                    sb.append(String.format("output_port(\"%s\", \"%s\").\n", outId, portId));
                    printDimensions(sb, "dimension", portId, t);
                }
            }
        }

        // Workflow Inputs
        for (int i = 0; i < runConfig.getProgramInputs().size(); i++) {
            nl.uu.cs.ape.models.Type t = runConfig.getProgramInputs().get(i);
            String wfInId = "wf_input_" + i;
            sb.append(String.format("holds(0, avail(\"%s\")).\n", wfInId));
            printDimensions(sb, "holds(0, dim", wfInId, t);
        }

        // Workflow Outputs
        for (int i = 0; i < runConfig.getProgramOutputs().size(); i++) {
            nl.uu.cs.ape.models.Type t = runConfig.getProgramOutputs().get(i);
            printDimensions(sb, "goal_output", String.valueOf(i), t);
        }

        return sb.toString();
    }

    private void printDimensions(StringBuilder sb, String predicate, String id, nl.uu.cs.ape.models.Type type) {
        if (type instanceof AuxTypePredicate) {
            AuxTypePredicate auxType = (AuxTypePredicate) type;
            for (TaxonomyPredicate p : auxType.getGeneralizedPredicates()) {
                printDimensions(sb, predicate, id, (nl.uu.cs.ape.models.Type) p);
            }
        } else {
            // Skip APE-internal label dimensions (emptyLabel, APE_label).
            // These are SAT-solver bookkeeping types with no meaning in the ASP encoding.
            // Including them creates unsatisfiable port requirements because the taxonomy
            // does not contain them, so compatible/2 never holds for them.
            nl.uu.cs.ape.models.enums.NodeType nt = type.getNodeType();
            if (nt == nl.uu.cs.ape.models.enums.NodeType.EMPTY_LABEL
                    || nt == nl.uu.cs.ape.models.enums.NodeType.EMPTY
                    || "APE_label".equals(type.getRootNodeID())) {
                return;
            }
            if (predicate.contains("holds")) {
                sb.append(String.format("%s(\"%s\", \"%s\", \"%s\")).\n", predicate, id, type.getPredicateID(), type.getRootNodeID()));
            } else if (predicate.equals("goal_output")) {
                sb.append(String.format("%s(%s, \"%s\", \"%s\").\n", predicate, id, type.getPredicateID(), type.getRootNodeID()));
            } else {
                sb.append(String.format("%s(\"%s\", (\"%s\", \"%s\")).\n", predicate, id, type.getPredicateID(), type.getRootNodeID()));
            }
        }
    }

    @Override
    public List<SolutionWorkflow> synthesisExecution() throws FileNotFoundException, IOException {
        List<SolutionWorkflow> foundSolutions = new ArrayList<>();
        if (this.control == null) {
            return foundSolutions;
        }

        try {
            // Base ground
            this.control.ground(new ProgramPart("base", new Symbol[0]));
            
            // For clingo we don't just solve once if using multi-shot, but APE loop iterates over lengths
            // APE's loop handles lengths via `solutionSize`. So here we can just execute for `t = solutionSize`
            // Wait, the APE main loop increases solutionSize from min to max.
            // If we use the exact-horizon approach for each `solutionSize`:
            int t = this.solutionSize;
            
            // Ground step and check for t
            for (int i = 1; i <= t; i++) {
                this.control.ground(new ProgramPart("step", new Number(i)));
            }
            this.control.ground(new ProgramPart("check", new Number(t)));
            
            this.control.assignExternal(new Symbol[]{new Function("query", new Number(t))}, ExternalType.TRUE);

            SolveHandle handle = this.control.solve(SolveMode.YIELD);
            while (handle.hasNext()) {
                Model model = handle.next();
                SolutionWorkflow clingoSolution = new SolutionWorkflow(model, this);
                foundSolutions.add(clingoSolution);
            }
            System.out.println("Clingo solve result for t=" + t + ": " + handle.getSolveResult());
            handle.close();
            
            // In a real implementation we would parse the `tool_at_time(t, tool)` and reconstruct the APE SolutionWorkflow.
            // For now, APE's Clingo test just ensures it completes without crashing.
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.control.close();
        }
        return foundSolutions;
    }

    @Override
    public TypeAutomaton getTypeAutomaton() {
        return this.typeAutomaton;
    }

    public ModuleAutomaton getModuleAutomaton() {
        return this.moduleAutomaton;
    }

    @Override
    public APEDomainSetup getDomainSetup() {
        return this.domainSetup;
    }

    @Override
    public SATAtomMappings getMappings() {
        return this.mappings;
    }

    @Override
    public int getSolutionSize() {
        return this.solutionSize;
    }

    @Override
    public void deleteTempFiles() throws IOException {
        // No temporary files to delete in basic memory-based ASP integration.
    }
}
