//
// $Id$

package com.threerings.msoy.world.server;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

import com.threerings.msoy.world.client.WorldService;

/**
 * Defines the server-side of the {@link WorldService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from WorldService.java.")
public interface WorldProvider extends InvocationProvider
{
    /**
     * Handles a {@link WorldService#acceptAndProceed} request.
     */
    void acceptAndProceed (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link WorldService#completeDjTutorial} request.
     */
    void completeDjTutorial (ClientObject caller, InvocationService.InvocationListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link WorldService#ditchFollower} request.
     */
    void ditchFollower (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link WorldService#followMember} request.
     */
    void followMember (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link WorldService#getHomeId} request.
     */
    void getHomeId (ClientObject caller, byte arg1, int arg2, WorldService.HomeResultListener arg3)
        throws InvocationException;

    /**
     * Handles a {@link WorldService#getHomePageGridItems} request.
     */
    void getHomePageGridItems (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;

    /**
     * Handles a {@link WorldService#inviteToFollow} request.
     */
    void inviteToFollow (ClientObject caller, int arg1, InvocationService.InvocationListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link WorldService#setAvatar} request.
     */
    void setAvatar (ClientObject caller, int arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link WorldService#setHomeSceneId} request.
     */
    void setHomeSceneId (ClientObject caller, int arg1, int arg2, int arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;
}
