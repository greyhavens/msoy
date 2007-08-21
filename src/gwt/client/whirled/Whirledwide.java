//
// $Id: MyWhirled.java 5569 2007-08-21 20:44:02Z nathan $

package client.whirled;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

public class Whirledwide extends FlexTable
{
    public Whirledwide ()
    {
        buildUi();
    }

    protected void buildUi ()
    {
        int row = 0;

        setCellPadding(0);
        setCellSpacing(7);
        
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
        topGamesContainer.add(_topGames = new GamesList());

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
        playersContainer.add(_players = new PlayersList());
        
        VerticalPanel featuredPlace = new VerticalPanel();
        setWidget(row++, 1, featuredPlace);
        featuredPlace.add(new Image("/images/whirled/featured_places.jpg"));
        featuredPlace.add(_featuredPlace = new FeaturedPlaceView());
    }

    protected static class GamesList extends VerticalPanel
    {
        public GamesList () 
        {
            setStyleName("TopGamesList");
        }
    }

    protected static class PlayersList extends VerticalPanel
    {
        public PlayersList ()
        {
            setStyleName("PlayersList");
        }
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

    protected GamesList _topGames;
    protected PlayersList _players;
    protected FeaturedPlaceView _featuredPlace;
    protected FeaturedPlacesList _featuredPlaces;
}
