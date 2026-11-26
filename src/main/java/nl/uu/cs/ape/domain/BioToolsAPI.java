package nl.uu.cs.ape.domain;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.uu.cs.ape.utils.APEFiles;
import nl.uu.cs.ape.utils.APEUtils;
import nl.uu.cs.ape.utils.cwl_parser.CWLParser;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * The {@code BioToolsAPI} class provides methods for fetching tool annotations
 * from bio.tools API.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class BioToolsAPI {

	/** Http-Client */
	private static final OkHttpClient client = new OkHttpClient();

	/** Base URL of the bio.tools API. */
	private static final HttpUrl BIO_TOOLS_API = HttpUrl.get("https://bio.tools/api/");

	/** URL of the bio.tools tool list endpoint. */
	private static final HttpUrl BIO_TOOLS_TOOL_LIST = HttpUrl.get("https://bio.tools/api/t");

	/**
	 * Allowed bio.tools IDs: letters, digits, '_', '-' and '.' (see the
	 * biotoolsSchema), but not only dots, which would change the URL path.
	 */
	private static final Pattern BIO_TOOLS_ID_PATTERN = Pattern.compile("(?!\\.+$)[A-Za-z0-9_.\\-]+");

	/**
	 * Retrieve the list of tools from the bio.tools API and convert it to the
	 * format used by the APE.
	 * The list of tools (biotoolsIDs) is read from a file, the JSON annotations are
	 * retrieved using bio.tools API
	 * and the result converted to APE annotation format and returned as a
	 * JSONObject.
	 * 
	 * @param listFile The file containing the list of biotoolsIDs as a JSON array.
	 * @return The JSONObject with the tool annotations in the APE format.
	 * @throws IOException If the file cannot be read or written.
	 */
	public static JSONObject getAndConvertToolList(File listFile) throws IOException {

		JSONArray toolList = APEFiles.readFileToJSONArray(listFile);
		List<String> biotoolsIDs = APEUtils.getListFromJSONArray(toolList, String.class);

		return getAndConvertToolList(biotoolsIDs);
	}

	/**
	 * Retrieve (using GET request) the list of tool annotations from the bio.tools
	 * API and convert it to the
	 * format used by the APE.
	 * 
	 * @param biotoolsIDs The list of bio.tools IDs.
	 * @return The JSONObject with the tool annotations in the APE format.
	 * @throws IOException If the file cannot be read or written.
	 */
	public static JSONObject getAndConvertToolList(List<String> biotoolsIDs) throws IOException {

		JSONArray bioToolsRAW = getToolListFromBioTools(biotoolsIDs);
		return convertBioTools2Ape(bioToolsRAW, false);

	}

	/**
	 * Fetch the list of all the tools from the bio.tools API and save them to a
	 * file in a format that can be used by the APE library.
	 * 
	 * @param destinationFilePath The path to the file where the tool annotations
	 *                            will be saved.
	 * @throws IOException If an error occurs while fetching the tools.
	 */
	public static void getAndSaveFullBioTools(String destinationFilePath) throws IOException {

		// Fetch the Limited (predefined) set of tool
		JSONObject biotools = BioToolsAPI.getAndConvertToolsFromEDAMTopic("", true);
		APEFiles.write2file(biotools.toString(4), new File(destinationFilePath), false);
	}

	/**
	 * Send Get request to get tool annotations for each elements in JSONArray from
	 * bio.tools API. It writes the result to a JSONArray.
	 * 
	 * @param domainName           Path to the file with the list of tools.
	 * @param excludeBadAnnotation If set to {@code true}, the method will exclude
	 *                             tools that do not have both the input and the
	 *                             output fully specified, i.e., with data and
	 *                             format types and formats specified.
	 * @return JSONArray with the tool annotations.
	 * @throws IOException If the file cannot be read or written.
	 */
	public static JSONObject getToolsFromDomain(String domainName, boolean excludeBadAnnotation) throws IOException {
		JSONArray toolAnnotations = null;
		HttpUrl.Builder url = BIO_TOOLS_TOOL_LIST.newBuilder();
		if (!domainName.isEmpty()) {
			url.addQueryParameter("domain", domainName);
		}
		toolAnnotations = fetchToolsFromURI(url.build());
		return convertBioTools2Ape(toolAnnotations, excludeBadAnnotation);
	}

	/**
	 * Send GET request to get tool annotations for the given topic from bio.tools.
	 * 
	 * @param topicName            The name of the topic.
	 * @param excludeBadAnnotation If set to {@code true}, the method will exclude
	 *                             tools that do not have both the input and the
	 *                             output fully specified, i.e., with data and
	 *                             format types and formats specified.
	 * @return The JSONObject containing the tool annotations in the APE format.
	 * @throws IOException If an error occurs while fetching the tools.
	 */
	public static JSONObject getAndConvertToolsFromEDAMTopic(String topicName, boolean excludeBadAnnotation)
			throws IOException {
		JSONArray toolAnnotations = null;
		HttpUrl.Builder url = BIO_TOOLS_TOOL_LIST.newBuilder();
		if (!topicName.isEmpty()) {
			url.addQueryParameter("topicID", "\"" + topicName + "\"");
		}
		toolAnnotations = fetchToolsFromURI(url.build());

		return convertBioTools2Ape(toolAnnotations, excludeBadAnnotation);
	}

	/**
	 * Send GET request to get tool annotations for the given list of bio.tools IDs.
	 * The
	 * result is saved to a JSONArray.
	 * 
	 * @param biotoolsIDList The list of bio.tools IDs.
	 * @return The JSONArray with the tool annotations as provided by bio.tools API.
	 * @throws IOException If an error occurs while fetching the tools.
	 */
	public static JSONArray getToolListFromBioTools(List<String> biotoolsIDList) throws IOException {
		JSONArray bioToolAnnotations = new JSONArray();
		for (String biotoolsID : biotoolsIDList) {
			JSONObject toolJson = fetchToolFromBioTools(biotoolsID);
			bioToolAnnotations.put(toolJson);
		}
		log.debug("The list of tools successfully fetched from bio.tools.");
		return bioToolAnnotations;
	}

	/**
	 * Send Get request to get tool annotations for a given tool from bio.tools API.
	 * 
	 * @param biotoolsID The ID of the tool.
	 * @return The JSONObject with the tool annotations as provided by bio.tools
	 *         API.
	 * @throws IOException   If an error occurs while fetching the tool.
	 * @throws JSONException If the JSON returned by the bio.tools API is not well
	 *                       formatted.
	 */
	public static JSONObject fetchToolFromBioTools(String biotoolsID) throws IOException, JSONException {

		if (biotoolsID == null || !BIO_TOOLS_ID_PATTERN.matcher(biotoolsID).matches()) {
			throw new IOException("Invalid bio.tools ID '" + biotoolsID
					+ "'. IDs can only contain letters, digits, '_', '-' and '.'.");
		}
		HttpUrl url = BIO_TOOLS_API.newBuilder()
				.addPathSegment(biotoolsID)
				.addQueryParameter("format", "json")
				.build();
		Request request = new Request.Builder().url(url).build();
		try (Response response = client.newCall(request).execute()) {
			if (!response.isSuccessful()) {
				log.error("The tool " + biotoolsID + " could not be fetched from bio.tools.");
			}
			// Get response body
			return new JSONObject(response.body().string());
		}
	}

	/**
	 * Send Get request to get tool annotations Saves JSONArray with all the tool
	 * annotations (in tool list)
	 * 
	 * @return
	 * @throws IOException
	 * @throws JSONException
	 */
	private static JSONArray fetchToolsFromURI(HttpUrl url) throws JSONException, IOException {
		JSONArray bioToolAnnotations = new JSONArray();
		log.info("Fetching tools from bio.tools: " + url);
		String next = "";
		int i = 1;
		while (next != null) {
			HttpUrl.Builder pageUrl = url.newBuilder().setQueryParameter("format", "json");
			/* 'next' is the query of the next page (e.g. "?page=2"), keep the original filters. */
			HttpUrl nextQuery = HttpUrl.parse("https://bio.tools/" + next);
			if (nextQuery != null) {
				for (String name : nextQuery.queryParameterNames()) {
					pageUrl.setQueryParameter(name, nextQuery.queryParameter(name));
				}
			}
			Request request = new Request.Builder().url(pageUrl.build()).build();
			try (Response response = client.newCall(request).execute()) {
				if (!response.isSuccessful())
					throw new IOException("Unexpected code when trying to fetch" + response);
				// Get response body
				JSONObject responseJson = new JSONObject(response.body().string());
				JSONArray toolListJson = responseJson.getJSONArray("list");
				for (int j = 0; j < toolListJson.length(); j++) {
					JSONObject tool = toolListJson.getJSONObject(j);
					bioToolAnnotations.put(tool);
				}
				try {
					next = responseJson.getString("next");
				} catch (JSONException e) {
					next = null;
				}
			}
			log.info("bio.tools: page " + i++ + " fetched.");

		}
		log.info("All tools fetched from a given URL.");
		return bioToolAnnotations;
	}

	/**
	 * Method converts tools annotated using 'bio.tools' standard (see <a href=
	 * "https://biotools.readthedocs.io/en/latest/api_usage_guide.html">bio.tools
	 * API</a>), into standard supported by the APE library. It is a strict
	 * conversion, where only tools that have inputs and outputs types and formats
	 * are accepted.
	 * <p>
	 * In practice, the method takes a {@link JSONArray} as an argument, where each
	 * {@link JSONObject} in the array represents a tool annotated using 'bio.tools'
	 * standard, and returns a {@link JSONObject} that represents tool annotations
	 * that can be used by the APE library.
	 *
	 * @param bioToolsAnnotation   A {@link JSONArray} object, that contains list of
	 *                             annotated tools ({@link JSONObject}s) according
	 *                             the
	 *                             bio.tools specification (see <a href=
	 *                             "https://biotools.readthedocs.io/en/latest/api_usage_guide.html">bio.tools
	 *                             API</a>)
	 * @param excludeBadAnnotation If set to {@code true}, the method will exclude
	 *                             tools
	 *                             that do not have both the input and the output
	 *                             fully specified, i.e., with data and format types
	 *                             and formats specified. If set to {@code false},
	 *                             the method will return annotations that have at
	 *                             least one of the two fully specified (at least
	 *                             one input or or output).
	 * @return {@link JSONObject} that represents the tool annotation supported by
	 *         the APE library.
	 * @throws JSONException the json exception
	 */
	public static JSONObject convertBioTools2Ape(JSONArray bioToolsAnnotation, boolean excludeBadAnnotation)
			throws JSONException {

		int notAcceptedOperations = 0;

		int bioToolFunctions = 0;

		JSONArray apeToolsAnnotations = new JSONArray();

		for (JSONObject bioJsonTool : APEUtils.getJSONListFromJSONArray(bioToolsAnnotation)) {

			String toolName = bioJsonTool.getString("name");
			String biotoolsID = bioJsonTool.getString("biotoolsID");

			List<JSONObject> functions = APEUtils.getJSONListFromJson(bioJsonTool, "function");
			if (functions.isEmpty()) {
				continue;
			}
			int functionNo = 1;

			for (JSONObject function : functions) {
				String toolID = biotoolsID +
						(functions.size() > 1 ? "_op" + (functionNo) : "");

				Optional<JSONObject> apeToolJson = convertSingleBioTool2Ape(toolName, toolID, biotoolsID, function,
						excludeBadAnnotation);
				if (apeToolJson.isPresent()) {
					JSONObject implementation = new JSONObject().put("cwl_reference", "PATH_TO_CWL_FILE.cwl");
					apeToolJson.get().put("implementation", implementation);
					apeToolsAnnotations.put(apeToolJson.get());
					bioToolFunctions++;
					functionNo++;
				} else {
					notAcceptedOperations++;
				}
			}
		}
		log.info("Provided bio.tools: " + bioToolsAnnotation.length());
		log.info("Total bio.tools functions: " + bioToolFunctions);
		log.info("Errored bio.tools functions: " + notAcceptedOperations);
		log.info("Created APE annotations: " + apeToolsAnnotations.length());

		return new JSONObject().put("functions", apeToolsAnnotations);
	}

	/**
	 * Convert a single function from bio.tools schema to an APE tool.
	 * 
	 * @param toolName             The name of the tool.
	 * @param biotoolsID           The ID of the tool.
	 * @param function             The function (see `function` under bio.tools
	 *                             schema) in JSON format.
	 * @param excludeBadAnnotation If set to {@code true}, the method will exclude
	 *                             tools that do not have both the input and the
	 *                             output fully specified, i.e., with data and
	 *                             format types and formats specified.
	 * @return The JSONObject with the tool annotations for a single tool according
	 *         to the APE tool annotation format.
	 * @throws JSONException If the JSON is not well formatted.
	 */
	public static Optional<JSONObject> convertSingleBioTool2Ape(String toolName, String toolID, String biotoolsID,
			JSONObject function, boolean excludeBadAnnotation)
			throws JSONException {
		JSONObject apeJsonTool = new JSONObject();
		apeJsonTool.put("label", toolName);
		apeJsonTool.put("id", toolID);
		apeJsonTool.put("biotoolsID", biotoolsID);

		JSONArray apeTaxonomyTerms = new JSONArray();

		JSONArray operations = function.getJSONArray("operation");
		for (JSONObject bioOperation : APEUtils.getJSONListFromJSONArray(operations)) {
			apeTaxonomyTerms.put(bioOperation.get("uri"));
		}
		apeJsonTool.put("taxonomyOperations", apeTaxonomyTerms);
		// reading inputs
		JSONArray apeInputs = new JSONArray();
		try {
			apeInputs = calculateBioToolsInputOutput(function.getJSONArray("input"),
					toolName);
			apeJsonTool.put("inputs", apeInputs);
		} catch (BioToolsAnnotationException e) {
			if (excludeBadAnnotation) {
				return Optional.empty();
			}
		}
		JSONArray apeOutputs = new JSONArray();
		try {
			apeOutputs = calculateBioToolsInputOutput(function.getJSONArray("output"),
					toolName);
			apeJsonTool.put("outputs", apeOutputs);
		} catch (BioToolsAnnotationException e) {
			if (excludeBadAnnotation) {
				return Optional.empty();
			}
		}
		if (!excludeBadAnnotation ||
				(apeInputs.length() > 0 && apeOutputs.length() > 0)) {
			return Optional.of(apeJsonTool);
		}
		return Optional.empty();
	}

	/**
	 * Method converts input and output tool annotations, following bio.tools
	 * schema,
	 * into the APE library tool annotation format.
	 * 
	 * @param bioInputs JSONArray with the bio.tools input/output annotations.
	 * @param toolID    ID of the tool that is being converted.
	 * 
	 * @return JSONArray with the APE library input/output annotations.
	 * @throws BioToolsAnnotationException If the input/output annotations are not
	 *                                     well defined.
	 */
	private static JSONArray calculateBioToolsInputOutput(JSONArray bioInputs, String toolID)
			throws BioToolsAnnotationException {
		JSONArray apeInputs = new JSONArray();
		for (JSONObject bioInput : APEUtils.getJSONListFromJSONArray(bioInputs)) {
			JSONObject apeInput = new JSONObject();
			JSONArray apeInputTypes = new JSONArray();
			JSONArray apeInputFormats = new JSONArray();
			// add all data types
			for (JSONObject bioType : APEUtils.getJSONListFromJson(bioInput, "data")) {
				apeInputTypes.put(bioType.getString("uri"));
			}
			if (apeInputTypes.length() == 0) {
				throw BioToolsAnnotationException.notExistingType(toolID);
			}
			apeInput.put(CWLParser.DATA_ROOT, apeInputTypes);
			// add all data formats (or just the first one)
			for (JSONObject bioType : APEUtils.getJSONListFromJson(bioInput, "format")) {
				apeInputFormats.put(bioType.getString("uri"));
			}
			if (apeInputFormats.length() == 0) {
				throw BioToolsAnnotationException.notExistingFormat(toolID);
			}
			apeInput.put(CWLParser.FORMAT_ROOT, apeInputFormats);

			apeInputs.put(apeInput);

		}
		return apeInputs;
	}

}