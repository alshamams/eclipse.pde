/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.pde.internal.ui.editor.manifest;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.dialogs.ProgressMonitorDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.*;
import org.eclipse.pde.core.*;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.core.*;
import org.eclipse.pde.internal.core.plugin.*;
import org.eclipse.pde.internal.ui.PDEPlugin;
import org.eclipse.pde.internal.ui.editor.*;
import org.eclipse.pde.internal.ui.elements.DefaultTableProvider;
import org.eclipse.pde.internal.ui.parts.TablePart;
import org.eclipse.pde.internal.ui.search.PluginSearchActionGroup;
import org.eclipse.pde.internal.ui.search.UnusedDependenciesAction;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.actions.*;
import org.eclipse.ui.actions.ActionContext;
import org.eclipse.update.ui.forms.internal.FormWidgetFactory;


public class ImportListSection
	extends TableSection
	implements IModelChangedListener, IModelProviderListener {
	private TableViewer importTable;
	public static final String SECTION_TITLE =
		"ManifestEditor.ImportListSection.title";
	public static final String SECTION_DESC =
		"ManifestEditor.ImportListSection.desc";
	public static final String SECTION_FDESC =
		"ManifestEditor.ImportListSection.fdesc";
	public static final String SECTION_NEW = "ManifestEditor.ImportListSection.new";
	public static final String POPUP_OPEN = "Actions.open.label";
	public static final String POPUP_DELETE = "Actions.delete.label";
	public static final String KEY_UPDATING_BUILD_PATH =
		"ManifestEditor.ImportListSection.updatingBuildPath";
	public static final String KEY_COMPUTE_BUILD_PATH =
		"ManifestEditor.ImportListSection.updateBuildPath";
	private Vector imports;
	private Action openAction;
	private Action newAction;
	private Action deleteAction;
	private Action buildpathAction;

	class ImportContentProvider extends DefaultTableProvider {
		public Object[] getElements(Object parent) {
			if (imports == null) {
				createImportObjects();
			}
			return imports.toArray();
		}
		private void createImportObjects() {
			imports = new Vector();
			IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
			IPluginImport[] iimports = model.getPluginBase().getImports();
			for (int i = 0; i < iimports.length; i++) {
				IPluginImport iimport = iimports[i];
				imports.add(new ImportObject(iimport));
			}
		}
	}

	public ImportListSection(ManifestDependenciesPage page) {
		super(page, new String[] { PDEPlugin.getResourceString(SECTION_NEW)});
		setHeaderText(PDEPlugin.getResourceString(SECTION_TITLE));
		boolean fragment =
			((ManifestEditor) getFormPage().getEditor()).isFragmentEditor();
		if (fragment)
			setDescription(PDEPlugin.getResourceString(SECTION_FDESC));
		else
			setDescription(PDEPlugin.getResourceString(SECTION_DESC));
		getTablePart().setEditable(false);
	}

	public Composite createClient(Composite parent, FormWidgetFactory factory) {
		Composite container = createClientContainer(parent, 2, factory);
		createViewerPartControl(container, SWT.MULTI, 2, factory);
		TablePart tablePart = getTablePart();
		importTable = tablePart.getTableViewer();

		importTable.setContentProvider(new ImportContentProvider());
		importTable.setLabelProvider(PDEPlugin.getDefault().getLabelProvider());
		importTable.setSorter(ListUtil.PLUGIN_SORTER);
		factory.paintBordersFor(container);
		makeActions();
		return container;
	}

	protected void selectionChanged(IStructuredSelection sel) {
		Object item = sel.getFirstElement();
		fireSelectionNotification(item);
		getFormPage().setSelection(sel);
	}
	protected void handleDoubleClick(IStructuredSelection sel) {
		handleOpen(sel);
	}

	protected void buttonSelected(int index) {
		if (index == 0) {
			handleNew();
		}
	}

	public void dispose() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		model.removeModelChangedListener(this);
		PDECore.getDefault().getWorkspaceModelManager().removeModelProviderListener(
			this);
		PDECore.getDefault().getExternalModelManager().removeModelProviderListener(
			this);
		super.dispose();
	}
	public boolean doGlobalAction(String actionId) {
		if (actionId.equals(ActionFactory.DELETE.getId())) {
			handleDelete();
			return true;
		}
		if (actionId.equals(ActionFactory.CUT.getId())) {
			// delete here and let the editor transfer
			// the selection to the clipboard
			handleDelete();
			return false;
		}
		if (actionId.equals(ActionFactory.PASTE.getId())) {
			doPaste();
			return true;
		}
		return false;
	}

	public void expandTo(Object object) {
		if (object instanceof IPluginImport) {
			ImportObject iobj = new ImportObject((IPluginImport) object);
			importTable.setSelection(new StructuredSelection(iobj), true);
		}
	}

	protected void fillContextMenu(IMenuManager manager) {
		ISelection selection = importTable.getSelection();
		manager.add(newAction);
		if (!selection.isEmpty()) {
			manager.add(openAction);
		}
		manager.add(new Separator());
		
		((DependenciesForm) getFormPage().getForm()).fillContextMenu(manager);
		
		if (!selection.isEmpty())
			manager.add(deleteAction);
		getFormPage().getEditor().getContributor().contextMenuAboutToShow(
			manager);
		manager.add(new Separator());
		
		PluginSearchActionGroup actionGroup = new PluginSearchActionGroup();
		actionGroup.setContext(new ActionContext(selection));
		actionGroup.fillContextMenu(manager);
		if (getFormPage().getModel() instanceof WorkspacePluginModelBase) {
			manager.add(new UnusedDependenciesAction((WorkspacePluginModelBase) getFormPage().getModel()));
		}
	}

	private void handleDelete() {
		IStructuredSelection ssel = (IStructuredSelection) importTable.getSelection();

		if (ssel.isEmpty())
			return;
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IPluginBase pluginBase = model.getPluginBase();

		try {
			for (Iterator iter = ssel.iterator(); iter.hasNext();) {
				ImportObject iobj = (ImportObject) iter.next();
				pluginBase.remove(iobj.getImport());
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}

	private void handleNew() {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		PluginSelectionDialog dialog =
			new PluginSelectionDialog(
				PDEPlugin.getActiveWorkbenchShell(),
				getAvailablePlugins(model),
				true);
		dialog.create();
		if (dialog.open() == PluginSelectionDialog.OK) {
			Object[] models = dialog.getResult();
			for (int i = 0; i < models.length; i++) {
				try {
					IPluginModel candidate = (IPluginModel) models[i];
					IPluginImport importNode = model.getPluginFactory().createImport();
					importNode.setId(candidate.getPlugin().getId());
					model.getPluginBase().add(importNode);
				} catch (CoreException e) {
				}
			}
		}
	}
	
	private IPluginModelBase[] getAvailablePlugins(IPluginModelBase model) {
		IPluginModelBase[] plugins = PDECore.getDefault().getModelManager().getPluginsOnly();
		HashSet existingImports = PluginSelectionDialog.getExistingImports(model.getPluginBase());
		ArrayList result = new ArrayList();
		for (int i = 0; i < plugins.length; i++) {
			if (!existingImports.contains(plugins[i].getPluginBase().getId())) {
				result.add(plugins[i]);
			}
		}
		return (IPluginModelBase[])result.toArray(new IPluginModelBase[result.size()]);
	}

	private void handleOpen(ISelection sel) {
		if (sel instanceof IStructuredSelection) {
			IStructuredSelection ssel = (IStructuredSelection) sel;
			if (ssel.size() == 1) {
				handleOpen(ssel.getFirstElement());
			}
		}
	}

	private void handleOpen(Object obj) {
		if (obj instanceof ImportObject) {
			IPlugin plugin = ((ImportObject) obj).getPlugin();
			if (plugin != null)
				 ManifestEditor.openPluginEditor(plugin);
		}
	}

	public void initialize(Object input) {
		IPluginModelBase model = (IPluginModelBase) input;
		importTable.setInput(model.getPluginBase());
		setReadOnly(!model.isEditable());
		getTablePart().setButtonEnabled(0, model.isEditable());
		model.addModelChangedListener(this);
		PDECore.getDefault().getWorkspaceModelManager().addModelProviderListener(
			this);
		PDECore.getDefault().getExternalModelManager().addModelProviderListener(this);
		newAction.setEnabled(model.isEditable());
		deleteAction.setEnabled(model.isEditable());
		buildpathAction.setEnabled(model.isEditable());
	}

	private void makeActions() {
		newAction = new Action() {
			public void run() {
				handleNew();
			}
		};
		newAction.setText(PDEPlugin.getResourceString("ManifestEditor.ImportListSection.new"));

		openAction = new Action() {
			public void run() {
				handleOpen(importTable.getSelection());
			}
		};
		openAction.setText(PDEPlugin.getResourceString(POPUP_OPEN));

		deleteAction = new Action() {
			public void run() {
				handleDelete();
			}
		};
		deleteAction.setText(PDEPlugin.getResourceString(POPUP_DELETE));
		buildpathAction = new Action() {
			public void run() {
				Object model = getFormPage().getModel();
				if (model instanceof IPluginModel)
					computeBuildPath((IPluginModel)model, true);
			}
		};
		buildpathAction.setText(PDEPlugin.getResourceString(KEY_COMPUTE_BUILD_PATH));
	}

	public void modelChanged(IModelChangedEvent event) {
		if (event.getChangeType() == IModelChangedEvent.WORLD_CHANGED) {
			imports = null;
			importTable.refresh();
			return;
		}

		Object changeObject = event.getChangedObjects()[0];
		if (changeObject instanceof IPluginImport) {
			IPluginImport iimport = (IPluginImport) changeObject;
			if (event.getChangeType() == IModelChangedEvent.INSERT) {
				ImportObject iobj = new ImportObject(iimport);
				imports.add(iobj);
				importTable.add(iobj);
				importTable.setSelection(new StructuredSelection(iobj), true);
				importTable.getTable().setFocus();
			} else {
				ImportObject iobj = findImportObject(iimport);
				if (iobj != null) {
					if (event.getChangeType() == IModelChangedEvent.REMOVE) {
						imports.remove(iobj);
						importTable.remove(iobj);
					} else {
						importTable.update(iobj, null);
					}
				}
			}
			setDirty(true);
		}
	}

	public void modelsChanged(IModelProviderEvent e) {
		imports = null;
		importTable.getControl().getDisplay().asyncExec(new Runnable() {
			public void run() {
				if (!importTable.getControl().isDisposed())
					importTable.refresh();
			}
		});
	}

	private ImportObject findImportObject(IPluginImport iimport) {
		if (imports == null)
			return null;
		for (int i = 0; i < imports.size(); i++) {
			ImportObject iobj = (ImportObject) imports.get(i);
			if (iobj.getImport().equals(iimport))
				return iobj;
		}
		return null;
	}

	public void commitChanges(boolean onSave) {
		if (onSave) {
			IResource resource =
				((IPluginModelBase) getFormPage().getModel())
					.getUnderlyingResource();
			if (resource != null) {
				IProject project = resource.getProject();
				if (WorkspaceModelManager.isJavaPluginProject(project)) {
					PDESourcePage sourcePage =
						(PDESourcePage) getFormPage().getEditor().getPage(
							ManifestEditor.SOURCE_PAGE);
					if (!sourcePage.containsError())
						updateBuildPath();
				}
			}
		}
		setDirty(false);
	}

	private void updateBuildPath() {
		computeBuildPath((IPluginModelBase) getFormPage().getModel(), false);
	}

	Action getBuildpathAction() {
		return buildpathAction;
	}

	private void computeBuildPath(
		final IPluginModelBase model,
		final boolean save) {
		IRunnableWithProgress op = new IRunnableWithProgress() {
			public void run(IProgressMonitor monitor) throws InvocationTargetException {
				monitor.beginTask(PDEPlugin.getResourceString(KEY_UPDATING_BUILD_PATH), 1);
				try {
					if (save && getFormPage().getEditor().isDirty()) {
						getFormPage().getEditor().doSave(monitor);
					}
					ClasspathUtilCore.setClasspath(model, monitor);
					monitor.worked(1);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				} finally {
					monitor.done();
				}
			}
		};

		ProgressMonitorDialog pm =
			new ProgressMonitorDialog(PDEPlugin.getActiveWorkbenchShell());
		try {
			pm.run(false, false, op);
		} catch (InterruptedException e) {
			PDEPlugin.logException(e);
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e.getTargetException());
		}
	}

	public void setFocus() {
		if (importTable != null)
			importTable.getTable().setFocus();
	}

	protected void doPaste(Object target, Object[] objects) {
		IPluginModelBase model = (IPluginModelBase) getFormPage().getModel();
		IPluginBase plugin = model.getPluginBase();

		try {
			for (int i = 0; i < objects.length; i++) {
				Object obj = objects[i];
				if (obj instanceof ImportObject) {
					ImportObject iobj = (ImportObject) obj;
					PluginImport iimport = (PluginImport) iobj.getImport();
					iimport.setModel(model);
					iimport.setParent(plugin);
					plugin.add(iimport);
				}
			}
		} catch (CoreException e) {
			PDEPlugin.logException(e);
		}
	}
	protected boolean canPaste(Object target, Object[] objects) {
		for (int i = 0; i < objects.length; i++) {
			if (!(objects[i] instanceof ImportObject))
				return false;
		}
		return true;
	}
	public boolean canPaste(Clipboard clipboard) {
		Object [] objects = (Object[])clipboard.getContents(ModelDataTransfer.getInstance());
		if (objects!=null && objects.length>0) {
			return canPaste(null, objects);
		}
		return false;
	}
	protected void doPaste() {
		Clipboard clipboard = getFormPage().getEditor().getClipboard();
		ModelDataTransfer modelTransfer = ModelDataTransfer.getInstance();
		Object [] objects = (Object[])clipboard.getContents(modelTransfer);
		if (objects!=null) {
			doPaste(null, objects);
		}
	}

}
