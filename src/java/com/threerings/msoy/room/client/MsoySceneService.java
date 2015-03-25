//
// $Id$

package com.threerings.msoy.room.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

import com.threerings.whirled.client.SceneService.SceneMoveListener;
import com.threerings.whirled.client.SceneService;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.world.client.WorldService;

/**
 * Extends the {@link SceneService} with a scene traversal mechanism needed by Whirled.
 */
public interface MsoySceneService extends InvocationService<ClientObject>
{
    public static interface MsoySceneMoveListener extends SceneMoveListener
    {
        /**
         * Indicates that the client should display an avatar select UI for the player,
         * call {@link WorldService#acceptAndProceed} when it's done and then finally retry
         * this move. The group name is passed for describing to the user what is happening.
         */
        public void selectGift (Avatar[] avatars, String groupName);

        /**
         * Indicates that the client must start up the given AVRG and let it take over,
         * informing it what scene it was we tried to enter.
         */
        public void moveToBeHandledByAVRG (int gameId, int sceneId);
    }

    /**
     * Requests that that this client's body be moved to the specified scene.
     *
     * @param sceneId the scene id to which we want to move.
     * @param version the version number of the scene object that we have in our local repository.
     * @param portalId the id of the portal via which we are departing the current scene, or 0.
     * @param destLoc the location in the target scene where the client wishes to enter.
     */
    public void moveTo (int sceneId, int version, int portalId, MsoyLocation destLoc,
                        MsoySceneMoveListener listener);
}
