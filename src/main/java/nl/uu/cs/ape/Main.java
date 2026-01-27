package nl.uu.cs.ape;

import lombok.extern.slf4j.Slf4j;
import nl.uu.cs.ape.configuration.APEConfigException;
import nl.uu.cs.ape.configuration.APERunConfig;
import nl.uu.cs.ape.domain.APEDimensionsException;
import nl.uu.cs.ape.domain.BioToolsAPI;
import nl.uu.cs.ape.utils.APEFiles;
import nl.uu.cs.ape.utils.APEUtils;
import nl.uu.cs.ape.utils.WorkflomicsConstants;
import nl.uu.cs.ape.solver.solutionStructure.SolutionsList;
import nl.uu.cs.ape.solver.solutionStructure.cwl.ToolCWLCreator;
import nl.uu.cs.ape.models.Module;

import org.apache.commons.lang3.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * The entry point of application when the library is used in a Command Line
 * Interface (CLI).
 *
 * @author Vedran Kasalica
 */
@Slf4j
public class Main {

    private static final String biotools_config_URL = WorkflomicsConstants.BIOTOOLS_CONFIG_URL;

    /**
     * The entry point of application when the library is used in a Command Line
     * Interface (CLI).
     *
     * @param args APE expects the first argument to specify the method to be
     *             executed.
     *             The rest of the arguments are provided depending on the method.
     */
    public static void main(String[] args) {

        String method = args[0];
        switch (method) {
            case "synthesis":
                executeSynthesis(ArrayUtils.remove(args, 0));
                break;
            case "convert-tools":
                convertBioToolsAnnotations(ArrayUtils.remove(args, 0));
                break;
            case "pull-a-tool":
                pullATool(ArrayUtils.remove(args, 0));
                break;
            case "bio.tools":
                try {
                    BioToolsAPI.getAndSaveFullBioTools("./tools.json");
                } catch (IOException e) {
                    log.error("Error in fetching the tools from bio.tools.");
                }
                break;
            default:
                if (args.length == 0 || args.length == 1) {
                    executeSynthesis(args);
                } else {
                    log.error("Invalid method provided.");
                }
        }

    }

    private static void pullATool(String[] args) {
        if (args.length != 1) {
            log.error("Error: pull-a-tool method expects biotoolsID as the only additional argument.");
            return;
        }

        String biotoolsID = args[0];
        if (biotoolsID.isEmpty()) {
            log.error("Error: biotoolsID should be provided as an additional argument.");
            return;
        }

        try {
            JSONArray tool = BioToolsAPI.getAndConvertToolList(List.of(biotoolsID)).getJSONArray("functions");

            String cwlURL = WorkflomicsConstants.getCwlToolUrl(biotoolsID);

            JSONArray toolArray = new JSONArray();
            JSONObject toolEntry = new JSONObject();
            toolEntry.put("type", "CWL_ANNOTATION");
            toolEntry.put("cwl_reference", cwlURL);
            toolArray.put(toolEntry);

            APEFiles.write2file(toolArray.toString(4), new File("./tool.json"), false);

            for (JSONObject toolAnnotation : APEUtils.getJSONListFromJSONArray(tool)) {
                APE apeFramework = new APE(biotools_config_URL);

                Optional<Module> cometModule = apeFramework.getDomainSetup()
                        .updateModuleFromJson(toolAnnotation);

                if (cometModule.isEmpty()) {
                    log.error(String.format("Error in defining '%s' tool using '%s' configuration.", biotoolsID,
                            biotools_config_URL));
                }
                ToolCWLCreator toolCWLCreator = new ToolCWLCreator(cometModule.get());

                APEFiles.write2file(toolCWLCreator.generate(), new File("./" + toolAnnotation.getString("id") + ".cwl"),
                        false);
            }

        } catch (IOException e) {
            log.error("Error in fetching the tool from bio.tools.");
        } catch (OWLOntologyCreationException e) {
            log.error(String.format(
                    "Error in setting up the APE framework from the %s configuration.", biotools_config_URL));
        }

    }

    /**
     * Retrieve tools from bio.tools using bio.tools API and convert them to
     * APE-compatible tool annotation format.
     * 
     * @param args The arguments provided to the method. Only one argument is
     *             expected, the path to the file where the biotoolsIDs are stored.
     */
    public static void convertBioToolsAnnotations(String[] args) {
        if (args.length != 1) {
            log.error("Error: bio.tools method expects path as the only additional argument.");
            return;
        }

        String pathToListIDs = args[0];
        if (!APEFiles.isValidReadFile(pathToListIDs)) {
            log.error("Error: Invalid path provided.");
            return;
        }

        try {
            JSONObject apeToolAnnotation = BioToolsAPI.getAndConvertToolList(new File(pathToListIDs));
            APEFiles.write2file(apeToolAnnotation.toString(4), new File("./tools.json"), false);
        } catch (IOException e) {
            log.error("Error in fetching the tools from bio.tools.");
            return;
        }
        log.info("File generated successfully in the current directory.");
    }

    /**
     * Executes the synthesis based on the provided configuration file.
     * 
     * @param args The arguments provided to the method.
     */
    public static void executeSynthesis(String[] args) {
        String path;
        int solutionsNo = -1;

        if (args.length > 2) {
            log.error("Error: synthesis method expects at most two additional arguments.");
            return;
        }

        if (args.length == 1) {
            path = args[0];
        } else if (args.length == 2) {
            path = args[0];
            try {
                solutionsNo = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                log.error("Second parameter is not an integer.");
            }
        } else {
            path = "./config.json";
        }
        if (!APEFiles.isValidReadFile(path)) {
            log.error("Bad path.");
            return;
        }

        APE apeFramework = null;
        try {

            // set up the APE framework
            apeFramework = new APE(path);

        } catch (APEConfigException | JSONException | IOException | OWLOntologyCreationException e) {
            log.error("Error in setting up the APE framework:");
            log.error(e.getMessage());
            return;
        }

        SolutionsList solutions;
        try {

            JSONObject runConfigJson = APEFiles.readFileToJSONObject(new File(path));
            APERunConfig runConfig = new APERunConfig(runConfigJson, apeFramework.getDomainSetup());

            if (solutionsNo > 0) {
                runConfig.setMaxNoSolutions(solutionsNo);
            }
            // run the synthesis and retrieve the solutions
            solutions = apeFramework.runSynthesis(runConfig);

        } catch (APEConfigException e) {
            log.error("Error in synthesis execution. APE configuration error:");
            log.error(e.getMessage());
            return;
        } catch (JSONException e) {
            log.error(
                    "Error in synthesis execution. Bad JSON formatting (APE configuration or constriants JSON). ");
            log.error(e.getMessage());
            return;
        } catch (IOException e) {
            log.error("Error in synthesis execution.");
            log.error(e.getMessage());
            return;
        } catch (APEDimensionsException e) {
            log.error("Error in synthesis execution.");
            log.error(e.getMessage());
            return;
        }

        /*
         * Writing solutions to the specified file in human readable format
         */
        if (solutions.isEmpty()) {
            log.info("The problem is UNSAT.");
        } else {
            try {
                boolean createPartialImplementations = solutions.getRunConfiguration().getCreatePartialScripts();
                APE.writeSolutionToFile(solutions);
                // The following method can be changed to write the solutions in different
                // formats (e.g., control flow graph, data flow graph)
                APE.writeTavernaDesignGraphs(solutions);
                APE.writeExecutableWorkflows(solutions, createPartialImplementations);
                APE.writeCWLWorkflows(solutions, createPartialImplementations);
                APE.writeSnakemakeWorkflows(solutions, createPartialImplementations);
            } catch (IOException e) {
                log.error("Error in writing the solutions. to the file system.");
                e.printStackTrace();
            }
        }
    }
}
