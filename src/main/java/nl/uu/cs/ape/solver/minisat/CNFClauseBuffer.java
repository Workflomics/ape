package nl.uu.cs.ape.solver.minisat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.sat4j.core.VecInt;
import org.sat4j.specs.ContradictionException;
import org.sat4j.specs.ISolver;

/**
 * In-memory collection of CNF clauses, stored as {@code int[]} (DIMACS
 * literals without the terminating {@code 0}).
 * <p>
 * Replaces the former pipeline that wrote the encoding to a temporary file,
 * re-read it to count clauses, copied it to prepend the DIMACS header and then
 * parsed it again with the SAT4J DIMACS reader.
 */
public class CNFClauseBuffer {

    private final List<int[]> clauses = new ArrayList<>();
    private int maxVar = 0;
    /** Scratch space for parsing, grown on demand. */
    private int[] scratch = new int[64];

    /**
     * Add clauses given in DIMACS text form. Each string may contain one or more
     * clauses, each terminated by {@code 0}.
     *
     * @param dimacsClauses clauses in DIMACS text form
     */
    public void addAll(Collection<String> dimacsClauses) {
        for (String clause : dimacsClauses) {
            add(clause);
        }
    }

    /**
     * Add clauses given in DIMACS text form. The string may contain any number of
     * clauses, each terminated by {@code 0}. Lines starting with {@code c} or
     * {@code p} are ignored.
     *
     * @param dimacs clauses in DIMACS text form
     */
    public void add(CharSequence dimacs) {
        int len = dimacs.length();
        int size = 0;
        int value = 0;
        boolean inNumber = false;
        boolean negative = false;
        int i = 0;
        while (i < len) {
            char c = dimacs.charAt(i);
            if (!inNumber && (c == 'c' || c == 'p')) {
                /* Skip comment or header line. */
                while (i < len && dimacs.charAt(i) != '\n') {
                    i++;
                }
            } else if (c >= '0' && c <= '9') {
                value = value * 10 + (c - '0');
                inNumber = true;
            } else if (c == '-' && !inNumber) {
                negative = true;
            } else if (inNumber) {
                /* End of a number. */
                if (value == 0) {
                    addClause(Arrays.copyOf(scratch, size));
                    size = 0;
                } else {
                    if (size == scratch.length) {
                        scratch = Arrays.copyOf(scratch, size * 2);
                    }
                    scratch[size++] = negative ? -value : value;
                }
                value = 0;
                inNumber = false;
                negative = false;
            }
            i++;
        }
        if (inNumber && value == 0) {
            addClause(Arrays.copyOf(scratch, size));
        } else if (inNumber || size > 0) {
            throw new IllegalArgumentException("Unterminated CNF clause in encoding.");
        }
    }

    /**
     * Add a single clause.
     *
     * @param literals DIMACS literals, without the terminating {@code 0}
     */
    public void addClause(int[] literals) {
        for (int literal : literals) {
            maxVar = Math.max(maxVar, Math.abs(literal));
        }
        clauses.add(literals);
    }

    /** @return number of clauses in the buffer */
    public int size() {
        return clauses.size();
    }

    /** @return highest variable number used in any clause */
    public int getMaxVar() {
        return maxVar;
    }

    /**
     * Load all the clauses into the solver.
     *
     * @param solver    SAT solver
     * @param variables number of variables to declare (the highest variable used
     *                  is declared if it is larger)
     * @throws ContradictionException if the clauses are trivially unsatisfiable
     */
    public void loadInto(ISolver solver, int variables) throws ContradictionException {
        solver.newVar(Math.max(variables, maxVar));
        solver.setExpectedNumberOfClauses(clauses.size());
        for (int[] clause : clauses) {
            solver.addClause(new VecInt(clause));
        }
    }

    /**
     * Write the clauses as a DIMACS file, including the {@code p cnf} header.
     *
     * @param file      target file
     * @param variables number of variables in the header (the highest variable
     *                  used is written if it is larger)
     * @throws IOException if the file cannot be written
     */
    public void writeDimacs(File file, int variables) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.US_ASCII), 1 << 16)) {
            writer.write("p cnf " + Math.max(variables, maxVar) + " " + clauses.size() + "\n");
            for (int[] clause : clauses) {
                for (int literal : clause) {
                    writer.write(Integer.toString(literal));
                    writer.write(' ');
                }
                writer.write("0\n");
            }
        }
    }

    /** Release the stored clauses. */
    public void clear() {
        clauses.clear();
    }
}
