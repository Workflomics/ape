package nl.uu.cs.ape.sat.test.configuration;

import nl.uu.cs.ape.APE;
import nl.uu.cs.ape.configuration.APEConfigException;
import nl.uu.cs.ape.configuration.APECoreConfig;
import nl.uu.cs.ape.configuration.APERunConfig;
import nl.uu.cs.ape.configuration.tags.APEConfigTag;
import nl.uu.cs.ape.models.Range;
import nl.uu.cs.ape.models.enums.ConfigEnum;
import nl.uu.cs.ape.sat.test.utils.Evaluation;
import nl.uu.cs.ape.utils.APEResources;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import java.io.IOException;

import static nl.uu.cs.ape.sat.test.utils.Evaluation.fail;
import static nl.uu.cs.ape.sat.test.utils.Evaluation.success;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests whether human mistakes in the configuration are detected and
 * exceptions are thrown with relevant information.
 */
class APEConfigTest {

    /**
     * A correct template construct that is converted to a String at initialisation.
     */
    private static final String jsonTemplate = new JSONObject()
            .put("ontology_path", APEResources.getAbsoluteResourcePath("template/ontology.owl"))
            .put("ontologyPrefixIRI", "http://www.co-ode.org/ontologies/ont.owl#")
            .put("toolsTaxonomyRoot", "ToolsTaxonomy")
            .put("dataDimensionsTaxonomyRoots", new String[] { "TypesTaxonomy" })
            .put("tool_annotations_path", APEResources.getAbsoluteResourcePath("template/tool_annotations.json"))
            .put("constraints_path", APEResources.getAbsoluteResourcePath("template/constraints.json"))
            .put("solutions_dir_path", APEResources.getAbsoluteResourcePath("template"))
            .put("tool_seq_repeat", false)
            .put("solution_length", new JSONObject().put(Range.MIN_TAG, 1).put(Range.MAX_TAG, 5))
            .put("solutions", 5)
            .put("number_of_execution_scripts", 1)
            .put("number_of_generated_graphs", 1)
            .put("inputs",
                    new JSONObject[] { new JSONObject().put("TypesTaxonomy", new String[] { "XYZ_table_file" }) })
            .put("outputs", new JSONObject[] { new JSONObject().put("TypesTaxonomy", new String[] { "PostScript" }) })
            .put("use_workflow_input", ConfigEnum.ALL.toString())
            .put("debug_mode", false)
            .put("use_all_generated_data", ConfigEnum.ONE.toString())
            .toString();

    /**
     * The tags can be divided in these categories displayed in the array.
     * TagInfo takes the following data:
     * - Type
     * - Correct value example
     * - Incorrect value examples
     * - Tag names of this type.
     */
    private static final TagInfo[] tagTypes = new TagInfo[] {
            new TagInfo(
                    "Boolean",
                    true,
                    new Object[] { "wrong", 1 },
                    new String[] { "debug_mode", "tool_seq_repeat" }),
            new TagInfo(
                    "Enum",
                    ConfigEnum.ONE.toString(),
                    new Object[] { false, "wrong", 1 },
                    new String[] { "use_workflow_input", "use_all_generated_data" }),
            new TagInfo(
                    "Integer",
                    1,
                    new Object[] { false, "wrong" },
                    new String[] { "solutions", "number_of_execution_scripts", "number_of_generated_graphs" }),
            new TagInfo(
                    "Range",
                    new JSONObject().put(Range.MIN_TAG, 2).put(Range.MAX_TAG, 5),
                    new Object[] { false, "wrong", 2 },
                    new String[] { "solution_length" }),
            new TagInfo(
                    "TaxonomyEntity",
                    "ToolsTaxonomy",
                    new Object[] { false, "wrong" },
                    new String[] { "toolsTaxonomyRoot" }),
            new TagInfo(
                    "TaxonomyEntity",
                    "TypesTaxonomy",
                    new Object[] { false, "wrong" },
                    new String[] { "dataDimensionsTaxonomyRoots" }),
    };

    /**
     * Get a copy of the configuration template.
     *
     * @return A copy of the configuration template.
     */
    public static JSONObject getCorrectTemplate() {
        return new JSONObject(jsonTemplate);
    }

    /**
     * Verify that the template is still correct.
     * Setting up the framework should not induce any exceptions.
     */
    @Test
    public void verifyCorrectTemplate() {
        assertDoesNotThrow(() -> {
            JSONObject template = getCorrectTemplate();
            APECoreConfig config = new APECoreConfig(template);
            APE ape = new APE(config);
            new APERunConfig(template, ape.getDomainSetup());
        });
    }

    /**
     * Test if the {@link APEConfigException#missingTag} exception
     * is thrown on obligatory tags that are missing.
     */
    @Test
    public void testMissingTags() {

        /*
         * Missing one of the obligatory core tags should result in an exception while
         * creating the framework.
         */
        for (APEConfigTag.Info<?> info : APECoreConfig.TAGS.getObligatory()) {
            JSONObject obj = getCorrectTemplate();
            obj.remove(info.tag_name);
            assertThrows(APEConfigException.class, () -> new APECoreConfig(obj));
        }

        /*
         * Missing one of the obligatory run tags should result in an exception while
         * executing the run phase.
         */
        for (APEConfigTag.Info<?> tag : APERunConfig.TAGS.getObligatory()) {
            JSONObject obj = getCorrectTemplate();
            obj.remove(tag.tag_name);
            assertDoesNotThrow(() -> new APECoreConfig(obj));
            assertThrows(APEConfigException.class, () -> setupRun(obj));
        }

        /*
         * Missing one of the optional tags should not throw an exception, but should
         * display a warning.
         */
        for (APEConfigTag.Info<?> tag_info : APECoreConfig.TAGS.getOptional()) {
            JSONObject obj = getCorrectTemplate();
            obj.remove(tag_info.tag_name);
            assertDoesNotThrow(() -> new APECoreConfig(obj));
            assertDoesNotThrow(() -> setupRun(obj));
        }
    }

    /**
     * Test if the {@link APEConfigException#pathNotFound} exception
     * is thrown on an incorrect value for tags that expect a path.
     *
     * @throws OWLOntologyCreationException Error reading the OWL (ontology) file.
     */
    @Test
    public void testIncorrectFilePaths() {

        final String[] pathTags = new String[] { "ontology_path", "tool_annotations_path" };
        final String[] wrongPaths = new String[] { null, "", "./does/not/exist.json", "does/not/exist.json",
                "./does/not/exist/", "does/not/exist/",
                APEResources.getAbsoluteResourcePath("") + "\\doesnotexist.json" };

        for (String tag : pathTags) {
            for (String path : wrongPaths) {
                try {
                    setupRun(getCorrectTemplate().put(tag, path));
                    fail("Expected exception for APECoreConfig with a wrong path '%s' for tag '%s' was not thrown.",
                            path, tag);
                } catch (APEConfigException | JSONException | IOException | OWLOntologyCreationException e) {
                    assertTrue(e.getMessage().contains(tag));
                    success("Expected exception was thrown for APECoreConfig with a wrong path for tag '%s'\nAPE message was: %s",
                            tag, e.getMessage());
                }
            }
        }
    }

    /**
     * Test if the {@link APEConfigException#pathNotFound} exception
     * is thrown on an incorrect value for tags that expect a path.
     *
     * @throws OWLOntologyCreationException Error reading the OWL (ontology) file.
     */
    @Test
    public void testIncorrectDirectoryPaths() {

        final String[] pathTags = new String[] { "solutions_dir_path" };
        final String[] wrongPaths = new String[] { "file.json",
                APEResources.getAbsoluteResourcePath("") + "\\file.json" };

        for (String tag : pathTags) {
            for (String path : wrongPaths) {
                try {
                    setupRun(getCorrectTemplate().put(tag, path));
                    fail("Expected exception for APEConfig with a wrong path '%s' for tag '%s' was not thrown.", path,
                            tag);
                } catch (APEConfigException | JSONException | IOException | OWLOntologyCreationException e) {
                    assertTrue(e.getMessage().contains(tag));
                    success("Expected exception was thrown for APEConfig with a wrong path for tag '%s'\nAPE message was: %s",
                            tag, e.getMessage());
                }
            }
        }
    }

    @Test
    public void tagTypeTest() {

        Evaluation.TagTypeEvaluation evaluation;

        for (TagInfo tagInfo : tagTypes) {

            // true positive
            evaluation = new Evaluation.TagTypeEvaluation(tagInfo.tagType, false);
            for (String tag : tagInfo.tags) {
                try {
                    setupRun(getCorrectTemplate().put(tag, tagInfo.correctExample)); // set invalid value for
                                                                                     // non-boolean tag and run the
                                                                                     // configuration
                    evaluation.forTag(tag).result(true);
                } catch (Exception e) {
                    evaluation.forTag(tag).result(false, e);
                }
            }

            for (String tag : tagInfo.tags) {
                for (Object wrongExample : tagInfo.wrongExamples) {
                    try {
                        setupRun(getCorrectTemplate().put(tag, wrongExample)); // set invalid value for boolean tag and
                                                                               // run the configuration
                        evaluation.forTag(tag).result(false);
                    } catch (Exception e) {
                        evaluation.forTag(tag).result(true, e);
                    }
                }
            }
        }
    }

    /**
     * Creates a configuration from JSON and executes {@link APECoreConfig}
     *
     * @param obj The configuration to set up.
     * @throws IOException                  Error if a path provided in the
     *                                      configuration file is incorrect.
     * @throws OWLOntologyCreationException Error reading the OWL (ontology) file.
     */
    private void setupRun(JSONObject obj) throws IOException, OWLOntologyCreationException {
        APE ape = new APE(obj);
        new APECoreConfig(obj);
        new APERunConfig(obj, ape.getDomainSetup());
    }

    static class TagInfo {

        public final String[] tags;
        public final String tagType;
        public final Object correctExample;
        public final Object[] wrongExamples;

        public TagInfo(String tagType, Object correctExample, Object[] wrongExamples, String[] tags) {
            this.tagType = tagType;
            this.tags = tags;
            this.correctExample = correctExample;
            this.wrongExamples = wrongExamples;
        }
    }
}
