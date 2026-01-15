package nl.uu.cs.ape.solver.solutionStructure.graphviz;

import static guru.nidi.graphviz.model.Factory.graph;
import static guru.nidi.graphviz.model.Factory.node;
import static guru.nidi.graphviz.model.Factory.to;

import java.util.Collections;
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

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class SolutionGraphFactory {

    /**
     * Generate a graph that represent the control-flow solution.
     *
     * @param workflow    The {@link SolutionWorkflow} object that represents the
     *                    solution.
     * @param title       The title of the graph.
     * @param orientation The orientation of the graph.
     * 
     * @return The {@link Graph} object that represents the solution workflow.
     */
    public static SolutionGraph generateControlflowGraph(SolutionWorkflow workflow, String title,
            RankDir orientation) {
        Graph workflowGraph = graph(title).directed().graphAttr().with(Rank.dir(orientation));

        String input = "START" + "     ";
        String output = "END" + "     ";
        workflowGraph = workflowGraph.with(node(input).with(Color.BLACK, Style.BOLD));
        String prevNode = input;
        for (ModuleNode currTool : workflow.getModuleNodes()) {
            workflowGraph = currTool.addModuleToGraph(workflowGraph);
            workflowGraph = workflowGraph
                    .with(node(prevNode).link(to(node(currTool.getNodeID())).with(Label.of("next   "), Color.RED)));
            prevNode = currTool.getNodeID();
        }
        workflowGraph = workflowGraph.with(node(output).with(Color.BLACK, Style.BOLD));
        workflowGraph = workflowGraph.with(node(prevNode).link(to(node(output)).with(Label.of("next   "), Color.RED)));

        return new SolutionGraph(workflowGraph);
    }

    /**
     * Generate a graph that represent the control-flow solution.
     *
     * @param workflow    The {@link SolutionWorkflow} object that represents the
     *                    solution.
     * @param title       The title of the graph.
     * @param orientation The orientation of the graph.
     * 
     * @return The {@link Graph} object that represents the solution workflow.
     */
    public static SolutionGraph generateDataFlowGraph(SolutionWorkflow workflow, String title, RankDir orientation) {
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
                workflowGraph = workflowGraph.with(node(input).with(Color.RED, Shape.RECTANGLE, Style.BOLD));
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
                    workflowGraph = workflowGraph.with(node(toolInput.getNodeID()).link(to(node(currTool.getNodeID()))
                            .with(Label.of("in " + (inputNo++) + "  "), Color.ORANGE, LinkAttr.weight(index++))));
                }
            }
            int outputNo = 1;
            for (TypeNode toolOutput : currTool.getOutputTypes()) {
                if (!toolOutput.isEmpty()) {
                    workflowGraph = toolOutput.addTypeToGraph(workflowGraph);
                    workflowGraph = workflowGraph.with(node(currTool.getNodeID()).link(to(node(toolOutput.getNodeID()))
                            .with(Label.of("out " + (outputNo++) + "  "), LinkAttr.weight(index++))));
                }
            }
        }
        int workflowOutNo = 1;
        for (TypeNode workflowOutput : workflowOutputs) {
            if (!outputDefined) {
                workflowGraph = workflowGraph.with(node(output).with(Color.RED, Shape.RECTANGLE, Style.BOLD));
                outputDefined = true;
            }
            workflowGraph = workflowOutput.addTypeToGraph(workflowGraph);
            workflowGraph = workflowGraph.with(node(workflowOutput.getNodeID()).link(
                    to(node(output)).with(Label.of((workflowOutNo++) + "  "), LinkAttr.weight(index++), Style.DOTTED)));
        }
        return new SolutionGraph(workflowGraph);
    }

    /**
     * Generate a graphical representation of the workflow solution with styling
     * based on the Apache Taverna workflow management system
     * 
     * @param workflow The {@link SolutionWorkflow} object that represents the
     *                 solution.
     * @param title    The title of the graph.
     * @return The {@link SolutionGraph} object that represents the solution
     *         workflow.
     */
    public static SolutionGraph generateTavernaDesignGraph(SolutionWorkflow workflow, String title) {
        Attributes<ForAll> helveticaFont = Font.name("Arial");
        Graph workflowGraph = graph(title).directed().graphAttr()
                .with(Rank.dir(RankDir.TOP_TO_BOTTOM))
                .graphAttr().with(helveticaFont, Font.size(14))
                .nodeAttr().with(helveticaFont, Font.size(16))
                .linkAttr().with(helveticaFont, Font.size(14));
        List<TypeNode> workflowInputs = workflow.getWorkflowInputTypeStates();
        List<TypeNode> workflowOutputs = workflow.getWorkflowOutputTypeStates();
        List<ModuleNode> moduleNodes = workflow.getModuleNodes();

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

        workflowGraph = workflowGraph.with(graph("inputs_frame").cluster()
                .graphAttr()
                .with(Style.DASHED, Color.BLACK, Label.html("<b>Workflow Inputs</b>"))
                .with(graph("inputs").directed().graphAttr()
                        .with(Rank.inSubgraph(RankType.MIN), Rank.dir(RankDir.LEFT_TO_RIGHT)).with(toolInputNodes)));

        for (ModuleNode currTool : moduleNodes) {
            workflowGraph = currTool.addTavernaStyleModuleToGraph(workflowGraph);
            for (TypeNode toolInput : currTool.getInputTypes()) {
                if (!toolInput.isEmpty()) {
                    if (toolInput.getCreatedByModule() == null) {
                        workflowGraph = workflowGraph
                                .with(node(toolInput.getNodeID()).link(to(node(currTool.getNodeID()))
                                        .with(Label.html(toolInput.getNodeGraphLabels()), Color.BLACK,
                                                LinkAttr.weight(index++))));
                    } else {
                        workflowGraph = workflowGraph
                                .with(node(toolInput.getCreatedByModule().getNodeID())
                                        .link(to(node(currTool.getNodeID()))
                                                .with(Label.html(toolInput.getNodeGraphLabels()), Color.BLACK,
                                                        LinkAttr.weight(index++))));
                    }
                }
            }
        }
        boolean outputDefined = false;
        Node toolOutputNodes = null;
        for (TypeNode workflowOutput : workflowOutputs) {

            if (!outputDefined) {
                toolOutputNodes = node(workflowOutput.getNodeID());
                outputDefined = true;
            } else {
                toolOutputNodes = node(workflowOutput.getNodeID())
                        .link(to(toolOutputNodes).with(Style.INVIS, LinkAttr.weight(100 + index++)));
            }
            workflowGraph = workflowOutput.addTavernaStyleTypeToGraph(workflowGraph);
            workflowGraph = workflowGraph.with(node(workflowOutput.getCreatedByModule().getNodeID())
                    .link(to(node(workflowOutput.getNodeID()))
                            .with(Label.html(workflowOutput.getNodeGraphLabels()), Color.BLACK,
                                    LinkAttr.weight(index++))));
        }
        workflowGraph = workflowGraph.with(graph("outputs_frame").cluster()
                .graphAttr().with(Style.DASHED, Color.BLACK, Label.html("<b>Workflow Outputs</b>"))
                .with(graph("outputs").directed().graphAttr()
                        .with(Rank.inSubgraph(RankType.MIN), Rank.dir(RankDir.LEFT_TO_RIGHT)).with(toolOutputNodes)));

        return new SolutionGraph(workflowGraph);
    }

    /**
     * Get a graph that represent the solution in .dot format (see
     * http://www.graphviz.org/).
     *
     * @return String that represents the solution workflow in .dot graph format.
     */
    public static String generateSolutionDotFormat(SolutionWorkflow workflow) {
        StringBuilder solution = new StringBuilder();
        List<TypeNode> workflowInputs = workflow.getWorkflowInputTypeStates();
        List<TypeNode> workflowOutputs = workflow.getWorkflowOutputTypeStates();
        List<ModuleNode> moduleNodes = workflow.getModuleNodes();

        String input = "\"Workflow INPUT\"";
        String output = "\"Workflow OUTPUT\"";
        boolean inputDefined = false;
        boolean outputDefined = false;

        for (TypeNode workflowInput : workflowInputs) {
            if (!inputDefined) {
                solution.append(input + " [shape=box, color = red];\n");
                inputDefined = true;
            }
            solution.append(input + "->" + workflowInput.getNodeID() + ";\n");
            solution.append(workflowInput.getDotDefinition());
        }

        for (ModuleNode currTool : moduleNodes) {
            solution.append(currTool.getDotDefinition());
            for (TypeNode toolInput : currTool.getInputTypes()) {
                if (!toolInput.isEmpty()) {
                    solution.append(
                            toolInput.getNodeID() + "->" + currTool.getNodeID() + "[label = in, fontsize = 10];\n");
                }
            }
            for (TypeNode toolOutput : currTool.getOutputTypes()) {
                if (!toolOutput.isEmpty()) {
                    solution.append(toolOutput.getDotDefinition());
                    solution.append(
                            currTool.getNodeID() + "->" + toolOutput.getNodeID() + " [label = out, fontsize = 10];\n");
                }
            }
        }
        for (TypeNode workflowOutput : workflowOutputs) {
            if (!outputDefined) {
                solution.append(output + " [shape=box, color = red];\n");
                outputDefined = true;
            }
            solution.append(workflowOutput.getDotDefinition());
            solution.append(workflowOutput.getNodeID() + "->" + output + ";\n");
        }

        return solution.toString();
    }

    /**
     * Return the executable shell script, that corresponds to the given workflow.
     *
     * @return String that represents the shell script for executing the given
     *         workflow.
     */
    public static String generateScriptExecution(SolutionWorkflow workflow) {
        StringBuilder script = new StringBuilder("#!/bin/bash\n");
        List<TypeNode> workflowInputs = workflow.getWorkflowInputTypeStates();
        List<TypeNode> workflowOutputs = workflow.getWorkflowOutputTypeStates();
        List<ModuleNode> moduleNodes = workflow.getModuleNodes();

        // Add the generation header
        script.append(String.format("# WorkflowNo_%02d\n", workflow.getIndex()+1));
        script.append("# This workflow is generated by APE (https://github.com/workflomics/ape).\n");

        // Add input parameter check and assignment.
        script.append("if [ $# -ne " + workflowInputs.size() + " ]\n\tthen\n");
        script
                .append("\t\techo \"" + workflowInputs.size()
                        + " argument(s) expected.\"\n\t\texit\nfi\n");
        int in = 1;
        for (TypeNode input : workflowInputs) {
            script.append(input.getShortNodeID() + "=$" + (in++) + "\n");
        }
        script.append("\n");

        // Process nodes of the workflow.
        for (ModuleNode operation : moduleNodes) {
            String code = operation.getUsedModule().getExecutionCommand();
            if (code == null || code.equals("")) {
                script.append("# Error: Tool '" + operation.getNodeLabel() + "' is missing the execution code. Skipping.")
                        .append("\n");
            } else {
                for (int i = 0; i < operation.getInputTypes().size(); i++) {
                    code = code.replace("@input[" + i + "]", operation.getInputTypes().get(i).getShortNodeID());
                }
                for (int i = 0; i < operation.getOutputTypes().size(); i++) {
                    code = code.replace("@output[" + i + "]", operation.getOutputTypes().get(i).getShortNodeID());
                }
                script.append(code).append("\n");
            }
        }
        int out = 1;
        for (TypeNode output : workflowOutputs) {
            script.append("echo \"" + (out++) + ". output is: $" + output.getShortNodeID() + "\"");
        }

        return script.toString();
    }

    /**
     * Get a readable version of the workflow solution.
     *
     * @return Printable String that represents the solution workflow.
     */
    public static String generateReadableSolution(SolutionWorkflow workflow) {
        StringBuilder solution = new StringBuilder();
        List<TypeNode> workflowInputs = workflow.getWorkflowInputTypeStates();
        List<TypeNode> workflowOutputs = workflow.getWorkflowOutputTypeStates();
        List<ModuleNode> moduleNodes = workflow.getModuleNodes();

        solution.append("WORKFLOW_IN:{");
        int i = 0;
        for (TypeNode workflowInput : workflowInputs) {
            solution.append(workflowInput.toString());
            if (++i < workflowInputs.size()) {
                solution.append(", ");
            }
        }
        solution.append("} |");

        for (ModuleNode currTool : moduleNodes) {
            solution.append(" IN:{");
            i = 0;
            for (TypeNode toolInput : currTool.getInputTypes()) {
                if (!toolInput.isEmpty()) {
                    if (i++ > 1) {
                        solution.append(", ");
                    }
                    solution.append(toolInput.toString());
                }
            }
            solution.append("} ").append(currTool.toString());
            solution.append(" OUT:{");
            i = 0;
            for (TypeNode toolOutput : currTool.getOutputTypes()) {
                if (!toolOutput.isEmpty()) {
                    if (i++ > 1) {
                        solution.append(", ");
                    }
                    solution.append(toolOutput.toString());
                }
            }
            solution.append("} |");
        }
        i = 0;
        solution.append("WORKFLOW_OUT:{");
        for (TypeNode workflowOutput : workflowOutputs) {
            solution.append(workflowOutput.toString());
            if (++i < workflowOutputs.size()) {
                solution.append(", ");
            }
        }
        solution.append("}");

        return solution.toString();
    }

}
