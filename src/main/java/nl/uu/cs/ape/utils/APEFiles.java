package nl.uu.cs.ape.utils;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.uu.cs.ape.configuration.APEConfigException;
import nl.uu.cs.ape.models.sltlxStruc.CNFClause;

import org.apache.commons.io.LineIterator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Locale;
import java.util.Scanner;
import java.util.Set;
import java.nio.charset.StandardCharsets;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

/**
 * The type Ape files.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class APEFiles {

    /**
     * READ and WRITE enums used to verify paths.
     */
    public enum Permission {
        /** Read permission. */
        READ,
        /** Write permission. */
        WRITE
    }

    /**
     * Verify and get full local path based based on the field.
     * 
     * @param tag  tag
     * @param path path
     * @return Return the path to the file
     */
    private static Path getLocalPath(String tag, String path) {

        // check on empty values
        if (path == null) {
            throw APEConfigException.invalidValue(tag, "null", "value is null.");
        }
        if (path.equals("")) {
            throw APEConfigException.invalidValue(tag, path, "value is empty.");
        }

        // check format
        try {
            return Paths.get(path);
        } catch (InvalidPathException | NullPointerException e) {
            throw APEConfigException.invalidValue(tag, path,
                    String.format("The path for tag '%s' is invalid. %s", tag, e.getMessage()));
        }
    }

    /**
     * Verify whether the path is a valid local path.
     * 
     * @param path local path
     * @return Return {@code true} if the local path exists, {@code false}
     *         otherwise.
     */
    public static boolean localPathExists(String path) {
        Path localPath = Paths.get(path);
        return Files.exists(localPath);
    }

    /**
     * Checks whether a path (local or URL) has a valid format.
     * E.g. will return false if the path contains
     * forbidden character.
     *
     * @param path the path (URL or local path).
     * @return A boolean indicating whether the path has a valid format.
     */
    public static boolean validPathFormat(String path) {
        return validLocalPathFormat(path) || isURI(path);
    }

    /**
     * Checks whether a local path has a valid format.
     * E.g. will return false if the path contains
     * forbidden character.
     *
     * @param path the path
     * @return a boolean indicating whether the path has a valid format
     */
    public static boolean validLocalPathFormat(String path) {
        try {
            Paths.get(path);
            return true;
        } catch (InvalidPathException | NullPointerException e) {
            return false;
        }
    }

    /**
     * Method checks whether the provided value represent a correct path to a file,
     * and returns the corresponding file if it does.
     *
     * @param tag                  Corresponding tag from the config file.
     * @param inputPath            Provided path for the file.
     * @param requestedPermissions the requested permissions
     * @return File represented by the path in the JSON object, or the default value
     *         if the tag is not present.
     * @throws IOException        Error if path is cannot be found.
     * @throws JSONException      Error in parsing the value for specified tag.
     * @throws APEConfigException Error in setting up the the configuration.
     */
    public static File readFileFromPath(String tag, String inputPath, Permission... requestedPermissions)
            throws IOException, JSONException, APEConfigException {

        final Path path = getLocalPath(tag, inputPath);

        if (Files.notExists(path)) {
            throw APEConfigException.pathNotFound(tag, inputPath);
        }

        if (!Files.isRegularFile(path)) {
            throw APEConfigException.notAFile(tag, inputPath);
        }

        // check permissions
        checkPermissions(tag, path, requestedPermissions);

        return path.toFile();
    }

    /**
     * Method checks whether the provided value represent a correct path, and
     * returns the path if it does.
     *
     * @param tag                  Corresponding tag from the config file.
     * @param inputPath            Path to the directory.
     * @param requestedPermissions the requested permissions
     * @return Path represented in the JSON object, or the default value if the tag
     *         is not present.
     * @throws IOException        Error if path is cannot be found.
     * @throws JSONException      Error in parsing the value for specified tag.
     * @throws APEConfigException Error in setting up the the configuration.
     */
    public static Path readDirectoryPath(String tag, String inputPath, Permission... requestedPermissions)
            throws IOException, JSONException, APEConfigException {

        final Path path = getLocalPath(tag, inputPath);
        final String absolutePath = path.toAbsolutePath().toString();

        // first check if the format of the string resembles a path to a folder
        if (!isFolderFormat(path)) {
            throw APEConfigException.notADirectory(tag, absolutePath);
        }

        // create a new directory if necessary
        createDirectory(tag, path);

        if (!directoryExists(path)) {
            throw APEConfigException.notADirectory(tag, absolutePath);
        }

        try {
            checkPermissions(tag, path, requestedPermissions);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return path.toAbsolutePath();
    }

    /**
     * Create a directory path if needed.
     * 
     * @param tag  tag used
     * @param path directory path
     */
    private static void createDirectory(String tag, Path path) throws APEConfigException {

        if (Files.notExists(path)) {

            final String absolutePath = path.toAbsolutePath().toString();

            if (!isFolderFormat(path)) {
                throw new APEConfigException("Path '" + absolutePath + "' for tag '" + tag + "' is not a directory!");
            }

            APEUtils.printWarning(absolutePath + "' does not exist. Directory will be created.");

            if (new File(absolutePath).mkdirs()) {
                log.debug("Successfully created directory '" + absolutePath + "'");
            } else {
                throw new APEConfigException("Could not create directory '" + absolutePath + "' for tag '" + tag + "'");
            }
        }

    }

    /**
     * Check permissions.
     *
     * @param tag                  the tag
     * @param path                 the path
     * @param requestedPermissions the requested permissions
     * @throws IOException exception if the path misses requested permissions.
     */
    public static void checkPermissions(String tag, Path path, Permission... requestedPermissions) throws IOException {

        if (Arrays.stream(requestedPermissions).anyMatch(p -> p == Permission.READ) && !Files.isReadable(path)) {
            throw APEConfigException.missingPermission(tag, path.toString(), Permission.READ);
        }

        if (Arrays.stream(requestedPermissions).anyMatch(p -> p == Permission.WRITE) && !Files.isWritable(path)) {
            throw APEConfigException.missingPermission(tag, path.toString(), Permission.WRITE);
        }
    }

    /**
     * Path is a folder.
     *
     * @param path the path
     * @return the boolean
     */
    public static boolean isFolderFormat(Path path) {
        return FilenameUtils.getExtension(path.toString()).equals("");
    }

    /**
     * Path is a file.
     *
     * @param path the path
     * @return the boolean
     */
    public static boolean isFileFormat(Path path) {
        return !isFolderFormat(path);
    }

    /**
     * Directory exists boolean.
     *
     * @param path the path
     * @return the boolean
     */
    public static boolean directoryExists(Path path) {
        return Files.isDirectory(path);
    }

    /**
     * The exists boolean.
     *
     * @param jsonObject the object
     * @return the boolean
     */
    public static boolean isJSON(JSONObject jsonObject) {
        return jsonObject != null;
    }

    /**
     * Directory exists boolean.
     *
     * @param jsonArray the path
     * @return the boolean
     */
    public static boolean isJSONArray(JSONArray jsonArray) {
        return jsonArray != null;
    }

    /** Connect and read timeout used when reading content from a URL. */
    private static final int URL_TIMEOUT_MS = 10000;

    /** Maximum size of the content read from a URL (100 MB). */
    public static final long MAX_URL_DOWNLOAD_BYTES = 100L * 1024 * 1024;

    /**
     * String is a valid URI.
     *
     * @param uri the uri
     * @return String is a valid URI
     */
    public static boolean isURI(String uri) {
        try {
            new URL(uri).toURI();
            return true;
        } catch (MalformedURLException | URISyntaxException e1) {
            return false;
        }
    }

    /**
     * Read file content from the given path (local path or a public http(s) URL)
     * and return the content as a File object. URLs with other protocols (such as
     * {@code file:}) are rejected, and the content read from a URL is limited to
     * {@link #MAX_URL_DOWNLOAD_BYTES}.
     * 
     * @param filePath Local path or a public URL with the content.
     * @return File containing info provided at the path.
     * @throws IOException Exception in case of a badly formatted path or file.
     */
    public static File readPathToFile(String filePath) throws IOException {

        return (isURI(filePath) ? readURLToFile(filePath) : new File(filePath));

    }

    /**
     * Read content from a URL and return it as a file.
     * 
     * @param fileUrl URL of the content
     * @return File containing info provided at the URL.
     * @throws IOException Exception in case of a badly formatted URL or file.
     */
    private static File readURLToFile(String fileUrl) throws IOException {
        URL url = new URL(fileUrl);
        String protocol = url.getProtocol().toLowerCase(Locale.ROOT);
        if (!protocol.equals("http") && !protocol.equals("https")) {
            /*
             * Other protocols (e.g. file: or jar:) would let a configuration read
             * arbitrary local files. Local files should be given as plain paths.
             */
            throw new IOException("Unsupported URL protocol '" + protocol
                    + ":'. Only http and https URLs are supported, local files should be given as a path.");
        }
        URLConnection connection = url.openConnection();
        connection.setConnectTimeout(URL_TIMEOUT_MS);
        connection.setReadTimeout(URL_TIMEOUT_MS);
        if (connection.getContentLengthLong() > MAX_URL_DOWNLOAD_BYTES) {
            throw new IOException("The content at '" + fileUrl + "' is larger than the limit of "
                    + MAX_URL_DOWNLOAD_BYTES + " bytes.");
        }

        File loadedFile = File.createTempFile("ape_temp_", "");
        loadedFile.deleteOnExit();
        try (InputStream in = connection.getInputStream();
                OutputStream out = Files.newOutputStream(loadedFile.toPath())) {
            byte[] buffer = new byte[8192];
            long total = 0;
            int read;
            while ((read = in.read(buffer)) != -1) {
                total += read;
                if (total > MAX_URL_DOWNLOAD_BYTES) {
                    throw new IOException("The content at '" + fileUrl + "' is larger than the limit of "
                            + MAX_URL_DOWNLOAD_BYTES + " bytes.");
                }
                out.write(buffer, 0, read);
            }
        } catch (IOException e) {
            Files.deleteIfExists(loadedFile.toPath());
            throw e;
        }
        return loadedFile;
    }

    /**
     * Appends text to the existing file. It adds the text at the end of the content
     * of the file.
     *
     * @param file    The existing file.
     * @param content The content that should be appended.
     * @throws IOException          In case of an I/O error.
     * @throws NullPointerException If the file or content is null.
     */
    public static void appendToFile(File file, String content) throws IOException {
        if (file == null || content == null) {
            throw new NullPointerException("File or content cannot be null.");
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.US_ASCII), 8192 * 4)) {
            writer.write(content);
        }
    }

    /**
     * Appends a set of text strings to the existing file. Each element in the set
     * is added at the end of the content of the file.
     *
     * @param file    The existing file.
     * @param content The set of content strings that should be appended.
     * @throws IOException          In case of an I/O error.
     * @throws NullPointerException If the file or content set is null.
     */
    public static void appendSetToFile(File file, Set<String> content) throws IOException {
        if (file == null || content == null) {
            throw new NullPointerException("File or content set cannot be null.");
        }

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.US_ASCII), 8192 * 4)) {
            for (String str : content) {
                writer.write(str);
            }
        }
    }

    /**
     * Appends CNFClause objects' CNF string representations to the existing file.
     * Each CNF clause is added at the end of the content of the file.
     *
     * @param file        The existing file.
     * @param cnfEncoding The set of CNFClause objects to be appended.
     * @throws IOException          In case of an I/O error.
     * @throws NullPointerException If the file or CNF encoding set is null.
     */
    public static void appendToFile(File file, Set<CNFClause> cnfEncoding) throws IOException {
        if (file == null || cnfEncoding == null) {
            throw new NullPointerException("File or CNF encoding set cannot be null.");
        }

        StringBuilder stringBuilder = new StringBuilder();
        cnfEncoding.forEach(clause -> stringBuilder.append(clause.toCNF()));

        try (BufferedWriter writer = new BufferedWriter(
                new OutputStreamWriter(new FileOutputStream(file, true), StandardCharsets.US_ASCII), 8192 * 4)) {
            writer.write(stringBuilder.toString());
        }
    }

    /**
     * Prepends text to the existing file content and creates a new file with the
     * modified content. It adds the text at the beginning, before the existing
     * content of the file.
     *
     * @param prefix The text to be prepended.
     * @param file   The existing file.
     * @return A new file with the prepended content.
     * @throws IOException          In case of an I/O error.
     * @throws NullPointerException If the file or prefix is null.
     */
    public static File prependToFile(String prefix, File file) throws IOException {
        if (file == null || prefix == null) {
            throw new NullPointerException("File or prefix cannot be null.");
        }

        File tempFile = File.createTempFile("prependPrefix", ".tmp");
        tempFile.deleteOnExit();

        try (LineIterator lineIterator = FileUtils.lineIterator(file, StandardCharsets.US_ASCII.name());
             BufferedWriter writer = new BufferedWriter(
                     new OutputStreamWriter(new FileOutputStream(tempFile, true), StandardCharsets.US_ASCII))) {

            // Write the prefix first.
            writer.write(prefix);
            writer.newLine();

            // Append the existing file content.
            while (lineIterator.hasNext()) {
                writer.write(lineIterator.nextLine());
                writer.newLine();
            }
        }

        return tempFile;
    }

    /**
     * Method checks whether the provided path corresponds to an existing file with
     * required reading permissions.
     *
     * @param path Path to the file.
     * @return true if the file exists and can be read, false otherwise.
     */
    public static boolean isValidReadFile(String path) {
        if (path == null || path.equals("")) {
            log.error("Path is not provided correctly.");
            return false;
        }
        File f = new File(path);
        if (!f.isFile()) {
            log.error("Provided path: \"" + path + "\" is not a file.");
            return false;
        } else {
            if (!f.canRead()) {
                log.error("Provided file: \"" + path + "\" is missing the reading permission.");
                return false;
            }
        }
        return true;
    }

    /**
     * Used to write the {@code text} to a file {@code file}. If @append is true,
     * the {@code text} is appended to the {@code file}, otherwise the {@code file}
     * is rewritten.
     *
     * @param text   Text that will be written in the file.
     * @param file   The system-dependent file name.
     * @param append If true, then bytes will be written to the end of the file
     *               rather than the beginning.
     * @return True if write to file was successful, false otherwise.
     * @throws IOException Exception if file not found.
     */
    public static boolean write2file(String text, File file, boolean append) throws IOException {

        try (
                FileWriter fw = new FileWriter(file, append)) {
            fw.write(text);
        }

        return true;
    }

    /**
     * Used to write the {@code InputStream} to a file {@code file}. If @append is
     * true,
     * the {@code InputStream} is appended to the {@code file}, otherwise the
     * {@code file}
     * is rewritten.
     *
     * @param tempSatInput Input stream that will be written in the file.
     * @param file         The system-dependent file name.
     * @param append       If true, then bytes will be written to the end of the
     *                     file
     *                     rather than the beginning.
     * @return True if write to file was successful, false otherwise.
     * @throws IOException Exception if file not found.
     */
    public static boolean write2file(InputStream tempSatInput, File file, Boolean append) throws IOException {
        StringBuilder humanReadable = new StringBuilder();
        Scanner scanner = new Scanner(tempSatInput);

        while (scanner.hasNextLine()) {
            String str = scanner.nextLine();

            humanReadable.append(str).append("\n");
        }
        scanner.close();

        return write2file(humanReadable.toString(), file, append);
    }

    /**
     * Reads the path and provides the JSONObject that represents its content.
     *
     * @param path the path (local or URL) to the file
     * @return JSONObject representing the content of the file.
     * @throws IOException   Error if the file is corrupted
     * @throws JSONException Error if the file is not in expected JSON format
     */
    public static JSONObject readPathToJSONObject(String path) throws IOException, JSONException {
        File file = readPathToFile(path);
        return readFileToJSONObject(file);
    }

    /**
     * Reads the file and provides the JSONObject that represents its content.
     *
     * @param file the JSON file
     * @return JSONObject representing the content of the file.
     * @throws IOException   Error if the file is corrupted
     * @throws JSONException Error if the file is not in expected JSON format
     */
    public static JSONObject readFileToJSONObject(File file) throws IOException, JSONException {
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return new JSONObject(content);
    }

    /**
     * Reads the file and provides the JSONArray that represents its content.
     *
     * @param file the JSON file
     * @return JSONArray representing the content of the file.
     * @throws IOException   Error if the file is corrupted
     * @throws JSONException Error if the file is not in expected JSON format
     */
    public static JSONArray readFileToJSONArray(File file) throws IOException, JSONException {
        String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
        return new JSONArray(content);
    }
}
