package nl.uu.cs.ape.automaton;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import nl.uu.cs.ape.models.enums.AtomType;

/**
 * The {@code TypeAutomaton} class is used to represent the collection of data
 * object states that exist in the workflow.
 * It comprises blocks of data objects, where each block groups inputs or
 * outputs of a specific operation.<br>
 * <br>
 * Each state (within a block) represents a data object (a specific input or
 * output data object) in the workflow.
 * <p>
 * Labeling of the automaton is provided in
 * <a href=
 * "https://ape-framework.readthedocs.io/en/latest/docs/developers/developers.html">the ReadTheDocs</a>.
 *
 * @author Vedran Kasalica
 */
public class TypeAutomaton implements Automaton {

    /**
     * Blocks of data types that are being added to the memory (usually outputs from
     * the tools, apart from the initial workflow input).
     */
    private List<Block> memoryTypesAutomaton;

    /**
     * Blocks of data types that are being used by tools from the memory (inputs to
     * the tools).
     */
    private List<Block> usedTypesAutomaton;

    /**
     * State is used in order to represent no state.
     */
    private State nullState;

    /**
     * Generate the Type State automatons based on the defined length and branching
     * factor.<br>
     * Labeling of the automaton is provided in
     * <a href=
     * "https://ape-framework.readthedocs.io/en/latest/docs/developers/developers.html">the ReadTheDocs</a>.
     *
     * @param automataBound   Length of the automaton
     * @param inputBranching  Input branching factor (max number of inputs for
     *                        modules)
     * @param outputBranching Output branching factor (max number of outputs for
     *                        modules)
     */
    public TypeAutomaton(int automataBound, int inputBranching, int outputBranching) {
        memoryTypesAutomaton = new ArrayList<>();
        usedTypesAutomaton = new ArrayList<>();
        nullState = new State(null, null, -1, inputBranching, outputBranching);

        automataBound = automataBound < 1 ? 1 : automataBound;

        for (int i = 0; i <= automataBound; i++) {

            Block tmpMemoryTypeBlock = new Block(i);

            for (int j = 0; j < outputBranching; j++) {
                State tmpMemoryState = new State(AtomType.MEMORY_TYPE, i, j, inputBranching, outputBranching);
                tmpMemoryTypeBlock.addState(tmpMemoryState);
            }
            memoryTypesAutomaton.add(tmpMemoryTypeBlock);

            Block tmpUsedTypesBlock = new Block(i);
            for (int j = 0; j < inputBranching; j++) {
                State tmpUsedState = new State(AtomType.USED_TYPE, i, j, inputBranching, outputBranching);
                tmpUsedTypesBlock.addState(tmpUsedState);
            }
            usedTypesAutomaton.add(tmpUsedTypesBlock);
        }
    }

    /**
     * Return from the automaton all the Type Blocks that contain types used by
     * tools.
     *
     * @return Blocks of data types used by tools.
     */
    public List<Block> getUsedTypesBlocks() {
        return usedTypesAutomaton;
    }

    /**
     * Return from the automaton all the Type Blocks that contain types added to the
     * memory.
     *
     * @return Blocks of data types added to the memory.
     */
    public List<Block> getMemoryTypesBlocks() {
        return memoryTypesAutomaton;
    }

    /**
     * Return from the automaton all the Type Blocks.
     *
     * @return Blocks of data types used by tools.
     */
    public List<Block> getAllBlocks() {
        List<Block> allBlocks = new ArrayList<>(usedTypesAutomaton);
        allBlocks.addAll(memoryTypesAutomaton);
        return allBlocks;
    }

    /**
     * Returns the null state.
     *
     * @return State representing a null state.
     */
    public State getNullState() {
        return nullState;
    }

    /**
     * Adds a block to the {@link #usedTypesAutomaton} list.
     *
     * @param block Add to the automaton the Type Block that contains types used by
     *              tools.
     * @return true (as specified by {@link Collection#add}
     */
    public boolean addUsedTypesBlock(Block block) {
        return usedTypesAutomaton.add(block);
    }

    /**
     * Adds a block to the {@link #memoryTypesAutomaton} list.
     *
     * @param block Add to the automaton the Type Block that contains types added to
     *              the memory.
     * @return true (as specified by {@link Collection#add}
     */
    public boolean addMemoryTypesBlock(Block block) {
        return memoryTypesAutomaton.add(block);
    }

    /**
     * Get the first memory Type state blocks from the automaton. The first memory
     * block represent the initial input to the workflow.
     *
     * @return Memory Type Block that represents workflow input.
     */
    public Block getWorkflowInputBlock() {
        Block tmp;
        try {
            tmp = getMemoryTypesBlock(0);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        return tmp;
    }

    /**
     * Get the last used Type state blocks from the automaton.
     * The last used block represent The last's tool output.
     *
     * @return The workflow output.
     */
    public Block getWorkflowOutputBlock() {
        Block tmp;
        try {
            tmp = usedTypesAutomaton.get(usedTypesAutomaton.size() - 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        return tmp;
    }

    /**
     * Get the last memory Type state blocks from the automaton.
     * The last memory block represent The last's tool output.
     *
     * @return The last's tool output Block.
     */
    public Block getLastToolOutputBlock() {
        Block tmp;
        try {
            tmp = getMemoryTypesBlock(memoryTypesAutomaton.size() - 1);
        } catch (IndexOutOfBoundsException e) {
            return null;
        }
        return tmp;
    }

    /**
     * Get from the automaton all the @i-th Type Block that contain types used by
     * tools.
     *
     * @param i Specifies number of the used type block to be returned.
     *          (inputs to the first tool are in the block 0, and n-th block
     *          contains the workflow output of the workflow of length n)
     * @return Block of Type states that are used by tools.
     */
    public Block getUsedTypesBlock(int i) {
        return usedTypesAutomaton.get(i);
    }

    /**
     * Get from the automaton all the {@code i}-th Type Block that contain types
     * added to the memory.
     *
     * @param i Specifies number of the memory type block to be returned
     *          (original workflow inputs are in the block 0, and n-th block
     *          contains the outputs of the last tool in the workflow of length n)
     * @return lock of Type states that are added to the memory.
     */
    public Block getMemoryTypesBlock(int i) {
        return memoryTypesAutomaton.get(i);
    }

    /**
     * Return all the memory type states that are generated until a certain block,
     * i.e. all the slots of memory are generated until a certain block/tool.
     *
     * @param maxBlockNo Memory block prior to which we are looking into memory
     *                   (this block is included).
     * @return List of memory States.
     */
    public List<State> getMemoryStatesUntilBlockNo(int maxBlockNo) {
        List<State> untilStates = new ArrayList<>();
        for (int i = 0; i <= maxBlockNo && i < this.usedTypesAutomaton.size(); i++) {
            Block currBlock = this.getMemoryTypesBlock(i);
            for (State currState : currBlock.getStates()) {
                untilStates.add(currState);
            }
        }
        return untilStates;
    }

    /**
     * Return all the type states that are available until (and including) a certain
     * block, i.e. all the slots of tool inputs and outputs that are created until
     * the current block.
     *
     * @param maxBlockNo Memory block until which we are looking into tool
     *                   inputs/outputs (this block is included).
     * @return List of Type States.
     */
    public List<State> getAllStatesUntilBlockNo(int maxBlockNo) {
        List<State> untilStates = new ArrayList<>();
        for (int i = 0; i <= maxBlockNo && i < this.usedTypesAutomaton.size(); i++) {
            Block currBlock = this.usedTypesAutomaton.get(i);
            for (State currState : currBlock.getStates()) {
                untilStates.add(currState);
            }
            currBlock = this.memoryTypesAutomaton.get(i);
            for (State currState : currBlock.getStates()) {
                untilStates.add(currState);
            }
        }
        return untilStates;
    }

    /**
     * Return all the memory states that are available until (and including) a
     * certain block, i.e. all the slots of tool outputs that are created until the
     * current block.
     *
     * @param maxBlockNo Memory block until which we are looking into tool
     *                   inputs/outputs (this block is included).
     * @return List of Memory Type States.
     */
    public List<State> getAllMemoryStatesUntilBlockNo(int maxBlockNo) {
        List<State> untilStates = new ArrayList<>();
        for (int i = 0; i <= maxBlockNo && i < this.usedTypesAutomaton.size(); i++) {
            Block currBlock = this.memoryTypesAutomaton.get(i);
            for (State currState : currBlock.getStates()) {
                untilStates.add(currState);
            }
        }
        return untilStates;
    }

    /**
     * Return all the memory type states that are generated after a certain block,
     * i.e. all the slots of memory are generated prior to a certain block/tool.
     *
     * @param minBlockNo Memory block after which we are looking into memory (this
     *                   block is not included).
     * @return List of memory States.
     */
    public List<State> getMemoryStatesAfterBlockNo(int minBlockNo) {
        List<State> afterStates = new ArrayList<>();
        for (int i = minBlockNo + 1; i < this.memoryTypesAutomaton.size(); i++) {
            Block currBlock = this.getMemoryTypesBlock(i);
            for (State currState : currBlock.getStates()) {
                afterStates.add(currState);
            }
        }
        return afterStates;
    }

    /**
     * Return all the type states that are used after a certain block, i.e. all the
     * slots of tool inputs that are used after current types were added to the
     * memory.
     *
     * @param minBlockNo Memory block after which we are looking into tool inputs
     *                   (this block is not included).
     * @return List of Used States.
     */
    public List<State> getUsedStatesAfterBlockNo(int minBlockNo) {
        List<State> afterStates = new ArrayList<>();
        for (int i = minBlockNo + 1; i < this.usedTypesAutomaton.size(); i++) {
            Block currBlock = this.usedTypesAutomaton.get(i);
            for (State currState : currBlock.getStates()) {
                afterStates.add(currState);
            }
        }
        return afterStates;
    }

    /*
     * (non-Javadoc)
     * 
     * @see nl.uu.cs.ape.automaton.Automaton#getAllStates()
     */
    @Override
    public List<State> getAllStates() {
        List<State> allStates = new ArrayList<>();
        for (Block currBlock : getAllBlocks()) {
            for (State currState : currBlock.getStates()) {
                allStates.add(currState);
            }
        }
        return allStates;
    }

    /**
     * Get all memory type states in the automaton.
     * 
     * @return List of memory type states.
     */
    public List<State> getAllMemoryTypesStates() {
        List<State> allMemoryStates = new ArrayList<>();
        for (Block currBlock : getMemoryTypesBlocks()) {
            for (State currState : currBlock.getStates()) {
                allMemoryStates.add(currState);
            }
        }
        return allMemoryStates;
    }

    /**
     * Get all used type states in the automaton.
     * 
     * @return List of used type states.
     */
    public List<State> getAllUsedTypesStates() {
        List<State> allUsedStates = new ArrayList<>();
        for (Block currBlock : getUsedTypesBlocks()) {
            for (State currState : currBlock.getStates()) {
                allUsedStates.add(currState);
            }
        }
        return allUsedStates;
    }

    /**
     * Return the size of the automaton, i.e., number of blocks in
     * memoryType/usedType automatons.<br>
     * <br>
     * <b>Note:</b>
     * Size of the type automaton is (workflow length + 1) as it includes the
     * workflow input and output as an additional block.
     * 
     * @return Size of type automaton.
     */
    public int getLength() {
        return this.memoryTypesAutomaton.size();
    }

}
