//
// $Id$

package client.msgs;

import java.util.List;

import client.shell.CShell;
import client.util.BorderedWidget;
import client.util.ItemThumbnail;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.data.ItemGiftObject;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.web.data.MemberName;

public abstract class ItemGift
{
    public static final class Composer
        implements MailPayloadComposer
    {
        // from MailPayloadComposer
        public MailPayload getComposedPayload ()
        {
            return new ItemGiftObject(_item.getIdent());
        }

        // from MailPayloadComposer
        public Widget widgetForComposition ()
        {
            return new CompositionWidget();
        }

        // from MailPayloadComposer
        public String okToSend ()
        {
            if (_item == null) {
                return "Please select an item before sending your message.";
            }
            return null;
        }

        // from MailPayloadComposer
        public void messageSent (MemberName recipient)
        {
            // TODO
        }

        protected class CompositionWidget extends BorderedWidget
        {
            public CompositionWidget ()
            {
                super();

                DockPanel panel = new DockPanel();
                panel.setStyleName("itemGift");

                _status = new Label();
                panel.add(_status, DockPanel.SOUTH);

                _title = new Label("Please choose the item to send.");
                _title.addStyleName("Title");
                panel.add(_title, DockPanel.NORTH);

                _right = new SimplePanel();
                panel.add(_right, DockPanel.CENTER);
                panel.setCellWidth(_right, "75%");

                _left = new SimplePanel();
                panel.add(_left, DockPanel.WEST);
                panel.setCellWidth(_left, "25%");

                _imageChooser = new AsyncCallback() {
                    public void onSuccess (Object result) {
                        _item = (Item) result;
                        _title.setText("The item you're sending:");
                        VerticalPanel selectedBits = new VerticalPanel();
                        selectedBits.add(new Image(_item.getThumbnailPath()));
                        Button backButton = new Button("Choose Another");
                        backButton.addClickListener(new ClickListener() {
                            public void onClick (Widget sender) {
                                _item = null;
                                leftBits();
                                rightBits(Item.PHOTO);
                            }
                        });
                        selectedBits.add(backButton);
                        _left.setWidget(selectedBits);
                        _right.clear();
                    }
                    public void onFailure (Throwable caught) {
                        // not used
                    }
                };

                leftBits();
                rightBits(Item.PHOTO);

                setWidget(panel);
            }
            
            protected void leftBits ()
            {
                final ListBox box = new ListBox();
                final byte[] types = new byte[] {
                    Item.PHOTO, Item.DOCUMENT, Item.FURNITURE, Item.GAME, Item.AVATAR,
                    Item.PET, Item.AUDIO, Item.VIDEO
                };
                for (int i = 0; i < types.length; i ++) {
                    box.addItem(capitalizeString(Item.getTypeName(types[i])));
                }
                box.setSelectedIndex(0);
                box.addChangeListener(new ChangeListener() {
                    public void onChange (Widget sender) {
                        _right.clear();
                        _status.setText(null);
                        if (box.getSelectedIndex() != -1) {
                            rightBits(types[box.getSelectedIndex()]);
                        }
                    }
                });
                _left.setWidget(box);
            }

            protected void rightBits (byte type)
            {
                CMsgs.membersvc.loadInventory(CMsgs.creds, type, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        if (((List) result).size() == 0) {
                            _status.setText("You have no items of this type.");
                            return;
                        }
                        _right.setWidget(
                            new ItemChooser((List) result, _imageChooser));
                    }
                    public void onFailure (Throwable caught) {
                        _status.setText(CMsgs.serverError(caught));
                        
                    }
                });
            }
            
            protected Label _status;
            protected Label _title;
            protected SimplePanel _left, _right;
            
            protected AsyncCallback _imageChooser;
        }
        
        protected Item _item;
    }

    public static final class Display extends MailPayloadDisplay
    {
        public Display (MailMessage message)
        {
            super(message);
            _giftObject = (ItemGiftObject) message.payload;
        }

        // @Override
        public Widget widgetForRecipient (MailUpdateListener listener)
        {
            _listener = listener;
            return new DisplayWidget(true);
        }

        // @Override
        public Widget widgetForOthers ()
        {
            return new DisplayWidget(false);
        }

        // @Override
        public String okToDelete ()
        {
            if (_giftObject.item == null) {
                return null;
            }
            return "You can't delete this message until you've accepted the attached item.";
        }
        
        protected class DisplayWidget extends DockPanel
        {
            public DisplayWidget (boolean enabled)
            {
                super();
                _enabled = enabled;

                if (_giftObject.item == null) {
                    _title = new Label("This message once had an item attached to it.");
                    _title.addStyleName("Title");
                    add(_title, DockPanel.NORTH);
                    return;
                }
                
                _title = new Label("There is an item attached to this message:");
                add(_title, DockPanel.NORTH);

                _status = new Label();
                add(_status, DockPanel.SOUTH);
                _content = new FlowPanel();
                add(_content, DockPanel.CENTER);

                final ClickListener listener = new ClickListener() {
                    public void onClick (Widget sender) {
                        // TODO
                    }
                };
                
                CMsgs.itemsvc.loadItem(CShell.creds, _giftObject.item, new AsyncCallback() {
                    public void onSuccess (Object result) {
                         _content.add(new ItemThumbnail((Item) result, listener));
                    }
                    public void onFailure (Throwable caught) {
                        _status.setText(CShell.serverError(caught));
                    }
                });

            }
             
            protected boolean _enabled;
            
            protected Label _status, _title;
            protected FlowPanel _content;
        }

        protected ItemGiftObject _giftObject;
        protected MailUpdateListener _listener;
    }

    protected static String capitalizeString (String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
