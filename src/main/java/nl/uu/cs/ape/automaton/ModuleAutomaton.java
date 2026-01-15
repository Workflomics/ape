package nl.uu.cs.ape.automaton;

import java.util.ArrayList;
import java.util.List;

import nl.uu.cs.ape.models.enums.AtomType;

/**
 * The {@code ModuleAutomaton} class is used to represent the collection of
 * operation states in the workflow.
 * Module automaton is represented as an array of {@link State}s where each
 * state is an operation performed within the workflow.
 * <p>
 * Labeling of the automaton is provided in
 * <a href=
 * "https://ape-framework.readthedocs.io/en/latest/docs/developers/developers.html">the ReadTheDocs</a>.
 *
 * @author Vedran Kasalica
 */
public class ModuleAutomaton implements Automaton {

    private List<State> moduleStates;

    /**
     * Generate the Module State automatons based on the defined length.
     *
     * @param automataBound   Length of the automaton.
     * @param inputBranching  Input branching factor (max number of inputs for
     *                        modules).
     * @param outputBranching Output branching factor (max number of inputs for
     *                        modules).
     */
    public ModuleAutomaton(int automataBound, int inputBranching, int outputBranching) {
        moduleStates = new ArrayList<>();
        automataBound = Math.max(automataBound, 1);

        for (int i = 1; i <= automataBound; i++) {
            State tmpState = new State(AtomType.MODULE, null, i, inputBranching, outputBranching);
            moduleStates.add(tmpState);
        }
    }

    /**
     * Add {@link State} to the Module automaton.
     *
     * @param state Module state to be added.
     */
    public void addState(State state) {
        moduleStates.add(state);
    }

    /**
     * Return the size of the Module automaton.
     *
     * @return Current amount of module states.
     */
    public int size() {
        return moduleStates.size();
    }

    /**
     * Return {@code i}-th Module state from the automaton (where 0 is the first
     * index).
     *
     * @param i Ordering number of the state to be returned.
     * @return Module State.
     */
    public State get(int i) {
        return moduleStates.get(i);
    }

    /**
     * Return {@code i}-th Module state from the automaton (where 0 is the first
     * index).
     * It will not throw out of bound error, but return {@code null} instead.
     *
     * @param i Ordering number of the state to be returned.
     * @return Module State, or {@code null} if the index is out of bounds.
     */
    public State getSafe(int i) {
        return ((i < moduleStates.size()) ? moduleStates.get(i) : null);
    }

    /*
     * (non-Javadoc)
     * 
     * @see nl.uu.cs.ape.automaton.Automaton#getAllStates()
     */
    @Override
    public List<State> getAllStates() {
        return moduleStates;
    }

    /**
     * Gets state object which corresponds to the type and order number (w.r.t. the
     * type) of the state.
     * 
     * @param typeDependantStateNumber order number of the state within the type
     * @return State no {@code typeDependantStateNumber} of the type
     *         {@code usedTypeState}
     */
    public State getState(int typeDependantStateNumber) {
        return get(typeDependantStateNumber);
    }
}
