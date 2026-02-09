package nl.uu.cs.ape.solver.solutionStructure.cwl;

import nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate;
import nl.uu.cs.ape.solver.solutionStructure.ModuleNode;
import nl.uu.cs.ape.solver.solutionStructure.SolutionCreationUtils;
import nl.uu.cs.ape.solver.solutionStructure.SolutionWorkflow;
import nl.uu.cs.ape.solver.solutionStructure.TypeNode;

import java.util.HashMap;
import java.util.List;

import lombok.extern.slf4j.Slf4j;

/**
 * Class to generate a CWL workflow structure from a given workflow solution.
 */
@Slf4j
public class AbstractCWLCreator extends CWLWorkflowBase {
    /**
     * Maintain a list of the CWL parameter names which represent {@link TypeNode}s.
     * I.e. this hashmap has TypeNode IDs as keys and their names in the CWL file as
     * values.
     */
    private final HashMap<String, String> workflowParameters = new HashMap<>();

    /**
     * Instantiates a new CWL creator.
     * 
     * @param solution The solution to represent in CWL.
     */
    public AbstractCWLCreator(SolutionWorkflow solution) {
        super(solution);
    }

    /**
     * Generates the CWL representation.
     */
    @Override
    protected void generateCWLRepresentation() {
        // Workflow
        generateWorkflowInputs();
        generateWorkflowOutputs();
        generateWorkflowSteps();
    }

    /**
     * Generate the top-level inputs definition of the workflow.
     */
    private void generateWorkflowInputs() {
        cwlRepresentation.append("inputs:").append("\n");
        // Inputs
        for (TypeNode typeNode : solution.getWorkflowInputTypeStates()) {
            String inputName = String.format("input_%d", workflowParameters.size() + 1);
            addParameter(typeNode, inputName);
            cwlRepresentation
                    // Name
                    .append(ind(1))
                    .append(inputName)
                    .append(":\n")
                    // Data type
                    .append(ind(2))
                    .append("type: File")
                    .append("\n")
                    // Format
                    .append(ind(2))
                    .append("format: ")
                    .append(typeNode.getFormat())
                    .append("\n");
        }
    }

    /**
     * Generate the top-level outputs definition of the workflow.
     */
    private void generateWorkflowOutputs() {
        cwlRepresentation.append("outputs:\n");
        // Outputs
        int i = 1;
        for (TypeNode typeNode : solution.getWorkflowOutputTypeStates()) {
            cwlRepresentation
                    // Name
                    .append(ind(1))
                    .append(String.format("output_%d", i))
                    .append(":\n")
                    // Data type
                    .append(ind(2))
                    .append("type: File")
                    .append("\n")
                    // Format
                    .append(ind(2))
                    .append("format: ")
                    .append(typeNode.getFormat())
                    .append("\n")
                    // outputSource
                    .append(ind(2))
                    .append("outputSource: ")
                    .append(SolutionCreationUtils.stepName(typeNode.getCreatedByModule()))
                    .append("/");
            // Get the id of the step run's output bound to this workflow output
            // (step_name/output_name_ID)
            int outId = typeNode.getCreatedByModule().getOutputTypes().get(i - 1).getAutomatonState()
                    .getLocalStateNumber();
            cwlRepresentation
                    .append(SolutionCreationUtils.generateInputOrOutputName(typeNode.getCreatedByModule(), "out", outId + 1))
                    .append("\n");
            i++;
        }
    }

    /**
     * Generate the top-level steps of the workflow.
     */
    private void generateWorkflowSteps() {
        cwlRepresentation.append("steps:").append("\n");
        for (ModuleNode moduleNode : solution.getModuleNodes()) {
            generateStep(moduleNode);
        }
    }

    /**
     * Generate one workflow step.
     * 
     * @param moduleNode The {@link ModuleNode} related to the step.
     */
    private void generateStep(ModuleNode moduleNode) {
        final int baseInd = 1;
        // Name
        cwlRepresentation
                .append(ind(baseInd))
                .append(SolutionCreationUtils.stepName(moduleNode))
                .append(":\n");
        generateStepIn(moduleNode);
        generateStepOut(moduleNode);
        generateStepRun(moduleNode);
    }

    /**
     * Generate the "in" section of a workflow step.
     * 
     * @param moduleNode The {@link ModuleNode} that is the step.
     */
    private void generateStepIn(ModuleNode moduleNode) {
        final int baseInd = 2;
        // "in" key
        cwlRepresentation.append(ind(baseInd)).append("in").append(":\n");
        // If there are no inputs, give an empty array as input
        if (moduleNode.getInputTypes().isEmpty()) {
            // Remove the last newline so the array is on the same line as "in:"
            deleteLastNCharactersFromCWL(1);
            cwlRepresentation
                    .append(" ")
                    .append("[]")
                    .append("\n");
            return;
        }

        int i = 0;
        for (TypeNode typeNode : moduleNode.getInputTypes()) {
            cwlRepresentation
                    .append(ind(baseInd + 1))
                    .append(SolutionCreationUtils.generateInputOrOutputName(moduleNode, "in", i + 1))
                    .append(": ")
                    .append(workflowParameters.get(typeNode.getNodeID()))
                    .append("\n");
            i++;
        }
    }

    /**
     * Generate the "out" section of a workflow step.
     * 
     * @param moduleNode The {@link ModuleNode} that is the step.
     */
    private void generateStepOut(ModuleNode moduleNode) {
        final int baseInd = 2;
        cwlRepresentation
                // "out" key
                .append(ind(baseInd))
                .append("out: ")
                // The outputs array
                .append("[");
        int i = 1;
        for (TypeNode typeNode : moduleNode.getOutputTypes()) {
            String name = SolutionCreationUtils.generateInputOrOutputName(moduleNode, "out", i);
            addParameter(typeNode, String.format("%s/%s", SolutionCreationUtils.stepName(moduleNode), name));
            cwlRepresentation
                    .append(name)
                    .append(", ");
            i++;
        }
        deleteLastNCharactersFromCWL(2);
        cwlRepresentation.append("]").append("\n");
    }

    /**
     * Generate the run field of a workflow step.
     * 
     * @param moduleNode The {@link ModuleNode} related to the step.
     */
    private void generateStepRun(ModuleNode moduleNode) {
        final int baseInd = 2;
        cwlRepresentation
                // Main key
                .append(ind(baseInd))
                .append("run:")
                .append("\n")
                // Class
                .append(ind(baseInd + 1))
                .append("class: Operation")
                .append("\n");
        // Inputs
        cwlRepresentation
                .append(ind(baseInd + 1))
                .append("inputs:")
                .append("\n");
        generateTypeNodes(moduleNode, moduleNode.getInputTypes(), true, baseInd + 2);
        // Outputs
        cwlRepresentation
                .append(ind(baseInd + 1))
                .append("outputs:")
                .append("\n");
        generateTypeNodes(moduleNode, moduleNode.getOutputTypes(), false, baseInd + 2);
        // Hints and intent
        generateStepHints(moduleNode, baseInd + 1);
        generateStepIntent(moduleNode, baseInd + 1);
    }

    /**
     * Generate the hints related to this step's run.
     * 
     * @param moduleNode The {@link ModuleNode} that is the step.
     * @param baseInd    The indentation at which the hints should start.
     */
    private void generateStepHints(ModuleNode moduleNode, int baseInd) {
        cwlRepresentation
                // "hints" key
                .append(ind(baseInd))
                .append("hints:")
                .append("\n")
                // "SoftwareRequirement" key
                .append(ind(baseInd + 1))
                .append("SoftWareRequirement:")
                .append("\n")
                // "packages" key
                .append(ind(baseInd + 2))
                .append("packages:")
                .append("\n")
                // The required package
                .append(ind(baseInd + 3))
                .append(
                        String.format("%s: [\"%s\"]",
                                moduleNode.getNodeLabel(),
                                moduleNode.getUsedModule().getPredicateID()))
                .append("\n");
    }

    /**
     * Generate the intent for a workflow step's run.
     * 
     * @param moduleNode The {@link ModuleNode} that is the workflow step.
     * @param baseInd    The indentation level at which the intent should start.
     */
    private void generateStepIntent(ModuleNode moduleNode, int baseInd) {
        cwlRepresentation
                .append(ind(baseInd))
                .append("intent: ")
                .append("[");
        for (TaxonomyPredicate predicate : moduleNode.getUsedModule().getParentPredicates()) {
            cwlRepresentation
                    .append("\"")
                    .append(predicate.getPredicateID())
                    .append("\"")
                    .append(", ");
        }
        deleteLastNCharactersFromCWL(2);
        cwlRepresentation.append("]").append("\n");
    }

    /**
     * Generate the inputs or outputs of a step's run.
     * 
     * @param moduleNode   The {@link ModuleNode} that is the workflow step.
     * @param typeNodeList The {@link TypeNode}s that are either the input or output
     *                     nodes.
     * @param input        Whether the type nodes are inputs. If false, they are
     *                     consider outputs.
     * @param baseInd      The indentation level of the "inputs:" or "outputs:" line
     *                     preceding this function.
     */
    private void generateTypeNodes(ModuleNode moduleNode, List<TypeNode> typeNodeList, boolean input, int baseInd) {
        // If the input or output nodes are empty, give an empty array as input or
        // output
        if (typeNodeList.isEmpty()) {
            // Remove the last newline so the array is on the same line as "inputs:"
            deleteLastNCharactersFromCWL(1);
            cwlRepresentation
                    .append(" ")
                    .append("[]")
                    .append("\n");
            return;
        }

        int i = 0;
        for (TypeNode typeNode : typeNodeList) {
            // Don't include empty nodes
            if (typeNode.isEmpty()) {
                return;
            }

            i++;
            cwlRepresentation
                    // Name
                    .append(ind(baseInd))
                    .append(SolutionCreationUtils.generateInputOrOutputName(moduleNode, input ? "in" : "out", i))
                    .append(":\n")
                    // Data type
                    .append(ind(baseInd + 1))
                    .append("type: File")
                    .append("\n")
                    // Format
                    .append(ind(baseInd + 1))
                    .append("format: ")
                    .append(typeNode.getFormat())
                    .append("\n");
        }
    }

    /**
     * Add a parameter to the parameter hashmap.
     * Will log an error to the error output if {@link TypeNode} is already known.
     * 
     * @param typeNode The {@link TypeNode} that is the input parameter.
     * @param name     The name of the parameter.
     */
    private void addParameter(TypeNode typeNode, String name) {
        if (workflowParameters.putIfAbsent(typeNode.getNodeID(), name) != null) {
            log.warn("Duplicate key \"%s\" in workflow inputs!\n", typeNode.getNodeID());
        }
    }

}
