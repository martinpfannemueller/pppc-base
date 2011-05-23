/*
 * Created on Mar 23, 2005
 
 */
package info.pppc.base.eclipse.generator.template;

import info.pppc.base.eclipse.Plugin;

import java.io.PrintWriter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaModelException;

/**
 * Base class of all templates. Templates are used to generate output to a given
 * print output stream.
 * 
 * @author handtems
 */
public abstract class AbstractTemplate {
	
	/**
	 * The base invocation exception.
	 */
	public static final String CLASS_EXCEPTION = Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.exception");
	
	/**
	 * The base serializable interface.
	 */
	public static final String CLASS_SERIALIZABLE = Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.serializable");

	/**
	 * The base result object for synchronous calls.
	 */
	public static final String CLASS_RESULT = Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.result");
	
	/**
	 * The base result object for asynchronous calls.
	 */
	public static final String CLASS_FUTURE = Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.future");

	/**
	 * The base invocation object.
	 */
	public static final String CLASS_INVOCATION = Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.invocation");

	/**
	 * The class name of the stream handler interface that must be treated seprately.
	 */
	public static final String CLASS_STREAM_HANDLER = Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.stream.handler");
	
	/**
	 * The class name of the stream descriptor that must be generated sometimes.
	 */
	public static final String CLASS_STREAM_DESCRIPTOR = Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.stream.descriptor");

	/**
	 * The class name of the stream descriptor that must be generated sometimes.
	 */
	public static final String CLASS_STREAM_CONNECTOR = Plugin.getDefault().getResourceString("info.pppc.base.eclipse.class.stream.connector");

	/**
	 * The full qualified class name of the class or interface to
	 * create.
	 */
	protected String classname = null;
	
	/**
	 * The full qualified class names of interfaces that should be
	 * extended or implemented. 
	 */
	protected String[] interfaces = null;

	/**
	 * The full qualified class names of the imports that should
	 * be imported.
	 */
	protected String[] imports = null;
	
	/**
	 * The full qualified class names of the base class that should
	 * be extended (ignored for interfaces).
	 */
	protected String baseclass = null;

	/**
	 * A flag that indicates whether the class is abstract. This flag
	 * is ignored if an interface is generated.
	 */
	protected boolean abstracted = false;
	
	/**
	 * The output stream to write to.
	 */
	protected PrintWriter out = null;
	
	/**
	 * The progress monitor used to report the progress of the class
	 * generation.
	 */
	protected IProgressMonitor monitor = null;
	
	/**
	 * The source object that defines the contents of the class.
	 */
	protected Object source = null;
	
	/**
	 * The java project used to perform type lookups.
	 */
	protected IJavaProject project = null;
	
	/**
	 * Creates a new template. 
	 */
	public AbstractTemplate(){	
		super();
	}
	
	/**
	 * Initializes the template with the specified class name, output stream,
	 * monitor and a user-defined source object. Typically this method will 
	 * be overwritten by user code and it will first call the super implementation,
	 * then it will configure the generation process.
	 * 
	 * @param project The java project used to perform source lookups.
	 * @param source The source object that delivers the model. The supported
	 * 	type depends on the type of template.
	 * @param classname The class name of the java class or interface that is
	 * 	created by the template. Note that this is the full qualified name 
	 * 	including packets (separated by dots).
	 * @param out The stream to write to.
	 * @param monitor The monitor that informs the user about the progress.
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	public void init(IJavaProject project, Object source, String classname, 
		PrintWriter out, IProgressMonitor monitor) throws JavaModelException {
		this.classname = classname;
		this.out = out;
		this.source = source;
		this.monitor = monitor;
		this.project = project;
	}
		
	/**
	 * Returns the package name for the full qualified class name specified
	 * using java source code syntax (dot separated).
	 * 
	 * @return The package name or null if the package would be the default
	 * 	pacakge.
	 */
	protected String getPackageName() {
		if (classname != null && classname.indexOf('.') != -1) {
			int separate = classname.lastIndexOf('.');
			return classname.substring(0, separate);
		} else {
			return null;
		}		
	}
	
	/**
	 * Returns the simple class name for the full qualified class name 
	 * specified using java source code syntax (dot separated). 
	 * 
	 * @return The simple class name.
	 */
	protected String getClassName() {
		if (classname != null && classname.indexOf('.') != -1) {
			int separate = classname.lastIndexOf('.');
			if (separate + 1 < classname.length()) {
				return classname.substring(separate + 1, classname.length());	
			} else {
				return "";
			}
		} else {
			return classname;
		}
	}
	
	/**
	 * Writes the java code for the specified imports to the specified 
	 * output stream.
	 *
	 * @throws JavaModelException Thrown by eclipse if the operation fails. 
	 */
	protected void writeImports() throws JavaModelException {
		if (imports != null && imports.length > 0) {
			for (int i = 0; i < imports.length; i++) {
				out.println("import " + imports[i] + ";");
			}			
		}
	}
	
	/**
	 * Writes the java code for the package definition for the specified
	 * classname.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writePackage() throws JavaModelException {
		String pack = getPackageName();
		if (pack != null) {
			out.println("package " + pack + ";");
		}
	}
	
	/**
	 * Writes the default interface comment.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writeInterfaceComment() throws JavaModelException {
		out.println("/**");
		out.println(" * Do not modify this file. This interface has been generated.");
		out.println(" * Use inheritance or composition to add functionality.");
		out.println(" *");
		out.println(" * @author 3PC Base Tools");
		out.println(" */");
	}
	
	/**
	 * Writes the java code for the header of an interface of the specified
	 * classname that extends the specified interfaces to the ouput stream.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writeInterfaceHeader() throws JavaModelException {
		writeInterfaceComment();
		if (interfaces != null && interfaces.length > 0) {
			out.print("public interface " + getClassName() + " extends ");
			for (int i = 0; i < interfaces.length - 1; i++) {
				out.print(interfaces[i] + ", ");
			}
			out.println(interfaces[interfaces.length - 1] + " {");
		} else {
			out.println("public interface " + getClassName() + " {");
		}
	}
	
	/**
	 * Writes the footer for an interface.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writeInterfaceFooter() throws JavaModelException {
		out.println("}");
	}
	
	/**
	 * Writes the default class comment.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writeClassComment() throws JavaModelException {
		out.println("/**");
		out.println(" * Do not modify this file. This class has been generated.");
		out.println(" * Use inheritance or composition to add functionality.");
		out.println(" *");
		out.println(" * @author 3PC Base Tools");
		out.println(" */");		
	}
	
	/**
	 * Writes the java code for the header of a class of the specified classname
	 * that extends the specified base class and implements the specified
	 * interfaces to the output stream.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writeClassHeader() throws JavaModelException {
		writeClassComment();
		out.print("public ");
		if (abstracted) {
			out.print("abstract ");
		}
		if (baseclass != null) {
			out.print("class " + getClassName() + " extends " + baseclass + " ");			
		} else {
			out.print("class " + getClassName() + " ");
		}
		if (interfaces != null && interfaces.length > 0) {
			out.print("implements ");
			for (int i = 0; i < interfaces.length - 1; i++) {
				out.print(interfaces[i] + ", ");
			}
			out.println(interfaces[interfaces.length - 1] + " {");
		} else {
			out.println("{");
		}		
	}

	/**
	 * Writes the footer for a class.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */	
	protected void writeClassFooter() throws JavaModelException {
		out.println("}");
	}

	/**
	 * Writes the default constructor for the specified class.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writeDefaultConstructor() throws JavaModelException {
		out.println("/**");
		out.println(" * Default constructor to create a new object.");
		out.println(" */");
		out.println("public " + getClassName() + "() { }");
	}


	/**
	 * Creates the contents of a java class file. This method
	 * creates the package definition, the specified imports,
	 * the class header, the user supplied body and the footer. 
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writeClass() throws JavaModelException {
		writePackage();
		out.println();
		writeImports();
		out.println();
		writeClassHeader();
		out.println();
		writeBody();
		out.println();
		writeClassFooter();		
	}
	
	/**
	 * Creates the contents of a java interface file. This method
	 * creates the specified imports, the package definition,
	 * the class header, the user supplied body and the footer.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writeInterface() throws JavaModelException {
		writeImports();
		writePackage();
		writeInterfaceHeader();
		writeBody();
		writeInterfaceFooter();
	}
	
	/**
	 * Called to write the contents of the java class or java
	 * interface.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	public abstract void write() throws JavaModelException;
	
	/**
	 * Called to write the body of the class.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected abstract void writeBody() throws JavaModelException;
		

		


}
