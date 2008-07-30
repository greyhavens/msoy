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

import client.shell.Args;
import client.shell.DynamicMessages;
import client.shell.Page;

import client.ui.MsoyUI;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Handles the MetaSOY inventory application.
 */
public class StuffPage extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new StuffPage();
            }
        };
    }

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
        if (CStuff.ident == null) {
            // if we have no creds, just display a message saying login
            setContent(MsoyUI.createLabel(CStuff.msgs.logon(), "infoLabel"));
            return;
        }

        String arg0 = args.get(0, "");

        // if we're displaying an item's detail, do that
        if ("d".equals(arg0)) {
            byte type = (byte)args.get(1, Item.AVATAR);
            int itemId = args.get(2, 0);

            // otherwise we're display a particular item's details
            ItemIdent ident = new ItemIdent(type, itemId);

            final String title = CStuff.msgs.stuffTitle(_dmsgs.getString("pItemType" + type));
            if (_detail != null && _detail.item.getIdent().equals(ident)) {
                // update the detail with the one in our models
                Item item = _models.findItem(type, itemId);
                if (item != null) {
                    _detail.item = item;
                }
                setContent(title, new ItemDetailPanel(_models, _detail));

            } else {
                _stuffsvc.loadItemDetail(CStuff.ident, ident,
                    new MsoyCallback<StuffService.DetailOrIdent>() {
                        public void onSuccess (StuffService.DetailOrIdent result) {
                            if (result.detail != null) {
                                _detail = result.detail;
                                _models.updateItem(_detail.item);
                                setContent(title, new ItemDetailPanel(_models, _detail));

                            } else {
                                // We didn't have access to that specific item, but have been given
                                // the catalog id for the prototype.
                                ItemIdent id = result.ident;
                                Link.go(Page.SHOP, Args.compose("l", "" + id.type, "" + id.itemId));
                            }
                        }
                    });
            }

        // if we're editing an item, display that interface
        } else if ("e".equals(arg0) || "c".equals(arg0)) {
            byte type = (byte)args.get(1, Item.AVATAR);
            ItemEditor editor = ItemEditor.createItemEditor(type, createEditorHost());
            if ("e".equals(arg0)) {
                int itemId = args.get(2, 0);
                Item item = _models.findItem(type, itemId);
                if (item == null) {
                    editor.setItem(itemId);
                } else {
                    editor.setItem(item);
                }
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
            ItemRemixer remixer = new ItemRemixer(createEditorHost());
            Item item = _models.findItem(type, itemId);
            if (item != null) {
                remixer.setItem(item);
            } else {
                remixer.setItem(type, itemId);
            }
            setContent(remixer);
            if (args.getArgCount() > 3) {
                remixer.setCatalogInfo(args.get(3, 0), args.get(4, 0), args.get(5, 0));
            }

        } else {
            // otherwise we're viewing our inventory
            byte type = (byte)args.get(0, Item.AVATAR);
            String title = CStuff.msgs.stuffTitle(_dmsgs.getString("pItemType" + type));
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
                    _models.updateItem(item);
                    Link.go(Page.STUFF,
                        Args.compose("d", "" + item.getType(), "" + item.itemId));
                } else {
                    History.back();
                }
            }
        };
    }

    @Override
    public String getPageId ()
    {
        return STUFF;
    }

    @Override // from Page
    protected String getTabPageId ()
    {
        return ME;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CStuff.msgs = (StuffMessages)GWT.create(StuffMessages.class);
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

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
    protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
        ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);
}
