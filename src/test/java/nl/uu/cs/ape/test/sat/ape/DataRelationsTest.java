package nl.uu.cs.ape.test.sat.ape;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import nl.uu.cs.ape.APE;
import nl.uu.cs.ape.solver.solutionStructure.SolutionsList;
import nl.uu.cs.ape.utils.APEResources;

/**
 * The data ancestor relation (R) and the identity relation (IS) are only
 * encoded when SLTLx formulas are given. A formula over them must still
 * constrain the workflows.
 */
class DataRelationsTest {

    @TempDir
    Path tempDir;

    /**
     * The formula requires two different data instances where one depends on
     * the other. No workflow of length 2 of the GMT example satisfies it, and 13
     * workflows of length 3 do. If the relations were not encoded, the solver
     * could choose them freely and would also return workflows of length 2.
     */
    @Test
    void formulaOverTheAncestorRelationConstrainsTheWorkflows() throws IOException, OWLOntologyCreationException {
        JSONObject config = APEResources.getConfigResource("cli/gmt/base_config.json",
                "cli/gmt/GMT_UseCase_taxonomy.owl", "cli/gmt/tool_annotations.json", "cli/gmt/constraints_e0.json",
                "cli/gmt");
        Path constraints = tempDir.resolve("constraints.json");
        JSONObject formula = new JSONObject().put("constraintid", "SLTLx").put("formula",
                "Exists (?x) Exists (?y) (R(?x,?y) & !(?x = ?y))");
        Files.write(constraints, new JSONObject().put("constraints", new JSONArray().put(formula)).toString()
                .getBytes(StandardCharsets.UTF_8));
        config.put("constraints_path", constraints.toString());
        config.put("solutions_dir_path", tempDir.toString());
        config.put("solutions", "100");
        config.put("solution_length", new JSONObject().put("min", 1).put("max", 3));

        SolutionsList solutions = new APE(config).runSynthesis(config);

        assertEquals(13, solutions.size());
        for (int i = 0; i < solutions.size(); i++) {
            assertEquals(3, solutions.get(i).getSolutionLength());
        }
    }
}
