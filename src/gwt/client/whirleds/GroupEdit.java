//
// $Id$

package client.whirleds;

import java.util.Collection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.GroupName;

import com.threerings.msoy.group.data.Group;
import com.threerings.msoy.group.data.GroupExtras;

import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.web.data.TagHistory;

import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.shell.ShellMessages;
import client.util.LimitedTextArea;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.PopupMenu;
import client.util.TagDetailPanel;

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

        // if this is a blank group, set up some defaults
        if (_group.policy == 0) {
            _group.policy = Group.POLICY_PUBLIC;
            _group.forumPerms = Group.makePerms(Group.PERM_MEMBER, Group.PERM_ALL);
        }

        // make sure the group's configured policy is consistent with what's shown in the GUI
        _policy = new ListBox();
        _policy.addItem(CWhirleds.msgs.policyPublic());
        _policy.addItem(CWhirleds.msgs.policyInvite());
        _policy.addItem(CWhirleds.msgs.policyExclusive());
        _policy.setSelectedIndex(_group.policy - Group.POLICY_PUBLIC);
        addRow(CWhirleds.msgs.editPolicy(), _policy);

        _thread = new ListBox();
        _thread.addItem(CWhirleds.msgs.forumPermsAll());
        _thread.addItem(CWhirleds.msgs.forumPermsMember());
        _thread.addItem(CWhirleds.msgs.forumPermsManager());
        _thread.setSelectedIndex(_group.getThreadPerm() - Group.PERM_ALL);
        addRow(CWhirleds.msgs.editThread(), _thread);

        _post = new ListBox();
        _post.addItem(CWhirleds.msgs.forumPermsAll());
        _post.addItem(CWhirleds.msgs.forumPermsMember());
        _post.addItem(CWhirleds.msgs.forumPermsManager());
        _post.setSelectedIndex(_group.getPostPerm() - Group.PERM_ALL);
        addRow(CWhirleds.msgs.editPost(), _post);

        addRow(CWhirleds.msgs.editLogo(), _logo = new PhotoChoiceBox(true, null));
        _logo.setMedia(_group.getLogo());

        _blurb = MsoyUI.createTextBox(_group.blurb, Group.MAX_BLURB_LENGTH, 40);
        addRow(CWhirleds.msgs.editBlurb(), _blurb);

        _homepage = MsoyUI.createTextBox(_extras.homepageUrl, 255, 40);
        addRow(CWhirleds.msgs.editHomepage(), _homepage);

        _charter = new LimitedTextArea(Group.MAX_CHARTER_LENGTH, 60, 3);
        _charter.setText(_extras.charter);
        addRow(CWhirleds.msgs.editCharter(), _charter);

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
        footer.add(_submit = new Button(_cmsgs.change(), new ClickListener() {
            public void onClick (Widget sender) {
                commitEdit();
            }
        }));
        footer.add(WidgetUtil.makeShim(5, 5));
        footer.add(new Button(_cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                Link.go(Page.WHIRLEDS, _group.groupId == 0 ? "" :
                        Args.compose("d", _group.groupId));
            }
        }));
        int frow = getRowCount();
        setWidget(frow, 1, footer);
        getFlexCellFormatter().setHorizontalAlignment(frow, 1, HasAlignment.ALIGN_RIGHT);

        // TODO integrate tags into the main form
        if (_group.groupId != 0 && _group.policy != Group.POLICY_EXCLUSIVE) {
            TagDetailPanel tags = new TagDetailPanel(new TagDetailPanel.TagService() {
                public void tag (String tag, AsyncCallback<TagHistory> cback) {
                    CWhirleds.groupsvc.tagGroup(CWhirleds.ident, _group.groupId, tag, true, cback);
                }
                public void untag (String tag, AsyncCallback<TagHistory> cback) {
                    CWhirleds.groupsvc.tagGroup(CWhirleds.ident, _group.groupId, tag, false, cback);
                }
                public void getRecentTags (AsyncCallback<Collection<TagHistory>> cback) {
                    CWhirleds.groupsvc.getRecentTags(CWhirleds.ident, cback);
                }
                public void getTags (AsyncCallback<Collection<String>> cback) {
                    CWhirleds.groupsvc.getTags(CWhirleds.ident, _group.groupId, cback);
                }
                public boolean supportFlags () {
                    return false;
                }
                public void setFlags (byte flag) {
                    // nada
                }
                public void addMenuItems (final String tag, PopupMenu menu) {
                    menu.addMenuItem(CWhirleds.msgs.detailTagLink(), new Command() {
                        public void execute () {
                            Link.go(Page.WHIRLEDS, Args.compose("tag", "0", tag));
                        }
                    });
                }
            }, true);
            addRow("", WidgetUtil.makeShim(1, 20));
            addRow("", tags);
        }

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
        _group.forumPerms = Group.makePerms(_thread.getSelectedIndex()+Group.PERM_ALL,
                                            _post.getSelectedIndex()+Group.PERM_ALL);
        _extras.charter = _charter.getText().trim();
        _extras.homepageUrl = _homepage.getText().trim();
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

        final MsoyCallback<Void> updateCallback = new MsoyCallback<Void>() {
            public void onSuccess (Void result) {
                Link.go(
                    Page.WHIRLEDS, Args.compose("d", String.valueOf(_group.groupId), "r"));
            }
        };
        final MsoyCallback<Group> createCallback = new MsoyCallback<Group>() {
            public void onSuccess (Group group) {
                Link.go(
                    Page.WHIRLEDS, Args.compose("d", String.valueOf(group.groupId), "r"));
            }
        };
        // check if we're trying to set the policy to exclusive on a group that has tags
        if (_group.policy == Group.POLICY_EXCLUSIVE) {
            CWhirleds.groupsvc.getTags(
                CWhirleds.ident, _group.groupId, new MsoyCallback<Collection<String>>() {
                    public void onSuccess (Collection<String> tags) {
                        if (tags.size() > 0) {
                            MsoyUI.error(CWhirleds.msgs.errTagsOnExclusive());
                        } else if (_group.groupId > 0) {
                            CWhirleds.groupsvc.updateGroup(
                                CWhirleds.ident, _group, _extras, updateCallback);
                        } else {
                            CWhirleds.groupsvc.createGroup(
                                CWhirleds.ident, _group, _extras, createCallback);
                        }
                    }
                });
        } else {
            if (_group.groupId > 0) {
                CWhirleds.groupsvc.updateGroup(CWhirleds.ident, _group, _extras, updateCallback);
            } else {
                CWhirleds.groupsvc.createGroup(CWhirleds.ident, _group, _extras, createCallback);
            }
        }
    }

    protected Group _group;
    protected GroupExtras _extras;

    protected TextBox _name, _blurb, _homepage, _catalogTag;
    protected PhotoChoiceBox _logo;
    protected ListBox _policy, _thread, _post, _catalogType;
    protected LimitedTextArea _charter;
    protected Button _submit;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
