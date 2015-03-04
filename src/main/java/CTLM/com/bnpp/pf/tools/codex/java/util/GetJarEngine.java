package CTLM.com.bnpp.pf.tools.codex.java.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

import CTLM.com.bnpp.pf.tools.codex.java.bean.DependencyLight;

public class GetJarEngine {

    public static String URL_REPO_RELEASE = "http://intra-docs.telematique.ctlmcof.fr/maven/repository/releases"; // 1

    public static String URL_REPO_CTLM = "http://intra-docs.telematique.ctlmcof.fr/nexus/content/groups/cetelem"; // 2

    private static List<String> missingStuff = new ArrayList<String>();

    private static final Logger LOGGER = Logger.getLogger(GetJarEngine.class);

    private GetJarEngine() {
    }

    // public static void main(String[] args) {
    // if (args.length != 1) {
    // printUsage();
    // } else {
    // GetJarEngine gmj = new GetJarEngine();
    // gmj.targetPath = args[0];
    //
    // File f1 = new File(gmj.targetPath);
    // if (false == f1.exists()) {
    // printUsage();
    // System.exit(0);
    // }
    //
    // gmj.go();
    //
    // System.out.println("\n\n");
    //
    // traceMissings();
    //
    // }
    // }

    public static void downloadJarsFromDependencies(Set<DependencyLight> sdl, String path) {

        missingStuff.clear();

        for (DependencyLight jd : sdl) {
            String urlEnd = toUrlPath(jd);
            if (urlEnd.contains("CTLM")) {
                getFile(URL_REPO_RELEASE + urlEnd, path);
            } else {
                getFile(URL_REPO_CTLM + urlEnd, path);
            }
        }

        if (missingStuff.isEmpty() == false) {
            traceMissings();
        }

    }

    private static String toUrlPath(DependencyLight d) {
        return "/" + transformGroup4URL(d.getGroupId()) + "/" + d.getArtifactId() + "/" + d.getVersion() + "/"
                + d.getArtifactId() + "-" + d.getVersion() + ".jar";
    }

    private final static String transformGroup4URL(String group) {
        return group.replace(".", "/");
    }

    private static void traceMissings() {
        LOGGER.info("Dependencies not found: ");
        for (String s : missingStuff) {
            LOGGER.info("\t" + s);
        }
    }

    // private void go() {
    // BufferedReader br;
    // String ligne = "";
    // try {
    // br = new BufferedReader(new FileReader("URLS.txt"));
    //
    // while ((ligne = br.readLine()) != null) {
    // System.out.println("GET " + ligne);
    // this.getFile(ligne);
    // System.out.print(" \tDONE to targetPath " + targetPath);
    // System.out.println("----------------------");
    // }
    //
    // } catch (Exception e) {
    // missingStuff.add(ligne);
    // System.err.println("MISSING : " + ligne);
    // System.out.println("----------------------");
    // }
    // }

    // private static void printUsage() {
    // System.out.println(" Passer en argument le chemin de destination des jars: exemple c:\\temp\\pf\\lib\\ ");
    // System.out.println(" Attention: le répertoire doit être déjà crée! ");
    //
    // }

    public static void getFile(String urlpath, String targetPath) {
        InputStream input = null;
        FileOutputStream writeFile = null;

        try {
            URL url = new URL(urlpath);

            URLConnection connection = url.openConnection();

            connection.connect();

            int fileLength = connection.getContentLength();

            if (fileLength == -1) {
                LOGGER.warn("Invalide URL or file.");
                return;
            }

            File f1 = new File(targetPath);
            if (false == f1.exists()) {
                f1.mkdir();
            }
            // if (targetPath.endsWith("\\") == false) {
            // targetPath = targetPath + "\\";
            // }
            if (targetPath.endsWith("/") == false) {
                targetPath = targetPath + "/";
            }

            input = connection.getInputStream();
            String fileName = url.getFile().substring(url.getFile().lastIndexOf('/') + 1);
            File toRtn = new File(targetPath + fileName);
            writeFile = new FileOutputStream(toRtn);
            byte[] buffer = new byte[1024];
            int read;

            while ((read = input.read(buffer)) > 0)
                writeFile.write(buffer, 0, read);
            writeFile.flush();

            LOGGER.info(" \t" + toRtn.getAbsolutePath());
        } catch (IOException e) {
            LOGGER.error("Error while trying to download the file. " + urlpath);
            missingStuff.add(urlpath);
            // System.err.println(e.getMessage());

        } finally {
            try {
                if (writeFile != null) {
                    writeFile.close();
                }
                if (input != null) {
                    input.close();
                }
            } catch (IOException e) {
                // e.printStackTrace();

            }
        }
    }

    public static void downloadJarsFromDependencies(Collection<DependencyLight> values, String libsAbsoluteDir) {
        downloadJarsFromDependencies(new HashSet<DependencyLight>(values), libsAbsoluteDir);
    }
}
