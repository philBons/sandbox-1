package CTLM.com.bnpp.pf.tools.codex.java;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;

import CTLM.com.bnpp.pf.tools.codex.java.bean.DependencyLight;
import CTLM.com.bnpp.pf.tools.codex.java.bean.TechnicalComponent;
import CTLM.com.bnpp.pf.tools.codex.java.util.CopyEngine;
import CTLM.com.bnpp.pf.tools.codex.java.util.GetJarEngine;

public class CodexStarter {

    /**
     * Correspond à l'entrée dans le fichier properties qui fournis le répertoire de base des composants techniques dans
     * la vue clearcase
     */
    public static final String PRJ_PATH = "PRJ_PATH";

    /**
     * Correspond à l'entrée dans le fichier properties qui fournis la la commande MVN à lancer pour effectuer le
     * dependency:list . <i> Est différente selon les OS, se trouve dans M2_HOME/bin </i>
     * 
     */
    public static final String MVN_COMMAND = "MVN_COMMAND";

    private static final Logger LOGGER = Logger.getLogger(CodexStarter.class);

    private static Properties properties;
    private static String appName = null;

    public static void main(String[] args) {

        initL4J();
        loadCodexProperties();

        if (args.length == 1) {
            if (args[0].equals("index")) {
                index();
            } else {
                // goOnFile(args[0]);
                printUsage();
            }
        } else if (args.length == 2) {
            if (args[1].equals("show")) {
                goOnFile(args[0], false);
            } else if (args[1].equals("extract")) {
                goOnFile(args[0], true);
            } else {
                printUsage();
            }
        } else {
            printUsage();
        }
    }

    private static void goOnFile(String fName, boolean go) {

        long startTime = System.currentTimeMillis();

        LOGGER.info("Startup de JavaCodex avec le fichier: " + fName);

        // On controle que le fichier existe
        if (fileCheck(fName) == false) {
            System.exit(0);
        }

        updateAppNameFromFileName(fName);

        // On lit le fichier et on controle que les entrÃ©es correspondent bien Ã  des composants techniques
        List<String> entriesList = null;
        try {
            entriesList = entriesCheck(fName);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (entriesList == null) {
            System.exit(0);
        }

        // On lance le component Mapper sur le fichier csv
        ComponentMapper cMapper = ComponentMapper.indexFromCSV();

        // On lance le DependenciesFinder sur chacun des Composants tech du fichier
        Set<TechnicalComponent> srcEntries = new HashSet<TechnicalComponent>();
        // Set<TechnicalComponent> srcDependencies;// = new HashSet<TechnicalComponent>();
        Set<DependencyLight> deps2Download = new HashSet<DependencyLight>();
        // Map<String, DependencyLight> deps2Download = new HashMap<String, DependencyLight>();

        DependenciesFinder myDF = new DependenciesFinder();

        for (String entry : entriesList) {

            TechnicalComponent srcEntry = cMapper.getTCForPath(entry);
            List<DependencyLight> deps = myDF.getDependenciesforEntry(entry);

            LOGGER.info("==========> " + srcEntry);

            srcEntries.add(srcEntry);
            // concat(deps2Download, deps);
            deps2Download.addAll(deps);

            myDF.reset();
        }

        // On lance un MapperService qui construit 3 listes de trucs à extraires...
        // 2 listes à copier, 1 liste élts à télécharger
        // Pour l'instant on a 2 set uniquement de remplis, il s'agit de remplir le 3ieme
        Set<TechnicalComponent> srcDependencies = cMapper.extractTCFtomDepList(deps2Download);

        // On copie et on télécharge ce qu'il y a dans les listes

        showAllList(srcEntries, srcDependencies, deps2Download);// new
                                                                // HashSet<DependencyLight>(deps2Download.values()));

        if (go) { // On copie les projets et binaires concernés

            // createDestDirs();
            String srcAbsoluteDir = CopyEngine.createDirInCascade("./" + appName + "/src");
            String depsSrcAbsoluteDir = CopyEngine.createDirInCascade("./" + appName + "/deps/src");
            String libsAbsoluteDir = CopyEngine.createDirInCascade("./" + appName + "/deps/lib");

            srcAbsoluteDir = srcAbsoluteDir.replace("./", "");
            depsSrcAbsoluteDir = depsSrcAbsoluteDir.replace("./", "");
            libsAbsoluteDir = libsAbsoluteDir.replace("./", "");

            srcAbsoluteDir = srcAbsoluteDir.replace(".\\", "");
            depsSrcAbsoluteDir = depsSrcAbsoluteDir.replace(".\\", "");
            libsAbsoluteDir = libsAbsoluteDir.replace(".\\", "");

            System.out.println(srcAbsoluteDir);
            System.out.println(depsSrcAbsoluteDir);
            System.out.println(libsAbsoluteDir);

            copySrcFromSetOfTC(srcEntries, srcAbsoluteDir);
            copySrcFromSetOfTC(srcDependencies, depsSrcAbsoluteDir);

            // GetJarEngine.downloadJarsFromDependencies(deps2Download.values(), libsAbsoluteDir);
            GetJarEngine.downloadJarsFromDependencies(deps2Download, libsAbsoluteDir);
        }

        LOGGER.info("End of Codex processing : Job done in  " + (System.currentTimeMillis() - startTime) + " ms");

    }

    private static void initL4J() {
        DOMConfigurator.configure("log4j.xml");
    }

    private static void concat(Map<String, DependencyLight> deps2Download, List<DependencyLight> deps) {

        for (DependencyLight dl : deps) {
            String key = dl.getKey4Map();
            if (deps2Download.get(key) == null) {
                deps2Download.put(key, dl);
            }
        }

    }

    private static void concat(Set<DependencyLight> deps2Download, List<DependencyLight> deps) {

        for (DependencyLight dl : deps) {
            if (deps2Download.contains(dl) == false) {
                deps2Download.add(dl);
            }
        }

    }

    private static void copySrcFromSetOfTC(Set<TechnicalComponent> setTC, String dest) {
        for (TechnicalComponent elt : setTC) {
            if (elt != null)
                new CopyEngine(elt.getPath(), dest + "/" + elt.getLastDirName());
        }
    }

    private static void showAllList(Set<TechnicalComponent> srcEntries, Set<TechnicalComponent> srcDependencies,
            Set<DependencyLight> deps2Download) {
        LOGGER.info("/src \t\t" + srcEntries.size());
        showSet(srcEntries);
        LOGGER.info("/deps/src \t\t" + srcDependencies.size());
        showSet(srcDependencies);
        LOGGER.info("/deps/bin\t\t" + deps2Download.size());
        showSet(deps2Download);
    }

    // private static String srcAbsoluteDir;
    // private static String depsSrcAbsoluteDir;
    // private static String libsAbsoluteDir;
    //
    // private static void createDestDirs() {
    // srcAbsoluteDir = CopyEngine.createDirInCascade("./" + appName + "/src");
    // depsSrcAbsoluteDir = CopyEngine.createDirInCascade("./" + appName + "/deps/src");
    // libsAbsoluteDir = CopyEngine.createDirInCascade("./" + appName + "/deps/lib");
    // }

    private static void showSet(Set s) {
        for (Object elt : s) {
            if (elt != null)
                LOGGER.info("\t" + elt.toString());
        }
    }

    private static List<String> entriesCheck(String fName) throws IOException {
        List<String> toRtn = new ArrayList<String>();
        List<String> entriesInError = new ArrayList<String>();
        BufferedReader elReader = null;
        String aLine;
        try {
            elReader = new BufferedReader(new FileReader(fName));
        } catch (FileNotFoundException fnfe) {
            System.out.println(fnfe); // ne devrait jamais arriver, on a fait un check avant
        }
        while ((aLine = elReader.readLine()) != null) {
            String depFName = aLine.trim();
            if (fileCheck(properties.getProperty(PRJ_PATH) + depFName + "/" + "pom.xml") == false) {
                entriesInError.add(depFName);
            } else {
                toRtn.add(depFName);
            }
        }
        if (elReader != null) {
            elReader.close();
        }
        if (entriesInError.isEmpty() == false) {
            LOGGER.info("Entries not available: ");
            for (String eie : entriesInError) {
                System.out.print("\t" + eie);
            }
            toRtn = null;
        }
        return toRtn;
    }

    // TODO A terme merger avec la méthode jsute au dessus
    private static boolean fileCheck(String fName) {
        File f = new File(fName);
        boolean toReturn;
        if (f.exists() == false) {
            LOGGER.info("Entry file " + fName + " can not be found");
            toReturn = false;
        } else if (f.canRead() == false) {
            LOGGER.info("Entry file " + fName + " can not be read");
            toReturn = false;
        } else {
            toReturn = true;
        }
        return toReturn;
    }

    private static void updateAppNameFromFileName(String fName) {
        File f = new File(fName);
        // baseDir = f.getParent();
        String n = f.getName();
        if (n.contains(".")) {
            appName = n.substring(0, n.indexOf("."));
        } else {
            appName = n;
        }
    }

    private static void index() {
        ComponentMapper.indexAll();
    }

    private static void printUsage() {
        System.out.println("USAGE: 2 modes:");
        System.out.println("fichier.txt  'options'->"
                + " where fichier is name of the functionalComponent and contains its entry points");
        System.out.println(" where options are:");
        System.out.println("\t show -> display the list of the Technical Components and Files used by this project");
        System.out.println("\t extract -> Technical Components and Files used by this project in a directory");
        System.out.println("index -> index all of the code assets available, might take some time.");
    }

    private static void loadCodexProperties() {
        try {
            load("javacodex.properties");
        } catch (FileNotFoundException e1) {
            e1.printStackTrace();
        } catch (IOException e1) {
            e1.printStackTrace();
        }
    }

    public static Properties load(String filename) throws IOException, FileNotFoundException {
        properties = new Properties();
        FileInputStream input = new FileInputStream(filename);
        try {
            properties.load(input);
            return properties;
        } finally {
            input.close();
        }
    }

    public static Properties getProperties() {
        if (properties == null) {
            loadCodexProperties();
        }
        return properties;
    }

}
