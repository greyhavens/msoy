//
// $Id$

package client.games;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.Link;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameInfo.Sort;
import com.threerings.msoy.web.gwt.Pages;

/**
 * User interface for adding games to an arcade. This is mostly the same as a game genre list
 * with genre ALL except the right hand column shows an add or remove button instead of a play
 * button.
 */
public class AddTopGamesPanel extends GameGenrePanel
{
    public AddTopGamesPanel (ArcadeData.Portal page, final Sort sort, String query)
    {
        super(GameGenre.ALL, sort, query);
        _gamesvc.loadTopGameIds(_portal = page, new InfoCallback<int[]> () {
            public void onSuccess (int[] result) {
                _topGameIds = new HashSet<Integer>();
                for (int gameId : result) {
                    _topGameIds.add(gameId);
                }
                for (Integer gameId : _actions.keySet()) {
                    updateAction(gameId);
                }
            }
        });
    }

    @Override
    protected void onSortChanged (Sort sort)
    {
        Link.go(Pages.GAMES, "at", _portal.toByte(), sort.toToken(), _header.getQuery());
    }

    @Override
    protected Widget createActionWidget (GameInfo game)
    {
        FlowPanel action = new FlowPanel();
        _actions.put(game.gameId, action);
        updateAction(game.gameId);
        return action;
    }

    protected void updateAction (final Integer gameId)
    {
        FlowPanel action = _actions.get(gameId);
        if (action == null) {
            return;
        }
        action.clear();
        if (_topGameIds == null) {
            action.add(new Label(_msgs.atgLoading()));
            return;
        }
        final boolean topGame = _topGameIds.contains(gameId);
        String text = topGame ? _msgs.atgRemove() : _msgs.atgAdd();
        Button button = new Button(text);
        action.add(button);
        new ClickCallback<Void>(button) {
            @Override protected boolean callService () {
                if (topGame) {
                    _gamesvc.removeTopGame(_portal, gameId, this);
                } else {
                    _gamesvc.addTopGame(_portal, gameId, this);
                }
                return true;
            }

            @Override protected boolean gotResult (Void result) {
                if (topGame) {
                    _topGameIds.remove(gameId);
                } else {
                    _topGameIds.add(gameId);
                }
                updateAction(gameId);
                return true;
            }
        };
    }

    protected ArcadeData.Portal _portal;
    protected Set<Integer> _topGameIds;
    protected HashMap<Integer, FlowPanel> _actions = new HashMap<Integer, FlowPanel>();
}
