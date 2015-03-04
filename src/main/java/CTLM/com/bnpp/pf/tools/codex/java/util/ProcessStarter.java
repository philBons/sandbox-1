package CTLM.com.bnpp.pf.tools.codex.java.util;

/*    
 *  ProccessLauncher is a tool to launch an extern application
 *  in a Java program with stream managed in separate threads.
 * 
 *  Copyright (C) 2006  Fabio MARAZZATO, Yann D'ISANTO
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Lancer une application externe en consommant les divers fluxs dans des threads separes.
 * 
 * @author a08960
 */
public class ProcessStarter {

    private OutputStream out = null;
    private OutputStream err = null;
    private InputStream in = null;
    private Process process;
    private long timeout = 0L;
    private boolean finished = false;

    public ProcessStarter() {
        this(null, null, null, 0L);
    }

    /**
     * @param out Outputstream vers lequel sera redirige la sortie standard de l'application externe (null pour ne pas
     *            rediriger).
     * @param err Outputstream vers lequel sera redirige la sortie d'erreur de l'application externe (null pour ne pas
     *            rediriger).
     */
    public ProcessStarter(OutputStream out, OutputStream err) {
        this(out, err, null, 0L);
    }

    /**
     * @param out Outputstream vers lequel sera redirige la sortie standard de l'application externe (null pour ne pas
     *            rediriger).
     * @param err Outputstream vers lequel sera redirige la sortie d'erreur de l'application externe (null pour ne pas
     *            rediriger).
     * @param in InputStream vers lequel sera redirige l'entree standard de l'application externe (null pour ne pas
     *            rediriger).
     * @param timeout temps en millisecondes avant de forcer l'arret de l'application externe (0 pour ne jamais forcer
     *            l'arret).
     */
    public ProcessStarter(OutputStream out, OutputStream err, InputStream in, long timeout) {
        this.out = out;
        this.err = err;
        this.in = in;
        this.timeout = timeout < 0 ? 0L : timeout;
    }

    /**
     * Execute une ligne de commande dans un processus separe en specifiant des variables d'environnement et le
     * repertoire de travail.
     * 
     * @param command ligne de commande
     * @param envp variables d'environnement
     * @param dir repertoire de travail
     * @return valeur de retour du processus
     */
    public int exec(String command, String[] envp, File dir) throws IOException {
        process = Runtime.getRuntime().exec(command, envp, dir);
        return execute();
    }

    /**
     * 
     * @throws IOException
     */
    private int execute() throws IOException {
        int status = -1;

        // Consommation des fluxs de sortie standard et d'erreur dans des threads separes.
        if (err == null) {
            process.getErrorStream().close();
        } else {
            createStreamThread(process.getErrorStream(), err);
        }
        if (out == null) {
            process.getInputStream().close();
        } else {
            createStreamThread(process.getInputStream(), out);
        }

        // Mapping de l'entree standard de l'application si besoin est.
        if (in != null) {
            createStreamThread(in, process.getOutputStream());
        }

        if (timeout > 0L) {
            Thread processThread = createProcessThread(process);
            processThread.start();
            try {
                processThread.join(timeout);
                try {
                    status = process.exitValue();
                } catch (IllegalThreadStateException itse) {
                    process.destroy();
                    status = process.exitValue();
                }
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        } else if (timeout == 0L) {
            try {
                status = process.waitFor();
            } catch (InterruptedException ie) {
                ie.printStackTrace();
            }
        }
        finished = true;
        return status;
    }

    /**
     * @param is
     * @param os
     */
    private void createStreamThread(final InputStream is, final OutputStream os) {
        new Thread(new Runnable() {
            public void run() {
                BufferedInputStream bis = new BufferedInputStream(is);
                BufferedOutputStream bos = null;
                if (os != null) {
                    bos = new BufferedOutputStream(os);
                }
                byte[] data = new byte[2048];
                int nbRead = 0;
                try {
                    while ((nbRead = bis.read(data)) > 0) {
                        if (bos != null) {
                            if (finished) {
                                break;
                            }
                            bos.write(data, 0, nbRead);
                            bos.flush();
                        }
                    }
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }).start();
    }

    /**
     * @param process
     */
    private Thread createProcessThread(final Process process) {
        return new Thread() {
            public void run() {
                try {
                    process.waitFor();
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
            }
        };
    }

    public OutputStream getErrorStream() {
        return err;
    }

    public InputStream getInputStream() {
        return in;
    }

    public OutputStream getOutputStream() {
        return out;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setErrorStream(OutputStream err) {
        this.err = err;
    }

    public void setInputStream(InputStream in) {
        this.in = in;
    }

    public void setOutputStream(OutputStream out) {
        this.out = out;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }
}