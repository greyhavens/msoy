package com.threerings.msoy.client {

import flash.utils.Dictionary;
import flash.utils.Timer;
import flash.events.TimerEvent;

import com.threerings.util.Log;
import com.threerings.util.Util;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.ClientEvent;

import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.VizMemberName;

import com.threerings.msoy.world.client.WorldController;

/**
 * Helps the user to make friends. Currently does the following:
 * <ul>
 *   <li>Captures all room occupants that are not already friends.</li>
 *   <li>After some time in the room, shows a batch friend request dialog.</li>
 *   <li>When playing a lobbied game, tracks all co-players in the session in games of at least a
 *     certain duration.</li>
 *   <li>When the user closes the game, shows a batch friend request dialog.</li>
 * </ul>
 * <p>The tracking of occupants and the room timer are reset when the user changes rooms. A person
 * will only be shown once in the batch dialog (until the client reloads). The batch dialog will
 * continue to be shown periodically if new people are seen.</p>
 *
 * TODO: change behavior based on whether the user skipped, invited or opted out
 * TODO: track the duration of other players so that someone who pops into the room or game and
 * pops back out again is not presented for friending
 * TODO: avrg tracking
 */
public class SocialDirector extends BasicDirector
{
    public static var log :Log = Log.getLog(SocialDirector);

    /** Time the player must be in a room before offering to friend seen people. */
    public static const ROOM_VISIT_TIME :int = (DeploymentConfig.devDeployment ? 1 : 5) * 60 * 1000;

    /** Time the player must be in a game before the other players are potential friends. */
    public static const GAME_PLAY_TIME :int = (DeploymentConfig.devDeployment ? 1 : 3) * 60 * 1000;

    /**
     * Creates a new social director.
     */
    public function SocialDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
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
        // TODO: why does this get called before clientDigLogon?
        if (_wobs != null) {
            _wobs.shutdown();
            _wobs = null;
        }
        _roomTimer.stop();
    }

    /**
     * Notifies us that the given observer is about to update its location.
     */
    public function willUpdateLocation (obs :Observer) :void
    {
        var seen :Array = obs.resetSeen();
        if (obs == _wobs) {
            // popup a friender if the user hasn't gone into a game and was in the current location
            // for at least ROOM_VISIT_TIME
            if (_roomTimer.currentCount > 0 && _gobs == null && seen.length > 0) {
                log.debug("Showing invite panel", "count", seen.length);
                _roomTimer.stop();
                BatchFriendInvitePanel.showRoom(_mctx, seen, function () :void {
                    _roomTimer.reset();
                    _roomTimer.start();
                });

                addNames(seen, _shown);

            } else {
                _roomTimer.reset();
                _roomTimer.start();
            }

        } else if (obs == _gobs) {
            // if the timer has expired, "bank" those seen. wait until the game ends to popup
            if (_gameTimer.currentCount > 0) {
                addNames(seen, _seenInGame);
                log.debug("Saved seen co-players", "count", seen.length);
            }
            _gameTimer.reset();
            _gameTimer.start();
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
     * Starts monitoring the occupants of a lobbied game, later potentially giving the user the
     * chance to befriend co-players.
     */
    public function trackLobbiedGame (ctx :CrowdContext) :void
    {
        var This :SocialDirector = this;

        // when the client logs on...
        ctx.getClient().addEventListener(ClientEvent.CLIENT_DID_LOGON,
            function (evt :ClientEvent) :void {
                // observe the occupants
                _gobs = new Observer(This, ctx.getLocationDirector());

                // show our popup when the user hits the close button
                if (_wctrl != null) {
                    _wctrl.addPlaceExitHandler(onExitLobbiedGame);
                }
            });

        // cleanup when the client logs off
        ctx.getClient().addEventListener(ClientEvent.CLIENT_DID_LOGOFF,
            function (evt :ClientEvent) :void {
                // TODO: why does this get called before clientDigLogon?
                if (_gobs != null) {
                    _gobs.shutdown();
                    _gobs = null;
                }
                _gameTimer.stop();
                if (_wctrl != null) {
                    _wctrl.removePlaceExitHandler(onExitLobbiedGame);
                }
            });
    }

    /**
     * Enables some world-specific features. Should be called close to client init time.
     */
    public function setWorldController (ctrl :WorldController) :void
    {
        _wctrl = ctrl;
    }

    /**
     * This is a place exit handler that will maybe invite the people seen during the game being
     * closed.
     */
    protected function onExitLobbiedGame () :Boolean
    {
        // get rid of our handler to stop this from getting called again
        _wctrl.removePlaceExitHandler(onExitLobbiedGame);

        // cheat and call this directly, we just want to bank any current occupants
        willUpdateLocation(_gobs);

        // grab the co-players and reset
        var seen :Array = Util.values(_seenInGame);
        _seenInGame = new Dictionary();

        // offer to befriend them
        var shown :Boolean = BatchFriendInvitePanel.showPostGame(
            _mctx, seen, _wctrl.handleClosePlaceView);

        // suppress future popups with the same people
        addNames(seen, _shown);

        log.debug("Lobbied game exit", "shown", shown, "count", seen.length);

        return !shown;
    }

    protected static function addNames (names :Array, dict :Dictionary) :void
    {
        for each (var name :VizMemberName in names) {
            dict[name.getMemberId()] = name;
        }
    }

    protected var _mctx :MsoyContext;
    protected var _wobs :Observer;
    protected var _gobs :Observer;
    protected var _wctrl :WorldController;
    protected var _shown :Dictionary = new Dictionary();
    protected var _roomTimer :Timer = new Timer(ROOM_VISIT_TIME);
    protected var _gameTimer :Timer = new Timer(GAME_PLAY_TIME);
    protected var _seenInGame :Dictionary = new Dictionary();
}
}

import flash.utils.Dictionary;

import com.threerings.util.Log;
import com.threerings.util.Name;
import com.threerings.util.Util;

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
        var names :Array = Util.values(_seen);
        _seen = new Dictionary();
        return names;
    }

    protected function locationDidChange (plobj :PlaceObject) :void
    {
        // let the director know the location is about to update
        _sdir.willUpdateLocation(this);

        if (_plobj != null) {
            _plobj.removeListener(_slnr);
            log.debug("Left location", "ploid", _plobj.getOid());
        }

        _plobj = plobj;

        if (_plobj != null) {
            log.debug("Entered location", "ploid", _plobj.getOid());
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
