//
// $Id$

package client.games;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.game.gwt.GameService.TopGamesResult;

import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * User interface for editing arcade portal pages.
 */
public class EditArcadePanel extends FlowPanel
{
    /**
     * Creates a new arcade editing panel.
     */
    public EditArcadePanel ()
    {
        setStyleName("editArcadePanel");
        setPage(ArcadeData.Portal.MAIN);
    }

    protected void setPage (ArcadeData.Portal portal)
    {
        clear();
        HorizontalPanel topBits = new HorizontalPanel();
        add(topBits);

        topBits.setStyleName("Pages");
        topBits.add(MsoyUI.createLabel(_msgs.editArcadePage(), null));
        topBits.add(WidgetUtil.makeShim(10, 10));
        topBits.add(_pages = new ListBox());
        for (ArcadeData.Portal val : ArcadeData.Portal.values()) {
            // TODO: i18n, this is just the enum name for now
            _pages.addItem(val.toString());
            if (val == portal) {
                _pages.setSelectedIndex(_pages.getItemCount() - 1);
            }
        }

        topBits.add(WidgetUtil.makeShim(10, 10));
        switch (portal) {
        case FACEBOOK:
            topBits.add(Link.create(_msgs.editArcadeView(), "View", Pages.GAMES, "fb"));
            break;
        case MAIN:
            topBits.add(Link.create(_msgs.editArcadeView(), "View", Pages.GAMES));
            break;
        }

        _pages.addChangeHandler(new ChangeHandler() {
            @Override public void onChange (ChangeEvent event) {
                setPage(ArcadeData.Portal.values()[_pages.getSelectedIndex()]);
            }
        });

        if (portal == null) {
            return;
        }

        add(new TongueBox(portal == ArcadeData.Portal.FACEBOOK ? _msgs.editArcadeApprovedGames() :
            _msgs.editArcadeFeaturedGames(), new TopGamesPanel(portal)));
    }

    /**
     * Panel for editing the top games in the arcade.
     */
    protected static class TopGamesPanel extends GameListPanel
    {
        public TopGamesPanel (ArcadeData.Portal portal)
        {
            _portal = portal;
            _gamesvc.loadTopGames(portal, new InfoCallback<TopGamesResult>() {
                @Override public void onSuccess (TopGamesResult result) {
                    _result = result;
                    init();
                }
            });
        }

        protected void init ()
        {
            _topGames = new ArrayList<GameInfo>(_result.topGames.length);
            for (GameInfo topGame : _result.topGames) {
                _topGames.add(topGame);
            }
            _featured = new HashSet<Integer>();
            for (int gameId : _result.featured) {
                _featured.add(gameId);
            }

            switch (_portal) {
            case FACEBOOK:
                add(MsoyUI.createLabel(_msgs.etgFacebookTip(), "Tip"));
                break;
            case MAIN:
                add(MsoyUI.createLabel(_msgs.etgMainTip(), "Tip"));
                break;
            }
            add(_grid = new GameGrid(_topGames));
            HorizontalPanel buttons = new HorizontalPanel();
            buttons.setSpacing(10);
            buttons.setStyleName("Buttons");
            buttons.add(_save = new Button(_msgs.etgSave()));
            buttons.add(Link.create(_msgs.etgAdd(), Pages.GAMES, "at", _portal.toByte()));
            add(buttons);
            new ClickCallback<Void>(_save) {
                @Override protected boolean callService () {
                    return saveChanges(this);
                }

                @Override
                protected boolean gotResult (Void result) {
                    MsoyUI.info(_msgs.etgSaved());
                    return true;
                }
            };
        }

        protected boolean saveChanges (AsyncCallback<Void> callback)
        {
            if (!_orderChanged && !_featuredChanged && _removed.size() == 0) {
                MsoyUI.info(_msgs.etgNoChanges());
                return false;
            }

            List<Integer> gameIds = null;
            if (_orderChanged) {
                gameIds = new ArrayList<Integer>(_topGames.size());
                for (GameInfo ginf : _topGames) {
                    gameIds.add(ginf.gameId);
                }
            }
            Set<Integer> featured = _featuredChanged ? _featured : null;
            Set<Integer> removed = _removed.size() > 0 ? _removed : null;
            _gamesvc.updateTopGames(_portal, gameIds, featured, removed, callback);
            return true;
        }

        protected int getIndex (int gameId)
        {
            for (int ii = 0, ll = _topGames.size(); ii < ll; ++ii) {
                if (_topGames.get(ii).gameId == gameId) {
                    return ii;
                }
            }
            return -1;
        }

        @Override
        protected Widget createActionWidget (GameInfo game)
        {
            final int index = getIndex(game.gameId);
            final int gameId = game.gameId;
            HorizontalPanel moveButtons = new HorizontalPanel();
            moveButtons.setStyleName("MoveButtons");
            moveButtons.add(MsoyUI.createImageButton("moveUp", new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    moveGame(index, -1);
                }
            }));
            moveButtons.add(MsoyUI.createImageButton("moveDown", new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    moveGame(index, 1);
                }
            }));
            moveButtons.add(MsoyUI.createCloseButton(new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    removeGame(index);
                }
            }));
            FlowPanel buttons = new FlowPanel();
            buttons.add(moveButtons);
            if (_portal == ArcadeData.Portal.FACEBOOK) {
                CheckBox feat = new CheckBox(_msgs.etgFeatured());
                feat.setValue(_featured.contains(gameId));
                feat.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    public void onValueChange (ValueChangeEvent<Boolean> event) {
                        if (event.getValue()) {
                            _featured.add(gameId);
                        } else {
                            _featured.remove(gameId);
                        }
                        _featuredChanged = true;
                    }
                });
                buttons.add(feat);
            }
            return buttons;
        }

        @Override
        protected int addCustomControls (FlexTable controls, int row)
        {
            return super.addCustomControls(controls, ++row);
        }

        @Override
        protected String getEmptyMessage ()
        {
            return _msgs.etgNoGames();
        }

        protected void moveGame (int index, int dir)
        {
            int nindex = index + dir;
            if (nindex < 0 || nindex >= _topGames.size()) {
                return;
            }
            GameInfo tmp = _topGames.get(index);
            _topGames.set(index, _topGames.get(nindex));
            _topGames.set(nindex, tmp);
            _orderChanged = true;
            refreshGrid();
        }

        protected void removeGame (int index)
        {
            _removed.add(_topGames.get(index).gameId);
            _topGames.remove(index);
            refreshGrid();
        }

        protected void refreshGrid ()
        {
            if (_grid.getOffset() >= _topGames.size()) {
                _grid.displayPage(0, true);
            } else {
                _grid.displayPage(_grid.getPage(), true);
            }
        }

        protected ArcadeData.Portal _portal;
        protected TopGamesResult _result;
        protected List<GameInfo> _topGames;
        protected Set<Integer> _featured;
        protected GameGrid _grid;
        protected Button _save;
        protected HashSet<Integer> _removed = new HashSet<Integer>();
        protected boolean _orderChanged;
        protected boolean _featuredChanged;
    }

    ListBox _pages;

    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
