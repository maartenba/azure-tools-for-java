/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azuretools.adauth;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(CacheDriver.class)
public class CacheDriverTest {
    private final String clientId = "123";
    private final String userId = "a@m.com";

    @Before
    public void setUp() throws Exception {
        AdTokenCache.getInstance().clear();
        IdToken idToken = new IdToken();
        idToken.upn = userId;
        idToken.subject = "subject";
        UserInfo info = UserInfo.createFromIdTokens(idToken);
        AuthResult authResult1 = new AuthResult("type", "atokenmanage", "rtokenmanage", 300000000, info, "manage");
        AdTokenCacheEntry entry = new AdTokenCacheEntry(authResult1, "a/common", clientId);
        AdTokenCache.getInstance().add(entry);
        CacheDriver driver = createDriver("a/common", "rtokenmanage2", 100);
        AuthResult authResult2 = null;
        try {
            authResult2 = driver.find("manage/", userId);
        } catch (Exception e) {}
         entry = new AdTokenCacheEntry(authResult2, "a/common", clientId);
        AdTokenCache.getInstance().add(entry);
    }

    @Test
    public void testFindCommon() {
        CacheDriver driver = createDriver("a/common", "rtokencommonnew", 500000000);
        try {
            AuthResult result = driver.find("manage/", userId);
            Assert.assertEquals("rtokencommonnew", result.getRefreshToken());
            Assert.assertEquals("manage/", result.getResource());
            Assert.assertEquals(userId, result.getUserId());

            AuthResult result2 = driver.find("manage/", userId);
            Assert.assertEquals(result, result2);
        } catch(Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testFindCommonRefresh() {
        CacheDriver driver = createDriver("a/common", "rtokencommonnew", 500000000);
        try {
            AuthResult result = driver.find("manage/", null);
            Assert.assertEquals("rtokencommonnew", result.getRefreshToken());
            Assert.assertEquals("manage/", result.getResource());
            Assert.assertEquals("a@m.com", result.getUserId());
            Assert.assertEquals(false, result.isMultipleResourceRefreshToken());
        } catch(Exception e) {
            Assert.fail();
        }
    }

    @Test
    public void testFindSpecificTenant() {
        String fixToken = "rtokentent1new";
        CacheDriver driver = createDriver("a/tent1", fixToken, 500000000);

       try {
           AuthResult result = driver.find("manage", userId);
           Assert.assertEquals(true, result.isMultipleResourceRefreshToken());
           Assert.assertEquals("manage", result.getResource());
           Assert.assertEquals(fixToken, result.getRefreshToken());

           result = driver.find("resource", userId);
           Assert.assertEquals(true, result.isMultipleResourceRefreshToken());
           Assert.assertEquals("resource", result.getResource());
           Assert.assertEquals(fixToken, result.getRefreshToken());

           result = driver.find("graph", userId);
           Assert.assertEquals(true, result.isMultipleResourceRefreshToken());
           Assert.assertEquals("graph", result.getResource());
           Assert.assertEquals(fixToken, result.getRefreshToken());

           AuthResult result1 = driver.find("graph", userId);
           Assert.assertEquals(result, result1);
       } catch (AuthException e) {
           Assert.fail();
       }
    }

    @Test
    public void testCreateAddEntry() {
        AuthResult authResult1 = new AuthResult("type", "atokenmanage", "rtokenmanage", 300000000, null, "test");
        AdTokenCacheEntry entry = new AdTokenCacheEntry(authResult1, "a/addentry", clientId);
        CacheDriver driver = createDriver("a/common", "testToken", 500);
        driver.createAddEntry(authResult1, null);
        try {
            AuthResult result = driver.find("test", null);
            Assert.assertEquals("rtokenmanage", authResult1.getRefreshToken());
            Assert.assertEquals("test", authResult1.getResource());
        } catch (Exception e) {
            Assert.fail();
        }

        authResult1 = new AuthResult("type", "atokentest", "rtokentest", 300000000, null, "");
        driver.createAddEntry(authResult1, "test2");
        try {
            AuthResult result = driver.find("test2", null);
            Assert.assertEquals("test2", result.getResource());
            Assert.assertEquals("rtokentest", result.getRefreshToken());
        } catch (Exception e) {
            Assert.fail();
        }
    }

    private CacheDriver createDriver(String authority, String fixToken, long expireIn) {
        CacheDriver driver = new CacheDriver(authority, clientId) {
            @Override
            protected AuthResult getTokenWithRefreshToken(String refreshToken, String resource) throws AuthException {
                return new AuthResult("type", "atokennew", fixToken, expireIn, null, resource);
            }
        };

        return driver;
    }
}
