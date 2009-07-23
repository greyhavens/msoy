//
// $Id$

package client.games;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.ServiceUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.Prize;
import com.threerings.msoy.item.data.all.IdentGameItem;
import com.threerings.msoy.item.data.all.TrophySource;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.MsoyUI;
import client.util.InfoCallback;
import client.util.Link;
import client.util.MediaUtil;
import client.shell.DynamicLookup;

/**
 * Displays a list of game subitems (trophy source, level pack, etc.).
 */
public class GameItemEditorPanel extends SmartTable
{
    public GameItemEditorPanel (final int gameId, final byte itemType)
    {
        super("gameItemEditor", 5, 0);

        setWidget(0, 0, MsoyUI.createNowLoading());

        _gamesvc.loadGameItems(gameId, itemType, new InfoCallback<List<Item>>() {
            public void onSuccess (List<Item> items) {
                init(gameId, itemType, items);
            }
        });
    }

    protected void init (int gameId, byte itemType, List<Item> items)
    {
        int row = 0;
        if (items.size() > 0) {
            int col = 0;
            setText(row, col++, ""); // clear out loading...
            setText(row, col++, _msgs.gieName(), 1, "Header");
            col++; // for [view]
            col++; // for [edit]
            setText(row, col++, _msgs.giePublished(), 1, "Header");
            if (items.get(0) instanceof IdentGameItem) {
                setText(row, col++, _msgs.gieIdent(), 1, "Header");
            }
            if (items.get(0) instanceof TrophySource) {
                setText(row, col++, _msgs.gieSecret(), 1, "Header");
            } else if (items.get(0) instanceof LevelPack) {
                setText(row, col++, _msgs.giePremium(), 1, "Header");
            } else if (items.get(0) instanceof Prize) {
                setText(row, col++, _msgs.giePrize(), 1, "Header");
            }
            getRowFormatter().setStyleName(row++, "Row");
        }

        for (Item item : items) {
            int col = 0;
            setWidget(row, col++, MediaUtil.createMediaView(
                          item.getThumbnailMedia(), MediaDesc.HALF_THUMBNAIL_SIZE), 1);
            setText(row, col++, item.name, 1);
            Args dargs = Args.compose("d", item.getType(), item.itemId);
            setWidget(row, col++, Link.create(_msgs.gieView(), Pages.STUFF, dargs), 1);
            Args eargs = Args.compose("e", item.getType(), item.itemId);
            setWidget(row, col++, Link.create(_msgs.gieEdit(), Pages.STUFF, eargs));
            setText(row, col++, item.isListedOriginal() ? _msgs.gieIsPublished() : "");
            if (item instanceof IdentGameItem) {
                setText(row, col++, ((IdentGameItem)item).ident);
            }
            if (item instanceof TrophySource) {
                setText(row, col++, ((TrophySource)item).secret ? _msgs.gieIsSecret() : "");
            } else if (item instanceof LevelPack) {
                setText(row, col++, ((LevelPack)item).premium ? _msgs.gieIsPremium() : "");
            } else if (item instanceof Prize) {
                Prize prize = (Prize)item;
                Args pargs = Args.compose("l", prize.targetType, prize.targetCatalogId);
                setWidget(row, col++, Link.create(_msgs.gieTarget(), Pages.SHOP, pargs));
            }
            getRowFormatter().setStyleName(row, "Row");
            if (row % 2 == 1) {
                getRowFormatter().addStyleName(row, "AltRow");
            }
            row++;
        }

        // add a button for creating these subitems; TODO: fancy this up and add links to the wiki
        HorizontalPanel bits = new HorizontalPanel();
        bits.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
        bits.add(MsoyUI.createHTML(_dmsgs.get("editorWikiLink" + itemType), "Tip"));
        bits.add(WidgetUtil.makeShim(10, 10));
        Args cargs = Args.compose("c", itemType, GameInfo.toDevId(gameId));
        bits.add(MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.gieCreate(),
                                     Link.createHandler(Pages.STUFF, cargs)));
        setWidget(row, 0, bits, getRowCount() == 0 ? 1 : getCellCount(0));
        getFlexCellFormatter().setHorizontalAlignment(row, 0, HasAlignment.ALIGN_CENTER);
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
