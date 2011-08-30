//
// $Id$

package com.threerings.msoy.client {

import flash.utils.Dictionary;
import flash.utils.Timer;

import com.threerings.util.Log;
import com.threerings.util.Util;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.ClientEvent;

import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Helps the user to make friends. Currently does the following:
 * <ul>
 *   <li>Captures all room occupants that are not already friends.</li>
 *   <li>After some time in the room, shows a batch friend request dialog when the player leaves.
 *     </li>
 *   <li>When playing a lobbied game, tracks all co-players in the session in games of at least a
 *     certain duration.</li>
 *   <li>When the user closes the game, shows a batch friend request dialog.</li>
 *   <li>When playing an AVR game, the same tracking as for lobbied games is done, but room sharing
 *     is an additional criteria for co-player-ness.</li>
 *   <li>Supports hooking into the AVRG deactivation process, which may invoke a popup prior to
 *     officially deactivating the game.</li>
 * </ul>
 * <p>The tracking of occupants and the room timer are reset when the user changes rooms. A person
 * will only be shown once in the batch dialog (until the client reloads). The batch dialog will
 * continue to be shown periodically if new people are seen.</p>
 *
 * TODO: change behavior based on whether the user skipped, invited or opted out
 *
 * TODO: vary the required visit interval for rooms during an AVRG session - currently AVRGs with
 * a fast room change dynamic will result in a low or no friend suggestions
 *
 * TODO: the use of _gobs._seen as a means of filtering co-players for AVRGs is not really ideal,
 * too many false positives in a large player base. Searching the current occupant info set might
 * work, except then there could be a lot of false negatives. Some kind of _recentlySeen, with
 * timestamps and purging would probably work best.
 */
public class SocialDirector extends BasicDirector
{
    public static var log :Log = Log.getLog(SocialDirector);

    /** Time the player must be in a room before offering to friend seen people. */
    public static const ROOM_VISIT_TIME :int = (DeploymentConfig.devDeployment ? 1 : 5) * 60 * 1000;

    /** Time the player must be in a game before the other players are potential friends. */
    public static const GAME_PLAY_TIME :int = (DeploymentConfig.devDeployment ? 1 : 3) * 60 * 1000;

    /** Time another player must be in a place before the he or she is considered a
     * potential friend. */
    public static const HANG_OUT_TIME :int = 1 * 60 * 1000;

    /**
     * Logs a debug message, if debug logging is currently enabled.
     */
    public static function logDebug (message :String, ...args) :void
    {
        // TODO: some sort of runtime configuration for logging would be nice
        if (false) {
            args.unshift(message);
            log.debug.apply(null, args);
        }
    }

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
        if (_mctx.getMyName().isViewer()) {
            // Don't do anything if we're not a real member.
            return;
        }

        // world client has connected, start observing (this calls willUpdateLocation)
        _wobs = new Observer(this, _mctx.getLocationDirector(), willUpdateLocation);

        addExitHandler(onExitRoom);
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

        removeExitHandler(onExitRoom);
    }

    /**
     * Determines whether the supplied member should be added to the 'seen' list.
     */
    public function shouldAdd (name :VizMemberName) :Boolean
    {
        var memId :int = name.getId();
        // if we're not logged on to the world server yet or this member is us or already our
        // friend or we've already added them to the seen list, skip 'em
        var member :MemberObject = _mctx.getMemberObject();
        return (member != null) && (memId != member.getMemberId()) && (_shown[memId] == null) &&
            !member.isOnlineFriend(memId) &&
            !WorldContext(_mctx).getPartyDirector().partyContainsPlayer(memId);
    }

    /**
     * Starts monitoring the occupants of a lobbied game, later potentially giving the user the
     * chance to befriend co-players.
     */
    public function trackLobbiedGame (ctx :CrowdContext) :void
    {
        trackGame(ctx, false);
    }

    /**
     * Starts monitoring the occupants of an avr game, later potentially giving the user the chance
     * to befriend co-players.
     */
    public function trackAVRGame (ctx :CrowdContext) :void
    {
        trackGame(ctx, true);
    }

    /**
     * If we have any friend suggestions, show our popup, return false and invoke the deactivator
     * callback (no arguments) when the popup closes. If not, or if the popup is opted out, return
     * true.
     */
    public function mayDeactivateAVRGame (deactivator :Function) :Boolean
    {
        logDebug("Checking for AVRG deactivation");
        return !maybeShowGamePopup(deactivator);
    }

    /**
     * Notifies us that the given observer is about to update its location.
     */
    protected function willUpdateLocation (obs :Observer) :void
    {
        var seen :Array = obs.resetSeen();
        if (obs == _wobs) {
            var resetter :Function = function () :void {
                _roomTimer.reset();
                _roomTimer.start();
            };

            if (!maybeShowRoomPopup(seen, resetter)) {
                resetter();
            }

        } else if (obs == _gobs && !_avrg) {
            // if the timer has expired, "bank" those seen. wait until the session ends to popup
            if (_gameTimer.currentCount > 0) {
                addNames(seen, _seenInGame);
                logDebug("Saved seen co-players", "count", seen.length);
            }
            _gameTimer.reset();
            _gameTimer.start();
        }
    }

    protected function maybeShowRoomPopup (seen :Array, onClose :Function) :Boolean
    {
        // get rid of people the user has already blocked
        seen = seen.filter(isNotMuted);

        // bail if the user has not been in the room very long of there are no new occupants
        if (_roomTimer.currentCount == 0 || seen.length == 0) {
            return false;
        }

        // the user is in an avrg, bank the users that are also in the game
        if (_gobs != null && _avrg) {
            var filtered :Array = _gobs.filterUnseen(seen);
            addNames(filtered, _seenInGame);
            logDebug("Saved seen avrg co-players", "count", filtered.length,
                "roomCount", seen.length);
            return false;
        }

        // if the server is not accessible, don't bother
        if (!_mctx.getClient().isConnected()) {
            return false;
        }

        logDebug("Showing invite panel", "count", seen.length);
        _roomTimer.stop();
        if (!BatchFriendInvitePanel.showRoom(_mctx, seen, onClose)) {
            return false;
        }

        addNames(seen, _shown);
        return true;
    }

    /**
     * Starts monitoring the occupants of a game, later potentially giving the user the chance to
     * befriend co-players.
     */
    protected function trackGame (ctx :CrowdContext, avrg :Boolean) :void
    {
        var socdir :SocialDirector = this;
        logDebug("Tracking game", "avrg", avrg);

        // when the client logs on...
        ctx.getClient().addEventListener(ClientEvent.CLIENT_DID_LOGON,
            function (evt :ClientEvent) :void {
                // observe the occupants
                _gobs = new Observer(socdir, ctx.getLocationDirector(), willUpdateLocation);
                _avrg = avrg;

                // show our popup when the user hits the close button
                addExitHandler(onExitGame);
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
                removeExitHandler(onExitGame);
            });
    }

    /**
     * This is a place exit handler that will maybe invite the people seen during the game being
     * closed.
     */
    protected function onExitGame () :Boolean
    {
        // get rid of our handler to stop this from getting called again
        removeExitHandler(onExitGame);

        return !maybeShowGamePopup(_mctx.getMsoyController().handleClosePlaceView);
    }

    protected function maybeShowGamePopup (onClose :Function) :Boolean
    {
        // cheat and call these directly, we just want to bank any current occupants
        if (_avrg) {
            willUpdateLocation(_wobs); // this one has to be first
        }
        willUpdateLocation(_gobs);

        // grab the co-players and reset
        var seen :Array = Util.values(_seenInGame);
        _seenInGame = new Dictionary();

        seen = seen.filter(isNotMuted);
        if (seen.length == 0) {
            logDebug("No one seen, not showing game popup");
            return false;
        }

        // if the server is not accessible, don't bother
        if (!_mctx.getClient().isConnected()) {
            return false;
        }

        // offer to befriend them
        var shown :Boolean = BatchFriendInvitePanel.showPostGame(_mctx, seen, onClose);

        // suppress future popups with the same people
        addNames(seen, _shown);

        logDebug("Game popup shown", "shown", shown, "count", seen.length, "avrg", _avrg);

        return shown;
    }

    protected function onExitRoom () :Boolean
    {
        // get rid of our handler to stop this from getting called again
        removeExitHandler(onExitRoom);

        // show the popup if conditions are right, carry on closing if we did not show
        return !maybeShowRoomPopup(_wobs.resetSeen(),
            _mctx.getMsoyController().handleClosePlaceView);
    }

    protected function addExitHandler (fn :Function) :void
    {
        _mctx.getMsoyController().addPlaceExitHandler(fn);
    }

    protected function removeExitHandler (fn :Function) :void
    {
        _mctx.getMsoyController().removePlaceExitHandler(fn);
    }

    /**
     * A function suitable for use with Array.filter()
     */
    protected function isNotMuted (name :MemberName, ... ignored) :Boolean
    {
        return !_mctx.getMuteDirector().isMuted(name);
    }

    protected static function addNames (names :Array, dict :Dictionary) :void
    {
        for each (var name :VizMemberName in names) {
            dict[name.getId()] = name;
        }
    }

    protected var _mctx :MsoyContext;
    protected var _wobs :Observer;
    protected var _gobs :Observer;
    protected var _avrg :Boolean;
    protected var _shown :Dictionary = new Dictionary();
    protected var _roomTimer :Timer = new Timer(ROOM_VISIT_TIME);
    protected var _gameTimer :Timer = new Timer(GAME_PLAY_TIME);
    protected var _seenInGame :Dictionary = new Dictionary();
}
}

import flash.utils.Dictionary;
import flash.utils.getTimer;

import com.threerings.util.Name;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.SocialDirector;
import com.threerings.msoy.data.all.VizMemberName;

/**
 * Takes care of observing the user's location and recording all seen players.
 */
class Observer
{
    /**
     * Creates a new observer.
     */
    public function Observer (sdir :SocialDirector, ldir :LocationDirector, lupdater :Function)
    {
        _sdir = sdir;
        _ldir = ldir;
        _lupdater = lupdater;
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
     * Gets the array of qualifying names seen in the current location and then clears the set.
     */
    public function resetSeen () :Array
    {
        var names :Array = [];
        for each (var seen :Seen in _seen) {
            seen.didPart();
            logDebug("Leaving seen", "name", seen.name, "qual", seen.isQualified());
            if (seen.isQualified()) {
                names.push(seen.name);
            }
        }
        _seen = new Dictionary();
        return names;
    }

    /**
     * Returns a new array created by filtering out all unseen names. The array is assumed to
     * already not contain unqualified names. We only remove never-mets.
     */
    public function filterUnseen (names :Array) :Array
    {
        return names.filter(function (name :VizMemberName, ...unused) :Boolean {
            return _seen[name.getId()] != null;
        });
    }

    protected function locationDidChange (plobj :PlaceObject) :void
    {
        // let the director know the location is about to update
        _lupdater(this);

        if (_plobj != null) {
            _plobj.removeListener(_slnr);
            logDebug("Left location", "ploid", _plobj.getOid());
        }

        _plobj = plobj;

        if (_plobj != null) {
            logDebug("Entered location", "ploid", _plobj.getOid());
            _plobj.addListener(_slnr);

            // add the current members
            for each (var oinf :OccupantInfo in plobj.occupantInfo.toArray()) {
                updateSeen(oinf, function (seen :Seen) :void {
                    seen.didMeet();
                    logDebug("Met resident", "name", seen.name, "qual", seen.isQualified());
                });
            }
        }
    }

    /**
     * Notifies us that an entry has been added to a dset in the place object.
     */
    protected function entryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            updateSeen(OccupantInfo(event.getEntry()), function (seen :Seen) :void {
                seen.didMeet();
                logDebug("Met newcomer", "name", seen.name, "qual", seen.isQualified());
            });
        }
    }

    /**
     * Notifies us that an entry has been updated in a dset in the place object.
     */
    protected function entryUpdated (event :EntryUpdatedEvent) :void
    {
        // do this just in case something changes
        if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            var oinf :OccupantInfo = OccupantInfo(event.getEntry());
            updateSeen(oinf, function (seen :Seen) :void {
                seen.name = VizMemberName(oinf.username); // in case it has changed
                logDebug("Seen changed", "name", seen.name, "qual", seen.isQualified());
            });
        }
    }

    /**
     * Notifies us that an entry has been removed from a dset in the place object.
     */
    protected function entryRemoved (event :EntryRemovedEvent) :void
    {
        // do this just in case something changes
        if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            updateSeen(OccupantInfo(event.getOldEntry()), function (seen :Seen) :void {
                seen.didPart();
                logDebug("Seen left", "name", seen.name, "qual", seen.isQualified());
            });
        }
    }

    /**
     * Updates the Seen for the given name, with culling. The member may be entering or leaving
     * the place, according to the parameter.
     */
    protected function updateSeen (occInfo :OccupantInfo, updater :Function) :void
    {
        if (occInfo.username is VizMemberName) {
            var vname :VizMemberName = VizMemberName(occInfo.username);
            if (_sdir.shouldAdd(vname)) {
                var seen :Seen = Seen(_seen[vname.getId()]);
                if (seen == null) {
                    _seen[vname.getId()] = seen = new Seen(vname);
                }
                updater(seen);
            }
        }
        // else: PetName, or something else
    }

    protected static function logDebug (message :String, ...args) :void
    {
        args.unshift(message);
        SocialDirector.logDebug.apply(null, args);
    }

    protected var _lobs :LocationAdapter = new LocationAdapter(null, locationDidChange);
    protected var _slnr :SetAdapter = new SetAdapter(entryAdded, entryUpdated, entryRemoved);

    protected var _sdir :SocialDirector;
    protected var _ldir :LocationDirector;
    protected var _lupdater :Function;
    protected var _seen :Dictionary = new Dictionary(); /* int -> Seen */
    protected var _plobj :PlaceObject;
}

/**
 * Holds information about someone we've seen.
 */
class Seen
{
    /** Name of the seen party. */
    public var name :VizMemberName;

    /**
     * Creates a Seen information for the given name.
     */
    public function Seen (name :VizMemberName)
    {
        this.name = name;
        _timeEntered = int.MAX_VALUE;
    }

    /**
     * Returns true if the seen member qualifies as having hung out.
     */
    public function isQualified () :Boolean
    {
        return _qualified;
    }

    /**
     * Marks the member as having been in the same place (we came to them or they came to us).
     */
    public function didMeet () :void
    {
        // we're already qualified, nothing to do
        if (_qualified) {
            return;
        }

        // mark the time entered
        _timeEntered = getTimer();
    }

    /**
     * Marks the member as having parted company (left the room or we left the room).
     */
    public function didPart () :void
    {
        // we're already qualified, nothing to do
        if (_qualified) {
            return;
        }

        // anyone that hangs out for a while qualifies
        _qualified = (getTimer() - _timeEntered) > SocialDirector.HANG_OUT_TIME;
    }

    protected var _timeEntered :int;
    protected var _qualified :Boolean;
}
