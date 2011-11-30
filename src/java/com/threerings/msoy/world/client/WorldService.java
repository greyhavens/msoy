//
// $Id$

package com.threerings.msoy.world.client;

import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.data.HomePageItem;
import com.threerings.msoy.item.data.all.Avatar;

/**
 * Provides global services to the world client.
 */
public interface WorldService extends InvocationService
{
    /**
     * Result listener for {@link #getHomeId()}.
     */
    interface HomeResultListener extends InvocationListener
    {
        /**
         * Instructs the client that an avatar selection is required before entering the room. The
         * client should show the avatar picker, then call {@link WorldService.acceptAndProceed}.
         */
        void selectGift (Avatar[] avatars, int sceneId);

        /**
         * Instructs the client that the room may now be entered. All preparations have been made.
         */
        void readyToEnter (int sceneId);
    }

    /**
     * Requests the items to populate the home page grid. The expected response is an arry of
     * {@link HomePageItem}. This should eventually take a parameter so that the top 3 "whirled"
     * items are a separate request from the very cachable 6 "what I've done recently" items.
     */
    void getHomePageGridItems (ResultListener listener);

    /**
     * Request to know the home scene id for the specified owner.
     * @see {@link com.threerings.msoy.room.data.MsoySceneModel}.
     */
    void getHomeId (byte ownerType, int ownerId, HomeResultListener listener);

    /**
     * Dresses the user in the avatar of the specified catalog id, adding it to their inventory
     * and returns the home scene id to go to. The catalog id must be chosen from a previously
     * returned gift list ({@link HomeResultListener#selectGift()}).
     */
    void acceptAndProceed (int giftCatalogId, ConfirmListener listener);

    /**
     * Set the given scene as the owner's home scene
     */
    void setHomeSceneId (int ownerType, int ownerId, int sceneId,
                         ConfirmListener listener);

    /**
     * Invites the specified member to follow the caller. Passing 0 for the memberId will clear all
     * of the caller's followers.
     */
    void inviteToFollow (int memberId, InvocationListener listener);

    /**
     * Requests to follow the specified member who must have previously issued an invitation to the
     * caller to follow them. Passing 0 for memberId will clear the caller's following status.
     */
    void followMember (int memberId, InvocationListener listener);

    /**
     * Removes a player from the caller's list of followers. Passing 0 for memberId will clear all
     * the caller's followers.
     */
    void ditchFollower (int memberId, InvocationListener listener);

    /**
     * Set the avatar in use by this user.
     */
    void setAvatar (int avatarId, ConfirmListener listener);

    /**
     * Called when a new DJ player has finished the tutorial.
     */
    void completeDjTutorial (InvocationListener listener);
}
