package nl.uu.cs.ape.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.commonwl.cwlsdk.cwl1_2.CommandInputParameter;
import org.commonwl.cwlsdk.cwl1_2.CommandInputParameter;
import org.commonwl.cwlsdk.cwl1_2.CommandOutputParameter;

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.uu.cs.ape.configuration.APECoreConfig;
import nl.uu.cs.ape.configuration.ToolAnnotationTag;
import nl.uu.cs.ape.constraints.ConstraintFactory;
import nl.uu.cs.ape.constraints.ConstraintFormatException;
import nl.uu.cs.ape.constraints.ConstraintTemplate;
import nl.uu.cs.ape.constraints.ConstraintTemplateParameter;
import nl.uu.cs.ape.utils.APEFiles;
import nl.uu.cs.ape.utils.APEUtils;
import nl.uu.cs.ape.utils.cwl_parser.CWLParser;
import nl.uu.cs.ape.models.AbstractModule;
import nl.uu.cs.ape.models.AllModules;
import nl.uu.cs.ape.models.AllTypes;
import nl.uu.cs.ape.models.AuxiliaryPredicate;
import nl.uu.cs.ape.models.ConstraintTemplateData;
import nl.uu.cs.ape.models.Module;
import nl.uu.cs.ape.models.SATAtomMappings;
import nl.uu.cs.ape.solver.minisat.CNFClauseBuffer;
import nl.uu.cs.ape.models.Type;
import nl.uu.cs.ape.models.enums.ToolAnnotationType;
import nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate;

/**
 * The {@code APEDomainSetup} class is used to store the domain information and
 * initial constraints that have to be encoded.
 *
 * @author Vedran Kasalica
 */
@Slf4j
public class APEDomainSetup {

    /* Helper objects used to keep track of the domain quality. */
    private Set<String> emptyTools = new HashSet<>();
    private Set<String> wrongToolIO = new HashSet<>();
    private Set<String> wrongToolTax = new HashSet<>();
    /**
     * All modules/operations used in the domain.
     */
    private AllModules allModules;

    /**
     * All data types defined in the domain.
     */
    private AllTypes allTypes;

    /**
     * Prefix used to define OWL class IDs
     */
    private String ontologyPrefixIRI;

    @Setter
    /**
     * Object used to write locally CNF SAT problem specification (in human readable
     * format).
     */
    private String writeLocalCNF = null;

    /**
     * Object used to create temporal constraints.
     */
    private ConstraintFactory constraintFactory = new ConstraintFactory();

    /**
     * List of data gathered from the constraint file.
     */
    private List<ConstraintTemplateData> unformattedConstr = new ArrayList<>();
    private List<AuxiliaryPredicate> helperPredicates = new ArrayList<>();
    private List<String> constraintsSLTLx = new ArrayList<>();

    /**
     * Maximum number of inputs that a tool can have.
     */
    private int maxNoToolInputs = 0;

    /**
     * Maximum number of outputs that a tool can have.
     */
    private int maxNoToolOutputs = 0;

    /**
     * Holds information whether the domain was annotated under the strict rules of
     * the output dependency.
     */
    private boolean useStrictToolAnnotations;

    private static final String CONSTR_JSON_TAG = "constraints";
    private static final String CONSTR_ID_TAG = "constraintid";
    private static final String CONSTR_SLTLx = "formula";
    private static final String CONSTR_PARAM_JSON_TAG = "parameters";
    private static final String TOOLS_JSON_TAG = "functions";

    /**
     * Instantiates a new Ape domain setup.
     *
     * @param config the config
     */
    public APEDomainSetup(APECoreConfig config) {
        this.allModules = new AllModules(config);
        this.allTypes = new AllTypes(config);
        this.ontologyPrefixIRI = config.getOntologyPrefixIRI();
        this.useStrictToolAnnotations = config.getUseStrictToolAnnotations();
    }

    /**
     * Gets all modules.
     *
     * @return The field {@link #allModules}.
     */
    public AllModules getAllModules() {
        return allModules;
    }

    /**
     * Add constraint data.
     *
     * @param constr Add a constraint to the list of constraints, that should be
     *               encoded during the execution of the synthesis.
     */
    public void addConstraintData(ConstraintTemplateData constr) {
        this.unformattedConstr.add(constr);
    }

    /**
     * Add the String that corresponds to an SLTLx formula that should be parsed to
     * the list of constraints.
     * 
     * @param formulaSLTLx String that corresponds to an SLTLx formula that should
     *                     be parsed
     */
    public void addSLTLxConstraint(String formulaSLTLx) {
        this.constraintsSLTLx.add(formulaSLTLx);
    }

    /**
     * Gets unformatted constraints.
     *
     * @return the field {@link #unformattedConstr}.
     */
    public List<ConstraintTemplateData> getUnformattedConstr() {
        return unformattedConstr;
    }

    /**
     * Gets all SLTLx constraints specified by the user in SLTLx as text.
     * 
     * @return Set of string representations of the constraints.
     */
    public List<String> getSLTLxConstraints() {
        return constraintsSLTLx;
    }

    /**
     * Removes all of the unformatted constraints, in order to start a new synthesis
     * run.
     */
    public void clearConstraints() {
        this.unformattedConstr.clear();
        this.constraintsSLTLx.clear();
    }

    /**
     * Gets all types.
     *
     * @return the field {@link #allTypes}.
     */
    public AllTypes getAllTypes() {
        return allTypes;
    }

    /**
     * Gets constraint factory.
     *
     * @return the field {@link #constraintFactory}.
     */
    public ConstraintFactory getConstraintFactory() {
        return constraintFactory;
    }

    /**
     * Adding each constraint format in the set of all cons. formats. method
     * should be called only once all the data types and modules have been
     * initialized.
     */
    public void initializeConstraints() {
        constraintFactory.initializeConstraints(allModules, allTypes);
    }

    /**
     * Trim taxonomy boolean.
     *
     * @return the boolean
     */
    public boolean trimTaxonomy() {
        boolean succRun = true;

        succRun &= allModules.trimTaxonomy();
        succRun &= allTypes.trimTaxonomy();
        return succRun;
    }

    /**
     * Return the {@link ConstraintTemplate} that corresponds to the given ID, or
     * null if the constraint with the given ID does not exist.
     *
     * @param constraintID ID of the {@code ConstraintTemplate}.
     * @return The {@code ConstraintTemplate} that corresponds to the given ID, or
     *         null if the ID is not mapped to any constraint.
     */
    public ConstraintTemplate getConstraintTemplate(String constraintID) {
        return constraintFactory.getConstraintTemplate(constraintID);
    }

    /**
     * Method reads the constraints from a JSON object and updates the
     * {@link APEDomainSetup} object accordingly.
     *
     * @param constraintsJSONArray JSON array containing the constraints
     * @throws ConstraintFormatException exception in case of bad constraint json
     *                                   formatting
     */
    public void updateConstraints(JSONArray constraintsJSONArray) throws ConstraintFormatException {
        if (constraintsJSONArray == null) {
            return;
        }
        String constraintID = null;
        int currNode = 0;

        List<JSONObject> constraints = APEUtils.getJSONListFromJSONArray(constraintsJSONArray);

        /* Iterate through each constraint in the list */
        for (JSONObject jsonConstraint : APEUtils.safe(constraints)) {
            currNode++;
            /* READ THE CONSTRAINT */
            try {
                constraintID = jsonConstraint.getString(CONSTR_ID_TAG);
                ConstraintTemplate currConstrTemplate = getConstraintFactory()
                        .getConstraintTemplate(constraintID);
                if (currConstrTemplate == null) {
                    if (constraintID.equals("SLTLx")) {
                        String formulaSLTLx = jsonConstraint.getString(CONSTR_SLTLx);
                        if (formulaSLTLx == null) {
                            throw ConstraintFormatException.wrongNumberOfParameters(
                                    getConstrErrorMsg(currNode, constraintID));
                        }
                        this.addSLTLxConstraint(formulaSLTLx);
                        continue;
                    } else {
                        throw ConstraintFormatException.wrongConstraintID(
                                getConstrErrorMsg(currNode, constraintID));
                    }
                }

                List<ConstraintTemplateParameter> currTemplateParameters = currConstrTemplate.getParameters();

                List<JSONObject> jsonConstParam = APEUtils.getListFromJson(jsonConstraint, CONSTR_PARAM_JSON_TAG,
                        JSONObject.class);
                if (currTemplateParameters.size() != jsonConstParam.size()) {
                    throw ConstraintFormatException.wrongNumberOfParameters(
                            getConstrErrorMsg(currNode, constraintID));
                }
                int paramNo = 0;
                List<TaxonomyPredicate> constraintParameters = new ArrayList<>();
                /* for each constraint parameter */
                for (JSONObject jsonParam : jsonConstParam) {
                    ConstraintTemplateParameter taxInstanceFromJson = currTemplateParameters.get(paramNo++);
                    TaxonomyPredicate currParameter = taxInstanceFromJson.readConstraintParameterFromJson(jsonParam,
                            this);
                    constraintParameters.add(currParameter);
                }

                ConstraintTemplateData currConstr = getConstraintFactory()
                        .generateConstraintTemplateData(constraintID, constraintParameters);
                if (constraintParameters.stream().anyMatch(Objects::isNull)) {
                    throw ConstraintFormatException.wrongParameter(
                            getConstrErrorMsg(currNode, constraintID));
                } else {
                    this.addConstraintData(currConstr);
                }

            } catch (JSONException e) {
                throw ConstraintFormatException.badFormat(
                        getConstrErrorMsg(currNode, constraintID));
            }

        }
    }

    private String getConstrErrorMsg(int currNode, String constraintID) {
        return String.format("Error at constraint no: %d, constraint ID: %s", currNode, constraintID);
    }

    /**
     * Annotate {@link #allModules} using the I/O DataInstance from
     * the @toolAnnotationsFile.
     * The existing modules (in {@link #allModules}) are updated, and new modules
     * are added to the list of modules.
     * Return true if the domain was updated, false otherwise.
     *
     * @param toolAnnotationsFile JSON file containing tool annotations.
     * @return {@code true} if the domain was updated, {@code false} otherwise.
     * @throws IOException   Error in handling a JSON file containing tool
     *                       annotations.
     * @throws JSONException Error if the tool annotation JSON file, bad format
     */
    public boolean annotateToolFromJson(JSONObject toolAnnotationsFile) throws IOException, JSONException {
        int currModule = 0;
        for (JSONObject jsonModule : APEUtils
                .safe(APEUtils.getListFromJson(toolAnnotationsFile, TOOLS_JSON_TAG, JSONObject.class))) {
            currModule++;
            updateModuleFromJson(jsonModule);
        }
        if (currModule == 0) {
            log.warn("No tools were annotated in the current domain.");
            return false;
        }
        return true;
    }

    /**
     * Parse the tool annotation from a JSON file and update the module in the
     * domain ({@link AllModules}) accordingly.
     * 
     * @param jsonModule JSON annotation of a module/tool
     * @return {@code true} if the domain was updated, false otherwise.
     * @throws JSONException Error if the JSON file was not properly formatted.
     * @throws APEDimensionsException
     * @throws IOException
     */
    public Optional<Module> updateModuleFromJson(JSONObject jsonModule)
            throws JSONException, APEDimensionsException, IOException {

        ToolAnnotationType annotationType;
        try {
            annotationType = ToolAnnotationType
                    .fromString(jsonModule.getString(ToolAnnotationTag.TYPE.toString()));
        } catch (JSONException e) {
            log.debug("Tool annotation type not specified. Defaulting to APE annotation.");
            return updateModuleFromJsonAPE(jsonModule);
        }

        switch (annotationType) {
            case APE_ANNOTATION:
                return updateModuleFromJsonAPE(jsonModule);
            case CWL_ANNOTATION:
                String cwlURL = jsonModule.getString(ToolAnnotationTag.CWL_REFERENCE.toString());
                return updateModuleFromCWL(cwlURL);
            default:
                log.warn("Tool annotation format not specified. Using default APE annotations.");
                return updateModuleFromJsonAPE(jsonModule);
        }
    }

    /**
     * Parse the tool annotation from a CWL file and update the module in the
     * domain ({@link AllModules}) accordingly. The annotations are expected to
     * follow EDAM ontology.
     * 
     * @param cwlFileLocation path to the CWL file (URL or local file) containing
     *                        the tool annotations
     * @return {@code true} if the domain was updated, false otherwise.
     * @throws IOException Error in accessing or parsing the CWL file.
     */
    public Optional<Module> updateModuleFromCWL(String cwlFileLocation) throws IOException {

        // Initialize CWL parser
        CWLParser cwlParser = new CWLParser(cwlFileLocation);

        // Extract the module's label and ID
        String moduleLabel = cwlParser.getLabel();
        String moduleIRI = APEUtils.createClassIRI(moduleLabel, ontologyPrefixIRI);

        if (allModules.get(moduleIRI) != null) {
            moduleIRI = moduleIRI + "[tool]";
        }

        // Extract taxonomy operations
        Set<String> taxonomyOperations = new HashSet<>(cwlParser.getOperations());
        Set<String> taxonomyParentModules = APEUtils.createIRIsFromLabels(taxonomyOperations, ontologyPrefixIRI);

        // Validate taxonomy parent modules
        List<String> toRemove = new ArrayList<>();
        for (String parentModule : taxonomyParentModules) {
            String parentModuleIRI = APEUtils.createClassIRI(parentModule, ontologyPrefixIRI);
            if (allModules.get(parentModuleIRI) == null) {
                log.debug("Tool '" + moduleIRI + "' annotation issue. " +
                        "Referenced taxonomy operation: '" + parentModuleIRI + "' cannot be found.");
                wrongToolTax.add(moduleLabel);
                toRemove.add(parentModuleIRI);
            }
        }
        taxonomyParentModules.removeAll(toRemove);

        if (taxonomyParentModules.isEmpty()) {
            log.debug("Tool '" + moduleIRI + "' annotation issue. " +
                    "No valid taxonomy operations found. Using root module as fallback.");
            taxonomyParentModules.add(allModules.getRootModuleID());
        }

        /*
         * Set the inputs and outputs of the module. If the inputs and outputs are not
         * valid, the module is not added to the domain.
         */
        List<Type> inputs = new ArrayList<>();
        List<String> inputCWLKeys = new ArrayList<>();
        List<Type> outputs = new ArrayList<>();
        List<String> outputCWLKeys = new ArrayList<>();
        try {
            List<CommandInputParameter> inputsRaw = cwlParser.getInputs();
            for (CommandInputParameter inputRaw : inputsRaw) {
                if (inputRaw.getFormat() != null) {
                    Type instance = Type.taxonomyInstanceFromCWLInput(inputRaw, this);
                    if (instance != null) {
                        inputs.add(instance);
                        inputCWLKeys.add(inputRaw.getId().get());
                    }
                }
            }
            updateMaxNoToolInputs(inputs.size());

            List<CommandOutputParameter> outputsRaw = cwlParser.getOutputs();
            for (CommandOutputParameter outputRaw : outputsRaw) {
                if (outputRaw.getFormat() != null) {
                    Type instance = Type.taxonomyInstanceFromCWLOutput(outputRaw, this);
                    if (instance != null) {
                        outputs.add(instance);
                        outputCWLKeys.add(outputRaw.getId().get());
                    }
                }
            }
            updateMaxNoToolOutputs(outputs.size());

            if (inputs.isEmpty() && outputs.isEmpty()) {
                emptyTools.add(moduleLabel);
                log.debug("Operation '" + moduleLabel
                        + "' was not included as it has no (valid) inputs and outputs specified.");
                return Optional.empty();
            }

        } catch (APEDimensionsException badDimension) {
            wrongToolIO.add(moduleLabel);
            log.debug("Operation '" + moduleLabel + "' was not included. " + badDimension.getMessage());
            return Optional.empty();
        }

        /* Set the implementation cwl file reference of the module, if specified. */
        String cwlReference = cwlFileLocation;

        /*
         * Add the module and make it sub module of the currSuperModule (if it was not
         * previously defined)
         */
        Module currModule = (Module) allModules
                .addPredicate(
                        new Module(moduleLabel, moduleIRI, allModules.getRootModuleID(), cwlReference, null));

        /* For each parent module add the current module as a subset and vice versa. */
        for (String parentModuleID : taxonomyParentModules) {
            AbstractModule parentModule = allModules.get(parentModuleID);
            if (parentModule != null) {
                parentModule.addSubPredicate(currModule);
                currModule.addParentPredicate(parentModule);
            }
        }

        currModule.setModuleInput(inputs);
        currModule.setModuleCWLInputKeys(inputCWLKeys);
        currModule.setModuleOutput(outputs);
        currModule.setModuleCWLOutputKeys(outputCWLKeys);
        currModule.setAsRelevantTaxonomyTerm(allModules);

        return Optional.of(currModule);
    }


    /**
     * Parse the tool annotation from a JSON file and update the module in the
     * domain ({@link AllModules}) accordingly.
     * 
     * @param jsonModule JSON annotation of a module/tool
     * @return {@code true} if the domain was updated, false otherwise.
     * @throws JSONException Error if the JSON file was not properly formatted.
     */
    public Optional<Module> updateModuleFromJsonAPE(JSONObject jsonModule)
            throws JSONException, APEDimensionsException {

        String moduleIRI = APEUtils.createClassIRI(jsonModule.getString(ToolAnnotationTag.ID.toString()),
                ontologyPrefixIRI);
        if (allModules.get(moduleIRI) != null) {
            moduleIRI = moduleIRI + "[tool]";
        }
        String moduleLabel = jsonModule.getString(ToolAnnotationTag.LABEL.toString());
        Set<String> taxonomyParentModules = new HashSet<>(
                APEUtils.getListFromJson(jsonModule, ToolAnnotationTag.TAXONOMY_OPERATIONS.toString(), String.class));
        taxonomyParentModules = APEUtils.createIRIsFromLabels(taxonomyParentModules, ontologyPrefixIRI);

        /* Check if the referenced module taxonomy classes exist. */
        List<String> toRemove = new ArrayList<>();
        for (String parentModule : taxonomyParentModules) {
            String parentModuleIRI = APEUtils.createClassIRI(parentModule, ontologyPrefixIRI);
            if (allModules.get(parentModuleIRI) == null) {
                log.debug("Tool '" + moduleIRI + "' annotation issue. "
                        + "Referenced '" + ToolAnnotationTag.TAXONOMY_OPERATIONS.toString() + "': '" + parentModuleIRI
                        + "' cannot be found in the Tool Taxonomy.");
                wrongToolTax.add(moduleLabel);
                toRemove.add(parentModuleIRI);
            }
        }
        taxonomyParentModules.removeAll(toRemove);

        /*
         * If the taxonomy terms were not properly specified the tool taxonomy root is
         * used as a parent class of the tool.
         */
        if (taxonomyParentModules.isEmpty()) {
            log.debug("Tool '" + moduleIRI + "' annotation issue. "
                    + "None of the referenced '" + ToolAnnotationTag.TAXONOMY_OPERATIONS.toString()
                    + "' can be found in the Tool Taxonomy.");
            taxonomyParentModules.add(allModules.getRootModuleID());
        }

        /*
         * Set the inputs and outputs of the module. If the inputs and outputs are not
         * valid, the module is not added to the domain.
         */
        List<Type> inputs;
        List<Type> outputs;
        try {
            inputs = getToolInputsFromAnnotation(jsonModule);
            updateMaxNoToolInputs(inputs.size());

            outputs = getToolOutputsFromAnnotation(jsonModule);
            updateMaxNoToolOutputs(outputs.size());

            if (inputs.isEmpty() && outputs.isEmpty()) {
                emptyTools.add(moduleLabel);
                log.debug("Operation '" + moduleLabel
                        + "' was not included as it has no (valid) inputs and outputs specified.");
                return Optional.empty();
            }

        } catch (APEDimensionsException badDimension) {
            wrongToolIO.add(moduleLabel);
            log.debug("Operation '" + moduleLabel + "' was not included. " + badDimension.getMessage());
            return Optional.empty();
        }

        /* Set the implementation cwl file reference of the module, if specified. */
        String cwlReference = getModuleImplementationFromAnnotation(jsonModule, ToolAnnotationTag.CWL_REFERENCE);

        /* Set the implementation code of the module, if specified. */
        String executionCode = getModuleImplementationFromAnnotation(jsonModule, ToolAnnotationTag.CODE);

        /*
         * Add the module and make it sub module of the currSuperModule (if it was not
         * previously defined)
         */
        Module currModule = (Module) allModules
                .addPredicate(
                        new Module(moduleLabel, moduleIRI, allModules.getRootModuleID(), cwlReference, executionCode));

        /* For each parent module add the current module as a subset and vice versa. */
        for (String parentModuleID : taxonomyParentModules) {
            AbstractModule parentModule = allModules.get(parentModuleID);
            if (parentModule != null) {
                parentModule.addSubPredicate(currModule);
                currModule.addParentPredicate(parentModule);
            }
        }

        currModule.setModuleInput(inputs);
        currModule.setModuleOutput(outputs);
        currModule.setAsRelevantTaxonomyTerm(allModules);

        return Optional.of(currModule);
    }

    /**
     * Get tool inputs list from the tool annotation.
     *
     * @param jsonModule the json tool annotation
     * @return The list of inputs of the module.
     */
    private List<Type> getToolInputsFromAnnotation(JSONObject jsonModule) throws APEDimensionsException {
        /* Get the inputs of the module */
        List<JSONObject> jsonModuleInput = APEUtils.getListFromJson(jsonModule, ToolAnnotationTag.INPUTS.toString(),
                JSONObject.class);
        List<Type> inputs = new ArrayList<>();
        for (JSONObject jsonInput : jsonModuleInput) {
            if (!jsonInput.isEmpty()) {
                inputs.add(Type.taxonomyInstanceFromJson(jsonInput, this, false));
            }
        }
        return inputs;
    }

    /**
     * Get tool output list from the tool annotation.
     *
     * @param jsonModule the json tool annotation
     * @return The list of outputs of the module.
     */
    private List<Type> getToolOutputsFromAnnotation(JSONObject jsonModule) throws APEDimensionsException {
        /* Get the outputs of the module */
        List<JSONObject> jsonModuleOutput = APEUtils.getListFromJson(jsonModule, ToolAnnotationTag.OUTPUTS.toString(),
                JSONObject.class);
        List<Type> outputs = new ArrayList<>();
        for (JSONObject jsonOutput : jsonModuleOutput) {
            if (!jsonOutput.isEmpty()) {
                outputs.add(Type.taxonomyInstanceFromJson(jsonOutput, this, true));
            }
        }
        return outputs;
    }

    /**
     * Get the implementation code of the module, if specified.
     *
     * @param jsonToolAnnotation the json tool annotation
     * @param implementationType the implementation type
     *                           ({@link ToolAnnotationTag#CWL_REFERENCE} or
     *                           {@link ToolAnnotationTag#CODE})
     * @return The implementation code of the module, if specified.
     */
    private String getModuleImplementationFromAnnotation(JSONObject jsonToolAnnotation,
            ToolAnnotationTag implementationType) {
        try {
            JSONObject implementationJson = jsonToolAnnotation
                    .getJSONObject(ToolAnnotationTag.IMPLEMENTATION.toString());
            String implementation = implementationJson.getString(implementationType.toString());
            if (implementation.equals("")) {
                return null;
            }
            return implementation;
        } catch (JSONException e) {
            /* Do not annotate the execution code. */
            return null;
        }
    }

    /**
     * Gets ontology prefix IRI.
     *
     * @return the ontology prefix IRI
     */
    public String getOntologyPrefixIRI() {
        return ontologyPrefixIRI;
    }

    /**
     * Get the maximum number of inputs that a tool can have.
     *
     * @return the field {@link #maxNoToolInputs}.
     */
    public int getMaxNoToolInputs() {
        return maxNoToolInputs;
    }

    /**
     * Update the maximum number of inputs that a tool can have, i.e. increase the
     * number if the current max number is smaller than the new number of inputs.
     *
     * @param currNoInputs the number of inputs that a tool has
     */
    public void updateMaxNoToolInputs(int currNoInputs) {
        if (this.maxNoToolInputs < currNoInputs) {
            this.maxNoToolInputs = currNoInputs;
        }
    }

    /**
     * Get the maximum number of outputs that a tool can have.
     *
     * @return the field {@link #maxNoToolOutputs}.
     */
    public int getMaxNoToolOutputs() {
        return maxNoToolOutputs;
    }

    /**
     * Update the maximum number of outputs that a tool can have, i.e. increase the
     * number if the current max number is smaller than the new number of outputs.
     *
     * @param currNoOutputs the number of outputs that the current tool has
     */
    public void updateMaxNoToolOutputs(int currNoOutputs) {
        if (this.maxNoToolOutputs < currNoOutputs) {
            this.maxNoToolOutputs = currNoOutputs;
        }
    }

    /**
     * Add predicate to the list of auxiliary predicates that should be encoded.
     * 
     * @param helperPredicate
     */
    public void addHelperPredicate(AuxiliaryPredicate helperPredicate) {
        helperPredicates.add(helperPredicate);

    }

    /**
     * Get information whether the domain was annotated under the strict rules of
     * the output dependency.
     * 
     * @return {@code true} if the strict rules apply, {@code false} otherwise.
     */
    public boolean getUseStrictToolAnnotations() {
        return useStrictToolAnnotations;
    }

    /**
     * Get the list of helper predicates used in the domain.
     * 
     * @return List of the auxiliary helper predicates.
     */
    public List<AuxiliaryPredicate> getHelperPredicates() {
        return helperPredicates;
    }

    /**
     * Write locally the SAT (CNF) workflow specification in human readable format.
     * 
     * @param clauseBuffer In-memory CNF encoding of the SAT problem.
     * @param mappings     Mappings between the SAT problem and the domain.
     * @throws IOException Error in writing the file to the local file system.
     */
    public void localCNF(CNFClauseBuffer clauseBuffer, SATAtomMappings mappings) throws IOException {
        if (writeLocalCNF != null) {
            File satInputFile = File.createTempFile("satCNF", ".cnf");
            try {
                clauseBuffer.writeDimacs(satInputFile, mappings.getSize());
                try (FileInputStream cnfStream = new FileInputStream(satInputFile)) {
                    String encoding = APEUtils.convertCNF2humanReadable(cnfStream, mappings);
                    APEFiles.write2file(encoding, new File(writeLocalCNF), false);
                }
            } finally {
                Files.deleteIfExists(satInputFile.toPath());
            }
        }
    }

}
