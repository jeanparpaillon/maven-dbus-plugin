package net.windwards.mavendbusplugin;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.freedesktop.dbus.bin.CreateInterface;

import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;

/**
 * Says "Hi" to the user.
 *
 * @goal generate
 * @phase generate-sources
 */
public class DBusXMLMojo extends AbstractMojo {

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
    private File interfaces;


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

        for(File iface : interfaces.listFiles(xmlFiles)) {
            CreateInterface.PrintStreamFactory factory =
                    new CreateInterface.FileStreamFactory(path);
            CreateInterface createInterface = new CreateInterface(factory, false);
            try {
                createInterface.createInterface(new FileReader(iface));
            } catch (Exception e) {
                throw new MojoExecutionException("Failed to generate sources from interface description", e);
            }
        }
    }
}