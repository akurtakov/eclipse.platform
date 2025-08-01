/*******************************************************************************
 * Copyright (c) 2007, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Martin Oberhuber (Wind River) - [225853][api] Provide more default functionality in TerminalConnectorImpl
 *******************************************************************************/
package org.eclipse.terminal.internal.speedtest;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.terminal.connector.ISettingsStore;
import org.eclipse.terminal.connector.ITerminalControl;
import org.eclipse.terminal.connector.Logger;
import org.eclipse.terminal.connector.NullSettingsStore;
import org.eclipse.terminal.connector.TerminalState;
import org.eclipse.terminal.connector.provider.AbstractTerminalConnector;
import org.eclipse.terminal.control.TerminalTitleRequestor;

public class SpeedTestConnector extends AbstractTerminalConnector {
	final SpeedTestSettings fSettings = new SpeedTestSettings();
	InputStream fInputStream;
	OutputStream fOutputStream;
	SpeedTestConnection fConnection;

	public SpeedTestConnector() {
	}

	@Override
	synchronized public void connect(ITerminalControl control) {
		super.connect(control);
		fControl.setState(TerminalState.CONNECTING);
		String file = fSettings.getInputFile();
		try {
			fInputStream = new BufferedInputStream(new FileInputStream(file));
		} catch (FileNotFoundException e) {
			disconnect();
			fControl.setMsg(file + ": " + e.getLocalizedMessage());
			return;
		}
		fOutputStream = System.out;
		fControl.setTerminalTitle(fSettings.getInputFile(), TerminalTitleRequestor.OTHER);
		fConnection = new SpeedTestConnection(fInputStream, fSettings, fControl);
		fConnection.start();
	}

	@Override
	synchronized public void doDisconnect() {
		if (fConnection != null) {
			fConnection.interrupt();
			fConnection = null;
		}
		if (fInputStream != null) {
			try {
				fInputStream.close();
			} catch (Exception exception) {
				Logger.logException(exception);
			}
		}
		fInputStream = null;
		if (fOutputStream != null) {
			try {
				fOutputStream.close();
			} catch (Exception exception) {
				Logger.logException(exception);
			}
		}
		fOutputStream = null;
	}

	synchronized public InputStream getInputStream() {
		return fInputStream;
	}

	@Override
	synchronized public OutputStream getTerminalToRemoteStream() {
		return fOutputStream;
	}

	@Override
	public String getSettingsSummary() {
		return fSettings.getInputFile();
	}

	@Override
	public void setDefaultSettings() {
		fSettings.load(new NullSettingsStore());
	}

	@Override
	public void load(ISettingsStore store) {
		fSettings.load(store);
	}

	@Override
	public void save(ISettingsStore store) {
		fSettings.save(store);
	}

	@Override
	public void setTerminalSize(int newWidth, int newHeight) {
	}

}
