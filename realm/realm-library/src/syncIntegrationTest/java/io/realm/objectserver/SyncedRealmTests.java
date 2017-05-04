/*
 * Copyright 2017 Realm Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.realm.objectserver;

import android.support.annotation.NonNull;
import android.support.test.annotation.UiThreadTest;
import android.support.test.rule.UiThreadTestRule;

import org.junit.Rule;
import org.junit.Test;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import io.realm.Realm;
import io.realm.SyncConfiguration;
import io.realm.SyncCredentials;
import io.realm.SyncUser;
import io.realm.TestHelper;
import io.realm.objectserver.utils.Constants;
import io.realm.rule.RunInLooperThread;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;


/**
 * Catch all class for tests that not naturally fit anywhere else.
 */
public class SyncedRealmTests extends BaseIntegrationTest {

    @Rule
    public RunInLooperThread looperThread = new RunInLooperThread();

    @Rule
    public final UiThreadTestRule uiThreadTestRule = new UiThreadTestRule();

    @Test
    @UiThreadTest
    public void waitForServerChanges_mainThreadThrows() {
        final SyncUser user = loginUser();

        SyncConfiguration config = new SyncConfiguration.Builder(user, Constants.USER_REALM)
                .waitForInitialRemoteData()
                .build();

        Realm realm = null;
        try {
            realm = Realm.getInstance(config);
            fail();
        } catch (IllegalStateException ignored) {
        } finally {
            if (realm != null) {
                realm.close();
            }
        }
    }

    // Login user on a worker thread, so this method can be used from both UI and non-ui threads.
    @NonNull
    private SyncUser loginUser() {
        final CountDownLatch userReady = new CountDownLatch(1);
        final AtomicReference<SyncUser> user = new AtomicReference<>();
        new Thread(new Runnable() {
            @Override
            public void run() {
                SyncCredentials credentials = SyncCredentials.usernamePassword(UUID.randomUUID().toString(), "password", true);
                user.set(SyncUser.login(credentials, Constants.AUTH_URL));
                userReady.countDown();
            }
        }).start();
        TestHelper.awaitOrFail(userReady);
        return user.get();
    }

    @Test
    public void waitForServerChanges() {
        // TODO We can improve this test once we got Sync Progress Notifications. Right now we cannot detect
        // when a Realm has been uploaded.
        SyncCredentials credentials = SyncCredentials.usernamePassword(UUID.randomUUID().toString(), "password", true);
        SyncUser user = SyncUser.login(credentials, Constants.AUTH_URL);
        SyncConfiguration config = new SyncConfiguration.Builder(user, Constants.USER_REALM)
                .waitForInitialRemoteData()
                .build();

        Realm realm = null;
        try {
            realm = Realm.getInstance(config);
            assertTrue(realm.isEmpty());
        } finally {
            if (realm != null) {
                realm.close();
            }
        }
    }
}
