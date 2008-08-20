//
// $Id$

package com.threerings.msoy.world.client;

import com.threerings.presents.client.InvocationReceiver;

public interface WatcherReceiver extends InvocationReceiver
{
    void memberMoved (int memberId, int sceneId, String hostname, int port);
}
