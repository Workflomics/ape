package nl.uu.cs.ape.configuration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.Getter;
import nl.uu.cs.ape.configuration.tags.APEConfigDependentTag;
import nl.uu.cs.ape.configuration.tags.APEConfigTag;
import nl.uu.cs.ape.configuration.tags.APEConfigTagFactory;
import nl.uu.cs.ape.configuration.tags.APEConfigTags;
import nl.uu.cs.ape.configuration.tags.APEConfigTagFactory.TAGS.*;
import nl.uu.cs.ape.configuration.tags.validation.ValidationResults;
import nl.uu.cs.ape.domain.APEDomainSetup;
import nl.uu.cs.ape.models.Range;
import nl.uu.cs.ape.models.Type;
import nl.uu.cs.ape.models.enums.ConfigEnum;
import nl.uu.cs.ape.models.enums.SolverType;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

/**
 * The {@link APERunConfig} class is used to define the run configuration
 * variables, required for the proper execution of the synthesis process.
 *
 * @author Vedran Kasalica
 */
public class APERunConfig {
    /**
     * Path to the file with all workflow constraints.
     */
    private final APEConfigTag<JSONArray> CONSTRAINTS_FILE = new APEConfigTagFactory.TAGS.CONSTRAINTS_FILE();
    /**
     * Content of workflow constraints.
     */
    private final APEConfigTag<JSONArray> CONSTRAINTS_CONTENT = new APEConfigTagFactory.TAGS.CONSTRAINTS_CONTENT();
    /**
     * Path to the directory that will contain all the solutions to the problem.
     */
    private final APEConfigTag<Path> SOLUTION_DIR_PATH = new APEConfigTagFactory.TAGS.SOLUTION_DIR_PATH();
    /**
     * Mode is true if partial shell scripts are generated.
     */
    private final APEConfigTag<Boolean> CREATE_PARTIAL_SCRIPTS = new APEConfigTagFactory.TAGS.CREATE_PARTIAL_SCRIPTS();
    /**
     * Min and Max possible length of the solutions (length of the automaton). For
     * no upper limit, max length should be set to 0.
     */
    private final APEConfigTag<Range> SOLUTION_LENGTH_RANGE = new APEConfigTagFactory.TAGS.SOLUTION_LENGTH_RANGE();
    /**
     * Number of solution that the solver should return.
     */
    private final APEConfigTag<Integer> NO_SOLUTIONS = new NO_SOLUTIONS();
    /**
     * Number of the workflow scripts that should be generated from candidate
     * workflows. Default is 0.
     */
    private final APEConfigTag<Integer> NO_EXECUTIONS = new APEConfigTagFactory.TAGS.NO_EXECUTIONS();
    /**
     * Number of the solution graphs that should be generated from candidate
     * workflows. Default is 0.
     */
    private final APEConfigTag<Integer> NO_GRAPHS = new APEConfigTagFactory.TAGS.NO_GRAPHS();
    /**
     * Number of CWL files that should be generated from candidate workflows.
     * Default is 0.
     */
    private final APEConfigTag<Integer> NO_CWL = new APEConfigTagFactory.TAGS.NO_CWL();
    /**
     * Number of Snakemake files that should be generated from candidate workflows.
     * Default is 0.
     */
    private final APEConfigTag<Integer> NO_SNAKEMAKE = new APEConfigTagFactory.TAGS.NO_SNAKEMAKE();
    /**
     * Determines the required usage for the data instances that are given as
     * workflow input:<br>
     * {@link ConfigEnum#ALL} if all the workflow inputs have to be used,<br>
     * {@link ConfigEnum#ONE} if one of the workflow inputs should be used or <br>
     * {@link ConfigEnum#NONE} if none of the workflow inputs has to be used
     */
    private final APEConfigTag<ConfigEnum> USE_WORKFLOW_INPUT = new APEConfigTagFactory.TAGS.USE_WORKFLOW_INPUT();
    /**
     * Determines the required usage for the generated data instances:<br>
     * {@link ConfigEnum#ALL} if all the generated data has to be used,<br>
     * {@link ConfigEnum#ONE} if one of the data instances that are generated as
     * output, per tool, has to be used or <br>
     * {@link ConfigEnum#NONE} if none of the data instances is obligatory to use.
     */
    private final APEConfigTag<ConfigEnum> USE_ALL_GENERATED_DATA = new APEConfigTagFactory.TAGS.USE_ALL_GENERATED_DATA();
    /**
     * Mode is true if debug mode is turned on.
     */
    private final APEConfigTag<Boolean> DEBUG_MODE = new APEConfigTagFactory.TAGS.DEBUG_MODE();
    /**
     * Synthesis timeout in seconds.
     */
    public final APEConfigTag<Integer> TIMEOUT_SEC = new APEConfigTagFactory.TAGS.TIMEOUT_SEC();
    /**
     * false iff the provided solutions should be distinguished based on the tool
     * sequences alone, i.e. tool sequences cannot repeat, ignoring the types in the
     * solutions.
     */
    private final APEConfigTag<Boolean> TOOL_SEQ_REPEAT = new APEConfigTagFactory.TAGS.TOOL_SEQ_REPEAT();
    /**
     * Input types of the workflow.
     */
    private final APEConfigDependentTag.One<List<Type>, APEDomainSetup> PROGRAM_INPUTS = new APEConfigTagFactory.TAGS.PROGRAM_INPUTS(
            this::getApeDomainSetup);
    /**
     * Output types of the workflow.
     */
    private final APEConfigDependentTag.One<List<Type>, APEDomainSetup> PROGRAM_OUTPUTS = new APEConfigTagFactory.TAGS.PROGRAM_OUTPUTS(
            this::getApeDomainSetup);
    /**
     * All the Tags specified in this class. Should be in correct order of
     * dependencies.
     */
    private final APEConfigTag<?>[] all_tags = new APEConfigTag[] {
            this.CONSTRAINTS_FILE,
            this.CONSTRAINTS_CONTENT,
            this.SOLUTION_DIR_PATH,
            this.CREATE_PARTIAL_SCRIPTS,
            this.SOLUTION_LENGTH_RANGE,
            this.NO_SOLUTIONS,
            this.NO_EXECUTIONS,
            this.NO_GRAPHS,
            this.NO_CWL,
            this.NO_SNAKEMAKE,
            this.USE_WORKFLOW_INPUT,
            this.USE_ALL_GENERATED_DATA,
            this.DEBUG_MODE,
            this.TIMEOUT_SEC,
            this.TOOL_SEQ_REPEAT,
            this.PROGRAM_OUTPUTS,
            this.PROGRAM_INPUTS
    };

    /**
     * Static versions of the Tags specified in this class. Should be in correct
     * order for the Web API.
     */
    public static final APEConfigTags TAGS = new APEConfigTags(
            new CONSTRAINTS_FILE(),
            new CONSTRAINTS_CONTENT(),
            new SOLUTION_DIR_PATH(),
            new CREATE_PARTIAL_SCRIPTS(),
            new SOLUTION_LENGTH_RANGE(),
            new NO_SOLUTIONS(),
            new NO_EXECUTIONS(),
            new NO_GRAPHS(),
            new NO_CWL(),
            new NO_SNAKEMAKE(),
            new USE_WORKFLOW_INPUT(),
            new USE_ALL_GENERATED_DATA(),
            new DEBUG_MODE(),
            new TIMEOUT_SEC(),
            new TOOL_SEQ_REPEAT(),
            new PROGRAM_OUTPUTS(null),
            new PROGRAM_INPUTS(null));

    /**
     * Object containing domain information needed for the execution.
     */
    @Getter
    private APEDomainSetup apeDomainSetup;

    /** Solver type that should be used (SAT). */
    private SolverType solverType = SolverType.SAT;

    /**
     * Constructor used to implement the Builder Pattern.
     *
     * @param builder Builder object
     */
    private APERunConfig(Builder builder) {

        if (builder.apeDomainSetup == null) {
            throw new APEConfigException("Domain setup provided cannot have null value.");
        }

        this.apeDomainSetup = builder.apeDomainSetup;

        setConstraintsJSON(builder.constraintsJSON);
        setSolutionLength(builder.solutionMinLength, builder.solutionMaxLength);
        setMaxNoSolutions(builder.maxNoSolutions);
        setToolSeqRepeat(builder.toolSeqRepeat);
        setSolutionPath(builder.solutionDirPath);
        setCreatePartialScrips(builder.createPartialScripts);
        setNoExecutions(builder.noExecutions);
        setNoGraphs(builder.noGraphs);
        setNoCWL(builder.noCWL);
        setNoSnakemake(builder.noSnakemake);
        setUseWorkflowInput(builder.useWorkflowInput);
        setUseAllGeneratedData(builder.useAllGeneratedData);
        setDebugMode(builder.debugMode);
        setTimeoutSec(builder.timeoutSec);
        setProgramInputs(builder.programInputs);
        setProgramOutputs(builder.programOutputs);
    }

    /**
     * Private constructor used by
     * {@link APERunConfig#validate(JSONObject config, APEDomainSetup setup)}
     * to create an empty instance.
     */
    private APERunConfig(APEDomainSetup setup) {
        this.apeDomainSetup = setup;
    }

    /**
     * Validate the JSONObject for each RUN tag.
     * If {@link ValidationResults#success()} ()} returns true,
     * the configuration object can be safely used to create
     * an APERunConfig object.
     *
     * @param json  the configuration file
     * @param setup the domain setup
     * @return the validation results
     */
    public static ValidationResults validate(JSONObject json, APEDomainSetup setup) {
        APERunConfig dummy = new APERunConfig(setup);
        ValidationResults results = new ValidationResults();
        for (APEConfigTag<?> tag : dummy.all_tags) {
            results.add(tag.validateConfig(json));
            if (results.hasFails()) {
                return results;
            } else {
                tag.setValueFromConfig(json); // for dependencies
            }
        }
        return results;
    }

    /**
     * Setup the configuration for the current run of the synthesis.
     *
     * @param runConfiguration The APE configuration {@link JSONObject}.
     * @param apeDomainSetup   The ape domain setup
     * @throws IOException        Error in reading the configuration file.
     * @throws JSONException      Error in parsing the configuration file.
     * @throws APEConfigException Error in setting up the the configuration.
     */
    public APERunConfig(JSONObject runConfiguration, APEDomainSetup apeDomainSetup)
            throws IOException, JSONException, APEConfigException {

        /* JSONObject must have been parsed correctly. */
        if (runConfiguration == null) {
            throw new APEConfigException(
                    "Cannot set up the run configuration, because the JSONObject is initialized to NULL. The configuration file might not have been parsed correctly.");
        }
        if (apeDomainSetup == null) {
            throw new APEConfigException("Domain setup provided cannot have null value.");
        }

        this.apeDomainSetup = apeDomainSetup;

        // set the apeDomain BEFORE setting the tags
        for (APEConfigTag<?> tag : all_tags) {
            tag.setValueFromConfig(runConfiguration);
        }
    }

    /**
     * Creates builder to build {@link APERunConfig}.
     *
     * @return created builder
     */
    public static ISolutionMinLengthStage builder() {
        return new Builder();
    }

    /**
     * Gets constraints as a JSONArray, represented as either
     * {@link #CONSTRAINTS_CONTENT} or {@link #CONSTRAINTS_FILE}.
     *
     * @return the constraints as a JSONArray.
     */
    public JSONArray getConstraintsJSON() {
        return CONSTRAINTS_CONTENT.getValue() != null ? CONSTRAINTS_CONTENT.getValue()
                : CONSTRAINTS_FILE.getValue();
    }

    /**
     * Set the constrains.
     * 
     * @param constraintsJSON JSON object that contains the constraints
     */
    public void setConstraintsJSON(JSONArray constraintsJSON) {
        CONSTRAINTS_CONTENT.setValue(constraintsJSON);
    }

    /**
     * Returns false if the provided solutions should be distinguished based on the
     * tool sequences alone, i.e. tool sequences cannot repeat, ignoring the types
     * in the solutions.
     *
     * @return {@code false} if tool sequences cannot repeat, ignoring the types in
     *         the
     *         solutions, or {@code true} in case that the tool sequences can repeat
     *         as
     *         long as the corresponding types differ.
     */
    public boolean getAllowToolSeqRepeat() {
        return TOOL_SEQ_REPEAT.getValue();
    }

    /**
     * @param toolSeqRepeat the toolSeqRepeat to set
     */
    public void setToolSeqRepeat(boolean toolSeqRepeat) {
        TOOL_SEQ_REPEAT.setValue(toolSeqRepeat);
    }

    /**
     * Gets solution path.
     *
     * @return the value of {@link #SOLUTION_DIR_PATH}
     */
    public Path getSolutionDirPath() {
        return SOLUTION_DIR_PATH.getValue();
    }

    /**
     * Get the path of the relative path in solution_dir_path.
     *
     * @param relativePath the relative path
     *
     * @return absolute path of the relative path in solution_dir_path
     */
    public Path getSolutionDirPath2(String relativePath) {
        // relative paths should not start with '/' or '\'
        if (relativePath.startsWith("/") || relativePath.startsWith("\\")) {
            return getSolutionDirPath().resolve(relativePath.substring(1));
        }
        return getSolutionDirPath().resolve(relativePath);
    }

    /** Tag value. */
    public static final String EXECUTABLES_FOLDER_NAME = "Executables";

    /**
     * Get the path to the directory where the executable scripts corresponding to
     * the given solutions should be stored.
     *
     * @return the path to the directory where the executable scripts corresponding
     *         to the given solutions should be stored
     */
    public Path getSolutionDirPath2Executables() {
        return getSolutionDirPath2(EXECUTABLES_FOLDER_NAME);
    }

    /** Tag value. */
    public static final String FIGURES_FOLDER_NAME = "Figures";

    /**
     * Get the path to the directory where the graphs representation of the
     * solutions should be stored.
     *
     * @return the path to the directory where the graphs representation of the
     *         solutions should be stored
     */
    public Path getSolutionDirPath2Figures() {
        return getSolutionDirPath2(FIGURES_FOLDER_NAME);
    }

    /** Tag value. */
    public static final String CWL_FOLDER_NAME = "CWL";

    /**
     * Get the path to the directory where the CWL scripts corresponding the given
     * solutions should be stored.
     *
     * @return the path to the directory where the CWL scripts corresponding to the
     *         given solutions should be stored
     */
    public Path getSolutionDirPath2CWL() {
        return getSolutionDirPath2(CWL_FOLDER_NAME);
    }

    /** Tag value. */
    public static final String SNAKEMAKE_FOLDER_NAME = "Snakemake";

    /**
     * Get the path to the directory where the snakemake scripts corresponding the given
     * solutions should be stored.
     *
     * @return the path to the directory where the snakemake scripts corresponding to the
     *         given solutions should be stored
     */
    public Path getSolutionDirPath2Snakemake() {
        return getSolutionDirPath2(SNAKEMAKE_FOLDER_NAME);
    }

    /** Tag value. */
    public static final String EXECUTABLE_CWL_FOLDER_NAME = "CWL_executables";

    /**
     * Get the path to the directory where the executable CWL scripts corresponding
     * the given solutions should be stored.
     *
     * @return the path to the directory where the executable CWL scripts
     *         corresponding to the given solutions should be stored
     */
    public Path getSolutionDirPath2ExecutableCWL() {
        return getSolutionDirPath2(EXECUTABLE_CWL_FOLDER_NAME);
    }

    /**
     * @param solutionPath the solutionPath to set
     */
    public void setSolutionPath(String solutionPath) {
        SOLUTION_DIR_PATH.setValue(Paths.get(solutionPath));
    }

    /**
     * Gets partial script output mode.
     *
     * @return the value of {@link #CREATE_PARTIAL_SCRIPTS}
     */
    public boolean getCreatePartialScripts() {
        return CREATE_PARTIAL_SCRIPTS.getValue();
    }

    /**
     * @param createPartialScripts the partial script output mode to set
     */
    public void setCreatePartialScrips(boolean createPartialScripts) {
        CREATE_PARTIAL_SCRIPTS.setValue(createPartialScripts);
    }

    /**
     * Gets solution min and max length.
     *
     * @return the value of {@link #SOLUTION_LENGTH_RANGE}
     */
    public Range getSolutionLength() {
        return SOLUTION_LENGTH_RANGE.getValue();
    }

    /**
     * Gets max no solutions.
     *
     * @return the value of {@link #NO_SOLUTIONS}
     */
    public int getMaxNoSolutions() {
        return NO_SOLUTIONS.getValue();
    }

    /**
     * @param maxNoSolutions the maxNoSolutions to set
     */
    public void setMaxNoSolutions(int maxNoSolutions) {
        NO_SOLUTIONS.setValue(maxNoSolutions);
    }

    /**
     * Gets no executions.
     *
     * @return the value of {@link #NO_EXECUTIONS}
     */
    public int getNoExecutions() {
        return NO_EXECUTIONS.getValue();
    }

    /**
     * @param noExecutions the noExecutions to set
     */
    public void setNoExecutions(int noExecutions) {
        NO_EXECUTIONS.setValue(noExecutions);
    }

    /**
     * Gets no graphs.
     *
     * @return the value of {@link #NO_GRAPHS}
     */
    public int getNoGraphs() {
        return NO_GRAPHS.getValue();
    }

    /**
     * @param noGraphs the noGraphs to set
     */
    public void setNoGraphs(int noGraphs) {
        NO_GRAPHS.setValue(noGraphs);
    }

    /**
     * Gets number of CWL files.
     * 
     * @return The value of {@link #NO_CWL}
     */
    public int getNoCWL() {
        return NO_CWL.getValue();
    }

    /**
     * Set the number of CWL files.
     * 
     * @param noCWL The number to set.
     */
    public void setNoCWL(int noCWL) {
        NO_CWL.setValue(noCWL);
    }

    /**
     * Gets number of Snakemake files.
     * 
     * @return The value of {@link #NO_SNAKEMAKE}
     */
    public int getNoSnakemake() {
        return NO_SNAKEMAKE.getValue();
    }

    /**
     * Set the number of Snakemake files.
     * 
     * @param noSnakemake The number to set.
     */
    public void setNoSnakemake(int noSnakemake) {
        NO_SNAKEMAKE.setValue(noSnakemake);
    }

    /**
     * Gets program inputs.
     *
     * @return the value of {@link #PROGRAM_INPUTS}
     */
    public List<Type> getProgramInputs() {
        return PROGRAM_INPUTS.getValue();
    }

    /**
     * @param programInputs the programInputs to set
     */
    public void setProgramInputs(List<Type> programInputs) {
        PROGRAM_INPUTS.setValue(programInputs);
    }

    /**
     * Gets program outputs.
     *
     * @return the value of {@link #PROGRAM_OUTPUTS}
     */
    public List<Type> getProgramOutputs() {
        return PROGRAM_OUTPUTS.getValue();
    }

    /**
     * @param programOutputs the programOutputs to set
     */
    public void setProgramOutputs(List<Type> programOutputs) {
        PROGRAM_OUTPUTS.setValue(programOutputs);
    }

    /**
     * Gets use workflow input.
     *
     * @return the value of {@link #USE_WORKFLOW_INPUT}
     */
    public ConfigEnum getUseWorkflowInput() {
        return USE_WORKFLOW_INPUT.getValue();
    }

    /**
     * @param useWorkflowInput the useWorkflowInput to set
     */
    public void setUseWorkflowInput(ConfigEnum useWorkflowInput) {
        USE_WORKFLOW_INPUT.setValue(useWorkflowInput);
    }

    /**
     * Gets all generated data.
     *
     * @return the value of {@link #USE_ALL_GENERATED_DATA}
     */
    public ConfigEnum getUseAllGeneratedData() {
        return USE_ALL_GENERATED_DATA.getValue();
    }

    /**
     * @param useAllGeneratedData the useAllGeneratedData to set
     */
    public void setUseAllGeneratedData(ConfigEnum useAllGeneratedData) {
        USE_ALL_GENERATED_DATA.setValue(useAllGeneratedData);
    }

    /**
     * Get timeout (in seconds) how long the execution should last.
     * 
     * @return Timeout in seconds.
     */
    public int getTimeoutSec() {
        return TIMEOUT_SEC.getValue();
    }

    /**
     * Get timeout (in ms) how long the execution should last.
     * 
     * @return Timeout in seconds.
     */
    public int getTimeoutMs() {
        return TIMEOUT_SEC.getValue() * 1000;
    }

    /**
     * Set the timeout in sec.
     * 
     * @param timeoutSec Timeout in sec.
     */
    public void setTimeoutSec(int timeoutSec) {
        TIMEOUT_SEC.setValue(timeoutSec);
    }

    /**
     * Gets debug mode.
     *
     * @return the value of {@link #DEBUG_MODE}
     */
    public boolean getDebugMode() {
        return DEBUG_MODE.getValue();
    }

    /**
     * @param debugMode the debugMode to set
     */
    public void setDebugMode(boolean debugMode) {
        DEBUG_MODE.setValue(debugMode);
    }

    /**
     * @param solutionMinLength the solutionMinLength to set
     * @param solutionMaxLength the solutionMaxLength to set
     */
    public void setSolutionLength(int solutionMinLength, int solutionMaxLength) {
        this.SOLUTION_LENGTH_RANGE.setValue(Range.of(solutionMinLength, solutionMaxLength));
    }

    /**
     * Gets the Solver type that should be used for solving.
     * 
     * @return {@link SolverType} that corresponds to the solver type
     */
    public SolverType getSolverType() {
        return this.solverType;
    }

    /**
     * @param solverType the solverType to set
     */
    public void setSolverType(SolverType solverType) {
        this.solverType = solverType;
    }

    /**
     * Creates interface for the min length of {@link APERunConfig}.
     */
    public interface ISolutionMinLengthStage {
        ISolutionMaxLengthStage withSolutionMinLength(int solutionMinLength);
    }

    /**
     * Creates interface for the max length of {@link APERunConfig}.
     */
    public interface ISolutionMaxLengthStage {
        /**
         * Set Max length.
         * 
         * @param solutionMaxLength
         * @return
         */
        IMaxNoSolutionsStage withSolutionMaxLength(int solutionMaxLength);
    }

    /**
     * Creates interface for the max solution no of {@link APERunConfig}.
     */
    public interface IMaxNoSolutionsStage {
        IApeDomainSetupStage withMaxNoSolutions(int maxNoSolutions);
    }

    /**
     * Creates interface to setup the domain of {@link APERunConfig}.
     */
    public interface IApeDomainSetupStage {
        IBuildStage withApeDomainSetup(APEDomainSetup apeDomainSetup);
    }

    /**
     * Interface for the builder class.
     * 
     * @author Vedran Kasalica
     *
     */
    public interface IBuildStage {
        IBuildStage withConstraintsJSON(JSONArray constraintsJSON);

        IBuildStage withToolSeqRepeat(boolean toolSeqRepeat);

        IBuildStage withSolutionDirPath(String solutionPath);

        IBuildStage withCreatePartialScripts(boolean createPartialScripts);

        IBuildStage withNoExecutions(int noExecutions);

        IBuildStage withNoGraphs(int noGraphs);

        IBuildStage withNoCWL(int noCWL);

        IBuildStage withNoSnakemake(int noSnakemake);

        IBuildStage withProgramInputs(List<Type> programInputs);

        IBuildStage withProgramOutputs(List<Type> programOutputs);

        IBuildStage withUseWorkflowInput(ConfigEnum useWorkflowInput);

        IBuildStage withUseAllGeneratedData(ConfigEnum useAllGeneratedData);

        IBuildStage withDebugMode(boolean debugMode);

        IBuildStage withTimeoutSec(int timeoutSec);

        APERunConfig build();
    }

    /**
     * Builder to build {@link APERunConfig}.
     */
    public static final class Builder implements ISolutionMinLengthStage, ISolutionMaxLengthStage, IMaxNoSolutionsStage,
            IApeDomainSetupStage, IBuildStage {
        private int solutionMinLength;
        private int solutionMaxLength;
        private int maxNoSolutions;
        private APEDomainSetup apeDomainSetup;
        private JSONArray constraintsJSON;
        private boolean toolSeqRepeat;
        private String solutionDirPath;
        private boolean createPartialScripts;
        private int noExecutions;
        private int noGraphs;
        private int noCWL;
        private int noSnakemake;
        private List<Type> programInputs = Collections.emptyList();
        private List<Type> programOutputs = Collections.emptyList();
        private ConfigEnum useWorkflowInput;
        private ConfigEnum useAllGeneratedData;
        private boolean debugMode;
        private int timeoutSec;

        private Builder() {
        }

        @Override
        public ISolutionMaxLengthStage withSolutionMinLength(int solutionMinLength) {
            this.solutionMinLength = solutionMinLength;
            return this;
        }

        @Override
        public IMaxNoSolutionsStage withSolutionMaxLength(int solutionMaxLength) {
            this.solutionMaxLength = solutionMaxLength;
            return this;
        }

        @Override
        public IApeDomainSetupStage withMaxNoSolutions(int maxNoSolutions) {
            this.maxNoSolutions = maxNoSolutions;
            return this;
        }

        @Override
        public IBuildStage withApeDomainSetup(APEDomainSetup apeDomainSetup) {
            this.apeDomainSetup = apeDomainSetup;
            return this;
        }

        @Override
        public IBuildStage withConstraintsJSON(JSONArray constraintsJSON) {
            this.constraintsJSON = constraintsJSON;
            return this;
        }

        @Override
        public IBuildStage withToolSeqRepeat(boolean toolSeqRepeat) {
            this.toolSeqRepeat = toolSeqRepeat;
            return this;
        }

        @Override
        public IBuildStage withSolutionDirPath(String solutionDirPath) {
            this.solutionDirPath = solutionDirPath;
            return this;
        }

        @Override
        public IBuildStage withCreatePartialScripts(boolean createPartialScripts) {
            this.createPartialScripts = createPartialScripts;
            return this;
        }

        @Override
        public IBuildStage withNoExecutions(int noExecutions) {
            this.noExecutions = noExecutions;
            return this;
        }

        @Override
        public IBuildStage withNoGraphs(int noGraphs) {
            this.noGraphs = noGraphs;
            return this;
        }

        @Override
        public IBuildStage withNoCWL(int noCWL) {
            this.noCWL = noCWL;
            return this;
        }

        @Override
        public IBuildStage withNoSnakemake(int noSnakemake) {
            this.noSnakemake = noSnakemake;
            return this;
        }

        @Override
        public IBuildStage withProgramInputs(List<Type> programInputs) {
            this.programInputs = programInputs;
            return this;
        }

        @Override
        public IBuildStage withProgramOutputs(List<Type> programOutputs) {
            this.programOutputs = programOutputs;
            return this;
        }

        @Override
        public IBuildStage withUseWorkflowInput(ConfigEnum useWorkflowInput) {
            this.useWorkflowInput = useWorkflowInput;
            return this;
        }

        @Override
        public IBuildStage withUseAllGeneratedData(ConfigEnum useAllGeneratedData) {
            this.useAllGeneratedData = useAllGeneratedData;
            return this;
        }

        @Override
        public IBuildStage withDebugMode(boolean debugMode) {
            this.debugMode = debugMode;
            return this;
        }

        @Override
        public IBuildStage withTimeoutSec(int timeout) {
            this.timeoutSec = timeout;
            return this;
        }

        @Override
        public APERunConfig build() {
            return new APERunConfig(this);
        }
    }

}
