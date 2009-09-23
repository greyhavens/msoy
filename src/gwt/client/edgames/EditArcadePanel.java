//
// $Id$

package client.edgames;

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

import com.threerings.msoy.edgame.gwt.EditGameService;
import com.threerings.msoy.edgame.gwt.EditGameServiceAsync;
import com.threerings.msoy.edgame.gwt.EditGameService.ArcadeEntriesResult;

import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameInfo;

import com.threerings.msoy.web.gwt.Pages;

import client.game.GameListPanel;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.util.Link;

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

        _pages.addChangeHandler(new ChangeHandler() {
            @Override public void onChange (ChangeEvent event) {
                setPage(ArcadeData.Portal.values()[_pages.getSelectedIndex()]);
            }
        });

        if (portal == ArcadeData.Portal.FACEBOOK) {
            topBits.add(WidgetUtil.makeShim(5, 5));
            topBits.add(Link.create("Edit Mochi Games", Pages.GAMES, "emg"));
        }

        if (portal == null) {
            return;
        }

        add(new TongueBox(portal.isFiltered() ? _msgs.editArcadeApprovedGames() :
            _msgs.editArcadeFeaturedGames(), new ArcadeEntriesPanel(portal)));
    }

    /**
     * Panel for editing the entries in the arcade.
     */
    protected static class ArcadeEntriesPanel extends GameListPanel
    {
        public ArcadeEntriesPanel (ArcadeData.Portal portal)
        {
            _portal = portal;
            _gamesvc.loadArcadeEntries(portal, new InfoCallback<ArcadeEntriesResult>() {
                @Override public void onSuccess (ArcadeEntriesResult result) {
                    _result = result;
                    init();
                }
            });
        }

        protected void init ()
        {
            _entries = new ArrayList<GameInfo>(_result.entries);
            _featured = new HashSet<Integer>(_result.featured);

            switch (_portal) {
            case FACEBOOK:
                add(MsoyUI.createLabel(_msgs.eaeFacebookTip(), "Tip"));
                break;
            case MAIN:
                add(MsoyUI.createLabel(_msgs.eaeMainTip(), "Tip"));
                break;
            }
            add(_grid = new GameGrid(_entries));
            HorizontalPanel buttons = new HorizontalPanel();
            buttons.setSpacing(10);
            buttons.setStyleName("Buttons");
            buttons.add(_save = new Button(_msgs.eaeSave()));
            buttons.add(Link.create(_msgs.eaeAdd(), Pages.GAMES, "aa", _portal.toByte()));
            add(buttons);
            new ClickCallback<Void>(_save) {
                @Override protected boolean callService () {
                    return saveChanges(this);
                }

                @Override
                protected boolean gotResult (Void result) {
                    MsoyUI.info(_msgs.eaeSaved());
                    return true;
                }
            };
        }

        protected boolean saveChanges (AsyncCallback<Void> callback)
        {
            if (!_orderChanged && !_featuredChanged && _removed.size() == 0) {
                MsoyUI.info(_msgs.eaeNoChanges());
                return false;
            }

            List<Integer> gameIds = null;
            if (_orderChanged) {
                gameIds = new ArrayList<Integer>(_entries.size());
                for (GameInfo ginf : _entries) {
                    gameIds.add(ginf.gameId);
                }
            }
            Set<Integer> featured = _featuredChanged ? _featured : null;
            Set<Integer> removed = _removed.size() > 0 ? _removed : null;
            _gamesvc.updateArcadeEntries(_portal, gameIds, featured, removed, callback);
            return true;
        }

        protected int getIndex (int gameId)
        {
            for (int ii = 0, ll = _entries.size(); ii < ll; ++ii) {
                if (_entries.get(ii).gameId == gameId) {
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
            // up/down are only relevant if the featured games are from whirled
            if (_portal.featuresWhirledGames()) {
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
            }
            moveButtons.add(MsoyUI.createCloseButton(new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    removeGame(index);
                }
            }));
            FlowPanel buttons = new FlowPanel();
            buttons.add(moveButtons);
            // FACEBOOK features mochi games so *no* whirled games are featured
            // MAIN is unfiltered so the set itself represents *only* featured games...
            // hence we don't need a featured tick box anymore. leaving the code in case we ditch
            // mochi, however unlikely that may be
            if (_portal.featuresWhirledGames() && _portal.isFiltered()) {
                CheckBox feat = new CheckBox(_msgs.eaeFeatured());
                feat.setValue(_featured.contains(gameId));
                feat.addValueChangeHandler(new ValueChangeHandler<Boolean>() {
                    public void onValueChange (ValueChangeEvent<Boolean> event) {
                        if (event.getValue()) {
                            _featured.add(gameId);
                            // TODO: make a whole separate UI for editing the facebook featured
                            // games... for now HACK reorder them to the top of the list
                            _entries.add(0, _entries.remove(getIndex(gameId)));
                            _orderChanged = true;
                            refreshGrid();

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
            return _msgs.eaeNoGames();
        }

        protected void moveGame (int index, int dir)
        {
            int nindex = index + dir;
            if (nindex < 0 || nindex >= _entries.size()) {
                return;
            }
            GameInfo tmp = _entries.get(index);
            _entries.set(index, _entries.get(nindex));
            _entries.set(nindex, tmp);
            _orderChanged = true;
            refreshGrid();
        }

        protected void removeGame (int index)
        {
            _removed.add(_entries.get(index).gameId);
            _entries.remove(index);
            refreshGrid();
        }

        protected void refreshGrid ()
        {
            if (_grid.getOffset() >= _entries.size()) {
                _grid.displayPage(0, true);
            } else {
                _grid.displayPage(_grid.getPage(), true);
            }
        }

        protected ArcadeData.Portal _portal;
        protected ArcadeEntriesResult _result;
        protected List<GameInfo> _entries;
        protected Set<Integer> _featured;
        protected GameGrid _grid;
        protected Button _save;
        protected HashSet<Integer> _removed = new HashSet<Integer>();
        protected boolean _orderChanged;
        protected boolean _featuredChanged;
    }

    protected ListBox _pages;

    protected static final EditGameServiceAsync _gamesvc = GWT.create(EditGameService.class);
    protected static final EditGamesMessages _msgs = GWT.create(EditGamesMessages.class);
}
