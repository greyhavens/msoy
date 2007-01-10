//
// $Id$

package client.group;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.CellPanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.Panel;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Photo;
import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.web.data.GroupName;
import com.threerings.msoy.web.data.GroupExtras;

import com.threerings.gwt.ui.InlineLabel;

import client.shell.MsoyEntryPoint;
import client.util.BorderedDialog;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * A popup that lets a member of sufficient rank modify a group's metadata.
 */
public class GroupEdit extends BorderedDialog
{
    /**
     * A callback interface for classes that want to know when a group successfully committed.
     */
    public static interface GroupSubmissionListener {
        public void groupSubmitted(Group group);
    }

    /**
     * This constructor is for creating new Groups.
     */
    public GroupEdit (GroupContext ctx)
    {
        this(ctx, new Group(), new GroupExtras(), null);
    }

    public GroupEdit (GroupContext ctx, Group group, GroupExtras extras,
        GroupSubmissionListener listener)
    {
        super();
        _ctx = ctx;
        _group = group;
        _extras = extras;
        _listener = listener;

        String title = _group.groupId == 0 ?
            _ctx.msgs.editCreateTitle() : _ctx.msgs.editEditTitle();
        _header.add(MsoyUI.createLabel(title, "GroupTitle"));
        VerticalPanel contents = (VerticalPanel)_contents;

        _errorContainer = new HorizontalPanel();
        contents.add(_errorContainer);
        contents.add(createTextEntryField(_ctx.msgs.editName(), GroupName.LENGTH_MAX, 20,
                                          _group.name, new ChangeListener() {
            public void onChange (Widget sender) {
                _group.name = ((TextBox)sender).getText().trim();
                updateSubmitButton();
            }
        }));

        TabPanel groupTabs = new TabPanel();
        groupTabs.setStyleName("Tabs");
        groupTabs.add(createInfoPanel(), _ctx.msgs.editInfoTab());
        groupTabs.add(createDescriptionPanel(), _ctx.msgs.editDescripTab());
        groupTabs.add(createBackgroundsPanel(), _ctx.msgs.editImagesTab());
        groupTabs.selectTab(0);
        contents.add(groupTabs);

        _submitButton = new Button(_ctx.cmsgs.submit());
        updateSubmitButton();
        _submitButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                commitEdit();
            }
        });
        _footer.add(_submitButton);
        Button cancelButton = new Button("Cancel");
        cancelButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        });
        _footer.add(cancelButton);
    }

    // from BorderedDialog.  This is called in the super constructor, so no UI components
    // that depend on members that are set in this object's constructor can be used here.
    public Widget createContents ()
    {
        VerticalPanel contents = new VerticalPanel();
        contents.setStyleName("groupEditor");
        return contents;
    }

    protected Panel createInfoPanel ()
    {
        VerticalPanel infoPanel = new VerticalPanel();
        infoPanel.add(createTextEntryField(_ctx.msgs.editHomepage(), 255, 20, _extras.homepageUrl,
            new ChangeListener() {
                public void onChange (Widget sender) {
                    _extras.homepageUrl = ((TextBox)sender).getText().trim();
                }
            }));

        byte selectedPolicy = _group.policy != 0 ? _group.policy : Group.POLICY_PUBLIC;
        HorizontalPanel policyPanel = new HorizontalPanel();
        policyPanel.add(new InlineLabel(_ctx.msgs.editPolicy()));
        final ListBox policyBox = new ListBox();
        policyBox.addItem(_ctx.msgs.policyPublic());
        policyBox.addItem(_ctx.msgs.policyInvite());
        policyBox.addItem(_ctx.msgs.policyExclusive());
        switch(selectedPolicy) {
        case Group.POLICY_PUBLIC: policyBox.setSelectedIndex(0); break;
        case Group.POLICY_INVITE_ONLY: policyBox.setSelectedIndex(1); break;
        case Group.POLICY_EXCLUSIVE: policyBox.setSelectedIndex(2); break;
        default: addError(_ctx.msgs.errUnknownPolicy("" + selectedPolicy));
        }
        policyBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                switch(policyBox.getSelectedIndex()) {
                case 0: _group.policy = Group.POLICY_PUBLIC; break;
                case 1: _group.policy = Group.POLICY_INVITE_ONLY; break;
                case 2: _group.policy = Group.POLICY_EXCLUSIVE; break;
                default: addError(_ctx.msgs.errUnknownPolicy("" + policyBox.getSelectedIndex()));
                }
            }
        });
        policyPanel.add(policyBox);
        infoPanel.add(policyPanel);

        VerticalPanel logoBox = new VerticalPanel();
        infoPanel.add(logoBox);
        updateImageBox(logoBox, IMAGE_LOGO, _ctx.msgs.editSetLogo());

        return infoPanel;
    }

    protected Panel createDescriptionPanel ()
    {
        VerticalPanel descriptionPanel = new VerticalPanel();
        descriptionPanel.add(createTextEntryField(_ctx.msgs.editBlurb(), 80, 20, _group.blurb,
            new ChangeListener() {
                public void onChange (Widget sender) {
                    _group.blurb = ((TextBox)sender).getText().trim();
                }
            }));

        HorizontalPanel charterPanel = new HorizontalPanel();
        charterPanel.add(new InlineLabel(_ctx.msgs.editCharter()));
        TextArea charterText = new TextArea();
        charterText.setCharacterWidth(50);
        charterText.setVisibleLines(10);
        if (_extras.charter != null) {
            charterText.setText(_extras.charter);
        }
        charterText.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                _extras.charter = ((TextArea)sender).getText().trim();
            }
        });
        charterPanel.add(charterText);
        descriptionPanel.add(charterPanel);

        return descriptionPanel;
    }

    protected Panel createBackgroundsPanel ()
    {
        HorizontalPanel backgroundsPanel = new HorizontalPanel();
        int types[] = { IMAGE_INFO_BACKGROUND, IMAGE_DETAIL_BACKGROUND,
            IMAGE_PEOPLE_BACKGROUND };
        String labels[] = { _ctx.msgs.editInfoBG(), _ctx.msgs.editDetailBG(),
                            _ctx.msgs.editPeopleBG() };
        for (int i = 0; i < types.length; i++) {
            VerticalPanel imageBox = new VerticalPanel();
            backgroundsPanel.add(imageBox);
            updateImageBox(imageBox, types[i], _ctx.msgs.editSetBG(labels[i]));
        }
        return backgroundsPanel;
    }

    protected Widget createTextEntryField(String label, int maxLength, int visibleLength,
        String startingText, ChangeListener listener)
    {
        HorizontalPanel textEntryField = new HorizontalPanel();
        textEntryField.add(new InlineLabel(label));
        TextBox textEntryBox = new TextBox();
        textEntryBox.setMaxLength(maxLength);
        textEntryBox.setVisibleLength(visibleLength);
        if (startingText != null) {
            textEntryBox.setText(startingText);
        }
        if (listener != null) {
            textEntryBox.addChangeListener(listener);
        }
        textEntryField.add(textEntryBox);
        return textEntryField;
    }

    // submit a modified group, and notify listeners
    protected void commitEdit ()
    {
        // check if the group name is valid.
        if (!Character.isLetter(_group.name.charAt(0)) &&
            !Character.isDigit(_group.name.charAt(0))) {
            Window.alert(_ctx.msgs.errInvalidGroupName());
            return;
        }

        AsyncCallback callback = new AsyncCallback() {
            public void onSuccess (Object result) {
                hide();
                if (_listener != null) {
                    _listener.groupSubmitted(_group);
                } else if (_group.groupId == 0) {
                    // new group created - go to the new group view page
                    Group newGroup = (Group)result;
                    History.newItem("" + newGroup.groupId);
                }
            }
            public void onFailure (Throwable caught) {
                addError(_ctx.serverError(caught));
            }
        };
        if (_group.groupId > 0) {
            _ctx.groupsvc.updateGroup(_ctx.creds, _group, _extras, callback);
        } else {
            _ctx.groupsvc.createGroup(_ctx.creds, _group, _extras, callback);
        }
    }

    // update the contents of the image box, e.g. after the image has been changed
    protected void updateImageBox (final CellPanel box, final int type, final String buttonLabel)
    {
        MediaDesc media = null;
        switch (type) {
        case IMAGE_LOGO: media = _group.logo; break;
        case IMAGE_INFO_BACKGROUND: media = _extras.infoBackground; break;
        case IMAGE_DETAIL_BACKGROUND: media = _extras.detailBackground; break;
        case IMAGE_PEOPLE_BACKGROUND: media = _extras.peopleBackground; break;
        default: addError("Internal Error! Unknown image type: " + type); return;
        }

        box.clear();
        if (media != null) {
            box.add(MediaUtil.createMediaView(media, MediaDesc.THUMBNAIL_SIZE));
        }
        Button changeButton = new Button(buttonLabel);
        changeButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                popupImageChooser(box, type, buttonLabel);
            }
        });
        box.add(changeButton);
    }

    // pop up a scrollable horizontal list of photo items from which to choose a logo
    protected void popupImageChooser (final CellPanel box, final int type, final String buttonLabel)
    {
        // the list of images is cached for this object
        if (_images == null) {
            _ctx.membersvc.loadInventory(_ctx.creds, Item.PHOTO, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _images = (List) result;
                    // will use the cached results this time.
                    popupImageChooser(box, type, buttonLabel);
                }
                public void onFailure (Throwable caught) {
                    _ctx.log("loadInventory failed", caught);
                    addError(_ctx.msgs.errPhotoLoadFailed(_ctx.serverError(caught)));
                }
            });

        } else {
            if (_images.size() == 0) {
                addError(_ctx.msgs.errNoPhotos());
                return;
            }

            // create the popup and its nested panels
            HorizontalPanel itemPanel = new HorizontalPanel();
            ScrollPanel chooser = new ScrollPanel(itemPanel);
            final PopupPanel popup = new PopupPanel(true);
            popup.setStyleName("groupLogoPopup");
            popup.setWidget(chooser);

            // set up a listener to pick an image for the logo, hide the popup, and update
            final ClickListener logoChanger = new ClickListener() {
                public void onClick (Widget sender) {
                    Photo photo = ((PhotoThumbnailImage) sender).photo;
                    switch(type) {
                    case IMAGE_LOGO:
                        _group.logo = photo.getThumbnailMedia();
                        break;
                    case IMAGE_INFO_BACKGROUND:
                        _extras.infoBackground = photo.photoMedia;
                        break;
                    case IMAGE_DETAIL_BACKGROUND:
                        _extras.detailBackground = photo.photoMedia;
                        break;
                    case IMAGE_PEOPLE_BACKGROUND:
                        _extras.peopleBackground = photo.photoMedia;
                        break;
                    default:
                        addError("Internal Error! Unkown image type: " + type);
                    }
                    popup.hide();
                    updateImageBox(box, type, buttonLabel);
                }
            };

            // iterate over all our photos and fill the popup panel
            Iterator i = _images.iterator();
            while (i.hasNext()) {
                Image image = new PhotoThumbnailImage(((Photo) i.next()));
                image.addClickListener(logoChanger);
                itemPanel.add(image);
            }

            // finally show the popup
            popup.show();
        }
    }

    protected void updateSubmitButton ()
    {
        _submitButton.setEnabled(_group.name != null &&
            _group.name.length() >= GroupName.LENGTH_MIN &&
            _group.name.length()  <= GroupName.LENGTH_MAX);
    }

    protected void addError (String error)
    {
        _errorContainer.add(new Label(error));
    }

    protected void clearErrors ()
    {
        _errorContainer.clear();
    }

    /**
     * A tiny helper class that carries a Photo in a Widget.
     */
    protected static class PhotoThumbnailImage extends Image {
        public Photo photo;
        protected PhotoThumbnailImage (Photo photo)
        {
            super(MsoyEntryPoint.toMediaPath(photo.getThumbnailPath()));
            this.photo = photo;
        }
    }

    protected GroupContext _ctx;
    protected Group _group;
    protected GroupExtras _extras;
    protected GroupSubmissionListener _listener;
    protected List _images;

    protected HorizontalPanel _errorContainer;
    protected Button _submitButton;

    // static final fields used to decide which image we're working with.
    protected static final int IMAGE_LOGO = 1;
    protected static final int IMAGE_INFO_BACKGROUND = 2;
    protected static final int IMAGE_DETAIL_BACKGROUND = 3;
    protected static final int IMAGE_PEOPLE_BACKGROUND = 4;
}
