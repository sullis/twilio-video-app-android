package com.twilio.video;

import android.os.Handler;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.annotation.RetentionPolicy.SOURCE;

/**
 * A Room represents a media session with zero or more remote Participants. Media shared by any one
 * {@link Participant} is distributed equally to all other Participants.
 */
public class Room {
    private static final Logger logger = Logger.getLogger(Room.class);

    @Retention(SOURCE)
    @IntDef({ERROR_INVALID_ACCESS_TOKEN,
            ERROR_ROOM_SIGNALING,
            ERROR_CREATE_PEERCONNECITON_FAILURE,
            ERROR_ICE_CONNECTION_FAILURE,
            ERROR_CREATE_LOCAL_SDP_FAILURE,
            ERROR_SET_LOCAL_SDP_FAILURE,
            ERROR_EMPTY_LOCAL_SDP,
            ERROR_PROCESS_REMOTE_SDP_FAILURE,
            ERROR_SET_REMOTE_SDP_FAILURE})
    public @interface Error {}
    public static final int ERROR_INVALID_ACCESS_TOKEN = 20101;
    public static final int ERROR_ROOM_SIGNALING = 53100;
    public static final int ERROR_CREATE_PEERCONNECITON_FAILURE = 54001;
    public static final int ERROR_ICE_CONNECTION_FAILURE = 54101;
    public static final int ERROR_CREATE_LOCAL_SDP_FAILURE = 54102;
    public static final int ERROR_SET_LOCAL_SDP_FAILURE = 54103;
    public static final int ERROR_EMPTY_LOCAL_SDP = 54104;
    public static final int ERROR_PROCESS_REMOTE_SDP_FAILURE = 54105;
    public static final int ERROR_SET_REMOTE_SDP_FAILURE = 54106;

    private long nativeRoomContext;
    private String name;
    private final LocalMedia localMedia;
    private String sid;
    private RoomState roomState;
    private Map<String, Participant> participantMap = new HashMap<>();
    private InternalRoomListenerHandle internalRoomListenerHandle;
    private InternalRoomListenerImpl internalRoomListenerImpl;
    private LocalParticipant localParticipant;
    private final Room.Listener listener;
    private final Handler handler;

    Room(String name, LocalMedia localMedia, Room.Listener listener, Handler handler) {
        this.name = name;
        this.localMedia = localMedia;
        this.sid = "";
        this.roomState = RoomState.DISCONNECTED;
        this.listener = listener;
        this.internalRoomListenerImpl = new InternalRoomListenerImpl();
        this.internalRoomListenerHandle = new InternalRoomListenerHandle(internalRoomListenerImpl);
        this.handler = handler;
    }

    /**
     * Returns the name of the current room. This method will return the SID if the room was
     * created without a name.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the SID of the current room.
     */
    public String getSid() {
        return sid;
    }

    /**
     * Returns the current room state.
     */
    public RoomState getState() {
        return roomState;
    }

    /**
     * Returns all currently connected participants.
     *
     * @return a map of identities to participants.
     */
    public Map<String, Participant> getParticipants() {
        return new HashMap<>(participantMap);
    }

    /**
     * Returns the current local participant. If the room has not reached
     * {@link RoomState#CONNECTED} then this method will return null.
     */
    public LocalParticipant getLocalParticipant() {
        return localParticipant;
    }

    /**
     * Disconnects from the room.
     */
    public synchronized void disconnect() {
        if (roomState != RoomState.DISCONNECTED && nativeRoomContext != 0) {
            nativeDisconnect(nativeRoomContext);
        }
    }

    long getListenerNativeHandle() {
        return internalRoomListenerHandle.get();
    }

    void setNativeContext(long nativeRoomHandle) {
        this.nativeRoomContext = nativeRoomHandle;
    }

    /*
     * Needed for synchronizing during room creation.
     */
    Object getConnectLock() {
        return internalRoomListenerImpl;
    }

    synchronized void release() {
        if (nativeRoomContext != 0) {
            nativeRelease(nativeRoomContext);
            nativeRoomContext = 0;
            // TODO: Once native video team makes decision about participant strategy
            // after disconnect, make sure it is properly implemented here. For now we are just
            // removing native participant context in order to prevent memory leak.
            for (Participant participant : participantMap.values()) {
                participant.release();
            }
            internalRoomListenerHandle.release();
        }
    }

    void setState(RoomState roomState) {
        this.roomState = roomState;
    }

    // JNI Callbacks Interface
    interface InternalRoomListener {
        void onConnected(String roomSid,
                         String localParticipantSid,
                         String localParticipantIdentity,
                         List<Participant> participantList);
        void onDisconnected(RoomError roomError);
        void onConnectFailure(RoomError roomError);
        void onParticipantConnected(Participant participant);
        void onParticipantDisconnected(String participantSid);
    }

    class InternalRoomListenerImpl implements InternalRoomListener {

        // TODO: find better way to pass Handler to Media
        Handler getHandler() {
            return Room.this.handler;
        }

        @Override
        public synchronized void onConnected(String roomSid,
                                             String localParticipantSid,
                                             String localParticipantIdentity,
                                             List<Participant> participantList) {
            logger.d("onConnected()");
            Room.this.sid = roomSid;
            if (Room.this.name == null || Room.this.name.isEmpty()) {
                Room.this.name = roomSid;
            }
            Room.this.localParticipant = new LocalParticipant(
                    localParticipantSid, localParticipantIdentity, localMedia);
            for (Participant participant : participantList) {
                participantMap.put(participant.getSid(), participant);
            }
            Room.this.roomState = RoomState.CONNECTED;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Room.this.listener.onConnected(Room.this);
                }
            });
        }

        @Override
        public synchronized void onDisconnected(final RoomError roomError) {
            logger.d("onDisconnected()");
            Room.this.roomState = RoomState.DISCONNECTED;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Room.this.listener.onDisconnected(Room.this, roomError);
                }
            });
            release();
        }

        @Override
        public synchronized void onConnectFailure(final RoomError roomError) {
            logger.d("onConnectFailure()");
            Room.this.roomState = RoomState.DISCONNECTED;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Room.this.listener.onConnectFailure(Room.this, roomError);
                }
            });
        }

        @Override
        public synchronized void onParticipantConnected(final Participant participant) {
            logger.d("onParticipantConnected()");

            participantMap.put(participant.getSid(), participant);

            handler.post(new Runnable() {
                @Override
                public void run() {
                    Room.this.listener.onParticipantConnected(Room.this, participant);
                }
            });
        }

        @Override
        public synchronized void onParticipantDisconnected(String participantSid) {
            logger.d("onParticipantDisconnected()");

            final Participant participant = participantMap.remove(participantSid);
            if (participant == null) {
                logger.w("Received participant disconnected callback for non-existent participant");
                return;
            }
            participant.release();

            handler.post(new Runnable() {
                @Override
                public void run() {
                    Room.this.listener.onParticipantDisconnected(Room.this, participant);
                }
            });
        }

    }

    /**
     * Listener definition of room related events.
     */
    public interface Listener {
        /**
         * Called when a room has succeeded.
         *
         * @param room the connected room.
         */
        void onConnected(Room room);

        /**
         * Called when a connection to a room failed.
         *
         * @param room the room that failed to be connected to.
         * @param error an exception describing why connect failed.
         */
        void onConnectFailure(Room room, RoomError error);

        /**
         * Called when a room has been disconnected from.
         *
         * @param room the room that was disconnected from.
         * @param error An exception if there was a problem that caused the room to be
         *              disconnected from. This value will be null is there were no problems
         *              disconnecting from the room.
         */
        void onDisconnected(Room room, RoomError error);

        /**
         * Called when a participant has connected to a room.
         *
         * @param room the room the participant connected to.
         * @param participant the newly connected participant.
         */
        void onParticipantConnected(Room room, Participant participant);

        /**
         * Called when a participant has disconnected from a room.
         * @param room the room the participant disconnected from.
         * @param participant the disconnected participant.
         */
        void onParticipantDisconnected(Room room, Participant participant);
    }

    class InternalRoomListenerHandle extends NativeHandle {
        public InternalRoomListenerHandle(InternalRoomListener listener) {
            super(listener);
        }

        /*
         * Native Handle
         */
        @Override
        protected native long nativeCreate(Object object);

        @Override
        protected native void nativeRelease(long nativeHandle);
    }

    private native void nativeDisconnect(long nativeRoomContext);
    private native void nativeRelease(long nativeRoomContext);
}