package nl.uu.cs.ape.test.clingo;

import nl.uu.cs.ape.APE;
import nl.uu.cs.ape.configuration.APERunConfig;
import nl.uu.cs.ape.domain.APEDomainSetup;
import nl.uu.cs.ape.utils.APEFiles;
import nl.uu.cs.ape.utils.APEResources;

import nl.uu.cs.ape.solver.solutionStructure.SolutionsList;
import nl.uu.cs.ape.solver.solutionStructure.SolutionWorkflow;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests to ensure Clingo is invoked correctly using the MaterialsScience use case
 * and that ASP facts are successfully extracted and passed to the jclingo engine.
 */
public class ClingoUseCaseTest {

    @Test
    public void testMaterialsScienceClingoFactExtraction() {
        assertDoesNotThrow(() -> {
            // Load the configuration file
            String configPath = APEResources.getAbsoluteResourcePath("clingo_example/defect_concentration/config.json");
            JSONObject configObject = APEFiles.readFileToJSONObject(new File(configPath));
            
            // Adjust relative paths to absolute based on the example directory
            String baseDir = APEResources.getAbsoluteResourcePath("clingo_example");
            configObject.put("ontology_path", baseDir + "/MaterialsScience_taxonomy.owl");
            configObject.put("tool_annotations_path", baseDir + "/tool_annotations.json");
            configObject.put("constraints_path", baseDir + "/defect_concentration/constraints.json");
            configObject.put("solutions_dir_path", baseDir + "/defect_concentration/solution/");
            
            // Explicitly set CLINGO to be safe, although it's now default
            configObject.put("solverType", "CLINGO"); // Our run config expects SAT/CLINGO enum implicitly or we set it later

            System.out.println("Running APE with Clingo solver...");

            // Initialize the APE framework
            APE apeFramework = new APE(configObject);
            
            // Set up run configuration
            APERunConfig runConfig = new APERunConfig(configObject, apeFramework.getDomainSetup());
            runConfig.setSolverType(nl.uu.cs.ape.models.enums.SolverType.CLINGO);
            
            // Run synthesis
            SolutionsList solutions = apeFramework.runSynthesis(runConfig);

            System.out.println("Clingo synthesis completed. Solutions: " + solutions.getNumberOfSolutions());

            // Verify that solutions have populated module nodes and workflow inputs
            assertFalse(solutions.isEmpty(), "Expected at least one solution");
            SolutionWorkflow first = solutions.get(0);
            assertFalse(first.getModuleNodes().isEmpty(),
                    "First solution must have module nodes (tools); got empty list");
            assertFalse(first.getWorkflowInputTypeStates().isEmpty(),
                    "First solution must have workflow input type states");
            System.out.println("First solution tools: " + first.getNativeSolution().getRelevantToolsInSolution());
        });
    }

    @Test
    public void testBenchmarkCsvWritten() {
        assertDoesNotThrow(() -> {
            String configPath = APEResources.getAbsoluteResourcePath("clingo_example/defect_concentration/config.json");
            JSONObject configObject = APEFiles.readFileToJSONObject(new File(configPath));
            String baseDir = APEResources.getAbsoluteResourcePath("clingo_example");
            configObject.put("ontology_path", baseDir + "/MaterialsScience_taxonomy.owl");
            configObject.put("tool_annotations_path", baseDir + "/tool_annotations.json");
            configObject.put("constraints_path", baseDir + "/defect_concentration/constraints.json");
            configObject.put("solutions_dir_path", baseDir + "/defect_concentration/solution/");

            APE apeFramework = new APE(configObject);
            APERunConfig runConfig = new APERunConfig(configObject, apeFramework.getDomainSetup());
            runConfig.setSolverType(nl.uu.cs.ape.models.enums.SolverType.CLINGO);
            runConfig.setBenchmarkMode(true);

            apeFramework.runSynthesis(runConfig);

            Path csv = runConfig.getSolutionDirPath().resolve("benchmark.csv");
            assertTrue(Files.exists(csv), "benchmark.csv must be created when benchmark mode is on");

            String content = Files.readString(csv);
            assertTrue(content.startsWith("length,"), "CSV must start with header row");
            assertTrue(content.contains("total,"), "CSV must contain a total row");
            System.out.println("benchmark.csv contents:\n" + content);
        });
    }
}
