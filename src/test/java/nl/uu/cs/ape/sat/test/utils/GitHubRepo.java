package nl.uu.cs.ape.sat.test.utils;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Objects;

import nl.uu.cs.ape.utils.APEResources;
import static nl.uu.cs.ape.sat.test.utils.Evaluation.fail;
import static nl.uu.cs.ape.sat.test.utils.Evaluation.success;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * The {@code GitHubRepo} class is used to download test use cases from a GitHub
 * repository.
 */
@Slf4j
public class GitHubRepo {

    private String absoluteLocalRoot;
    private String repository;
    private String commit;
    private HashMap<Integer, TempFile> files;

    public GitHubRepo(String repository, String commitOrBranch) {
        this.absoluteLocalRoot = Paths.get(APEResources.getAbsoluteRoot(), "temp").toString();
        this.repository = repository;
        this.commit = commitOrBranch;
        this.files = new HashMap<>();
    }

    public String getRoot() {
        return this.absoluteLocalRoot;
    }

    public void cleanUp() {
        files.clear();
        try {
            File directory = new File(this.absoluteLocalRoot);
            FileUtils.deleteDirectory(directory);
            assertFalse(directory.exists());
        } catch (IOException e) {
            log.info("Could not delete folder '{}':\n{}", this.absoluteLocalRoot, e.getMessage());
        }
    }

    public boolean canConnect() {
        try {
            final URL url = new URL("https://github.com/");
            final URLConnection conn = url.openConnection();
            conn.connect();
            conn.getInputStream().close();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public String getFile(String filePath, String commit) {
        GitFile gf = new GitFile(absoluteLocalRoot, repository, commit, filePath);
        int key = gf.hash(commit, filePath);
        if (!files.containsKey(key)) {
            files.put(key, gf);
        }
        return files.get(key).getFilePath();
    }

    public String getFile(String filePath) {
        return getFile(filePath, this.commit);
    }

    public JSONObject getJSONObject(String filePath, String commit) {
        try {
            return new JSONObject(FileUtils.readFileToString(new File(getFile(filePath, commit)), "utf-8"));
        } catch (Exception e) {
            fail(String.format("Could not read %s(%s..) to a JSONObject.", filePath, commit.substring(0, 7)));
            return null;
        }
    }

    public JSONObject getJSONObject(String filePath) {
        return getJSONObject(filePath, this.commit);
    }

    private static int file_count = 0;

    public String createJSONFile(JSONObject put, String name) {
        TempFile temp = new TempFile(absoluteLocalRoot,
                new SimpleDateFormat("yyyyMMdd_HHmmss_").format(new Date()) + name + "_" + (file_count++) + ".json");
        temp.write(put.toString(3));
        int key = temp.hash();
        if (!files.containsKey(key)) {
            files.put(key, temp);
        }
        return files.get(key).getFilePath();
    }

    private static class TempFile {

        private String folder = "";
        private String file = "";

        public TempFile(String folder, String file) {
            this.folder = folder;
            this.file = file;
        }

        public TempFile() {
        }

        public String getFilePath() {
            return Paths.get(folder, file).toString();
        }

        protected static String resolvePath(String path) {
            return path.replace("./", "").replace("/", "\\");
        }

        protected static void createDirectory(Path directory) {
            if (!Files.exists(directory) && !new File(directory.toString()).mkdirs()) {
                fail("Could not create directory " + directory.toString());
            }
        }

        protected void createFile() {

            Path file = Paths.get(getFilePath());

            createDirectory(file.getParent());

            if (!Files.exists(file) && !Files.isDirectory(file)) {
                try {
                    Files.createFile(file);
                } catch (IOException e) {
                    fail("Could not create file " + file.toString());
                }
            }
        }

        protected boolean exists() {
            return Files.exists(Paths.get(getFilePath()));
        }

        public void write(String content) {
            try (FileWriter fw = new FileWriter(new File(getFilePath()))) {
                fw.write(content);
                fw.flush();
            } catch (IOException e) {
                log.error(e.getMessage());
                fail("Cannot write to file '%s'", getFilePath());
            }
        }

        public int hash() {
            return Objects.hash(getFilePath());
        }
    }

    private static class GitFile extends TempFile {

        private final String root;
        private final String repository;
        private final String commit;
        private final String relative_file_path;

        public GitFile(String root, String repository, String commit, String relative_file_path) {
            super();
            this.root = root;
            this.repository = resolvePath(repository);
            this.commit = commit;
            this.relative_file_path = resolvePath(relative_file_path);

            if (!exists()) {
                createFile();
                fetchFile();
            }
        }

        public int hash(String commit, String file) {
            return Objects.hash(commit, resolvePath(file));
        }

        @Override
        public String getFilePath() {
            return Paths.get(root, repository, commit, relative_file_path).toString();
        }

        private void fetchFile() {
            URL url;
            URLConnection con;
            DataInputStream dis;
            FileOutputStream fos;
            File targetFile;
            byte[] fileData;
            try {
                url = new URL(String.format("https://raw.githubusercontent.com/%s/%s/%s", this.repository, this.commit,
                        this.relative_file_path).replace("\\", "/"));
                con = url.openConnection(); // open the url connection.
                dis = new DataInputStream(con.getInputStream());
                fileData = new byte[con.getContentLength()];
                for (int q = 0; q < fileData.length; q++) {
                    fileData[q] = dis.readByte();
                }
                dis.close(); // close the data input stream
                targetFile = new File(getFilePath());
                fos = new FileOutputStream(targetFile);
                fos.write(fileData); // write out the file we want to save.
                fos.close(); // close the output stream writer
                success("Downloading file '%s' from '%s'", url.getPath(), this.repository);
            } catch (IOException e) {
                fail("Could not download file " + toString());
            }
        }

        @Override
        public String toString() {
            return String.format("https://github.com/%s/blob/%s/%s", this.repository, this.commit,
                    this.relative_file_path);
        }
    }
}
