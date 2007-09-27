//
// $Id: MyWhirled.java 5569 2007-08-21 20:44:02Z nathan $

package client.whirled;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.History;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.RootPanel;
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
import client.shell.WorldClient;

import client.util.MediaUtil;
import client.util.MsoyUI;

public class Whirledwide extends FlexTable
{
    public Whirledwide (final PopulationDisplay popDisplay)
    {
        setStyleName("WhirledwidePage");
        buildUi();

        CWhirled.membersvc.getWhirledwide(new AsyncCallback() {
            public void onSuccess (Object result) {
                WhirledwideData data = (WhirledwideData) result;
                popDisplay.displayPopulation(data.whirledPopulation);
                fillUi(data);
            }
            public void onFailure (Throwable caught) {
                MsoyUI.error(CWhirled.serverError(caught));
            }
        });
    }

    protected void buildUi ()
    {
        int row = 0;

        setCellPadding(0);
        setCellSpacing(5);

        getFlexCellFormatter().setRowSpan(row, 0, TOTAL_ROWS);
        VerticalPanel topGamesContainer = new VerticalPanel();
        setWidget(row, 0, topGamesContainer);
        topGamesContainer.setStyleName("TopGamesContainer");
        HorizontalPanel header = new HorizontalPanel();
        header.setStyleName("Header");
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        Label star = new Label();
        star.setStyleName("HeaderLeft");
        header.add(star);
        Label title = new Label(CWhirled.msgs.headerTopGames());
        title.setStyleName("HeaderCenter");
        header.add(title);
        header.setCellWidth(title, "100%");
        star = new Label();
        star.setStyleName("HeaderRight");
        header.add(star);
        topGamesContainer.add(header);
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
                History.newItem(Application.createLinkToken("catalog", "" + Item.GAME));
            }
        });
        allGames.add(allGamesImage);
        topGamesContainer.add(allGames);


        getFlexCellFormatter().setRowSpan(row, 2, TOTAL_ROWS);
        VerticalPanel playersContainer = new VerticalPanel();
        setWidget(row, 2, playersContainer);
        playersContainer.setStyleName("PlayersContainer");
        header = new HorizontalPanel();
        header.setStyleName("Header");
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        star = new Label();
        star.setStyleName("HeaderLeft");
        header.add(star);
        title = new Label(CWhirled.msgs.headerPlayers());
        title.setStyleName("HeaderCenter");
        header.add(title);
        header.setCellWidth(title, "100%");
        star = new Label();
        star.setStyleName("HeaderRight");
        header.add(star);
        playersContainer.add(header);
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

        VerticalPanel newsBox = new VerticalPanel();
        getFlexCellFormatter().setAlignment(row, 0, HorizontalPanel.ALIGN_CENTER,
                                                    HorizontalPanel.ALIGN_TOP);
        setWidget(row++, 0, newsBox);
        newsBox.setStyleName("NewsBox");
        header = new HorizontalPanel();
        header.setStyleName("Header");
        header.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        star = new Label();
        star.setStyleName("HeaderLeft");
        header.add(star);
        title = new Label(CWhirled.msgs.headerNews());
        title.setStyleName("HeaderCenter");
        header.add(title);
        header.setCellWidth(title, "100%");
        star = new Label();
        star.setStyleName("HeaderRight");
        header.add(star);
        newsBox.add(header);
        newsBox.add(_news = new ScrollPanel());
        _news.setStyleName("NewsContainer");
        _news.setAlwaysShowScrollBars(true);
        DOM.setStyleAttribute(_news.getElement(), "overflowX", "hidden");
    }

    protected void fillUi (WhirledwideData whirledwide) 
    {
        _featuredPlace.setSceneList(whirledwide.places);
        _news.setWidget(new HTML(whirledwide.newsHtml));

        // games
        Iterator gamesIter = whirledwide.games.iterator();
        if (gamesIter.hasNext()) {
            VerticalPanel topGame = new VerticalPanel();
            topGame.setStyleName("TopGame");
            topGame.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
            topGame.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
            addGameDataTo(topGame, (SceneCard) gamesIter.next());
            _topGames.add(topGame);
        }
        while(gamesIter.hasNext()) {
            HorizontalPanel game = new HorizontalPanel();
            game.setStyleName("GameWidget");
            game.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            game.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
            addGameDataTo(game, (SceneCard) gamesIter.next());
            _topGames.add(game);
        }

        // people
        Iterator peopleIter = whirledwide.people.iterator();
        while (peopleIter.hasNext()) {
            final MemberCard person = (MemberCard) peopleIter.next();
            VerticalPanel personPanel = new VerticalPanel();
            personPanel.setStyleName("PlayerWidget");
            personPanel.setVerticalAlignment(VerticalPanel.ALIGN_MIDDLE);
            personPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
            
            ClickListener goToProfile = new ClickListener() {
                public void onClick (Widget sender) {
                    History.newItem(
                        Application.createLinkToken("profile", "" + person.name.getMemberId()));
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
    }

    protected void addGameDataTo (CellPanel panel, final SceneCard game) 
    {
        ClickListener goToGame = new ClickListener() {
            public void onClick (Widget sender) {
                History.newItem(Application.createLinkToken("game", "" + game.sceneId));
            }
        };

        MediaDesc logoMedia = game.logo != null ? game.logo :
            Item.getDefaultThumbnailMediaFor(Item.GAME);
        Widget logo;
        if (panel instanceof VerticalPanel) {
            logo = MediaUtil.createMediaView(logoMedia, MediaDesc.HALF_THUMBNAIL_SIZE);
            panel.add(logo);
        } else {
            HorizontalPanel logoContainer = new HorizontalPanel();
            logoContainer.setStyleName("LogoContainer");
            logoContainer.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
            logoContainer.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
            logo = MediaUtil.createMediaView(logoMedia,
                // THUMBNAIL is too big, HALF_THUMBNAIL too small - do something custom
                (int) (MediaDesc.THUMBNAIL_WIDTH * 0.3), (int) (MediaDesc.THUMBNAIL_HEIGHT * 0.3));
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
            setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
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
                    History.newItem(Application.createLinkToken("world", "s" + card.sceneId));
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

    protected static final int TOTAL_ROWS = 2;

    protected VerticalPanel _topGames;
    protected VerticalPanel _players;
    protected FeaturedPlaceView _featuredPlace;
    protected ScrollPanel _news;
}
