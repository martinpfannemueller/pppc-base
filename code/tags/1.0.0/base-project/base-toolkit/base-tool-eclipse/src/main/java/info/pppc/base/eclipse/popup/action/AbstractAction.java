/*
 * Revision: $Revision: 1.1 $
 * Author:   $Author: handtems $
 * Date:     $Date: 2006/04/27 09:21:31 $ 
 */
package info.pppc.base.eclipse.popup.action;

import info.pppc.base.eclipse.Plugin;
import info.pppc.base.eclipse.generator.Generator;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IObjectActionDelegate;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

/**
 * The abstract action provides the default behaviour for actions that create a stub.
 * 
 * @author Mac
 */
public abstract class AbstractAction implements IObjectActionDelegate {

	/**
	 * The workbench part that has been selected recently.
	 */
	protected IWorkbenchPart part;
	
	/**
	 * The selected file that proboably contains a pcom contract.
	 */
	protected IType type;

	/**
	 * The java project of the selection.
	 */
	protected IJavaProject project;


	/**
	 * Creates a new service action.
	 */
	public AbstractAction() {
		super();
	}

	/**
	 * Sets the active part of the action.
	 * 
	 * @param action The action.
	 * @param targetPart The target part of the workbench.
	 */
	public void setActivePart(IAction action, IWorkbenchPart targetPart) {
		this.part = targetPart;
	}

	/**
	 * Called whenever the action should be executed. This will
	 * generate the files specified by the modifier.
	 * 
	 * @param action The action that should be executed.
	 */
	public void run(IAction action) {
		Shell shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
		ProgressMonitorDialog dialog = new ProgressMonitorDialog(shell);
		try {
			dialog.run(true, false, new IRunnableWithProgress() {
				public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
					try {
						project = type.getJavaProject();
						Generator generator = new Generator();
						generator.init(project, type, monitor);
						int[] modifiers = getModifiers();
						int modifier = 0;
						for (int i = 0; i < modifiers.length; i++) {
							modifier = modifier | modifiers[i];
						}
						generator.generate(modifier);
					} catch (Throwable t) {
						throw new InvocationTargetException(t);		
					}					
				}
			});	
		} catch (InvocationTargetException t) {
			try {
				throw t.getTargetException();	
			} catch (JavaModelException e) {			
				ErrorDialog.openError(shell, "3PC Base Tools", "Generation failed (JDT JavaModelException).", e.getStatus());
				e.printStackTrace();
			} catch (Throwable e) {
				String name = Plugin.getDefault().getBundle().getSymbolicName();
				IStatus status = new Status(IStatus.ERROR, name, IStatus.OK, "Unknown exception type.", e);
				ErrorDialog.openError(shell, "3PC Base Tools", "Generation failed (?).", status);
				e.printStackTrace();
			}
		} catch (InterruptedException e) {
			String name = Plugin.getDefault().getBundle().getSymbolicName();
			IStatus status = new Status(IStatus.ERROR, name, IStatus.OK, "Thread failure (Interrupted Exception).", e);
			ErrorDialog.openError(shell, "3PC Base Tools", "Generation status unknown.", status);			
		}		
	}
	
	/**
	 * Called whenever the action is executed and the generation
	 * process is about to be started.
	 * 
	 * @return The modifier for the action.
	 */
	public abstract int[] getModifiers();

	/**
	 * Determine whether the selection is a compilation unit that
	 * contains an interface as top level type.
	 * 
	 * @param action The action.
	 * @param selection The current selection.
	 */
	public void selectionChanged(IAction action, ISelection selection) {
		action.setEnabled(false);
		try {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection s = (IStructuredSelection)selection;
				if (! selection.isEmpty()) {
					Object element = s.getFirstElement();
					if (element instanceof ICompilationUnit) {
						ICompilationUnit unit = (ICompilationUnit)element;
						IType[] types = unit.getAllTypes();
						if (types != null && types.length > 0 &&
							types[0].isInterface()) {
							type = types[0];		
							action.setEnabled(true);
						}
					}
				}
			}			
		} catch (JavaModelException e) {
			e.printStackTrace();
		}
	}

}
