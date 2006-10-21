//
// $Id$

package com.threerings.msoy.world.server;

import java.util.ArrayList;
import java.util.HashMap;

import com.samskivert.io.PersistenceException;

import com.samskivert.jdbc.RepositoryUnit;

import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.RandomUtil;
import com.samskivert.util.ResultListener;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.util.ResultAdapter;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SceneLocation;
import com.threerings.whirled.spot.server.SpotSceneManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MemberName;
import com.threerings.msoy.data.FriendEntry;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;

import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.RoomMarshaller;

import static com.threerings.msoy.Log.log;

/**
 * Manages a "Room".
 */
public class RoomManager extends SpotSceneManager
    implements RoomProvider
{
    @Override
    protected void didStartup ()
    {
        super.didStartup();

        _roomObj = (RoomObject) _plobj;

        _roomObj.setRoomService(
            (RoomMarshaller) MsoyServer.invmgr.registerDispatcher(
            new RoomDispatcher(this), false));
    }

    @Override
    protected void didShutdown ()
    {
        MsoyServer.invmgr.clearDispatcher(_roomObj.roomService);

        super.didShutdown();
    }


    @Override // documentation inherited
    protected SceneLocation computeEnteringLocation (
        BodyObject body, Portal entry)
    {
        MemberObject memberObj = (MemberObject) body;

        // automatically add the room to their recent list
        memberObj.addToRecentScenes(_scene.getId(), _scene.getName());

        if (entry != null) {
            return super.computeEnteringLocation(body, entry);
        }

        // fallback if there is no portal
        return new SceneLocation(
            new MsoyLocation(.5, 0, .5, (short) 0), body.getOid());
    }

    @Override
    protected PlaceObject createPlaceObject ()
    {
        return new RoomObject();
    }

    // documentation inherited from RoomProvider
    public void editRoom (
        ClientObject caller, InvocationService.ResultListener listener)
        throws InvocationException
    {
        if (!((MsoyScene) _scene).canEdit((MemberObject) caller)) {
            throw new InvocationException(ACCESS_DENIED);
        }

        // Create a list of all item ids
        ArrayList<ItemIdent> list = new ArrayList<ItemIdent>();
        for (FurniData furni : ((MsoyScene) _scene).getFurni()) {
            if (furni.itemType == Item.NOT_A_TYPE) {
                continue;
            }
            list.add(new ItemIdent(furni.itemType, furni.itemId));
        }

        MsoyServer.itemMan.getItems(list,
            new ResultAdapter<ArrayList<Item>>(listener));
    }

    // documentation inherited from RoomProvider
    public void updateRoom (
        ClientObject caller, SceneUpdate[] updates,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        if (!((MsoyScene) _scene).canEdit((MemberObject) caller)) {
            throw new InvocationException(ACCESS_DENIED);
        }

        ArrayIntSet scenesToId = null;
        for (SceneUpdate update : updates) {
            // TODO: complicated verification of changes, including
            // verifying that the user owns the items they're adding
            // (and that they don't add any props)

            // furniture modification updates require us to mark item usage
            if (update instanceof ModifyFurniUpdate) {
                ModifyFurniUpdate mfu = (ModifyFurniUpdate) update;
                MsoyServer.itemMan.updateItemUsage(_scene.getId(),
                    mfu.furniRemoved, mfu.furniAdded,
                    new ResultListener() {
                        public void requestCompleted (Object result) {}
                        public void requestFailed (Exception cause) {
                            log.warning("Unable to update item usage " +
                                "[e=" + cause + "].");
                        }
                    });


                // also, we locate all the scene ids named in added portals
                if (mfu.furniAdded != null) {
                    for (FurniData furni : mfu.furniAdded) {
                        if (furni.actionType == FurniData.ACTION_PORTAL) {
                            try {
                                int sceneId = Integer.parseInt(
                                    furni.splitActionData()[0]);
                                if (scenesToId == null) {
                                    scenesToId = new ArrayIntSet();
                                }
                                scenesToId.add(sceneId);
                            } catch (Exception e) {
                                // just accept it
                            }
                        }
                    }
                }
            }
        }

        if (scenesToId != null) {
            updateRoom2(updates, scenesToId);

        } else {
            finishUpdateRoom(updates);
        }
    }

    /**
     * updateRoom, continued. Look up any scene names for new portals.
     */
    protected void updateRoom2 (
        final SceneUpdate[] updates, ArrayIntSet scenesToId)
    {
        final int[] sceneIds = scenesToId.toIntArray();

        MsoyServer.invoker.postUnit(new RepositoryUnit("IdentifyScenes") {
            public void invokePersist ()
                throws PersistenceException
            {
                _sceneNames = MsoyServer.sceneRepo.identifyScenes(sceneIds);
            }

            public void handleSuccess ()
            {
                updateRoom3(updates, _sceneNames);
            }

            public void handleFailure (Exception e)
            {
                log.warning("Unable to identify scenes [err=" + e + "]");
                // just finish off
                finishUpdateRoom(updates);
            }

            protected HashIntMap<String> _sceneNames;
        });
    }

    /**
     * Assign the names to portals.
     */
    protected void updateRoom3 (
        SceneUpdate[] updates, HashIntMap<String> sceneNames)
    {
        for (SceneUpdate update : updates) {
            if (update instanceof ModifyFurniUpdate) {
                ModifyFurniUpdate mfu = (ModifyFurniUpdate) update;
                if (mfu.furniAdded != null) {
                    for (FurniData furni : mfu.furniAdded) {
                        if (furni.actionType == FurniData.ACTION_PORTAL) {
                            try {
                                int sceneId = Integer.parseInt(
                                    furni.splitActionData()[0]);
                                String name = sceneNames.get(sceneId);
                                if (name != null) {
                                    furni.actionData = sceneId + ":" + name;
                                }
                            } catch (Exception e) {
                                // whatever
                            }
                        }
                    }
                }
            }
        }

        finishUpdateRoom(updates);
    }

    /**
     * Ah, the final step in updating the room.
     */
    protected void finishUpdateRoom (SceneUpdate[] updates)
    {
        for (SceneUpdate update : updates) {
            recordUpdate(update);
        }
    }

    /** The room object. */
    protected RoomObject _roomObj;
}
