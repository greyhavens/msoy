//
// $Id$

package client.stuff;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;

import com.threerings.msoy.item.data.all.GameItem;
import com.threerings.msoy.item.data.all.IdentGameItem;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.editem.EditorHost;
import client.editem.ItemEditor;
import client.remix.ItemRemixer;
import client.remix.RemixerHost;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.Page;
import client.ui.MsoyUI;
import client.util.InfoCallback;
import client.util.Link;

/**
 * Handles the MetaSOY inventory application.
 */
public class StuffPage extends Page
{
    public static final String DETAIL = "d";
    public static final String EDIT = "e";
    public static final String CREATE = "c";
    public static final String REMIX = "r";
    public static final String LINEUP = "l";

    @Override // from Page
    public void onPageLoad ()
    {
        super.onPageLoad();

        _models.startup();
    }

    @Override // from Page
    public void onPageUnload ()
    {
        _models.shutdown();

        super.onPageUnload();
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        if (CShell.isGuest()) {
            // if we have no creds, just display a message saying logon
            setContent(MsoyUI.createLabel(_msgs.logon(), "infoLabel"));
            return;
        }

        String arg0 = args.get(0, "");
        MsoyItemType type = MsoyItemType.NOT_A_TYPE;
        int memberId = CShell.getMemberId();

        // if we're displaying an item's detail, do that
        if (DETAIL.equals(arg0)) {
            type = args.get(1, MsoyItemType.AVATAR);
            int itemId = args.get(2, 0);

            // otherwise we're display a particular item's details
            ItemIdent ident = new ItemIdent(type, itemId);
            if (_detail != null && _detail.item.getIdent().equals(ident)) {
                // update the detail with the one in our models
                Item item = _models.findItem(type, itemId);
                if (item != null) {
                    _detail.item = item;
                }
                setContent(null, new ItemDetailPanel(_models, _detail));

            } else {
                _stuffsvc.loadItemDetail(ident, new InfoCallback<StuffService.DetailOrIdent>() {
                    public void onSuccess (StuffService.DetailOrIdent result) {
                        if (result.detail != null) {
                            _detail = result.detail;
                            _models.itemUpdated(_detail.item);
                            setContent(null, new ItemDetailPanel(_models, _detail));
                        } else {
                            // We didn't have access to that specific item, but have been given
                            // the catalog id for the prototype.
                            ItemIdent id = result.ident;
                            Link.replace(Pages.SHOP, "l", id.type, id.itemId);
                        }
                    }
                });
            }

        // if we're editing an item, display that interface
        } else if (EDIT.equals(arg0) || CREATE.equals(arg0)) {
            if (!MsoyUI.requireRegistered()) {
                return; // permaguests can't create or edit items
            }
            type = args.get(1, MsoyItemType.AVATAR);
            final ItemEditor editor = ItemEditor.createItemEditor(
                type, createEditorHost(CREATE.equals(arg0)));
            if ("e".equals(arg0)) {
                int itemId = args.get(2, 0);
                getItem(type, itemId, new InfoCallback<Item>() {
                    public void onSuccess (Item result) {
                        editor.setItem(result);
                    }
                });
            } else {
                editor.setItem(editor.createBlankItem());
                editor.setGameId(args.get(2, 0)); // must be called after setItem()
            }
            setContent(editor);

        // or maybe we're remixing an item
        } else if (REMIX.equals(arg0)) {
            type = args.get(1, MsoyItemType.AVATAR);
            int itemId = args.get(2, 0);
            final ItemRemixer remixer = new ItemRemixer();
            getItem(type, itemId, new InfoCallback<Item>() {
                public void onSuccess (Item result) {
                    remixer.init(createRemixHost(), result, 0);
                }
            });
            setContent(remixer);

        // or we're showing a theme's lineup
        } else if (LINEUP.equals(arg0)) {
            ThemeLineupPanel panel = new ThemeLineupPanel(args.get(1, -1));
            panel.setArgs(args.get(2, 0));
            setContent(panel);

        // otherwise we're viewing some player's inventory
        } else {
            type = args.get(0, MsoyItemType.AVATAR);
            memberId = args.get(1, CShell.getMemberId());
            StuffPanel panel = getStuffPanel(memberId, type);
            panel.setArgs(args.get(2, -1), args.get(3, ""));
            setContent(null, panel);
        }
    }

    protected EditorHost createEditorHost (final boolean create)
    {
        return new EditorHost() {
            public void editComplete (Item item) {
                if (item != null) {
                    _models.itemUpdated(item);
                    // if we're editing a listing original, it has no owner, don't do the bulk bit
                    if (create && item.ownerId != 0 && BULK_TYPES.contains(item.getType())) {
                        Link.go(Pages.STUFF, item.getType(), item.ownerId);
                    } else if (GAME_TYPES.containsKey(item.getType())) {
                        int gameId = Math.abs(((IdentGameItem)item).gameId);
                        int tabIdx = GAME_TYPES.get(item.getType());
                        Link.go(Pages.EDGAMES, "e", gameId, tabIdx);
                    } else {
                        Link.go(Pages.STUFF, DETAIL, item.getType(), item.itemId);
                    }
                } else {
                    History.back();
                }
            }
        };
    }

    protected RemixerHost createRemixHost ()
    {
        final EditorHost ehost = createEditorHost(false);
        return new RemixerHost() {
            public void buyItem () {
                // not needed here
            }

            public void remixComplete (Item item) {
                ehost.editComplete(item);
            }
        };
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.STUFF;
    }

    protected void getItem (MsoyItemType type, int itemId, InfoCallback<Item> callback)
    {
        Item item = _models.findItem(type, itemId);
        if (item != null) {
            callback.onSuccess(item);
            return;
        }

        // otherwise load it
        _stuffsvc.loadItem(new ItemIdent(type, itemId), callback);
    }

    /**
     * Return the StuffPanel for this member+itemType. Only store one StuffPanel per itemType.
     */
    protected StuffPanel getStuffPanel (int memberId, MsoyItemType itemType)
    {
        StuffPanel panel = _stuffPanels.get(itemType);
        if (panel == null || panel.getMemberId() != memberId) {
            _stuffPanels.put(itemType, panel = new StuffPanel(_models, memberId, itemType));
        }
        return panel;
    }

    protected InventoryModels _models = new InventoryModels();
    protected Map<MsoyItemType, StuffPanel> _stuffPanels = Maps.newHashMap();
    protected ItemDetail _detail;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final StuffMessages _msgs = GWT.create(StuffMessages.class);
    protected static final StuffServiceAsync _stuffsvc = GWT.create(StuffService.class);

    /** Denotes item types that might be uploaded in bulk. */
    protected static final Set<MsoyItemType> BULK_TYPES = Sets.newHashSet();
    static {
        BULK_TYPES.add(MsoyItemType.PHOTO);
        BULK_TYPES.add(MsoyItemType.DOCUMENT);
        BULK_TYPES.add(MsoyItemType.AUDIO);
        BULK_TYPES.add(MsoyItemType.VIDEO);
    }

    /** The number of tabs on the edit game page before we get to the subitems. */
    protected static final int PRE_ITEM_TABS = 4;

    /** A mapping from game sub-item type to edit game page tab index. */
    protected static final Map<MsoyItemType, Integer> GAME_TYPES = Maps.newHashMap();

    static {
        int idx = PRE_ITEM_TABS;
        for (MsoyItemType type : GameItem.TYPES) {
            GAME_TYPES.put(type, idx++);
        }
    }
}
