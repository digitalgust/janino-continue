package org.codehaus.commons.compiler.tools;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.commons.compiler.CompileException;
import org.codehaus.commons.compiler.CompilerFactoryFactory;
import org.codehaus.commons.compiler.ICompiler;
import org.codehaus.commons.compiler.ICompilerFactory;
import org.codehaus.commons.compiler.ErrorHandler;
import org.codehaus.commons.compiler.Location;
import org.codehaus.commons.compiler.util.resource.ResourceCreator;

/**
 * A command line compiler front end that mimics a useful subset of the JDK
 * "javac" tool, on top of whichever {@link ICompiler} implementation is
 * discoverable on the class path (e.g. "janino.jar"):
 *
 * <pre>
 * java org.codehaus.commons.compiler.tools.Javac &lt;options&gt; &lt;sources...&gt;
 * </pre>
 *
 * Options (javac-compatible subset):
 * <table>
 *   <tr><td>-d &lt;dir&gt;</td><td>output directory for generated class files</td></tr>
 *   <tr><td>-cp | -classpath | --class-path &lt;path&gt;</td><td>class search path, '{@link File#pathSeparatorChar}'-separated</td></tr>
 *   <tr><td>-bootclasspath &lt;path&gt;</td><td>bootstrap class path</td></tr>
 *   <tr><td>-sourcepath &lt;path&gt;</td><td>where to find referenced source files</td></tr>
 *   <tr><td>-encoding &lt;name&gt;</td><td>source file encoding (e.g. UTF-8)</td></tr>
 *   <tr><td>-source &lt;release&gt;</td><td>language release (e.g. 8)</td></tr>
 *   <tr><td>-target &lt;release&gt;</td><td>class file release (e.g. 8)</td></tr>
 *   <tr><td>--release &lt;release&gt;</td><td>sets both -source and -target</td></tr>
 *   <tr><td>-g | -g:none | -g:lines,vars,source</td><td>debug information</td></tr>
 *   <tr><td>-verbose</td><td>verbose compiler output</td></tr>
 *   <tr><td>-nowarn</td><td>accepted and ignored</td></tr>
 *   <tr><td>-version | --version</td><td>print version and exit</td></tr>
 *   <tr><td>-help | -h | --help</td><td>print usage and exit</td></tr>
 *   <tr><td>@&lt;file&gt;</td><td>read additional arguments from &lt;file&gt; (one per line)</td></tr>
 * </table>
 *
 * Arguments naming {@code .java} files are compiled; arguments naming DIRECTORIES
 * are scanned recursively for {@code .java} files (a convenience over javac).
 *
 * On compilation errors, ALL erroneous compilation units are reported in javac
 * style ("<i>file:line:col error: message</i>"), and the exit code is 1.
 */
public final
class Javac {

    private Javac() {}

    public static void
    main(String[] args) {
        try {
            System.exit(new Javac().run(args) ? 0 : 1);
        } catch (IOException ioe) {
            System.err.println("javac: I/O error: " + ioe.getMessage());
            System.exit(1);
        } catch (Exception e) {
            System.err.println("javac: " + e.getMessage());
            System.exit(2);
        }
    }

    private boolean
    run(String[] args) throws Exception {

        List<File>  sources      = new ArrayList<File>();
        List<File> classPath     = new ArrayList<File>();
        List<File> bootClassPath = new ArrayList<File>();
        List<File> sourcePath    = new ArrayList<File>();
        File        destDir      = null;
        String      encoding     = null;
        int         sourceVersion = -2, targetVersion = -2;
        boolean     verbose = false, debugLines = true, debugVars = false, debugSource = true;
        boolean parameters = false;

        List<String> expanded = expandAtFiles(args);

        for (int i = 0; i < expanded.size(); i++) {
            String a = expanded.get(i);
            if (a.equals("-d")) {
                destDir = new File(expect(expanded, ++i, a));
            } else
            if (a.equals("-cp") || a.equals("-classpath") || a.equals("--class-path")) {
                addPath(classPath, expect(expanded, ++i, a));
            } else
            if (a.equals("-bootclasspath")) {
                addPath(bootClassPath, expect(expanded, ++i, a));
            } else
            if (a.equals("-sourcepath")) {
                addPath(sourcePath, expect(expanded, ++i, a));
            } else
            if (a.equals("-encoding")) {
                encoding = expect(expanded, ++i, a);
            } else
            if (a.equals("-source")) {
                sourceVersion = parseRelease(expect(expanded, ++i, a), a);
            } else
            if (a.equals("-target")) {
                targetVersion = parseRelease(expect(expanded, ++i, a), a);
            } else
            if (a.equals("--release")) {
                int r = parseRelease(expect(expanded, ++i, a), a);
                sourceVersion = r;
                targetVersion = r;
            } else
            if (a.startsWith("-g")) {
                String spec = a.equals("-g") ? "" : a.substring(2); // "", ":none", ":lines,vars,source"
                if (spec.startsWith(":")) spec = spec.substring(1);
                if (spec.equals("none")) {
                    debugLines = debugVars = debugSource = false;
                } else
                if (spec.length() > 0) {
                    debugLines = debugVars = debugSource = false;
                    for (String s : spec.split(",")) {
                        if (s.equals("lines")) debugLines = true; else
                        if (s.equals("vars"))  debugVars  = true; else
                        if (s.equals("source")) debugSource = true; else
                        throw new Exception("invalid flag: -g:" + s);
                    }
                }
            } else
            if (a.equals("-parameters")) {
                parameters = true;
            } else
            if (a.equals("-verbose")) {
                verbose = true;
            } else
            if (a.equals("-nowarn") || a.equals("-proc:none") || a.equals("-XDcompilePolicy=simple")) {
                // accepted and ignored
            } else
            if (a.equals("-version") || a.equals("--version")) {
                System.out.println("javac 1.8.0/commons-compiler-" + version());
                return true;
            } else
            if (a.equals("-help") || a.equals("-h") || a.equals("--help")) {
                printUsage(System.out);
                return true;
            } else
            if (a.startsWith("-")) {
                throw new Exception("invalid flag: " + a);
            } else {
                sources.add(new File(a));
            }
        }

        if (sources.isEmpty()) {
            printUsage(System.err);
            return false;
        }

        // Without an explicit -bootclasspath, JANINO's default looks for classes through the
        // Java 9+ module system, which does not exist on a Java 8 host; fall back to the
        // JVM's boot class path property there. (On miniJVM, always pass -bootclasspath.)
        if (bootClassPath.isEmpty()) {
            String sbcp = System.getProperty("sun.boot.class.path");
            if (sbcp != null) addPath(bootClassPath, sbcp);
        }

        // Expand directory arguments to their contained *.java files (sorted for determinism).
        List<File> units = new ArrayList<File>();
        boolean inferSourcePath = sourcePath.isEmpty();
        for (File f : sources) {
            if (f.isDirectory()) {
                if (inferSourcePath) sourcePath.add(f);
                collectJavaFiles(f, units);
            } else units.add(f);
        }
        java.util.Collections.sort(units);

        ICompilerFactory factory = CompilerFactoryFactory.getDefaultCompilerFactory(Javac.class.getClassLoader());
        if (factory == null) {
            System.err.println("javac: no compiler factory on the class path - add e.g. \"janino.jar\"");
            return false;
        }

        // Pass 1: compile all units in one go (allows cross-unit dependencies).
        final List<String> reported = new ArrayList<String>();
        final List<CompileException> errors = new ArrayList<CompileException>();
        ErrorHandler handler = new ErrorHandler() {
            @Override public void handleError(String message, Location location) {
                CompileException e = new CompileException(message, location);
                errors.add(e);
                if (!reported.contains(keyOf(e))) reportError(e, reported);
            }
        };
        try {
            ICompiler compiler = configure(
                factory.newCompiler(), destDir, encoding, classPath, bootClassPath, sourcePath,
                sourceVersion, targetVersion, verbose, debugLines, debugVars, debugSource
            );
            compiler.setCompileErrorHandler(handler);
            compiler.setParameters(parameters);
            compiler.compile(files(units));
            if (verbose) System.out.println("[javac] " + units.size() + " compilation unit(s) compiled");
            return true;
        } catch (CompileException first) {
            if (errors.isEmpty() || first.getLocation() != null) {
                errors.add(first);
                if (!reported.contains(keyOf(first))) reportError(first, reported);
            }
        }

        // Retry the remaining units together, retaining their cross-unit dependencies.
        // Diagnostic rounds never create class files or change subsequent lookup results.
        List<File> remaining = new ArrayList<File>(units);
        while (!remaining.isEmpty()) {
            boolean removed = false;
            for (int j = remaining.size() - 1; j >= 0; j--) {
                for (CompileException error : errors) {
                    Location location = error.getLocation();
                    if (location != null && location.getFileName() != null
                        && remaining.get(j).getCanonicalFile().equals(new File(location.getFileName()).getCanonicalFile())) {
                        remaining.remove(j);
                        removed = true;
                        break;
                    }
                }
            }
            if (!removed || remaining.isEmpty()) break;
            int previousErrors = errors.size();
            try {
                ICompiler compiler = configure(
                    factory.newCompiler(), destDir, encoding, classPath, bootClassPath, sourcePath,
                    sourceVersion, targetVersion, verbose, debugLines, debugVars, debugSource
                );
                compiler.setCompileErrorHandler(handler);
                compiler.setParameters(parameters);
                compiler.setClassFileCreator(new ResourceCreator() {
                    @Override public java.io.OutputStream createResource(String name) {
                        return new java.io.OutputStream() {
                            @Override public void write(int b) {}
                            @Override public void write(byte[] b, int off, int len) {}
                        };
                    }
                    @Override public boolean deleteResource(String name) { return true; }
                });
                compiler.compile(files(remaining));
                break;
            } catch (CompileException e) {
                if (errors.size() == previousErrors || e.getLocation() != null) {
                    errors.add(e);
                    if (!reported.contains(keyOf(e))) reportError(e, reported);
                }
            }
        }
        System.err.println(reported.size() + " error" + (reported.size() == 1 ? "" : "s"));
        return false;
    }

    private ICompiler
    configure(
        ICompiler          c,
        File               destDir,
        String             encoding,
        List<File>         classPath,
        List<File>         bootClassPath,
        List<File>         sourcePath,
        int                sourceVersion,
        int                targetVersion,
        boolean            verbose,
        boolean            debugLines,
        boolean            debugVars,
        boolean            debugSource
    ) {
        // Existing destination classes may satisfy dependencies outside the input set.
        // Explicit input units are always compiled together, including diagnostic rounds.
        List<File> cp = new ArrayList<File>(classPath);
        if (destDir != null) cp.add(0, destDir);

        c.setClassPath(files(cp));
        c.setBootClassPath(files(bootClassPath));
        c.setSourcePath(sourcePath.isEmpty()
            ? (classPath.isEmpty() ? new File[] { new File(".") } : files(classPath))
            : files(sourcePath));
        if (destDir != null) c.setDestinationDirectory(destDir, true);
        if (encoding != null) c.setCharacterEncoding(encoding);
        if (sourceVersion > 0) c.setSourceVersion(sourceVersion);
        if (targetVersion > 0) c.setTargetVersion(targetVersion);
        c.setVerbose(verbose);
        c.setDebugLines(debugLines);
        c.setDebugVars(debugVars);
        c.setDebugSource(debugSource);
        return c;
    }

    private static void
    reportError(CompileException e, List<String> reported) {
        String loc = e.getLocation() == null ? "" : (e.getLocation().getFileName() + ":" + e.getLocation().getLineNumber() + ":" + e.getLocation().getColumnNumber() + " ");
        String msg = loc + "error: " + messageOf(e);
        System.err.println(msg);
        reported.add(keyOf(e));
    }

    private static String
    keyOf(CompileException e) {
        org.codehaus.commons.compiler.Location l = e.getLocation();
        return (l == null ? "?" : l.getFileName() + ":" + l.getLineNumber() + ":" + l.getColumnNumber()) + ":" + messageOf(e);
    }

    private static String
    messageOf(Throwable t) {
        StringBuilder sb = new StringBuilder();
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (sb.length() > 0) sb.append(": ");
            sb.append(c.getMessage() == null ? c.getClass().getName() : c.getMessage());
        }
        return sb.toString();
    }

    private static int
    parseRelease(String s, String flag) throws Exception {
        try {
            String version = s.startsWith("1.") && !flag.equals("--release") ? s.substring(2) : s;
            int release = Integer.parseInt(version);
            if (release < 1) throw new NumberFormatException();
            return release;
        } catch (NumberFormatException nfe) {
            throw new Exception("invalid " + flag + " release: " + s);
        }
    }

    private static String
    expect(List<String> args, int i, String flag) throws Exception {
        if (i >= args.size()) throw new Exception("missing argument after " + flag);
        return args.get(i);
    }

    private static void
    addPath(List<File> result, String path) {
        for (String p : path.split(String.valueOf(File.pathSeparatorChar))) {
            p = p.trim();
            if (p.length() > 0) result.add(new File(p));
        }
    }

    private static File[]
    files(List<File> l) { return (File[]) l.toArray(new File[l.size()]); }

    private static void
    collectJavaFiles(File dir, List<File> result) {
        File[] entries = dir.listFiles(new FileFilter() {
            @Override public boolean accept(File f) { return f.isDirectory() || f.getName().endsWith(".java"); }
        });
        if (entries == null) return;
        for (File f : entries) {
            if (f.isDirectory()) collectJavaFiles(f, result); else result.add(f);
        }
    }

    private static List<String>
    expandAtFiles(String[] args) throws IOException {
        List<String> result = new ArrayList<String>();
        for (String a : args) {
            if (a.startsWith("@")) {
                File       f   = new File(a.substring(1));
                FileInputStream in = new FileInputStream(f);
                try {
                    byte[] b = new byte[(int) f.length()];
                    int off = 0, n;
                    while (off < b.length && (n = in.read(b, off, b.length - off)) >= 0) off += n;
                    for (String line : new String(b, 0, off).split("\r?\n")) {
                        line = line.trim();
                        if (line.length() > 0 && !line.startsWith("#")) result.add(line);
                    }
                } finally {
                    in.close();
                }
            } else {
                result.add(a);
            }
        }
        return result;
    }

    private static String
    version() {
        Package p = Javac.class.getPackage();
        return p != null && p.getImplementationVersion() != null ? p.getImplementationVersion() : "dev";
    }

    private static void
    printUsage(java.io.PrintStream ps) {
        ps.println("Usage: java org.codehaus.commons.compiler.tools.Javac <options> <sources...>");
        ps.println("Options (javac-compatible subset):");
        ps.println("  -d <dir>                         output directory for class files");
        ps.println("  -cp | -classpath | --class-path <path>");
        ps.println("                                   class search path, '" + File.pathSeparatorChar + "'-separated");
        ps.println("  -bootclasspath <path>            bootstrap class path");
        ps.println("  -sourcepath <path>               where to find referenced source files");
        ps.println("  -encoding <name>                 source file encoding (e.g. UTF-8)");
        ps.println("  -parameters                      emit reflection parameter names (target 8+)");
        ps.println("  -source <release> | -target <release> | --release <release>");
        ps.println("                                   language / class file release (e.g. 8)");
        ps.println("  -g | -g:none | -g:lines,vars,source");
        ps.println("                                   debug information (default: lines+source)");
        ps.println("  -verbose | -nowarn               verbosity switches");
        ps.println("  -version | -help                 version / this help");
        ps.println("  @<file>                          read additional arguments from <file>");
        ps.println("Sources are .java files, or directories scanned recursively for .java files.");
    }
}
