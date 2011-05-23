/*
 * Revision: $Revision: 1.2 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2007/08/29 13:55:30 $ 
 */
package info.pppc.base.eclipse.generator.template;

import info.pppc.base.eclipse.generator.util.JavaUtility;

import java.io.PrintWriter;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;


/**
 * The template for base proxies. It generates a method that creates and sends an
 * invocation for each method defined by the interface.
 *  
 * @author Mac
 */
public class ProxyTemplate extends AbstractTemplate {

	/**
	 * The base proxy class.
	 */
	private String CLASS_PROXY;

	
	/**
	 * The methods that must be implemented by the proxy.
	 */
	protected IMethod[] methods = null;


	/**
	 * Creates a new service proxy template.
	 * 
	 * @param base The base class of the proxy.
	 */
	public ProxyTemplate(String base) {
		CLASS_PROXY = base; 
	}

	/**
	 * Initializes the template with the specified class name, output stream,
	 * progress monitor and source object. The source object must be of
	 * type component demand contract.
	 * 
	 * @param project The java project to perform lookups.
	 * @param source An itype that must be implemented.
	 * @param classname The class name of the contract to create.
	 * @param out The output stream to write to.
	 * @param monitor The monitor used to report progress.
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	public void init(IJavaProject project, Object source, String classname, 
			PrintWriter out, IProgressMonitor monitor) throws JavaModelException {
		super.init(project, source, classname, out, monitor);
		// initialize base class
		baseclass = CLASS_PROXY;
		// initialize interfaces and methods
		IType type = (IType)source;
		interfaces = new String[] { type.getFullyQualifiedName() };
		methods = JavaUtility.mergeMethods(new IType[] { type});
	}

	/**
	 * Writes the body of the proxy.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	protected void writeBody() throws JavaModelException {
		// write constructor
		writeDefaultConstructor();
		// write method proxies
		if (methods != null) {
			for (int i = 0; i < methods.length; i++) {
				out.println();
				IMethod current = methods[i];
				String declaration = current.getDeclaringType().getFullyQualifiedName();
				if (declaration != null && declaration.equals(CLASS_STREAM_HANDLER)) {
					writeMethodStream(current);
				} else {
					writeMethodSync(current);
					writeMethodDef(current);
					if (current.getReturnType().equals(Signature.SIG_VOID) &&
						current.getExceptionTypes().length == 1) {
						writeMethodAsync(current);						
					}
				}
			}		
		}
	}
	
	/**
	 * This method generates the proxy method for deferred synchronous method calls.
	 * It is called for all methods.
	 * 
	 * @param method The method to generate.
	 * @throws JavaModelException Thrown by the underlying java model.
	 */
	public void writeMethodDef(IMethod method) throws JavaModelException {
		// generate javadoc comment that meets compiler checks
		out.println("/**");
		out.println(" * Proxy method that creates and transfers a deferred synchronous invocation.");
		out.println(" *");
		for (int i = 0; i < method.getParameterNames().length; i++) {
			out.print(" * @param ");
			out.print(method.getParameterNames()[i]);
			out.print(" see ");
			out.println(method.getDeclaringType().getFullyQualifiedName());
		}
		out.print(" * @return A future result that delivers the return value and exceptions.");
		out.println(" * @see " + method.getDeclaringType().getFullyQualifiedName());
		out.println(" */");
		IType interfaceType = method.getDeclaringType();
		// needed for method name in invokeSynchronious
		String signature = JavaUtility.generateMethodSignature(method, "Def", true, false);
		// generate method header
		out.print(Flags.toString(method.getFlags()) + " " + CLASS_FUTURE + " " + signature + " ");
		out.println(" {");
		// generate method body
		String[] parNames = method.getParameterNames();
		String[] parTypes = method.getParameterTypes();
		out.println("Object[] __args = new Object[" + parNames.length + "];");
		for(int i = 0; i < parNames.length; i++){
			out.print("__args["+ i + "] = ");
			String qualified = JavaUtility.getQualifiedType(interfaceType, parTypes[i]);
			out.print(JavaUtility.boxVariable(qualified, parNames[i]));
			out.println(";");
		}
		out.println("String __method = \"" + JavaUtility.generateMethodSignature(method, "", false, true) + "\";");
		out.println(CLASS_INVOCATION + " __invocation = proxyCreateSynchronous(__method, __args);");
		out.println("return proxyInvokeDeferred(__invocation);");
		// generate method footer
		out.println("}");
	}
		
	/**
	 * This method generates the proxy method for asynchronous method calls.
	 * It is called for all methods without return values and exceptions.
	 * 
	 * @param method The method to generate.
	 * @throws JavaModelException Thrown by the underlying java model.
	 */
	public void writeMethodAsync(IMethod method) throws JavaModelException {
		// generate javadoc comment that meets compiler checks
		out.println("/**");
		out.println(" * Proxy method that creates and transfers an asynchronous call.");
		out.println(" *");
		for (int i = 0; i < method.getParameterNames().length; i++) {
			out.print(" * @param ");
			out.print(method.getParameterNames()[i]);
			out.print(" see ");
			out.println(method.getDeclaringType().getFullyQualifiedName());
		}
		if (! method.getReturnType().equals(Signature.SIG_VOID)) {
			out.print(" * @return see");
			out.println(method.getDeclaringType().getFullyQualifiedName());
		}
		for (int i = 0; i < method.getExceptionTypes().length; i++) {
			out.print(" * @throws ");
			out.print(JavaUtility.getQualifiedType(method.getDeclaringType(), method.getExceptionTypes()[i]));
			out.print(" see ");
			out.println(method.getDeclaringType().getFullyQualifiedName());
		}
		out.println(" * @see " + method.getDeclaringType().getFullyQualifiedName());
		out.println(" */");
		IType interfaceType = method.getDeclaringType();
		// needed for method name in invokeSynchronious
		String signature = JavaUtility.generateMethodSignature(method, "Async", true, true);
		// generate method header
		out.print(Flags.toString(method.getFlags()) + " " + signature + " ");
		String[] exceptions = method.getExceptionTypes();				
		if(exceptions != null && exceptions.length > 0){
			out.print("throws ");	
			for(int i = 0; i < exceptions.length - 1; i++){				
				out.print(JavaUtility.getQualifiedType(interfaceType, exceptions[i]) + ", ");
			}
			out.print(JavaUtility.getQualifiedType(interfaceType, exceptions[exceptions.length - 1]));	
		}
		out.println(" {");
		// generate method body
		String[] parNames = method.getParameterNames();
		String[] parTypes = method.getParameterTypes();
		out.println("Object[] __args = new Object[" + parNames.length + "];");
		for(int i = 0; i < parNames.length; i++){
			out.print("__args["+ i + "] = ");
			String qualified = JavaUtility.getQualifiedType(interfaceType, parTypes[i]);
			out.print(JavaUtility.boxVariable(qualified, parNames[i]));
			out.println(";");
		}
		out.println("String __method = \"" + JavaUtility.generateMethodSignature(method, "", false, true) + "\";");
		out.println(CLASS_INVOCATION + " __invocation = proxyCreateAsynchronous(__method, __args);");
		out.println(CLASS_RESULT + " __result = proxyInvokeAsynchronous(__invocation);");
		out.println("if (__result.hasException()) {");
		for(int i = 0; i < exceptions.length; i++){
			out.println("if (__result.getException() instanceof " + JavaUtility.getQualifiedType(interfaceType, exceptions[i]) + ") {"); 
			out.println("throw (" + JavaUtility.getQualifiedType(interfaceType,exceptions[i]) + ")__result.getException();");	
			out.println("}");								
		}
		out.println("throw (RuntimeException)__result.getException();");
		out.println("}");
		String returnType = JavaUtility.getQualifiedType(interfaceType, method.getReturnType());
		out.println("return " + JavaUtility.unboxVariable(returnType, "__result.getValue()") + ";");
		// generate method footer
		out.println("}");		
	}

	/**
	 * This method generates the proxy method to initiate streaming connections.
	 * 
	 * @param method The method.
	 * @throws JavaModelException Thrown by the underlying java model.
	 */
	public void writeMethodStream(IMethod method) throws JavaModelException {
		// generate javadoc comment that meets compiler checks
		out.println("/**");
		out.println(" * Proxy method that creates an invocation to open a stream.");
		out.println(" *");
		for (int i = 0; i < method.getParameterNames().length; i++) {
			out.print(" * @param ");
			out.print(method.getParameterNames()[i]);
			out.print(" see ");
			out.println(method.getDeclaringType().getFullyQualifiedName());
		}
		if (! method.getReturnType().equals(Signature.SIG_VOID)) {
			out.print(" * @return see");
			out.println(method.getDeclaringType().getFullyQualifiedName());
		}
		for (int i = 0; i < method.getExceptionTypes().length; i++) {
			out.print(" * @throws ");
			out.print(JavaUtility.getQualifiedType(method.getDeclaringType(), method.getExceptionTypes()[i]));
			out.print(" see ");
			out.println(method.getDeclaringType().getFullyQualifiedName());
		}
		out.println(" * @see " + method.getDeclaringType().getFullyQualifiedName());
		out.println(" */");
		IType interfaceType = method.getDeclaringType();
		// needed for method name in invokeSynchronious
		String signature = JavaUtility.generateMethodSignature(method, "", true, true);
		// generate method header
		out.print(Flags.toString(method.getFlags()) + " " + signature + " ");
		String[] exceptions = method.getExceptionTypes();				
		if(exceptions != null && exceptions.length > 0){
			out.print("throws ");	
			for(int i = 0; i < exceptions.length - 1; i++){				
				out.print(JavaUtility.getQualifiedType(interfaceType, exceptions[i]) + ", ");
			}
			out.print(JavaUtility.getQualifiedType(interfaceType, exceptions[exceptions.length - 1]));	
		}
		out.println(" {");
		// generate special handler for streaming method
		String descriptor = method.getParameterNames()[0];
		out.println("Object[] __args = new Object[1];");
		out.print("__args[0] = " + descriptor + ".getData()");
		out.println(";");
		out.println("String __method = \"" + JavaUtility.generateMethodSignature(method, "", false, true) + "\";");
		out.println(CLASS_INVOCATION + " __invocation = proxyCreateStream(__method, __args);");
		out.println(CLASS_RESULT + " __result = proxyInvokeSynchronous(__invocation);");
		out.println("if (__result.hasException()) {");
		for(int i = 0; i < exceptions.length; i++){
			out.println("if (__result.getException() instanceof " + JavaUtility.getQualifiedType(interfaceType, exceptions[i]) + ") {"); 
			out.println("throw (" + JavaUtility.getQualifiedType(interfaceType,exceptions[i]) + ")__result.getException();");	
			out.println("}");								
		}
		out.println("throw (RuntimeException)__result.getException();");
		out.println("}");
		out.println(descriptor + ".setConnector( " + JavaUtility.unboxVariable(CLASS_STREAM_CONNECTOR, "__result.getValue()") + ");");
		// generate method footer
		out.println("}");
	}

	/**
	 * Writes a single synchronous method call for the specified method.
	 * 
	 * @param method The method generated.
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	public void writeMethodSync(IMethod method) throws JavaModelException {
		// generate javadoc comment that meets compiler checks
		out.println("/**");
		out.println(" * Proxy method that creates and transfers an invocation for the interface method.");
		out.println(" *");
		for (int i = 0; i < method.getParameterNames().length; i++) {
			out.print(" * @param ");
			out.print(method.getParameterNames()[i]);
			out.print(" see ");
			out.println(method.getDeclaringType().getFullyQualifiedName());
		}
		if (! method.getReturnType().equals(Signature.SIG_VOID)) {
			out.print(" * @return see");
			out.println(method.getDeclaringType().getFullyQualifiedName());
		}
		for (int i = 0; i < method.getExceptionTypes().length; i++) {
			out.print(" * @throws ");
			out.print(JavaUtility.getQualifiedType(method.getDeclaringType(), method.getExceptionTypes()[i]));
			out.print(" see ");
			out.println(method.getDeclaringType().getFullyQualifiedName());
		}
		out.println(" * @see " + method.getDeclaringType().getFullyQualifiedName());
		out.println(" */");
		IType interfaceType = method.getDeclaringType();
		// needed for method name in invokeSynchronious
		String signature = JavaUtility.generateMethodSignature(method, "", true, true);
		// generate method header
		out.print(Flags.toString(method.getFlags()) + " " + signature + " ");
		String[] exceptions = method.getExceptionTypes();				
		if(exceptions != null && exceptions.length > 0){
			out.print("throws ");	
			for(int i = 0; i < exceptions.length - 1; i++){				
				out.print(JavaUtility.getQualifiedType(interfaceType, exceptions[i]) + ", ");
			}
			out.print(JavaUtility.getQualifiedType(interfaceType, exceptions[exceptions.length - 1]));	
		}
		out.println(" {");
		// generate method body
		String[] parNames = method.getParameterNames();
		String[] parTypes = method.getParameterTypes();
		out.println("Object[] __args = new Object[" + parNames.length + "];");
		for(int i = 0; i < parNames.length; i++){
			out.print("__args["+ i + "] = ");
			String qualified = JavaUtility.getQualifiedType(interfaceType, parTypes[i]);
			out.print(JavaUtility.boxVariable(qualified, parNames[i]));
			out.println(";");
		}
		out.println("String __method = \"" + JavaUtility.generateMethodSignature(method, "", false, true) + "\";");
		out.println(CLASS_INVOCATION + " __invocation = proxyCreateSynchronous(__method, __args);");
		out.println(CLASS_RESULT + " __result = proxyInvokeSynchronous(__invocation);");
		out.println("if (__result.hasException()) {");
		for(int i = 0; i < exceptions.length; i++){
			out.println("if (__result.getException() instanceof " + JavaUtility.getQualifiedType(interfaceType, exceptions[i]) + ") {"); 
			out.println("throw (" + JavaUtility.getQualifiedType(interfaceType,exceptions[i]) + ")__result.getException();");	
			out.println("}");								
		}
		out.println("throw (RuntimeException)__result.getException();");
		out.println("}");
		String returnType = JavaUtility.getQualifiedType(interfaceType, method.getReturnType());
		out.println("return " + JavaUtility.unboxVariable(returnType, "__result.getValue()") + ";");
		// generate method footer
		out.println("}");
	}

	/**
	 * Writes the proxy class to the output stream.
	 * 
	 * @throws JavaModelException Thrown by eclipse if the operation fails.
	 */
	public void write() throws JavaModelException {
		writeClass();	
	}
	
}
