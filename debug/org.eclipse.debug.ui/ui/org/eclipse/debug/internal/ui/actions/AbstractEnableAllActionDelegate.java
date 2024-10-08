/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.actions;

import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWindowActionDelegate;

public abstract class AbstractEnableAllActionDelegate
		implements IViewActionDelegate, IActionDelegate2, IWorkbenchWindowActionDelegate {

	private IAction fAction;

	/**
	 * Needed for reflective creation
	 */

	@Override
	public void dispose() {
		fAction = null;
	}

	@Override
	public void init(IAction action) {
		fAction = action;
	}

	/**
	 * Returns this delegate's action.
	 *
	 * @return the underlying <code>IAction</code>
	 */
	protected IAction getAction() {
		return fAction;
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	@Override
	public void init(IViewPart view) {
		initialize();
		update();
	}

	@Override
	public void init(IWorkbenchWindow window) {
		initialize();
		update();
	}

	/**
	 * Initializes any listeners, etc.
	 */
	protected abstract void initialize();

	/**
	 * Update enablement.
	 */
	protected void update() {
		IAction action = getAction();
		if (action != null) {
			action.setEnabled(isEnabled());
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection s) {
		// do nothing
	}

	@Override
	public void run(IAction action) {

	}

	protected abstract boolean isEnabled();

}
