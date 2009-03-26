//
// $Id$

package com.threerings.msoy.world.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.data.HomePageItem;

/**
 * Provides global services to the world client.
 */
public interface WorldService extends InvocationService
{
    /**
     * Get the given group's home scene id.
     */
    void getGroupHomeSceneId (Client client, int groupId, ResultListener listener);

    /**
     * Requests the items to populate the home page grid. The expected response is an arry of
     * {@link HomePageItem}. This should eventually take a parameter so that the top 3 "whirled"
     * items are a separate request from the very cachable 6 "what I've done recently" items.
     */
    void getHomePageGridItems (Client client, ResultListener listener);
}
