//
// $Id: MyWhirled.java 5569 2007-08-21 20:44:02Z nathan $

package client.whirled;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.GroupName;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.web.data.MemberCard;
import com.threerings.msoy.web.data.SceneCard;
import com.threerings.msoy.web.data.WhirledwideData;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.shell.WorldClient;

import client.util.ClickCallback;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RichTextToolbar;

public class Whirledwide extends FlexTable
{
    public Whirledwide (final PopulationDisplay popDisplay)
    {
        setStyleName("WhirledwidePage");
        buildUI();

        CWhirled.worldsvc.getWhirledwide(new MsoyCallback() {
            public void onSuccess (Object result) {
                WhirledwideData data = (WhirledwideData) result;
                popDisplay.displayPopulation(data.whirledPopulation);
                setWhirledData(data);
            }
        });
    }

    protected void buildUI ()
    {
        setCellPadding(0);
        setCellSpacing(5);

        int row = 0;
        VerticalPanel topGamesContainer = new VerticalPanel();
        getFlexCellFormatter().setRowSpan(row, 0, TOTAL_ROWS);
        getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_TOP);
        setWidget(row, 0, topGamesContainer);
        topGamesContainer.setStyleName("TopGamesContainer");
        topGamesContainer.add(makeHeader(CWhirled.msgs.headerTopGames()));

        VerticalPanel topGamesList = new VerticalPanel();
        topGamesList.addStyleName("TopGamesList");
        topGamesList.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        topGamesList.add(_topGames = new VerticalPanel());
        topGamesContainer.add(topGamesList);
        HorizontalPanel allGames = new HorizontalPanel();
        allGames.setStyleName("AllGames");
        allGames.setHorizontalAlignment(HorizontalPanel.ALIGN_RIGHT);
        allGames.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        Image allGamesImage = new Image("/images/whirled/all_games.png");
        allGamesImage.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.CATALOG, "" + Item.GAME);
            }
        });
        allGames.add(allGamesImage);
        topGamesContainer.add(allGames);

        VerticalPanel playersContainer = new VerticalPanel();
        getFlexCellFormatter().setRowSpan(row, 2, TOTAL_ROWS);
        getFlexCellFormatter().setVerticalAlignment(row, 2, HasAlignment.ALIGN_TOP);
        setWidget(row, 2, playersContainer);
        playersContainer.setStyleName("PlayersContainer");
        playersContainer.add(makeHeader(CWhirled.msgs.headerPlayers()));

        VerticalPanel playersList = new VerticalPanel();
        playersList.addStyleName("PlayersList");
        playersList.setVerticalAlignment(VerticalPanel.ALIGN_TOP);
        playersList.add(_players = new VerticalPanel());
        playersContainer.add(playersList);

        HorizontalPanel featuredPlaceContainer = new HorizontalPanel();
        featuredPlaceContainer.setSpacing(0);
        featuredPlaceContainer.setStyleName("FeaturedPlaceContainer");
        featuredPlaceContainer.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
        getFlexCellFormatter().setAlignment(row, 1, HorizontalPanel.ALIGN_CENTER,
                                                    HorizontalPanel.ALIGN_TOP);
        setWidget(row++, 1, featuredPlaceContainer);
        VerticalPanel featuredPlace = new VerticalPanel();
        featuredPlace.setStyleName("FeaturedPlace");
        featuredPlace.setSpacing(0);
        featuredPlaceContainer.add(featuredPlace);
        featuredPlace.add(new Image("/images/whirled/featured_places.jpg"));
        featuredPlace.add(_featuredPlace = new FeaturedPlaceView());

        _newsRow = row;
        getFlexCellFormatter().setAlignment(
            _newsRow, 0, HorizontalPanel.ALIGN_CENTER, HorizontalPanel.ALIGN_TOP);
    }

    protected HorizontalPanel makeHeader (String title)
    {
        HorizontalPanel header = new HorizontalPanel();
        header.setStyleName("Header");
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        header.add(MsoyUI.createLabel("", "HeaderLeft"));
        Label tlabel = MsoyUI.createLabel(title, "HeaderCenter");
        header.add(tlabel);
        header.setCellWidth(tlabel, "100%");
        header.add(MsoyUI.createLabel("", "HeaderRight"));
        return header;
    }

    protected void showNews (String newsHtml)
    {
        VerticalPanel newsBox = new VerticalPanel();
        newsBox.setStyleName("NewsBox");
        newsBox.add(makeHeader(CWhirled.msgs.headerNews()));

        newsBox.add(_news = new ScrollPanel());
        _news.setStyleName("NewsContainer");
        _news.setAlwaysShowScrollBars(true);
        DOM.setStyleAttribute(_news.getElement(), "overflowX", "hidden");
        if (newsHtml != null) {
            _news.setWidget(new HTML(newsHtml));
        }

        if (CWhirled.isAdmin()) {
            newsBox.setHorizontalAlignment(HasAlignment.ALIGN_RIGHT);
            newsBox.add(MsoyUI.createActionLabel("[edit news]", "tipLabel", new ClickListener() {
                public void onClick (Widget sender) {
                    editNews();
                }
            }));
        }

        setWidget(_newsRow, 0, newsBox);
    }

    protected void editNews ()
    {
        if (_wwdata == null) {
            return; // no editing until we have our data
        }

        VerticalPanel newsBox = new VerticalPanel();
        newsBox.setStyleName("NewsBox");
        newsBox.add(makeHeader(CWhirled.msgs.headerNews()));

        final RichTextArea editor = new RichTextArea();
        editor.setWidth("100%");
        editor.setHeight("300px");
        editor.setHTML(_wwdata.newsHtml);

        newsBox.setHorizontalAlignment(HasAlignment.ALIGN_LEFT);
        newsBox.add(new RichTextToolbar(editor));
        newsBox.add(editor);

        newsBox.setHorizontalAlignment(HasAlignment.ALIGN_RIGHT);
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setSpacing(5);
        buttons.add(new Button(CWhirled.cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget source) {
                showNews(_wwdata.newsHtml);
            }
        }));
        Button update = new Button(CWhirled.cmsgs.update());
        buttons.add(update);
        new ClickCallback(update) {
            public boolean callService () {
                _newsHtml = editor.getHTML();
                CWhirled.worldsvc.updateWhirledNews(CWhirled.ident, _newsHtml, this);
                return true;
            }
            public boolean gotResult (Object result) {
                showNews(_newsHtml);
                return false;
            }
            protected String _newsHtml;
        };
        newsBox.add(buttons);

        setWidget(_newsRow, 0, newsBox);
    }

    protected void setWhirledData (WhirledwideData whirledwide)
    {
        _wwdata = whirledwide;

        // display the featured scenes
        _featuredPlace.setSceneList(whirledwide.places);

        // games
        Iterator gamesIter = whirledwide.games.iterator();
        if (gamesIter.hasNext()) {
            VerticalPanel topGame = new VerticalPanel();
            topGame.setStyleName("TopGame");
            topGame.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
            topGame.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
            addGameDataTo(topGame, (SceneCard) gamesIter.next());
            _topGames.add(topGame);
        }
        while(gamesIter.hasNext()) {
            HorizontalPanel game = new HorizontalPanel();
            game.setStyleName("GameWidget");
            game.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
            game.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
            addGameDataTo(game, (SceneCard) gamesIter.next());
            _topGames.add(game);
        }

        // people
        Iterator peopleIter = whirledwide.people.iterator();
        while (peopleIter.hasNext()) {
            final MemberCard person = (MemberCard) peopleIter.next();
            VerticalPanel personPanel = new VerticalPanel();
            personPanel.setStyleName("PlayerWidget");
            personPanel.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
            personPanel.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);

            ClickListener goToProfile = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.PROFILE, "" + person.name.getMemberId());
                }
            };

            Widget logo = MediaUtil.createMediaView(person.photo, 93, 70);
            if (logo instanceof Image) {
                ((Image) logo).addClickListener(goToProfile);
            }
            personPanel.add(logo);

            Label nameLabel = new Label("" + person.name);
            nameLabel.setStyleName("NameLabel");
            nameLabel.addClickListener(goToProfile);
            personPanel.add(nameLabel);
            _players.add(personPanel);
        }

        // update the news
        showNews(whirledwide.newsHtml);
    }

    protected void addGameDataTo (CellPanel panel, final SceneCard game)
    {
        ClickListener goToGame = new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.GAME, Args.compose("d", game.sceneId /* gameId */));
            }
        };

        MediaDesc logoMedia = (game.logo != null) ? game.logo :
            Item.getDefaultThumbnailMediaFor(Item.GAME);
        Widget logo;
        if (panel instanceof VerticalPanel) {
            logo = MediaUtil.createMediaView(logoMedia, MediaDesc.THUMBNAIL_SIZE);
            panel.add(logo);
        } else {
            HorizontalPanel logoContainer = new HorizontalPanel();
            logoContainer.setStyleName("LogoContainer");
            logoContainer.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
            logoContainer.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
            logo = MediaUtil.createMediaView(logoMedia, MediaDesc.HALF_THUMBNAIL_SIZE);
            logoContainer.add(logo);
            panel.add(logoContainer);
        }
        if (logo instanceof Image) {
            ((Image) logo).addClickListener(goToGame);
        }

        Label nameLabel = new Label(game.name);
        nameLabel.setStyleName("NameLabel");
        nameLabel.addClickListener(goToGame);
        panel.add(nameLabel);
    }

    protected static class FeaturedPlaceView extends VerticalPanel
        implements ClickListener
    {
        public FeaturedPlaceView ()
        {
            setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
            add(_featuredPlaceContainer = new HorizontalPanel());
            HorizontalPanel nav = new HorizontalPanel();
            nav.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            nav.setStyleName("SceneNavigation");
            nav.add(_prevButton = new Button(CWhirled.msgs.prev(), this));
            _prevButton.addStyleName("PrevButton");
            nav.add(_sceneNameContainer = new FlowPanel());
            _sceneNameContainer.setStyleName("SceneNameContainer");
            nav.add(_nextButton = new Button(CWhirled.msgs.next(), this));
            _nextButton.addStyleName("NextButton");
            add(nav);
        }

        public void onClick (Widget sender)
        {
            if (_scenes == null) {
                return;
            }

            if (sender == _prevButton) {
                if (_sceneIdx > 0) {
                    displayScene((SceneCard) _scenes.get(--_sceneIdx));
                }
            } else if (sender == _nextButton) {
                if (_scenes.size() > _sceneIdx + 1) {
                    displayScene((SceneCard) _scenes.get(++_sceneIdx));
                }
            }

            _prevButton.setEnabled(_sceneIdx > 0);
            _nextButton.setEnabled(_scenes.size() > _sceneIdx + 1);
        }

        public void setSceneList (List scenes)
        {
            _scenes = scenes;
            _sceneIdx = -1;
            onClick(_nextButton);
        }

        protected void displayScene (final SceneCard card)
        {
            WorldClient.displayFeaturedPlace(card.sceneId, _featuredPlaceContainer);
            _sceneNameContainer.clear();

            String name = card.name.toString();
            if (name.length() > 30) {
                name = name.substring(0, 32) + "...";
            }
            Label sceneName = new Label("\"" + name + "\"");
            sceneName.setStyleName("SceneName");
            sceneName.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.WORLD, "s" + card.sceneId);
                }
            });
            _sceneNameContainer.add(sceneName);
        }

        HorizontalPanel _featuredPlaceContainer;
        FlowPanel _sceneNameContainer;

        Button _prevButton, _nextButton;
        List _scenes;
        int _sceneIdx;
    }

    protected WhirledwideData _wwdata;
    protected int _newsRow;

    protected VerticalPanel _topGames;
    protected VerticalPanel _players;
    protected FeaturedPlaceView _featuredPlace;
    protected ScrollPanel _news;

    protected static final int TOTAL_ROWS = 2;
}
