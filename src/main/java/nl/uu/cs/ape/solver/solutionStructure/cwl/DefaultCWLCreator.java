package nl.uu.cs.ape.solver.solutionStructure.cwl;

import nl.uu.cs.ape.models.Type;
import nl.uu.cs.ape.solver.solutionStructure.ModuleNode;
import nl.uu.cs.ape.solver.solutionStructure.SolutionWorkflow;
import nl.uu.cs.ape.solver.solutionStructure.TypeNode;
import nl.uu.cs.ape.utils.APEResources;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.json.JSONArray;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

/**
 * Class to generate a CWL workflow structure from a given workflow solution.
 */
@Slf4j
public class DefaultCWLCreator extends CWLWorkflowBase {
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
    public DefaultCWLCreator(SolutionWorkflow solution) {
        super(solution);
    }


    /**
     * Generates the CWL representation.
     */
    @Override
    protected void generateCWLRepresentation() {
        // Workflow
        generateWorkflowInputs();
        generateWorkflowSteps();
        generateWorkflowOutputs();
    }

    /**
     * Generate the top-level inputs definition of the workflow.
     */
    private void generateWorkflowInputs() {
        cwlRepresentation.append("inputs:").append("\n");
        cwlRepresentation.append(getInputsInCWL(false));
    }

    /**
     * Generate the top-level outputs definition of the workflow.
     * 
     * @param formatForCwlInputsYmlFile Whether the inputs are used to generate the
     *                                  CWL inputs.yml file.
     *
     * @return The CWL representation of the workflow outputs.
     */
    private String getInputsInCWL(boolean formatForCwlInputsYmlFile) {

        StringBuilder inputsInCWL = new StringBuilder();

        int indentLevel = 1;
        String typeLabel = "type";
        if (formatForCwlInputsYmlFile) {
            indentLevel = 0;
            typeLabel = "class";
        }
        int i = 1;
        for (TypeNode typeNode : solution.getWorkflowInputTypeStates()) {
            String currTypeFormat = "";
            for (Type type : typeNode.getTypes()) {
                if (type.getRootNodeID().equals("http://edamontology.org/format_1915")) {
                    currTypeFormat = type.getPredicateID();
                }
            }
            String inputName = String.format("input_%d", i++);
            addNewParameterToMap(typeNode, inputName);
            inputsInCWL
                    // Name
                    .append(ind(indentLevel))
                    .append(inputName)
                    .append(":\n")
                    // Data type
                    .append(ind(indentLevel + 1))
                    .append(typeLabel + ": File")
                    .append("\n")
                    // Format
                    .append(ind(indentLevel + 1))
                    .append("format: ")
                    .append(typeNode.getFormat())
                    .append("\n");
            if (formatForCwlInputsYmlFile) {
                // Load resources file
                JSONObject availableDataJson = APEResources.getJSONResource("default_cwl_creator_data.json");
                JSONArray inputsArray = availableDataJson.getJSONArray("inputs");

                Map<String, String> availableData = new HashMap<>();
                for (int j = 0; j < inputsArray.length(); j++) {
                    JSONObject entry = inputsArray.getJSONObject(j);
                    String format = entry.getString("format");
                    String data = entry.getString("data");

                    availableData.put(format, data);
                    log.trace("Read default CWL creator config format and data {} : {}", format, data);
                }
                String inputPath = availableData.containsKey(currTypeFormat) ? availableData.get(currTypeFormat)
                        : "set_full_path_to_the_file_with_extension_here";
                inputsInCWL
                        .append(ind(indentLevel + 1))
                        .append("path: ")
                        .append(inputPath)
                        .append("\n");
            }
        }
        return inputsInCWL.toString();
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
                    .append(stepName(typeNode.getCreatedByModule()))
                    .append("/");
            // Get the id of the step run's output bound to this workflow output
            // (step_name/output_name_ID)
            int outId = typeNode.getCreatedByModule().getOutputTypes().get(i - 1).getAutomatonState()
                    .getLocalStateNumber();
            cwlRepresentation
                    .append(typeNode.getCreatedByModule().getOutputCWLKeys().get(outId))
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
                .append(stepName(moduleNode))
                .append(":\n");
        generateDefaultStepRun(moduleNode);
        generateStepIn(moduleNode);
        generateStepOut(moduleNode);
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
        if (!moduleNode.hasInputTypes()) {
            // Remove the last newline so the array is on the same line as "in:"
            deleteLastNCharactersFromCWL(1);
            cwlRepresentation
                    .append(" ")
                    .append("[]")
                    .append("\n");
        } else {
            List<TypeNode> inputs = moduleNode.getInputTypes();
            IntStream.range(0, inputs.size()).filter(i -> !inputs.get(i).isEmpty())
                    .forEach(i -> cwlRepresentation
                            .append(ind(baseInd + 1))
                            .append(moduleNode.getInputCWLKeys().get(i))
                            .append(": ")
                            .append(workflowParameters.get(inputs.get(i).getNodeID()))
                            .append("\n"));
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

        List<TypeNode> outputs = moduleNode.getOutputTypes();
        IntStream.range(0, outputs.size()).filter(i -> !outputs.get(i).isEmpty())
                .forEach(i -> {
                    String name = moduleNode.getOutputCWLKeys().get(i);
                    addNewParameterToMap(outputs.get(i), String.format("%s/%s", stepName(moduleNode), name));
                    cwlRepresentation
                            .append(name)
                            .append(", ");
                });
        if (moduleNode.hasOutputTypes()) {
            // Remove the last comma
            deleteLastNCharactersFromCWL(2);
        }
        cwlRepresentation.append("]").append("\n");
    }

    /**
     * Generate the run field of a workflow step.
     * 
     * @param moduleNode The {@link ModuleNode} related to the step.
     */
    private void generateDefaultStepRun(ModuleNode moduleNode) {
        final int baseInd = 2;
        String moduleReference = "add-path-to-the-implementation/" + moduleNode.getUsedModule().getPredicateID()
                + ".cwl ";
        if (moduleNode.getUsedModule().getCwlFileReference() != null) {
            moduleReference = moduleNode.getUsedModule().getCwlFileReference();
        }
        cwlRepresentation
                // Main key
                .append(ind(baseInd))
                .append("run: " + moduleReference)
                .append("\n");
    }

    /**
     * Add a parameter to the parameter hashmap.
     * Will log an error to the error output if {@link TypeNode} is already known.
     * 
     * @param typeNode The {@link TypeNode} that is the input parameter.
     * @param name     The name of the parameter.
     * @return The ID of the parameter.
     */
    private String addNewParameterToMap(TypeNode typeNode, String name) {
        if (workflowParameters.putIfAbsent(typeNode.getNodeID(), name) != null) {
            log.warn("Duplicate key \"%s\" in workflow inputs!", typeNode.getNodeID());
        }
        return typeNode.getNodeID();
    }

    /**
     * Generates the CWL inputs as a YML file content.
     * 
     * @return Content of a YML file that describes the CWL workflow inputs.
     */
    public String generateCWLWorkflowInputs() {
        return getInputsInCWL(true);
    }
}
