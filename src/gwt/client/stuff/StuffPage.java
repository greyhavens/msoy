//
// $Id$

package client.stuff;

import java.util.HashMap;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;

import client.editem.EditorHost;
import client.editem.ItemEditor;
import client.remix.ItemRemixer;
import client.remix.RemixerHost;
import client.shell.Args;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.Page;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Handles the MetaSOY inventory application.
 */
public class StuffPage extends Page
{
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

        // if we're displaying an item's detail, do that
        if ("d".equals(arg0)) {
            byte type = (byte)args.get(1, Item.AVATAR);
            int itemId = args.get(2, 0);

            // otherwise we're display a particular item's details
            ItemIdent ident = new ItemIdent(type, itemId);
            final String title = _msgs.stuffTitle(_dmsgs.xlate("pItemType" + type));
            if (_detail != null && _detail.item.getIdent().equals(ident)) {
                // update the detail with the one in our models
                Item item = _models.findItem(type, itemId);
                if (item != null) {
                    _detail.item = item;
                }
                setContent(title, new ItemDetailPanel(_models, _detail));

            } else {
                _stuffsvc.loadItemDetail(ident, new MsoyCallback<StuffService.DetailOrIdent>() {
                    public void onSuccess (StuffService.DetailOrIdent result) {
                        if (result.detail != null) {
                            _detail = result.detail;
                            _models.itemUpdated(_detail.item);
                            setContent(title, new ItemDetailPanel(_models, _detail));
                        } else {
                            // We didn't have access to that specific item, but have been given
                            // the catalog id for the prototype.
                            ItemIdent id = result.ident;
                            Link.replace(
                                Pages.SHOP, Args.compose("l", "" + id.type, "" + id.itemId));
                        }
                    }
                });
            }

        // if we're editing an item, display that interface
        } else if ("e".equals(arg0) || "c".equals(arg0)) {
            byte type = (byte)args.get(1, Item.AVATAR);
            final ItemEditor editor = ItemEditor.createItemEditor(type, createEditorHost());
            if ("e".equals(arg0)) {
                int itemId = args.get(2, 0);
                getItem(type, itemId, new MsoyCallback<Item>() {
                    public void onSuccess (Item result) {
                        editor.setItem(result);
                    }
                });
            } else {
                editor.setItem(editor.createBlankItem());
                byte ptype = (byte)args.get(2, 0);
                if (ptype != 0) {
                    editor.setParentItem(new ItemIdent(ptype, args.get(3, 0)));
                }
            }
            setContent(editor);

        // or maybe we're remixing an item
        } else if ("r".equals(arg0)) {
            byte type = (byte) args.get(1, Item.AVATAR);
            int itemId = args.get(2, 0);
            final ItemRemixer remixer = new ItemRemixer();
            getItem(type, itemId, new MsoyCallback<Item>() {
                public void onSuccess (Item result) {
                    remixer.init(createRemixHost(), result, 0);
                }
            });
            setContent(remixer);

        } else {
            // otherwise we're viewing our inventory
            byte type = (byte)args.get(0, Item.AVATAR);
            String title = _msgs.stuffTitleMain();
            ItemPanel panel = getItemPanel(type);
            panel.setPage(args.get(1, -1));
            setContent(title, panel);
        }
    }

    protected EditorHost createEditorHost ()
    {
        return new EditorHost() {
            public void editComplete (Item item) {
                if (item != null) {
                    _models.itemUpdated(item);
                    Link.go(Pages.STUFF,
                        Args.compose("d", "" + item.getType(), "" + item.itemId));
                } else {
                    History.back();
                }
            }
        };
    }

    protected RemixerHost createRemixHost ()
    {
        final EditorHost ehost = createEditorHost();
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

    protected void getItem (byte type, int itemId, MsoyCallback<Item> callback)
    {
        Item item = _models.findItem(type, itemId);
        if (item != null) {
            callback.onSuccess(item);
            return;
        }

        // otherwise load it
        _stuffsvc.loadItem(new ItemIdent(type, itemId), callback);
    }

    protected ItemPanel getItemPanel (byte itemType)
    {
        ItemPanel panel = _itemPanels.get(itemType);
        if (panel == null) {
            _itemPanels.put(itemType, panel = new ItemPanel(_models, itemType));
        }
        return panel;
    }

    protected InventoryModels _models = new InventoryModels();
    protected HashMap<Byte, ItemPanel> _itemPanels = new HashMap<Byte, ItemPanel>();
    protected ItemDetail _detail;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final StuffMessages _msgs = GWT.create(StuffMessages.class);
    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
        ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);
}
