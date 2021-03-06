package org.eclipse.angularjs.internal.ui.views;

import org.eclipse.angularjs.core.AngularProject;
import org.eclipse.angularjs.core.link.AngularLinkHelper;
import org.eclipse.angularjs.internal.ui.AngularUIPlugin;
import org.eclipse.angularjs.internal.ui.Trace;
import org.eclipse.angularjs.internal.ui.views.actions.LexicalSortingAction;
import org.eclipse.angularjs.internal.ui.views.actions.LinkToControllerAction;
import org.eclipse.angularjs.internal.ui.views.actions.RefreshExplorerAction;
import org.eclipse.angularjs.internal.ui.views.actions.UnLinkToControllerAction;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.navigator.CommonViewer;

import tern.TernResourcesManager;
import tern.angular.AngularType;
import tern.angular.modules.IAngularElement;
import tern.angular.modules.IModule;
import tern.angular.protocol.outline.AngularOutline;
import tern.angular.protocol.outline.IAngularOutlineListener;
import tern.eclipse.ide.core.resources.TernDocumentFile;
import tern.eclipse.ide.ui.views.AbstractTernContentOutlinePage;

public class AngularContentOutlinePage extends AbstractTernContentOutlinePage implements IAngularOutlineListener {

	private LinkToControllerAction linkAction;
	private UnLinkToControllerAction unLinkAction;
	// private GoToDefinitionAction openAction;
	private RefreshExplorerAction refreshAction;
	private LexicalSortingAction sortAction;

	public AngularContentOutlinePage(IProject project, AngularExplorerView view) {
		super(project, view);
	}

	@Override
	protected String getViewerId() {
		return AngularUIPlugin.PLUGIN_ID + ".outline";
	}

	@Override
	public IFile getFile() {
		return null;
	}

	@Override
	protected void registerActions(IToolBarManager manager) {
		sortAction = new LexicalSortingAction(this);
		manager.add(sortAction);
		this.linkAction = new LinkToControllerAction(this);
		manager.add(linkAction);
		this.unLinkAction = new UnLinkToControllerAction(this);
		manager.add(unLinkAction);
		// this.openAction = new GoToDefinitionAction(this);
		// manager.add(openAction);
		this.refreshAction = new RefreshExplorerAction(this);
		manager.add(refreshAction);
		super.registerActions(manager);
	}

	@Override
	protected void registerContextMenu(Control control) {
		MenuManager contextMenu = new MenuManager();
		contextMenu.add(linkAction);
		contextMenu.add(unLinkAction);
		// contextMenu.add(openAction);
		contextMenu.add(refreshAction);

		Menu menu = contextMenu.createContextMenu(control);
		control.setMenu(menu);
	}

	private boolean isHTMLFile(IFile resource) {
		return (resource != null && TernResourcesManager.isHTMLFile(resource));
	}

	public void updateEnabledLinkActions(boolean isLinked) {
		this.linkAction.setEnabled(!isLinked);
		this.unLinkAction.setEnabled(isLinked);
	}

	@Override
	protected void updateEnabledActions() {
		this.linkAction.setEnabled(false);
		this.unLinkAction.setEnabled(false);
		IStructuredSelection selection = (IStructuredSelection) getViewer().getSelection();
		if (!selection.isEmpty()) {
			boolean htmlFile = isHTMLFile(getCurrentFile());
			if (htmlFile) {
				Object firstSelection = selection.getFirstElement();
				if (firstSelection instanceof IAngularElement) {
					String elementId = null;
					IAngularElement element = (IAngularElement) firstSelection;
					IModule module = element.getModule();
					if (module != null) {
						boolean isLinked = AngularLinkHelper.isSameController(getCurrentFile(), module.getName(),
								element.isType(AngularType.module) ? null : element.getName(), null);
						updateEnabledLinkActions(isLinked);
					}
				}
			}
		}
	}

	@Override
	public void changed(AngularOutline outline) {
		documentChanged(null);
	}

	@Override
	protected boolean isRefreshOutline(IFile oldFile, IFile newFile) {
		IProject oldProject = oldFile != null ? oldFile.getProject() : null;
		IProject newProject = newFile != null ? newFile.getProject() : null;
		if (oldProject != newProject) {
			// project has changed
			if (oldProject != null) {
				try {
					AngularProject angularProject = AngularProject.getAngularProject(oldProject);
					angularProject.removeAngularOutlineListener(this);
				} catch (Exception e) {
					Trace.trace(Trace.SEVERE, "Error while getting angular project.", e);
				}
			}
			if (newProject != null) {
				try {
					AngularProject angularProject = AngularProject.getAngularProject(newProject);
					angularProject.addAngularOutlineListener(this);
				} catch (Exception e) {
					Trace.trace(Trace.SEVERE, "Error while getting angular project.", e);
				}
				// project has changed, refresh the angular outline
				return true;
			}
		}
		return false;
	}
}
