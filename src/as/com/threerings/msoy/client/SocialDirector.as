package com.threerings.msoy.client {

import flash.utils.Dictionary;
import flash.utils.Timer;
import flash.events.TimerEvent;

import com.threerings.util.Log;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.ClientEvent;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.VizMemberName;

/**
 * Helps the user to make friends. Currently does the following:
 * <ul>
 *   <li>Captures all room occupants that are not already friends.</li>
 *   <li>After some time in the room, shows a batch friend request dialog.</li>
 * </ul>
 * <p>The tracking of occupants and the room timer are reset when the user changes rooms. A person
 * will only be shown once in the batch dialog (until the client reloads). The batch dialog will
 * continue to be shown periodically if new people are seen.</p>
 *
 * TODO: change behavior based on whether the user skipped, invited or opted out
 * TODO: lobbied game and avrg tracking
 */
public class SocialDirector extends BasicDirector
{
    public static var log :Log = Log.getLog(SocialDirector);

    /** Time the player must be in a room before offering to friend seen people. */
    public static const ROOM_VISIT_TIME :int = (DeploymentConfig.devDeployment ? 1 : 5) * 60 * 1000;

    /**
     * Creates a new social director.
     */
    public function SocialDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
        _roomTimer.addEventListener(TimerEvent.TIMER, handleRoomTimer);
    }

    // from BasicDirector
    override public function clientDidLogon (event :ClientEvent) :void
    {
        // world client has connected, start observing (this calls willUpdateLocation)
        _wobs = new Observer(this, _mctx.getLocationDirector());
    }

    // from BasicDirector
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        // world client has disconnected, kill observer
        _wobs.shutdown();
        _wobs = null;
        _roomTimer.stop();
    }

    /**
     * Notifies us that the given observer is about to update its location.
     */
    public function willUpdateLocation (obs :Observer) :void
    {
        // if this is the world observer, reset the seen list and restart the timer
        if (obs == _wobs) {
            _wobs.resetSeen();
            _roomTimer.reset();
            _roomTimer.start();
        }
    }

    /**
     * Detects if a given name is a friend.
     */
    public function isFriend (name :VizMemberName) :Boolean
    {
        var member :MemberObject = MemberObject(_mctx.getMsoyClient().getClientObject());
        return name.getMemberId() == member.getMemberId() ||
            member.friends.containsKey(name.getKey());
    }

    /**
     * Detects whether the given name has been offered to the user already.
     */
    public function hasShown (name :VizMemberName) :Boolean
    {
        return _shown[name.getMemberId()] != null;
    }

    /**
     * Notifies us that the user has been in a room for a while.
     */
    protected function handleRoomTimer (event :TimerEvent) :void
    {
        var seen :Array = _wobs.resetSeen();
        if (seen.length > 0) {
            log.debug("Showing invite panel", "count", seen.length);

            _roomTimer.stop();

            // TODO: change our behavior if the user skips the invite?
            BatchFriendInvitePanel.showRoom(_mctx, seen, function () :void {
                _roomTimer.reset();
                _roomTimer.start();
            });

            for each (var name :VizMemberName in seen) {
                _shown[name.getMemberId()] = true;
            }

        } else {
            log.debug("Not showing invite panel, noone new seen");
        }
    }

    protected var _mctx :MsoyContext;
    protected var _wobs :Observer;
    protected var _gobs :Observer;
    protected var _shown :Dictionary = new Dictionary();
    protected var _roomTimer :Timer = new Timer(ROOM_VISIT_TIME);
}
}

import flash.utils.Dictionary;

import com.threerings.util.Log;
import com.threerings.util.Name;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.LocationDirector;

import com.threerings.msoy.client.SocialDirector;

import com.threerings.msoy.data.all.VizMemberName;

/**
 * Takes care of observing the user's location and recording all seen players.
 */
class Observer
{
    public static var log :Log = SocialDirector.log;

    /**
     * Creates a new observer.
     */
    public function Observer (sdir :SocialDirector, ldir :LocationDirector)
    {
        _sdir = sdir;
        _ldir = ldir;
        _ldir.addLocationObserver(_lobs);
        locationDidChange(_ldir.getPlaceObject());
    }

    /**
     * Stops the observing.
     */
    public function shutdown () :void
    {
        _ldir.removeLocationObserver(_lobs);
        locationDidChange(null);
    }

    /**
     * Gets the array of players seen in the current location and resets it.
     */
    public function resetSeen () :Array
    {
        var names :Array = [];
        for each (var name :VizMemberName in _seen) {
            names.push(name);
        }
        _seen = new Dictionary();
        return names;
    }

    protected function locationDidChange (plobj :PlaceObject) :void
    {
        // let the director know the location is about to update
        _sdir.willUpdateLocation(this);

        if (_plobj != null) {
            _plobj.removeListener(_slnr);
        }

        _plobj = plobj;

        if (_plobj != null) {
            _plobj.addListener(_slnr);

            // add the current members
            for each (var oinf :OccupantInfo in plobj.occupantInfo.toArray()) {
                didSee(oinf.username);
            }
        }
    }

    /**
     * Notifies us that an entry has been added to a dset in the place object.
     */
    protected function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            didSee(OccupantInfo(event.getEntry()).username);
        }
    }

    /**
     * Notifies us that an entry has been updated in a dset in the place object.
     */
    protected function entryUpdated (event :EntryUpdatedEvent) :void
    {
        // do this just in case something changes
        if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            didSee(OccupantInfo(event.getEntry()).username);
        }
    }

    /**
     * Marks the given name as having been seen. Culls appropriately.
     */
    protected function didSee (name :Name) :void
    {
        if (name is VizMemberName) {
            var vname :VizMemberName = VizMemberName(name);
            if (_sdir.isFriend(vname)) {
                log.debug("Not adding friend occupant", "name", name);

            } else if (_sdir.hasShown(vname)) {
                log.debug("Not adding previously shown occupant", "name", name);

            } else {
                _seen[vname.getMemberId()] = name;
                log.debug("Adding seen occupant", "name", name);
            }

        } else {
            log.warning("Name not a VizMemberName?", "name", name);
        }
    }

    protected var _lobs :LocationAdapter = new LocationAdapter(null, locationDidChange);
    protected var _slnr :SetAdapter = new SetAdapter(entryAdded, entryUpdated, null);

    protected var _sdir :SocialDirector;
    protected var _ldir :LocationDirector;
    protected var _seen :Dictionary = new Dictionary();
    protected var _plobj :PlaceObject;
}
