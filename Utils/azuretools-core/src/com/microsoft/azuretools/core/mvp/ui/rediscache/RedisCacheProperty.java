/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.core.mvp.ui.rediscache;

import com.microsoft.azuretools.core.mvp.ui.base.ResourceProperty;

public class RedisCacheProperty extends ResourceProperty {

    private String version;

    private int sslPort;

    private boolean nonSslPort;

    private String primaryKey;

    private String secondaryKey;

    private String hostName;

    /**
     * Constructor of Redis Cache Property.
     *
     * @param name
     *            Redis Cache's name
     * @param type
     *            Redis Cache's resource type
     * @param groupName
     *            Redis Cache's group name
     * @param regionName
     *            Redis Cache's location name
     * @param subscriptionId
     *            Redis Cache's subscription id
     * @param version
     *            Redis Cache's version
     * @param sslPort
     *            Redis Cache's SSL port number
     * @param nonSslPort
     *            Flag to show whether the non-SSL port is enabled
     * @param primaryKey
     *            Redis Cache's primary key
     * @param secondaryKey
     *            Redis Cache's secondary key
     * @param hostName
     *            Redis Cache's host name
     */
    public RedisCacheProperty(String name, String type, String groupName, String regionName, String subscriptionId,
            String version, int sslPort, boolean nonSslPort, String primaryKey, String secondaryKey, String hostName) {
        super(name, type, groupName, regionName, subscriptionId);
        this.version = version;
        this.sslPort = sslPort;
        this.nonSslPort = nonSslPort;
        this.primaryKey = primaryKey;
        this.secondaryKey = secondaryKey;
        this.hostName = hostName;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public int getSslPort() {
        return sslPort;
    }

    public void setSslPort(int sslPort) {
        this.sslPort = sslPort;
    }

    public boolean isNonSslPort() {
        return nonSslPort;
    }

    public void setNonSslPort(boolean nonSslPort) {
        this.nonSslPort = nonSslPort;
    }

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }

    public String getSecondaryKey() {
        return secondaryKey;
    }

    public void setSecondaryKey(String secondaryKey) {
        this.secondaryKey = secondaryKey;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

}
