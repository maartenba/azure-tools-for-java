/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.appservice;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleListCellRenderer;
import com.intellij.ui.components.fields.ExtendableTextComponent;
import com.microsoft.azure.toolkit.intellij.common.AzureComboBox;
import com.microsoft.azuretools.azurecommons.helpers.NotNull;
import com.microsoft.azuretools.azurecommons.helpers.Nullable;
import org.apache.commons.lang3.StringUtils;
import rx.Subscription;

import javax.swing.*;
import java.util.List;
import java.util.Objects;

public abstract class AppServiceComboBox<T extends AppServiceComboBoxModel> extends AzureComboBox<T> {

    protected Project project;
    protected Subscription subscription;

    private T configModel;

    public AppServiceComboBox(final Project project) {
        super(false);
        this.project = project;
        this.setRenderer(new AppCombineBoxRender(this));
    }

    public void setConfigModel(T configModel) {
        this.configModel = configModel;
        setValue(new ItemReference<>(item -> AppServiceComboBoxModel.isSameApp(item, configModel)));
    }

    @NotNull
    @Override
    protected List<? extends T> loadItems() throws Exception {
        final List<T> items = loadAppServiceModels();
        if (configModel != null && configModel.isNewCreateResource()) {
            final boolean exist = items.stream().anyMatch(item -> AppServiceComboBoxModel.isSameApp(item, configModel));
            if (!exist) {
                items.add(configModel);
            }
        }
        return items;
    }

    protected abstract List<T> loadAppServiceModels() throws Exception;

    @Nullable
    @Override
    protected ExtendableTextComponent.Extension getExtension() {
        return ExtendableTextComponent.Extension.create(
            AllIcons.General.Add, "Create", this::createResource);
    }

    @Override
    protected String getItemText(final Object item) {
        if (item instanceof AppServiceComboBoxModel) {
            final AppServiceComboBoxModel selectedItem = (AppServiceComboBoxModel) item;
            return selectedItem.isNewCreateResource() ?
                String.format("(New) %s", selectedItem.getAppName()) : selectedItem.getAppName();
        } else {
            return Objects.toString(item, StringUtils.EMPTY);
        }
    }

    protected abstract void createResource();

    public class AppCombineBoxRender extends SimpleListCellRenderer {
        private final JComboBox comboBox;

        public AppCombineBoxRender(JComboBox comboBox) {
            this.comboBox = comboBox;
        }

        @Override
        public void customize(JList list, Object value, int index, boolean b, boolean b1) {
            if (value instanceof AppServiceComboBoxModel) {
                final AppServiceComboBoxModel app = (AppServiceComboBoxModel) value;
                if (index >= 0) {
                    setText(getAppServiceLabel(app));
                } else {
                    setText(app.getAppName());
                }
            }
        }

        private String getAppServiceLabel(AppServiceComboBoxModel appServiceModel) {
            final String appServiceName = appServiceModel.isNewCreateResource() ?
                String.format("(New) %s", appServiceModel.getAppName()) : appServiceModel.getAppName();
            final String runtime = appServiceModel.getRuntime();
            final String resourceGroup = appServiceModel.getResourceGroup();

            return String.format("<html><div>%s</div></div><small>Runtime: %s | Resource Group: %s</small></html>",
                appServiceName, runtime, resourceGroup);
        }
    }
}
