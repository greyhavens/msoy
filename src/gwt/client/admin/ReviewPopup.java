//
// $Id$

package client.admin;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.editem.EditorHost;
import client.util.BorderedDialog;
import client.util.BorderedPopup;
import client.util.ClickCallback;
import client.util.MediaUtil;

/**
 * An interface for dealing with flagged items: mark them mature if they were flagged thus,
 * or delete them, or simply remove the flags.
 */
public class ReviewPopup extends BorderedDialog
    implements EditorHost
{
    /**
     * Constructs a new {@link ReviewPopup}.
     */
    public ReviewPopup ()
    {
        super(false, true);

        _dock.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);

        _header.add(createTitleLabel(CAdmin.msgs.reviewTitle(), null));

        HorizontalPanel buttonRow = new HorizontalPanel();
        buttonRow.setSpacing(10);

        Button reloadButton = new Button(CAdmin.msgs.reviewReload());
        reloadButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                refresh();
            }
        });
        buttonRow.add(reloadButton);

        Button dismissButton = new Button(CAdmin.msgs.reviewDismiss());
        dismissButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        });
        buttonRow.add(dismissButton);
        _footer.add(buttonRow);

        _status = new Label();
        _dock.add(_status, DockPanel.SOUTH);
        _centerContent = new FlexTable();
        _dock.add(_centerContent, DockPanel.CENTER);

        refresh();
    }

    // from EditorHost
    public void editComplete (Item item)
    {
        // let's ignore this; I think a refresh() would be annoying
    }

    // from EditorHost
    public void setStatus (String string)
    {
        _status.setText(string);
    }

    // @Override
    protected Widget createContents ()
    {
        return _dock = new DockPanel();
    }

    // clears the UI and repopuplates the list
    protected void refresh ()
    {
        _centerContent.clear();
        CAdmin.itemsvc.getFlaggedItems(CAdmin.ident, 10, new AsyncCallback() {
            public void onSuccess (Object result) {
                populateUI((List) result);
            }
            public void onFailure (Throwable caught) {
                _status.setText(CAdmin.msgs.reviewErrFlaggedItems(caught.getMessage()));
            }
        });
    }

    // builds the UI from the given list
    protected void populateUI (List list)
    {
        _centerContent.clear();
        if (list.size() == 0) {
            _centerContent.setWidget(0, 0, new Label(CAdmin.msgs.reviewNoItems()));
            return;
        }
        int row = 0;
        Iterator i = list.iterator();
        while (i.hasNext()) {
            ItemDetail itemDetail = (ItemDetail) i.next();

            // thumbnail to the left
            _centerContent.setWidget(row, 0, MediaUtil.createMediaView(
                itemDetail.item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE));

            _centerContent.setWidget(row, 1, new ItemBits(itemDetail));
            row ++;
        }
    }

    /**
     * A class one UI row representing one item.
     */
    protected class ItemBits extends VerticalPanel
    {
        /**
         * Build a new {@link ItemBits} object associated with the given {@link ItemDetail}.
         */
        public ItemBits (ItemDetail detail)
        {
            _item = detail.item;

            // first a horizontal line with item name & creator nam
            HorizontalPanel line = new HorizontalPanel();

            // the name popups an item inspector
            InlineLabel nameLabel = new InlineLabel(_item.name);
            nameLabel.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    new AdminItemPopup(_item, ReviewPopup.this).show();
                }
            });
            line.add(nameLabel);
            line.add(new InlineLabel("  - " + detail.creator.toString()));
            add(line);

            add(new Label(_item.description));

            // then a row of action buttons
            line = new HorizontalPanel();

            // TODO: Let's nix 'delist' for a bit and see if we need it later.
//          if (item.ownerId == 0) {
//          Button button = new Button("Delist");
//          new ClickCallback(button) {
//          public boolean callService () {
//          CAdmin.catalogsvc.listItem(CAdmin.ident, item.getIdent(), false, this);
//          return true;
//          }
//          public boolean gotResult (Object result) {
//          if (result != null) {
//          _status.setText(CAdmin.msgs.reviewDelisted());
//          return false; // don't reenable delist
//          }
//          _status.setText(CAdmin.msgs.errListingNotFound());
//          return true;
//          }
//          };
//          line.add(button);
//          }

            // a button to mark someting as mature
            if (_item.isSet(Item.FLAG_FLAGGED_MATURE)) {
                markButton = new Button(CAdmin.msgs.reviewMark());
                new ClickCallback(markButton) {
                    public boolean callService () {
                        _status.setText("");
                        if (_item == null) {
                            // should not happen, but let's be careful
                            return false;
                        }
                        CAdmin.itemsvc.setMature(CAdmin.ident, _item.getIdent(), true, this);
                        return true;
                    }
                    public boolean gotResult (Object result) {
                        _status.setText(CAdmin.msgs.reviewMarked());
                        return false; // don't reenable button
                    }
                };
                line.add(markButton);
            }

            // a button to delete an item and possibly all its clones
            deleteButton = new Button(
                _item.ownerId != 0 ? CAdmin.msgs.reviewDelete() : CAdmin.msgs.reviewDeleteAll());
            deleteButton.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    _status.setText("");
                    if (_item == null) {
                        // should not happen, but let's be careful
                        return;
                    }
                    new DeleteDialog().show();
                }
            });
            line.add(deleteButton);

            // a button to signal we're done
            doneButton = new Button(CAdmin.msgs.reviewDone());
            new ClickCallback(doneButton) {
                public boolean callService () {
                    _status.setText("");
                    if (_item == null) {
                        refresh();
                        return false;
                    }
                    CAdmin.itemsvc.setFlags(
                        CAdmin.ident, _item.getIdent(),
                        (byte) (Item.FLAG_FLAGGED_COPYRIGHT | Item.FLAG_FLAGGED_MATURE),
                        (byte) 0, this);
                    return true;
                }
                public boolean gotResult (Object result) {
                    // the flags are set: refresh the UI
                    refresh();
                    // keep the button disabled until the UI refreshes
                    return false;
                }
            };
            line.add(doneButton);
            add(line);
        }

        /**
         * Handle the deletion message and prompt.
         */
        protected class DeleteDialog extends BorderedPopup
            implements KeyboardListener
        {
            public DeleteDialog ()
            {
                VerticalPanel content = new VerticalPanel();
                content.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);

                content.add(new Label(CAdmin.msgs.reviewDeletionPrompt()));

                _area = new TextArea();
                _area.setCharacterWidth(60);
                _area.setVisibleLines(4);
                _area.addKeyboardListener(this);
                content.add(_area);

                _feedback = new Label();
                content.add(_feedback);

                _yesButton = new Button(CAdmin.msgs.reviewDeletionDo());
                _yesButton.setEnabled(false);
                final Button noButton = new Button(CAdmin.msgs.reviewDeletionDont());
                ClickListener listener = new ClickListener () {
                    public void onClick (Widget sender) {
                        if (sender == _yesButton) {
                            doDelete();
                        }
                        hide();
                    }
                };
                _yesButton.addClickListener(listener);
                noButton.addClickListener(listener);
                HorizontalPanel buttons = new HorizontalPanel();
                buttons.setSpacing(10);
                buttons.add(_yesButton);
                buttons.add(noButton);
                content.add(buttons);

                setWidget(content);
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
                    // you just never know
                    return;
                }
                CAdmin.itemsvc.deleteItemAdmin(
                   CAdmin.ident, _item.getIdent(), CAdmin.msgs.reviewDeletionMailHeader(),
                   CAdmin.msgs.reviewDeletionMailMessage(_item.name, _area.getText().trim()),
                   new AsyncCallback() {
                       public void onSuccess (Object result) {
                           _status.setText(CAdmin.msgs.reviewDeletionSuccess(result.toString()));
                           if (markButton != null) {
                               markButton.setEnabled(false);
                           }
                           deleteButton.setEnabled(false);
                           _item = null;
                           hide();
                       }
                       public void onFailure (Throwable caught) {
                           _feedback.setText(
                               CAdmin.msgs.reviewErrDeletionFailed(caught.getMessage()));
                           if (markButton != null) {
                               markButton.setEnabled(true);
                           }
                           deleteButton.setEnabled(true);
                           hide();
                       }
                   });
            }

            protected TextArea _area;
            protected Label _feedback;
            protected Button _yesButton;
        }

        protected Button markButton, deleteButton, doneButton;
        protected Item _item;
    }

    protected DockPanel _dock;
    protected FlexTable _centerContent;
    protected Label _status;
}
