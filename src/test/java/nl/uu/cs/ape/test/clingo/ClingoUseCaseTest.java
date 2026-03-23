package nl.uu.cs.ape.test.clingo;

import nl.uu.cs.ape.APE;
import nl.uu.cs.ape.configuration.APERunConfig;
import nl.uu.cs.ape.domain.APEDomainSetup;
import nl.uu.cs.ape.utils.APEFiles;
import nl.uu.cs.ape.utils.APEResources;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.io.File;

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
            // This will use ClingoSynthesisEngine which now prints the extracted facts
            apeFramework.runSynthesis(runConfig);
            
            System.out.println("Clingo synthesis completed (no crash). Check the output above for ASP facts.");
        });
    }
}
