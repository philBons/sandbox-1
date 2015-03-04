package CTLM.com.bnpp.pf.tools.codex.java;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;

import CTLM.com.bnpp.pf.tools.codex.java.bean.DependencyLight;
import CTLM.com.bnpp.pf.tools.codex.java.bean.TechnicalComponent;
import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import au.com.bytecode.opencsv.bean.ColumnPositionMappingStrategy;
import au.com.bytecode.opencsv.bean.CsvToBean;

public class ComponentMapper {

    // public static final String BASE_SRC_STUFF = "C:\\Work\\Java_dependencies\\vobs\\"; //
    // "C:\\Work\\Java_dependencies\\vobs\\";
    public static final String STORAGE_FILE = "POMJuice.csv";

    private static final Logger LOGGER = Logger.getLogger(ComponentMapper.class);

    private List<File> lstEffectivePom = new ArrayList<File>();

    private Map<String, TechnicalComponent> allTechComps = new HashMap<String, TechnicalComponent>();

    // public static void main(String[] args) {
    //
    // long startTime = System.currentTimeMillis();
    // ComponentMapper cm = new ComponentMapper();
    // // cm.readAllEffectivePOM(BASE_SRC_STUFF);
    // // cm.mapEffectiveApple();
    //
    // System.out.println("Done in " + (System.currentTimeMillis() - startTime) + " ms");
    // // cm.export2CSV();
    //
    // cm.retreiveFromCSVBrutal();
    // cm.traceALLForTest();
    //
    // System.out.println(" saveALL Done  " + (System.currentTimeMillis() - startTime) + " ms");
    // }

    public static void indexAll() {
        long startTime = System.currentTimeMillis();
        // System.out.println("Done in " + (System.currentTimeMillis() - startTime) + " ms");

        ComponentMapper cm = new ComponentMapper();
        cm.readAllEffectivePOM(CodexStarter.getProperties().getProperty("PRJ_PATH"));
        cm.mapEffectiveApple();
        cm.export2CSV();

        LOGGER.info("indexALL Done  " + (System.currentTimeMillis() - startTime) + " ms");
    }

    public static ComponentMapper indexFromCSV() {

        long startTime = System.currentTimeMillis();
        // System.out.println("Done in " + (System.currentTimeMillis() - startTime) + " ms");

        ComponentMapper cm = new ComponentMapper();
        cm.retreiveFromCSVBrutal();

        LOGGER.info("index retrieval from file Done  " + (System.currentTimeMillis() - startTime) + " ms");

        return cm;

    }

    public void export2CSV() {
        try {
            String outputfileName = STORAGE_FILE;
            FileWriter fw = new FileWriter(outputfileName, false);

            CSVWriter csvWriter = new CSVWriter(fw, ';');

            List<String[]> allList4csv = new ArrayList<String[]>();

            for (TechnicalComponent techC : this.allTechComps.values()) {
                String[] lineElts = generateLineEntry(techC);
                allList4csv.add(lineElts);
            }

            csvWriter.writeAll(allList4csv);

            csvWriter.flush();
            csvWriter.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void retreiveFromCSV() {

        File f = new File(STORAGE_FILE);
        FileReader fR = null;
        try {
            fR = new FileReader(f);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        CSVReader csvReader = new CSVReader(fR, ';');

        CsvToBean<TechnicalComponent> ctb = new CsvToBean<TechnicalComponent>();

        ColumnPositionMappingStrategy<TechnicalComponent> cpms = new ColumnPositionMappingStrategy<TechnicalComponent>();

        cpms.setType(TechnicalComponent.class);
        cpms.setColumnMapping(this.getMappingkeys());

        List<TechnicalComponent> allStuffs = ctb.parse(cpms, csvReader);

        // System.out.println("\t ->" + allStuffs.size());

    }

    public void retreiveFromCSVBrutal() {

        File f = new File(STORAGE_FILE);
        FileReader fR = null;
        try {
            fR = new FileReader(f);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        CSVReader csvReader = new CSVReader(fR, ';');
        // List<TechnicalComponent> allStuffs = new ArrayList<TechnicalComponent>();
        String[] aLine;
        try {
            while ((aLine = csvReader.readNext()) != null) {
                // System.out.println(aLine);
                // System.out.println(aLine[0]);
                if (aLine.length == 7) {

                    // {"key", "groupId", "artifactId", "version", "parent", "target", "path"};
                    TechnicalComponent newTC = new TechnicalComponent();
                    newTC.setKey(aLine[0]);
                    newTC.setGroupId(aLine[1]);
                    newTC.setArtifactId(aLine[2]);
                    newTC.setVersion(aLine[3]);
                    newTC.setParent(aLine[4]);
                    newTC.setTarget(aLine[5]);
                    newTC.setPath(aLine[6]);

                    // System.out.println(newTC.toString());
                    // allStuffs.add(newTC);

                    this.add2AllTechComps(newTC, newTC.getKey());

                }
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }

        LOGGER.info("Nb entrées dans l'indexation " + this.allTechComps.entrySet().size());

    }

    private int readAllEffectivePOM(String startDir) {

        int toRtn = 0;

        File fStart = new File(startDir);

        if (fStart.exists() == false) {
            System.out.println("L'emplacement de départ indiqué n'existe pas : " + startDir);
            return 0;
        }

        this.searchDir4EffectivePOM(fStart);

        toRtn = lstEffectivePom.size();

        LOGGER.info("Nb POM >\t" + toRtn);

        // System.out.println(lstEffectivePom);

        return toRtn;
    }

    private void mapEffectiveApple() {
        for (File ff : lstEffectivePom) {
            this.readEffectiveApple(ff);
        }
        this.updateOnceItIsDoneWithInheritance();
        // System.out.println("Nb entrée dans la map " + this.allTechComps.entrySet().size());

        traceALLForTest();

    }

    private void updateOnceItIsDoneWithInheritance() {
        for (TechnicalComponent tc : this.allTechComps.values()) {
            if ((tc.getGroupId() == null) || (tc.getVersion() == null)) {
                this.updateFromParent(tc);
                // System.out.println("\t" + tc.toString());
            }
            if (tc.getVersion() != null) {
                if (tc.getVersion().equalsIgnoreCase("${artifactversion}")) {
                    this.updateFromParent(tc);
                }
            }

        }
    }

    private void updateFromParent(TechnicalComponent tc) {
        // System.out.println(" <>>>>>>>>>>>> Into updateFromParent  for : " + tc.toString());
        String p = tc.getParent();
        if (p != null) {
            DependencyLight dlp = this.string2Art(p);
            if (tc.getGroupId() == null) {
                tc.setGroupId(dlp.getGroupId());
                tc.setKey(dlp.getGroupId() + ":" + tc.getArtifactId());
            }
            if ((tc.getVersion() == null)) {
                tc.setVersion(dlp.getVersion());
            } else {
                if (tc.getVersion().equalsIgnoreCase("${artifactversion}")) {
                    tc.setVersion(dlp.getVersion());
                }
            }
        }
    }

    private void searchDir4EffectivePOM(File fstart) {
        File[] rf = fstart.listFiles();
        for (File f : rf) {
            if (f.isDirectory()) {
                searchDir4EffectivePOM(f);
            } else if (f.isFile()) {
                if (f.getName().equalsIgnoreCase("pom.xml")) {
                    lstEffectivePom.add(f);
                }
            }
        }
    }

    private void readEffectiveApple(File pom) {
        try {

            Reader reader = new FileReader(pom);
            MavenXpp3Reader mxr = new MavenXpp3Reader();
            // Model model = mxr.read(reader);
            Model model = mxr.read(reader);

            String artifact = model.getArtifactId();
            String group = model.getGroupId();
            String v = model.getVersion();
            String longName = model.getName();
            String cible = model.getPackaging();

            // System.out.println("=> " + longName);
            // System.out.println("> artifactId: " + artifact + "\t> groupId : " + group + "\t> version : " + v
            // + "\t packaging : " + cible);
            // System.out.println("\t>Parent " + model.getParent());
            // String key = group+":"+artifact+":"+v;
            String shortKey = group + ":" + artifact;

            TechnicalComponent tc = new TechnicalComponent();
            tc.setArtifactId(artifact);
            tc.setGroupId(group);
            tc.setVersion(v);
            tc.setTarget(cible);
            tc.setKey(shortKey);
            tc.setPath(pom.getParent().replace("\\", "/"));// pom.getAbsolutePath();
            // System.out.println("\t>Path " + tc.getPath());

            if (model.getParent() != null) {
                tc.setParent(model.getParent().getGroupId() + ":" + model.getParent().getArtifactId() + ":" + "pom"
                        + ":" + model.getParent().getVersion());
            }

            System.out.println("----------------------------------------------");

            add2AllTechComps(tc, shortKey);

        } catch (Exception ex) {
            // TODO :gérer proprement les logs
            System.err.println(pom.getAbsolutePath());
            ex.printStackTrace();
        }
        // processAllTechComps();
    }

    private void traceALLForTest() {
        for (TechnicalComponent tc : this.allTechComps.values()) {
            System.out.println("> artifactId: " + tc.getArtifactId() + "\t> groupId : " + tc.getGroupId()
                    + "\t> version : " + tc.getVersion() + "\t packaging : " + tc.getTarget());
            System.out.println("\t> Parent " + tc.getParent());
            System.out.println("\t> Path " + tc.getPath());
            System.out.println("################################################");
        }
    }

    /**
     * @param tc
     * @param shortKey
     */
    private void add2AllTechComps(TechnicalComponent tc, String shortKey) {
        if (allTechComps.get(shortKey) == null) {
            allTechComps.put(shortKey, tc);
        } else { // Ne devrait jamais servir
            LOGGER.warn("Incohérence, nom déja existant : " + shortKey);
            if (allTechComps.get(shortKey + "__v1") == null) {
                allTechComps.put(shortKey + "__v1", tc);
            } else {
                allTechComps.put(shortKey + "__v2", tc);
            }
        }
    }

    private DependencyLight string2Art(String s) {
        // System.out.println("string2Art : " + s);
        StringTokenizer st = new StringTokenizer(s, ":");
        DependencyLight toRtn = new DependencyLight();
        int pos = 0;
        while (st.hasMoreTokens()) {
            if (pos == 0) {
                toRtn.setGroupId(st.nextToken());
            } else if (pos == 1) {
                toRtn.setArtifactId(st.nextToken());
            } else if (pos == 2) {
                toRtn.setType(st.nextToken());
            } else if (pos == 3) {
                toRtn.setVersion(st.nextToken());
            } else if (pos == 4) { // compile or provided or whatever
                toRtn.setGoal(st.nextToken());
            } else {
                st.nextToken();
            }
            pos++;
        }
        return toRtn;
    }

    private final String[] generateLineEntry(final TechnicalComponent tc) {
        return new String[] { tc.getKey(), tc.getGroupId(), tc.getArtifactId(), tc.getVersion(), tc.getParent(),
                tc.getTarget(), tc.getPath() };
    }

    private final String[] getMappingkeys() {
        return new String[] { "key", "groupId", "artifactId", "version", "parent", "target", "path" };
    }

    public List<File> getLstEffectivePom() {
        return lstEffectivePom;
    }

    public void setLstEffectivePom(List<File> lstEffectivePom) {
        this.lstEffectivePom = lstEffectivePom;
    }

    public Map<String, TechnicalComponent> getAllTechComps() {
        return allTechComps;
    }

    public void setAllTechComps(Map<String, TechnicalComponent> allTechComps) {
        this.allTechComps = allTechComps;
    }

    public TechnicalComponent getTCForPath(final String entry) {

        Collection<TechnicalComponent> ltc = this.allTechComps.values();
        String baseDir = CodexStarter.getProperties().getProperty(CodexStarter.PRJ_PATH);

        String toFind;
        if ((entry.startsWith("/") == false) && (baseDir.endsWith("/") == false)) {
            toFind = baseDir + "/" + entry;
        } else {
            toFind = baseDir + entry;
        }
        if (toFind.endsWith("/")) {
            toFind = toFind.substring(0, toFind.length() - 2);
        }
        toFind = toFind.replaceAll("//", "/");
        // TODO A optimiser - Violation CAST à supprimer sur le return
        for (TechnicalComponent tc : ltc) {
            if (tc.getPath().equals(toFind)) {
                return tc;
            }
        }
        return null;
    }

    public Set<TechnicalComponent> extractTCFtomDepList(Set<DependencyLight> sdl) {
        Set<TechnicalComponent> toRtn = new HashSet<TechnicalComponent>();
        List<DependencyLight> toRemove = new ArrayList<DependencyLight>();
        for (DependencyLight myDl : sdl) {
            TechnicalComponent tc2Add = this.allTechComps.get(myDl.toSimpleKey());
            if (tc2Add != null) {
                toRtn.add(tc2Add);
                toRemove.add(myDl);
            }
        }
        sdl.removeAll(toRemove);
        return toRtn;
    }

    public Set<TechnicalComponent> extractTCFtomDepList(Map<String, DependencyLight> sdl) {
        Set<TechnicalComponent> toRtn = new HashSet<TechnicalComponent>();
        // Map<String, DependencyLight> newSdl = new HashMap<String, DependencyLight>();
        List<String> toRemove = new ArrayList<String>();
        for (DependencyLight myDl : sdl.values()) {
            TechnicalComponent tc2Add = this.allTechComps.get(myDl.toSimpleKey());
            if (tc2Add != null) {
                toRtn.add(tc2Add);
                toRemove.add(myDl.getKey4Map());
            }
        }
        for (String dl : toRemove) {
            sdl.remove(dl);
        }
        return toRtn;
    }

}