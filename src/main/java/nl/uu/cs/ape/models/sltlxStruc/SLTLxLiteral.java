package nl.uu.cs.ape.models.sltlxStruc;

import nl.uu.cs.ape.automaton.State;
import nl.uu.cs.ape.models.SATAtomMappings;
import nl.uu.cs.ape.models.enums.AtomType;
import nl.uu.cs.ape.models.logic.constructs.PredicateLabel;

/**
 * The {@code SLTLxLiteral} class represents literals (atoms that can be
 * negated) corresponding to the usage of the modules
 * and types in the solution.
 * <p>
 * {@code Literals} can start with a negation.<br>
 * {@code Literals} are compared according to the state in which they are used
 * ({@linkplain #getUsedInStateArgument}),
 * i.e. a literal that represents the n-th state in the workflow, comes before
 * the literal that represents the (n+1)th state.
 *
 * @author Vedran Kasalica
 */
public class SLTLxLiteral implements Comparable<SLTLxLiteral> {

    /**
     * Integer value used to encode the atom into cnf form.
     */
    private Integer mappedAtom;

    /**
     * true if the atom is negated.
     */
    private Boolean negated;

    /**
     * The {@link SLTLxAtom} class represents elements of the workflow, that can be
     * true or not (depending of the truth value of the literal).
     */
    private SLTLxAtom atom;

    /**
     * The {@link SLTLxAtomVar} class represents abstractions over elements of the
     * workflow which contain variables, that can be true or not (depending of the
     * truth value of the literal).
     */
    private SLTLxAtomVar atomVar;

    /**
     * Create a literal from its mapped integer value, as returned by the SAT
     * solver (negative values denote negated atoms).
     *
     * @param mappedLiteral mapped literal
     * @param atomMapping   mapping of atoms to integers
     */
    public SLTLxLiteral(int mappedLiteral, SATAtomMappings atomMapping) {
        super();
        this.negated = mappedLiteral < 0;
        this.mappedAtom = Math.abs(mappedLiteral);
        this.atom = atomMapping.findOriginal(mappedAtom);
        this.atomVar = atomMapping.findOriginalVar(mappedAtom);
    }

    /**
     * Generating an object from a mapped representation of the SLTLxLiteral.
     *
     * @param mappedLiteral Mapped literal.
     * @param atomMapping   Mapping of the atoms.
     */
    public SLTLxLiteral(String mappedLiteral, SATAtomMappings atomMapping) {
        super();
        if (mappedLiteral.startsWith("-")) {
            negated = true;
            mappedAtom = Integer.parseInt(mappedLiteral.substring(1));
        } else {
            negated = false;
            mappedAtom = Integer.parseInt(mappedLiteral);
        }

        this.atom = atomMapping.findOriginal(mappedAtom);
        this.atomVar = atomMapping.findOriginalVar(mappedAtom);

    }

    /**
     * To mapped string string.
     *
     * @return The value of the mapped literal.
     */
    public String toMappedString() {
        if (negated) {
            return "-" + mappedAtom.toString();
        } else {
            return mappedAtom.toString();
        }
    }

    /**
     * To negated mapped string string.
     *
     * @return Negation of the value of the mapped literal.
     */
    public String toNegatedMappedString() {
        if (negated) {
            return mappedAtom.toString();
        } else {
            return "-" + mappedAtom.toString();
        }
    }

    /**
     * To negated mapped int int.
     *
     * @return Negation of the value of the mapped literal.
     */
    public int toNegatedMappedInt() {
        if (negated) {
            return mappedAtom;
        } else {
            return -mappedAtom;
        }
    }

    /**
     * Return the type of the element in the workflow (tool, memory type, etc.)
     *
     * @return The {@link AtomType} that corresponds to the SLTLxLiteral usage or
     *         not usage (in case of a negated literal).
     */
    public AtomType getWorkflowElementType() {
        return atom.getWorkflowElementType();
    }

    /**
     * Return true if the current workflow element is of the given {@link AtomType}
     * type.
     *
     * @param workflowElemType Element type that is current literal is compared to.
     * @return true if the current workflow element corresponds to the given
     *         {@link AtomType}, false otherwise.
     */
    public boolean isWorkflowElementType(AtomType workflowElemType) {
        return atom.getWorkflowElementType() == workflowElemType;
    }

    /**
     * Returns true in case the literal is NEGATED, false otherwise.
     *
     * @return true if SLTLxLiteral is negated.
     */
    public boolean isNegated() {
        return negated;
    }

    /**
     * Returns the state in the automaton.
     *
     * @return String representation of the module/type automaton state.
     */
    public State getUsedInStateArgument() {
        return atom.getUsedInStateArgument();
    }

    /**
     * Return the atom that is part of the literal.
     * 
     * @return SLTLxAtom object that is the base for the literal.
     */
    public SLTLxAtom getAtom() {
        return atom;
    }

    /**
     * Returns the predicate/label used to depict
     * {@code AbstractModule, Module, Type} or {@code State}.
     * Each of those refers to the element that is described by the SLTLxLiteral
     * (e.g. StateInterface(State)).
     *
     * @return StateInterface object that is referred by the literal.
     */
    public PredicateLabel getPredicate() {
        return atom.getPredicate();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = atom.hashCode();
        result = prime * result + ((negated == null) ? 0 : negated.hashCode());
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
        SLTLxLiteral other = (SLTLxLiteral) obj;
        if (negated == null) {
            if (other.negated != null)
                return false;
        } else if (!negated.equals(other.negated))
            return false;
        return atom.equals(other.atom);
    }

    /**
     * Compare the two Literals according to the state they are used in.
     * Returns a negative integer, zero, or a positive integer as this
     * SLTLxLiteral's state comes before than, is equal to, or comes after
     * than the @otherLiteral's state.
     *
     * @param otherLiteral The SLTLxLiteral to be compared
     * @return The value 0 if the argument SLTLxLiteral's state is equal to this
     *         SLTLxLiteral's state;
     *         a value less than 0 if this SLTLxLiteral's state comes before
     *         the @otherLiteral's state;
     *         and a value greater than 0 if this SLTLxLiteral's state comes after
     *         the @otherLiteral's state.
     */
    public int compareTo(SLTLxLiteral otherLiteral) {

        int thisLiteralState = this.getUsedInStateArgument().getAbsoluteStateNumber();
        int otherLiteralState = otherLiteral.getUsedInStateArgument().getAbsoluteStateNumber();
        int diff = 0;
        if ((diff = Integer.compare(thisLiteralState, otherLiteralState)) != 0) {
            return diff;
        } else {
            return this.getPredicate().compareTo(otherLiteral.getPredicate());
        }
    }

    /**
     * Returns the Original (human readable) value of the literal. The atom of the
     * SLTLxLiteral is transformed using the {@link SLTLxAtom#toString} function.
     *
     * @return The value of the original literal
     */
    public String toString() {
        if (atom != null) {
            if (negated) {
                return "-" + atom.toString();
            } else {
                return atom.toString();
            }
        } else {
            if (negated) {
                return "-" + atomVar.toString();
            } else {
                return atomVar.toString();
            }
        }
    }

}
