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

import org.eclipse.pde.internal.ui.*;
import org.eclipse.pde.ui.templates.*;

public class PopupMenuNewWizard extends NewPluginTemplateWizard {
	private static final String KEY_WTITLE = "PopupMenuNewWizard.wtitle";
	/**
	 * Constructor for PerspectiveExtensionsNewWizard.
	 */
	public PopupMenuNewWizard() {
		super();
	}

	public void init(String id) {
		super.init(id);
		setWindowTitle(PDEPlugin.getResourceString(KEY_WTITLE));
	}
	/**
	 * @see NewPluginTemplateWizard#createTemplateSections()
	 */
	public ITemplateSection[] createTemplateSections() {
		return new ITemplateSection[] { new PopupMenuTemplate()};
	}

}
