//
// $Id$

package client.inventory;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.Item;
import com.threerings.msoy.web.client.WebContext;

/**
 * The base class for an interface for creating and editing digital items.
 *
 * <p> Styles:
 * <ul>
 * <li> item_editor - the style of the main editor
 * <li> item_editor_title - the style of the title label
 * <li> item_editor_submit - the style of the submit button
 * </ul>
 */
public abstract class ItemEditor extends DockPanel
{
    public ItemEditor ()
    {
        setStyleName("item_editor");
        setSpacing(5);
        add(_etitle = new Label("title"), NORTH);
        _etitle.setStyleName("item_editor_title");

        HorizontalPanel bpanel = new HorizontalPanel();
        add(bpanel, SOUTH);
        setCellHorizontalAlignment(bpanel, ALIGN_RIGHT);

        bpanel.add(_esubmit = new Button("submit"));
        _esubmit.setStyleName("item_editor_button");
        _esubmit.setEnabled(false);
        _esubmit.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                commitEdit();
            }
        });

        Button ecancel;
        bpanel.add(ecancel = new Button("Cancel"));
        ecancel.setStyleName("item_editor_button");
        ecancel.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                _parent.editComplete(ItemEditor.this, null);
            }
        });
    }

    /**
     * Configures this editor with a reference to the item service and its item
     * panel parent.
     */
    public void init (WebContext ctx, ItemPanel parent)
    {
        _ctx = ctx;
        _parent = parent;
    }

    /**
     * Configures this editor with an item to edit. The item may be freshly
     * constructed if we are using the editor to create a new item.
     */
    public void setItem (Item item)
    {
        _item = item;
        _etitle.setText((item.itemId <= 0) ? "Create" : "Edit");
        _esubmit.setText((item.itemId <= 0) ? "Create" : "Update");
        updateSubmittable();
    }

    /**
     * Returns the currently configured item.
     */
    public Item getItem ()
    {
        return _item;
    }

    /**
     * Editors should override this method to indicate when the item is in a
     * consistent state and may be uploaded.
     */
    protected boolean itemConsistent ()
    {
        return false;
    }

    /**
     * Editors should call this method when something changes that might render
     * an item consistent or inconsistent. It will update the enabled status of
     * the submit button.
     */
    protected void updateSubmittable ()
    {
        _esubmit.setEnabled(itemConsistent());
    }

    /**
     * Called when the user has clicked the "update" or "create" button to
     * commit their edits or create a new item, respectively.
     */
    protected void commitEdit ()
    {
        _ctx.itemsvc.createItem(_ctx.creds, _item, new AsyncCallback() {
            public void onSuccess (Object result) {
                _parent.setStatus("Item created.");
                _parent.editComplete(ItemEditor.this, _item);
            }
            public void onFailure (Throwable caught) {
                String reason = caught.getMessage();
                _parent.setStatus("Item creation failed: " + reason);
            }
        });
    }

    /**
     * Creates a blank item for use when creating a new item using this editor.
     */
    protected abstract Item createBlankItem ();

    protected WebContext _ctx;
    protected ItemPanel _parent;

    protected Item _item;

    protected Label _etitle;
    protected Button _esubmit;
}
