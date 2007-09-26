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
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.web.data.ItemGiftObject;
import com.threerings.msoy.web.data.MailMessage;
import com.threerings.msoy.web.data.MailPayload;
import com.threerings.msoy.data.all.MemberName;

public abstract class ItemGift
{
    public static final class Composer
        implements MailPayloadComposer
    {
        public Composer (MailComposition composer)
        {
            _composer = composer;
        }

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
                return CMsgs.mmsgs.giftNoItem();
            }
            return null;
        }

        // from MailPayloadComposer
        public void messageSent (MemberName recipient)
        {
            CMsgs.itemsvc.wrapItem(CMsgs.ident, _item.getIdent(), true, new AsyncCallback() {
                public void onSuccess (Object result) {
                    // good
                }
                public void onFailure (Throwable caught) {
                    // this would be bad. we must write the server-side code defensively,
                    // with good logging, and also TODO: there should be a last-minute check
                    // that the item is still owned by the user in getComposedPayload().
                    CMsgs.log("Failed to wrap item [item=" + _item + "]", caught);
                }
            });
        }

        protected class CompositionWidget extends BorderedWidget
        {
            public CompositionWidget ()
            {
                super();
                buildUI();
            }

            protected void buildUI ()
            {
                DockPanel panel = new DockPanel();
                panel.setStyleName("itemGift");

                _status = new Label();
                final FlowPanel southBits = new FlowPanel();
                southBits.add(_status);
                Button cancelButton = new Button(CMsgs.mmsgs.giftCancel());
                cancelButton.addStyleName("ControlButton");
                cancelButton.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        _composer.removePayload();
                    }
                });
                southBits.add(cancelButton);
                panel.add(southBits, DockPanel.SOUTH);

                _title = new Label(CMsgs.mmsgs.giftChoose());
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
                        _title.setText(CMsgs.mmsgs.giftChosen());
                        VerticalPanel selectedBits = new VerticalPanel();
                        selectedBits.add(new Image(_item.getThumbnailPath()));
                        Button backButton = new Button(CMsgs.mmsgs.giftBtnAnother());
                        backButton.addStyleName("ControlButton");
                        backButton.addClickListener(new ClickListener() {
                            public void onClick (Widget sender) {
                                _item = null;
                                buildUI();
                            }
                        });
                        southBits.add(backButton);
                        _left.setWidget(selectedBits);
                        _right.clear();
                    }
                    public void onFailure (Throwable caught) {
                        // not used
                    }
                };

                final ListBox box = new ListBox();
                for (int i = 0; i < Item.GIFT_TYPES.length; i ++) {
                    box.addItem(CMsgs.dmsgs.getString("itemType" + Item.GIFT_TYPES[i]));
                }
                box.setSelectedIndex(0);
                box.addChangeListener(new ChangeListener() {
                    public void onChange (Widget sender) {
                        _right.clear();
                        _status.setText(null);
                        if (box.getSelectedIndex() != -1) {
                            rightBits(Item.GIFT_TYPES[box.getSelectedIndex()]);
                        }
                    }
                });
                _left.setWidget(box);

                rightBits(Item.PHOTO);

                setWidget(panel);
            }

            protected void rightBits (final byte type)
            {
                CMsgs.membersvc.loadInventory(CMsgs.ident, type, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        if (((List) result).size() == 0) {
                            _status.setText(CMsgs.mmsgs.giftNoItems());
                            return;
                        }
                        _right.setWidget(
                            new ItemChooser((List) result, _imageChooser));
                    }
                    public void onFailure (Throwable caught) {
                        CMsgs.log("Failed to load inventory [type=" + type + "]", caught);
                        _status.setText(CMsgs.serverError(caught));

                    }
                });
            }

            protected Label _status;
            protected Label _title;
            protected SimplePanel _left, _right;

            protected AsyncCallback _imageChooser;
        }

        protected MailComposition _composer;

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
            return CMsgs.mmsgs.giftNoDelete();
        }

        protected class DisplayWidget extends DockPanel
        {
            public DisplayWidget (boolean enabled)
            {
                super();
                _enabled = enabled;
                buildUI();
            }

            protected void buildUI ()
            {
                clear();

                if (_giftObject.item == null) {
                    _title = new Label(CMsgs.mmsgs.giftGone());
                    _title.addStyleName("Title");
                    add(_title, DockPanel.NORTH);
                    return;
                }

                _title = new Label(CMsgs.mmsgs.giftItem());
                add(_title, DockPanel.NORTH);

                _status = new Label();
                add(_status, DockPanel.SOUTH);
                _content = new FlowPanel();
                add(_content, DockPanel.CENTER);

                final ClickListener listener = new ClickListener() {
                    public void onClick (Widget sender) {
                        unwrapItem();
                    }
                };

                CMsgs.itemsvc.loadItem(CShell.ident, _giftObject.item, new AsyncCallback() {
                    public void onSuccess (Object result) {
                         _content.add(new ItemThumbnail((Item) result, listener));
                    }
                    public void onFailure (Throwable caught) {
                        CMsgs.log("Failed to load item [item=" + _giftObject.item + "]", caught);
                        _status.setText(CShell.serverError(caught));
                    }
                });

            }

            protected void unwrapItem ()
            {
                if (_giftObject.item == null) {
                    // this happens if the user clicks the thumbnail a second time after the
                    // unwrapping suceeds, but before the payload state is updated on the
                    // server; just swallow the click.
                    return;
                }
                CMsgs.itemsvc.wrapItem(CMsgs.ident, _giftObject.item, false, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        // the item is unwrapped, just update the payload
                        _giftObject.item = null;
                        updateState(_giftObject, new AsyncCallback() {
                            public void onSuccess (Object result) {
                                // all went well: rebuild the view
                                buildUI();
                            }
                            public void onFailure (Throwable caught) {
                                // this is an unpleasant inconsistency
                                CMsgs.log("Failed to update payload state [item=" +
                                          _giftObject.item + "]", caught);
                                _status.setText(CShell.serverError(caught));
                            }
                        });
                    }
                    public void onFailure (Throwable caught) {
                        CMsgs.log("Failed to unwrap item [item=" + _giftObject.item + "]", caught);
                        _status.setText(CShell.serverError(caught));
                    }
                });
            };

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
