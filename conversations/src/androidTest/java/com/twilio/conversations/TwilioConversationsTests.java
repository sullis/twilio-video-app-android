package com.twilio.conversations;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.filters.LargeTest;
import android.support.test.runner.AndroidJUnit4;

import com.twilio.common.TwilioAccessManager;
import com.twilio.common.TwilioAccessManagerFactory;
import com.twilio.conversations.helper.AccessTokenHelper;
import com.twilio.conversations.helper.TwilioConversationsHelper;
import com.twilio.conversations.helper.TwilioConversationsTestsBase;
import com.twilio.conversations.internal.ClientOptionsInternal;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static junit.framework.Assert.fail;
import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class TwilioConversationsTests extends TwilioConversationsTestsBase {
    private TwilioAccessManager accessManager;
    private Context context;

    @Before
    public void setup() {
        context = InstrumentationRegistry.getContext();
    }

    @After
    public void teardown() {
        TwilioConversationsHelper.destroy();
        if(accessManager != null) {
            accessManager.dispose();
        }
    }

    @Test(expected = NullPointerException.class)
    public void initialize_shouldNotAllowNullContext() {
        TwilioConversationsClient.initialize(null,
                TwilioConversationsHelper.createInitListener());
    }

    @Test(expected = NullPointerException.class)
    public void initialize_shouldNotAllowNullInitListener() {
        TwilioConversationsClient.initialize(context, null);
    }

    @Test
    public void initialize_shouldSucceedWithValidContextAndListener() throws InterruptedException {
        TwilioConversationsHelper.initialize(context);

        assertTrue(TwilioConversationsClient.isInitialized());
    }

    @Test
    public void initialize_shouldNotifyWithErrorWhenAlreadyInitialized()
            throws InterruptedException {
        final CountDownLatch errorCallback = new CountDownLatch(1);
        TwilioConversationsHelper.initialize(context);

        TwilioConversationsClient.initialize(context, new TwilioConversationsClient.InitListener() {
            @Override
            public void onInitialized() {
                fail("Should receive error because sdk is initialized already!");
            }

            @Override
            public void onError(Exception exception) {
                errorCallback.countDown();
            }
        });
        assertTrue(errorCallback.await(10, TimeUnit.SECONDS));
    }

    @Test
    public void destroy_shouldSucceedAfterInitilization() throws InterruptedException {
        TwilioConversationsHelper.initialize(context);
        TwilioConversationsHelper.destroy();
    }

    @Test
    public void initialize_shouldWorkRepeatedlyAfterDestroy() throws InterruptedException {
        for (int i = 0 ; i < 10 ; i++) {
            TwilioConversationsHelper.initialize(context);
            assertTrue(TwilioConversationsClient.isInitialized());
            TwilioConversationsHelper.destroy();
            assertFalse(TwilioConversationsClient.isInitialized());
        }
    }

    /**
     * TODO
     * Add way to validate this test using reflection on conversations client
     * @throws InterruptedException
     */
    @Test
    @Ignore
    public void destroy_shouldDestroyActiveConversationClients() throws InterruptedException {
        TwilioConversationsHelper.initialize(context);
        accessManager = AccessTokenHelper.obtainTwilioAccessManager(context, "username");
        TwilioConversationsClient conversationClient = (TwilioConversationsClient)TwilioConversationsClient
                .create(accessManager,
                        conversationsClientListener());
    }

    @Test(expected =  IllegalStateException.class)
    public void createConversationsClient_shouldBeAllowedBeforeInitialize()
            throws InterruptedException {
        accessManager = AccessTokenHelper.obtainTwilioAccessManager(context, "username");
        TwilioConversationsClient.create(accessManager,
                conversationsClientListener());
    }

    @Test(expected = NullPointerException.class)
    public void createConversationsClient_shouldNowAllowNullAcessToken()
            throws InterruptedException {
        TwilioConversationsHelper.initialize(context);

        TwilioConversationsClient.create(accessManager, conversationsClientListener());
    }

    @Test(expected = NullPointerException.class)
    public void createConversationsClient_shouldNotAllowNullAccessManager()
            throws InterruptedException {
        TwilioConversationsHelper.initialize(context);

        TwilioConversationsClient.create(null,
                new ClientOptions(),
                conversationsClientListener());
    }

    @Test
    public void createConversationsClient_shouldReturnClientForValidToken()
            throws InterruptedException {
        TwilioConversationsHelper.initialize(context);

        accessManager = TwilioAccessManagerFactory.createAccessManager(context, "DEADBEEF", null);
        TwilioConversationsClient twilioConversationsClient = TwilioConversationsClient
                .create(accessManager, conversationsClientListener());

        assertNotNull(twilioConversationsClient);
    }

    @Test
    public void createConversationsClient_shouldAllowEmptyOptions()
            throws InterruptedException {
        TwilioConversationsHelper.initialize(context);

        accessManager = TwilioAccessManagerFactory.createAccessManager(context, "DEADBEEF", null);
        TwilioConversationsClient twilioConversationsClient = TwilioConversationsClient
                .create(accessManager, null, conversationsClientListener());

        assertNotNull(twilioConversationsClient);
    }

    @Test
    public void createConversationsClient_shouldAllowRandomOptions()
            throws InterruptedException {
        TwilioConversationsHelper.initialize(context);

        HashMap optionsMap = new HashMap<>();
        optionsMap.put("foo", "bar");
        ClientOptionsInternal options = new ClientOptionsInternal(optionsMap);
        accessManager = TwilioAccessManagerFactory.createAccessManager(context, "DEADBEEF", null);
        TwilioConversationsClient twilioConversationsClient = TwilioConversationsClient
                .create(accessManager, options, conversationsClientListener());

        assertNotNull(twilioConversationsClient);
    }

    @Test
    public void setLogLevel_canBeDoneBeforeAndAfterInit() throws InterruptedException {
        LogLevel level = LogLevel.DEBUG;

        TwilioConversationsClient.setLogLevel(level);
        assertEquals(level, TwilioConversationsClient.getLogLevel());

        TwilioConversationsHelper.initialize(context);

        level = LogLevel.ERROR;
        TwilioConversationsClient.setLogLevel(level);
        assertEquals(level, TwilioConversationsClient.getLogLevel());
    }

    @Test
    public void getVersion_shouldReturnValidSemVerFormattedVersion() {
        String semVerRegex = "^([0-9]+)\\.([0-9]+)\\.([0-9]+)(?:-([0-9A-" +
                "Za-z-]+(?:\\.[0-9A-Za-z-]+)*))?(?:\\+[0-9A-Za-z-]+)?$";
        String version = TwilioConversationsClient.getVersion();

        assertNotNull(version);
        assertNotEquals("", version);
        assertTrue(version.matches(semVerRegex));
    }

    private TwilioConversationsClient.Listener conversationsClientListener() {
        return new TwilioConversationsClient.Listener() {
            @Override
            public void onStartListeningForInvites(TwilioConversationsClient twilioConversationsClient) {

            }

            @Override
            public void onStopListeningForInvites(TwilioConversationsClient twilioConversationsClient) {

            }

            @Override
            public void onFailedToStartListening(TwilioConversationsClient twilioConversationsClient,
                                                 TwilioConversationsException e) {

            }

            @Override
            public void onIncomingInvite(TwilioConversationsClient twilioConversationsClient,
                                         IncomingInvite incomingInvite) {

            }

            @Override
            public void onIncomingInviteCancelled(TwilioConversationsClient twilioConversationsClient,
                                                  IncomingInvite incomingInvite) {

            }
        };
    }
}
