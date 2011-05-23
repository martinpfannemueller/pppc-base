/*
 * Revision: $Revision: 1.2 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2006/07/02 18:34:21 $ 
 */
package info.pppc.base.eclipse.generator;

import info.pppc.base.eclipse.Plugin;
import info.pppc.base.eclipse.generator.io.FormattingWriter;
import info.pppc.base.eclipse.generator.template.ProxyTemplate;
import info.pppc.base.eclipse.generator.template.SkeletonTemplate;
import info.pppc.base.eclipse.generator.util.JavaUtility;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;

/**
 * The service generator can be used to create base proxies and skeletons.
 * A modifier can be used to determine the types of output to generate.
 * The generator itself uses templates to generate the java source code.
 * 
 * @author Mac
 */
public class Generator {

	/**
	 * A modifier that can be used to generate a proxy.
	 */
	public static final int MODIFIER_PROXY = 1;
	
	/**
	 * A modifier that can be used to generate a skeleton.
	 */
	public static final int MODIFIER_SKELETON = 2;

	/**
	 * A modifier that can be used to generate a secure proxy.
	 */
	public static final int MODIFIER_SECURE_PROXY = 4;
	
	/**
	 * A modifier that can be used to generate a skeleton.
	 */
	public static final int MODIFIER_SECURE_SKELETON = 8;
	
	/**
	 * The progress monitor used to report any progress.
	 */
	protected IProgressMonitor monitor;
	
	/**
	 * The type of the generator.
	 */
	protected IType type;
	
	/**
	 * The java project of the generator.
	 */
	protected IJavaProject project;

	/**
	 * Creates a new service generator.
	 */
	public Generator() {
		super();
	}
	
	/**
	 * Initializes the service generator with the specified java
	 * project, type and progress monitor.
	 * 
	 * @param project The java project used for reflection.
	 * @param type The type to generate.
	 * @param monitor A progress monitor to display status messages.
	 */
	public void init(IJavaProject project, IType type, IProgressMonitor monitor) {
		this.monitor = monitor;
		this.type = type;
		this.project = project;
	}
	
	/**
	 * Generates the files for the specified modifiers. A call to
	 * this method will overwrite any existing file.
	 * 
	 * @param modifiers The modifiers that configure the generation
	 * 	process.
	 * @throws JavaModelException Thrown if the generation fails.
	 */
	public void generate(int modifiers) throws JavaModelException {
		// determine the number of 1 flags
		int steps = 0;
		int mask = 1;
		for (int i = 0; i < 32; i++) {
			if ((mask & modifiers ) != 0) {
				steps++;
			}
			mask = mask << 1;
		}
		monitor.beginTask("Generating ...", steps);
		if ((modifiers & MODIFIER_PROXY) != 0) {
			monitor.subTask("Generating proxy ...");
			// initialize buffer
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(bos);
			FormattingWriter fow = new FormattingWriter(osw);
			PrintWriter pw = new PrintWriter(fow);
			// generate contents
			String classname = convert(type.getFullyQualifiedName()) + "Proxy";
			ProxyTemplate spt = new ProxyTemplate(Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.proxy"));
			spt.init(project, type, classname, pw, null);
			spt.write();
			pw.flush();
			pw.close();
			byte[] bytes = bos.toByteArray();
			String content = new String(bytes);
			// create java file
			JavaUtility.createJavaFile(project, classname, content);
			monitor.worked(1);
		}
		if ((modifiers & MODIFIER_SKELETON) != 0) {
			monitor.subTask("Generating skeleton ...");
			// initialize buffer
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(bos);
			FormattingWriter fow = new FormattingWriter(osw);
			PrintWriter pw = new PrintWriter(fow);
			// generate contents
			String classname = convert(type.getFullyQualifiedName()) + "Skeleton";
			SkeletonTemplate sst = new SkeletonTemplate(Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.skeleton"));
			sst.init(project, type, classname, pw, null);
			sst.write();
			pw.flush();
			pw.close();
			byte[] bytes = bos.toByteArray();
			String content = new String(bytes);
			// create java file
			JavaUtility.createJavaFile(project, classname, content);
			monitor.worked(1);							
		}
		if ((modifiers & MODIFIER_SECURE_PROXY) != 0) {
			monitor.subTask("Generating secure proxy ...");
			// initialize buffer
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(bos);
			FormattingWriter fow = new FormattingWriter(osw);
			PrintWriter pw = new PrintWriter(fow);
			// generate contents
			String classname = convert(type.getFullyQualifiedName()) + "SecureProxy";
			ProxyTemplate spt = new ProxyTemplate(Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.secureproxy"));
			spt.init(project, type, classname, pw, null);
			spt.write();
			pw.flush();
			pw.close();
			byte[] bytes = bos.toByteArray();
			String content = new String(bytes);
			// create java file
			JavaUtility.createJavaFile(project, classname, content);
			monitor.worked(1);
		}
		if ((modifiers & MODIFIER_SECURE_SKELETON) != 0) {
			monitor.subTask("Generating secure skeleton ...");
			// initialize buffer
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			OutputStreamWriter osw = new OutputStreamWriter(bos);
			FormattingWriter fow = new FormattingWriter(osw);
			PrintWriter pw = new PrintWriter(fow);
			// generate contents
			String classname = convert(type.getFullyQualifiedName()) + "SecureSkeleton";
			SkeletonTemplate sst = new SkeletonTemplate(Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.secureskeleton"));
			sst.init(project, type, classname, pw, null);
			sst.write();
			pw.flush();
			pw.close();
			byte[] bytes = bos.toByteArray();
			String content = new String(bytes);
			// create java file
			JavaUtility.createJavaFile(project, classname, content);
			monitor.worked(1);							
		}	
		
	}
 	
	/**
	 * Converts type names of interfaces that contain a trailing I to names
	 * that do not contain the I, e.g. base.IFoo --> base.Foo.  
	 * 
	 * @param name The name of the type to convert.
	 * @return The converted type name.
	 */
	protected String convert(String name) {
		// find package name and type name
		int start = name.lastIndexOf('.');
		String packageName = "";
		String typeName = name;
		if (start != -1) {
			packageName = name.substring(0, start + 1);
			typeName = name.substring(start + 1, name.length());			
		}
		if (typeName.startsWith("I") && typeName.length() > 1 
					&& Character.isUpperCase(typeName.charAt(1))) {
			typeName = typeName.substring(1, typeName.length());
		}
		return packageName + typeName;
	}
	
}
