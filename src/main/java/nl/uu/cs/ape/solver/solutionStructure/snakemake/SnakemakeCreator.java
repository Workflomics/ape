package nl.uu.cs.ape.solver.solutionStructure.snakemake;

import nl.uu.cs.ape.solver.solutionStructure.SolutionCreationUtils;
import nl.uu.cs.ape.solver.solutionStructure.SolutionCreationUtils.IndentStyle;
import nl.uu.cs.ape.solver.solutionStructure.SolutionWorkflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;

import nl.uu.cs.ape.solver.solutionStructure.ModuleNode;
import nl.uu.cs.ape.solver.solutionStructure.TypeNode;

import lombok.extern.slf4j.Slf4j;

/**
 * Class to generate a Snakemake workflow structure from a given workflow solution.
 */
@Slf4j
public class SnakemakeCreator {
    
    protected final StringBuilder snakemakeRepresentation;
    protected SolutionWorkflow solution;
    private IndentStyle indentStyle;

    private final HashMap<String, String> workflowParameters = new HashMap<>();
    
    /**
     * Instantiates a new Snakemake creator.
     * 
     * @param solution The solution to represent in Snakemake.
     */
    public SnakemakeCreator(SolutionWorkflow solution) {
        this.solution = solution;
        this.snakemakeRepresentation = new StringBuilder();
        this.indentStyle = IndentStyle.SPACES4;
    }

    /**
     * Generates the Snakemake representation.
     */
    public String generateSnakemakeRepresentation() {
        generateTopComment();
        snakemakeRepresentation.append("\n");
        addWorkflowInputs();
        generateRuleAll();
        for (ModuleNode moduleNode : solution.getModuleNodes()) {
            snakemakeRepresentation.append(String.format("rule %s:\n'", SolutionCreationUtils.stepName(moduleNode)));
            generateRuleInput(moduleNode);
            generateRuleOutput(moduleNode);
            generateRuleShell(moduleNode);
            snakemakeRepresentation.append("\n");
        }

        return snakemakeRepresentation.toString();
    }

    /**
     * Generate rule with all target outputs of the workflow.
     */
    private void generateRuleAll() {
        snakemakeRepresentation
                .append("rule all:\n")
                .append(ind(1))
                .append("input:\n");
        int i = 1;
        for (TypeNode typeNode : solution.getWorkflowOutputTypeStates()) {
            snakemakeRepresentation
                    .append(ind(2))
                    .append("'add-path/");
            int outId = typeNode.getCreatedByModule().getOutputTypes().get(i - 1).getAutomatonState()
                    .getLocalStateNumber();
            snakemakeRepresentation
                    .append(SolutionCreationUtils.generateInputOrOutputName(typeNode.getCreatedByModule(), "out", outId + 1))
                    .append("'\n");
            i++;
        }
        snakemakeRepresentation.append("\n");
    }

    /**
     * Add workflow inputs to parameter hashmap.
     */
    private void addWorkflowInputs() {
        int i = 1;
        for (TypeNode typeNode : solution.getWorkflowInputTypeStates()) {
            String inputName = String.format("input_%d", i++);
            addNewParameterToMap(typeNode, inputName);
        }
    }

     /**
     * Generate "input" section of a rule.
     * 
     * @param moduleNode The {@link ModuleNode} corresponding to the rule.
     */
    private void generateRuleInput(ModuleNode moduleNode) {
        snakemakeRepresentation
                .append(ind(1))
                .append("input:\n");

        List<String> valid_inputs = new ArrayList<>();
        // Create a path representation for each valid, i.e. non-empty, input type
        for (TypeNode node : moduleNode.getInputTypes())
        {
            if (!node.isEmpty())
            {
                valid_inputs.add(String.format("%s'add-path/%s'", ind(2), workflowParameters.get(node.getNodeID())));
            }
        }

        // Join all valid input representations with a colon followed by a newline
        snakemakeRepresentation.append(String.join(",\n", valid_inputs)).append("\n");
    }

    /**
     * Generate "output" section of a rule.
     * 
     * @param moduleNode The {@link ModuleNode} corresponding to the rule.
     */
    private void generateRuleOutput(ModuleNode moduleNode) {
        snakemakeRepresentation
                .append(ind(1))
                .append("output:\n");

        List<String> valid_output = new ArrayList<>();
        int valid_output_id = 0;
        // Create a path representation for each valid, i.e. non-empty, output type
        for (TypeNode node : moduleNode.getOutputTypes())
        {
            if (!node.isEmpty())
            {
                // Increment the output id and generate the output name
                valid_output_id += 1;
                String name = SolutionCreationUtils.generateInputOrOutputName(moduleNode, "out",  valid_output_id);
                addNewParameterToMap(node, String.format("%s", name));

                valid_output.add(String.format("%s'add-path/%s'", ind(2), name));
            }
        }

        // Join all valid output representations with a colon followed by a newline
        snakemakeRepresentation.append(String.join(",\n", valid_output)).append("\n");
    }

    /**
     * Generate "shell" section of a rule.
     * 
     * @param moduleNode The {@link ModuleNode} corresponding to the rule.
     */
    private void generateRuleShell(ModuleNode moduleNode) {
        String moduleName = moduleNode.getUsedModule().getPredicateLabel();
        String command = moduleNode.getUsedModule().getExecutionCommand();
        if (command == null || command.equals("")) {
            log.info("No command for {} specified, using this name as fallback command \"{}\"", moduleName);
            command = moduleName;
        }
        snakemakeRepresentation
                .append(ind(1))
                .append(String.format("shell: 'add-path-to-implementation/%s {input} {output}'", command))
                .append("\n");
    }

    private String addNewParameterToMap(TypeNode typeNode, String name) {
        if (workflowParameters.putIfAbsent(typeNode.getNodeID(), name) != null) {
            log.warn("Duplicate key \"%s\" in workflow inputs!", typeNode.getNodeID());
        }
        return typeNode.getNodeID();
    }


    /**
     * Adds the comment at the top of the file.
     */
    protected void generateTopComment() {
        snakemakeRepresentation.append(String.format("# %s%n", getWorkflowName()));
        snakemakeRepresentation.append("# This workflow is generated by APE (https://github.com/workflomics/ape).\n");
    }

    /**
     * Get the name of the workflow.
     * 
     * @return The name of the workflow.
     */
    private String getWorkflowName() {
        return String.format("WorkflowNo_%d", solution.getIndex());
    }

    /**
     * Generate the indentation at the start of a line.
     * 
     * @param level The level of indentation.
     * @return The indentation of the given level.
     */
    protected String ind(int level) {
        return SolutionCreationUtils.ind(level, this.indentStyle);
    }

}
