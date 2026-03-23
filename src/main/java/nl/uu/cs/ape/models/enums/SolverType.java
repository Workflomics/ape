package nl.uu.cs.ape.models.enums;

/**
 * Defines the types of solvers supported by the framework.
 * <p>
 * 
 * @author Vedran Kasalica
 */
public enum SolverType {

    /**
     * SAT solver will be used.
     */
    SAT,
    
    /**
     * Clingo ASP solver will be used.
     */
    CLINGO
}
