package nl.uu.cs.ape.solver.solutionStructure.cwl;

import nl.uu.cs.ape.solver.solutionStructure.ModuleNode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import nl.uu.cs.ape.models.AuxTypePredicate;
import nl.uu.cs.ape.models.Module;
import nl.uu.cs.ape.models.Type;
import nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate;

/**
 * Base class with shared behavior for CWL export classes.
 */
public abstract class CWLToolBase {
    /**
     * Cwl representation.
     */
    protected final StringBuilder cwlRepresentation;
    /**
     * Tool depicted in the CWL file.
     */
    protected Module tool;
    /**
     * Indent style used.
     */
    private IndentStyle indentStyle;

    /**
     * Generate the creator base from the workflow solution.
     * 
     * @param tool APE workflow solution
     */
    protected CWLToolBase(Module tool) {
        this.tool = tool;
        this.cwlRepresentation = new StringBuilder();
        this.indentStyle = IndentStyle.SPACES2;
    }

    /**
     * Override the default indentation style.
     * 
     * @param indentStyle The indentation style to use.
     * @return A reference to this CWLCreator.
     */
    public CWLToolBase setIndentStyle(IndentStyle indentStyle) {
        this.indentStyle = indentStyle;
        return this;
    }

    /**
     * Get the CWL version required for the CWL file.
     * 
     * @return The required CWL version.
     */
    public String getCWLVersion() {
        return "v1.2";
    }

    /**
     * Generates the CWL representation.
     * 
     * @return The CWL representation.
     */
    public String generate() {
        // Top of file comment
        generateTopComment();

        cwlRepresentation.append(String.format("cwlVersion: %s%n", getCWLVersion()));
        cwlRepresentation.append("class: CommandLineTool").append("\n");

        cwlRepresentation.append("baseCommand: ").append(tool.getPredicateID()).append("\n");
        cwlRepresentation.append("label: ").append(tool.getPredicateLabel()).append("\n");

        // Add requirements (generic)
        cwlRepresentation.append("requirements:\n");
        cwlRepresentation.append("  ShellCommandRequirement: {}\n");
        cwlRepresentation.append("  InitialWorkDirRequirement:\n");
        cwlRepresentation.append("    listing:\n");

        // Dynamically add inputs to InitialWorkDirRequirement listing
        for (String input : this.getFormats(tool.getModuleInput(), "-").keySet()) {
            cwlRepresentation.append("      - $(inputs.").append(input).append(")\n");
        }

        // Add DockerRequirement dynamically
        cwlRepresentation.append("  DockerRequirement:\n");
        cwlRepresentation.append("    dockerPull: fix-this-path/").append(tool.getPredicateLabel()).append("\n")
                .append("\n");

        cwlRepresentation.append("$namespaces:\n");
        cwlRepresentation.append("  edam: http://edamontology.org/\n");
        cwlRepresentation.append("\n");

        generateCWLRepresentation();

        return cwlRepresentation.toString();
    }

    /**
     * Get the map of input names and their data formats.
     * 
     * @return The map of input names and their data formats.
     */
    protected Map<String, List<TaxonomyPredicate>> getFormats(List<Type> toolIOs, String nameSuffix) {
        int i = 0;
        Map<String, List<TaxonomyPredicate>> inputNames = new HashMap<>();
        for (Type inType : toolIOs) {
            List<TaxonomyPredicate> dataFormat = new ArrayList<>();
            if (inType instanceof AuxTypePredicate) {
                dataFormat = extractFormat((AuxTypePredicate) inType);
            }

            inputNames.put(tool.getPredicateLabel() + "_" + nameSuffix + "_" + i++, dataFormat);

        }
        return inputNames;
    }

    /**
     * Get the map of input names and their data types.
     * 
     * @return The map of input names and their data types.
     */
    protected Map<String, List<TaxonomyPredicate>> getTypes(List<Type> toolIOs, String nameSuffix) {
        int i = 0;
        Map<String, List<TaxonomyPredicate>> inputNames = new HashMap<>();
        for (Type inType : toolIOs) {
            List<TaxonomyPredicate> dataType = new ArrayList<>();
            if (inType instanceof AuxTypePredicate) {
                dataType = extractDataType((AuxTypePredicate) inType);
            }

            inputNames.put(tool.getPredicateLabel() + "_" + nameSuffix + "_" + i++, dataType);

        }
        return inputNames;
    }

    private List<TaxonomyPredicate> extractFormat(AuxTypePredicate auxType) {
        List<TaxonomyPredicate> formatTypes = new ArrayList<>();
        for (TaxonomyPredicate subtype : auxType.getGeneralizedPredicates()) {
            if (subtype instanceof AuxTypePredicate) {
                formatTypes.addAll(extractFormat((AuxTypePredicate) subtype));
            } else if (subtype.getRootNodeID().equals("http://edamontology.org/format_1915")) {
                formatTypes.add(subtype);
            }
        }
        return formatTypes;
    }

    private List<TaxonomyPredicate> extractDataType(AuxTypePredicate auxType) {
        List<TaxonomyPredicate> dataTypes = new ArrayList<>();
        for (TaxonomyPredicate subtype : auxType.getGeneralizedPredicates()) {
            if (subtype instanceof AuxTypePredicate) {
                dataTypes.addAll(extractDataType((AuxTypePredicate) subtype));
            } else if (subtype.getRootNodeID().equals("http://edamontology.org/data_0006")) {
                dataTypes.add(subtype);
            }
        }
        return dataTypes;
    }

    /**
     * Adds the comment at the top of the file.
     */
    protected void generateTopComment() {
        cwlRepresentation.append(
                "# The template for this tool description is generated by APE (https://github.com/workflomics/ape).\n");
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
     * Generate the main part of the CWL representation.
     */
    protected abstract void generateCWLRepresentation();

    /**
     * Gets the CWL representation.
     * 
     * @return The CWL representation.
     */
    public String getCWL() {
        return cwlRepresentation.toString();
    }

    /**
     * Delete a number of characters at the end of the CWL file.
     * 
     * @param numberOfCharToDel The number of characters to remove.
     */
    protected void deleteLastNCharactersFromCWL(int numberOfCharToDel) {
        cwlRepresentation.delete(cwlRepresentation.length() - numberOfCharToDel, cwlRepresentation.length());
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
     * The available indentation styles.
     */
    public enum IndentStyle {
        /** Two spaces for indentation. */
        SPACES2("  "),
        /** Four spaces for indentation. */
        SPACES4("    ");

        private final String text;

        IndentStyle(String s) {
            this.text = s;
        }

        @Override
        public String toString() {
            return this.text;
        }
    }
}
