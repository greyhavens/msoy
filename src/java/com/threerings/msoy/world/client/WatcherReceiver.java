//
// $Id$

package com.threerings.msoy.world.client;

import com.threerings.presents.client.InvocationReceiver;

/**
 * Interface for notifying clients of the whereabouts of a member, including the server which the
 * member is physically logged into.
 */
public interface WatcherReceiver extends InvocationReceiver
{
    /**
     * Notifes the receiver that the member has logged on and/or entered a new scene.
     * @param memberId id of the member making the move
     * @param sceneId id of the scene the member moved to
     * @param hostname world server that now hosts the member
     * @param port the port number the world server is running on
     */
    void memberMoved (int memberId, int sceneId, String hostname, int port);
    
    /**
     * Notifies the receiver that a member's session with a world server has ended. This may mean
     * the member is in transit to a room on another server or has logged off permanently. There
     * is no way to tell.
     */
    void memberLoggedOff (int memberId);
}
