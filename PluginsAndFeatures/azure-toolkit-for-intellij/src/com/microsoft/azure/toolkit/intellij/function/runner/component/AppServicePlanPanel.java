/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.function.runner.component;

import com.intellij.ui.PopupMenuListenerAdapter;
import com.intellij.ui.SimpleListCellRenderer;
import com.microsoft.azure.management.appservice.AppServicePlan;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.PricingTier;
import com.microsoft.azure.management.resources.fluentcore.arm.Region;
import com.microsoft.azure.toolkit.lib.common.operation.AzureOperation;
import com.microsoft.azure.toolkit.lib.common.task.AzureTaskManager;
import com.microsoft.azuretools.core.mvp.model.function.AzureFunctionMvpModel;
import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import java.awt.*;
import java.awt.event.ItemListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static com.microsoft.intellij.CommonConst.NEW_CREATED_RESOURCE;
import static com.microsoft.azure.toolkit.intellij.function.runner.component.NewAppServicePlanDialog.CONSUMPTION;
import static com.microsoft.azure.toolkit.intellij.function.runner.component.NewAppServicePlanDialog.CONSUMPTION_PRICING_TIER;

public class AppServicePlanPanel extends JPanel {
    private static final String CREATE_APP_SERVICE_PLAN = "Create app service plan...";
    private JComboBox cbAppServicePlan;
    private JLabel lblLocation;
    private JLabel lblPricingTier;
    private JPanel pnlRoot;

    private Window window;
    private Disposable rxDisposable;

    private String subscriptionId;
    private OperatingSystem operatingSystem;
    private AppServicePlanWrapper selectedAppServicePlan = null;
    private List<AppServicePlanWrapper> appServicePlanWrapperList = new ArrayList<>();

    public AppServicePlanPanel() {
        cbAppServicePlan.setRenderer(new SimpleListCellRenderer() {
            @Override
            public void customize(JList list, Object object, int i, boolean b, boolean b1) {
                if (object instanceof AppServicePlanWrapper || object instanceof String) {
                    setText(object.toString());
                }
            }
        });

        cbAppServicePlan.addPopupMenuListener(new PopupMenuListenerAdapter() {
            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                onSelectAppServicePlan();
            }
        });

        // Set label for screen reader for accessibility issue
        final JLabel label = new JLabel("App Service Plan");
        label.setLabelFor(cbAppServicePlan);
    }

    public AppServicePlanPanel(Window window) {
        this();
        this.window = window;
    }

    public void setOSType(OperatingSystem os) {
        this.operatingSystem = os;
        reloadAppServicePlan();
    }

    public boolean isNewAppServicePlan() {
        return selectedAppServicePlan == null ? false : selectedAppServicePlan.isNewCreate();
    }

    public String getAppServicePlanName() {
        return selectedAppServicePlan == null ? null : selectedAppServicePlan.getName();
    }

    public String getAppServicePlanRegion() {
        return selectedAppServicePlan == null ? null : selectedAppServicePlan.getRegion().name();
    }

    public String getAppServicePlanPricingTier() {
        return selectedAppServicePlan == null ? null : selectedAppServicePlan.getPricingTier().toSkuDescription().size();
    }

    public String getAppServicePlanResourceGroup() {
        return selectedAppServicePlan == null ? null : selectedAppServicePlan.resourceGroup;
    }

    @AzureOperation(
        name = "appservice|plan.list.subscription",
        params = {"subscriptionId"},
        type = AzureOperation.Type.SERVICE
    )
    public void loadAppServicePlan(String subscriptionId, OperatingSystem operatingSystem) {
        if (!StringUtils.equalsIgnoreCase(subscriptionId, this.subscriptionId)) {
            this.subscriptionId = subscriptionId;
            this.operatingSystem = operatingSystem;
            beforeLoadAppServicePlan();
            if (rxDisposable != null && !rxDisposable.isDisposed()) {
                rxDisposable.dispose();
            }
            rxDisposable = Observable
                .fromCallable(() -> AzureFunctionMvpModel
                    .getInstance()
                    .listAppServicePlanBySubscriptionId(subscriptionId).stream()
                    .sorted((first, second) -> StringUtils.compare(first.name(), second.name()))
                    .collect(Collectors.toList()))
                .subscribeOn(Schedulers.io())
                .doOnError((e) -> fillAppServicePlan(Collections.emptyList()))
                .subscribe(this::fillAppServicePlan);
        }
    }

    public void addItemListener(ItemListener actionListener) {
        cbAppServicePlan.addItemListener(actionListener);
    }

    public JComponent getComboComponent() {
        return cbAppServicePlan;
    }

    private void onSelectAppServicePlan() {
        final Object selectedObject = cbAppServicePlan.getSelectedItem();
        if (selectedObject instanceof AppServicePlanWrapper) {
            selectedAppServicePlan = (AppServicePlanWrapper) selectedObject;
            showAppServicePlan(selectedAppServicePlan);
        } else if (CREATE_APP_SERVICE_PLAN.equals(selectedObject)) {
            AzureTaskManager.getInstance().runLater(this::createAppServicePlan);
        } else {
            selectedAppServicePlan = null;
            showAppServicePlan(null);
        }
    }

    private void createAppServicePlan() {
        cbAppServicePlan.setSelectedItem(null);
        cbAppServicePlan.setPopupVisible(false);
        final NewAppServicePlanDialog dialog = new NewAppServicePlanDialog(subscriptionId);
        if (dialog.showAndGet()) {
            final AppServicePlanWrapper newCreateAppServicePlan = dialog.getAppServicePlan();
            if (newCreateAppServicePlan != null) {
                selectedAppServicePlan = newCreateAppServicePlan;
                appServicePlanWrapperList.removeIf(appServicePlanWrapper -> StringUtils.equals(appServicePlanWrapper.name, newCreateAppServicePlan.name));
                appServicePlanWrapperList.add(selectedAppServicePlan);
            }
        }
        reloadAppServicePlan();
    }

    private void showAppServicePlan(AppServicePlanWrapper appServicePlanWrapper) {
        if (appServicePlanWrapper == null) {
            lblLocation.setText("N/A");
            lblPricingTier.setText("N/A");
        } else {
            lblLocation.setText(appServicePlanWrapper.getRegion().name());
            final PricingTier pricingTier = appServicePlanWrapper.getPricingTier();
            lblPricingTier.setText(pricingTier == CONSUMPTION_PRICING_TIER ? CONSUMPTION : pricingTier.toString());
        }
    }

    private void beforeLoadAppServicePlan() {
        showAppServicePlan(null);
        cbAppServicePlan.removeAllItems();
        cbAppServicePlan.setEnabled(false);
        cbAppServicePlan.addItem("Loading...");
    }

    private void fillAppServicePlan(List<AppServicePlan> appServicePlans) {
        appServicePlanWrapperList = appServicePlans.stream()
                .map(appServicePlan -> new AppServicePlanWrapper(appServicePlan))
                .collect(Collectors.toCollection(ArrayList::new));
        reloadAppServicePlan();
    }

    private void reloadAppServicePlan() {
        cbAppServicePlan.removeAllItems();
        cbAppServicePlan.setEnabled(true);
        cbAppServicePlan.addItem(CREATE_APP_SERVICE_PLAN);

        final List<AppServicePlanWrapper> items = appServicePlanWrapperList.stream()
                .filter(appServicePlanWrapper -> appServicePlanWrapper.isNewCreate() || operatingSystem == appServicePlanWrapper.getOperatingSystem())
                .collect(Collectors.toList());
        items.forEach(appServicePlanWrapper -> cbAppServicePlan.addItem(appServicePlanWrapper));
        final AppServicePlanWrapper selectedItem = items.size() == 0 ? null :
                (selectedAppServicePlan == null || !items.contains(selectedAppServicePlan)) ? items.get(0) : selectedAppServicePlan;
        cbAppServicePlan.setSelectedItem(selectedItem);
        onSelectAppServicePlan();

        if (window != null) {
            window.pack();
        }
    }

    static class AppServicePlanWrapper {

        private boolean isNewCreate;
        private String name;
        private String resourceGroup;
        private Region region;
        private PricingTier pricingTier;
        private OperatingSystem operatingSystem;

        public AppServicePlanWrapper(AppServicePlan appServicePlan) {
            this.isNewCreate = false;
            this.name = appServicePlan.name();
            this.region = appServicePlan.region();
            this.pricingTier = appServicePlan.pricingTier();
            this.operatingSystem = appServicePlan.operatingSystem();
            this.resourceGroup = appServicePlan.resourceGroupName();
        }

        public AppServicePlanWrapper(String name, Region region, PricingTier pricingTier) {
            this.isNewCreate = true;
            this.name = name;
            this.region = region;
            this.pricingTier = pricingTier;
        }

        public boolean isNewCreate() {
            return isNewCreate;
        }

        public String getName() {
            return name;
        }

        public Region getRegion() {
            return region;
        }

        public PricingTier getPricingTier() {
            return pricingTier;
        }

        public OperatingSystem getOperatingSystem() {
            return operatingSystem;
        }

        public String getResourceGroup() {
            return resourceGroup;
        }

        @Override
        public String toString() {
            return isNewCreate ? String.format(NEW_CREATED_RESOURCE, name) : String.format("%s (Resource Group: %s)", name, resourceGroup);
        }
    }
}
