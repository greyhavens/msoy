//
// $Id$

package client.adminz;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;
import com.threerings.msoy.web.client.Args;
import com.threerings.msoy.web.client.Pages;

import client.ui.BorderedDialog;
import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.util.ClickCallback;
import client.util.ServiceUtil;
import client.util.Link;

/**
 * Displays an item to be reviewed.
 */
public class ReviewItem extends FlowPanel
{
    public ReviewItem (ReviewPanel parent, ItemDetail detail)
    {
        _parent = parent;
        _item = detail.item;

        // say what flags are set on it
        FlowPanel flaggedAs = new FlowPanel();
        flaggedAs.add(new InlineLabel("Flagged as:"));
        if (_item.isFlagSet(Item.FLAG_FLAGGED_MATURE)) {
            flaggedAs.add(new InlineLabel("Mature", false, true, false));
        }
        if (_item.isFlagSet(Item.FLAG_FLAGGED_COPYRIGHT)) {
            flaggedAs.add(new InlineLabel("Copyright Violation", false, true, false));
        }
        add(flaggedAs);

        // the name displays an item inspector
        String name = _item.name + " - " + detail.creator.toString();
        String args = Args.compose("d", ""+_item.getType(), ""+_item.itemId);
        add(Link.create(name, Pages.STUFF, args));

        add(MsoyUI.createLabel(_item.description, null));

        // then a row of action buttons
        RowPanel line = new RowPanel();

//             // TODO: Let's nix 'delist' for a bit and see if we need it later.
//             if (item.ownerId == 0) {
//                 Button button = new Button("Delist");
//                 new ClickCallback<Integer>(button) {
//                     public boolean callService () {
//                         _catalogsvc.listItem(item.getIdent(), false, this);
//                         return true;
//                     }
//                     public boolean gotResult (Integer result) {
//                         if (result != null) {
//                             MsoyUI.info(_msgs.reviewDelisted());
//                             return false; // don't reenable delist
//                         }
//                         MsoyUI.error(_msgs.errListingNotFound());
//                         return true;
//                     }
//                 };
//                 line.add(button);
//             }

        // a button to mark someting as mature
        if (_item.isFlagSet(Item.FLAG_FLAGGED_MATURE)) {
            _mark = new Button(_msgs.reviewMark());
            new ClickCallback<Void>(_mark) {
                public boolean callService () {
                    if (_item == null) {
                        // should not happen, but let's be careful
                        return false;
                    }
                    _itemsvc.setMature(_item.getIdent(), true, this);
                    return true;
                }
                public boolean gotResult (Void result) {
                    MsoyUI.info(_msgs.reviewMarked());
                    return false; // don't reenable button
                }
            };
            line.add(_mark);
        }

        // a button to delete an item and possibly all its clones
        _delete = new Button(_item.ownerId != 0 ?
                             _msgs.reviewDelete() : _msgs.reviewDeleteAll());
        _delete.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                if (_item == null) {
                    // should not happen, but let's be careful
                    return;
                }
                new DeleteDialog().show();
            }
        });
        line.add(_delete);

        // a button to signal we're done
        _done = new Button(_msgs.reviewDone());
        new ClickCallback<Void>(_done) {
            public boolean callService () {
                if (_item == null) {
                    _parent.refresh();
                    return false;
                }
                byte flags = (byte) (Item.FLAG_FLAGGED_COPYRIGHT | Item.FLAG_FLAGGED_MATURE);
                _itemsvc.setFlags(_item.getIdent(), flags, (byte) 0, this);
                return true;
            }
            public boolean gotResult (Void result) {
                // the flags are set: refresh the UI
                _parent.refresh();
                // keep the button disabled until the UI refreshes
                return false;
            }
        };
        line.add(_done);
        add(line);
    }

    /**
     * Handle the deletion message and prompt.
     */
    protected class DeleteDialog extends BorderedDialog
        implements KeyboardListener
    {
        public DeleteDialog ()
        {
            setHeaderTitle(_msgs.reviewDeletionTitle());

            VerticalPanel contents = new VerticalPanel();
            contents.setSpacing(10);
            contents.setWidth("500px");
            contents.add(MsoyUI.createLabel(_msgs.reviewDeletionPrompt(), null));
            contents.add(_area = MsoyUI.createTextArea("", 50, 4));
            _area.addKeyboardListener(this);
            setContents(contents);

            addButton(_yesButton = new Button(_msgs.reviewDeletionDo(), new ClickListener () {
                public void onClick (Widget sender) {
                    doDelete();
                    hide();
                }
            }));
            _yesButton.setEnabled(false);

            addButton(new Button(_msgs.reviewDeletionDont(), new ClickListener () {
                public void onClick (Widget sender) {
                    hide();
                }
            }));

            show();
        }

        public void onKeyDown (Widget sender, char keyCode, int modifiers) { /* empty*/ }
        public void onKeyPress (Widget sender, char keyCode, int modifiers) { /* empty */ }
        public void onKeyUp (Widget sender, char keyCode, int modifiers)
        {
            _yesButton.setEnabled(_area.getText().trim().length() > 0);
        }

        protected void doDelete ()
        {
            if (!_yesButton.isEnabled()) {
                return; // you just never know
            }

            _adminsvc.deleteItemAdmin(
                _item.getIdent(), _msgs.reviewDeletionMailHeader(),
                _msgs.reviewDeletionMailMessage(_item.name, _area.getText().trim()),
                new AsyncCallback<Integer>() {
                    public void onSuccess (Integer result) {
                        MsoyUI.info(_msgs.reviewDeletionSuccess(result.toString()));
                        if (_mark != null) {
                            _mark.setEnabled(false);
                        }
                        _delete.setEnabled(false);
                        _item = null;
                        hide();
                    }
                    public void onFailure (Throwable caught) {
                        MsoyUI.error(_msgs.reviewErrDeletionFailed(caught.getMessage()));
                        if (_mark != null) {
                            _mark.setEnabled(true);
                        }
                        _delete.setEnabled(true);
                        hide();
                    }
                });
        }

        protected TextArea _area;
        protected Button _yesButton;
    }

    protected ReviewPanel _parent;
    protected Item _item;
    protected Button _mark, _delete, _done;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
    protected static final ItemServiceAsync _itemsvc = (ItemServiceAsync)
        ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
}
