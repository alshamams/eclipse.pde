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
package org.eclipse.pde.internal.ui.wizards.templates;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.pde.ui.templates.*;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.pde.core.plugin.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.jface.wizard.*;

public class MultiPageEditorTemplate extends BaseEditorTemplate {
	private static final String KEY_TITLE = "MultiPageEditorTemplate.title";
	private static final String KEY_DESC = "MultiPageEditorTemplate.desc";
	private static final String KEY_PACKAGE_LABEL =
		"MultiPageEditorTemplate.packageName";
	private static final String KEY_CLASS_LABEL =
		"MultiPageEditorTemplate.className";
	private static final String KEY_CONTRIBUTOR_LABEL =
		"MultiPageEditorTemplate.contributor";
	private static final String KEY_EDITOR_LABEL =
		"MultiPageEditorTemplate.editorName";
	private static final String KEY_DEFAULT_EDITOR_NAME =
		"MultiPageEditorTemplate.defaultEditorName";
	private static final String KEY_EXTENSIONS_LABEL =
		"MultiPageEditorTemplate.extensions";

	/**
	 * Constructor for MultiPageEditorTemplate.
	 */
	public MultiPageEditorTemplate() {
		setPageCount(1);
		createOptions();
	}

	public String getSectionId() {
		return "multiPageEditor";
	}
	
	public IPluginReference[] getDependencies(String schemaVersion) {
		if (schemaVersion != null) {
			IPluginReference[] dep = new IPluginReference[4];
			dep[0] = new PluginReference("org.eclipse.jface.text", null, 0);
			dep[1] = new PluginReference("org.eclipse.ui.editors", null, 0);
			dep[2] = new PluginReference("org.eclipse.ui.ide", null, 0);
			dep[3] = new PluginReference("org.eclipse.ui.workbench.texteditor", null, 0);
			return dep;
		}
		return super.getDependencies(schemaVersion);
	}
	
	/*
	 * @see ITemplateSection#getNumberOfWorkUnits()
	 */
	public int getNumberOfWorkUnits() {
		return super.getNumberOfWorkUnits() + 1;
	}

	private void createOptions() {
		// first page	
		addOption(
			KEY_PACKAGE_NAME,
			PDEPlugin.getResourceString(KEY_PACKAGE_LABEL),
			(String) null,
			0);
		addOption(
			"editorClassName",
			PDEPlugin.getResourceString(KEY_CLASS_LABEL),
			"MultiPageEditor",
			0);
		addOption(
			"contributorClassName",
			PDEPlugin.getResourceString(KEY_CONTRIBUTOR_LABEL),
			"MultiPageEditorContributor",
			0);
		addOption(
			"editorName",
			PDEPlugin.getResourceString(KEY_EDITOR_LABEL),
			PDEPlugin.getResourceString(KEY_DEFAULT_EDITOR_NAME),
			0);
		addOption(
			"extensions",
			PDEPlugin.getResourceString(KEY_EXTENSIONS_LABEL),
			"mpe",
			0);
	}

	protected void initializeFields(String id) {
		// In a new project wizard, we don't know this yet - the
		// model has not been created
		initializeOption(KEY_PACKAGE_NAME, id + ".editors");
	}
	public void initializeFields(IPluginModelBase model) {
		// In the new extension wizard, the model exists so 
		// we can initialize directly from it
		String pluginId = model.getPluginBase().getId();
		initializeOption(KEY_PACKAGE_NAME, pluginId + ".editors");
	}

	public boolean isDependentOnFirstPage() {
		return true;
	}

	public void addPages(Wizard wizard) {
		WizardPage page = createPage(0, IHelpContextIds.TEMPLATE_MULTIPAGE_EDITOR);
		page.setTitle(PDEPlugin.getResourceString(KEY_TITLE));
		page.setDescription(PDEPlugin.getResourceString(KEY_DESC));
		wizard.addPage(page);
		markPagesAdded();
	}

	public void validateOptions(TemplateOption source) {
		if (source.isRequired() && source.isEmpty()) {
			flagMissingRequiredOption(source);
		} else {
			validateContainerPage(source);
		}
	}

	private void validateContainerPage(TemplateOption source) {
		TemplateOption[] allPageOptions = getOptions(0);
		for (int i = 0; i < allPageOptions.length; i++) {
			TemplateOption nextOption = allPageOptions[i];
			if (nextOption.isRequired() && nextOption.isEmpty()) {
				flagMissingRequiredOption(nextOption);
				return;
			}
		}
		resetPageState();
	}

	protected void updateModel(IProgressMonitor monitor) throws CoreException {
		IPluginBase plugin = model.getPluginBase();
		IPluginExtension extension = createExtension("org.eclipse.ui.editors", true);
		IPluginModelFactory factory = model.getPluginFactory();

		String editorClassName =
			getStringOption(KEY_PACKAGE_NAME) + "." + getStringOption("editorClassName");
		String contributorClassName =
			getStringOption(KEY_PACKAGE_NAME)
				+ "."
				+ getStringOption("contributorClassName");

		IPluginElement editorElement = factory.createElement(extension);
		editorElement.setName("editor");
		editorElement.setAttribute("id", editorClassName);
		editorElement.setAttribute("name", getStringOption("editorName"));
		editorElement.setAttribute("icon", "icons/sample.gif");
		editorElement.setAttribute("extensions", getStringOption("extensions"));

		editorElement.setAttribute("class", editorClassName);
		editorElement.setAttribute("contributorClass", contributorClassName);
		extension.add(editorElement);
		if (!extension.isInTheModel())
			plugin.add(extension);
	}
}