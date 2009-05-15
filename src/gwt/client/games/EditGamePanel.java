//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.LazyPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.NaviTabPanel;
import client.util.InfoCallback;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays an interface for editing a game.
 */
public class EditGamePanel extends FlowPanel
{
    public EditGamePanel ()
    {
        setStyleName("editGame");
        add(MsoyUI.createNowLoading());
    }

    public void setGame (int gameId, final int tabIdx)
    {
        if (_gameId == gameId) {
            _tabs.activateTab(tabIdx);
            return;
        }

        _gameId = gameId;
        _gamesvc.loadGameData(gameId, new InfoCallback<GameService.GameData>() {
            public void onSuccess (GameService.GameData data) {
                init(data);
                _tabs.activateTab(tabIdx);
            }
        });
    }

    protected void init (final GameService.GameData data)
    {
        clear();

        SmartTable header = new SmartTable("Header", 0, 10);
        header.setText(0, 0, data.info.name, 1, "Title");
        header.setWidget(0, 1, MsoyUI.createHTML(_msgs.egTip(), null), 1, "Tip");
        add(header);

        // add our giant tab list of doom
        add(_tabs = new NaviTabPanel(Pages.GAMES) {
            protected String getTabArgs (int tabIdx) {
                return Args.compose("e", _gameId, tabIdx);
            }
        });

        addTab(_msgs.egTabInfo(), new LazyPanel() {
            protected Widget createWidget () {
                return new InfoEditorPanel(data.info);
            }
        });
        addTab(_msgs.egTabCode(), new LazyPanel() {
            protected Widget createWidget () {
                return new CodeEditorPanel(data.devCode);
            }
        });
        addTab(_msgs.egTabPublish(), new LazyPanel() {
            protected Widget createWidget () {
                return new PublishPanel(data);
            }
            @Override public void setVisible (boolean visible) {
                if (!visible) {
                    setWidget(null); // clear out our panel when we change tabs
                }
                super.setVisible(visible);
            }
        });
        for (final byte type : SUBITEM_TYPES) {
            addTab(_dmsgs.get("pItemType" + type), new LazyPanel() {
                protected Widget createWidget () {
                    return new GameItemEditorPanel(data.info.gameId, type);
                }
            });
        }
    }

    protected void addTab (String label, LazyPanel panel)
    {
        _tabs.add(panel, label);
    }

    protected int _gameId;
    protected NaviTabPanel _tabs;

    protected static final byte[] SUBITEM_TYPES = {
        Item.TROPHY_SOURCE, Item.ITEM_PACK, Item.LEVEL_PACK, Item.PRIZE };

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);

    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
