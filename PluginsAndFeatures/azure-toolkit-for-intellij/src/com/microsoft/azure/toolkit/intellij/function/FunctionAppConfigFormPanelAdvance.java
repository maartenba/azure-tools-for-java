/*
 * Copyright (c) Microsoft Corporation
 *
 * All rights reserved.
 *
 * MIT License
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and
 * to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of
 * the Software.
 *
 * THE SOFTWARE IS PROVIDED *AS IS*, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO
 * THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
 * TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.microsoft.azure.toolkit.intellij.function;

import com.intellij.openapi.project.Project;
import com.microsoft.azure.toolkit.intellij.appservice.AppServiceConfigFormPanelAdvanced;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.function.FunctionAppConfig;
import com.microsoft.intellij.runner.functions.component.ApplicationInsightsPanel;

import javax.swing.*;
import java.util.List;

public class FunctionAppConfigFormPanelAdvance extends JPanel implements AzureFormPanel<FunctionAppConfig> {
    private JTabbedPane tabPane;
    private JPanel pnlRoot;
    private Project project;
    private AppServiceConfigFormPanelAdvanced appServiceConfigPanelAdvanced;
    private ApplicationInsightsComboBox applicationInsightsComboBox;
    private JRadioButton noRadioButton;
    private JRadioButton yesRadioButton;
    private ApplicationInsightsPanel applicationInsightsPanel;

    public FunctionAppConfigFormPanelAdvance(final Project project) {
        this.project = project;
    }

    @Override
    public void setVisible(final boolean visible) {
        pnlRoot.setVisible(visible);
    }

    @Override
    public FunctionAppConfig getData() {
        return (FunctionAppConfig) appServiceConfigPanelAdvanced.getData();
    }

    @Override
    public void setData(final FunctionAppConfig data) {
        appServiceConfigPanelAdvanced.setData(data);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return appServiceConfigPanelAdvanced.getInputs();
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        appServiceConfigPanelAdvanced = new AppServiceConfigFormPanelAdvanced(project, () -> FunctionAppConfig.builder().build());
        // Function does not support file deployment
        appServiceConfigPanelAdvanced.setDeploymentVisible(false);

        applicationInsightsPanel = new ApplicationInsightsPanel();
        applicationInsightsComboBox = new ApplicationInsightsComboBox();

        appServiceConfigPanelAdvanced.getSelectorSubscription().addItemListener(event -> {
            applicationInsightsComboBox.setSubscription(appServiceConfigPanelAdvanced.getSelectorSubscription().getValue());
        });
    }
}
