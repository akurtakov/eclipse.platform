package org.eclipse.debug.internal.ui.views;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;

import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.ClearOutputAction;
import org.eclipse.debug.internal.ui.ConsoleTerminateActionDelegate;
import org.eclipse.debug.internal.ui.ControlAction;
import org.eclipse.debug.internal.ui.DebugUIMessages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.TextViewerAction;
import org.eclipse.debug.internal.ui.TextViewerGotoLineAction;
import org.eclipse.debug.ui.AbstractDebugView;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.ITextInputListener;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.IUpdate;

public class ConsoleView extends AbstractDebugEventHandlerView implements IDocumentListener, ISelectionChangedListener {

	protected ClearOutputAction fClearOutputAction= null;

	protected Map fGlobalActions= new HashMap(10);
	protected List fSelectionActions = new ArrayList(3);
	
	protected IDocument fCurrentDocument= null;
	
	/**
	 * The current process being viewed, or <code>null</code.
	 */
	private IProcess fProcess;
	
	/**
	 * @see AbstractDebugView#createViewer(Composite)
	 */
	protected Viewer createViewer(Composite parent) {
		ConsoleViewer cv = new ConsoleViewer(parent);		
		cv.getSelectionProvider().addSelectionChangedListener(getSelectionChangedListener());
		cv.addTextInputListener(getTextInputListener());
		getSite().setSelectionProvider(cv.getSelectionProvider());
		
		// listen to selection changes in the debug view
		DebugSelectionManager.getDefault().addSelectionChangedListener(this,getSite().getPage(), IDebugUIConstants.ID_DEBUG_VIEW);
		
		setEventHandler(new ConsoleViewEventHandler(this, cv));
		return cv;
	}
	
	/**
	 * @see AbstractDebugView#getHelpContextId()
	 */
	protected String getHelpContextId() {
		return IDebugHelpContextIds.CONSOLE_VIEW;
	}
	
	/** 
	 * Sets the console to view the documents steams
	 * associated with the given process.
	 */
	public void setViewerInput(IProcess process) {
		if (getViewer() == null || getViewer().getControl() == null || getViewer().getControl().isDisposed()) {
			return;
		}
		if (getProcess() == process) {
			// do nothing if the input is the same as what is
			// being viewed. If this is the first input, set
			// the console to an empty document
			if (getConsoleViewer().getDocument() == null) {
				getConsoleViewer().setDocument(new ConsoleDocument(null));
				updateActions();
			}			
			return;
		}
		setProcess(process)	;
		Runnable r = new Runnable() {
			public void run() {
				if (getViewer() == null) {
					return;
				}
				Control viewerControl= getViewer().getControl();
				if (viewerControl == null || viewerControl.isDisposed()) {
					return;
				}
				IDocument doc = null;
				if (getProcess() != null) {
					doc = DebugUIPlugin.getDefault().getConsoleDocument(getProcess());
				}
				if (doc == null) {
					doc = new ConsoleDocument(null);
				}
				getConsoleViewer().setDocument(doc);
				// update view title
				String title = null;
				if (getProcess() == null) {
					title = "Console";
				} else {
					// use debug target title if applicable
					Object obj = getProcess().getAdapter(IDebugTarget.class);
					if (obj == null) {
						obj = getProcess();
					}
					title = "Console [" + DebugUIPlugin.getModelPresentation().getText(obj) + "]";
				}
				setTitle(title);
				updateActions();
			}
		};
		asyncExec(r);
	}
		
	/**
	 * @see AbstractDebugView#createActions()
	 */
	protected void createActions() {
		fClearOutputAction= new ClearOutputAction(getConsoleViewer());
		
		// In order for the clipboard actions to accessible via their shortcuts
		// (e.g., Ctrl-C, Ctrl-V), we *must* set a global action handler for
		// each action		
		IActionBars actionBars= getViewSite().getActionBars();
		TextViewerAction action= new TextViewerAction(getTextViewer(), TextViewer.CUT);
		action.configureAction(DebugUIMessages.getString("ConsoleView.Cu&t@Ctrl+X_3"), DebugUIMessages.getString("ConsoleView.Cut_4"), DebugUIMessages.getString("ConsoleView.Cut_5")); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		setGlobalAction(actionBars, ITextEditorActionConstants.CUT, action);
		action= new TextViewerAction(getTextViewer(), TextViewer.COPY);
		action.configureAction(DebugUIMessages.getString("ConsoleView.&Copy@Ctrl+C_6"), DebugUIMessages.getString("ConsoleView.Copy_7"), DebugUIMessages.getString("ConsoleView.Copy_8")); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		setGlobalAction(actionBars, ITextEditorActionConstants.COPY, action);
		action= new TextViewerAction(getTextViewer(), TextViewer.PASTE);
		action.configureAction(DebugUIMessages.getString("ConsoleView.&Paste@Ctrl+V_9"), DebugUIMessages.getString("ConsoleView.Paste_10"), DebugUIMessages.getString("ConsoleView.Paste_Clipboard_Text_11")); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		setGlobalAction(actionBars, ITextEditorActionConstants.PASTE, action);
		action= new TextViewerAction(getTextViewer(), TextViewer.SELECT_ALL);
		action.configureAction(DebugUIMessages.getString("ConsoleView.Select_&All@Ctrl+A_12"), DebugUIMessages.getString("ConsoleView.Select_All_13"), DebugUIMessages.getString("ConsoleView.Select_All_14")); //$NON-NLS-3$ //$NON-NLS-2$ //$NON-NLS-1$
		setGlobalAction(actionBars, ITextEditorActionConstants.SELECT_ALL, action);
		
		//XXX Still using "old" resource access
		ResourceBundle bundle= ResourceBundle.getBundle("org.eclipse.debug.internal.ui.DebugUIMessages"); //$NON-NLS-1$
		setGlobalAction(actionBars, ITextEditorActionConstants.FIND, new FindReplaceAction(bundle, "find_replace_action.", this));				 //$NON-NLS-1$
	
		action= new TextViewerGotoLineAction(getConsoleViewer());
		setGlobalAction(actionBars, ITextEditorActionConstants.GOTO_LINE, action);				
		actionBars.updateActionBars();
		
		getConsoleViewer().getTextWidget().addVerifyKeyListener(new VerifyKeyListener() {
			public void verifyKey(VerifyEvent event) {
				IAction gotoLine= (IAction)fGlobalActions.get(ITextEditorActionConstants.GOTO_LINE);
				if (event.stateMask == SWT.CTRL && event.keyCode == 0 && event.character == 0x0C && event.keyCode == 0 && gotoLine.isEnabled()) {
					gotoLine.run();
					event.doit= false;
				}
			}
		});
		
		fSelectionActions.add(ITextEditorActionConstants.CUT);
		fSelectionActions.add(ITextEditorActionConstants.COPY);
		fSelectionActions.add(ITextEditorActionConstants.PASTE);
		updateAction(ITextEditorActionConstants.FIND);
		
		ConsoleTerminateActionDelegate delegate = new ConsoleTerminateActionDelegate();
		delegate.init(this);
		IAction terminate = new ControlAction(getViewer(), delegate);
		setAction("Terminate", terminate);
				
		// initialize input, after viewer has been created
		setViewerInput(DebugUITools.getCurrentProcess());
	}

	protected void setGlobalAction(IActionBars actionBars, String actionID, IAction action) {
		fGlobalActions.put(actionID, action); 
		actionBars.setGlobalActionHandler(actionID, action);
	}
	
	/**
	 * @see AbstractDebugView#configureToolBar(IToolBarManager)
	 */
	protected void configureToolBar(IToolBarManager mgr) {
		mgr.add(fClearOutputAction);
		mgr.add(getAction("Terminate"));
	}
	/**
	 * Adds the text manipulation actions to the <code>ConsoleViewer</code>
	 */
	protected void fillContextMenu(IMenuManager menu) {
		Point selectionRange= getConsoleViewer().getTextWidget().getSelection();
		ConsoleDocument doc= (ConsoleDocument) getConsoleViewer().getDocument();
		if (doc == null) {
			return;
		}
		if (doc.isReadOnly() || selectionRange.x < doc.getStartOfEditableContent()) {
			menu.add((IAction)fGlobalActions.get(ITextEditorActionConstants.COPY));
			menu.add((IAction)fGlobalActions.get(ITextEditorActionConstants.SELECT_ALL));
		} else {
			menu.add((IAction)fGlobalActions.get(ITextEditorActionConstants.CUT));
			menu.add((IAction)fGlobalActions.get(ITextEditorActionConstants.COPY));
			menu.add((IAction)fGlobalActions.get(ITextEditorActionConstants.PASTE));
			menu.add((IAction)fGlobalActions.get(ITextEditorActionConstants.SELECT_ALL));
		}

		menu.add(new Separator("FIND")); //$NON-NLS-1$
		menu.add((IAction)fGlobalActions.get(ITextEditorActionConstants.FIND));
		menu.add((IAction)fGlobalActions.get(ITextEditorActionConstants.GOTO_LINE));
		menu.add(fClearOutputAction);
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
		menu.add(getAction("Terminate"));
	}

	/**
	 * @see WorkbenchPart#getAdapter(Class)
	 */
	public Object getAdapter(Class required) {
		if (IFindReplaceTarget.class.equals(required)) {
			return getConsoleViewer().getFindReplaceTarget();
		}
		if (Widget.class.equals(required)) {
			return getConsoleViewer().getTextWidget();
		}
		return super.getAdapter(required);
	}

	protected final ISelectionChangedListener getSelectionChangedListener() {
		return new ISelectionChangedListener() {
				public void selectionChanged(SelectionChangedEvent event) {
					updateSelectionDependentActions();
				}
			};
	}
	
	protected final ITextInputListener getTextInputListener() {
		return new ITextInputListener() {
				public void inputDocumentAboutToBeChanged(IDocument old, IDocument nw) {
					if (old != null) {
						old.removeDocumentListener(ConsoleView.this);
					}
					fCurrentDocument = nw;
					if (nw != null) {
						nw.addDocumentListener(ConsoleView.this);
					}
				}
				public void inputDocumentChanged(IDocument doc, IDocument doc2) {
					updateAction(ITextEditorActionConstants.FIND);
				}
			};
	}

	protected void updateSelectionDependentActions() {
		Iterator iterator= fSelectionActions.iterator();
		while (iterator.hasNext())
			updateAction((String)iterator.next());		
	}

	protected void updateAction(String actionId) {
		IAction action= (IAction)fGlobalActions.get(actionId);
		if (action instanceof IUpdate) {
			((IUpdate) action).update();
		}
	}
	
	public void dispose() {
		DebugSelectionManager.getDefault().removeSelectionChangedListener(this, getSite().getPage(), IDebugUIConstants.ID_DEBUG_VIEW);
		if (getConsoleViewer() != null) {
			getConsoleViewer().dispose();
		}
		if (fCurrentDocument != null) {
			fCurrentDocument.removeDocumentListener(this);
		}
		super.dispose();
	}
	/**
	 * @see IDocumentListener#documentAboutToBeChanged(DocumentEvent)
	 */
	public void documentAboutToBeChanged(DocumentEvent arg0) {
	}

	/**
	 * @see IDocumentListener#documentChanged(DocumentEvent)
	 */
	public void documentChanged(DocumentEvent arg0) {
		updateAction(ITextEditorActionConstants.FIND);
	}
	
	/**
	 * Change to show the document associated with the 
	 * selection in the debug view.
	 * 
	 * @see ISelectionChangedListener#selectionChanged(SelectionChangedEvent)
	 */
	public void selectionChanged(SelectionChangedEvent event) {
		setViewerInput(DebugUITools.getCurrentProcess());
	}

	public ConsoleViewer getConsoleViewer() {
		return (ConsoleViewer)getViewer();
	}
	
	/**
	 * Sets the process being viewed
	 * 
	 * @param process process or <code>null</code>
	 */
	private void setProcess(IProcess process) {
		fProcess = process;
	}
	
	/**
	 * Returns the process being viewed, or <code>null</code>
	 * 
	 * @return process
	 */
	public IProcess getProcess() {
		return fProcess;
	}
}

