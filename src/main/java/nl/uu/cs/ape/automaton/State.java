package nl.uu.cs.ape.automaton;

import nl.uu.cs.ape.models.enums.AtomType;
import nl.uu.cs.ape.models.logic.constructs.PredicateLabel;

/***
 * The {@code State} class is used to represent the states in module and type
 * automatons. Automaton corresponds to the structure of the possible solutions
 * of the synthesis, i.e. it represents the structure that the provided
 * solutions will follow.
 * <p>
 * Labeling of the automaton is provided in
 * <a href=
 * "https://ape-framework.readthedocs.io/en/latest/docs/developers/developers.html">the ReadTheDocs</a>.
 *
 * @author Vedran Kasalica
 */
public class State implements PredicateLabel, StateInterface {

    /** Unique name of the state */
    private final String stateName;
    /** Local number of the state (within the block) */
    private final int localStateNumber;
    /**
     * Order number of the state with respect to the state type (i.e., unique number
     * within the same type)
     */
    private final int typeDependantStateNumber;
    /** Absolute order number of the state (i.e., unique number per state). */
    private final int absoluteStateNumber;
    /** Type of the state */
    private final AtomType workflowStateType;

    /**
     * Creates a state that corresponds to a state of the overall solution workflow.
     *
     * @param workflowStateType Parameter determining the state type.
     * @param blockNumber       Corresponds to the block number within the type
     *                          automaton (not applicable for the module automaton).
     * @param stateNumber       Corresponds to the state number within block.
     * @param inputBranching    Max number of inputs per module.
     * @param outputBranching   Max number of outputs per module.
     */
    public State(AtomType workflowStateType, Integer blockNumber, int stateNumber, int inputBranching,
            int outputBranching) {

        this.stateName = AtomType.getStringShortcut(workflowStateType, blockNumber, stateNumber);
        this.localStateNumber = stateNumber;
        this.typeDependantStateNumber = calculateAutomatonStateNumber(blockNumber, stateNumber, inputBranching,
                outputBranching, workflowStateType);
        this.absoluteStateNumber = calculateAbsStateNumber(blockNumber, stateNumber, inputBranching, outputBranching,
                workflowStateType);
        this.workflowStateType = workflowStateType;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + absoluteStateNumber;
        result = prime * result + ((stateName == null) ? 0 : stateName.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        State other = (State) obj;
        if (absoluteStateNumber != other.absoluteStateNumber)
            return false;
        if (stateName == null) {
            if (other.stateName != null)
                return false;
        } else if (!stateName.equals(other.stateName))
            return false;
        return true;
    }

    /**
     * Compares the two States based on their order in the solution.
     *
     * @return Negative integer, zero, or a positive integer as this object is
     *         before than, equal to, or after than the specified State.
     */
    @Override
    public int compareTo(PredicateLabel other) {
        if (!(other instanceof State)) {
            return this.getPredicateID().compareTo(other.getPredicateID());
        }
        State otherState = (State) other;
        int diff = 0;
        if ((diff = Integer.compare(this.absoluteStateNumber, otherState.absoluteStateNumber)) != 0) {
            return diff;
        } else {
            return this.getPredicateID().compareTo(otherState.getPredicateID());
        }
    }

    /**
     * Returns the text representing the state (e.g. M04 or T02.31).
     *
     * @return String representation of the state.
     */
    public String getPredicateID() {
        return this.stateName;
    }

    /**
     * Returns the text representing the state (e.g. M04 or T02.31).
     *
     * @return String representation of the state.
     */
    public String getPredicateLabel() {
        return this.stateName;
    }

    /**
     * Returns the text representing the state (e.g. M04 or T02.31).
     *
     * @return String representation of the state.
     */
    public String getPredicateLongLabel() {
        return this.stateName;
    }

    /**
     * Returns the order number of the state in the respective array of states.
     *
     * @return Order number of the state (within the block). Unlike the type states,
     *         tool state indexing starts with index 1.
     */
    public int getLocalStateNumber() {
        return this.localStateNumber;
    }

    /**
     * Returns the order number of the state with respect to the State Type
     * ({@link AtomType}). Unlike {@link #getLocalStateNumber}, this function
     * returns number that can be used to compare ordering of any 2 states of the
     * same type in the system,
     * disregarding the block. null memory state has
     * {@link #typeDependantStateNumber} = -1.
     *
     * @return Non-negative number that corresponds to the order number of the state
     *         within the same type or -1 for null memory state.
     */
    public int getTypeDependantStateNumber() {
        return this.typeDependantStateNumber;
    }

    /**
     * Returns the absolute order number of the state within the whole workflow.
     * Unlike {@link #getLocalStateNumber}, this function returns number that can be
     * used to compare ordering of any 2 states in the system,
     * disregarding the block or their type (data type, tool, etc.). null state has
     * AbsoluteStateNumber -1.
     *
     * @return Non-negative number that corresponds to the absolute order number of
     *         the state or -1 for null state.
     */
    public int getAbsoluteStateNumber() {
        return this.absoluteStateNumber;
    }

    /**
     * Get the type of the state (e.g. tool, memory type, etc.).
     *
     * @return The {@link AtomType} that describes the state.
     */
    public AtomType getWorkflowStateType() {
        return this.workflowStateType;
    }

    /**
     * Function used to calculate the absolute order number of a state based on the
     * information regarding its block number, order number within the block and
     * type of the state.
     * <p>
     * {@link AtomType#MEMORY_TYPE} corresponds to the Memory Type State,<br>
     * {@link AtomType#USED_TYPE} corresponds to the Used Type State,<br>
     * {@link AtomType#MODULE} corresponds to the Module/Tool State.
     *
     * @param blockNumber     Corresponds to the block number within the type
     *                        automaton (not applicable for the module automaton).
     * @param stateNumber     Corresponds to the state number within block.
     * @param inputBranching  Max number of inputs per module.
     * @param outputBranching Max number of outputs per module.
     * @param typeOfTheState  Parameter determining the state type.
     * @return The calculated absolute order number of the state.
     */
    private static int calculateAbsStateNumber(Integer blockNumber, int stateNumber, int inputBranching,
            int outputBranching, AtomType typeOfTheState) {
        int absOrderNumber = -1;

        if (typeOfTheState == AtomType.MEMORY_TYPE) { /* Case: Memory Type State */
            absOrderNumber = (blockNumber * (inputBranching + outputBranching)) + blockNumber + stateNumber;
        } else if (typeOfTheState == AtomType.USED_TYPE) { /* Case: Used Type State */
            absOrderNumber = (blockNumber * (inputBranching + outputBranching)) + blockNumber + outputBranching
                    + stateNumber;
        } else if (typeOfTheState == AtomType.MODULE) { /* Case: Module/Tool State */
            absOrderNumber = (stateNumber * (inputBranching + outputBranching) * 2) + stateNumber - 1;
        }

        return absOrderNumber;
    }

    /**
     * Function used to calculate the order number of a state within the
     * corresponding automaton (e.g., order number of a MemoryState in a MemoryState
     * Automaton)
     * The information is calculated based on the block number, order number within
     * the block and type of the state.
     * <p>
     * {@link AtomType#MEMORY_TYPE} corresponds to the Memory Type State,<br>
     * {@link AtomType#USED_TYPE} corresponds to the Used Type State,<br>
     * {@link AtomType#MODULE} corresponds to the Module/Tool State.
     *
     * @param blockNumber     Corresponds to the block number within the type
     *                        automaton (not applicable for the module automaton).
     * @param stateNumber     Corresponds to the state number within block.
     * @param inputBranching  Max number of inputs per module.
     * @param outputBranching Max number of outputs per module.
     * @param typeOfTheState  Parameter determining the state type.
     * @return The calculated order number of this type of state (where indexing
     *         starts from 0).
     */
    private static int calculateAutomatonStateNumber(Integer blockNumber, int stateNumber, int inputBranching,
            int outputBranching, AtomType typeOfTheState) {
        int orderNumber = 0;

        if (typeOfTheState == AtomType.MEMORY_TYPE) { /* Case: Memory Type State */
            orderNumber = (blockNumber * outputBranching) + stateNumber + 1;
        } else if (typeOfTheState == AtomType.USED_TYPE) { /* Case: Used Type State */
            orderNumber = (blockNumber * inputBranching) + stateNumber;
        } else if (typeOfTheState == AtomType.MODULE) { /* Case: Module/Tool State */
            orderNumber = stateNumber - 1;
        }

        return orderNumber;
    }
}
