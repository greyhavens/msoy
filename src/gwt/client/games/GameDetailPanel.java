//
// $Id$

package client.games;

import java.util.Map;

import com.google.common.collect.Maps;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.LazyPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.comment.data.all.CommentType;
import com.threerings.msoy.data.all.RatingResult;
import com.threerings.msoy.game.gwt.GameDetail;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.web.gwt.SharedNaviUtil.GameDetails;

import client.comment.CommentsPanel;
import client.game.GameNamePanel;
import client.game.PlayButton;
import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.NaviTabPanel;
import client.ui.Rating.RateService;
import client.ui.Rating;
import client.ui.ThumbBox;
import client.util.InfoCallback;
import client.util.Link;

/**
 * Displays detail information on a particular game.
 */
public class GameDetailPanel extends SmartTable
{
    public GameDetailPanel ()
    {
        super("gameDetail", 0, 10);
    }

    public void setGame (int gameId, final GameDetails tab)
    {
        if (_gameId == gameId) {
            selectTab(tab);
        } else {
            _gamesvc.loadGameDetail(gameId, new InfoCallback<GameDetail>() {
                public void onSuccess (GameDetail detail) {
                    if (detail == null) {
                        MsoyUI.error(_msgs.gdpNoSuchGame());
                    } else {
                        setGameDetail(detail);
                        selectTab(tab);
                    }
                }
            });
        }
    }

    public void setGameDetail (final GameDetail detail)
    {
        final GameInfo info = detail.info;
        CShell.frame.setTitle(info.name);

        // keep our requested game id around
        _gameId = detail.gameId;

        VerticalPanel shot = new VerticalPanel();
        shot.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        shot.add(new ThumbBox(info.shotMedia, MediaDescSize.GAME_SHOT_SIZE));
        shot.add(WidgetUtil.makeShim(5, 5));
        Rating rating = new Rating(
            info.rating, info.ratingCount, detail.memberRating, false, new RateService() {
                public void handleRate (byte newRating, InfoCallback<RatingResult> callback) {
                    _gamesvc.rateGame(_gameId, newRating, callback);
                }
            }, null);

        shot.add(rating);
        HorizontalPanel mbits = new HorizontalPanel();
        mbits.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        mbits.add(MsoyUI.makeShareButton(
                      Pages.GAMES, Args.compose("d", _gameId), _msgs.gdpGame(),
                      info.name, info.description, info.shotMedia));
        shot.add(mbits);
        setWidget(0, 0, shot);
        getFlexCellFormatter().setRowSpan(0, 0, 2);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        setWidget(0, 1, new GameNamePanel(
                      info.name, info.genre, info.creator, info.description), 2);

        setWidget(1, 0, new GameBitsPanel(detail));

        FlowPanel play = new FlowPanel();
        play.setStyleName("playPanel");
        play.add(PlayButton.createLarge(_gameId));
        if (info.playersOnline > 0) {
            play.add(MsoyUI.createLabel(_msgs.featuredOnline(""+info.playersOnline), "Online"));
        }
        if (!info.integrated) {
            play.add(MsoyUI.createLabel(_msgs.gdpNoCoins(), null));
        }
        setWidget(1, 1, play, 1, "Play");
        getFlexCellFormatter().setHorizontalAlignment(1, 1, HasAlignment.ALIGN_CENTER);

        // add "Discussions" (if appropriate) and "Shop" button
        Widget buttons = null;
        if (info.groupId > 0) {
            ClickHandler onClick = Link.createHandler(Pages.GROUPS, "f", info.groupId);
            buttons = MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.gdpDiscuss(), onClick);
        }
        ClickHandler onShop = Link.createHandler(Pages.SHOP, "g", Math.abs(_gameId));
        PushButton shop = MsoyUI.createButton(MsoyUI.MEDIUM_THIN, _msgs.gdpShop(), onShop);
        buttons = (buttons == null) ? shop : MsoyUI.createButtonPair(buttons, shop);
        setWidget(2, 0, buttons);
        getFlexCellFormatter().setRowSpan(0, 0, 3);
        getFlexCellFormatter().setRowSpan(1, 1, 2);

        setWidget(3, 0, _tabs = new NaviTabPanel(Pages.GAMES) {
            protected Args getTabArgs (int tabIdx) {
                return getTabCode(tabIdx).args(_gameId);
            }
        }, 3);

        // add the about/instructions tab
        addTab(GameDetails.INSTRUCTIONS, _msgs.tabInstructions(), new InstructionsPanel(detail));

        // add comments tab
        addTab(GameDetails.COMMENTS, _msgs.tabComments(), new LazyPanel() {
            protected Widget createWidget () {
                CommentsPanel comments = new CommentsPanel(CommentType.GAME, info.gameId, true);
                comments.expand();
                return comments;
            }
        });

        // add trophies tab, passing in the potentially negative gameId
        addTab(GameDetails.TROPHIES, _msgs.tabTrophies(), new LazyPanel() {
            protected Widget createWidget () {
                return new GameTrophyPanel(_gameId);
            }
        });

        // add top rankings tabs
        if (!CShell.isGuest()) {
            addTab(GameDetails.MYRANKINGS, _msgs.tabMyRankings(), new LazyPanel() {
                protected Widget createWidget () {
                    return new TopRankingPanel(info.gameId, true);
                }
            });
        }
        addTab(GameDetails.TOPRANKINGS, _msgs.tabTopRankings(), new LazyPanel() {
            protected Widget createWidget () {
                return new TopRankingPanel(info.gameId, false);
            }
        });

        // if we're the creator of the game or support, add the metrics and logs tabs
        if (info.isCreator(CShell.getMemberId()) || CShell.isSupport()) {
            // addTab(GameDetails.METRICS, _msgs.tabMetrics(), new LazyPanel() {
            //     protected Widget createWidget () {
            //         return new GameMetricsPanel(detail);
            //     }
            // });
            addTab(GameDetails.LOGS, _msgs.tabLogs(), new LazyPanel() {
                protected Widget createWidget  () {
                    return new GameLogsPanel(info.gameId);
                }
            });
            addTab(GameDetails.DEV_LOGS, _msgs.tabDevLogs(), new LazyPanel() {
                protected Widget createWidget  () {
                    return new GameLogsPanel(GameInfo.toDevId(info.gameId));
                }
            });
        }
    }

    protected void addTab (GameDetails ident, String title, Widget tab)
    {
        _tabs.add(tab, title);
        _tabmap.put(ident, _tabs.getWidgetCount() - 1);
    }

    protected void selectTab (GameDetails tab)
    {
        Integer tosel = _tabmap.get(tab);
        _tabs.activateTab(tosel == null ? 0 : tosel);
    }

    protected GameDetails getTabCode (int tabIndex)
    {
        for (Map.Entry<GameDetails, Integer> entry : _tabmap.entrySet()) {
            if (entry.getValue() == tabIndex) {
                return entry.getKey();
            }
        }
        return null;
    }

    protected NaviTabPanel _tabs;
    protected int _gameId;
    protected Map<GameDetails, Integer> _tabmap = Maps.newHashMap();

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
