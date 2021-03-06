/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.authmanage.srvpri;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.management.Azure;
import com.microsoft.azuretools.authmanage.CommonSettings;
import com.microsoft.azuretools.authmanage.srvpri.entities.AuthenticationError;
import com.microsoft.azuretools.authmanage.srvpri.report.FileListener;
import com.microsoft.azuretools.authmanage.srvpri.report.IListener;
import com.microsoft.azuretools.authmanage.srvpri.report.Reporter;
import com.microsoft.azuretools.authmanage.srvpri.step.ApplicationStep;
import com.microsoft.azuretools.authmanage.srvpri.step.CommonParams;
import com.microsoft.azuretools.authmanage.srvpri.step.RoleAssignmentStep;
import com.microsoft.azuretools.authmanage.srvpri.step.ServicePrincipalStep;
import com.microsoft.azuretools.authmanage.srvpri.step.Status;
import com.microsoft.azuretools.authmanage.srvpri.step.StepManager;
import com.microsoft.azuretools.sdkmanage.AzureManagerBase;

import lombok.SneakyThrows;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Logger;

import reactor.core.Exceptions;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

/**
 * Created by vlashch on 8/16/16.
 */


public class SrvPriManager {
    // relationship;
    // app - sp (1-1)
    // sp - role (1-many)

    private static final Logger LOGGER = Logger.getLogger(SrvPriManager.class.getName());
    private static final int RETRY_COUNT = 5;

    public static String createSp(AzureManagerBase preAccessTokenAzureManager,
                                  String tenantId,
                                  List<String> subscriptionIds,
                                  String suffix,
                                  IListener<Status> statusListener,
                                  String destinationFolder)
            throws IOException {

        System.out.print(tenantId + ": [");
        for (String sid : subscriptionIds) {
            System.out.print(sid + ", ");
        }
        System.out.println("]");

        Path spDirPath = Paths.get(destinationFolder);
        if (spDirPath == null) {
            String baseDir = System.getProperty("user.home");
            String dirName = "MSAzureAuthFiles";
            spDirPath = Paths.get(baseDir, dirName);
            if (!Files.exists(spDirPath)) {
                Files.createDirectory(spDirPath);
            }
        }

        Reporter<Status> statusReporter = new Reporter<Status>();
        statusReporter.addListener(statusListener);
        CommonParams.setStatusReporter(statusReporter);

        // generate a password
        String password = "AZURE" + UUID.randomUUID().toString();

        CommonParams.setSubscriptionIdList(subscriptionIds);
        CommonParams.setResultSubscriptionIdList(new LinkedList<>());
        CommonParams.setTenantId(tenantId);
        String filename = tenantId + "_" + suffix;
        //String filename = tenantId + "_" + subscriptionIds.get(0) + "_" + suffix;
        //String filename = subscriptionIds.get(0) + "_" + suffix;
        String spFilename = "sp-" + filename + ".azureauth";
        String reportFilename = "report-" + filename + ".txt";

        Reporter<String> fileReporter = new Reporter<String>();
        fileReporter.addListener(new FileListener(reportFilename, spDirPath.toString()));
        fileReporter.addConsoleLister();
        CommonParams.setReporter(fileReporter);

        StepManager sm = new StepManager();
        sm.getParamMap().put("displayName", "AzureTools4j-" + suffix);
        sm.getParamMap().put("homePage", "https://github.com/Microsoft/azure-tools-for-java");
        // MUST be unique
        sm.getParamMap().put("identifierUri", "file://" + spFilename);
        sm.getParamMap().put("password", password);
        sm.getParamMap().put("status", "standby");

        sm.add(new ApplicationStep(preAccessTokenAzureManager));
        sm.add(new ServicePrincipalStep(preAccessTokenAzureManager));
        sm.add(new RoleAssignmentStep(preAccessTokenAzureManager));

        fileReporter.report(String.format("== Starting for tenantId: '%s'", tenantId));

        sm.execute();

        String overallStatusText = "=== Overall status";
        // create a file artifact
        if (sm.getParamMap().get("status").toString().equals("done")) {
            Path filePath = Paths.get(spDirPath.toString(), spFilename);;

            createArtifact(
                    filePath.toString(),
                    (UUID) sm.getParamMap().get("appId"),
                    password
            );

            statusReporter.report(new Status("Waiting for service principal activation to complete...", null, null));
            statusReporter.report(new Status("Checking auth file...", null, null));
            try {
                checkArtifact(fileReporter, filePath).block();
            } catch (Throwable ex) {
                throw new IllegalStateException("Failed to check cred file due to error:" + ex.getMessage());
            }

            String successSidsResult = String.format("Succeeded for %d of %d subscriptions. ",
                    CommonParams.getResultSubscriptionIdList().size(),
                    CommonParams.getSubscriptionIdList().size());

            statusReporter.report(new Status(
                    overallStatusText,
                    Status.Result.SUCCESSFUL,
                    successSidsResult
            ));

            fileReporter.report(String.format("Authentication file created, path: %s", filePath.toString()));
            return filePath.toString();

        } else {
            statusReporter.report(new Status(
                    overallStatusText,
                    Status.Result.FAILED,
                    "Can't create a service principal."
            ));
        }

        return null;
    }

    private static void createArtifact(String filepath, UUID appId, String appPassword) throws IOException {

        Properties prop = new Properties();
        Writer writer = null;

        try {
            // to ignore date comment
            String lineSeparator = System.getProperty("line.separator");
            writer = new BufferedWriter(new FileWriter(filepath)) {
                private boolean skipLineSeparator = false;
                @Override
                public void write(String str) throws IOException {
                    // avoid to print key in SP
                    // System.out.println(str);
                    if (str.startsWith("#")) {
                        skipLineSeparator = true;
                        return;
                    }
                    if (str.startsWith(lineSeparator) && skipLineSeparator) {
                        skipLineSeparator = false;
                        return;
                    }
                    super.write(str);
                }
            };

            // set the properties value
            prop.setProperty("tenant", CommonParams.getTenantId());
            int i = 0;
            for (String subscriptionId : CommonParams.getResultSubscriptionIdList()) {
                if (i == 0) {
                    prop.setProperty("subscription", subscriptionId);
                } else {
                    prop.setProperty("subscription" + i, subscriptionId);
                }
                i++;
            }
            prop.setProperty("client", appId.toString());
            prop.setProperty("key", appPassword);

            AzureEnvironment azureEnv = CommonSettings.getAdEnvironment();
            prop.setProperty("managementURI", azureEnv.managementEndpoint());
            prop.setProperty("baseURL", azureEnv.resourceManagerEndpoint());
            prop.setProperty("authURL", azureEnv.activeDirectoryEndpoint());
            prop.setProperty("graphURL", azureEnv.graphEndpoint());

            prop.store(writer, null);
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
    }

    private static Mono<Boolean> checkArtifact(Reporter<String> fileReporter, Path filePath) {
        // here we try to use the file to check it's ok with retry logic
        fileReporter.report("Checking cred file...");

        return retryOnError(RETRY_COUNT, Mono.fromCallable(() -> {
            File authFile = new File(filePath.toString());
            fileReporter.report("Checking: Azure.authenticate(authFile)...");
            Azure.Authenticated azureAuthenticated = Azure.authenticate(authFile);
            fileReporter.report("Checking: azureAuthenticated.subscriptions().list()...");
            azureAuthenticated.subscriptions().list();
            fileReporter.report("Checking: azureAuthenticated.withDefaultSubscription()...");
            Azure azure = azureAuthenticated.withDefaultSubscription();
            fileReporter.report("Checking: resourceGroups().list()...");
            azure.resourceGroups().list();
            fileReporter.report("Done.");
            return true;
        }), SrvPriManager::needToRetry);

    }

    // TODO: move this to common-lib
    private static <T> Mono<T> retryOnError(int retryCount, Mono<T> observable, Function<Throwable, Boolean> shouldRetryOnError) {
        return observable
                .retryWhen(Retry.withThrowable(flux -> flux.zipWith(Flux.range(1, retryCount + 1), (throwable, count) -> {
                    if (count < retryCount && (shouldRetryOnError.apply(throwable))) {
                        return count;
                    } else {
                        throw Exceptions.propagate(throwable);
                    }
                }).flatMap(i -> Mono.delay(Duration.ofSeconds(((long) i * 10))))));
    }

    @SneakyThrows
    private static boolean needToRetry(Throwable e) {
        final String ERROR_LABEL = "\"error\":";
        final String ERROR_TEXT = "unauthorized_client";
        if (e instanceof com.microsoft.aad.adal4j.AuthenticationException) {
            LOGGER.info("=== needToRetry@SrvPriManager: AuthenticationException info: " + e.getMessage());
            ObjectMapper om = new ObjectMapper();
            AuthenticationError ae = om.readValue(e.getMessage(), AuthenticationError.class);
            return ae.error.equals(ERROR_TEXT);
        } else {
            LOGGER.info("=== needToRetry@SrvPriManager: Exception info: " + e.getMessage());
            // if we can't catch the exception by type - the one we are catching may be on a deep level of cause.
            // we are looking for an error text;
            String mes = e.getMessage();
            int i1 = mes.indexOf(ERROR_LABEL);
            if (i1 >= 0) {
                String error = mes.substring(i1 + ERROR_LABEL.length());
                return error.contains(ERROR_TEXT);
            }
        }

        return false;
    }
}
