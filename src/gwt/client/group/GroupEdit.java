//
// $Id$

package client.group;

import java.util.Collection;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupExtras;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;

import client.shell.Application;
import client.shell.Page;
import client.util.BorderedDialog;
import client.util.LimitedTextArea;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PhotoChoiceBox;

/**
 * A popup that lets a member of sufficient rank modify a group's metadata.
 */
public class GroupEdit extends BorderedDialog
{
    /**
     * A callback interface for classes that want to know when a group successfully committed.
     */
    public static interface GroupSubmissionListener
    {
        /**
         * Called by {@link GroupEdit}; reloads the group.
         */
        public void groupSubmitted (Group group);
    }

    /**
     * This constructor is for creating new Groups.
     */
    public GroupEdit ()
    {
        this(new Group(), new GroupExtras(), null);
    }

    public GroupEdit (Group group, GroupExtras extras, GroupSubmissionListener listener)
    {
        super();

        _group = group;
        _extras = extras;
        _listener = listener;

        String title = _group.groupId == 0 ?
            CGroup.msgs.editCreateTitle() : CGroup.msgs.editEditTitle();
        _header.add(createTitleLabel(title, "GroupTitle"));

        // set up our editor contents
        _name = MsoyUI.createTextBox(_group.name, GroupName.LENGTH_MAX, 20);
        addRow(CGroup.msgs.editName(), _name);

        // make sure the group's configured policy is consistent with what's shown in the GUI
        if (_group.policy == 0) {
            _group.policy = Group.POLICY_PUBLIC;
        }
        _policy = new ListBox();
        _policy.addItem(CGroup.msgs.policyPublic());
        _policy.addItem(CGroup.msgs.policyInvite());
        _policy.addItem(CGroup.msgs.policyExclusive());
        _policy.setSelectedIndex(_group.policy - Group.POLICY_PUBLIC);
        addRow(CGroup.msgs.editPolicy(), _policy);

        addRow(CGroup.msgs.editLogo(), _logo = new PhotoChoiceBox(null));
        _logo.setMedia(_group.getLogo());

        _blurb = MsoyUI.createTextBox(_group.blurb, Group.MAX_BLURB_LENGTH, 40);
        addRow(CGroup.msgs.editBlurb(), _blurb);

        _homepage = MsoyUI.createTextBox(_extras.homepageUrl, 255, 40);
        addRow(CGroup.msgs.editHomepage(), _homepage);

        _charter = new LimitedTextArea(Group.MAX_CHARTER_LENGTH, 40, 3);
        _charter.setText(_extras.charter);
        addRow(CGroup.msgs.editCharter(), _charter);

        addRow(CGroup.msgs.editBackground(), _background = new PhotoChoiceBox(null) {
            protected MediaDesc toMedia (Photo photo) {
                return (photo == null) ? null : new MediaDesc(
                    photo.photoMedia.hash, photo.photoMedia.mimeType,
                    // the background image constraint is computed on thumbnail size so that it
                    // can be scaled properly in the edit dialog
                    MediaDesc.computeConstraint(
                        MediaDesc.THUMBNAIL_SIZE, photo.photoWidth, photo.photoHeight));
            }
        });
        _background.setMedia(_extras.background);

        _bgmode = new ListBox();
        _bgmode.addItem(CGroup.msgs.editTile());
        _bgmode.addItem(CGroup.msgs.editAnchor());
        _bgmode.setSelectedIndex(_extras.backgroundControl);
        addRow(CGroup.msgs.editMode(), _bgmode);

        _footer.add(_submit = new Button(CGroup.cmsgs.submit(), new ClickListener() {
            public void onClick (Widget sender) {
                commitEdit();
            }
        }));
        _footer.add(new Button(CGroup.cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        }));
    }

    // @Override // from BorderedDialog
    protected Widget createContents ()
    {
        FlexTable contents = new FlexTable();
        contents.setStyleName("groupEditor");
        contents.setCellSpacing(5);
        contents.setCellPadding(0);
        return contents;
    }

    protected void addRow (String label, Widget contents)
    {
        FlexTable table = (FlexTable)_contents;
        int row = table.getRowCount();
        table.setText(row, 0, label);
        table.getFlexCellFormatter().setStyleName(row, 0, "nowrapLabel");
        table.getFlexCellFormatter().addStyleName(row, 0, "rightLabel");
        table.setWidget(row, 1, contents);
    }

    protected void commitEdit ()
    {
        // extract our values
        _group.name = _name.getText().trim();
        _group.logo = _logo.getMedia();
        _group.blurb = _blurb.getText().trim();
        _group.policy = (byte)(_policy.getSelectedIndex()+Group.POLICY_PUBLIC);
        _extras.charter = _charter.getText().trim();
        _extras.homepageUrl = _homepage.getText().trim();
        _extras.backgroundControl = _bgmode.getSelectedIndex();
        _extras.background = _background.getMedia();

        // check that the group name is valid
        if (_group.name.length() < GroupName.LENGTH_MIN ||
            _group.name.length() > GroupName.LENGTH_MAX ||
            !(Character.isLetter(_group.name.charAt(0)) ||
              Character.isDigit(_group.name.charAt(0)))) {
            MsoyUI.error(CGroup.msgs.errInvalidGroupName());
            return;
        }

        final MsoyCallback updateCallback = new MsoyCallback() {
            public void onSuccess (Object result) {
                hide();
                if (_listener != null) {
                    _listener.groupSubmitted(_group);
                } else if (_group.groupId == 0) {
                    // new group created - go to the new group view page
                    Group newGroup = (Group)result;
                    Application.go(Page.GROUP, "" + newGroup.groupId);
                }
            }
        };
        // check if we're trying to set the policy to exclusive on a group that has tags
        if (_group.policy == Group.POLICY_EXCLUSIVE) {
            CGroup.groupsvc.getTags(CGroup.ident, _group.groupId, new MsoyCallback () {
                public void onSuccess (Object result) {
                    if (((Collection)result).size() > 0) {
                        MsoyUI.error(CGroup.msgs.errTagsOnExclusive());
                    } else if (_group.groupId > 0) {
                        CGroup.groupsvc.updateGroup(CGroup.ident, _group, _extras, updateCallback);
                    } else {
                        CGroup.groupsvc.createGroup(CGroup.ident, _group, _extras, updateCallback);
                    }
                } 
            });
        } else {
            if (_group.groupId > 0) {
                CGroup.groupsvc.updateGroup(CGroup.ident, _group, _extras, updateCallback);
            } else {
                CGroup.groupsvc.createGroup(CGroup.ident, _group, _extras, updateCallback);
            }
        }
    }

    protected Group _group;
    protected GroupExtras _extras;
    protected GroupSubmissionListener _listener;

    protected TextBox _name, _blurb, _homepage;
    protected PhotoChoiceBox _logo, _background;
    protected ListBox _policy, _bgmode;
    protected LimitedTextArea _charter;
    protected Button _submit;
}
