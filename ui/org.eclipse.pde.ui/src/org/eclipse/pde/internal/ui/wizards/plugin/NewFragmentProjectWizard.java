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
package org.eclipse.pde.internal.ui.wizards.plugin;

import java.lang.reflect.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.internal.ui.wizards.*;
import org.eclipse.pde.ui.*;
import org.eclipse.ui.dialogs.*;
import org.eclipse.ui.wizards.newresource.*;

public class NewFragmentProjectWizard extends NewWizard implements IExecutableExtension {

	private WizardNewProjectCreationPage fMainPage;
	private ProjectStructurePage fStructurePage;
	private ContentPage fContentPage;
	private FragmentFieldData fFragmentData;
	private IProjectProvider fProjectProvider;
	private IConfigurationElement fConfig;
	
	public NewFragmentProjectWizard() {
		setDefaultPageImageDescriptor(PDEPluginImages.DESC_NEWFRAGPRJ_WIZ);
		setWindowTitle(PDEPlugin.getResourceString("NewFragmentProjectWizard.title"));
		setNeedsProgressMonitor(true);
		PDEPlugin.getDefault().getLabelProvider().connect(this);
		fFragmentData = new FragmentFieldData();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#addPages()
	 */
	public void addPages() {
		fMainPage = new WizardNewProjectCreationPage("main");
		fMainPage.setTitle(PDEPlugin.getResourceString("NewProjectWizard.MainPage.ftitle"));
		fMainPage.setDescription(PDEPlugin.getResourceString("NewProjectWizard.MainPage.fdesc"));
		addPage(fMainPage);
		
		fProjectProvider = new IProjectProvider() {
			public String getProjectName() {
				return fMainPage.getProjectName();
			}
			public IProject getProject() {
				return fMainPage.getProjectHandle();
			}
			public IPath getLocationPath() {
				return fMainPage.getLocationPath();
			}
		};
		
		fStructurePage = new ProjectStructurePage("page1", fProjectProvider, true);
		fContentPage = new ContentPage("page2", fProjectProvider, fStructurePage, true);
		addPage(fStructurePage);
		addPage(fContentPage);
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.pde.internal.ui.wizards.NewWizard#performFinish()
	 */
	public boolean performFinish() {
		fStructurePage.finish(fFragmentData);
		fContentPage.finish(fFragmentData);
		try {
			getContainer().run(false, true,
					new NewProjectCreationOperation(fFragmentData, fProjectProvider));
			BasicNewProjectResourceWizard.updatePerspective(fConfig);
			return true;
		} catch (InvocationTargetException e) {
			PDEPlugin.logException(e);
		} catch (InterruptedException e) {
		}
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.wizard.Wizard#dispose()
	 */
	public void dispose() {
		super.dispose();
		PDEPlugin.getDefault().getLabelProvider().disconnect(this);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.IExecutableExtension#setInitializationData(org.eclipse.core.runtime.IConfigurationElement, java.lang.String, java.lang.Object)
	 */
	public void setInitializationData(IConfigurationElement config, String propertyName, Object data) throws CoreException {
		fConfig = config;
	}
}
