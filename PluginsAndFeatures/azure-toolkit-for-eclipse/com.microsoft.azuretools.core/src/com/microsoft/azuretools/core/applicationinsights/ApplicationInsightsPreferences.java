/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.applicationinsights;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.List;

import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.microsoft.applicationinsights.preference.ApplicationInsightsResource;
import com.microsoft.applicationinsights.preference.ApplicationInsightsResourceRegistry;
import com.microsoft.azuretools.core.Activator;
import com.microsoft.azuretools.core.utils.PluginUtil;


/**
 * Preference utility class to save and load
 * preferences of application insights resource registry.
 */
public class ApplicationInsightsPreferences {
    private static final String PREF_KEY = "applicationinsights" + ".resources";
    private static final String PREF_FILE = "com.microsoft.applicationinsights.ui";
    private static final ApplicationInsightsPreferences INSTANCE = new ApplicationInsightsPreferences();
    private static boolean loaded;

    public synchronized static void save() {
        INSTANCE.savePreferences();
    }

    /**
     * Stores application insights resources list
     * in preference file in the form of byte array.
     */
    private void savePreferences() {
        try {
            Preferences prefs = PluginUtil.getPrefs(PREF_FILE);
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            ObjectOutput output = new ObjectOutputStream(buffer);
            List<ApplicationInsightsResource> data = ApplicationInsightsResourceRegistry.getAppInsightsResrcList();
            ApplicationInsightsResource[] dataArray = data.stream().filter(a -> !a.isImported()).sorted().toArray(ApplicationInsightsResource[]::new);
            /*
             * Sort list according to application insights resource name.
             * Save only manually added resources
             */
            try {
                output.writeObject(dataArray);
            } finally {
                output.close();
            }
            prefs.putByteArray(PREF_KEY, buffer.toByteArray());
            prefs.flush();
        } catch (BackingStoreException e) {
            Activator.getDefault().log(e.getMessage(), e);
        } catch (IOException e) {
            Activator.getDefault().log(e.getMessage(), e);
        }
    }

    public static void load(){
        INSTANCE.loadPreferences();
    }

    /**
     * Read and load preference file data.
     * Converts byte array format data to list of application insights resources.
     */
    private void loadPreferences() {
        Preferences prefs = PluginUtil.getPrefs(PREF_FILE);
        try {
            byte[] data = prefs.getByteArray(PREF_KEY, null);
            if (data != null) {
                ByteArrayInputStream buffer = new ByteArrayInputStream(data);
                ObjectInput input = new ObjectInputStream(buffer);
                try {
                    ApplicationInsightsResource[] resources = (ApplicationInsightsResource[]) input.readObject();
                    for (ApplicationInsightsResource resource : resources) {
                        if (!ApplicationInsightsResourceRegistry.getAppInsightsResrcList().contains(resource)) {
                            ApplicationInsightsResourceRegistry.getAppInsightsResrcList().add(resource);
                        }
                    }
                } finally {
                    input.close();
                }
            }
        } catch (IOException e) {
            Activator.getDefault().log(e.getMessage(), e);
        } catch (ClassNotFoundException e) {
            Activator.getDefault().log(e.getMessage(), e);
        }
    }

    public static boolean isLoaded() {
        return loaded;
    }

    public static void setLoaded(boolean bool) {
        loaded = bool;
    }
}
