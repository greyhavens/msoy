//
// $Id$

package client.stuff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import client.editem.EditorHost;
import client.editem.ItemEditor;
import client.remix.ItemRemixer;
import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.MsoyUI;

/**
 * Handles the MetaSOY inventory application.
 */
public class index extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override from Page
    public void onHistoryChanged (Args args)
    {
        if (CStuff.ident == null) {
            // if we have no creds, just display a message saying login
            setTitle(null);
            setContent(MsoyUI.createLabel(CStuff.msgs.logon(), "infoLabel"));
            _inventory = null;
            return;
        }

        String arg0 = args.get(0, "");

        // if we're editing an item, display that interface
        if ("e".equals(arg0) || "c".equals(arg0)) {
            byte type = (byte)args.get(1, Item.AVATAR);
            ItemEditor editor = ItemEditor.createItemEditor(type, createEditorHost());
            if ("e".equals(arg0)) {
                int itemId = args.get(2, 0);
                setTitle(CStuff.msgs.editItemTitle());
                Item item = _models.findItem(type, itemId);
                if (item == null) {
                    editor.setItem(type, itemId);
                } else {
                    editor.setItem(item);
                }
            } else {
                setTitle(CStuff.msgs.createItemTitle());
                editor.setItem(editor.createBlankItem());
                byte ptype = (byte)args.get(2, 0);
                if (ptype != 0) {
                    editor.setParentItem(new ItemIdent(ptype, args.get(3, 0)));
                }
            }
            setContent(editor);
            return;

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
            setTitle(CStuff.msgs.remixItemTitle());
            setContent(remixer);
            return;
        }

        // otherwise we're viewing our inventory
        displayInventory((byte)args.get(0, Item.AVATAR), args.get(1, -1), args.get(2, 0));
    }

    protected EditorHost createEditorHost ()
    {
        return new EditorHost() {
            public void editComplete (Item item) {
                if (item != null) {
                    _models.updateItem(item);
                    CStuff.viewParent(item);
                } else {
                    History.back();
                }
            }
        };
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return STUFF;
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CStuff.msgs = (InventoryMessages)GWT.create(InventoryMessages.class);
    }

    // @Override // from Page
    protected void didLogoff ()
    {
        // go to the landing page instead of reloading as a non-member
        Application.go(Page.ME, "");
    }

    protected void displayInventory (byte type, int pageNo, int itemId)
    {
        setTitle(null);
        setContent(_inventory);
        setPageTabs(_inventory.getTabs());
        _inventory.display(type, pageNo, itemId);
    }

    protected void setTitle (String subtitle)
    {
        Frame.setTitle(CStuff.msgs.inventoryTitle(), subtitle);
    }

    protected InventoryModels _models = new InventoryModels();
    protected InventoryPanel _inventory = new InventoryPanel(_models);
}
