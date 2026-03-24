package nl.uu.cs.ape.solver.solutionStructure.graphviz;

import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;
import static guru.nidi.graphviz.model.Factory.to;

import java.util.List;

import guru.nidi.graphviz.attribute.Attributes;
import guru.nidi.graphviz.attribute.Color;
import guru.nidi.graphviz.attribute.Font;
import guru.nidi.graphviz.attribute.ForAll;
import guru.nidi.graphviz.attribute.Label;
import guru.nidi.graphviz.attribute.LinkAttr;
import guru.nidi.graphviz.attribute.Rank;
import guru.nidi.graphviz.attribute.Rank.RankDir;
import guru.nidi.graphviz.attribute.Rank.RankType;
import guru.nidi.graphviz.attribute.Shape;
import guru.nidi.graphviz.attribute.Style;
import guru.nidi.graphviz.model.Graph;
import guru.nidi.graphviz.model.Node;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import nl.uu.cs.ape.solver.solutionStructure.ModuleNode;
import nl.uu.cs.ape.solver.solutionStructure.SolutionWorkflow;
import nl.uu.cs.ape.solver.solutionStructure.TypeNode;

/**
 * Graph factory for Clingo-derived {@link SolutionWorkflow} instances.
 * <p>
 * Mirrors the methods in {@link SolutionGraphFactory} but adds defensive
 * handling for Clingo-specific edge cases:
 * <ul>
 *   <li>Empty {@code workflowOutputTypeStates} — output cluster is omitted
 *       rather than crashing with a null {@code toolOutputNodes}.</li>
 *   <li>Workflow-output {@link TypeNode} whose {@code createdByModule} is
 *       {@code null} (pass-through from a workflow input) — the incoming edge
 *       is omitted rather than causing a NullPointerException.</li>
 * </ul>
 * This class is the natural extension point for future Clingo-specific
 * visualisation features.
 */
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class ClingoSolutionGraphFactory {

    // -----------------------------------------------------------------------
    // Control-flow graph
    // -----------------------------------------------------------------------

    /**
     * Generate a control-flow graph for a Clingo solution.
     * Identical in behaviour to
     * {@link SolutionGraphFactory#generateControlflowGraph} — the method is safe
     * for Clingo because it only reads {@code moduleNodes} and never touches
     * TypeNode connections.
     */
    public static SolutionGraph generateControlflowGraph(SolutionWorkflow workflow,
            String title, RankDir orientation) {
        Graph workflowGraph = graph(title).directed().graphAttr().with(Rank.dir(orientation));

        String input = "START" + "     ";
        String output = "END" + "     ";
        workflowGraph = workflowGraph.with(node(input).with(Color.BLACK, Style.BOLD));
        String prevNode = input;
        for (ModuleNode currTool : workflow.getModuleNodes()) {
            workflowGraph = currTool.addModuleToGraph(workflowGraph);
            workflowGraph = workflowGraph
                    .with(node(prevNode).link(to(node(currTool.getNodeID()))
                            .with(Label.of("next   "), Color.RED)));
            prevNode = currTool.getNodeID();
        }
        workflowGraph = workflowGraph.with(node(output).with(Color.BLACK, Style.BOLD));
        workflowGraph = workflowGraph
                .with(node(prevNode).link(to(node(output)).with(Label.of("next   "), Color.RED)));

        return new SolutionGraph(workflowGraph);
    }

    // -----------------------------------------------------------------------
    // Data-flow graph
    // -----------------------------------------------------------------------

    /**
     * Generate a data-flow graph for a Clingo solution.
     * Identical in behaviour to
     * {@link SolutionGraphFactory#generateDataFlowGraph} — the method already
     * handles an empty {@code workflowOutputTypeStates} gracefully.
     */
    public static SolutionGraph generateDataFlowGraph(SolutionWorkflow workflow,
            String title, RankDir orientation) {
        Graph workflowGraph = graph(title).directed().graphAttr().with(Rank.dir(orientation));
        List<TypeNode> workflowInputs = workflow.getWorkflowInputTypeStates();
        List<TypeNode> workflowOutputs = workflow.getWorkflowOutputTypeStates();
        List<ModuleNode> moduleNodes = workflow.getModuleNodes();

        String input = "Workflow INPUT" + "     ";
        String output = "Workflow OUTPUT" + "     ";
        boolean inputDefined = false;
        boolean outputDefined = false;
        int index = 0;
        int workflowInNo = 1;
        for (TypeNode workflowInput : workflowInputs) {
            if (!inputDefined) {
                workflowGraph = workflowGraph
                        .with(node(input).with(Color.RED, Shape.RECTANGLE, Style.BOLD));
                inputDefined = true;
            }
            workflowGraph = workflowInput.addTypeToGraph(workflowGraph);
            workflowGraph = workflowGraph.with(node(input).link(to(node(workflowInput.getNodeID()))
                    .with(Label.of((workflowInNo++) + "  "), LinkAttr.weight(index++), Style.DOTTED)));
        }

        for (ModuleNode currTool : moduleNodes) {
            workflowGraph = currTool.addModuleToGraph(workflowGraph);
            int inputNo = 1;
            for (TypeNode toolInput : currTool.getInputTypes()) {
                if (!toolInput.isEmpty()) {
                    workflowGraph = workflowGraph.with(node(toolInput.getNodeID())
                            .link(to(node(currTool.getNodeID()))
                                    .with(Label.of("in " + (inputNo++) + "  "), Color.ORANGE,
                                            LinkAttr.weight(index++))));
                }
            }
            int outputNo = 1;
            for (TypeNode toolOutput : currTool.getOutputTypes()) {
                if (!toolOutput.isEmpty()) {
                    workflowGraph = toolOutput.addTypeToGraph(workflowGraph);
                    workflowGraph = workflowGraph.with(node(currTool.getNodeID())
                            .link(to(node(toolOutput.getNodeID()))
                                    .with(Label.of("out " + (outputNo++) + "  "),
                                            LinkAttr.weight(index++))));
                }
            }
        }
        int workflowOutNo = 1;
        for (TypeNode workflowOutput : workflowOutputs) {
            if (!outputDefined) {
                workflowGraph = workflowGraph
                        .with(node(output).with(Color.RED, Shape.RECTANGLE, Style.BOLD));
                outputDefined = true;
            }
            workflowGraph = workflowOutput.addTypeToGraph(workflowGraph);
            workflowGraph = workflowGraph.with(node(workflowOutput.getNodeID())
                    .link(to(node(output)).with(Label.of((workflowOutNo++) + "  "),
                            LinkAttr.weight(index++), Style.DOTTED)));
        }
        return new SolutionGraph(workflowGraph);
    }

    // -----------------------------------------------------------------------
    // Taverna-style graph
    // -----------------------------------------------------------------------

    /**
     * Generate a Taverna-style design graph for a Clingo solution.
     * <p>
     * Differences from {@link SolutionGraphFactory#generateTavernaDesignGraph}:
     * <ul>
     *   <li>The output cluster frame is only added when at least one workflow
     *       output was resolved (guards the {@code null toolOutputNodes} crash).</li>
     *   <li>When a workflow output's {@code createdByModule} is {@code null}
     *       (pass-through from a workflow input), the creator-to-output edge is
     *       omitted rather than causing a NullPointerException.</li>
     * </ul>
     */
    public static SolutionGraph generateTavernaDesignGraph(SolutionWorkflow workflow,
            String title) {
        Attributes<ForAll> helveticaFont = Font.name("Arial");
        Graph workflowGraph = graph(title).directed()
                .graphAttr().with(Rank.dir(RankDir.TOP_TO_BOTTOM))
                .graphAttr().with(helveticaFont, Font.size(14))
                .nodeAttr().with(helveticaFont, Font.size(16))
                .linkAttr().with(helveticaFont, Font.size(14));

        List<TypeNode> workflowInputs = workflow.getWorkflowInputTypeStates();
        List<TypeNode> workflowOutputs = workflow.getWorkflowOutputTypeStates();
        List<ModuleNode> moduleNodes = workflow.getModuleNodes();

        // --- Inputs cluster ---
        int index = 0;
        boolean inputDefined = false;
        Node toolInputNodes = null;
        for (TypeNode workflowInput : workflowInputs) {
            if (!inputDefined) {
                toolInputNodes = node(workflowInput.getNodeID());
                inputDefined = true;
            } else {
                toolInputNodes = node(workflowInput.getNodeID())
                        .link(to(toolInputNodes).with(Style.INVIS, LinkAttr.weight(index++)));
            }
            workflowGraph = workflowInput.addTavernaStyleTypeToGraph(workflowGraph);
        }
        if (inputDefined) {
            workflowGraph = workflowGraph.with(graph("inputs_frame").cluster()
                    .graphAttr()
                    .with(Style.DASHED, Color.BLACK, Label.html("<b>Workflow Inputs</b>"))
                    .with(graph("inputs").directed().graphAttr()
                            .with(Rank.inSubgraph(RankType.MIN),
                                    Rank.dir(RankDir.LEFT_TO_RIGHT))
                            .with(toolInputNodes)));
        }

        // --- Tool nodes and data edges ---
        for (ModuleNode currTool : moduleNodes) {
            workflowGraph = currTool.addTavernaStyleModuleToGraph(workflowGraph);
            for (TypeNode toolInput : currTool.getInputTypes()) {
                if (!toolInput.isEmpty()) {
                    if (toolInput.getCreatedByModule() == null) {
                        workflowGraph = workflowGraph
                                .with(node(toolInput.getNodeID())
                                        .link(to(node(currTool.getNodeID()))
                                                .with(Label.html(toolInput.getNodeGraphLabels()),
                                                        Color.BLACK, LinkAttr.weight(index++))));
                    } else {
                        workflowGraph = workflowGraph
                                .with(node(toolInput.getCreatedByModule().getNodeID())
                                        .link(to(node(currTool.getNodeID()))
                                                .with(Label.html(toolInput.getNodeGraphLabels()),
                                                        Color.BLACK, LinkAttr.weight(index++))));
                    }
                }
            }
        }

        // --- Outputs cluster (omitted if no outputs were resolved) ---
        boolean outputDefined = false;
        Node toolOutputNodes = null;
        for (TypeNode workflowOutput : workflowOutputs) {
            if (!outputDefined) {
                toolOutputNodes = node(workflowOutput.getNodeID());
                outputDefined = true;
            } else {
                toolOutputNodes = node(workflowOutput.getNodeID())
                        .link(to(toolOutputNodes)
                                .with(Style.INVIS, LinkAttr.weight(100 + index++)));
            }
            workflowGraph = workflowOutput.addTavernaStyleTypeToGraph(workflowGraph);
            // Only draw the creator→output edge when the creator module is known.
            if (workflowOutput.getCreatedByModule() != null) {
                workflowGraph = workflowGraph
                        .with(node(workflowOutput.getCreatedByModule().getNodeID())
                                .link(to(node(workflowOutput.getNodeID()))
                                        .with(Label.html(workflowOutput.getNodeGraphLabels()),
                                                Color.BLACK, LinkAttr.weight(index++))));
            }
        }
        if (outputDefined) {
            workflowGraph = workflowGraph.with(graph("outputs_frame").cluster()
                    .graphAttr()
                    .with(Style.DASHED, Color.BLACK, Label.html("<b>Workflow Outputs</b>"))
                    .with(graph("outputs").directed().graphAttr()
                            .with(Rank.inSubgraph(RankType.MIN),
                                    Rank.dir(RankDir.LEFT_TO_RIGHT))
                            .with(toolOutputNodes)));
        }

        return new SolutionGraph(workflowGraph);
    }
}
