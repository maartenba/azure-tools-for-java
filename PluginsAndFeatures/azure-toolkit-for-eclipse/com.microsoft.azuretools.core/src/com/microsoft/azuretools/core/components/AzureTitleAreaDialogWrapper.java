/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.components;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.swt.widgets.Shell;

import com.microsoft.azuretools.telemetry.TelemetryProperties;

public abstract class AzureTitleAreaDialogWrapper extends TitleAreaDialog implements AzureDialogProtertiesHelper, TelemetryProperties{
    public AzureTitleAreaDialogWrapper(Shell parentShell) {
        super(parentShell);
    }

    @Override
    protected void okPressed() {
        sentTelemetry(OK);
        super.okPressed();
    }

    @Override
    protected void cancelPressed() {
        sentTelemetry(CANCEL);
        super.cancelPressed();
    }

    @Override
    public Map<String, String> toProperties() {
        return new HashMap<>();
    }
}
