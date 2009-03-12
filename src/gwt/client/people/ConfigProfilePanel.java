//
// $Id$

package client.people;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.CoinAwards;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.gwt.MemberCard;

import client.item.ImageChooserPopup;
import client.ui.MsoyUI;
import client.ui.TongueBox;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.TextBoxUtil;

/**
 * Displays a streamlined interface for configuring some basic profile information for a new
 * user. This is step two of the register -> config profile -> find friends -> spam friends
 * registration process.
 */
public class ConfigProfilePanel extends FlowPanel
{
    public ConfigProfilePanel ()
    {
        setStyleName("configProfile");

        // TODO: need proper image (step 3)
        add(new TongueBox(null, InvitePanel.makeHeader(
                              "/images/people/share_header.png",
                              _msgs.cpIntro(""+CoinAwards.CREATED_PROFILE))));

        FlowPanel bits = new FlowPanel();
        bits.add(_card = new SmartTable("Card", 0, 10));
        _card.setWidget(0, 0, MediaUtil.createMediaView(MemberCard.DEFAULT_PHOTO,
                                                        MediaDesc.THUMBNAIL_SIZE));
        _card.getFlexCellFormatter().setRowSpan(0, 0, 2);
        _card.setText(0, 1, "???", 2, "Name");
        _card.setText(1, 0, _msgs.memberSince());
        _card.setText(1, 1, MsoyUI.formatDate(new Date()));

        SmartTable config = new SmartTable(0, 5);
        bits.add(config);
        config.setText(0, 0, "Upload a Photo:");
        config.setWidget(0, 1, new Button("Select...", new ClickListener() {
            public void onClick (Widget source) {
                ImageChooserPopup.displayImageChooser(true, new MsoyCallback<MediaDesc>() {
                    public void onSuccess (MediaDesc photo) {
                        if (photo != null) {
                            _photo = photo;
                            _card.setWidget(
                                0, 0, MediaUtil.createMediaView(_photo, MediaDesc.THUMBNAIL_SIZE));
                        }
                    }
                });
            }
        }));

        config.setText(1, 0, "Pick a Whirled Name:");
        _name = MsoyUI.createTextBox("", MemberName.MAX_DISPLAY_NAME_LENGTH, 20);
        TextBoxUtil.addTypingListener(_name, new Command() {
            public void execute () {
                String name = _name.getText().trim();
                _card.setText(0, 1, (name.length() == 0) ? "???" : name);
            }
        });
        config.setWidget(1, 1, _name);
        
        add(new TongueBox("Who Are You?", bits));
    }

    protected SmartTable _card;
    protected TextBox _name;
    protected MediaDesc _photo;

    protected static final PeopleMessages _msgs = GWT.create(PeopleMessages.class);
}
