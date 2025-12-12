package nl.uu.cs.ape.solver.solutionStructure;

/**
 * The {@code SolutionCreationUtils} class provides general functionality
 * for creating workflow definition files as indentations and name generation.
 *
 * @author Mario Frank
 */
public class SolutionCreationUtils {


    /**
     * Generate the name for a step in the workflow.
     *
     * @param moduleNode The {@link ModuleNode} that is the workflow step.
     * @return The name of the workflow step.
     */
    public static String stepName(ModuleNode moduleNode) {
        int stepNumber = moduleNode.getAutomatonState().getLocalStateNumber();

        // The step number is automatically padded with 0 if the number is smaller than 10 (< 2 chars).
        return String.format("%s_%02d", moduleNode.getUsedModule().getPredicateLabel(), stepNumber);
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
    public static String generateInputOrOutputName(ModuleNode moduleNode, String indicator, int n) {
        return String.format("%s_%s_%d",
                moduleNode.getNodeLabel(),
                indicator,
                n);
    }


    /**
     * Generate the indentation at the start of a line.
     *
     * @param level The level of indentation.
     * @param indentStyle The indentation style.
     * @return The indentation of the given level.
     */
    public static String ind(int level, IndentStyle indentStyle) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < level; i++) {
            builder.append(indentStyle);
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
