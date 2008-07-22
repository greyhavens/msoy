//
// $Id: MsoySceneService.java 8844 2008-04-15 17:05:43Z nathan $

package com.threerings.msoy.world.client;

import com.threerings.presents.client.InvocationReceiver;

public interface WatcherReceiver extends InvocationReceiver
{
    void memberMoved (int memberId, int sceneId, String hostname, int port);
}
