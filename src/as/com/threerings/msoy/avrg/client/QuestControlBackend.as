//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.Iterator;
import com.threerings.util.Log;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;

import com.threerings.presents.dobj.*;

import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.data.QuestState;

import com.threerings.msoy.avrg.data.AVRGameObject;

public class QuestControlBackend
{
    public static const log :Log = Log.getLog(QuestControlBackend);

    public function QuestControlBackend (
        wctx :WorldContext, gctx :GameContext, backend :AVRGameBackend, gameObj :AVRGameObject)
    {
        _gctx = gctx;
        _wctx = wctx;
        _backend = backend;
        _gameObj = gameObj;

        _playerObj = _gctx.getPlayerObject();
        _playerObj.addListener(_playerStateListener);
    }

    public function shutdown () :void
    {
         _playerObj.removeListener(_playerStateListener);
    }

    public function populateSubProperties (o :Object) :void
    {
        // QuestControl (sub)
        o["offerQuest_v1"] = offerQuest_v1;
        o["updateQuest_v1"] = updateQuest_v1;
        o["completeQuest_v1"] = completeQuest_v1;
        o["cancelQuest_v1"] = cancelQuest_v1;
        o["getActiveQuests_v1"] = getActiveQuests_v1;
    }

    protected function offerQuest_v1 (questId :String, intro :String, initialStatus :String)
        :Boolean
    {
        if (!questId || !_backend.isPlaying() || isOnQuest(questId)) {
            return false;
        }
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view == null) {
            // should hopefully not happen
            return false;
        }

        var actualOffer :Function = function() :void {
            _gameObj.avrgService.startQuest(_gctx.getClient(), questId, initialStatus,
                loggingConfirmListener("startQuest", function () :void {
//                    _wctx.displayFeedback(null, "Quest begun: " + initialStatus);
                }));
        };

        if (intro == null) {
            // only the tutorial is allowed to skip the UI
            if (_wctx.getGameDirector().isPlayingTutorial()) {
                actualOffer();
                return true;
            }
            return false;
        }

        new QuestOfferPanel(_wctx, intro, actualOffer).open(false);
        return true;
    }

    protected function updateQuest_v1 (questId :String, step :int, status :String) :Boolean
    {
        if (!questId || !isOnQuest(questId)) {
            return false;
        }
        _gameObj.avrgService.updateQuest(
            _gctx.getClient(), questId, step, status, loggingConfirmListener(
                "updateQuest", function () :void {
//                    _wctx.displayFeedback(null, "Quest update: " + status);
                }));
        return true;
    }

    protected function completeQuest_v1 (questId :String, outro :String, payout :Number) :Boolean
    {
        if (!questId || !_backend.isPlaying()) {
            return false;
        }
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view == null) {
            // should hopefully not happen
            return false;
        }

        // sanity check payout
        if (payout < 0 || payout > 1) {
            _wctx.displayFeedback(null, "completeQuest() payout must be between 0 and 1.");
            return false;
        }

        var actualComplete :Function = function() :void {
            _gameObj.avrgService.completeQuest(
                _gctx.getClient(), questId, payout, loggingConfirmListener(
                    "completeQuest", function () :void {
//                        _wctx.displayFeedback(null, "Quest completed!");
                    }));
        };

        if (outro == null) {
            actualComplete();
            return true;
        }

        new QuestCompletionPanel(_wctx, outro, actualComplete).open(false);
        return true;
    }

    protected function cancelQuest_v1 (questId :String) :Boolean
    {
        if (!questId || !_backend.isPlaying() || !isOnQuest(questId)) {
            return false;
        }
        // TODO: confirmation dialog
        _gameObj.avrgService.cancelQuest(
            _gctx.getClient(), questId, loggingConfirmListener(
                "cancelQuest", function () :void {
//                    _wctx.displayFeedback(null, "Quest cancelled!");
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

    protected var _wctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _backend :AVRGameBackend;
    protected var _gameObj :AVRGameObject;
    protected var _playerObj :PlayerObject;

    protected var _playerStateListener :SetAdapter = new SetAdapter(
        function (event :EntryAddedEvent) :void {
            if (event.getName() == PlayerObject.QUEST_STATE) {
                _backend.callUserCode(
                    "questStateChanged_v1", QuestState(event.getEntry()).questId, true);
            }
        },
        null,
        function (event :EntryRemovedEvent) :void {
            if (event.getName() == PlayerObject.QUEST_STATE) {
                _backend.callUserCode("questStateChanged_v1", event.getKey(), false);
            }
        });
}
}
