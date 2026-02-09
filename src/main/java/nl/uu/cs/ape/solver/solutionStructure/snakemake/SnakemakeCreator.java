package nl.uu.cs.ape.solver.solutionStructure.snakemake;

import nl.uu.cs.ape.solver.solutionStructure.SolutionWorkflow;
import nl.uu.cs.ape.solver.solutionStructure.cwl.CWLWorkflowBase.IndentStyle;

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
            snakemakeRepresentation
                    .append("rule ")
                    .append(stepName(moduleNode))
                    .append(":\n");
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
                    .append(generateInputOrOutputName(typeNode.getCreatedByModule(), "out", outId + 1))
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
        if (moduleNode.hasInputTypes()){
            List<TypeNode> inputs = moduleNode.getInputTypes();
            IntStream.range(0, inputs.size()).filter(i -> !inputs.get(i).isEmpty())
                    .forEach(i -> snakemakeRepresentation
                            .append(ind(2))
                            .append(String.format("'add-path/%s'", workflowParameters.get(inputs.get(i).getNodeID())))
                            .append(",\n"));
            if (moduleNode.hasOutputTypes()) {
            // Remove the last comma
            deleteLastNCharactersFromSnakefile(2);
            }
            snakemakeRepresentation.append("\n");
        }
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
        
        List<TypeNode> outputs = moduleNode.getOutputTypes();
        IntStream.range(0, outputs.size()).filter(i -> !outputs.get(i).isEmpty())
                .forEach(i -> {
                    String name = generateInputOrOutputName(moduleNode, "out", i + 1);
                    addNewParameterToMap(outputs.get(i), String.format("%s", name));
                    snakemakeRepresentation
                            .append(ind(2))
                            .append(String.format("'add-path/%s'", name))
                            .append(",\n");
                });
        if (moduleNode.hasOutputTypes()) {
            // Remove the last comma
            deleteLastNCharactersFromSnakefile(2);
        }
        snakemakeRepresentation.append("\n");
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

    public void deleteLastNCharactersFromSnakefile(int numberOfCharToDel) {
        snakemakeRepresentation.delete(snakemakeRepresentation.length() - numberOfCharToDel, snakemakeRepresentation.length());
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
     * Generate the name for a step in the workflow.
     * 
     * @param moduleNode The {@link ModuleNode} that is the workflow step.
     * @return The name of the workflow step.
     */
    protected String stepName(ModuleNode moduleNode) {
        int stepNumber = moduleNode.getAutomatonState().getLocalStateNumber();
        if (stepNumber < 10) {
            return String.format("%s_0%d", moduleNode.getUsedModule().getPredicateLabel(), stepNumber);
        } else {
            return String.format("%s_%d", moduleNode.getUsedModule().getPredicateLabel(), stepNumber);
        }
    }

    /**
     * Generate the indentation at the start of a line.
     * 
     * @param level The level of indentation.
     * @return The indentation of the given level.
     */
    protected String ind(int level) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < level; i++) {
            builder.append(this.indentStyle);
        }
        return builder.toString();
    }

    /**
     * Generate the name of the input or output of a step's run input or output.
     * I.e. "moduleName_indicator_n".
     * 
     * @param moduleNode The {@link ModuleNode} that is the workflow step.
     * @param indicator  Indicator whether it is an input or an output.
     * @param n          The n-th input or output this is.
     * @return The name of the input or output.
     */
    protected String generateInputOrOutputName(ModuleNode moduleNode, String indicator, int n) {
        return String.format("%s_%s_%d",
                moduleNode.getNodeLabel(),
                indicator,
                n);
    }

}
