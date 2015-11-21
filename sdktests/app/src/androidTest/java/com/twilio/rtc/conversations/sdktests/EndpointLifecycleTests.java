package com.twilio.rtc.conversations.sdktests;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;

import com.twilio.rtc.conversations.sdktests.utils.TwilioRTCUtils;
import com.twilio.signal.Conversation;
import com.twilio.signal.ConversationException;
import com.twilio.signal.ConversationListener;
import com.twilio.signal.Endpoint;
import com.twilio.signal.EndpointListener;
import com.twilio.signal.Invite;
import com.twilio.signal.LocalMedia;
import com.twilio.signal.LocalVideoTrack;
import com.twilio.signal.MediaFactory;
import com.twilio.signal.Participant;
import com.twilio.signal.TwilioRTC;
import com.twilio.signal.VideoTrack;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


@RunWith(AndroidJUnit4.class)
public class EndpointLifecycleTests {

    private static String TOKEN = "token";
    private static String PARTICIPANT = "janne";

    @Rule
    public ActivityTestRule<TwilioRTCActivity> mActivityRule = new ActivityTestRule<>(
            TwilioRTCActivity.class);

    @Test
    public void testTwilioCreateEndpointWithToken() {
        Endpoint endpoint = createEndpoint();
        org.junit.Assert.assertNotNull(endpoint);
    }

    @Test(expected = IllegalStateException.class)
    public void testTwilioCannotListenAfterEndpointDisposal() {
        Endpoint endpoint = createEndpoint();
        org.junit.Assert.assertNotNull(endpoint);

        endpoint.dispose();
        endpoint.listen();
    }

    @Test(expected = IllegalStateException.class)
    public void testTwilioCannotUnlistenAfterEndpointDisposal() {
        Endpoint endpoint = createEndpoint();
        org.junit.Assert.assertNotNull(endpoint);

        endpoint.dispose();
        endpoint.unlisten();
    }

    @Test(expected = IllegalStateException.class)
    public void testTwilioCannotCreateConversationAfterEndpointDisposal() {
        Endpoint endpoint = createEndpoint();
        org.junit.Assert.assertNotNull(endpoint);

        endpoint.dispose();
        Set<String> participants = new HashSet<>();
        participants.add(PARTICIPANT);
        LocalMedia localMedia = MediaFactory.createLocalMedia();
        Conversation conv = endpoint.createConversation(participants, localMedia, conversationListener());
    }

    @Test
    public void testTwilioMultiDisposeEndpoint() {
        for (int i= 1; i < 50; i++) {
            Endpoint endpoint = createEndpoint();
            org.junit.Assert.assertNotNull(endpoint);
            endpoint.dispose();
        }
    }

    private Endpoint createEndpoint() {
        TwilioRTCUtils.initializeTwilioSDK(mActivityRule.getActivity().getApplicationContext());
        Endpoint endpoint = TwilioRTC.createEndpoint(TOKEN, endpointListener());
        return endpoint;
    }

    @Test
    public void testTwilioFailsToListenWithDummyToken() {
        final CountDownLatch wait = new CountDownLatch(1);
        /*
         * The test thread cannot create new handlers. Use the main
         * thread to ensure we can receive callbacks on the Endpoint which
         * uses a handler to callback on the thread that created it.
         */
        InstrumentationRegistry.getInstrumentation().runOnMainSync(new Runnable() {
            @Override
            public void run() {
                Endpoint endpoint = TwilioRTC.createEndpoint(TOKEN, new EndpointListener() {
                    @Override
                    public void onStartListeningForInvites(Endpoint endpoint) {
                        org.junit.Assert.fail();
                    }

                    @Override
                    public void onStopListeningForInvites(Endpoint endpoint) {
                        org.junit.Assert.fail();
                    }

                    @Override
                    public void onFailedToStartListening(Endpoint endpoint, ConversationException e) {
                        wait.countDown();
                    }

                    @Override
                    public void onReceiveConversationInvite(Endpoint endpoint, Invite invite) {
                        org.junit.Assert.fail();
                    }
                });
                endpoint.listen();
            }
        });
        TwilioRTCUtils.wait(wait, 10, TimeUnit.SECONDS);
    }

    private EndpointListener endpointListener() {
        return new EndpointListener() {
            @Override
            public void onStartListeningForInvites(Endpoint endpoint) {

            }

            @Override
            public void onStopListeningForInvites(Endpoint endpoint) {

            }

            @Override
            public void onFailedToStartListening(Endpoint endpoint, ConversationException e) {

            }

            @Override
            public void onReceiveConversationInvite(Endpoint endpoint, Invite invite) {

            }
        };
    }

    private ConversationListener conversationListener() {
        return new ConversationListener() {
            @Override
            public void onConnectParticipant(Conversation conversation, Participant participant) {

            }

            @Override
            public void onFailToConnectParticipant(Conversation conversation, Participant participant, ConversationException e) {

            }

            @Override
            public void onDisconnectParticipant(Conversation conversation, Participant participant) {

            }

            @Override
            public void onLocalVideoAdded(Conversation conversation, LocalVideoTrack videoTrack) {
                
            }

            @Override
            public void onLocalVideoRemoved(Conversation conversation, LocalVideoTrack videoTrack) {

            }

            @Override
            public void onVideoAddedForParticipant(Conversation conversation, Participant participant, VideoTrack videoTrack) {

            }

            @Override
            public void onVideoRemovedForParticipant(Conversation conversation, Participant participant, VideoTrack videoTrack) {

            }

            @Override
            public void onLocalStatusChanged(Conversation conversation, Conversation.Status status) {

            }

            @Override
            public void onConversationEnded(Conversation conversation) {

            }

            @Override
            public void onConversationEnded(Conversation conversation, ConversationException e) {

            }
        };
    }

}
