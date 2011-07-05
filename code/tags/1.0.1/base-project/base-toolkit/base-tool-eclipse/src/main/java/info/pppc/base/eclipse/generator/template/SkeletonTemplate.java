/*
 * Revision: $Revision: 1.2 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2007/08/29 13:55:30 $ 
 */
package info.pppc.base.eclipse.generator.template;

import info.pppc.base.eclipse.generator.util.JavaUtility;

import java.io.PrintWriter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;


/**
 * The template for base skeletons. It generates a dispatch method that
 * treats every method defined in the interface.
 * 
 * @author Mac
 */
public class SkeletonTemplate extends AbstractTemplate {

	/**
	 * The base skeleton class.
	 */
	private String CLASS_SKELETON;
	
	/**
	 * The methods that must be dispatched by the skeleton.
	 */
	protected IMethod[] methods = null;

	/**
	 * Creates a new service skeleton template.
	 * 
	 * @param base The base class of the skeleton.
	 */
	public SkeletonTemplate(String base) {
		CLASS_SKELETON = base;
	}
	
	/**
	 * Initializes the template with the specified class name, output stream,
	 * progress monitor and source object. The source object must be of
	 * type component contract.
	 * 
	 * @param project The java project to perform lookups.
	 * @param source A component contract.
	 * @param classname The class name of the contract to create.
	 * @param out The output stream to write to.
	 * @param monitor The monitor used to report progress.
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	public void init(IJavaProject project, Object source, String classname, PrintWriter out, IProgressMonitor monitor)
		throws JavaModelException {
		super.init(project, source, classname, out, monitor);
		baseclass = CLASS_SKELETON;
		IType type = (IType)source;
		methods = JavaUtility.mergeMethods(new IType[] { type});
	}

	/**
	 * Writes the body of the skeleton. This will create the dispatch 
	 * method and the corresponding helper methods to initalize the 
	 * component instance.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writeBody() throws JavaModelException {
		// write default constructor
		writeDefaultConstructor();
		out.println();
		// write dispatch method
		writeDispatchMethod();
	}	
	
	/**
	 * Creates the dispatch method for all application methods that need
	 * to be supported.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writeDispatchMethod() throws JavaModelException {
		out.println("/**");
		out.println(" * Dispatch method that dispatches incoming invocations to the skeleton's implementation.");
		out.println(" *");
		out.println(" * @param method The signature of the method to call.");
		out.println(" * @param args The parameters of the method call.");
		out.println(" * @return The result of the method call.");
		out.println(" */");
		IType type = (IType)source;
		String cinterface = type.getFullyQualifiedName();
		// create method signature
		out.println("protected " + CLASS_RESULT + 
			" dispatch(String method, Object[] args) {");
		// create method body
		out.println(cinterface + " impl = (" + cinterface + ")getImplementation();");
		// try for application exceptions
		out.println("try {");
		if (methods != null) {
			for (int i = 0; i < methods.length; i++) {
				
				IMethod current = methods[i];
				String declaration = current.getDeclaringType().getFullyQualifiedName();
				if (declaration != null && declaration.equals(CLASS_STREAM_HANDLER)) {
					writeDispatchMethodStream(methods[i], i == 0, i != methods.length - 1);
				} else {
					writeDispatchMethodSync(methods[i], i == 0, i != methods.length - 1);
				}
			}
		}
		// return unknown signature
		out.print("return new " + CLASS_RESULT + "(null, new ");
		out.println(CLASS_EXCEPTION + "(\"Illegal signature.\"));");
		// try catch block for application exceptions.
		out.println("} catch (Throwable t) {");
		out.println("return new " + CLASS_RESULT + "(null, t);");
		out.println("}");
		// create method footer
		out.println("}");		
	}
	
	/**
	 * Writes a fragment of the dispatch method that will dispatch the
	 * specified method.
	 * 
	 * @param method The method to dispatch.
	 * @param first A flag that indicates whether this is the first 
	 * 	method to generate.
	 * @param last A flag that indicates whether this is the last 
	 * 	method to generate.
	 * @throws JavaModelException Thrown by eclipse if the operation
	 * 	fails.
	 */
	protected void writeDispatchMethodSync(IMethod method, boolean first, 
			boolean last) throws JavaModelException{
		IType interfaceType = method.getDeclaringType();
		String signature = JavaUtility.generateMethodSignature(method, "", false, true);
		String returnType = JavaUtility.getQualifiedType(interfaceType, method.getReturnType());
		if (! first) {
			out.print("else ");
		}
		out.println("if (method.equals(\"" + signature + "\")) {");
		if (returnType.equals("V")) {
			out.println("Object result = null;");	
		} else {
			out.print("Object result = ");
		}
		// create method call
		String methodName = method.getElementName();
		String[] paramNames = method.getParameterNames();
		String[] paramTypes = method.getParameterTypes();
		String callName = "impl." + methodName + "(";		
		for(int i = 0; i < paramNames.length; i++){
			String qualified = JavaUtility.getQualifiedType(interfaceType, paramTypes[i]);
			callName += (JavaUtility.unboxVariable(qualified, "args["+i+"]"));
			if(i != paramNames.length - 1)
				callName += ", ";
		}
		callName+=")";
		out.println(JavaUtility.boxVariable(returnType, callName) + ";");		
		out.println("return new " + CLASS_RESULT + "(result, null);");
		out.print("}");
		if (last) {
			out.println();
		}
	}

	/**
	 * Writes a fragment of the dispatch method that will dispatch the
	 * specified method as incoming stream connection.
	 * 
	 * @param method The method to dispatch.
	 * @param first A flag that indicates whether this is the first 
	 * 	method to generate.
	 * @param last A flag that indicates whether this is the last 
	 * 	method to generate.
	 * @throws JavaModelException Thrown by eclipse if the operation
	 * 	fails.
	 */
	protected void writeDispatchMethodStream(IMethod method, boolean first, 
			boolean last) throws JavaModelException{
		IType interfaceType = method.getDeclaringType();
		String signature = JavaUtility.generateMethodSignature(method, "", false, true);
		String returnType = JavaUtility.getQualifiedType(interfaceType, method.getReturnType());
		if (! first) {
			out.print("else ");
		}
		out.println("if (method.equals(\"" + signature + "\")) {");
		if (returnType.equals("V")) {
			out.println("Object result = null;");	
		} else {
			out.print("Object result = ");
		}
		// create method call
		String methodName = method.getElementName();
		out.println(CLASS_STREAM_DESCRIPTOR + " __desc = new " + CLASS_STREAM_DESCRIPTOR + "();");
		out.println("__desc.setData(args[1]);");
		out.println("__desc.setConnector((" + CLASS_STREAM_CONNECTOR + ")args[0]);");
		String callName = "impl." + methodName + "(__desc);";		
		out.println(JavaUtility.boxVariable(returnType, callName) + ";");		
		out.println("return new " + CLASS_RESULT + "(result, null);");
		out.print("}");
		if (last) {
			out.println();
		}
	}
	
	
	/**
	 * Writes the component skeleton to the output stream.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	public void write() throws JavaModelException {
		writeClass();
	}


}
