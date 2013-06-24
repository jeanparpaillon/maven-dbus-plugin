package net.windwards.mavendbusplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.freedesktop.dbus.bin.CreateInterface;
import org.freedesktop.dbus.exceptions.DBusException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintStream;

/**
 * Transform a DBus interface XML into Java sources. This maven plugin is an
 * interface on CreateInterface from dbus-java. Tested with dbus-java version
 * 2.8.
 *
 * @goal generate
 * @phase generate-sources
 */
public class DBusXMLMojo extends AbstractMojo {

    public static class FileStreamFactory extends CreateInterface.PrintStreamFactory {
        File targetdir;

        public FileStreamFactory(String targetdir) {
            this.targetdir = new File(targetdir);
        }

        public void init(String file, String path) {
            new File(targetdir, path).mkdirs();
        }

        public PrintStream createPrintStream(final String file) throws IOException {
            File output = new File(targetdir, file);
            return new PrintStream(new FileOutputStream(output));
        }
    }

    /**
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;

    /**
     * The source directory containing *.xml files.
     *
     * @parameter default-value="${basedir}/src/main/dbus"
     */
    private String interfaces;

    /**
     * The directory to output the generated sources to.
     *
     * @parameter default-value="${project.build.directory}/generated-sources/dbus
     */
    private File output;

    protected FilenameFilter xmlFiles = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            boolean accept = name.toLowerCase().endsWith(".xml");
            if (accept)
                return true;

            getLog().warn("Ignoring non-xml file " + name);
            return false;
        }
    };

    public void execute() throws MojoExecutionException {
        String path = output.getPath();
        this.project.addCompileSourceRoot(path);

        File ifacedir = new File(interfaces);

        if(!ifacedir.exists()) {
            getLog().warn("Directory does not exist: " + interfaces);
            return;
        }
        File[] interfaceFiles = ifacedir.listFiles(xmlFiles);

        if (interfaceFiles.length == 0) {
            getLog().warn("There are no interfaces in " + interfaces);
            return;
        }

        for(File iface : interfaceFiles) {
            FileStreamFactory factory = new FileStreamFactory(path);
            CreateInterface createInterface = new CreateInterface(factory, false);
            FileReader input = null;
            try {
                input = new FileReader(iface);
            } catch (IOException e) {
                throw new MojoExecutionException("Could not read interface " +
                        "XML from " + iface.getPath(), e);
            }
            try {
                createInterface.createInterface(input);
            } catch (ParserConfigurationException e) {
                throw new MojoExecutionException("dbus-java internal error on XML parser", e);
            } catch (SAXException e) {
                throw new MojoExecutionException("Malformatted interface XML", e);
            } catch (IOException e) {
                throw new MojoExecutionException("Failed to write java source", e);
            } catch (DBusException e) {
                throw new MojoExecutionException("DBus error", e);
            }
        }
    }
}