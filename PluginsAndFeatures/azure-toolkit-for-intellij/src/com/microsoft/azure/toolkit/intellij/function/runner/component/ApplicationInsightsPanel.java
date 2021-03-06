/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.component;

import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.management.applicationinsights.v2015_05_01.ApplicationInsightsComponent;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.intellij.CommonConst;
import com.microsoft.tooling.msservices.helpers.azure.sdk.AzureSDKManager;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang.StringUtils;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.util.Collections;
import java.util.List;

import static com.microsoft.intellij.ui.messages.AzureBundle.message;

public class ApplicationInsightsPanel extends JPanel {
    private static final String CREATE_NEW_APPLICATION_INSIGHTS = message("function.applicationInsights.create");

    private JComboBox cbInsights;
    private JPanel pnlRoot;
    private String subscriptionId;

    private Disposable rxDisposable;
    private ApplicationInsightsWrapper selectWrapper;
    private ApplicationInsightsWrapper newInsightsWrapper;

    public ApplicationInsightsPanel() {
        cbInsights.setRenderer(new SimpleListCellRenderer() {
            @Override
            public void customize(@NotNull final JList list,
                                  final Object o,
                                  final int i,
                                  final boolean b,
                                  final boolean b1) {
                setText(o == null ? StringUtils.EMPTY : o.toString());
            }
        });

        cbInsights.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(final PopupMenuEvent e) {
                onSelectApplicationInsights();
            }
        });

        newInsightsWrapper = ApplicationInsightsWrapper.wrapperNewInsightsInstance();
    }

    @AzureOperation(
        name = "ai.list.subscription",
        params = {"subscriptionId"},
        type = AzureOperation.Type.SERVICE
    )
    public void loadApplicationInsights(String subscriptionId) {
        this.subscriptionId = subscriptionId;
        beforeLoadApplicationInsights();
        if (rxDisposable != null && !rxDisposable.isDisposed()) {
            rxDisposable.dispose();
        }
        rxDisposable = Observable
            .fromCallable(() -> AzureSDKManager.getInsightsResources(subscriptionId))
            .subscribeOn(Schedulers.io())
            .doOnError((e) -> fillApplicationInsights(Collections.emptyList()))
            .subscribe(this::fillApplicationInsights);
    }

    public void changeDefaultApplicationInsightsName(String name) {
        newInsightsWrapper.setName(name);
        cbInsights.repaint();
    }

    public boolean isCreateNewInsights() {
        return selectWrapper != null && selectWrapper.isNewCreated;
    }

    public String getApplicationInsightsInstrumentKey() {
        return selectWrapper == null ? null : selectWrapper.instrumentKey;
    }

    public String getNewApplicationInsightsName() {
        return selectWrapper == null ? null : selectWrapper.name;
    }

    public JComponent getComboComponent() {
        return cbInsights;
    }

    private void onSelectApplicationInsights() {
        final Object selectedObject = cbInsights.getSelectedItem();
        if (CREATE_NEW_APPLICATION_INSIGHTS.equals(selectedObject)) {
            AzureTaskManager.getInstance().runLater(this::onSelectCreateApplicationInsights);
        } else if (selectedObject instanceof ApplicationInsightsWrapper) {
            selectWrapper = (ApplicationInsightsWrapper) selectedObject;
        }
    }

    private void onSelectCreateApplicationInsights() {
        cbInsights.setSelectedItem(null);
        cbInsights.setPopupVisible(false);
        final CreateApplicationInsightsDialog dialog = new CreateApplicationInsightsDialog();
        dialog.pack();
        if (dialog.showAndGet()) {
            newInsightsWrapper.setName(dialog.getApplicationInsightsName());
            selectWrapper = newInsightsWrapper;
        }
        cbInsights.setSelectedItem(selectWrapper);
    }

    private void fillApplicationInsights(final List<ApplicationInsightsComponent> applicationInsightsComponents) {
        cbInsights.removeAllItems();
        cbInsights.setEnabled(true);
        cbInsights.addItem(CREATE_NEW_APPLICATION_INSIGHTS);
        cbInsights.addItem(newInsightsWrapper);
        applicationInsightsComponents
                .forEach(component -> cbInsights.addItem(ApplicationInsightsWrapper.wrapperInsightsInstance(component)));
        final ApplicationInsightsWrapper toSelectWrapper =
                selectWrapper != null && applicationInsightsComponents.contains(selectWrapper) ?
                selectWrapper : newInsightsWrapper;
        cbInsights.setSelectedItem(toSelectWrapper);
        onSelectApplicationInsights();
    }

    private void beforeLoadApplicationInsights() {
        cbInsights.removeAllItems();
        cbInsights.setEnabled(false);
        cbInsights.addItem(CommonConst.LOADING_TEXT);
    }

    static class ApplicationInsightsWrapper {
        private String name;
        private String resourceGroup;
        private String instrumentKey;
        private boolean isNewCreated;

        public static ApplicationInsightsWrapper wrapperNewInsightsInstance() {
            final ApplicationInsightsWrapper result = new ApplicationInsightsWrapper();
            result.isNewCreated = true;
            return result;
        }

        public static ApplicationInsightsWrapper wrapperInsightsInstance(ApplicationInsightsComponent component) {
            final ApplicationInsightsWrapper result = new ApplicationInsightsWrapper();
            result.name = component.name();
            result.resourceGroup = component.resourceGroupName();
            result.instrumentKey = component.instrumentationKey();
            result.isNewCreated = false;
            return result;
        }

        public String getName() {
            return name;
        }

        public boolean isNewCreated() {
            return isNewCreated;
        }

        public String getResourceGroup() {
            return resourceGroup;
        }

        public String getInstrumentKey() {
            return instrumentKey;
        }

        public void setName(final String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return isNewCreated ? String.format(CommonConst.NEW_CREATED_RESOURCE, name) :
                   String.format(CommonConst.RESOURCE_WITH_RESOURCE_GROUP, name, resourceGroup);
        }
    }
}
