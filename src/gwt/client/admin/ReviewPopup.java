package client.admin;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListener;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemDetail;
import com.threerings.msoy.item.web.MediaDesc;

import client.util.BorderedDialog;
import client.util.BorderedPopup;
import client.util.ClickCallback;
import client.util.MediaUtil;

public class ReviewPopup extends BorderedDialog
{
    public ReviewPopup (AdminContext ctx)
    {
        _ctx = ctx;
        fetchFromServer();
        _status = new Label();
        _dock.add(_status, DockPanel.SOUTH);
    }

    protected void fetchFromServer ()
    {
        _ctx.itemsvc.getFlaggedItems(_ctx.creds, 10, new AsyncCallback() {
            public void onSuccess (Object result) {
                populateUI((List) result);
            }
            public void onFailure (Throwable caught) {
                _status.setText("Error fetching items: " + caught.getMessage());
            }
        });
    }

    protected void populateUI (List list)
    {
        if (list.size() == 0) {
            _dock.add(new Label(_ctx.msgs.reviewNoItems()), DockPanel.CENTER);
            return;
        }
        FlexTable panel = new FlexTable();
        int row = 0;
        Iterator i = list.iterator();
        while (i.hasNext()) {
            final ItemDetail itemDetail = (ItemDetail) i.next();
            final Item item = itemDetail.item;
            
            // thumbnail to the left
            Widget thumb = MediaUtil.createMediaView(
                item.getThumbnailMedia(), MediaDesc.THUMBNAIL_SIZE);
            panel.setWidget(row, 0, thumb);

            // bits to the right
            VerticalPanel itemData = new VerticalPanel();

            // first a horizontal line with item name & creator nam
            HorizontalPanel line = new HorizontalPanel();
            
            // the name popups an item inspector
            InlineLabel nameLabel = new InlineLabel(item.name);
            nameLabel.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    new AdminItemPopup(_ctx, item, ReviewPopup.this).show();
                }
            });
            line.add(nameLabel);
            line.add(new InlineLabel("&nbsp; - &nbsp; " + itemDetail.creator.toString()));
            itemData.add(line);
            
            itemData.add(new Label(item.description));

            // then a row of action buttons
            line = new HorizontalPanel();
            if (item.ownerId == 0) {
                Button button = new Button("Delist");
                new ClickCallback(_ctx, button, _status) {
                    public boolean callService () {
                        _ctx.catalogsvc.listItem(_ctx.creds, item.getIdent(), false, this);
                        return true;
                    }
                    public boolean gotResult (Object result) {
                        if (result != null) {
                            _status.setText(_ctx.msgs.reviewDelisted());
                            fetchFromServer();
                            return false; // don't reenable delist
                        }
                        _status.setText(_ctx.msgs.errListingNotFound());
                        return true;
                    }
                };
                line.add(button);
            }

            if (item.isSet(Item.FLAG_FLAGGED_MATURE)) {
                Button button = new Button("Mark Mature");
                new ClickCallback(_ctx, button, _status) {
                    public boolean callService () {
                        _ctx.itemsvc.setFlags(_ctx.creds, item.getIdent(), Item.FLAG_MATURE,
                                              Item.FLAG_MATURE, this);
                        return true;
                    }
                    public boolean gotResult (Object result) {
                        _status.setText(_ctx.msgs.reviewMarked());
                        fetchFromServer();
                        return false; // don't reenable button
                    }
                };
                line.add(button);
            }

            Button button = new Button("Delete");
            button.addClickListener(new ClickListener() {
                public void onClick (Widget sender) {
                    new DeleteDialog(item).show();
                }
            });
            line.add(button);
            itemData.add(line);
            
            panel.setWidget(row, 1, itemData);
            row ++;

            _dock.add(panel, DockPanel.CENTER);
        }
    }

    // @Override
    protected Widget createContents ()
    {
        return _dock = new DockPanel();
    }

    protected class DeleteDialog extends BorderedPopup
        implements KeyboardListener
    {
        public DeleteDialog (Item item)
        {
            _item = item;
            VerticalPanel content = new VerticalPanel();
            content.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);

            content.add(new HTML("To delete this item, enter a message that will be sent to the item's creator:"));

            _area = new TextArea();
            _area.setCharacterWidth(60);
            _area.addKeyboardListener(this);
            content.add(_area);
            
            _feedback = new Label();
            content.add(_feedback);

            _yesButton = new Button("Delete");
            final Button noButton = new Button("Cancel");
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

        // from KeyboardListener
        public void onKeyDown (Widget sender, char keyCode, int modifiers) { /* empty*/ }
        // from KeyboardListener
        public void onKeyPress (Widget sender, char keyCode, int modifiers) { /* empty */ }

        // from KeyboardListener
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
            _ctx.itemsvc.deleteItemAdmin(
               _ctx.creds, _item.getIdent(), "Item Deleted",
               "The item \"" + _item.name + "\" has been deleted by the game administrators:\n" +
               _area.getText().trim(), new AsyncCallback() {
                   public void onSuccess (Object result) {
                       _status.setText("Item(s) successfully deleted and mail(s) sent out.");
                       hide();
                   }
                   public void onFailure (Throwable caught) {
                       _feedback.setText("Deletion failed on server: " + caught.getMessage());
                   }
               });
        }
        
        protected Item _item;
        protected TextArea _area;
        protected Label _feedback;
        protected Button _yesButton;
    }

    protected DockPanel _dock;
    protected AdminContext _ctx;
    protected Label _status;
}
