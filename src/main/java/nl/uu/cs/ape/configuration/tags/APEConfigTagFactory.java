package nl.uu.cs.ape.configuration.tags;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.uu.cs.ape.configuration.APEConfigException;
import nl.uu.cs.ape.configuration.tags.validation.ValidationResults;
import nl.uu.cs.ape.domain.APEDomainSetup;
import nl.uu.cs.ape.utils.APEFiles;
import nl.uu.cs.ape.utils.APEUtils;
import nl.uu.cs.ape.models.Range;
import nl.uu.cs.ape.models.Type;
import nl.uu.cs.ape.models.enums.ConfigEnum;

import javax.inject.Provider;

import static nl.uu.cs.ape.configuration.tags.APEConfigTag.TagType.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class APEConfigTagFactory {

    private static final String ONTOLOGY_IRI_MSG = "Ontology IRI should be an absolute IRI (Internationalized Resource Identifier).";

    /** Hide the implicit public constructor. */
    private APEConfigTagFactory() {
    }

    /**
     * Types of tag fields.
     * 
     * @author Vedran Kasalica
     *
     */
    public static class TYPES {

        /** Hide the implicit public constructor. */
        private TYPES() {
        }

        /**
         * Abstract field type.
         */
        public abstract static class ExistingFile extends APEConfigTag<File> {

            @Override
            public TagType getType() {
                return TagType.FILE_PATH;
            }

            @Override
            protected File constructFromJSON(JSONObject obj) {
                final String input = obj.getString(getTagName());
                try {
                    return APEFiles.readPathToFile(input);
                } catch (IOException e) {
                    throw APEConfigException.invalidValue(getTagName(), input, e.getMessage());
                }
            }

            protected abstract APEFiles.Permission[] getRequiredPermissions();

            @Override
            public ValidationResults validate(File file, ValidationResults results) {
                results.add(getTagName(), "The file should exist.", file.exists());
                return results;
            }
        }

        /**
         * Abstract field type.
         */
        public abstract static class DataDimensions extends APEConfigDependentTag.One<List<String>, String> {

            protected DataDimensions(Provider<String> provider) {
                super(provider);
            }

            @Override
            protected List<String> constructFromJSON(JSONObject obj, String ontologyPrefix) {

                if (ontologyPrefix == null) {
                    throw APEConfigException.requiredValidationTag(getTagName(),
                            new TAGS.ONTOLOGY_PREFIX().getTagName(), "");
                }

                List<String> dataDimensionRoots = new ArrayList<>();
                try {
                    for (String subTaxonomy : APEUtils.getListFromJson(obj, getTagName(), String.class)) {
                        dataDimensionRoots.add(APEUtils.createClassIRI(subTaxonomy, ontologyPrefix));
                    }
                } catch (ClassCastException e) {
                    throw APEConfigException.invalidValue(getTagName(), obj, "expected a list in correct format.");
                } catch (IllegalArgumentException e) {
                    throw APEConfigException.invalidValue(getTagName(), obj, "elements of the list cannot be empty.");
                }
                return dataDimensionRoots;
            }

            @Override
            protected ValidationResults validate(List<String> value, String ontologyPrefix,
                    ValidationResults results) {
                // TODO
                return results;
            }

            @Override
            public TagType getType() {
                return TagType.DATA_DIMENSIONS;
            }

        }

        /**
         * Abstract field type.
         */
        public abstract static class DataInstances extends APEConfigDependentTag.One<List<Type>, APEDomainSetup> {

            protected DataInstances(Provider<APEDomainSetup> provider) {
                super(provider);
            }

            @Override
            public TagType getType() {
                return TagType.DATA_INSTANCES;
            }

            @Override
            protected ValidationResults validate(List<Type> value, APEDomainSetup apeDomainSetup,
                    ValidationResults results) {
                // TODO: check data instances
                return results;
            }

            @Override
            public APEConfigDefaultValue<List<Type>> getDefault() {
                return APEConfigDefaultValue.withDefault(new ArrayList<>());
            }

        }

        /**
         * Abstract field type.
         */
        public abstract static class Bool extends APEConfigTag<Boolean> {

            @Override
            public TagType getType() {
                return TagType.BOOLEAN;
            }

            @Override
            protected Boolean constructFromJSON(JSONObject obj) {
                return obj.getBoolean(getTagName());
            }

            @Override
            protected ValidationResults validate(Boolean value, ValidationResults results) {
                return results;
            }
        }

        /**
         * Abstract field type.
         */
        public abstract static class Int extends APEConfigTag<Integer> {

            private final Range range;

            protected Int(Range range) {
                this.range = range;
            }

            @Override
            public TagType getType() {
                return TagType.INTEGER;
            }

            @Override
            protected Integer constructFromJSON(JSONObject obj) {
                return obj.getInt(getTagName());
            }

            @Override
            public APEConfigDefaultValue<Integer> getDefault() {
                return APEConfigDefaultValue.noDefault();
            }

            @Override
            protected JSONObject getTagConstraints() {
                return range.toJSON();
            }
        }

        /**
         * Abstract field type.
         */
        public abstract static class IntRange extends APEConfigTag<Range> {

            private final Range boundaries;

            protected IntRange(Range boundaries) {
                this.boundaries = boundaries;
            }

            @Override
            protected Range constructFromJSON(JSONObject obj) {
                return Range.from(obj.getJSONObject(getTagName()));
            }

            @Override
            public APEConfigDefaultValue<Range> getDefault() {
                return APEConfigDefaultValue.noDefault();
            }

            @Override
            public TagType getType() {
                return TagType.INTEGER_RANGE;
            }

            @Override
            protected ValidationResults validate(Range range, ValidationResults results) {
                results.add(getTagName(),
                        "Maximal solution length should be greater or equal to the minimal solution length.",
                        range.getMax() >= range.getMin());
                return results;
            }

            @Override
            protected JSONObject getTagConstraints() {
                return boundaries.toJSON();
            }
        }

        /**
         * Abstract field type.
         */
        public abstract static class Directory extends APEConfigTag<Path> {

            protected abstract APEFiles.Permission[] getRequiredPermissions();

            @Override
            public TagType getType() {
                return TagType.FOLDER_PATH;
            }

            @Override
            protected Path constructFromJSON(JSONObject obj) {
                final String input = obj.getString(getTagName());
                try {
                    return APEFiles.readDirectoryPath(getTagName(), input, getRequiredPermissions());
                } catch (IOException e) {
                    throw APEConfigException.invalidValue(getTagName(), input, e.getMessage());
                }
            }

            @Override
            public APEConfigDefaultValue<Path> getDefault() {
                return APEConfigDefaultValue.withDefault(null);
            }

            @Override
            protected ValidationResults validate(Path value, ValidationResults results) {
                // results.add(getTagName(), "Directory does not exist.",
                // APEFiles.directoryExists(value));
                return results;
            }
        }

        /**
         * Abstract field type.
         */
        public abstract static class Option<E extends Enum<E>> extends APEConfigTag<E> {

            @Override
            public TagType getType() {
                return TagType.ENUM;
            }

            public abstract Class<E> getEnumClass();

            public E[] getOptions() {
                return getEnumClass().getEnumConstants();
            }

            @Override
            protected E constructFromJSON(JSONObject json) {
                return E.valueOf(getEnumClass(), json.getString(getTagName()).toUpperCase());
            }

            @Override
            protected ValidationResults validate(E value, ValidationResults results) {
                return results;
            }

            @Override
            protected JSONObject getTagConstraints() {
                return new JSONObject().put("options", new JSONArray(getOptions()));
            }
        }

        /**
         * Abstract field type.
         */
        public abstract static class IRI extends APEConfigTag<String> {

            @Override
            public TagType getType() {
                return TagType.IRI;
            }

            @Override
            protected String constructFromJSON(JSONObject obj) {
                return obj.getString(getTagName());
            }

            @Override
            protected ValidationResults validate(String uri, ValidationResults results) {
                results.add(getTagName(), "ONTOLOGY_IRI_MSG", APEFiles.isURI(uri));
                return results;
            }
        }

        /**
         * Abstract field type.
         */
        public abstract static class JSONFile extends APEConfigTag<JSONArray> {

            @Override
            public TagType getType() {
                return TagType.FILE_PATH;
            }

            @Override
            protected JSONArray constructFromJSON(JSONObject obj) {
                String constraintsPath = obj.getString(getTagName());
                JSONArray constraints = null;
                try {
                    constraints = APEFiles.readPathToJSONObject(constraintsPath).getJSONArray("constraints");
                } catch (IOException | JSONException e) {
                    throw APEConfigException.invalidValue(getTagName(), constraintsPath, e.getMessage());
                }

                return constraints;
            }

            @Override
            protected ValidationResults validate(JSONArray jsonArray, ValidationResults results) {
                results.add(getTagName(), "JSON Array is not well formatted.", APEFiles.isJSONArray(jsonArray));
                return results;
            }
        }

        /**
         * Abstract field type.
         */
        public abstract static class JSONContent extends APEConfigTag<JSONArray> {

            @Override
            public TagType getType() {
                return TagType.JSON;
            }

            @Override
            protected JSONArray constructFromJSON(JSONObject obj) {
                try {
                    return obj.getJSONArray(getTagName());
                } catch (JSONException e) {
                    throw APEConfigException.invalidValue(getTagName(), obj, e.getMessage());
                }
            }

            @Override
            protected ValidationResults validate(JSONArray jsonArray, ValidationResults results) {
                results.add(getTagName(), "JSON Array is not well formatted.", APEFiles.isJSONArray(jsonArray));
                return results;
            }
        }
    }

    /**
     * Configuration tags.
     */
    public static class TAGS {

        /** Hide the implicit public constructor. */
        private TAGS() {
        }

        /**
         * Configuration field.
         */
        public static class ONTOLOGY extends TYPES.ExistingFile {

            @Override
            public String getTagName() {
                return "ontology_path";
            }

            @Override
            public String getLabel() {
                return "Ontology";
            }

            @Override
            public String getDescription() {
                return "This tag should be a path to an existing .owl file.";
            }

            @Override
            public APEConfigDefaultValue<File> getDefault() {
                return APEConfigDefaultValue.noDefault();
            }

            @Override
            protected APEFiles.Permission[] getRequiredPermissions() {
                return new APEFiles.Permission[] { APEFiles.Permission.READ };
            }
        }

        /**
         * Configuration field.
         */
        public static class ONTOLOGY_PREFIX extends TYPES.IRI {

            @Override
            public String getTagName() {
                return "ontologyPrefixIRI";
            }

            @Override
            public String getLabel() {
                return "Ontology Prefix";
            }

            @Override
            public String getDescription() {
                return ONTOLOGY_IRI_MSG;
            }

            @Override
            public APEConfigDefaultValue<String> getDefault() {
                return APEConfigDefaultValue.noDefault();
            }

        }

        /**
         * Configuration field.
         */
        public static class DIMENSIONS_ONTOLOGY extends TYPES.DataDimensions {

            public DIMENSIONS_ONTOLOGY(Provider<String> provider) {
                super(provider);
            }

            @Override
            public String getTagName() {
                return "dataDimensionsTaxonomyRoots";
            }

            @Override
            public String getLabel() {
                return "Data Dimensions";
            }

            @Override
            public String getDescription() {
                return "List of ontology classes that represent data dimensions (e.g., data type, data format).";
            }

            @Override
            public APEConfigDefaultValue<List<String>> getDefault() {
                return APEConfigDefaultValue.noDefault();
            }
        }

        /**
         * Configuration field.
         */
        public static class PROGRAM_INPUTS extends TYPES.DataInstances {

            public PROGRAM_INPUTS(Provider<APEDomainSetup> provider) {
                super(provider);
            }

            @Override
            public String getTagName() {
                return "inputs";
            }

            @Override
            public String getLabel() {
                return "Input data";
            }

            @Override
            public String getDescription() {
                return "List of input data instances. Each input data instance is defined by a taxonomy class per dimension.";
            }

            @Override
            protected List<Type> constructFromJSON(JSONObject obj, APEDomainSetup apeDomainSetup) {
                final ArrayList<Type> instances = new ArrayList<>();

                if (apeDomainSetup == null) {
                    throw APEConfigException.requiredValidationTag(getTagName(), "core configuration", "");
                }

                try {
                    for (JSONObject jsonType : APEUtils.getListFromJson(obj, getTagName(), JSONObject.class)) {
                        Type input;
                        if ((input = Type.taxonomyInstanceFromJson(jsonType, apeDomainSetup, true)) != null) {
                            instances.add(input);
                        }
                    }
                } catch (ClassCastException e) {
                    instances.clear();
                    throw APEConfigException.cannotParse(getTagName(), obj.get(getTagName()).toString(),
                            JSONObject[].class,
                            "please provide the correct format.");
                }

                return instances;
            }
        }

        /**
         * Configuration field.
         */
        public static class PROGRAM_OUTPUTS extends TYPES.DataInstances {

            public PROGRAM_OUTPUTS(Provider<APEDomainSetup> provider) {
                super(provider);
            }

            @Override
            public String getTagName() {
                return "outputs";
            }

            @Override
            public String getLabel() {
                return "Output data";
            }

            @Override
            public String getDescription() {
                return "List of output data instances. Each output data instance is defined by a taxonomy class per dimension.";
            }

            @Override
            protected List<Type> constructFromJSON(JSONObject obj, APEDomainSetup apeDomainSetup) {
                final ArrayList<Type> instances = new ArrayList<>();

                if (apeDomainSetup == null) {
                    throw APEConfigException.requiredValidationTag(getTagName(), "core configuration", "");
                }

                try {
                    for (JSONObject jsonType : APEUtils.getListFromJson(obj, getTagName(), JSONObject.class)) {
                        Type output;
                        if ((output = Type.taxonomyInstanceFromJson(jsonType, apeDomainSetup, false)) != null) {
                            instances.add(output);
                        }
                    }
                } catch (ClassCastException e) {
                    instances.clear();
                    throw APEConfigException.cannotParse(getTagName(), obj.get(getTagName()).toString(),
                            JSONObject[].class,
                            "please provide the correct format.");
                }

                return instances;
            }
        }

        /**
         * Configuration field.
         */
        public static class TOOL_ONTOLOGY_ROOT extends APEConfigDependentTag.One<String, String> {

            public TOOL_ONTOLOGY_ROOT(Provider<String> prefixProvider) {
                super(prefixProvider);
            }

            @Override
            public String getTagName() {
                return "toolsTaxonomyRoot";
            }

            @Override
            public String getLabel() {
                return "Tools root";
            }

            @Override
            public TagType getType() {
                return TagType.MODULE;
            }

            @Override
            public String getDescription() {
                return "Ontology class that represents the root of the tools taxonomy.";
            }

            @Override
            public APEConfigDefaultValue<String> getDefault() {
                return APEConfigDefaultValue.noDefault();
            }

            @Override
            protected String constructFromJSON(JSONObject obj, String prefix) {
                return APEUtils.createClassIRI(obj.getString(getTagName()), prefix);
            }

            @Override
            protected ValidationResults validate(String value, String prefix, ValidationResults results) {
                return results;
            }
        }

        /**
         * Configuration field.
         */
        public static class TOOL_ANNOTATIONS extends TYPES.ExistingFile {

            @Override
            protected APEFiles.Permission[] getRequiredPermissions() {
                return new APEFiles.Permission[] { APEFiles.Permission.READ };
            }

            @Override
            public String getTagName() {
                return "tool_annotations_path";
            }

            @Override
            public String getLabel() {
                return "Tool annotations";
            }

            @Override
            public String getDescription() {
                return "This tag should be a path to an existing .json file.";
            }

            @Override
            public APEConfigDefaultValue<File> getDefault() {
                return APEConfigDefaultValue.noDefault();
            }
        }

        /**
         * Configuration field.
         */
        public static class CONSTRAINTS_FILE extends TYPES.JSONFile {

            @Override
            public String getTagName() {
                return "constraints_path";
            }

            @Override
            public String getLabel() {
                return "Constraints file path";
            }

            @Override
            public String getDescription() {
                return "Path to the .json file containing the constraints.";
            }

            @Override
            public APEConfigDefaultValue<JSONArray> getDefault() {
                return APEConfigDefaultValue.withDefault(null);
            }
        }

        /**
         * Configuration field.
         */
        public static class CONSTRAINTS_CONTENT extends TYPES.JSONContent {

            @Override
            public String getTagName() {
                return "constraints";
            }

            @Override
            public String getLabel() {
                return "Constraints";
            }

            @Override
            public String getDescription() {
                return "JSON object containing the constraints.";
            }

            @Override
            public APEConfigDefaultValue<JSONArray> getDefault() {
                return APEConfigDefaultValue.withDefault(null);
            }
        }

        /**
         * Configuration field.
         */
        public static class STRICT_TOOL_ANNOTATIONS extends TYPES.Bool {

            @Override
            public String getTagName() {
                return "strict_tool_annotations";
            }

            @Override
            public String getLabel() {
                return "Implement strict tool annotations";
            }

            @Override
            public String getDescription() {
                return "Tag to indicate whether strict tool annotations should be implemented.";
            }

            @Override
            public APEConfigDefaultValue<Boolean> getDefault() {
                return APEConfigDefaultValue.withDefault(true);
            }
        }

        /**
         * Configuration field.
         */
        public static class SOLUTION_LENGTH_RANGE extends TYPES.IntRange {

            public SOLUTION_LENGTH_RANGE() {
                super(Range.of(1, 50));
            }

            @Override
            public String getTagName() {
                return "solution_length";
            }

            @Override
            public String getLabel() {
                return "Solution min/max length";
            }

            @Override
            public String getDescription() {
                return "Number of desired steps in the solution. The minimal solution length should be greater or equal to 0.";
            }

            @Override
            protected ValidationResults validate(Range range, ValidationResults results) {
                results.add(super.validate(range, results));
                results.add(getTagName(),
                        "Minimal solution length should be greater or equal to 0.",
                        range.getMin() > 0);
                return results;
            }
        }

        /**
         * Configuration field.
         */
        public static class NO_SOLUTIONS extends TYPES.Int {

            public NO_SOLUTIONS() {
                super(Range.of(0, Integer.MAX_VALUE));
            }

            @Override
            public String getTagName() {
                return "solutions";
            }

            @Override
            public String getLabel() {
                return "Number of solutions";
            }

            @Override
            public String getDescription() {
                return "Number of solutions to be generated. The number of solutions should be greater or equal to 0.";
            }

            @Override
            protected ValidationResults validate(Integer i, ValidationResults results) {
                results.add(getTagName(), "The number of generated solutions should be greater or equal to 0.", i >= 0);
                return results;
            }
        }

        /**
         * Configuration field.
         */
        public static class SOLUTION_DIR_PATH extends TYPES.Directory {

            @Override
            protected APEFiles.Permission[] getRequiredPermissions() {
                return new APEFiles.Permission[] { APEFiles.Permission.READ, APEFiles.Permission.WRITE };
            }

            @Override
            public String getTagName() {
                return "solutions_dir_path";
            }

            @Override
            public String getLabel() {
                return "Solution directory";
            }

            @Override
            public String getDescription() {
                return "Path to the directory where the solutions should be stored.";
            }
        }

        /**
         * Configuration field.
         */
        public static class CREATE_PARTIAL_SCRIPTS extends TYPES.Bool {

            @Override
            public String getTagName() {
                return "create_partial_scripts";
            }

            @Override
            public String getLabel() {
                return "Create partial script files";
            }

            @Override
            public String getDescription() {
                return "Tag to indicate whether partial shell scripts shall be created.";
            }

            @Override
            public APEConfigDefaultValue<Boolean> getDefault() {
                return APEConfigDefaultValue.withDefault(true);
            }
        }


        /**
         * Configuration field.
         */
        public static class NO_EXECUTIONS extends TYPES.Int {

            public NO_EXECUTIONS() {
                super(Range.of(0, Integer.MAX_VALUE));
            }

            @Override
            public String getTagName() {
                return "number_of_execution_scripts";
            }

            @Override
            public String getLabel() {
                return "Number of executions scripts";
            }

            @Override
            public String getDescription() {
                return "Number of execution scripts to be generated. The number of execution scripts should be greater or equal to 0.";
            }

            @Override
            protected ValidationResults validate(Integer value, ValidationResults results) {
                return results;
            }
        }

        /**
         * Configuration field.
         */
        public static class NO_GRAPHS extends TYPES.Int {

            public NO_GRAPHS() {
                super(Range.of(0, Integer.MAX_VALUE));
            }

            @Override
            public String getTagName() {
                return "number_of_generated_graphs";
            }

            @Override
            public String getLabel() {
                return "Number of generated graphs";
            }

            @Override
            public String getDescription() {
                return "Number of generated graphs. The number of generated graphs should be greater or equal to 0.";
            }

            @Override
            protected ValidationResults validate(Integer value, ValidationResults results) {
                return results;
            }
        }

        /**
         * Configuration field.
         */
        public static class NO_CWL extends TYPES.Int {
            public NO_CWL() {
                super(Range.of(0, Integer.MAX_VALUE));
            }

            @Override
            public APEConfigDefaultValue<Integer> getDefault() {
                return APEConfigDefaultValue.withDefault(0);
            }

            @Override
            public String getTagName() {
                return "number_of_cwl_files";
            }

            @Override
            public String getLabel() {
                return "Number of CWL files";
            }

            @Override
            public String getDescription() {
                return "The number of CWL representations of solutions should be generated.";
            }

            @Override
            protected ValidationResults validate(Integer value, ValidationResults results) {
                return results;
            }
        }

        /**
         * Configuration field.
         */
        public static class NO_SNAKEMAKE extends TYPES.Int {
            public NO_SNAKEMAKE() {
                super(Range.of(0, Integer.MAX_VALUE));
            }

            @Override
            public APEConfigDefaultValue<Integer> getDefault() {
                return APEConfigDefaultValue.withDefault(0);
            }

            @Override
            public String getTagName() {
                return "number_of_snakemake_files";
            }

            @Override
            public String getLabel() {
                return "Number of Snakemake files";
            }

            @Override
            public String getDescription() {
                return "The number of Snakemake representations of solutions that should be generated.";
            }

            @Override
            protected ValidationResults validate(Integer value, ValidationResults results) {
                return results;
            }
        }

        /**
         * Configuration field.
         */
        public static class TIMEOUT_SEC extends TYPES.Int {

            public TIMEOUT_SEC() {
                super(Range.of(0, Integer.MAX_VALUE));
            }

            @Override
            public String getTagName() {
                return "timeout_sec";
            }

            @Override
            public String getLabel() {
                return "Timeout (in sec)";
            }

            @Override
            public String getDescription() {
                return "Timeout in seconds. The timeout should be greater or equal to 0.";
            }

            @Override
            protected ValidationResults validate(Integer value, ValidationResults results) {
                return results;
            }

            @Override
            public APEConfigDefaultValue<Integer> getDefault() {
                return APEConfigDefaultValue.withDefault(300);
            }
        }

        /**
         * Configuration field.
         */
        public static class USE_WORKFLOW_INPUT extends TYPES.Option<ConfigEnum> {

            @Override
            public Class<ConfigEnum> getEnumClass() {
                return ConfigEnum.class;
            }

            @Override
            public String getTagName() {
                return "use_workflow_input";
            }

            @Override
            public String getLabel() {
                return "Use workflow input";
            }

            @Override
            public String getDescription() {
                return "Tag to indicate whether the workflow input should always be used.";
            }

            @Override
            public APEConfigDefaultValue<ConfigEnum> getDefault() {
                return APEConfigDefaultValue.withDefault(ConfigEnum.ALL);
            }
        }

        /**
         * Configuration field.
         */
        public static class USE_ALL_GENERATED_DATA extends TYPES.Option<ConfigEnum> {

            @Override
            public Class<ConfigEnum> getEnumClass() {
                return ConfigEnum.class;
            }

            @Override
            public String getTagName() {
                return "use_all_generated_data";
            }

            @Override
            public String getLabel() {
                return "Use all generated data";
            }

            @Override
            public String getDescription() {
                return "Tag to indicate whether all generated data outputs per tool should be used.";
            }

            @Override
            public APEConfigDefaultValue<ConfigEnum> getDefault() {
                return APEConfigDefaultValue.withDefault(ConfigEnum.ONE);
            }
        }

        /**
         * Configuration field.
         */
        public static class DEBUG_MODE extends TYPES.Bool {

            @Override
            public String getTagName() {
                return "debug_mode";
            }

            @Override
            public String getLabel() {
                return "Use debug mode";
            }

            @Override
            public String getDescription() {
                return "Tag to indicate whether the debug mode should be activated.";
            }

            @Override
            public APEConfigDefaultValue<Boolean> getDefault() {
                return APEConfigDefaultValue.withDefault(false);
            }
        }

        /**
         * Configuration field.
         */
        public static class TOOL_SEQ_REPEAT extends TYPES.Bool {

            @Override
            public String getTagName() {
                return "tool_seq_repeat";
            }

            @Override
            public String getLabel() {
                return "Tool sequence repeat";
            }

            @Override
            public String getDescription() {
                return "Tag to indicate whether the tool sequence repetition is allowed.";
            }

            @Override
            public APEConfigDefaultValue<Boolean> getDefault() {
                return APEConfigDefaultValue.withDefault(true);
            }
        }
    }
}
