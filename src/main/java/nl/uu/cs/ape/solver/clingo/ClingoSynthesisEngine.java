package nl.uu.cs.ape.solver.clingo;

import nl.uu.cs.ape.automaton.ModuleAutomaton;
import nl.uu.cs.ape.automaton.TypeAutomaton;
import nl.uu.cs.ape.configuration.APERunConfig;
import nl.uu.cs.ape.domain.APEDomainSetup;
import nl.uu.cs.ape.models.SATAtomMappings;
import nl.uu.cs.ape.models.AuxTypePredicate;
import nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate;
import nl.uu.cs.ape.solver.SynthesisEngine;
import nl.uu.cs.ape.solver.solutionStructure.SolutionWorkflow;
import nl.uu.cs.ape.solver.solutionStructure.SolutionsList;
import nl.uu.cs.ape.utils.APEResources;

import lombok.extern.slf4j.Slf4j;
import org.potassco.clingo.control.Control;
import org.potassco.clingo.control.ProgramPart;
import org.potassco.clingo.solving.Model;
import org.potassco.clingo.solving.SolveHandle;
import org.potassco.clingo.solving.SolveResult;
import org.potassco.clingo.symbol.Function;
import org.potassco.clingo.symbol.Number;
import org.potassco.clingo.symbol.Symbol;
import org.potassco.clingo.backend.ExternalType;
import org.potassco.clingo.solving.SolveMode;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

/**
 * The {@code ClingoSynthesisEngine} class provides an integration with the Clingo ASP solver
 * using the jclingo Java bindings.
 */
@Slf4j
public class ClingoSynthesisEngine implements SynthesisEngine {

    /** Accumulated encoding time (LP loading + fact gen + base grounding). */
    private static final AtomicLong totalEncodingTime = new AtomicLong(0);

    /** Accumulated incremental grounding time (step/check per length). */
    private static final AtomicLong totalGroundingTime = new AtomicLong(0);

    /** Accumulated pure solving time (solve() + model iteration). */
    private static final AtomicLong totalSolvingTime = new AtomicLong(0);

    private final APEDomainSetup domainSetup;
    private final SolutionsList allSolutions;
    private final APERunConfig runConfig;
    private final int solutionSize;
    private final SATAtomMappings mappings;
    private Control control;
    private ModuleAutomaton moduleAutomaton;
    private TypeAutomaton typeAutomaton;

    /** Facts string cached from encoding phase for debug output. */
    private String cachedFacts;

    /**
     * Instantiates a new Clingo synthesis engine.
     *
     * @param domainSetup  The domain setup
     * @param allSolutions The solutions list
     * @param runConfig    The run configuration
     * @param solutionSize The current solution length
     */
    public ClingoSynthesisEngine(APEDomainSetup domainSetup, SolutionsList allSolutions,
                                 APERunConfig runConfig, int solutionSize) {
        this.domainSetup = domainSetup;
        this.allSolutions = allSolutions;
        this.runConfig = runConfig;
        this.solutionSize = solutionSize;
        this.mappings = allSolutions.getMappings();

        int maxNoToolInputs = Math.max(domainSetup.getMaxNoToolInputs(), runConfig.getProgramOutputs().size());
        int maxNoToolOutputs = Math.max(domainSetup.getMaxNoToolOutputs(), runConfig.getProgramInputs().size());
        this.moduleAutomaton = new ModuleAutomaton(solutionSize, maxNoToolInputs, maxNoToolOutputs);
        this.typeAutomaton = new TypeAutomaton(solutionSize, maxNoToolInputs, maxNoToolOutputs);
    }

    // -----------------------------------------------------------------------
    // Timing
    // -----------------------------------------------------------------------

    static void addEncodingTime(long ms)  { totalEncodingTime.addAndGet(ms); }
    static void addGroundingTime(long ms) { totalGroundingTime.addAndGet(ms); }
    static void addSolvingTime(long ms)   { totalSolvingTime.addAndGet(ms); }

    public static long getTotalEncodingTime()  { return totalEncodingTime.get(); }
    public static long getTotalGroundingTime() { return totalGroundingTime.get(); }
    public static long getTotalSolvingTime()   { return totalSolvingTime.get(); }

    /** Reset static counters at the start of a new APE run. */
    public static void resetTimers() {
        totalEncodingTime.set(0);
        totalGroundingTime.set(0);
        totalSolvingTime.set(0);
    }

    // -----------------------------------------------------------------------
    // Encoding
    // -----------------------------------------------------------------------

    @Override
    public boolean synthesisEncoding() throws IOException {
        long t0 = System.currentTimeMillis();
        try {
            // Unlimited models — the inner loop breaks when enough are collected.
            this.control = new Control("--models=0");

            // Load base encodings
            Path customEncodingsPath = runConfig.getClingoEncodingsPath();
            if (customEncodingsPath != null && Files.isDirectory(customEncodingsPath)) {
                try (Stream<Path> paths = Files.list(customEncodingsPath)) {
                    paths.filter(Files::isRegularFile)
                         .filter(p -> p.toString().endsWith(".lp"))
                         .forEach(p -> this.control.load(p));
                }
            } else {
                String[] files = new String[]{
                        "base.lp", "step.lp", "check.lp", "ape_extract.lp",
                        "tool_inclusion.lp", "tool_dependency.lp", "properties.lp",
                        "artifact_state_constraints.lp", "temporal_constraint.lp",
                        "input_usage.lp", "output_usage.lp", "recipe_constraints.lp",
                        "workflode_constraints.lp", "energy_cost.lp"
                };
                for (String file : files) {
                    String content = APEResources.getTextResource("clingo/" + file);
                    if (content != null) {
                        this.control.add("base", content);
                    }
                }
            }

            this.cachedFacts = generateASPFacts();
            this.control.add("base", this.cachedFacts);

            // Ground base once — shared across all length iterations.
            this.control.ground(new ProgramPart("base", new Symbol[0]));

            addEncodingTime(System.currentTimeMillis() - t0);
            return true;
        } catch (Exception e) {
            log.error("Error during Clingo encoding: {}", e.getMessage());
            addEncodingTime(System.currentTimeMillis() - t0);
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Fact generation
    // -----------------------------------------------------------------------

    private String generateASPFacts() {
        StringBuilder sb = new StringBuilder();

        // --- Exact-horizon mode: goal must be reached for the first time at step t ---
        sb.append("exact_horizon_mode.\n");

        // --- Config-driven activation facts ---
        nl.uu.cs.ape.models.enums.ConfigEnum useWfInput = runConfig.getUseWorkflowInput();
        if (useWfInput == nl.uu.cs.ape.models.enums.ConfigEnum.ALL) {
            sb.append("enable_all_inputs_used.\n");
        } else if (useWfInput == nl.uu.cs.ape.models.enums.ConfigEnum.ONE) {
            sb.append("enable_inputs_used_once.\n");
        }

        // Output usage: mirror SAT's useAllGeneratedData constraint
        nl.uu.cs.ape.models.enums.ConfigEnum useGenData = runConfig.getUseAllGeneratedData();
        if (useGenData == nl.uu.cs.ape.models.enums.ConfigEnum.ALL) {
            sb.append("enable_all_outputs_consumed.\n");
        } else if (useGenData == nl.uu.cs.ape.models.enums.ConfigEnum.ONE) {
            sb.append("enable_primary_output_consumed.\n");
        }

        // Tool repetition: emit multi_run for every tool when repetition is allowed
        if (runConfig.getAllowToolSeqRepeat()) {
            sb.append("multi_run(Tool) :- tool(Tool).\n");
        }

        // --- Taxonomies and Types ---
        // taxonomy(Domain, Dimension, (GroupRoot, Child, Parent))
        // Only parent-child edges; no self-references (self-refs break is_terminal/compatible).
        for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate type : domainSetup.getAllTypes().getTypes()) {
            nl.uu.cs.ape.models.enums.NodeType nt = type.getNodeType();
            if (nt == nl.uu.cs.ape.models.enums.NodeType.EMPTY
                    || nt == nl.uu.cs.ape.models.enums.NodeType.EMPTY_LABEL
                    || "APE_label".equals(type.getRootNodeID())) {
                continue;
            }
            String dimension = type.getRootNodeID();
            if (type.getSubPredicates() != null) {
                for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate subType : type.getSubPredicates()) {
                    if (subType.getNodeType() == nl.uu.cs.ape.models.enums.NodeType.EMPTY
                            || subType.getNodeType() == nl.uu.cs.ape.models.enums.NodeType.EMPTY_LABEL
                            || "APE_label".equals(subType.getRootNodeID())) {
                        continue;
                    }
                    sb.append(String.format("taxonomy(\"ape\", \"%s\", (\"%s\", \"%s\", \"%s\")).\n",
                            dimension, dimension, subType.getPredicateID(), type.getPredicateID()));
                }
            }
        }

        // --- Tools and tool taxonomy ---
        for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate module : domainSetup.getAllModules().getModules()) {
            nl.uu.cs.ape.models.enums.NodeType nt = module.getNodeType();

            if (nt == nl.uu.cs.ape.models.enums.NodeType.LEAF
                    || nt == nl.uu.cs.ape.models.enums.NodeType.ARTIFICIAL_LEAF) {
                sb.append(String.format("tool(\"%s\").\n", module.getPredicateID()));
            } else if (nt == nl.uu.cs.ape.models.enums.NodeType.ABSTRACT) {
                sb.append(String.format("tool_abstract_class(\"%s\").\n", module.getPredicateID()));
            }

            if (module.getSubPredicates() != null) {
                for (nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate subModule : module.getSubPredicates()) {
                    sb.append(String.format("tool_taxonomy(\"ape\", (\"%s\", \"%s\")).\n",
                            subModule.getPredicateID(), module.getPredicateID()));
                }
            }

            if (module instanceof nl.uu.cs.ape.models.Module) {
                nl.uu.cs.ape.models.Module m = (nl.uu.cs.ape.models.Module) module;

                if (!m.getModuleInput().isEmpty()) {
                    String variantId = module.getPredicateID() + "_v0";
                    sb.append(String.format("tool_input(\"%s\", \"%s\").\n", module.getPredicateID(), variantId));
                    for (int i = 0; i < m.getModuleInput().size(); i++) {
                        nl.uu.cs.ape.models.Type t = m.getModuleInput().get(i);
                        String portId = variantId + "_p" + i;
                        sb.append(String.format("input_port(\"%s\", \"%s\").\n", variantId, portId));
                        printDimensions(sb, "dimension", portId, t);
                    }
                }

                for (int i = 0; i < m.getModuleOutput().size(); i++) {
                    nl.uu.cs.ape.models.Type t = m.getModuleOutput().get(i);
                    String outId = module.getPredicateID() + "_out_" + i;
                    String portId = outId + "_port_0";
                    sb.append(String.format("tool_output(\"%s\", \"%s\").\n", module.getPredicateID(), outId));
                    sb.append(String.format("output_port(\"%s\", \"%s\").\n", outId, portId));
                    printDimensions(sb, "dimension", portId, t);
                }
            }
        }

        // --- Workflow Inputs ---
        for (int i = 0; i < runConfig.getProgramInputs().size(); i++) {
            nl.uu.cs.ape.models.Type t = runConfig.getProgramInputs().get(i);
            String wfInId = "wf_input_" + i;
            sb.append(String.format("holds(0, avail(\"%s\")).\n", wfInId));
            printDimensions(sb, "holds(0, dim", wfInId, t);
            // Emit ape_holds_dim base facts for workflow inputs: ape_holds_dim(WF, V, Cat).
            // Tool-output annotations are derived by the step rule in step.lp.
            printAPEHoldsDim(sb, wfInId, t);
        }

        // --- Workflow Outputs ---
        for (int i = 0; i < runConfig.getProgramOutputs().size(); i++) {
            nl.uu.cs.ape.models.Type t = runConfig.getProgramOutputs().get(i);
            printDimensions(sb, "goal_output", String.valueOf(i), t);
        }

        // --- Show directives are handled by show.lp ---
        // tool_at_time/2, ape_bind/3, ape_holds_dim/3, ape_goal_out/2 are shown
        // via predicate-based #show in show.lp. Suppress the "show all atoms"
        // default so only those predicates appear in the model output.
        sb.append("#show.\n");

        return sb.toString();
    }

    private void printDimensions(StringBuilder sb, String predicate, String id, nl.uu.cs.ape.models.Type type) {
        if (type instanceof AuxTypePredicate) {
            AuxTypePredicate auxType = (AuxTypePredicate) type;
            for (TaxonomyPredicate p : auxType.getGeneralizedPredicates()) {
                printDimensions(sb, predicate, id, (nl.uu.cs.ape.models.Type) p);
            }
        } else {
            nl.uu.cs.ape.models.enums.NodeType nt = type.getNodeType();
            if (nt == nl.uu.cs.ape.models.enums.NodeType.EMPTY_LABEL
                    || nt == nl.uu.cs.ape.models.enums.NodeType.EMPTY
                    || "APE_label".equals(type.getRootNodeID())) {
                return;
            }
            if (predicate.contains("holds")) {
                sb.append(String.format("%s(\"%s\", \"%s\", \"%s\")).\n",
                        predicate, id, type.getPredicateID(), type.getRootNodeID()));
            } else if (predicate.equals("goal_output")) {
                sb.append(String.format("%s(%s, \"%s\", \"%s\").\n",
                        predicate, id, type.getPredicateID(), type.getRootNodeID()));
            } else {
                sb.append(String.format("%s(\"%s\", (\"%s\", \"%s\")).\n",
                        predicate, id, type.getPredicateID(), type.getRootNodeID()));
            }
        }
    }

    /** Emit {@code ape_holds_dim("id", "valueID", "catID").} facts for workflow-input type annotations. */
    private void printAPEHoldsDim(StringBuilder sb, String wfId, nl.uu.cs.ape.models.Type type) {
        if (type instanceof AuxTypePredicate) {
            for (TaxonomyPredicate p : ((AuxTypePredicate) type).getGeneralizedPredicates()) {
                printAPEHoldsDim(sb, wfId, (nl.uu.cs.ape.models.Type) p);
            }
        } else {
            nl.uu.cs.ape.models.enums.NodeType nt = type.getNodeType();
            if (nt == nl.uu.cs.ape.models.enums.NodeType.EMPTY_LABEL
                    || nt == nl.uu.cs.ape.models.enums.NodeType.EMPTY
                    || "APE_label".equals(type.getRootNodeID())) {
                return;
            }
            sb.append(String.format("ape_holds_dim(\"%s\", \"%s\", \"%s\").\n",
                    wfId, type.getPredicateID(), type.getRootNodeID()));
        }
    }

    // -----------------------------------------------------------------------
    // Execution
    // -----------------------------------------------------------------------

    /** Per-length benchmark snapshot collected when benchmark mode is active. */
    private record LengthMetrics(int length, long groundingMs, long solvingMs,
                                 int solutionsAtLength, int cumulativeSolutions,
                                 long clingoMemoryBytes) {}

    /**
     * Multi-shot incremental execution: grounds step(t) and check(t) one length at a time,
     * reusing the single Control created in {@link #synthesisEncoding()}. Solutions are added
     * to {@code allSolutions} directly; the returned list is always empty.
     */
    @Override
    public List<SolutionWorkflow> synthesisExecution() throws FileNotFoundException, IOException {
        if (this.control == null) {
            return java.util.Collections.emptyList();
        }

        int minLength = runConfig.getSolutionLength().getMin();
        // solutionSize holds the max length (set by APE.java for the multi-shot engine).
        int maxLength = this.solutionSize;

        boolean benchmarkMode = runConfig.getBenchmarkMode();
        List<LengthMetrics> benchmarkData = benchmarkMode ? new ArrayList<>() : null;

        try {
            for (int t = minLength; t <= maxLength; t++) {
                if (allSolutions.getNumberOfSolutions() >= allSolutions.getMaxNumberOfSolutions()) break;

                nl.uu.cs.ape.utils.APEUtils.printHeader(t, "Workflow discovery - length");

                // Incrementally ground only the new timestep and horizon check.
                long tGround = System.currentTimeMillis();
                this.control.ground(new ProgramPart("step", new Number(t)));
                this.control.ground(new ProgramPart("check", new Number(t)));
                this.control.assignExternal(
                        new Symbol[]{new Function("query", new Number(t))}, ExternalType.TRUE);
                long groundingMs = System.currentTimeMillis() - tGround;
                addGroundingTime(groundingMs);

                int remaining = allSolutions.getMaxNumberOfSolutions() - allSolutions.getNumberOfSolutions();
                List<SolutionWorkflow> lengthSolutions = new ArrayList<>();
                SolveResult solveResult = null;

                long tSolve = System.currentTimeMillis();
                SolveHandle handle = this.control.solve(SolveMode.YIELD);
                while (handle.hasNext() && lengthSolutions.size() < remaining) {
                    Model model = handle.next();
                    lengthSolutions.add(new SolutionWorkflow(model, this));
                }
                solveResult = handle.getSolveResult();
                handle.close();
                long solvingMs = System.currentTimeMillis() - tSolve;
                addSolvingTime(solvingMs);

                // Deactivate this horizon so check(t) does not interfere with check(t+1).
                this.control.assignExternal(
                        new Symbol[]{new Function("query", new Number(t))}, ExternalType.FALSE);

                log.info("Clingo t={}: {} — {} solution(s) at this length",
                        t, solveResult, lengthSolutions.size());

                allSolutions.addSolutions(lengthSolutions);
                allSolutions.addNoSolutionsForLength(t, allSolutions.getNumberOfSolutions());

                if (runConfig.getClingoDebugMode()) {
                    writeDebugFiles(lengthSolutions, solveResult, t);
                }

                if (benchmarkMode) {
                    long clingoMem = 0;
                    try {
                        clingoMem = (long) this.control.getStatistics().get("summary").get("memory").get();
                    } catch (Exception ignored) {}
                    benchmarkData.add(new LengthMetrics(t, groundingMs, solvingMs,
                            lengthSolutions.size(), allSolutions.getNumberOfSolutions(), clingoMem));
                }
            }

            if (benchmarkMode) {
                long jvmUsed = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
                writeBenchmarkCsv(benchmarkData, jvmUsed);
            }
        } catch (Exception e) {
            log.error("Error during Clingo multi-shot solving: {}", e.getMessage());
        } finally {
            this.control.close();
        }

        return java.util.Collections.emptyList();
    }

    // -----------------------------------------------------------------------
    // Debug output
    // -----------------------------------------------------------------------

    private void writeDebugFiles(List<SolutionWorkflow> solutions, SolveResult solveResult, int t) {
        try {
            Path debugDir = runConfig.getSolutionDirPath().resolve("clingo_debug");
            Files.createDirectories(debugDir);

            // Facts file — written once (only at first length, since facts don't change)
            Path factsFile = debugDir.resolve("facts_t" + t + ".lp");
            Files.writeString(factsFile, cachedFacts != null ? cachedFacts : "", StandardCharsets.UTF_8);

            // Results file
            Path resultFile = debugDir.resolve("result_t" + t + ".txt");
            try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(resultFile, StandardCharsets.UTF_8))) {
                pw.println("=== Clingo debug output: length t=" + t + " ===");
                pw.println("Solve result : " + solveResult);
                pw.println("Solutions    : " + solutions.size());
                pw.println();
                for (int i = 0; i < solutions.size(); i++) {
                    SolutionWorkflow wf = solutions.get(i);
                    pw.println("Solution " + (i + 1) + ": " + wf.getNativeSolution().getRelevantToolsInSolution());
                }
            }
        } catch (IOException e) {
            log.warn("Could not write Clingo debug files for t={}: {}", t, e.getMessage());
        }
    }

    private static final String BENCHMARK_HEADER =
            "length,encoding_ms,grounding_ms,solving_ms,solutions_at_length,"
            + "cumulative_solutions,clingo_peak_memory_bytes,jvm_used_bytes";

    private void writeBenchmarkCsv(List<LengthMetrics> metrics, long jvmUsedBytes) {
        Path csvFile = runConfig.getSolutionDirPath().resolve("benchmark.csv");
        boolean fileExists = Files.exists(csvFile);
        try (PrintWriter pw = new PrintWriter(
                Files.newBufferedWriter(csvFile,
                        StandardCharsets.UTF_8,
                        java.nio.file.StandardOpenOption.CREATE,
                        java.nio.file.StandardOpenOption.APPEND))) {
            // Write header only when creating the file for the first time
            if (!fileExists) {
                pw.println(BENCHMARK_HEADER);
            }
            for (LengthMetrics m : metrics) {
                pw.printf("%d,,%d,%d,%d,%d,%d,%n",
                        m.length(), m.groundingMs(), m.solvingMs(),
                        m.solutionsAtLength(), m.cumulativeSolutions(), m.clingoMemoryBytes());
            }
            // Total row — encoding only happens once before the length loop
            long totalGrounding = metrics.stream().mapToLong(LengthMetrics::groundingMs).sum();
            long totalSolving   = metrics.stream().mapToLong(LengthMetrics::solvingMs).sum();
            int  totalSolutions = metrics.isEmpty() ? 0 : metrics.get(metrics.size() - 1).cumulativeSolutions();
            long peakClingoMem  = metrics.stream().mapToLong(LengthMetrics::clingoMemoryBytes).max().orElse(0);
            pw.printf("total,%d,%d,%d,,%d,%d,%d%n",
                    getTotalEncodingTime(), totalGrounding, totalSolving,
                    totalSolutions, peakClingoMem, jvmUsedBytes);
            log.info("Benchmark {} to {}", fileExists ? "appended" : "written", csvFile);
        } catch (IOException e) {
            log.warn("Could not write benchmark CSV: {}", e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // SynthesisEngine interface
    // -----------------------------------------------------------------------

    @Override
    public TypeAutomaton getTypeAutomaton() { return this.typeAutomaton; }

    public ModuleAutomaton getModuleAutomaton() { return this.moduleAutomaton; }

    @Override
    public APEDomainSetup getDomainSetup() { return this.domainSetup; }

    @Override
    public SATAtomMappings getMappings() { return this.mappings; }

    @Override
    public int getSolutionSize() { return this.solutionSize; }

    @Override
    public void deleteTempFiles() throws IOException {
        // No temporary files — everything is in memory or written on demand.
    }
}
