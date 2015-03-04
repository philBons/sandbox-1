package CTLM.com.bnpp.pf.tools.codex.java.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

public class CopyEngine {

    private static final Logger LOGGER = Logger.getLogger(CopyEngine.class);

    private String src, dest;
    private File fDEST, fSRC;

    public CopyEngine(String src, String dest) {
        this.src = src;
        this.dest = dest;
        this.fSRC = new File(src);
        this.fDEST = new File(dest);
        // System.out.println("-> " + src + " => " + dest);
        // if (fSRC.exists() == false) {
        // System.out.println("\t-> " + src + " do not exist");
        // }
        // si le rep dest n'existe pas, et notre source est un repertoire
        if (!fDEST.exists()) {
            if (fSRC.isDirectory()) {
                // fDEST.mkdir();
                createDirInCascade(dest);
            }
        }
        // Mais si jammais c'est un fichier, on fait un simple copie
        if (fSRC.isFile()) {
            copyFile(fSRC, fDEST);

            // et si notre source est un repertoire qu'on doit copié!!!
        } else if (fSRC.isDirectory()) {
            // on parcours tout les elements de ce catalogue,
            for (File f : fSRC.listFiles()) {
                // et hop on fait un appel recursif a cette classe en mettant a jour les path de src et dest: et le tour
                // est joué
                new CopyEngine(f.getAbsolutePath(), fDEST.getAbsoluteFile() + "/" + f.getName());
                if (f.isFile()) {
                    copyFile(f, new File(fDEST.getAbsoluteFile() + "/" + f.getName()));
                }
            }
        }
    }

    /**
     * copie le fichier source dans le fichier resultat
     */
    private void copyFile(File src, File dest) {

        FileInputStream fis = null;
        FileOutputStream fos = null;
        // System.out.println("\t-> Into copyFile " + src + " => " + dest);
        try {
            fis = new FileInputStream(src);
            fos = new FileOutputStream(dest);

            java.nio.channels.FileChannel channelSrc = fis.getChannel();
            java.nio.channels.FileChannel channelDest = fos.getChannel();

            channelSrc.transferTo(0, channelSrc.size(), channelDest);

            fis.close();
            fos.close();
        } catch (java.io.FileNotFoundException f) {
            // System.out.println("\t-> Into copyFile fnfe " + src + " => " + dest + "\t" + f.getMessage());
            f.printStackTrace();
        } catch (java.io.IOException e) {
            // System.out.println("\t-> Into copyFile ioe " + src + " => " + dest + "\t" + e.getMessage());
            e.printStackTrace();
        } finally {
            try {
                fis.close();
            } catch (Exception e) { // do not matters
                // e.printStackTrace();
            }
            try {
                fos.close();
            } catch (Exception e) {
                // e.printStackTrace();
            }
        }
    }

    public static String createDirInCascade(String path) {
        // System.out.println("# Into createDirInCascade for : " + path);
        StringTokenizer st;
        if (path.contains("\\")) {
            st = new StringTokenizer(path, "\\");
        } else {
            st = new StringTokenizer(path, "/");
        }
        // st = new StringTokenizer(path, File.pathSeparator);
        String first = null;
        StringBuffer path2create = new StringBuffer();
        File f1 = null;
        while (st.hasMoreElements()) {
            if (first == null) {
                first = st.nextToken();
                path2create.append(first);
            } else {
                path2create.append("/" + st.nextToken());
                if (path2create.toString().startsWith(".")) { // Chemin relatif
                    f1 = new File(path2create.toString());
                } else { // chemin absolu, nécessaire au bon fonctionnement sur plateforme UNIX
                    f1 = new File("/" + path2create.toString());
                }
                f1.mkdir();
            }
        }
        return f1.getAbsolutePath().toString();

    }

}
