//
// $Id: MyWhirled.java 5569 2007-08-21 20:44:02Z nathan $

package client.whirled;

import java.util.Iterator;

import com.google.gwt.user.client.History;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.web.data.SceneCard;
import com.threerings.msoy.web.data.Whirled;

import client.shell.Application;

import client.util.MediaUtil;

public class Whirledwide extends FlexTable
{
    public Whirledwide ()
    {
        buildUi();

        CWhirled.membersvc.getWhirledwide(new AsyncCallback() {
            public void onSuccess (Object result) {
                fillUi((Whirled) result);
            }
            public void onFailure (Throwable caught) {
                _errorContainer.add(new Label(CWhirled.serverError(caught)));
            }
        });
    }

    protected void buildUi ()
    {
        int row = 0;

        setCellPadding(0);
        setCellSpacing(5);

        getFlexCellFormatter().setColSpan(row, 0, 3);
        setWidget(row++, 0, _errorContainer = new HorizontalPanel());
        
        getFlexCellFormatter().setRowSpan(row, 0, 4);
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
        //topGamesContainer.add(_topGames = new VerticalPanel());
        //_topGames.setStyleName("TopGamesList");
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


        getFlexCellFormatter().setRowSpan(row, 2, 4);
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
        playersContainer.add(_players = new VerticalPanel());
        _players.setStyleName("PlayersList");
        
        VerticalPanel featuredPlace = new VerticalPanel();
        setWidget(row++, 1, featuredPlace);
        featuredPlace.add(new Image("/images/whirled/featured_places.jpg"));
        featuredPlace.add(_featuredPlace = new FeaturedPlaceView());
    }

    protected void fillUi (Whirled whirledwide) 
    {
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

    protected static class FeaturedPlacesList extends HorizontalPanel
    {
        public FeaturedPlacesList ()
        {
        }
    }
    
    protected static class FeaturedPlaceView extends HorizontalPanel
    {
        public FeaturedPlaceView ()
        {
        }
    }

    protected HorizontalPanel _errorContainer;
    protected VerticalPanel _topGames;
    protected VerticalPanel _players;
    protected FeaturedPlaceView _featuredPlace;
    protected FeaturedPlacesList _featuredPlaces;
}
