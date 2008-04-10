//
// $Id$

package client.whirleds;

import java.util.Collection;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.RadioButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupExtras;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Photo;

import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.LimitedTextArea;
import client.util.MsoyCallback;
import client.util.MsoyUI;

/**
 * A popup that lets a member of sufficient rank modify a group's metadata.
 */
public class GroupEdit extends FlexTable
{
    /**
     * This constructor is for creating new Groups.
     */
    public GroupEdit ()
    {
        this(new Group(), new GroupExtras());
    }

    public GroupEdit (Group group, GroupExtras extras)
    {
        _group = group;
        _extras = extras;

        setStyleName("groupEditor");
        setCellSpacing(5);
        setCellPadding(0);

        Frame.setTitle(_group.groupId == 0 ? CWhirleds.msgs.editCreateTitle() : group.name);

        // set up our editor contents
        _name = MsoyUI.createTextBox(_group.name, GroupName.LENGTH_MAX, 20);
        addRow(CWhirleds.msgs.editName(), _name);

        // make sure the group's configured policy is consistent with what's shown in the GUI
        if (_group.policy == 0) {
            _group.policy = Group.POLICY_PUBLIC;
        }
        _policy = new ListBox();
        _policy.addItem(CWhirleds.msgs.policyPublic());
        _policy.addItem(CWhirleds.msgs.policyInvite());
        _policy.addItem(CWhirleds.msgs.policyExclusive());
        _policy.addItem(CWhirleds.msgs.policyBlog());
        _policy.setSelectedIndex(_group.policy - Group.POLICY_PUBLIC);
        addRow(CWhirleds.msgs.editPolicy(), _policy);

        addRow(CWhirleds.msgs.editLogo(), _logo = new PhotoChoiceBox(true, null));
        _logo.setMedia(_group.getLogo());

        _blurb = MsoyUI.createTextBox(_group.blurb, Group.MAX_BLURB_LENGTH, 40);
        addRow(CWhirleds.msgs.editBlurb(), _blurb);

        _homepage = MsoyUI.createTextBox(_extras.homepageUrl, 255, 40);
        addRow(CWhirleds.msgs.editHomepage(), _homepage);

        _charter = new LimitedTextArea(Group.MAX_CHARTER_LENGTH, 60, 3);
        _charter.setText(_extras.charter);
        addRow(CWhirleds.msgs.editCharter(), _charter);

        addRow(CWhirleds.msgs.editBackground(), _background = new PhotoChoiceBox(false, null));
        _background.setMedia(_extras.background);

        _bgmode = new ListBox();
        _bgmode.addItem(CWhirleds.msgs.editTile());
        _bgmode.addItem(CWhirleds.msgs.editAnchor());
        _bgmode.setSelectedIndex(_extras.backgroundControl);
        addRow(CWhirleds.msgs.editMode(), _bgmode);

        _catalogType = new ListBox();
        for (int ii = 0; ii < Item.TYPES.length; ii++) {
            _catalogType.addItem(CWhirleds.dmsgs.getString("itemType" + Item.TYPES[ii]));
            if (_extras.catalogItemType == Item.TYPES[ii]) {
                _catalogType.setSelectedIndex(ii);
            }
        }
        addRow(CWhirleds.msgs.editCatalogType(), _catalogType);
        addRow(CWhirleds.msgs.editCatalogTag(), 
            _catalogTag = MsoyUI.createTextBox(_extras.catalogTag, 24, 24));

        HorizontalPanel footer = new HorizontalPanel();
        footer.add(_submit = new Button(CWhirleds.cmsgs.submit(), new ClickListener() {
            public void onClick (Widget sender) {
                commitEdit();
            }
        }));
        footer.add(WidgetUtil.makeShim(5, 5));
        footer.add(new Button(CWhirleds.cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.WHIRLEDS, _group.groupId == 0 ? "" :
                               Args.compose("d", _group.groupId));
            }
        }));
        int frow = getRowCount();
        setWidget(frow, 1, footer);
        getFlexCellFormatter().setHorizontalAlignment(frow, 1, HasAlignment.ALIGN_RIGHT);
    }

    protected void addRow (String label, Widget contents)
    {
        int row = getRowCount();
        setText(row, 0, label);
        getFlexCellFormatter().setStyleName(row, 0, "nowrapLabel");
        getFlexCellFormatter().addStyleName(row, 0, "rightLabel");
        setWidget(row, 1, contents);
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
        _extras.catalogItemType = Item.TYPES[_catalogType.getSelectedIndex()];
        _extras.catalogTag = _catalogTag.getText().trim();

        // check that the group name is valid
        if (_group.name.length() < GroupName.LENGTH_MIN ||
            _group.name.length() > GroupName.LENGTH_MAX ||
            !(Character.isLetter(_group.name.charAt(0)) ||
              Character.isDigit(_group.name.charAt(0)))) {
            MsoyUI.error(CWhirleds.msgs.errInvalidGroupName());
            return;
        }

        final MsoyCallback updateCallback = new MsoyCallback() {
            public void onSuccess (Object result) {
                int groupId = (result == null) ? _group.groupId : ((Group)result).groupId;
                Application.go(Page.WHIRLEDS, Args.compose("d", String.valueOf(groupId), "r"));
            }
        };
        // check if we're trying to set the policy to exclusive on a group that has tags
        if (_group.policy == Group.POLICY_EXCLUSIVE) {
            CWhirleds.groupsvc.getTags(CWhirleds.ident, _group.groupId, new MsoyCallback () {
                public void onSuccess (Object result) {
                    if (((Collection)result).size() > 0) {
                        MsoyUI.error(CWhirleds.msgs.errTagsOnExclusive());
                    } else if (_group.groupId > 0) {
                        CWhirleds.groupsvc.updateGroup(
                            CWhirleds.ident, _group, _extras, updateCallback);
                    } else {
                        CWhirleds.groupsvc.createGroup(
                            CWhirleds.ident, _group, _extras, updateCallback);
                    }
                } 
            });
        } else {
            if (_group.groupId > 0) {
                CWhirleds.groupsvc.updateGroup(CWhirleds.ident, _group, _extras, updateCallback);
            } else {
                CWhirleds.groupsvc.createGroup(CWhirleds.ident, _group, _extras, updateCallback);
            }
        }
    }

    protected Group _group;
    protected GroupExtras _extras;

    protected TextBox _name, _blurb, _homepage, _catalogTag;
    protected PhotoChoiceBox _logo, _background;
    protected ListBox _policy, _bgmode, _catalogType;
    protected LimitedTextArea _charter;
    protected Button _submit;
}
