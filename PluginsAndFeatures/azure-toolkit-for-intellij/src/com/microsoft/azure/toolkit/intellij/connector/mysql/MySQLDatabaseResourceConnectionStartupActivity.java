package com.microsoft.azure.toolkit.intellij.connector.mysql;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.microsoft.azure.toolkit.intellij.connector.Connection;
import com.microsoft.azure.toolkit.intellij.connector.ConnectionManager;
import com.microsoft.azure.toolkit.intellij.connector.ModuleResource;
import com.microsoft.azure.toolkit.intellij.connector.ResourceManager;
import org.jetbrains.annotations.NotNull;

public class MySQLDatabaseResourceConnectionStartupActivity implements StartupActivity {
    @Override
    public void runActivity(@NotNull Project project) {
        final String type = Connection.typeOf(MySQLDatabaseResource.Definition.AZURE_MYSQL.getType(), ModuleResource.Definition.IJ_MODULE.getType());
        ResourceManager.registerDefinition(MySQLDatabaseResource.Definition.AZURE_MYSQL);
        ConnectionManager.registerDefinition(type, MySQLDatabaseResourceConnection.Definition.MODULE_MYSQL);
    }
}
