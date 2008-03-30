//
// $Id$

package client.msgs;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.person.data.ItemGiftPayload;
import com.threerings.msoy.person.data.MailPayload;

import client.shell.CShell;
import client.util.BorderedWidget;
import client.util.ItemThumbnail;
import client.util.MsoyCallback;

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
            return new ItemGiftPayload(_item.getIdent());
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
            CMsgs.itemsvc.wrapItem(CMsgs.ident, _item.getIdent(), true, new MsoyCallback() {
                public void onSuccess (Object result) {
                    // good
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
                FlexTable panel = new FlexTable();
                panel.setStyleName("itemGift");

                _title = new Label(CMsgs.mmsgs.giftChoose());
                _title.addStyleName("Title");
                panel.setWidget(0, 0, _title);
                panel.getFlexCellFormatter().setColSpan(0, 0, 2);

                panel.setWidget(1, 0, _left = new SimplePanel());
                panel.getFlexCellFormatter().setVerticalAlignment(1, 0, VerticalPanel.ALIGN_TOP);
                panel.setWidget(1, 1, _right = new PagedGrid(1, 4) {
                    protected Widget createWidget (Object item) {
                        return new ItemThumbnail((Item)item, new ClickListener() {
                            public void onClick (Widget sender) {
                                _imageChooser.onSuccess(
                                    ((ItemThumbnail)(sender.getParent())).getItem());
                            }
                        });
                    }
                    protected String getEmptyMessage () {
                        return CMsgs.mmsgs.giftNoItems();
                    }
                });
                _right.setWidth("100%");

                final FlowPanel southBits = new FlowPanel();
                southBits.add(_status = new Label());
                Button cancelButton = new Button(CMsgs.mmsgs.giftCancel());
                cancelButton.addStyleName("ControlButton");
                cancelButton.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        _composer.removePayload();
                    }
                });
                southBits.add(cancelButton);
                panel.setWidget(2, 0, southBits);
                panel.getFlexCellFormatter().setColSpan(2, 0, 2);

                _imageChooser = new MsoyCallback() {
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
                        _right.setVisible(false);
                    }
                };

                final ListBox box = new ListBox();
                for (int i = 0; i < Item.GIFT_TYPES.length; i ++) {
                    box.addItem(CMsgs.dmsgs.getString("itemType" + Item.GIFT_TYPES[i]));
                }
                box.setSelectedIndex(0);
                box.addChangeListener(new ChangeListener() {
                    public void onChange (Widget sender) {
                        _status.setText(null);
                        if (box.getSelectedIndex() != -1) {
                            loadInventory(Item.GIFT_TYPES[box.getSelectedIndex()]);
                        }
                    }
                });
                _left.setWidget(box);
                setWidget(panel);
                loadInventory(Item.GIFT_TYPES[0]);
            }

            protected void loadInventory (final byte type)
            {
                CMsgs.membersvc.loadInventory(CMsgs.ident, type, 0, new AsyncCallback() {
                    public void onSuccess (Object result) {
                        _right.setModel(new SimpleDataModel((List)result), 0);
                    }
                    public void onFailure (Throwable caught) {
                        _status.setText(CMsgs.serverError(caught));
                    }
                });
            }

            protected Label _status;
            protected Label _title;
            protected SimplePanel _left;
            protected PagedGrid _right;

            protected AsyncCallback _imageChooser;
        }

        protected MailComposition _composer;

        protected Item _item;
    }

    protected static String capitalizeString (String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
