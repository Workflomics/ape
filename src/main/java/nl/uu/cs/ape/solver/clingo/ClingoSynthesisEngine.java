package nl.uu.cs.ape.solver.clingo;

import nl.uu.cs.ape.automaton.TypeAutomaton;
import nl.uu.cs.ape.configuration.APERunConfig;
import nl.uu.cs.ape.domain.APEDomainSetup;
import nl.uu.cs.ape.models.SATAtomMappings;
import nl.uu.cs.ape.solver.SynthesisEngine;
import nl.uu.cs.ape.solver.solutionStructure.SolutionWorkflow;
import nl.uu.cs.ape.solver.solutionStructure.SolutionsList;

import org.potassco.clingo.control.Control;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    }

    @Override
    public boolean synthesisEncoding() throws IOException {
        try {
            this.control = new Control();
            String aspProgram = generateASPFacts();
            if (this.solutionSize == 1) { // just print once
                System.out.println("--- Generated ASP Program ---");
                System.out.println(aspProgram);
                System.out.println("-----------------------------");
            }
            this.control.add("base", aspProgram);
            this.control.ground();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String generateASPFacts() {
        StringBuilder sb = new StringBuilder();

        // Taxonomies and Types
        for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate type : domainSetup.getAllTypes().getTypes()) {
            sb.append(String.format("type(\"%s\").\n", type.getPredicateID()));
            if (type.getSubPredicates() != null) {
                for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate subType : type.getSubPredicates()) {
                    sb.append(String.format("sub_type(\"%s\", \"%s\").\n", type.getPredicateID(), subType.getPredicateID()));
                }
            }
        }

        // Tools and taxonomy
        for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate module : domainSetup.getAllModules().getModules()) {
            sb.append(String.format("tool(\"%s\").\n", module.getPredicateID()));
            if (module.getSubPredicates() != null) {
                for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate subModule : module.getSubPredicates()) {
                    sb.append(String.format("sub_tool(\"%s\", \"%s\").\n", module.getPredicateID(), subModule.getPredicateID()));
                }
            }
            if (module instanceof nl.uu.cs.ape.models.Module) {
                nl.uu.cs.ape.models.Module m = (nl.uu.cs.ape.models.Module) module;
                for (int i = 0; i < m.getModuleInput().size(); i++) {
                    nl.uu.cs.ape.models.Type t = m.getModuleInput().get(i);
                    sb.append(String.format("tool_input(\"%s\", %d, \"%s\").\n", module.getPredicateID(), i, t.getPredicateID()));
                }
                for (int i = 0; i < m.getModuleOutput().size(); i++) {
                    nl.uu.cs.ape.models.Type t = m.getModuleOutput().get(i);
                    sb.append(String.format("tool_output(\"%s\", %d, \"%s\").\n", module.getPredicateID(), i, t.getPredicateID()));
                }
            }
        }

        // Workflow Inputs
        for (int i = 0; i < runConfig.getProgramInputs().size(); i++) {
            nl.uu.cs.ape.models.Type t = runConfig.getProgramInputs().get(i);
            sb.append(String.format("workflow_input(%d, \"%s\").\n", i, t.getPredicateID()));
        }

        // Workflow Outputs
        for (int i = 0; i < runConfig.getProgramOutputs().size(); i++) {
            nl.uu.cs.ape.models.Type t = runConfig.getProgramOutputs().get(i);
            sb.append(String.format("workflow_output(%d, \"%s\").\n", i, t.getPredicateID()));
        }

        sb.append(String.format("solution_length(%d, %d).\n", runConfig.getSolutionLength().getMin(), runConfig.getSolutionLength().getMax()));

        return sb.toString();
    }

    @Override
    public List<SolutionWorkflow> synthesisExecution() throws FileNotFoundException, IOException {
        List<SolutionWorkflow> foundSolutions = new ArrayList<>();
        if (this.control == null) {
            return foundSolutions;
        }

        try {
            org.potassco.clingo.solving.SolveHandle handle = this.control.solve();
            if (this.solutionSize == 1) {
                System.out.println("Clingo solve result: " + handle.getSolveResult());
            }
            while (handle.hasNext()) {
                org.potassco.clingo.solving.Model model = handle.next();
                if (this.solutionSize == 1) {
                    System.out.println("Extracted ASP Facts (Model):");
                    System.out.println(java.util.Arrays.toString(model.getSymbols()));
                }
            }
            handle.close();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            this.control.close();
        }
        return foundSolutions;
    }

    @Override
    public TypeAutomaton getTypeAutomaton() {
        return null; // Stub
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
