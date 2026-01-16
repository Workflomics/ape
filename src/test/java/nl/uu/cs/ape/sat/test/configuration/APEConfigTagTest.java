package nl.uu.cs.ape.sat.test.configuration;

import nl.uu.cs.ape.APE;
import nl.uu.cs.ape.configuration.APECoreConfig;
import nl.uu.cs.ape.configuration.APERunConfig;
import nl.uu.cs.ape.configuration.tags.APEConfigTag;
import nl.uu.cs.ape.configuration.tags.validation.ValidationResults;
import nl.uu.cs.ape.domain.APEDomainSetup;
import nl.uu.cs.ape.utils.APEResources;
import nl.uu.cs.ape.utils.APEUtils;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import static nl.uu.cs.ape.sat.test.utils.Evaluation.fail;
import static nl.uu.cs.ape.sat.test.utils.Evaluation.success;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The {@code APEConfigTagTest} class is used to test the functionality of the
 * {@link APEConfigTag} class.
 */
@Slf4j
public class APEConfigTagTest {

    @Test
    public void infoOutputTest() {

        log.debug("Test type tags..");

        for (APEConfigTag.Info<?> tag : APERunConfig.TAGS.getAll()) {

            if (tag.type == APEConfigTag.TagType.INTEGER) {
                log.debug("Web API shows `{}` box for tag `{}`, with min:`{}` and max:`{}`\n", tag.type,
                        tag.label, tag.constraints.getInt("min"), tag.constraints.getInt("max"));
            }
        }

        log.debug("\n### Display all tag info ####\nCORE:\n{}\n\nRUN:\n{}\n",
                APECoreConfig.TAGS.toJSON().toString(3),
                APERunConfig.TAGS.toJSON().toString(3));
    }

    @Test
    public void coreValidationTest() {
        final JSONObject correct_config = APEResources.getJSONResource("cli/gmt/base_config.json");
        if (correct_config.isEmpty())
        {
            fail("Could not retrieve resource '%s' ", "cli/gmt/base_config.json");
        } else {
            // add paths to the other files to the configuration
            correct_config.put("ontology_path", APEResources.getAbsoluteResourcePath("cli/gmt/GMT_UseCase_taxonomy.owl"))
                    .put("tool_annotations_path", APEResources.getAbsoluteResourcePath("cli/gmt/tool_annotations.json"))
                    .put("constraints_path", APEResources.getAbsoluteResourcePath("cli/gmt/constraints_e0.json"))
                    .put("solutions_dir_path", APEResources.getAbsoluteResourcePath("cli/gmt"));
        }

        ValidationResults results = APECoreConfig.validate(correct_config);

        assertFalse(results.hasFails());

        /* Test missing obligatory tag */
        List<String> tags = APECoreConfig.TAGS.getObligatory().stream().map(info -> info.tag_name)
                .collect(Collectors.toList());
        for (String tag : tags) {

            JSONObject altered_config = APEUtils.clone(correct_config);
            altered_config.remove(tag);

            results = APECoreConfig.validate(altered_config);

            assertTrue(results.hasFails());
            assertTrue(results.stream().anyMatch(result -> result.isFail() && result.getTag().equals(tag)));

            success("CoreConfig is missing an obligatory tag -> %s", results.getFails().get(0).toJSON().toString());
        }

        /* Test missing optional tag */
        tags = APECoreConfig.TAGS.getOptional().stream().map(info -> info.tag_name).collect(Collectors.toList());
        for (String tag : tags) {

            JSONObject altered_config = APEUtils.clone(correct_config);
            altered_config.remove(tag);

            results = APECoreConfig.validate(altered_config);

            assertFalse(results.hasFails());
        }

        /* Test incorrect tag */
        tags = APECoreConfig.TAGS.getAll().stream().map(info -> info.tag_name).collect(Collectors.toList());
        for (String tag : tags) {

            JSONObject altered_config = APEUtils.clone(correct_config)
                    .put(tag, "");

            results = APECoreConfig.validate(altered_config);

            assertTrue(results.hasFails());
            assertTrue(results.stream().anyMatch(result -> result.isFail() && result.getTag().equals(tag)));

            success("CoreConfig contains an incorrect tag -> %s", results.getFails().get(0).toJSON().toString());
        }
    }

    @Test
    public void runValidationTest() throws IOException, OWLOntologyCreationException {
        final JSONObject correct_config = APEResources.getJSONResource("cli/gmt/base_config.json");
        if (correct_config.isEmpty())
        {
            fail("Could not retrieve resource '%s' ", "cli/gmt/base_config.json");
        } else {
            // add paths to the other files to the configuration
            correct_config.put("ontology_path", APEResources.getAbsoluteResourcePath("cli/gmt/GMT_UseCase_taxonomy.owl"))
                    .put("tool_annotations_path", APEResources.getAbsoluteResourcePath("cli/gmt/tool_annotations.json"))
                    .put("constraints_path", APEResources.getAbsoluteResourcePath("cli/gmt/constraints_e0.json"))
                    .put("solutions_dir_path", APEResources.getAbsoluteResourcePath("cli/gmt"));
        }

        APEDomainSetup domainSetup = new APE(correct_config).getDomainSetup();

        ValidationResults results = APERunConfig.validate(correct_config, domainSetup);

        assertFalse(results.hasFails());

        /* Test missing obligatory tag */
        List<String> tags = APERunConfig.TAGS.getObligatory().stream().map(info -> info.tag_name)
                .collect(Collectors.toList());
        for (String tag : tags) {

            JSONObject altered_config = APEUtils.clone(correct_config);
            altered_config.remove(tag);

            results = APERunConfig.validate(altered_config, domainSetup);

            assertTrue(results.hasFails());
            assertTrue(results.stream().anyMatch(result -> result.isFail() && result.getTag().equals(tag)));

            success("RunConfig is missing an obligatory tag -> %s", results.getFails().get(0).toJSON().toString());
        }

        /* Test missing optional tag */
        tags = APERunConfig.TAGS.getOptional().stream().map(info -> info.tag_name).collect(Collectors.toList());
        for (String tag : tags) {

            JSONObject altered_config = APEUtils.clone(correct_config);
            altered_config.remove(tag);

            results = APERunConfig.validate(altered_config, domainSetup);

            assertFalse(results.hasFails());
        }

        /* Test incorrect tag */
        tags = APERunConfig.TAGS.getAll().stream().map(info -> info.tag_name).collect(Collectors.toList());
        for (String tag : tags) {

            JSONObject altered_config = APEUtils.clone(correct_config)
                    .put(tag, "");

            results = APERunConfig.validate(altered_config, domainSetup);

            assertTrue(results.hasFails());
            assertTrue(results.stream().anyMatch(result -> result.isFail() && result.getTag().equals(tag)));

            success("RunConfig contains an incorrect tag -> %s", results.getFails().get(0).toJSON().toString());
        }
    }

}
