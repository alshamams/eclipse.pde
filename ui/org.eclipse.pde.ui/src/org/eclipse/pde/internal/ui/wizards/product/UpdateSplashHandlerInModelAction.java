/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/

package org.eclipse.pde.internal.ui.wizards.product;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.Action;
import org.eclipse.osgi.util.NLS;
import org.eclipse.pde.core.plugin.IPluginAttribute;
import org.eclipse.pde.core.plugin.IPluginElement;
import org.eclipse.pde.core.plugin.IPluginExtension;
import org.eclipse.pde.core.plugin.IPluginExtensionPoint;
import org.eclipse.pde.core.plugin.IPluginModelBase;
import org.eclipse.pde.core.plugin.IPluginObject;
import org.eclipse.pde.internal.core.util.PDETextHelper;
import org.eclipse.pde.internal.ui.PDEUIMessages;

/**
 * UpdateSplashHandlerInModelAction
 *
 */
public class UpdateSplashHandlerInModelAction extends Action {

	private IPluginModelBase fModel;
	
	private IProgressMonitor fMonitor;
	
	private CoreException fException;
	
	private String fFieldID;

	private String fFieldSplashID;
	
	private String fFieldProductID;
	
	private String fFieldClass;
	
	private String fFieldTemplate;

	private String fFieldPluginID;	
	
	public final static String[] F_SPLASH_SCREEN_CLASSES = {
		"InteractiveSplashHandler", //$NON-NLS-1$
		"BrowserSplashHandler", //$NON-NLS-1$
		"ExtensibleSplashHandler" //$NON-NLS-1$
	};

	public final static String F_UNQUALIFIED_EXTENSION_ID = "splashHandlers";	 //$NON-NLS-1$
	
	public final static String[][] F_SPLASH_SCREEN_TYPE_CHOICES = new String[3][2];

	public final static String F_ATTRIBUTE_TOOLTIP = "tooltip"; //$NON-NLS-1$

	public final static String F_ATTRIBUTE_ICON = "icon"; //$NON-NLS-1$

	public final static String F_ELEMENT_SPLASH = "splashExtension"; //$NON-NLS-1$

	public final static String F_ATTRIBUTE_SPLASH_ID = "splashId"; //$NON-NLS-1$

	public final static String F_ATTRIBUTE_PRODUCT_ID = "productId"; //$NON-NLS-1$

	public final static String F_ELEMENT_PRODUCT_BINDING = "splashHandlerProductBinding"; //$NON-NLS-1$

	public final static String F_ATTRIBUTE_ID = "id"; //$NON-NLS-1$

	public final static String F_ATTRIBUTE_CLASS = "class"; //$NON-NLS-1$

	public final static String F_ELEMENT_SPLASH_HANDLER = "splashHandler"; //$NON-NLS-1$

	public final static String F_SPLASH_EXTENSION_POINT = "splashExtension"; //$NON-NLS-1$

	public final static String F_SPLASH_HANDLERS_EXTENSION = "org.eclipse.ui.splashHandlers";	 //$NON-NLS-1$
	
	static {
		F_SPLASH_SCREEN_TYPE_CHOICES[0][0] = "interactive"; //$NON-NLS-1$
		F_SPLASH_SCREEN_TYPE_CHOICES[0][1] = PDEUIMessages.UpdateSplashHandlerInModelAction_templateTypeInteractive;
	
		F_SPLASH_SCREEN_TYPE_CHOICES[1][0] = "browser"; //$NON-NLS-1$
		F_SPLASH_SCREEN_TYPE_CHOICES[1][1] = PDEUIMessages.UpdateSplashHandlerInModelAction_templateTypeBrowser;
	
		F_SPLASH_SCREEN_TYPE_CHOICES[2][0] = "extensible"; //$NON-NLS-1$
		F_SPLASH_SCREEN_TYPE_CHOICES[2][1] = PDEUIMessages.UpdateSplashHandlerInModelAction_templateTypeExtensible;	
	}		
	
	/**
	 * 
	 */
	public UpdateSplashHandlerInModelAction() {
		reset();
	}

	/**
	 * @param fieldID the fFieldID to set
	 */
	public void setFieldID(String fieldID) {
		fFieldID = fieldID;
	}

	/**
	 * @param fieldSplashID the fFieldSplashID to set
	 */
	public void setFieldSplashID(String fieldSplashID) {
		fFieldSplashID = fieldSplashID;
	}

	/**
	 * @param fieldProductID the fFieldProductID to set
	 */
	public void setFieldProductID(String fieldProductID) {
		fFieldProductID = fieldProductID;
	}

	/**
	 * @param fieldClass the fFieldClass to set
	 */
	public void setFieldClass(String fieldClass) {
		fFieldClass = fieldClass;
	}

	/**
	 * @param fieldTemplate the fFieldTemplate to set
	 */
	public void setFieldTemplate(String fieldTemplate) {
		fFieldTemplate = fieldTemplate;
	}

	/**
	 * @param fieldPluginID
	 */
	public void setFieldPluginID(String fieldPluginID) {
		fFieldPluginID = fieldPluginID;
	}
	
	/**
	 * 
	 */
	public void reset() {
		fModel = null;
		fMonitor = null;
		fException = null;		
		
		fFieldID = null;
		fFieldClass = null;
		fFieldSplashID = null;
		fFieldProductID = null;
		fFieldTemplate = null;		
		fFieldPluginID = null;
	}

	
	/**
	 * @param model
	 */
	public void setModel(IPluginModelBase model) {
		fModel = model;
	}
	
	/**
	 * @param monitor
	 */
	public void setMonitor(IProgressMonitor monitor) {
		fMonitor = monitor;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.action.Action#run()
	 */
	public void run() {
		try {
			updateModel();
		} catch (CoreException e) {
			fException = e;
		}
	}

	/**
	 * @throws CoreException
	 */
	public void hasException() throws CoreException {
		// Release any caught exceptions
		if (fException != null) {
			throw fException;
		}
	}
	
	/**
	 * @throws CoreException
	 */
	public void updateModel() throws CoreException {
		// Find the first splash handler extension
		IPluginExtension extension = 
			findFirstExtension(UpdateSplashHandlerInModelAction.F_SPLASH_HANDLERS_EXTENSION);
		// Check to see if one was found
		if (extension == null) {
			// None found, add a new splash handler extension
			addExtensionSplashHandlers();
		} else {
			// Found one, modify the existing splash handler extension
			modifyExtensionSplashHandlers(extension);
		}
		// Determine whether the extensible template was selected
		if (isExtensibleTemplateSelected(fFieldTemplate)) {
			// Extensible template was selected
			// Extra model modifications required for this template
			// Find the first spash extension point declaration (should only
			// ever be one)
			IPluginExtensionPoint extensionPoint = 
				findFirstExtensionPoint(UpdateSplashHandlerInModelAction.F_SPLASH_EXTENSION_POINT);
			// Check to see if one was found
			// If one is found, just assume all its values are correct (no sync)
			if (extensionPoint == null) {
				// No splash extension point definition found, add one
				addExtensionPointSplashExtension();
			}
			// Find the first splash extension contribution
			String fullExtensionPointID = fFieldPluginID + 
				'.' + UpdateSplashHandlerInModelAction.F_SPLASH_EXTENSION_POINT;			
			IPluginExtension extensionSplash = 
				findFirstExtension(fullExtensionPointID);
			// Check to see if one was found
			// If one is found, just assume all its values are correct (no sync)
			if (extensionSplash == null) {
				// No splash extension contribution found, add one
				addExtensionSplash();
			}
		}
	}	

	/**
	 * @throws CoreException
	 */
	private void addExtensionSplash() throws CoreException {
		// Update progress work units
		String fullExtensionPointID = fFieldPluginID + 
			'.' + UpdateSplashHandlerInModelAction.F_SPLASH_EXTENSION_POINT;
		fMonitor.beginTask(NLS.bind(PDEUIMessages.UpdateSplashHandlerInModelAction_msgAddingExtension, 
				fullExtensionPointID), 1); 	
		// Create the new extension
		IPluginExtension extension = createExtensionSplash();
		// Add extension to the model
		fModel.getPluginBase().add(extension);
		// Update progress work units
		fMonitor.done();			
	}	
	
	/**
	 * @throws CoreException
	 */
	private void addExtensionPointSplashExtension() throws CoreException {
		// Update progress work units
		fMonitor.beginTask(NLS.bind(PDEUIMessages.UpdateSplashHandlerInModelAction_msgAddingExtensionPoint, 
				UpdateSplashHandlerInModelAction.F_SPLASH_EXTENSION_POINT), 1); 	
		// Create the new extension point
		IPluginExtensionPoint extensionPoint = createExtensionPointSplash();
		// Add extension point to the model
		fModel.getPluginBase().add(extensionPoint);
		// Update progress work units
		fMonitor.done();	
	}
	
	/**
	 * @return
	 * @throws CoreException
	 */
	private IPluginExtensionPoint createExtensionPointSplash()
		throws CoreException {
		// Create the extension point
		IPluginExtensionPoint extensionPoint = 
			fModel.getFactory().createExtensionPoint();
		// ID
		extensionPoint.setId(UpdateSplashHandlerInModelAction.F_SPLASH_EXTENSION_POINT);
		// Name
		extensionPoint.setName(PDEUIMessages.UpdateSplashHandlerInModelAction_splashExtensionPointName);
		// Schema
		extensionPoint.setSchema("schema/splashExtension.exsd"); //$NON-NLS-1$
		
		return extensionPoint;
	}		
	

	/**
	 * @param extensionPointID
	 * @return
	 */
	private IPluginExtension findFirstExtension(
			String extensionPointID) {
		// Get all the extensions
		IPluginExtension[] extensions = fModel.getPluginBase().getExtensions();
		// Get the first extension matching the specified extension point ID
		for (int i = 0; i < extensions.length; i++) {
			String point = extensions[i].getPoint();
			if (extensionPointID.equals(point)) {
				return extensions[i];
			}
		}
		return null;
	}	
	

	/**
	 * @param extensionPointID
	 * @return
	 */
	private IPluginExtensionPoint findFirstExtensionPoint(
			String extensionPointID) {
		// Get all the extension points
		IPluginExtensionPoint[] extensionPoints = 
			fModel.getPluginBase().getExtensionPoints();
		// Get the first extension point (should only be one) matching the
		// specified extension point ID
		for (int i = 0; i < extensionPoints.length; i++) {
			// Not full ID
			String point = extensionPoints[i].getId();
			if (extensionPointID.equals(point)) {
				return extensionPoints[i];
			}
		}
		return null;
	}	
	
	/**
	 * @throws CoreException
	 */
	private void addExtensionSplashHandlers() throws CoreException {
		// Update progress work units
		fMonitor.beginTask(NLS.bind(PDEUIMessages.UpdateSplashHandlerInModelAction_msgAddingExtension, 
				UpdateSplashHandlerInModelAction.F_SPLASH_HANDLERS_EXTENSION), 1); 	
		// Create the new extension
		IPluginExtension extension = createExtensionSplashHandlers();
		fModel.getPluginBase().add(extension);
		// Update progress work units
		fMonitor.done();		
	}
	
	/**
	 * @return
	 * @throws CoreException
	 */
	private IPluginExtension createExtensionSplashHandlers()
		throws CoreException {
		// Create the extension
		IPluginExtension extension = fModel.getFactory().createExtension();
		// Point
		extension.setPoint(UpdateSplashHandlerInModelAction.F_SPLASH_HANDLERS_EXTENSION);
		// NO id
		// NO name 
		// Create the extension's children
		createExtensionChildrenSplashHandlers(extension);
		
		return extension;
	}	
	
	/**
	 * @param extension
	 * @throws CoreException
	 */
	private void createExtensionChildrenSplashHandlers(
			IPluginExtension extension) throws CoreException {
		// Add a splash handler element
		addElementSplashHandler(extension);		
		// Add a product handler element
		addElementProductBinding(extension);		
	}

	/**
	 * @param extension
	 * @throws CoreException
	 */
	private void addElementSplashHandler(IPluginExtension extension)
			throws CoreException {
		// Create the element
		IPluginElement splashHandlerElement = 
			createElementSplashHandler(extension);
		// Ensure element was defined and add it to the extension
		if (splashHandlerElement != null) {
			// Extension uses the first element only when choosing a splash
			// handler. Always set as the first extension element to 
			// override any previous elements
			extension.add(0, splashHandlerElement);
		}
	}

	/**
	 * @param extension
	 * @throws CoreException
	 */
	private void addElementProductBinding(IPluginExtension extension)
			throws CoreException {
		// Create the element
		IPluginElement productBindingElement = 
			createElementProductBinding(extension);
		// Ensure element was defined and add it to the extension
		if (productBindingElement != null) {
			// Extension uses the first element only when choosing a splash
			// handler. Always set as the first extension element to 
			// override any previous elements
			extension.add(1, productBindingElement);
		}
	}

	/**
	 * @param extension
	 * @return
	 * @throws CoreException
	 */
	private IPluginElement createElementSplashHandler(
			IPluginExtension extension) throws CoreException{
		// Create the element
		IPluginElement element = 
			extension.getModel().getFactory().createElement(extension);
		// Element: Splash handler
		element.setName(UpdateSplashHandlerInModelAction.F_ELEMENT_SPLASH_HANDLER);
		// Attribute: ID
		element.setAttribute(UpdateSplashHandlerInModelAction.F_ATTRIBUTE_ID, fFieldID);
		// Attribute: Class
		element.setAttribute(UpdateSplashHandlerInModelAction.F_ATTRIBUTE_CLASS, fFieldClass);
		
		return element;
	}	
	
	/**
	 * @param extension
	 * @return
	 * @throws CoreException
	 */
	private IPluginElement createElementProductBinding(
			IPluginExtension extension) throws CoreException {
		// Create the element
		IPluginElement element = 
			extension.getModel().getFactory().createElement(extension);
		// Element: Product binding
		element.setName(UpdateSplashHandlerInModelAction.F_ELEMENT_PRODUCT_BINDING);
		// Attribute: Product ID
		element.setAttribute(UpdateSplashHandlerInModelAction.F_ATTRIBUTE_PRODUCT_ID, fFieldProductID);
		// Attribute: Splash ID
		element.setAttribute(UpdateSplashHandlerInModelAction.F_ATTRIBUTE_SPLASH_ID, fFieldSplashID);
		
		return element;
	}
	
	/**
	 * @param extension
	 * @throws CoreException
	 */
	private void modifyExtensionSplashHandlers(IPluginExtension extension) throws CoreException {
		// Update progress work units
		fMonitor.beginTask(NLS.bind(PDEUIMessages.UpdateSplashHandlerInModelAction_msgModifyingExtension, 
				UpdateSplashHandlerInModelAction.F_SPLASH_HANDLERS_EXTENSION), 1); 
		// modify the existing extension children
		modifyExtensionChildrenSplashHandlers(extension);
		// Update progress work units
		fMonitor.done();	
	}	
	
	/**
	 * @param extension
	 * @throws CoreException
	 */
	private void modifyExtensionChildrenSplashHandlers(
			IPluginExtension extension) throws CoreException {
		// Find a matching pre-generated splash handler element
		IPluginElement splashHandlerElement = findSplashHandlerElement(extension);
		// Check to see if one was found
		if (splashHandlerElement == null) {
			// No element found, add one
			addElementSplashHandler(extension);
		} else {
			// One element found, synchronize it
			syncSplashHandlerElement(splashHandlerElement);
		}
		
		// Find a matching pre-generated product handler element
		IPluginElement productBindingElement = findProductBindingElement(extension);
		// Check to see if one was found
		if (productBindingElement == null) {
			// No element found, add one
			addElementProductBinding(extension);
		} else {
			// One element found, synchronize it
			syncProductBindingElement(productBindingElement);
		}
	}	
	
	/**
	 * @param extension
	 * @return
	 */
	private IPluginElement findSplashHandlerElement(IPluginExtension extension) {
		// Check to see if the extension has any children
		if (extension.getChildCount() == 0) {
			// Extension has no children
			return null;
		}
		IPluginObject[] pluginObjects = extension.getChildren();
		// Process all children
		for (int j = 0; j < pluginObjects.length; j++) {
			if (pluginObjects[j] instanceof IPluginElement) {
				IPluginElement element = (IPluginElement)pluginObjects[j];
				// Find splash handler elements
				if (element.getName().equals(F_ELEMENT_SPLASH_HANDLER)) {
					// Get the id attribute
					IPluginAttribute idAttribute = 
						element.getAttribute(F_ATTRIBUTE_ID);
					// Check for the generated ID 
					if ((idAttribute != null) && 
							PDETextHelper.isDefined(idAttribute.getValue()) &&
							idAttribute.getValue().equals(fFieldID)) {
						// Matching element found
						return element;
					}	
				}					
			}
		}
		return null;
	}	
	
	/**
	 * @param element
	 * @throws CoreException
	 */
	private void syncSplashHandlerElement(IPluginElement element) throws CoreException {
		// Get the class attribute
		IPluginAttribute classAttribute = 
			element.getAttribute(F_ATTRIBUTE_CLASS);
		// Check to see if an update is necessary
		if ((classAttribute != null) && 
				PDETextHelper.isDefined(classAttribute.getValue()) &&
				classAttribute.getValue().equals(fFieldClass)) {
			// Exact match, no update necessary
			return;
		}
		// No match, override
		element.setAttribute(F_ATTRIBUTE_CLASS, fFieldClass);
	}
	
	/**
	 * @param element
	 * @throws CoreException
	 */
	private void syncProductBindingElement(IPluginElement element) throws CoreException {
		// Get the product ID attribute
		IPluginAttribute productIDAttribute = 
			element.getAttribute(F_ATTRIBUTE_PRODUCT_ID);
		// Check to see if an update is necessary
		if ((productIDAttribute != null) && 
				PDETextHelper.isDefined(productIDAttribute.getValue()) &&
				productIDAttribute.getValue().equals(fFieldProductID)) {
			// Exact match, no update necessary
			return;
		}
		// No match, override
		element.setAttribute(F_ATTRIBUTE_PRODUCT_ID, fFieldProductID);
	}	
	
	/**
	 * @param extension
	 * @return
	 */
	private IPluginElement findProductBindingElement(IPluginExtension extension) {
		// Check to see if the extension has any children
		if (extension.getChildCount() == 0) {
			// Extension has no children
			return null;
		}
		IPluginObject[] pluginObjects = extension.getChildren();
		// Process all children
		for (int j = 0; j < pluginObjects.length; j++) {
			if (pluginObjects[j] instanceof IPluginElement) {
				IPluginElement element = (IPluginElement)pluginObjects[j];
				// Find splash handler elements
				if (element.getName().equals(F_ELEMENT_PRODUCT_BINDING)) {
					// Get the id attribute
					IPluginAttribute splashIDAttribute = 
						element.getAttribute(F_ATTRIBUTE_SPLASH_ID);
					// Check for the generated ID 
					if ((splashIDAttribute != null) && 
							PDETextHelper.isDefined(splashIDAttribute.getValue()) &&
							splashIDAttribute.getValue().equals(fFieldSplashID)) {
						// Matching element found
						return element;
					}	
				}					
			}
		}		
		return null;
	}		
	
	/**
	 * @return
	 * @throws CoreException
	 */
	private IPluginExtension createExtensionSplash() throws CoreException {
		
		String fullExtensionPointID = fFieldPluginID + 
			'.' + UpdateSplashHandlerInModelAction.F_SPLASH_EXTENSION_POINT;	
		// Create the extension
		IPluginExtension extension = fModel.getFactory().createExtension();
		// Point
		extension.setPoint(fullExtensionPointID);
		// NO id
		// NO name 
		// Create the extension's children
		createExtensionChildrenSplash(extension);
			
		return extension;		
	}

	/**
	 * @param extension
	 * @throws CoreException
	 */
	private void createExtensionChildrenSplash(IPluginExtension extension) throws CoreException {
		
		String iconsDir = "icons" + '/'; //$NON-NLS-1$
		
		// Splash element: Application Framework
		IPluginElement splashElementAf = 
			createElementSplash(extension, "af", iconsDir + "af.png", PDEUIMessages.UpdateSplashHandlerInModelAction_nameApplicationFramework); //$NON-NLS-1$ //$NON-NLS-2$
		if (splashElementAf != null) {
			extension.add(splashElementAf);
		}		
		// Splash element: Embedded
		IPluginElement splashElementEmbedded = 
			createElementSplash(extension, "embedded", iconsDir + "embedded.png", PDEUIMessages.UpdateSplashHandlerInModelAction_nameEmbedded); //$NON-NLS-1$ //$NON-NLS-2$
		if (splashElementEmbedded != null) {
			extension.add(splashElementEmbedded);
		}	
		// Splash element: Enterprise
		IPluginElement splashElementEnterprise = 
			createElementSplash(extension, "enterprise", iconsDir + "enterprise.png", PDEUIMessages.UpdateSplashHandlerInModelAction_nameEnterprise); //$NON-NLS-1$ //$NON-NLS-2$
		if (splashElementEnterprise != null) {
			extension.add(splashElementEnterprise);
		}	
		// Splash element: Languages
		IPluginElement splashElementLanguages = 
			createElementSplash(extension, "languages", iconsDir + "languages.png", PDEUIMessages.UpdateSplashHandlerInModelAction_nameLanguages); //$NON-NLS-1$ //$NON-NLS-2$
		if (splashElementLanguages != null) {
			extension.add(splashElementLanguages);
		}	
		// Splash element: RCP
		IPluginElement splashElementRCP = 
			createElementSplash(extension, "rcp", iconsDir + "rcp.png", PDEUIMessages.UpdateSplashHandlerInModelAction_nameRCP); //$NON-NLS-1$ //$NON-NLS-2$
		if (splashElementRCP != null) {
			extension.add(splashElementRCP);
		}			
	}

	/**
	 * @param extension
	 * @param id
	 * @param icon
	 * @param tooltip
	 * @return
	 * @throws CoreException
	 */
	private IPluginElement createElementSplash(
			IPluginExtension extension, String id, String icon,
			String tooltip) throws CoreException {
		// Create the element
		IPluginElement element = 
			extension.getModel().getFactory().createElement(extension);
		// Element: Splash handler
		element.setName(UpdateSplashHandlerInModelAction.F_ELEMENT_SPLASH);
		// Attribute: ID
		element.setAttribute(UpdateSplashHandlerInModelAction.F_ATTRIBUTE_ID, id);
		// Attribute: Icon
		element.setAttribute(UpdateSplashHandlerInModelAction.F_ATTRIBUTE_ICON, icon);
		// Attribute: Tooltip
		element.setAttribute(UpdateSplashHandlerInModelAction.F_ATTRIBUTE_TOOLTIP, tooltip);		
		
		return element;		
	}	

	/**
	 * @param template
	 * @return true if the extension template was selected; false, otherwise
	 */
	public static boolean isExtensibleTemplateSelected(String template) {
		if (template.equals(UpdateSplashHandlerInModelAction.F_SPLASH_SCREEN_TYPE_CHOICES[2][0])) {
			return true;
		}
		return false;
	}	
	
}
