package nl.uu.cs.ape.solver.solutionStructure;

import guru.nidi.graphviz.attribute.Rank.RankDir;
import lombok.Getter;
import nl.uu.cs.ape.automaton.Block;
import nl.uu.cs.ape.automaton.ModuleAutomaton;
import nl.uu.cs.ape.automaton.State;
import nl.uu.cs.ape.automaton.TypeAutomaton;
import nl.uu.cs.ape.utils.APEUtils;
import nl.uu.cs.ape.models.AbstractModule;
import nl.uu.cs.ape.models.AuxiliaryPredicate;
import nl.uu.cs.ape.models.Module;
import nl.uu.cs.ape.models.Type;
import nl.uu.cs.ape.models.enums.NodeType;
import nl.uu.cs.ape.models.sltlxStruc.SLTLxLiteral;
import nl.uu.cs.ape.solver.SolutionInterpreter;
import nl.uu.cs.ape.solver.minisat.SATOutput;
import nl.uu.cs.ape.solver.minisat.SATSynthesisEngine;
import nl.uu.cs.ape.solver.solutionStructure.graphviz.SolutionGraph;
import nl.uu.cs.ape.solver.solutionStructure.graphviz.SolutionGraphFactory;
import nl.uu.cs.ape.models.enums.AtomType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The {@code SolutionWorkflow} class is used to represent a single workflow
 * solution. The workflow consists of multiple instances of
 * {@link SolutionWorkflowNode}.
 *
 * @author Vedran Kasalica
 */
public class SolutionWorkflow {

    @Getter
    private static final String fileNamePrefix = "candidate_workflow_";

    /**
     * List of module nodes ordered according to their position in the workflow.
     */
    @Getter
    private List<ModuleNode> moduleNodes;

    /**
     * List of memory type nodes provided as the initial workflow input, ordered
     * according the initial description (config.json file).
     */
    @Getter
    private List<TypeNode> workflowInputTypeStates;

    /**
     * List of used type nodes provided as the final workflow output, ordered
     * according the initial description (config.json file).
     */
    @Getter
    private List<TypeNode> workflowOutputTypeStates;

    /**
     * Map of all {@code ModuleNodes}, where key value is the {@link State} provided
     * by the {@link ModuleAutomaton}.
     */
    private Map<State, ModuleNode> mappedModuleNodes;

    /**
     * Map of all {@code MemTypeNode}, where key value is the {@link State} provided
     * by the {@link TypeAutomaton}.
     */
    private Map<State, TypeNode> mappedMemoryTypeNodes;

    /**
     * Mapping used to allow us to determine the correlation between the usage of
     * data instances and the actual tools that take the instance as input.
     * A mapping is a pair of an Automaton {@link State} that depicts
     * {@link AtomType#USED_TYPE} and a {@link ModuleNode}.<br>
     * If the second is NULL, the data is used as WORKFLOW OUTPUT.
     */
    private Map<State, ModuleNode> usedType2ToolMap;

    /**
     * Non-structured solution obtained directly from the SAT output.
     */
    @Getter
    private SolutionInterpreter nativeSolution;

    /**
     * Graph representation of the control-flow workflow solution.
     */
    private SolutionGraph controlflowGraph;

    /**
     * Graph representation of the data-flow workflow solution.
     */
    private SolutionGraph dataflowGraph;

    /**
     * Graph representation of the workflow solution with styling based on the
     * Apache Taverna workflow management system.
     */
    private SolutionGraph tavernaStyleGraph;

    /**
     * Shell script used to execute the workflow.
     */
    @Getter(lazy = true)
    private final String scriptExecution = SolutionGraphFactory.generateScriptExecution(this);

    /**
     * Graphviz representation of the workflow solution in the DOT format.
     */
    @Getter(lazy = true)
    private final String graphDotFormat = SolutionGraphFactory.generateSolutionDotFormat(this);

    /**
     * Human readable text representation of the workflow solution.
     */
    @Getter(lazy = true)
    private final String readableSolution = SolutionGraphFactory.generateReadableSolution(this);

    /**
     * Index of the solution.
     */
    private int index;

    /**
     * Create the structure of the {@link SolutionWorkflow} based on the
     * {@link ModuleAutomaton} and {@link TypeAutomaton} provided.
     *
     * @throws ExceptionInInitializerError exception in case of a mismatch between
     *                                     the type of
     *                                     automaton states and workflow nodes.
     */
    private SolutionWorkflow(ModuleAutomaton toolAutomaton, TypeAutomaton typeAutomaton)
            throws ExceptionInInitializerError {
        this.moduleNodes = new ArrayList<>();
        this.workflowInputTypeStates = new ArrayList<>();
        this.workflowOutputTypeStates = new ArrayList<>();
        this.mappedModuleNodes = new HashMap<>();
        this.mappedMemoryTypeNodes = new HashMap<>();
        this.usedType2ToolMap = new HashMap<>();

        ModuleNode prev = null;
        for (State currState : toolAutomaton.getAllStates()) {
            ModuleNode currNode = new ModuleNode(currState);
            currNode.setPrevModuleNode(prev);
            if (prev != null) {
                prev.setNextModuleNode(currNode);
            }
            this.moduleNodes.add(currNode);
            this.mappedModuleNodes.put(currState, currNode);
            prev = currNode;
        }

        for (Block currBlock : typeAutomaton.getMemoryTypesBlocks()) {
            /*
             * typeGenerator represents the tool that created the current block of types as
             * output. If NULL the block is the initial workflow input.
             */
            ModuleNode typeGenerator = APEUtils.safeGet(moduleNodes, currBlock.getBlockNumber() - 1);
            for (State currState : currBlock.getStates()) {
                TypeNode currTypeNode = new TypeNode(currState);
                currTypeNode.setCreatedByModule(typeGenerator);
                if (typeGenerator != null) {
                    typeGenerator.addOutputType(currTypeNode);
                }
                this.mappedMemoryTypeNodes.put(currState, currTypeNode);
                if (currBlock.getBlockNumber() == 0) {
                    this.workflowInputTypeStates.add(currTypeNode);
                } else if (currBlock.getBlockNumber() == toolAutomaton.size()) {
                    // this.workflowOutputTypeStates.add(currTypeNode); THIS IS WRONG
                }
            }
        }
        /*
         * Use the used type blocks to define INPUT relationship between memory
         * instances and tools (that use it as input). The types that are used as final
         * workflow output are input for NULL object.
         */
        for (Block currBlock : typeAutomaton.getUsedTypesBlocks()) {
            ModuleNode inputForTool = APEUtils.safeGet(moduleNodes, currBlock.getBlockNumber());
            for (State currState : currBlock.getStates()) {
                this.usedType2ToolMap.put(currState, inputForTool);
            }
        }
    }

    /**
     * Create a solution workflow, based on the SAT output.
     *
     * @param satSolution       SAT solution, presented as array of integers.
     * @param synthesisInstance Current synthesis instance
     */
    public SolutionWorkflow(int[] satSolution, SATSynthesisEngine synthesisInstance) {
        /* Call for the default constructor. */
        this(synthesisInstance.getModuleAutomaton(), synthesisInstance.getTypeAutomaton());

        this.nativeSolution = new SATOutput(satSolution, synthesisInstance);

        for (int mappedLiteral : satSolution) {
            if (mappedLiteral >= synthesisInstance.getMappings().getInitialNumOfMappedAtoms()) {
                SLTLxLiteral currLiteral = new SLTLxLiteral(mappedLiteral,
                        synthesisInstance.getMappings());
                if (!currLiteral.isNegated()) {
                    // Skip elements that should not be presented.
                    if (currLiteral.getPredicate() instanceof AuxiliaryPredicate
                            || (currLiteral.isWorkflowElementType(AtomType.USED_TYPE)
                                    && ((Type) currLiteral.getPredicate()).isSimplePredicate())
                            || currLiteral.isWorkflowElementType(AtomType.R_RELATION)) {
                        continue;
                    } else if (currLiteral.isWorkflowElementType(AtomType.MODULE)) {
                        ModuleNode currNode = this.mappedModuleNodes.get(currLiteral.getUsedInStateArgument());
                        if (currLiteral.getPredicate() instanceof Module) {
                            currNode.setUsedModule((Module) currLiteral.getPredicate());
                        } else {
                            currNode.addAbstractDescriptionOfUsedType((AbstractModule) currLiteral.getPredicate());
                        }
                    } else if (currLiteral.isWorkflowElementType(AtomType.MEMORY_TYPE)) {
                        TypeNode currNode = this.mappedMemoryTypeNodes.get(currLiteral.getUsedInStateArgument());
                        if (currLiteral.getPredicate() instanceof Type
                                && ((Type) currLiteral.getPredicate()).isNodeType(NodeType.LEAF)) {
                            currNode.addUsedType((Type) currLiteral.getPredicate());
                        } else if ((currLiteral.getPredicate() instanceof Type)
                                && !((Type) currLiteral.getPredicate()).isNodeType(NodeType.EMPTY_LABEL)) {
                            currNode.addAbstractDescriptionOfUsedType((Type) currLiteral.getPredicate());
                        } else {
                            /* Memory type cannot be anything else except a Type. */
                        }
                    } else if (currLiteral.isWorkflowElementType(AtomType.MEM_TYPE_REFERENCE)
                            && ((State) (currLiteral.getPredicate())).getAbsoluteStateNumber() != -1) {
                        /*
                         * Add all positive literals that describe memory type references that are not
                         * pointing to null state (NULL state has AbsoluteStateNumber == -1), i.e. that
                         * are valid.
                         */
                        ModuleNode usedTypeNode = this.usedType2ToolMap.get(currLiteral.getUsedInStateArgument());
                        TypeNode memoryTypeNode = this.mappedMemoryTypeNodes.get(currLiteral.getPredicate());
                        int inputIndex = currLiteral.getUsedInStateArgument().getLocalStateNumber();
                        /* = Keep the order of inputs as they were defined in the solution file. */
                        if (usedTypeNode != null) {
                            usedTypeNode.setInputType(inputIndex, memoryTypeNode);
                        } else {
                            APEUtils.safeSet(this.workflowOutputTypeStates, inputIndex, memoryTypeNode);
                        }
                        memoryTypeNode.addUsedByTool(usedTypeNode);
                    }
                }
            }
        }

        /* Remove empty elements of the sets. */
        this.workflowInputTypeStates.removeIf(TypeNode::isEmpty);

        this.workflowOutputTypeStates.removeIf(TypeNode::isEmpty);

    }

    /**
     * Get the graphical representation of the data-flow diagram with the required
     * title and in the defined orientation.
     *
     * @param title       The title of the SolutionGraph.
     * @param orientation Orientation of the solution graph (e.g.
     *                    {@link RankDir#TOP_TO_BOTTOM}.
     * @return The solution graph.
     */
    public SolutionGraph getDataflowGraph(String title, RankDir orientation) {
        if (this.dataflowGraph == null) {
            this.dataflowGraph = SolutionGraphFactory.generateDataFlowGraph(this, title, orientation);
        }
        return this.dataflowGraph;
    }

    /**
     * Get the graphical representation of the control-flow diagram with the
     * required title and in the defined orientation.
     *
     * @param title       The title of the SolutionGraph.
     * @param orientation Orientation of the solution graph (e.g.
     *                    {@link RankDir#TOP_TO_BOTTOM}).
     * @return The solution graph.
     */
    public SolutionGraph getControlflowGraph(String title, RankDir orientation) {
        if (this.controlflowGraph == null) {
            this.controlflowGraph = SolutionGraphFactory.generateControlflowGraph(this, title, orientation);
        }
        return this.controlflowGraph;
    }

    /**
     * Get the graphical representation of the workflow solution with styling based
     * on the Apache Taverna workflow management system with the required
     * title and in the default orientation ({@link RankDir#TOP_TO_BOTTOM}).
     *
     * @param title The title of the SolutionGraph.
     * @return The solution graph.
     */
    public SolutionGraph getTavernaStyleGraph(String title) {
        if (this.tavernaStyleGraph == null) {
            this.tavernaStyleGraph = SolutionGraphFactory.generateTavernaDesignGraph(this, title);
        }
        return this.tavernaStyleGraph;
    }

    /**
     * Get file name of the solution file (without the extension).
     * 
     * @return The file name of the solution file (without the file extension).
     */
    public String getFileName() {
        return String.format("%s%d", getFileNamePrefix(), getIndex() + 1);
    }

    public String getDescriptiveName() {
        StringBuilder descrName = new StringBuilder();
        this.moduleNodes
                .forEach(moduleNode -> descrName.append(moduleNode.getUsedModule().getPredicateLabel()).append("->"));
        descrName.delete(descrName.length() - 2, descrName.length());
        return descrName.toString();
    }

    public String getDescription() {
        StringBuilder descrName = new StringBuilder();
        int stepNo = 1;
        for (ModuleNode moduleNode : this.moduleNodes) {
            descrName.append("Step ").append(stepNo++).append(": ")
                    .append(moduleNode.getUsedModule().getPredicateLabel())
                    .append("\n");
        }
        descrName.delete(descrName.length() - 1, descrName.length());
        return descrName.toString();

    }

    /**
     * Gets solution length.
     *
     * @return the solution length
     */
    public int getSolutionLength() {
        return this.moduleNodes.size();
    }

    /**
     * Sets index.
     *
     * @param i Sets the index of the solution in all the solutions.
     */
    public void setIndex(int i) {
        this.index = i;
    }

    /**
     * Gets index.
     *
     * @return The index of the solution in all the solutions.
     */
    public int getIndex() {
        return this.index;
    }
}
