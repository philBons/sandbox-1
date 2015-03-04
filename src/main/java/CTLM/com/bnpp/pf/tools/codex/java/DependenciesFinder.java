package CTLM.com.bnpp.pf.tools.codex.java;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import CTLM.com.bnpp.pf.tools.codex.java.bean.DependencyLight;
import CTLM.com.bnpp.pf.tools.codex.java.util.ProcessStarter;

public class DependenciesFinder {

    private static final Logger LOGGER = Logger.getLogger(DependenciesFinder.class);

    private String treePath;
    // private DependencyLight baseArt;
    private List<DependencyLight> dls;

    // public static void main(String[] args) {
    //
    // DependenciesFinder df = new DependenciesFinder();
    // List<String> findings = df.launch();
    //
    // df.convertFindingsToDL(findings);
    // }

    public DependenciesFinder() {
        dls = new ArrayList<DependencyLight>();
    }

    public void reset() {
        dls.clear();
        treePath = null;
    }

    public List<DependencyLight> getDependenciesforEntry(String entry) {

        long startTime = System.currentTimeMillis();

        this.treePath = CodexStarter.getProperties().getProperty(CodexStarter.PRJ_PATH) + entry;
        List<String> lsart = this.launch();
        this.convertFindingsToDL(lsart);

        LOGGER.info(dls.size() + " dependencies found");

        LOGGER.info("Dependency for " + entry + "resolved in " + (System.currentTimeMillis() - startTime) + " ms");

        return dls;
    }

    private void convertFindingsToDL(List<String> findings) {

        for (String fdg : findings) {
            dls.add(convert(fdg));
        }
        // this.traceTarzan();
    }

    private List<String> launch() {

        String prj_path = this.treePath;
        String mvn_command = CodexStarter.getProperties().getProperty("MVN_COMMAND");

        LOGGER.info("USING: mvn_command " + mvn_command + " \tprj_path " + prj_path);

        OutputStream os = new ByteArrayOutputStream();

        ProcessStarter pl = new ProcessStarter(os, os);

        try {
            pl.exec(mvn_command, null, new File(prj_path));
        } catch (IOException e) {
            e.printStackTrace();
        }

        String sout = os.toString();

        // System.out.println(sout);

        StringTokenizer st = new StringTokenizer(sout, "\n");
        List<String> depList = new ArrayList<String>();
        boolean go = false;

        while (st.hasMoreTokens()) {
            String toto = st.nextToken();
            // System.out.print("> " + toto);
            if (go == true) {
                String t1 = toto.replace("[INFO]", "").trim();
                if (t1.endsWith("compile")) {
                    depList.add(t1);
                }
            }
            if (toto.startsWith("[INFO] The following files have been resolved:")) { // on commence à la prochaine ligne
                go = true;
            }
        }
        // System.out.println("#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=");
        // for (String s1 : depList) {
        // System.out.println("# " + s1);
        // }
        // System.out.println(depList.size() + " #=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=#=");
        return depList;

    }

    private DependencyLight convert(String s) {

        DependencyLight toRtn = null;

        String s1 = s.trim();

        if (s1.isEmpty() == false) {

            toRtn = this.string2Art(s1);
        } else {
            // une ligne vide et on arrete les frais..
            // this.letsgo = false;
        }
        if (toRtn == null) {
            // TODO remonter une exception ?
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("DependencyLight NULL for : s" + s);
            }
        } else {
            // System.out.println(toRtn + toRtn.toString());
        }

        return toRtn;
    }

    public int countStuff(String str, char ch) {
        int counter = 1;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == ch) {
                counter++;
            }
        }
        return counter;
    }

    // TODO : A mutualiser avec le code similaire dans ComponentMapper
    private DependencyLight string2Art(String s) {
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
        if (toRtn.getGoal().equalsIgnoreCase("compile") == false) {
            toRtn = null;
        }

        return toRtn;
    }

    private void traceTarzan() {
        // System.out.println(this.baseArt.toString());
        for (DependencyLight dl : this.dls) {
            if (dl != null) {
                System.out.println(dl.toString());
            }
        }
        System.out.println("-> " + this.dls.size());
    }

    public String getTreePath() {
        return treePath;
    }

    public void setTreePath(String treePath) {
        this.treePath = treePath;
    }

    public List<DependencyLight> getDls() {
        return dls;
    }

    public void setDls(List<DependencyLight> dls) {
        this.dls = dls;
    }

}
