/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.hdinsight.sdk.rest.azure.datalake.analytics.job.models;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The Data Lake Analytics job resources.
 */
public class JobResource {
    /**
     * The name of the resource.
     */
    @JsonProperty(value = "name")
    private String name;

    /**
     * The path to the resource.
     */
    @JsonProperty(value = "resourcePath")
    private String resourcePath;

    /**
     * The job resource type. Possible values include: 'VertexResource', 'JobManagerResource', 'StatisticsResource',
     * 'VertexResourceInUserFolder', 'JobManagerResourceInUserFolder', 'StatisticsResourceInUserFolder'.
     */
    @JsonProperty(value = "type")
    private JobResourceType type;

    /**
     * Get the name of the resource.
     *
     * @return the name value
     */
    public String name() {
        return this.name;
    }

    /**
     * Set the name of the resource.
     *
     * @param name the name value to set
     * @return the JobResource object itself.
     */
    public JobResource withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * Get the path to the resource.
     *
     * @return the resourcePath value
     */
    public String resourcePath() {
        return this.resourcePath;
    }

    /**
     * Set the path to the resource.
     *
     * @param resourcePath the resourcePath value to set
     * @return the JobResource object itself.
     */
    public JobResource withResourcePath(String resourcePath) {
        this.resourcePath = resourcePath;
        return this;
    }

    /**
     * Get the job resource type. Possible values include: 'VertexResource', 'JobManagerResource', 'StatisticsResource', 'VertexResourceInUserFolder', 'JobManagerResourceInUserFolder', 'StatisticsResourceInUserFolder'.
     *
     * @return the type value
     */
    public JobResourceType type() {
        return this.type;
    }

    /**
     * Set the job resource type. Possible values include: 'VertexResource', 'JobManagerResource', 'StatisticsResource', 'VertexResourceInUserFolder', 'JobManagerResourceInUserFolder', 'StatisticsResourceInUserFolder'.
     *
     * @param type the type value to set
     * @return the JobResource object itself.
     */
    public JobResource withType(JobResourceType type) {
        this.type = type;
        return this;
    }

}
