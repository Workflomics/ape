package nl.uu.cs.ape.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import nl.uu.cs.ape.configuration.APERunConfig;
import nl.uu.cs.ape.domain.APEDomainSetup;
import nl.uu.cs.ape.models.ConstraintTemplateData;
import nl.uu.cs.ape.models.Module;
import nl.uu.cs.ape.models.Pair;
import nl.uu.cs.ape.models.SATAtomMappings;
import nl.uu.cs.ape.models.Type;
import nl.uu.cs.ape.models.enums.LogicOperation;
import nl.uu.cs.ape.models.logic.constructs.PredicateLabel;
import nl.uu.cs.ape.models.logic.constructs.TaxonomyPredicate;
import nl.uu.cs.ape.models.sltlxStruc.SLTLxAtom;
import nl.uu.cs.ape.models.sltlxStruc.SLTLxAtomVar;
import nl.uu.cs.ape.solver.minisat.SATSynthesisEngine;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.IntStream;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * The {@link APEUtils} class provide static helper methods used within the
 * project.
 * 
 * @author Vedran Kasalica
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class APEUtils {

	private static final Map<String, Long> timers = new HashMap<>();
	private static final PrintStream original = System.err;
	private static final PrintStream nullStream = new PrintStream(new OutputStream() {
		@Override
		public void write(int b) {
		}
	});

	/**
	 * Create the full class IRI (ID) based on the label and the OWL prefix.
	 *
	 * @param label             Label of the current term.
	 * @param ontologyPrefixIRI OWL prefix information.
	 * @return String representing full OWL class IRI.
	 * @throws IllegalArgumentException Error if the given label is an empty String.
	 */
	public static String createClassIRI(String label, String ontologyPrefixIRI) throws IllegalArgumentException {
		if (label == null || label.equals("")) {
			throw new IllegalArgumentException("The OWL object label cannot be an empty String.");
		} else if (label.startsWith("http")) {
			return label;
		} else {
			return ontologyPrefixIRI + label;
		}
	}

	/**
	 * Create the full set of class IRI's (ID) based on the labels and the OWL
	 * prefix.
	 *
	 * @param taxonomyTerms     Tool labels.
	 * @param ontologyPrefixIRI OWL prefix information.
	 * @return Set of strings representing full OWL class IRI.
	 */
	public static Set<String> createIRIsFromLabels(Set<String> taxonomyTerms, String ontologyPrefixIRI) {
		Set<String> taxonomyTermIRIs = new HashSet<>();
		for (String taxonomyTermLabel : taxonomyTerms) {
			taxonomyTermIRIs.add(createClassIRI(taxonomyTermLabel, ontologyPrefixIRI));
		}
		return taxonomyTermIRIs;
	}

	/**
	 * The method return a list of {@link JSONObject} elements that correspond to a
	 * given key in a Json file. If the key corresponds to a {@link JSONArray} all
	 * the elements are put in a {@link List}, otherwise if the key corresponds to a
	 * {@link JSONObject} list will contain only that object.
	 *
	 * @param jsonFile File instance containing a json file.
	 * @param key      Key label that corresponds to the elements.
	 * @return List of elements that corresponds to the key. If the key does not
	 *         exists returns empty list.
	 * @throws IOException Error in handling a JSON file.
	 */
	public static List<JSONObject> getListFromJson(File jsonFile, String key) throws IOException, JSONException {
		String content = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
		JSONObject jsonObject = new JSONObject(content);

		return getListFromJson(jsonObject, key, JSONObject.class);

	}

	/**
	 * The method return a list of {@code <T>} elements that correspond to a given
	 * key in the given json object. If the key corresponds to a {@link JSONArray}
	 * all the elements are put in a {@link List}, otherwise if the key corresponds
	 * to a {@code <T>} list will contain only that object.
	 *
	 * @param <T>        Class to which the elements should belong to.
	 * @param jsonObject {@link JSONObject} that is being explored.
	 * @param key        Key label that corresponds to the elements.
	 * @param clazz      Class to which the elements should belong to.
	 * @return List of elements that corresponds to the key. If the key does not
	 *         exists returns empty list.
	 */
	public static <T> List<T> getListFromJson(JSONObject jsonObject, String key, Class<T> clazz) {
		List<T> jsonList = new ArrayList<>();
		try {
			Object tmp = jsonObject.get(key);
			try {
				if (tmp instanceof JSONArray) {
					jsonList = getListFromJSONArray((JSONArray) tmp, clazz);
				} else {
					T element = (T) tmp;
					jsonList.add(element);
				}
			} catch (JSONException e) {
				log.warn("Json parsing error. Expected object '" + clazz.getSimpleName() + "' under the tag '"
						+ key + "'. The following object does not match the provided format:\n"
						+ jsonObject.toString());
				return jsonList;
			}
			return jsonList;
		} catch (JSONException e) {
			// Return empty list in case the key doesn't exist.
			return jsonList;
		}
	}

	/**
	 * The method return a list of {@link JSONObject} elements that correspond to
	 * the value of a given key in the given json object. If the key corresponds to
	 * a {@link JSONArray} all the elements are put in a {@link List}, otherwise if
	 * the key corresponds to a {@link JSONObject} list will contain only that
	 * object.
	 * 
	 * @param jsonObject {@link JSONObject} that is being explored.
	 * @param key        Key label that corresponds to the elements.
	 * 
	 * @return List of elements that corresponds to the key. If the key does not
	 *         exists returns empty list.
	 */
	public static List<JSONObject> getJSONListFromJson(JSONObject jsonObject, String key) {
		return getListFromJson(jsonObject, key, JSONObject.class);
	}

	/**
	 * The method converts the {@link JSONArray} object to {@link List} of objects
	 * of the given structure.
	 *
	 * @param <T>       Class to which the elements should belong to.
	 * @param jsonArray JSON array object.
	 * @param clazz     Class type that the elements of the array are.
	 * @return List of objects of type {@link T}.
	 */
	@SuppressWarnings("unchecked")
	public static <T> List<T> getListFromJSONArray(JSONArray jsonArray, Class<T> clazz) {
		List<T> newList = new ArrayList<>();
		for (int i = 0; i < jsonArray.length(); i++) {
			T element = (T) jsonArray.get(i);
			newList.add(element);
		}
		return newList;
	}

	/**
	 * The method converts the {@link JSONArray} object to {@link List} of
	 * {@link JSONObject} objects.
	 *
	 * @param jsonArray JSON array object.
	 * @return List of {@link JSONObject} objects.
	 */
	public static List<JSONObject> getJSONListFromJSONArray(JSONArray jsonArray) {
		return getListFromJSONArray(jsonArray, JSONObject.class);
	}

	/**
	 * Debug printout.
	 *
	 * @param runConfig   Configuration of the APE run.
	 * @param domainSetup Domain information, including all the existing tools and
	 *                    types.
	 */
	public static void debugPrintout(APERunConfig runConfig, APEDomainSetup domainSetup) {
		String line = "-------------------------------------------------------------";

		if (runConfig.getDebugMode()) {

			/*
			 * Printing the constraint templates
			 */
			log.info(line);
			log.info("\tConstraint templates:");
			log.info(line);
			log.info(domainSetup.getConstraintFactory().printConstraintsCodes() + "\n");

			/*
			 * Printing the Module and Taxonomy Tree
			 */
			log.info(line);
			log.info("\tTool Taxonomy:");
			log.info(line);
			domainSetup.getAllModules().getRootModule().printTree(" ", domainSetup.getAllModules());
			log.info("\n" + line);
			log.info("\tData Taxonomy dimensions:");
			for (TaxonomyPredicate dimension : domainSetup.getAllTypes().getRootPredicates()) {
				log.info("\n" + line);
				log.info("\t" + dimension.getPredicateLabel() + "Taxonomy:");
				log.info(line);
				dimension.printTree(" ", domainSetup.getAllTypes());
			}
			log.info(line);
			log.info("\tLabels Taxonomy:");
			log.info(line);
			domainSetup.getAllTypes().getLabelRoot().printTree(" ", domainSetup.getAllTypes());

			/*
			 * Printing the tool annotations
			 */
			boolean noTools = true;
			log.info(line);
			log.info("\tAnnotated tools:");
			log.info(line);
			for (TaxonomyPredicate module : domainSetup.getAllModules().getModules()) {
				if (module instanceof Module) {
					log.info(module.toString());
					noTools = false;
				}
			}
			if (noTools) {
				log.info("\tNo annotated tools.");
			}

			/*
			 * Print out the constraints
			 */
			log.info(line);
			log.info("\tConstraints:");
			log.info(line);
			for (ConstraintTemplateData constr : domainSetup.getUnformattedConstr()) {
				log.info(domainSetup.getConstraintFactory().getDescription(constr));
			}
			if (domainSetup.getUnformattedConstr().isEmpty()) {
				log.info("\tNo constraints.");
			}
			log.info(line);

			int i = 1;
			for (Type input : runConfig.getProgramInputs()) {
				log.info((i++) + ". program input is " + input.toShortString());
			}
			log.info(line);
			i = 1;
			for (Type output : runConfig.getProgramOutputs()) {
				log.info((i++) + ". program output is " + output.toShortString());
			}
			log.info(line);
		}
	}

	/**
	 * Print header to illustrate the part of the synthesis that is being performed.
	 *
	 * @param argument Order number of the (sub)title.
	 * @param title    The mail content of the title.
	 */
	public static void printHeader(Integer argument, String... title) {
		String line = "-------------------------------------------------------------";

		String arg = (argument == null) ? "" : (" " + argument);

		log.info(line);
		log.info("\t" + title[0] + arg);
		if (title.length > 1) {
			log.info("\t" + title[1] + arg);
		}
		log.info(line);
	}

	/**
	 * Provide a safe interface for iteration trough a list/set.
	 *
	 * @param <E>      Any type.
	 * @param currList List/set that is being evaluated.
	 * @return An empty list in case of {@code currList == null}, or
	 *         {@code currList} otherwise.
	 */
	public static <E> Collection<E> safe(Collection<E> currList) {
		return currList == null ? Collections.emptyList() : currList;
	}

	/**
	 * Provide a safe interface for getting an element from a list. In order to
	 * bypass "index out of bounds" error.
	 *
	 * @param <E>      Any type.
	 * @param currList List of elements.
	 * @param index    Index of the element that is to be returned.
	 * @return Element of the list, or null if the index is out of bounds.
	 */
	public static <E> E safeGet(List<E> currList, int index) {
		if (currList == null || index < 0 || currList.size() <= index) {
			return null;
		} else {
			return currList.get(index);
		}
	}

	/**
	 * Functions sets an element into a list at a specific place, if the element
	 * already exists, it overrides it. If the element is out of bound it creates
	 * null elements to fit the given size of the array and then adds the new
	 * element. If the index is negative number it does not change the array.
	 *
	 * @param <E>     Any type.
	 * @param list    List that is manipulated.
	 * @param index   Absolute position of the new element.
	 * @param element New element to be added to the list.
	 * @throws IndexOutOfBoundsException Exception if the index is out of range
	 *                                   (index &lt; 0).
	 */
	public static <E> void safeSet(List<E> list, int index, E element) {
		if (index < 0) {
			throw new IndexOutOfBoundsException();
		}
		if (list.size() == index) {
			list.add(element);
		} else if (list.size() >= index) {
			list.set(index, element);
		}
		if (list.size() < index) {
			for (int i = list.size(); i < index; i++) {
				list.add(null);
			}
			list.add(element);
		}
	}

	/**
	 * Count number of new lines in a Sting.
	 *
	 * @param inputString String that is evaluated.
	 * @return Number of lines in the String.
	 * @throws IOException In case that the string.
	 */
	public static int countNewLines(String inputString) throws IOException {
		try (InputStream stream = IOUtils.toInputStream(inputString, StandardCharsets.UTF_8)) {
			byte[] c = new byte[1024];

			int readChars = stream.read(c);
			if (readChars == -1) {
				// bail out if nothing to read
				return 1;
			}

			// make it easy for the optimizer to tune this loop
			int count = 0;
			while (readChars == 1024) {
				for (int i = 0; i < 1024;) {
					if (c[i++] == '\n') {
						++count;
					}
				}
				readChars = stream.read(c);
			}

			// count remaining characters
			while (readChars != -1) {
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
				readChars = stream.read(c);
			}

			return count == 0 ? 1 : count;
		}
	}

	/**
	 * Timer start if in debug mode.
	 *
	 * @param timerID   the timer id
	 * @param debugMode the debug mode
	 */
	public static void timerStart(String timerID, Boolean debugMode) {
		if (Boolean.TRUE.equals(debugMode)) {
			timers.put(timerID, System.currentTimeMillis());
		} else {
			timers.put(timerID, (long) -1);
		}
	}

	/**
	 * Return the time left on the timer in milliseconds.
	 * 
	 * @param timerID Timer ID, used to identify the timer.
	 * @param timeout Set timeout (for the synthesis).
	 * @return Time left on the timer in milliseconds.
	 */
	public static long timerTimeLeft(String timerID, long timeout) {
		if (timers.get(timerID) == -1) {
			return 0;
		}

		long elapsedTimeMs = System.currentTimeMillis() - timers.get(timerID);
		return timeout - elapsedTimeMs;
	}

	/**
	 * Timer restart and print.
	 *
	 * @param timerID     the timer id
	 * @param printString the print string
	 */
	public static void timerRestartAndPrint(String timerID, String printString) {
		if (timers.get(timerID) == -1) {
			return;
		}
		long printTime = System.currentTimeMillis() - timers.get(timerID);
		log.info(printString + " setup time: " + (printTime / 1000F) + " sec.");
		timers.put(timerID, System.currentTimeMillis());

		// APEUtils.printMemoryStatus(true);
	}

	/**
	 * Timer print solutions.
	 *
	 * @param timerID        the timer id
	 * @param solutionsFound the solutions found
	 * @return The time counted by the timer.
	 */
	public static long timerPrintSolutions(String timerID, int solutionsFound) {
		return timerPrintSolutions(timerID, solutionsFound, nl.uu.cs.ape.models.enums.SolverType.SAT);
	}

	/**
	 * Timer print solutions with solver-specific timing labels.
	 *
	 * @param timerID        the timer id
	 * @param solutionsFound the solutions found
	 * @param solverType     the solver that was used
	 * @return The time counted by the timer.
	 */
	public static long timerPrintSolutions(String timerID, int solutionsFound,
			nl.uu.cs.ape.models.enums.SolverType solverType) {
		if (timers.get(timerID) == -1) {
			return -1;
		}
		long printTime = System.currentTimeMillis() - timers.get(timerID);
		log.info("APE found " + solutionsFound + " solutions.");
		log.info("Total APE runtime: \t\t" + (printTime / 1000F) + " sec.");
		if (solverType == nl.uu.cs.ape.models.enums.SolverType.CLINGO) {
			log.info("Total encoding time: \t\t"
					+ (nl.uu.cs.ape.solver.clingo.ClingoSynthesisEngine.getTotalEncodingTime() / 1000F) + " sec.");
			log.info("Total grounding time: \t\t"
					+ (nl.uu.cs.ape.solver.clingo.ClingoSynthesisEngine.getTotalGroundingTime() / 1000F) + " sec.");
			log.info("Total Clingo solving time: \t"
					+ (nl.uu.cs.ape.solver.clingo.ClingoSynthesisEngine.getTotalSolvingTime() / 1000F) + " sec.");
		} else {
			log.info("Total encoding time: \t\t" + (SATSynthesisEngine.getTotalEncodingTime() / 1000F) + " sec.");
			log.info("Total SAT solving time: \t" + (SATSynthesisEngine.getTotalSolvingTime() / 1000F) + " sec.");
		}
		return printTime;
	}

	/**
	 * Timer print text.
	 *
	 * @param timerID the timer id
	 * @param text    the text
	 */
	public static void timerPrintText(String timerID, String text) {
		if (timers.get(timerID) == -1) {
			return;
		}
		long printTime = System.currentTimeMillis() - timers.get(timerID);
		log.info(text + " Running time: " + (printTime / 1000F) + " sec.");
	}

    /**
     * This function counts the CNF clauses in a stream, assuming correct CNF syntax.
     *
     * @param cnfEncoding the CNF encoding
     * @return the count of non-empty clauses
     */
    public static int countCNFClauses(InputStream cnfEncoding) {

        int count = 0;
        try (BufferedReader b = new BufferedReader(new InputStreamReader(cnfEncoding))) {
            String line = null;
            while ((line = b.readLine()) != null) {
                if (line.endsWith(" 0")) count++;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return count;
    }

    /**
     * This function counts the non-empty CNF clauses in a stream and
     * while also checking the CNF syntax and warns if a clause is empty.
     *
     * @param cnfEncoding the CNF encoding
     * @return the count of non-empty clauses
     */
    public static int checkCNFClauses(InputStream cnfEncoding) {

        int clauseCount = 0;
        int litCount    = 0;

        Scanner scanner = new Scanner(cnfEncoding);
        while (scanner.hasNextInt()) {
            int intAtom = scanner.nextInt();

            if (intAtom == 0) {
                if (litCount == 0) {
                    log.warn("Found empty clause (" + (clauseCount + 1) + ") after in CNF input, will not count for DIMACS.");
                } else {
                    clauseCount++;
                }
                litCount = 0;
            } else {
                litCount++;
            }
        }
        scanner.close();

        return clauseCount;
    }

	/**
	 * Convert cnf 2 human readable string.
	 *
	 * @param tempSatInput the temp sat input
	 * @param mappings     the mappings
	 * @return the string
	 */
	public static String convertCNF2humanReadable(InputStream tempSatInput, SATAtomMappings mappings) {
		StringBuilder humanReadable = new StringBuilder();
		Scanner scanner = new Scanner(tempSatInput);
		scanner.nextLine();
		while (scanner.hasNextInt()) {
			int intAtom = scanner.nextInt();

			if (intAtom == 0) {
				humanReadable.append("\n");
			} else if (intAtom > -3 & intAtom < 3) {
				if (intAtom == 1 || intAtom == -2) {
					humanReadable.append("true ");
				} else {
					humanReadable.append("false ");
				}
			} else if (intAtom > 0) {
				SLTLxAtom atom = mappings.findOriginal(intAtom);
				if (atom == null) {
					SLTLxAtomVar varAtom = mappings.findOriginalVar(intAtom);
					humanReadable.append(varAtom.toString()).append(" ");
				} else {
					humanReadable.append(atom.toString()).append(" ");
				}

			} else if (intAtom < 0) {
				SLTLxAtom atom = mappings.findOriginal(-intAtom);
				if (atom == null) {
					SLTLxAtomVar varAtom = mappings.findOriginalVar(-intAtom);
					if (varAtom == null) {
						log.error("Error: Could not find atom for " + intAtom);
					} else {
						humanReadable.append("-").append(varAtom.toString()).append(" ");
					}
				} else {
					humanReadable.append("-").append(atom.toString()).append(" ");
				}

			}
		}
		scanner.close();

		return humanReadable.toString();
	}

	/**
	 * Return the string without its last character.
	 *
	 * @param str Given string.
	 * @return A copy of the given string without its last character.
	 */
	public static String removeLastChar(String str) {
		if (str != null && str.length() > 0) {
			str = str.substring(0, str.length() - 1);
		}
		return str;
	}

	/**
	 * Return the string without its last N characters.
	 *
	 * @param str Given string.
	 * @param n   Number of characters to be removed.
	 * @return A copy of the given string without its last character.
	 */
	public static String removeNLastChar(String str, int n) {
		if (str != null && str.length() > 0) {
			str = str.substring(0, str.length() - n);
		}
		return str;
	}

	/**
	 * Method creates a label based on the list of predicates and the logical
	 * operator.
	 *
	 * @param relatedPredicates List of predicates that should be used to create the
	 *                          new label.
	 * @param logicOp           Logical operator that configures the label.
	 * @return String representing a new label made based on the predicates and the
	 *         logical operator.
	 */
	public static String getLabelFromList(SortedSet<TaxonomyPredicate> relatedPredicates, LogicOperation logicOp) {
		StringBuilder abstractLabel = new StringBuilder(logicOp.toStringSign());
		for (TaxonomyPredicate label : relatedPredicates) {
			abstractLabel.append(label.getPredicateLabel()).append(logicOp.toStringSign());
		}

		return abstractLabel.toString();
	}

	/**
	 * Print a warning to the console for the user to see.
	 *
	 * @param message The warning message.
	 * @param params  additional parameters (uses String.format)
	 */
	public static void printWarning(String message, Object... params) {
		log.warn("\u001B[35mWARNING: " + String.format(message, params) + "\u001B[0m");
	}

	/**
	 * Disable System.err temporarily, enable again with {@link #enableErr}.
	 * 
	 */
	public static void disableErr() {
		System.setErr(nullStream);
	}

	/**
	 * Reset System.err to normal.
	 * 
	 */
	public static void enableErr() {
		System.setErr(original);
	}

	/**
	 * Clone the given JSON object
	 * 
	 * @param original original JSON object
	 * @return copy of the original JSONObject.
	 */
	public static JSONObject clone(JSONObject original) {
		return new JSONObject(original, JSONObject.getNames(original));
	}

	/**
	 * Count the number of lines in a file.
	 * 
	 * @param cnfEncoding file to count lines
	 * @return number of lines
	 */
	public static int countLines(File cnfEncoding) {
		int lines = 0;
		try (BufferedReader b = new BufferedReader(new FileReader(cnfEncoding))) {
			while (b.readLine() != null) {
				lines++;

			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return lines;
	}

	/**
	 * Get a String value from a Map by its key.
	 * 
	 * @param map Map containing the key-value pairs.
	 * @param key Key to look for in the map.
	 * @return The value associated with the key as a String, or null if the key
	 *         does not exist or the value is not a String.
	 */
	public static String mapGetString(Map<String, Object> map, String key) {
		Object val = map.get(key);
		return (val instanceof String) ? (String) val : null;
	}

	/**
	 * Visualises in the command line the memory status of the VM at the given step,
	 * if debug mode is on.
	 * 
	 * @param debugMode true if debug mode is on
	 */
	public static void printMemoryStatus(boolean debugMode) {
		if (!debugMode) {
			return;
		}
		double currMemMB = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024;
		double totalMemMB = (Runtime.getRuntime().totalMemory()) / 1024 / 1024;
		double oneCharValueMB = 100;
		int currMemChars = (int) Math.ceil(currMemMB / oneCharValueMB);
		int totalMemChars = (int) Math.ceil(totalMemMB / oneCharValueMB);

		StringBuilder memoryStatus = new StringBuilder();
		IntStream.range(0, currMemChars).forEach(step -> memoryStatus.append("#"));
		IntStream.range(currMemChars, totalMemChars).forEach(step -> memoryStatus.append(" "));
		log.info("\n[" + memoryStatus.toString() + "]\t" + totalMemMB + "\tMB \r");

	}

	/**
	 * Get all unique pairs of PredicateLabels within the collection.
	 * 
	 * @param <T> The type that extends PredicateLabel
	 * @param set Set of PredicateLabel that should be used to create the pairs
	 * @return Set of unique pairs.
	 */
	public static <T extends PredicateLabel> Set<Pair<T>> getUniquePairs(Collection<T> set) {
		Set<Pair<T>> pairs = new HashSet<>();
		set.stream().forEach(ele1 -> set.stream().filter(ele2 -> ele1.compareTo(ele2) < 0)
				.forEach(ele2 -> pairs.add(new Pair<>(ele1, ele2))));
		return pairs;
	}

	/**
	 * Get unique pairs of elements within 2 collections.
	 * 
	 * @param set1 Set of elements that should be used to create the first
	 *             elements of the pairs
	 * @param set2 Set of elements that should be used to create the second
	 *             elements of the pairs
	 * @return Set of unique pairs.
	 */
	public static <T> Set<Pair<T>> getUniquePairs(Collection<T> set1, Collection<T> set2) {
		Set<Pair<T>> pairs = new HashSet<>();
		set1.stream().forEach(ele1 -> set2.stream().forEach(ele2 -> pairs.add(new Pair<>(ele1, ele2))));
		return pairs;
	}

	/**
	 * Generates a list of distinct pairs of integers where the first element is
	 * smaller than the second.
	 * Each pair is generated up to the specified maximum number (exclusive).
	 *
	 * @param maxNumber The exclusive upper bound for generating pairs.
	 * @return List of distinct pairs of integers.
	 */
	public static List<Pair<Integer>> generateDistinctPairs(int maxNumber) {
		List<Pair<Integer>> distinctPairs = new ArrayList<>();

		for (int first = 0; first < maxNumber - 1; first++) {
			for (int second = first + 1; second < maxNumber; second++) {
				distinctPairs.add(new Pair<>(first, second));
			}
		}

		return distinctPairs;
	}

}
