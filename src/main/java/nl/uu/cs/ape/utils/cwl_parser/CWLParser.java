package nl.uu.cs.ape.utils.cwl_parser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.commonwl.cwlsdk.cwl1_2.CommandInputParameter;
import org.commonwl.cwlsdk.cwl1_2.CommandLineTool;
import org.commonwl.cwlsdk.cwl1_2.CommandOutputParameter;
import org.commonwl.cwlsdk.cwl1_2.utils.RootLoader;

import nl.uu.cs.ape.utils.APEFiles;

public class CWLParser {

    private CommandLineTool cwlToolContent;

    public static final String DATA_ROOT = "data_0006";
    public static final String FORMAT_ROOT = "format_1915";
    public static final String DATA_ROOT_IRI = "http://edamontology.org/" + DATA_ROOT;

    /**
     * Constructor to load CWL from a URL string
     * @param inputStream The input stream containing the CWL file content.
     * @param uriString URI string pointing to the CWL file.
     * @throws IOException if the file cannot be read
     */
    protected CWLParser(InputStream inputStream, String uriString) throws IOException {
        String fileContent = new String(inputStream.readAllBytes());
        inputStream.close();
        cwlToolContent = (CommandLineTool) RootLoader.loadDocument(fileContent, uriString);

    }

    /**
     * Constructor to load CWL from a URL string
     * @param urlString URL string pointing to the CWL file
     * @throws IOException if the file cannot be read
     */
    public CWLParser(String urlString) throws IOException {
        this(new FileInputStream(APEFiles.readPathToFile(urlString)), urlString);
    }

    /**
     * Constructor to load CWL from a file path
     * 
     * @param filePath {@link Path} to the CWL file
     * @throws IOException if the file cannot be read
     */
    public CWLParser(Path filePath) throws IOException {
        this(Files.newInputStream(filePath), filePath.toString());
    }

    /**
     * Get the operations (intents) defined in the CWL tool.
     * 
     * @return List of operation strings. List is empty if no operations are defined.
     */
    public List<String> getOperations() {
        Optional<List<String>> intentObj = cwlToolContent.getIntent();

        return intentObj.orElse(List.of());
    }



    /**
     * Get the input parameters defined in the CWL tool.
     * 
     * @return List of input parameters.
     */
    public List<CommandInputParameter> getInputs() {
        return cwlToolContent.getInputs().stream()
            .map(obj -> (CommandInputParameter) obj)
            .collect(Collectors.toList());

    }

    /**
     * Get the output parameters defined in the CWL tool.
     * 
     * @return List of output parameters.
     */
    public List<CommandOutputParameter> getOutputs() {
        return cwlToolContent.getOutputs().stream()
                .map(obj -> (CommandOutputParameter) obj)
                .collect(Collectors.toList());
    }
    
    /**
     * Get the label of the CWL tool, used as the name of the tool.
     * 
     * @return Label string. Empty string if no label is defined.
     */
    public String getLabel() {
        return cwlToolContent.getLabel().orElse("");
    }

}
