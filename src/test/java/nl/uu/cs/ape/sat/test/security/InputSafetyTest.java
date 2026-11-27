package nl.uu.cs.ape.sat.test.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.semanticweb.owlapi.model.AxiomType;
import org.semanticweb.owlapi.model.OWLOntology;

import com.sun.net.httpserver.HttpServer;

import nl.uu.cs.ape.domain.BioToolsAPI;
import nl.uu.cs.ape.domain.OWLReader;
import nl.uu.cs.ape.utils.APEFiles;

/**
 * Checks that user provided paths, URLs, IDs and ontologies cannot be used to
 * read local files or make APE request arbitrary URLs.
 */
class InputSafetyTest {

    private HttpServer server;
    private final AtomicInteger requests = new AtomicInteger();
    private String baseUrl;

    @TempDir
    Path tempDir;

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/small.json", exchange -> {
            requests.incrementAndGet();
            byte[] body = "{\"ok\": true}".getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.createContext("/huge.json", exchange -> {
            requests.incrementAndGet();
            /* Declare a body larger than the limit, the client must not read it. */
            exchange.sendResponseHeaders(200, APEFiles.MAX_URL_DOWNLOAD_BYTES + 1);
            exchange.close();
        });
        server.createContext("/", exchange -> {
            requests.incrementAndGet();
            exchange.sendResponseHeaders(404, -1);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    @Test
    void localFileUrlsAreRejected() throws IOException {
        Path secret = Files.writeString(tempDir.resolve("secret.txt"), "TOPSECRET");
        String fileUrl = secret.toUri().toString();
        assertThrows(IOException.class, () -> APEFiles.readPathToFile(fileUrl));
        assertThrows(IOException.class, () -> APEFiles.readPathToFile("jar:" + fileUrl + "!/x"));
    }

    @Test
    void plainLocalPathsStillWork() throws IOException {
        Path file = Files.writeString(tempDir.resolve("config.json"), "{}");
        assertEquals(file.toFile(), APEFiles.readPathToFile(file.toString()));
    }

    @Test
    void httpUrlsAreDownloaded() throws IOException {
        File file = APEFiles.readPathToFile(baseUrl + "/small.json");
        assertEquals("{\"ok\": true}", Files.readString(file.toPath()));
    }

    @Test
    void oversizedDownloadsAreRejected() {
        assertThrows(IOException.class, () -> APEFiles.readPathToFile(baseUrl + "/huge.json"));
    }

    @Test
    void ontologyImportsAreNotFetched() throws Exception {
        Path ontologyFile = Files.writeString(tempDir.resolve("imports.owl"),
                "<?xml version=\"1.0\"?>\n"
                        + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
                        + " xmlns:owl=\"http://www.w3.org/2002/07/owl#\">\n"
                        + "  <owl:Ontology rdf:about=\"http://example.org/main\">\n"
                        + "    <owl:imports rdf:resource=\"" + baseUrl + "/imported.owl\"/>\n"
                        + "  </owl:Ontology>\n"
                        + "</rdf:RDF>\n");
        OWLOntology ontology = OWLReader.createOntologyManager()
                .loadOntologyFromOntologyDocument(ontologyFile.toFile());
        assertEquals(0, requests.get(), "The imported ontology must not be requested.");
        assertEquals(0, ontology.getImports().size());
    }

    @Test
    void xmlExternalEntitiesAreNotResolved() throws Exception {
        Path secret = Files.writeString(tempDir.resolve("secret.txt"), "TOPSECRET");
        Path ontologyFile = Files.writeString(tempDir.resolve("xxe.owl"),
                "<?xml version=\"1.0\"?>\n"
                        + "<!DOCTYPE rdf:RDF [\n"
                        + "  <!ENTITY xxe SYSTEM \"" + secret.toUri() + "\">\n"
                        + "  <!ENTITY xxehttp SYSTEM \"" + baseUrl + "/xxe\">\n"
                        + "]>\n"
                        + "<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\""
                        + " xmlns:owl=\"http://www.w3.org/2002/07/owl#\""
                        + " xmlns:rdfs=\"http://www.w3.org/2000/01/rdf-schema#\">\n"
                        + "  <owl:Ontology rdf:about=\"http://example.org/xxe\"/>\n"
                        + "  <owl:Class rdf:about=\"http://example.org/A\"><rdfs:label>[&xxe;]</rdfs:label></owl:Class>\n"
                        + "  <owl:Class rdf:about=\"http://example.org/B\"><rdfs:label>[&xxehttp;]</rdfs:label></owl:Class>\n"
                        + "</rdf:RDF>\n");
        OWLOntology ontology = OWLReader.createOntologyManager()
                .loadOntologyFromOntologyDocument(ontologyFile.toFile());
        assertFalse(ontology.getAxioms(AxiomType.ANNOTATION_ASSERTION).toString().contains("TOPSECRET"),
                "Local file content must not be included through an XML entity.");
        assertEquals(0, requests.get(), "External XML entities must not be requested.");
    }

    @Test
    void invalidBioToolsIdsAreRejectedBeforeAnyRequest() {
        for (String id : new String[] { "..", ".", "../t", "comet/../../x", "comet?format=xml", "a b", "" }) {
            assertThrows(IOException.class, () -> BioToolsAPI.fetchToolFromBioTools(id), id);
        }
    }
}
