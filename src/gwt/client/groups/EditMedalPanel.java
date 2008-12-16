//
// $Id$

package client.groups;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.group.data.all.Medal;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ImageChooserPopup;

import client.ui.LimitedTextArea;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

public class EditMedalPanel extends FlexTable
{
    public EditMedalPanel (int groupId, int medalId)
    {
        setStyleName("editMedalPanel");
        if (medalId == 0) {
            init(new Medal(groupId));
        } else {
            _groupsvc.getMedal(medalId, new MsoyCallback<Medal>() {
                public void onSuccess (Medal medal) {
                    if (medal == null) {
                        MsoyUI.error(_msgs.editMedalNotFound());
                        return;
                    }

                    init(medal);
                }
            });
        }
    }

    protected void init (Medal medal)
    {
        _medal = medal;

        addName();
        addIconUploader();
        addDescription();

        Button esubmit = new Button(_msgs.editMedalSubmit());
        esubmit.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                commitEdit();
            }
        });
        Button ecancel = new Button(_msgs.editMedalCancel());
        ecancel.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                Link.go(Pages.GROUPS, GroupsPage.Nav.MEDALS.composeArgs(_medal.groupId));
            }
        });

        int row = getRowCount();
        setWidget(row, 1, MsoyUI.createButtonPair(ecancel, esubmit));
        getFlexCellFormatter().setHorizontalAlignment(row, 1, HasAlignment.ALIGN_RIGHT);
    }

    protected void commitEdit ()
    {
        if (_medal.name == null || _medal.name.equals("") ||
            _medal.description == null || _medal.description.equals("") ||
            _medal.icon == null) {
            MsoyUI.error(_msgs.editMedalAllRequired());
            return;
        }

        _groupsvc.updateMedal(_medal, new MsoyCallback<Void>() {
            public void onSuccess(Void result) {
                Link.go(Pages.GROUPS, GroupsPage.Nav.MEDALS.composeArgs(_medal.groupId));
            }
        });
    }

    protected void addRow (String label, Widget widget)
    {
        int row = getRowCount();
        getFlexCellFormatter().setWidth(row, 0, "125px");
        setText(row, 0, label);
        getFlexCellFormatter().setStyleName(row, 0, "nowrapLabel");
        getFlexCellFormatter().setVerticalAlignment(row, 0, HasAlignment.ALIGN_TOP);
        setWidget(row, 1, widget);
    }

    protected void addName ()
    {
        _name = new TextBox();
        if (_medal.name != null) {
            _name.setText(_medal.name);
        }
        _name.addKeyboardListener(new KeyboardListenerAdapter() {
            @Override public void onKeyPress (Widget sender, char keyCode, int mods) {
                DeferredCommand.addCommand(new Command() {
                    public void execute () {
                        _medal.name = _name.getText();
                    }
                });
            }
        });
        _name.setMaxLength(Medal.MAX_NAME_LENGTH);
        addRow(_msgs.editMedalName(), _name);
    }

    protected void addDescription ()
    {
        _description = new LimitedTextArea(Medal.MAX_DESCRIPTION_LENGTH, 40, 3);
        if (_medal.description != null) {
            _description.setText(_medal.description);
        }
        _description.getTextArea().addKeyboardListener(new KeyboardListenerAdapter() {
            @Override public void onKeyPress (Widget sender, char keyCode, int mods) {
                DeferredCommand.addCommand(new Command() {
                    public void execute () {
                        _medal.description = _description.getText();
                    }
                });
            }
        });
        addRow(_msgs.editMedalDescription(), _description);
    }

    protected void addIconUploader ()
    {
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(_iconPreview = new SimplePanel());
        if (_medal.icon != null) {
            setIconImage(_medal.icon);
        }
        Button pickImage = new Button(_msgs.editMedalChooseIconImage());
        pickImage.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                ImageChooserPopup.displayRestrictedImageChooser(
                    Medal.MEDAL_WIDTH, Medal.MEDAL_HEIGHT, new MsoyCallback<MediaDesc>() {
                        public void onSuccess(MediaDesc media) {
                            if (media != null) {
                                setIconImage(_medal.icon = media);
                            }
                        }
                    });
            }
        });
        panel.add(pickImage);
        addRow(_msgs.editMedalIcon(), panel);
    }

    protected void setIconImage (MediaDesc media)
    {
        _iconPreview.setWidget(MediaUtil.createMediaView(media, MediaDesc.THUMBNAIL_SIZE));
    }

    protected Medal _medal;
    protected TextBox _name;
    protected LimitedTextArea _description;
    protected SimplePanel _iconPreview;

    protected static final String ICON_MEDIA_ID = "medalIcon";

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final GroupServiceAsync _groupsvc = (GroupServiceAsync)ServiceUtil.bind(
        GWT.create(GroupService.class), GroupService.ENTRY_POINT);
}
