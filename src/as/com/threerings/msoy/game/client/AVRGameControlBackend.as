//
// $Id$

package com.threerings.msoy.game.client {

import flash.geom.Point;
import flash.geom.Rectangle;
import flash.utils.ByteArray;

import com.threerings.util.Iterator;
import com.threerings.util.Name;
import com.threerings.util.ObjectMarshaller;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.LocationObserver;
import com.threerings.crowd.client.OccupantAdapter;
import com.threerings.crowd.client.OccupantObserver;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.ControlBackend;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.data.GameState;
import com.threerings.msoy.game.data.QuestState;
import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.data.RoomObject;

public class AVRGameControlBackend extends ControlBackend
{
    public static const log :Log = Log.getLog(AVRGameControlBackend);

    public function AVRGameControlBackend (
        gctx :GameContext, ctrl :AVRGameController, gameObj :AVRGameObject)
    {
        _gctx = gctx;
        _mctx = gctx.getWorldContext();
        _gameObj = gameObj;
        _ctrl = ctrl;

        _gameObj.addListener(_stateListener);
        _gameObj.addListener(_messageListener);

        _mctx.getLocationDirector().addLocationObserver(_locationObserver);
        _mctx.getOccupantDirector().addOccupantObserver(_occupantObserver);

        // will be null if not a room
        _roomObj = (_mctx.getLocationDirector().getPlaceObject() as RoomObject);
        if (_roomObj != null) {
            _roomObj.addListener(_movementListener);
        }
        _playerObj = _gctx.getPlayerObject();
        _playerObj.addListener(_playerStateListener);
        _playerObj.addListener(_playerMessageListener);
    }

    // from ControlBackend
    override public function shutdown () :void
    {
         super.shutdown();
        
         _gameObj.removeListener(_stateListener);
         _playerObj.removeListener(_playerStateListener);

         _mctx.getLocationDirector().removeLocationObserver(_locationObserver);
         _mctx.getOccupantDirector().removeOccupantObserver(_occupantObserver);
        
         if (_roomObj != null) {
             _roomObj.removeListener(_movementListener);
             _roomObj = null;
         }
    }

    public function tutorialEvent (eventName :String) :void
    {
        callUserCode("messageReceived_v1", "tutorialEvent", eventName);
    }

    // from GameControlBackend
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["getStageBounds_v1"] = getStageBounds_v1;
        o["getRoomBounds_v1"] = getRoomBounds_v1;

        o["getProperty_v1"] = getProperty_v1;
        o["setProperty_v1"] = setProperty_v1;
//        o["setPropertyAt_v1"] = setPropertyAt_v1;
        o["getPlayerProperty_v1"] = getPlayerProperty_v1;
        o["setPlayerProperty_v1"] = setPlayerProperty_v1;
//        o["setPlayerPropertyAt_v1"] = setPlayerPropertyAt_v1;
        o["sendMessage_v1"] = sendMessage_v1;

        o["offerQuest_v1"] = offerQuest_v1;
        o["updateQuest_v1"] = updateQuest_v1;
        o["completeQuest_v1"] = completeQuest_v1;
        o["cancelQuest_v1"] = cancelQuest_v1;
        o["getActiveQuests_v1"] = getActiveQuests_v1;

        o["deactivateGame_v1"] = deactivateGame_v1;
    }

    protected function getStageBounds_v1 () :Rectangle
    {
        return _mctx.getTopPanel().getPlaceViewBounds();
    }

    protected function getRoomBounds_v1 () :Rectangle
    {
        var view :RoomView = _mctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            var p :Point = view.localToGlobal(view.getScrollBounds().bottomRight);
            return new Rectangle(0, 0, p.x, p.y);
        }
        return null;
    }

    protected function getProperty_v1 (key :String) :Object
    {
        var entry :GameState = GameState(_gameObj.state.get(key));
        return (entry == null) ? null : ObjectMarshaller.decode(entry.value);
    }

    protected function setProperty_v1 (
        key :String, value: Object, persistent :Boolean) :Boolean
    {
        var wgsvc :AVRGameService = _gameObj.avrgService;
        if (value == null) {
            wgsvc.deleteProperty(_gctx.getClient(), key,
                                 loggingConfirmListener("deleteProperty"));

        } else {
            wgsvc.setProperty(_gctx.getClient(), key,
                              ObjectMarshaller.validateAndEncode(value), persistent,
                              loggingConfirmListener("setProperty"));

        }
        return true;
    }
    
    protected function getPlayerProperty_v1 (key :String) :Object
    {
        var entry :GameState = GameState(_playerObj.gameState.get(key));
        return (entry == null) ? null : ObjectMarshaller.decode(entry.value);
    }

    protected function setPlayerProperty_v1 (
        key :String, value: Object, persistent :Boolean) :Boolean
    {
        var wgsvc :AVRGameService = _gameObj.avrgService;
        if (value == null) {
            wgsvc.deletePlayerProperty(_gctx.getClient(), key,
                                       loggingConfirmListener("deletePlayerProperty"));

        } else {
            wgsvc.setPlayerProperty(_gctx.getClient(), key,
                                    ObjectMarshaller.validateAndEncode(value), persistent,
                                    loggingConfirmListener("setPlayerProperty"));
        }
        return true;
    }

    protected function sendMessage_v1 (key :String, value :Object, playerId :int) :Boolean
    {
        _gameObj.avrgService.sendMessage(_gctx.getClient(), key,
                                         ObjectMarshaller.validateAndEncode(value),
                                         playerId, loggingInvocationListener("sendMessage"));
        return true;
    }

    protected function offerQuest_v1 (questId :String, intro :String, initialStatus :String)
        :Boolean
    {
        if (!isPlaying() || isOnQuest(questId)) {
            return false;
        }
        var view :RoomView = _mctx.getTopPanel().getPlaceView() as RoomView;
        if (view == null) {
            // should hopefully not happen
            return false;
        }

        var actualOffer :Function = function() :void {
            _gameObj.avrgService.startQuest(_gctx.getClient(), questId, initialStatus,
                loggingConfirmListener("startQuest", function () :void {
                    _mctx.displayFeedback(null, "Quest begun: " + initialStatus);
                }));
        };

        if (intro == null) {
            // only the tutorial is allowed to skip the UI
            if (_mctx.getGameDirector().isPlayingTutorial()) {
                actualOffer();
                return true;
            }
            return false;
        }

        view.getRoomController().offerQuest(_gctx, intro, actualOffer);
        return true;
    }

    protected function updateQuest_v1 (questId :String, step :int, status :String) :Boolean
    {
        if (!isOnQuest(questId)) {
            return false;
        }
        _gameObj.avrgService.updateQuest(
            _gctx.getClient(), questId, step, status, loggingConfirmListener(
                "updateQuest", function () :void {
                    _mctx.displayFeedback(null, "Quest update: " + status);
                }));
        return true;
    }

    protected function completeQuest_v1 (questId :String, outro :String, payout :int) :Boolean
    {
        if (!isPlaying() || !isOnQuest(questId)) {
            return false;
        }
        var view :RoomView = _mctx.getTopPanel().getPlaceView() as RoomView;
        if (view == null) {
            // should hopefully not happen
            return false;
        }

        var actualComplete :Function = function() :void {
            _gameObj.avrgService.completeQuest(
                _gctx.getClient(), questId, payout, loggingConfirmListener(
                    "completeQuest", function () :void {
                        _mctx.displayFeedback(null, "Quest completed!");
                    }));
        };

        if (outro == null) {
            // only the tutorial is allowed to skip the UI
            if (_mctx.getGameDirector().isPlayingTutorial()) {
                actualComplete();
                return true;
            }
            return false;
        }

        view.getRoomController().completeQuest(_gctx, outro, actualComplete);
        return true;
    }

    protected function cancelQuest_v1 (questId :String) :Boolean
    {
        if (!isPlaying() || !isOnQuest(questId)) {
            return false;
        }
        // TODO: confirmation dialog
        _gameObj.avrgService.cancelQuest(
            _gctx.getClient(), questId, loggingConfirmListener(
                "cancelQuest", function () :void {
                    _mctx.displayFeedback(null, "Quest cancelled!");
                }));
        return true;
    }

    protected function getActiveQuests_v1 () :Array
    {
        var list :Array = new Array();
        var i :Iterator = _playerObj.questState.iterator();
        while (i.hasNext()) {
            var state :QuestState = QuestState(i.next());
            list.push([ state.questId, state.step, state.status ]);
        }
        return list;
    }

    protected function deactivateGame_v1 () :Boolean
    {
        if (!isPlaying()) {
            return false;
        }
        _mctx.getGameDirector().leaveAVRGame();
        return true;
    }

    protected function callStateChanged (entry :GameState) :void
    {
        callUserCode("stateChanged_v1", entry.key, ObjectMarshaller.decode(entry.value));
    }
    
    protected function callPlayerStateChanged (entry :GameState) :void
    {
        callUserCode("playerStateChanged_v1", entry.key, ObjectMarshaller.decode(entry.value));
    }

    protected function loggingConfirmListener (svc :String, processed :Function = null)
        :InvocationService_ConfirmListener
    {
        return new ConfirmAdapter(function (cause :String) :void {
            log.warning("Service failure [service=" + svc + ", cause=" + cause + "].");
        }, processed);
    }

    protected function loggingInvocationListener (svc :String) :InvocationService_InvocationListener
    {
        return new InvocationAdapter(function (cause :String) :void {
            log.warning("Service failure [service=" + svc + ", cause=" + cause + "].");
        });
    }

    protected function isPlaying () :Boolean
    {
        return _mctx.getGameDirector().getGameId() == _ctrl.getGameId();
    }

    protected function isOnQuest (questId :String) :Boolean
    {
        var i :Iterator = _playerObj.questState.iterator();
        while (i.hasNext()) {
            var state :QuestState = QuestState(i.next());
            if (state.questId == questId) {
                return true;
            }
        }
        return false;
    }

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;
    protected var _gameObj :AVRGameObject;
    protected var _playerObj :PlayerObject;
    protected var _roomObj :RoomObject;

    protected var _stateListener :SetAdapter = new SetAdapter(
        function (event :EntryAddedEvent) :void {
            if (event.getName() == AVRGameObject.STATE) {
                callStateChanged(event.getEntry() as GameState);
            }
        },
        function (event :EntryUpdatedEvent) :void {
            if (event.getName() == AVRGameObject.STATE) {
                callStateChanged(event.getEntry() as GameState);
            }
        });

    protected var _messageListener :MessageListener = new MessageAdapter(
        function (event :MessageEvent) :void {
            if (AVRGameObject.USER_MESSAGE == event.getName()) {
                var args :Array = event.getArgs();
                var key :String = (args[0] as String);
                callUserCode("messageReceived_v1", key, ObjectMarshaller.decode(args[1]));
            }
        });

    protected var _playerStateListener :SetAdapter = new SetAdapter(
        function (event :EntryAddedEvent) :void {
            if (event.getName() == PlayerObject.GAME_STATE) {
                callPlayerStateChanged(event.getEntry() as GameState);
            } else if (event.getName() == PlayerObject.QUEST_STATE) {
                callUserCode("questStateChanged_v1", QuestState(event.getEntry()).questId, true);
            }
        },
        function (event :EntryUpdatedEvent) :void {
            if (event.getName() == PlayerObject.GAME_STATE) {
                callPlayerStateChanged(event.getEntry() as GameState);
            }
        },
        function (event :EntryRemovedEvent) :void {
            if (event.getName() == PlayerObject.QUEST_STATE) {
                callUserCode("questStateChanged_v1", event.getKey(), false);
            }
        });

    protected var _playerMessageListener :MessageListener = new MessageAdapter(
        function (event :MessageEvent) :void {
            if (AVRGameObject.USER_MESSAGE + ":" + _gameObj.getOid() == event.getName()) {
                var args :Array = event.getArgs();
                var key :String = (args[0] as String);
                callUserCode("messageReceived_v1", key, ObjectMarshaller.decode(args[1]));
            }
        });

    // TODO: These events are not in themselves a complete API
    protected var _locationObserver :LocationObserver = new LocationAdapter(
        null, function (place :PlaceObject) :void {
            if (_roomObj != null) {
                _roomObj.removeListener(_movementListener);
                callUserCode("leftRoom_v1");
            }
            _roomObj = (place as RoomObject);
            if (_roomObj != null) {
                _roomObj.addListener(_movementListener);
                callUserCode("enteredRoom_v1");
            }
    }, null);

    protected var _movementListener :SetAdapter = new SetAdapter(null,
        function (event :EntryUpdatedEvent) :void {
            if (event.getName() == SpotSceneObject.OCCUPANT_LOCS) {
                callUserCode("occupantMoved_v1", int(event.getEntry().getKey()));
            }
        });
    
    protected var _occupantObserver :OccupantObserver = new OccupantAdapter(
        function (info :OccupantInfo) :void {
            if (_roomObj != null) {
                callUserCode("occupantEntered_v1", info.getBodyOid());
            }
        },
        function (info :OccupantInfo) :void {
            if (_roomObj != null) {
                callUserCode("occupantLeft_v1", info.getBodyOid());
            }
        });
}
}
