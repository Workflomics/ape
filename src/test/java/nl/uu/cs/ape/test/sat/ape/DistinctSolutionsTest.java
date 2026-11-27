package nl.uu.cs.ape.test.sat.ape;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import nl.uu.cs.ape.APE;
import nl.uu.cs.ape.solver.solutionStructure.SolutionsList;
import nl.uu.cs.ape.utils.APEResources;

/**
 * Each workflow must be returned only once. With {@code tool_seq_repeat} the
 * workflows are distinguished by their tools, the types of the tool inputs and
 * the data the inputs use, but not by relations that are derived from them,
 * such as the data ancestor relation.
 */
class DistinctSolutionsTest {

    @TempDir
    Path tempDir;

    @Test
    void everyWorkflowIsReturnedOnce() throws IOException, OWLOntologyCreationException {
        JSONObject config = APEResources.getConfigResource("cli/gmt/base_config.json",
                "cli/gmt/GMT_UseCase_taxonomy.owl", "cli/gmt/tool_annotations.json", "cli/gmt/constraints_e0.json",
                "cli/gmt");
        config.put("solutions_dir_path", tempDir.toString());
        config.put("solutions", "200");
        config.put("tool_seq_repeat", "true");

        SolutionsList solutions = new APE(config).runSynthesis(config);

        assertFalse(solutions.isEmpty());
        Set<String> workflows = new HashSet<>();
        for (int i = 0; i < solutions.size(); i++) {
            String workflow = solutions.get(i).getReadableSolution();
            assertTrue(workflows.add(workflow), "Workflow returned twice: " + workflow);
        }
    }
}
