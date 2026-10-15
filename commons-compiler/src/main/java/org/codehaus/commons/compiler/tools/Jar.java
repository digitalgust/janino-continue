package org.codehaus.commons.compiler.tools;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * A command line archive tool that mimics the OLD-STYLE options of the JDK
 * "jar" tool (entry names are always encoded as UTF-8, like
 * maven-jar-plugin):
 *
 * <pre>
 * java org.codehaus.commons.compiler.tools.Jar {ctx}[vf0me] [jarfile] [manifest] [entrypoint] [-C dir] inputs...
 * </pre>
 *
 * <table>
 *   <tr><td>c</td><td>create the archive</td></tr>
 *   <tr><td>t</td><td>list the table of contents</td></tr>
 *   <tr><td>x</td><td>extract the (optionally named) entries</td></tr>
 *   <tr><td>v</td><td>verbose output</td></tr>
 *   <tr><td>0</td><td>store only, without compression</td></tr>
 *   <tr><td>f</td><td>the archive file name follows, in flag order</td></tr>
 *   <tr><td>m</td><td>a manifest file name follows (create only)</td></tr>
 *   <tr><td>e</td><td>the application entry point follows (create only; adds "Main-Class")</td></tr>
 * </table>
 *
 * Examples:
 * <pre>
 *   Jar cf out.jar -C classes . -C resources res
 *   Jar cfe app.jar com.acme.Main -C classes .
 *   Jar tf out.jar
 *   Jar xf out.jar com/acme/Main.class
 * </pre>
 *
 * Each "-C &lt;dir&gt;" switches the base directory for the input names that
 * follow it; a directory input is added recursively, "." adds the entire
 * current base directory.
 */
public final
class Jar {

    private Jar() {}

    public static void
    main(String[] args) {
        try {
            System.exit(new Jar().run(args) ? 0 : 1);
        } catch (IOException ioe) {
            System.err.println("jar: I/O error: " + ioe.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("jar: " + e.getMessage());
            printUsage(System.err);
            System.exit(2);
        }
    }

    private boolean
    run(String[] args) throws Exception {

        if (args.length == 0) throw new Exception("operation required");

        String flags = args[0];
        if (flags.startsWith("-")) {
            flags = flags.substring(1); // tolerate a leading "-", e.g. "-cf"
        }

        boolean create = false, list = false, extract = false, verbose = false, store = false, fileGiven = false, manifestGiven = false, entryGiven = false;
        int i = 1;
        String jarfile = null, manifest = null, entryPoint = null;
        Set<Character> seenFlags = new HashSet<Character>();
        for (char c : flags.toCharArray()) {
            if (!seenFlags.add(c)) throw new Exception("duplicate flag: " + c);
            switch (c) {
                case 'c': create     = true; break;
                case 't': list       = true; break;
                case 'x': extract    = true; break;
                case 'v': verbose    = true; break;
                case '0': store      = true; break;
                case 'f': fileGiven = true; jarfile = arg(args, i++, "jarfile"); break;
                case 'm': manifestGiven = true; manifest = arg(args, i++, "manifest"); break;
                case 'e': entryGiven = true; entryPoint = arg(args, i++, "entry point"); break;
                default:  throw new Exception("invalid flag: " + c);
            }
        }
        if ((create ? 1 : 0) + (list ? 1 : 0) + (extract ? 1 : 0) != 1) {
            throw new Exception("exactly one of 'c', 't', 'x' required");
        }

        if (!fileGiven) throw new Exception("this tool requires 'f' and a jar file name");
        if (!create && (manifestGiven || entryGiven || store)) throw new Exception("'m', 'e', '0' require create");

        File archive = new File(jarfile);

        if (list)  return listContents(archive, tail(args, i), verbose);
        if (extract) return extractArchive(archive, tail(args, i), verbose);

        // CREATE.
        List<String[]> inputs = new ArrayList<String[]>(); // { baseDir, name }
        File curDir = new File(".");
        for (; i < args.length; i++) {
            if (args[i].equals("-C")) {
                curDir = new File(arg(args, ++i, "-C directory"));
            } else {
                inputs.add(new String[] { curDir.getPath(), args[i] });
            }
        }
        if (inputs.isEmpty()) throw new Exception("no input files given");

        List<Object[]> entries = new ArrayList<Object[]>(); // { entryName, File }
        for (String[] in : inputs) {
            collect(new File(in[0]), in[1], entries);
        }
        // Validate names and read the manifest before opening/truncating the output.
        String archivePath = realPath(archive);
        if (manifest != null && archivePath.equals(realPath(new File(manifest)))) {
            throw new IOException("archive and manifest refer to the same file");
        }
        byte[] manifestBytes = manifest(manifest, entryPoint);
        Set<String> names = new HashSet<String>();
        names.add("META-INF/");
        names.add("META-INF/MANIFEST.MF");
        List<Object[]> unique = new ArrayList<Object[]>();
        for (Object[] e : entries) {
            String name = ((String) e[0]).replace('\\', '/');
            while (name.startsWith("./")) name = name.substring(2);
            e[0] = name;
            validateName(name);
            if (archivePath.equals(realPath((File) e[1]))) continue;
            if (!names.add(name)) {
                if (name.endsWith("/") || name.equals("META-INF/MANIFEST.MF")) continue;
                throw new IOException("duplicate entry: " + name);
            }
            unique.add(e);
        }
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(new FileOutputStream(archive), 1 << 16));
        int fileCount = 0;
        try {
            writeBytes(zos, "META-INF/", new byte[0], store);
            writeBytes(zos, "META-INF/MANIFEST.MF", manifestBytes, store);
            for (Object[] e : unique) {
                String name = (String) e[0];
                File   f    = (File)   e[1];
                if (name.endsWith("/")) {  // directory entry - no content
                    writeBytes(zos, name, new byte[0], store);
                    if (verbose) System.out.println("added dir: " + name);
                    continue;
                }
                ZipEntry ze = new ZipEntry(name);
                if (store) {
                    writeBytes(zos, name, readFile(f), true);
                } else {
                    ze.setMethod(ZipEntry.DEFLATED);
                    zos.putNextEntry(ze);
                    copy(new FileInputStream(f), zos);
                }
                if (!store) zos.closeEntry();
                fileCount++;
                if (verbose) System.out.println("added: " + name + " (" + f.length() + " bytes)");
            }
            zos.finish();
            } finally {
            zos.close();
        }
        if (verbose) System.out.println(fileCount + " file(s) archived into " + jarfile);
        return true;
    }

    private boolean
    listContents(File archive, List<String> patterns, boolean verbose) throws IOException {
        java.util.zip.ZipFile zf = new java.util.zip.ZipFile(archive);
        try {
            java.util.Enumeration<? extends ZipEntry> en = zf.entries();
            int n = 0;
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!matches(patterns, e.getName())) continue;
                System.out.println((verbose ? e.getSize() + "  " : "") + e.getName());
                n++;
            }
            if (verbose) System.out.println(n + " entr" + (n == 1 ? "y" : "ies") + " listed");
            return true;
        } finally {
            zf.close();
        }
    }

    private boolean
    extractArchive(File archive, List<String> patterns, boolean verbose) throws IOException {
        java.util.zip.ZipFile zf = new java.util.zip.ZipFile(archive);
        try {
            File root = new File(".").getCanonicalFile();
            java.util.Enumeration<? extends ZipEntry> en = zf.entries();
            int n = 0;
            while (en.hasMoreElements()) {
                ZipEntry e = en.nextElement();
                if (!matches(patterns, e.getName())) continue;
                validateName(e.getName());
                File out = new File(root, e.getName().replace('\\', '/'));
                checkInside(root, out);
                if (e.isDirectory()) {
                    if (!out.isDirectory() && !out.mkdirs()) throw new IOException("cannot create directory: " + out);
                    if (verbose) System.out.println("created: " + e.getName());
                    continue;
                }
                File parent = out.getParentFile();
                if (parent != null && !parent.isDirectory() && !parent.mkdirs()) throw new IOException("cannot create directory: " + parent);
                checkInside(root, out);
                OutputStream os = new BufferedOutputStream(new FileOutputStream(out), 1 << 16);
                try {
                    copy(zf.getInputStream(e), os);
                } finally {
                    os.close();
                }
                if (verbose) System.out.println("inflated: " + e.getName());
                n++;
            }
            if (verbose) System.out.println(n + " file(s) extracted");
            return true;
        } finally {
            zf.close();
        }
    }

    /** Entry names are RELATIVE to their base directory; directories are added recursively. */
    private void
    collect(File baseDir, String name, List<Object[]> entries) throws IOException {
        if (name.equals(".") || name.equals("*")) {
            File[] fs = baseDir.listFiles();
            if (fs == null) throw new IOException("cannot list " + baseDir);
            List<File> sorted = new ArrayList<File>();
            for (File f : fs) sorted.add(f);
            Collections.sort(sorted, new Comparator<File>() {
                @Override public int compare(File a, File b) { return a.getName().compareTo(b.getName()); }
            });
            for (File f : sorted) collect(baseDir, f.getName(), entries);
            return;
        }
        File f = new File(baseDir, name);
        if (f.isDirectory()) {
            entries.add(new Object[] { name.endsWith("/") ? name : name + "/", f });
            File[] fs = f.listFiles();
            if (fs == null) throw new IOException("cannot list " + f);
            List<File> sorted = new ArrayList<File>();
            for (File c : fs) sorted.add(c);
            Collections.sort(sorted, new Comparator<File>() {
                @Override public int compare(File a, File b) { return a.getName().compareTo(b.getName()); }
            });
            for (File c : sorted) collect(baseDir, name + "/" + c.getName(), entries);
        } else {
            if (!f.isFile()) throw new IOException("no such file: " + f);
            entries.add(new Object[] { name, f });
        }
    }

    private byte[]
    manifest(String manifestFile, String entryPoint) throws IOException {
        // miniJVM's java.util.jar.Manifest cannot serialize or retain named sections yet.
        // Unfold physical lines first, then rewrite the main attributes and preserve sections.
        String text = manifestFile == null ? "Created-By: Commons Compiler Jar\r\n" : unfoldManifest(readFile(new File(manifestFile)));
        List<List<String>> sections = new ArrayList<List<String>>();
        List<String> section = new ArrayList<String>();
        sections.add(section);
        for (String line : text.split("\r\n|\n|\r", -1)) {
            if (line.length() == 0) {
                if (!section.isEmpty()) { section = new ArrayList<String>(); sections.add(section); }
            } else if (line.charAt(0) == ' ') {
                if (section.isEmpty()) throw new IOException("invalid manifest continuation");
                int last = section.size() - 1;
                section.set(last, section.get(last) + line.substring(1));
            } else {
                int colon = line.indexOf(':');
                if (colon < 1 || colon > 70 || colon + 1 >= line.length() || line.charAt(colon + 1) != ' '
                    || !line.substring(0, colon).matches("[A-Za-z0-9_-]+")) throw new IOException("invalid manifest header: " + line);
                section.add(line);
            }
        }
        List<String> main = sections.get(0);
        String version = "Manifest-Version: 1.0";
        for (int j = main.size() - 1; j >= 0; j--) {
            String line = main.get(j);
            String key = line.substring(0, line.indexOf(':'));
            if (key.equalsIgnoreCase("Manifest-Version")) { version = main.remove(j); }
            else if (entryPoint != null && key.equalsIgnoreCase("Main-Class")) main.remove(j);
        }
        main.add(0, version);
        if (entryPoint != null) {
            if (entryPoint.indexOf('\n') >= 0 || entryPoint.indexOf('\r') >= 0) throw new IOException("invalid entry point");
            main.add("Main-Class: " + entryPoint);
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int j = 0; j < sections.size(); j++) {
            List<String> lines = sections.get(j);
            if (lines.isEmpty()) continue;
            if (j > 0 && !lines.get(0).regionMatches(true, 0, "Name: ", 0, 6)) throw new IOException("manifest section missing Name");
            for (String line : lines) {
                byte[] bytes = line.getBytes("UTF-8");
                int offset = 0;
                while (offset < bytes.length) {
                    int count = Math.min(offset == 0 ? 72 : 71, bytes.length - offset);
                    if (offset > 0) out.write(' ');
                    out.write(bytes, offset, count);
                    out.write('\r'); out.write('\n');
                    offset += count;
                }
            }
            out.write('\r'); out.write('\n');
        }
        return out.toByteArray();
    }

    private static String unfoldManifest(byte[] bytes) throws IOException {
        // A physical line may end in the middle of a UTF-8 character. Unfold bytes
        // before decoding, just as java.util.jar.Manifest does.
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int lineStart = 0;
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] == '\r' || bytes[i] == '\n') {
                int next = i + 1;
                if (bytes[i] == '\r' && next < bytes.length && bytes[next] == '\n') next++;
                if (next < bytes.length && bytes[next] == ' ') {
                    if (i == lineStart) throw new IOException("invalid manifest continuation");
                    lineStart = next;
                    i = next;
                } else {
                    out.write('\n');
                    lineStart = next;
                    i = next - 1;
                }
            } else {
                out.write(bytes[i]);
            }
        }
        return new String(out.toByteArray(), "UTF-8");
    }

    private static boolean
    matches(List<String> patterns, String name) {
        if (patterns.isEmpty()) return true;
        for (String p : patterns) {
            p = p.replace('\\', '/');
            if (p.equals(name) || name.startsWith(p.endsWith("/") ? p : p + "/")) return true;
        }
        return false;
    }

    private static byte[]
    readFile(File f) throws IOException {
        InputStream in = new FileInputStream(f);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    private static void writeBytes(ZipOutputStream zos, String name, byte[] bytes, boolean store) throws IOException {
        ZipEntry e = new ZipEntry(name);
        if (store) {
            CRC32 crc = new CRC32();
            crc.update(bytes);
            e.setMethod(ZipEntry.STORED);
            e.setSize(bytes.length);
            e.setCompressedSize(bytes.length);
            e.setCrc(crc.getValue());
        }
        zos.putNextEntry(e);
        zos.write(bytes);
        zos.closeEntry();
    }

    private static void validateName(String name) throws IOException {
        String normalized = name.replace('\\', '/');
        if (normalized.length() == 0 || normalized.startsWith("/") || normalized.indexOf(':') >= 0 || normalized.indexOf('\0') >= 0) {
            throw new IOException("unsafe ZIP entry: " + name);
        }
        for (String part : normalized.split("/")) {
            if (part.equals("..")) throw new IOException("unsafe ZIP entry: " + name);
        }
    }

    private static void checkInside(File root, File out) throws IOException {
        String base = realPath(root);
        if (!base.endsWith(File.separator)) base += File.separator;
        if (!realPath(out).startsWith(base)) throw new IOException("ZIP entry escapes extraction directory: " + out);
    }

    private static String realPath(File file) throws IOException {
        try {
            // JDK 8 File.getCanonicalPath() on Windows does not resolve junctions.
            // Access NIO reflectively because miniJVM supplies File but not Path.
            java.lang.reflect.Method toPath = File.class.getMethod("toPath");
            List<String> suffix = new ArrayList<String>();
            File existing = file;
            while (!existing.exists()) {
                suffix.add(existing.getName());
                existing = existing.getParentFile();
                if (existing == null) throw new IOException("cannot resolve path: " + file);
            }
            Object options = java.lang.reflect.Array.newInstance(Class.forName("java.nio.file.LinkOption"), 0);
            Object path = Class.forName("java.nio.file.Path").getMethod("toRealPath", options.getClass())
                .invoke(toPath.invoke(existing), new Object[] { options });
            File resolved = new File(path.toString());
            for (int i = suffix.size() - 1; i >= 0; i--) resolved = new File(resolved, suffix.get(i));
            return resolved.getPath();
        } catch (NoSuchMethodException e) {
            return file.getCanonicalPath();
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new IOException("cannot resolve path: " + file, e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new IOException("cannot resolve path: " + file, e);
        }
    }

    private static void
    copy(InputStream in, OutputStream out) throws IOException {
        byte[] b = new byte[65536];
        int n;
        try {
            while ((n = in.read(b)) >= 0) out.write(b, 0, n);
        } finally {
            in.close();
        }
    }

    private static String
    arg(String[] args, int i, String what) throws Exception {
        if (i >= args.length) throw new Exception("missing " + what + " argument");
        return args[i];
    }

    private static List<String>
    tail(String[] args, int from) {
        List<String> l = new ArrayList<String>();
        for (int i = from; i < args.length; i++) l.add(args[i]);
        return l;
    }

    private static void
    printUsage(java.io.PrintStream ps) {
        ps.println("Usage: java org.codehaus.commons.compiler.tools.Jar {ctx}[vf0me] [jarfile] [manifest] [entrypoint] [-C dir] inputs...");
        ps.println("  c create | t list contents | x extract;  v verbose;  0 store only;");
        ps.println("  f jarfile follows;  m manifest file follows (create);  e Main-Class follows (create)");
        ps.println("Examples:");
        ps.println("  Jar cf out.jar -C classes . -C resources res");
        ps.println("  Jar cfe app.jar com.acme.Main -C classes .");
        ps.println("  Jar tvf out.jar");
    }
}
