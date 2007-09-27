//
// $Id$

package client.group;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
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
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.RadioButton;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Photo;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.Group;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.web.data.GroupExtras;

import com.threerings.gwt.ui.InlineLabel;

import client.shell.Application;
import client.util.BorderedDialog;
import client.util.MediaUtil;
import client.util.MsoyUI;
import client.util.StyledTabPanel;

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

        String title = _group.groupId == 0 ? CGroup.msgs.editCreateTitle() : _group.name;
        _header.add(createTitleLabel(title, "GroupTitle"));
        VerticalPanel contents = (VerticalPanel)_contents;

        _errorContainer = new HorizontalPanel();
        contents.add(_errorContainer);
        CellPanel groupNameEdit = createTextEntryField(CGroup.msgs.editName(), 
            GroupName.LENGTH_MAX, 20, _group.name, new ChangeListener() {
            public void onChange (Widget sender) {
                _group.name = ((TextBox)sender).getText().trim();
                updateSubmitButton();
            }
        });
        groupNameEdit.addStyleName("GroupNameEdit");
        groupNameEdit.setSpacing(5);
        contents.add(groupNameEdit);

        TabPanel groupTabs = new StyledTabPanel();
        groupTabs.add(createInfoPanel(), CGroup.msgs.editInfoTab());
        groupTabs.add(createDescriptionPanel(), CGroup.msgs.editDescripTab());
        groupTabs.add(createBackgroundsPanel(), CGroup.msgs.editImagesTab());
        groupTabs.selectTab(0);
        contents.add(groupTabs);

        _submitButton = new Button(CGroup.cmsgs.submit());
        updateSubmitButton();
        _submitButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                commitEdit();
            }
        });
        _footer.add(_submitButton);
        Button cancelButton = new Button(CGroup.cmsgs.cancel());
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
        VerticalPanel containerPanel = new VerticalPanel();
        VerticalPanel infoPanel = new VerticalPanel();
        infoPanel.setHorizontalAlignment(VerticalPanel.ALIGN_RIGHT);
        infoPanel.setSpacing(3);
        infoPanel.setStyleName("CurrentPanel");
        CellPanel homepageEntry = createTextEntryField(CGroup.msgs.editHomepage(), 255, 17, 
            _extras.homepageUrl, new ChangeListener() {
                public void onChange (Widget sender) {
                    _extras.homepageUrl = ((TextBox)sender).getText().trim();
                }
            });
        homepageEntry.setSpacing(3);
        infoPanel.add(homepageEntry);

        // make sure the group's configured policy is consistent with what's shown in the GUI
        if (_group.policy == 0) {
            _group.policy = Group.POLICY_PUBLIC;
        }
        HorizontalPanel policyPanel = new HorizontalPanel();
        policyPanel.setSpacing(3);
        policyPanel.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        policyPanel.setWidth("100%");
        InlineLabel policyLabel = new InlineLabel(CGroup.msgs.editPolicy());
        policyPanel.add(policyLabel);
        policyPanel.setCellWidth(policyLabel, "100%");
        final ListBox policyBox = new ListBox();
        policyBox.addItem(CGroup.msgs.policyPublic());
        policyBox.addItem(CGroup.msgs.policyInvite());
        policyBox.addItem(CGroup.msgs.policyExclusive());
        switch(_group.policy) {
        case Group.POLICY_PUBLIC: policyBox.setSelectedIndex(0); break;
        case Group.POLICY_INVITE_ONLY: policyBox.setSelectedIndex(1); break;
        case Group.POLICY_EXCLUSIVE: policyBox.setSelectedIndex(2); break;
        default: addError(CGroup.msgs.errUnknownPolicy("" + _group.policy));
        }
        policyBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget sender) {
                switch(policyBox.getSelectedIndex()) {
                case 0: _group.policy = Group.POLICY_PUBLIC; break;
                case 1: _group.policy = Group.POLICY_INVITE_ONLY; break;
                case 2: _group.policy = Group.POLICY_EXCLUSIVE; break;
                default: addError(CGroup.msgs.errUnknownPolicy("" + policyBox.getSelectedIndex()));
                }
            }
        });
        policyBox.setWidth("160px");
        policyPanel.add(policyBox);
        infoPanel.add(policyPanel);

        VerticalPanel logoBox = new VerticalPanel();
        logoBox.setSpacing(3);
        infoPanel.add(logoBox);
        updateImageBox(logoBox, IMAGE_LOGO, CGroup.msgs.editSetLogo());

        // containerPanel is only here so that infoPanel doesn't take up 100% width
        containerPanel.add(infoPanel);

        return containerPanel;
    }

    protected Panel createDescriptionPanel ()
    {
        VerticalPanel containerPanel = new VerticalPanel();
        VerticalPanel descriptionPanel = new VerticalPanel();
        descriptionPanel.setSpacing(10);
        descriptionPanel.setStyleName("CurrentPanel");

        CellPanel blurbEdit = createTextEntryField(CGroup.msgs.editBlurb(), 80, 43, _group.blurb,
            new ChangeListener() {
                public void onChange (Widget sender) {
                    _group.blurb = ((TextBox)sender).getText().trim();
                }
            });
        blurbEdit.setSpacing(3);
        blurbEdit.setWidth("100%");
        blurbEdit.setCellWidth(((HorizontalPanel)blurbEdit).getWidget(0), "100%");
        descriptionPanel.add(blurbEdit);

        HorizontalPanel charterPanel = new HorizontalPanel();
        charterPanel.setSpacing(3);
        charterPanel.add(new InlineLabel(CGroup.msgs.editCharter()));
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

        // containerPanel is only here so that descriptionPanel doesn't take up 100% width
        containerPanel.add(descriptionPanel);

        return containerPanel;
    }

    protected Panel createBackgroundsPanel ()
    {
        VerticalPanel imagesPanel = new VerticalPanel();
        imagesPanel.setStyleName("CurrentPanel");
        imagesPanel.setHorizontalAlignment(VerticalPanel.ALIGN_CENTER);
        HorizontalPanel backgroundsPanel = new HorizontalPanel();
        backgroundsPanel.setSpacing(10);
        backgroundsPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        HorizontalPanel capsPanel = new HorizontalPanel();
        capsPanel.setSpacing(10);
        capsPanel.setVerticalAlignment(HorizontalPanel.ALIGN_BOTTOM);
        imagesPanel.add(backgroundsPanel);
        imagesPanel.add(capsPanel);

        Panel radiosFieldset = new ComplexPanel () { {
                Element fieldset = DOM.createElement("fieldset");
                DOM.setStyleAttribute(fieldset, "width", "175px");
                Element legend = DOM.createElement("legend");
                DOM.setInnerText(legend, CGroup.msgs.editBackgroundControlLegend());
                DOM.appendChild(fieldset, legend);
                setElement(fieldset);
            }
            public void add (Widget w) {
                add(w, getElement());
            }
        };
        // index of radio must match the value of that radio in the GroupExtras constants
        final RadioButton radios[] = { new RadioButton("backgroundControl", CGroup.msgs.
            editTileBackgrounds()), new RadioButton("backgroundControl", CGroup.msgs.
            editAnchorBackgrounds()), new RadioButton("backgroundControl", CGroup.msgs.
            editFitToImages()) };
        ClickListener radioClickListener = new ClickListener () {
            public void onClick (Widget sender) {
                for (int ii = 0; ii < radios.length; ii++) {
                    if (radios[ii].isChecked()) {
                        _extras.backgroundControl = ii;
                        break;
                    }
                }
            }
        };
        for (int ii = 0; ii < radios.length; ii++) {
            radios[ii].addClickListener(radioClickListener);
            if (ii == _extras.backgroundControl) {
                radios[ii].setChecked(true);
            }
            radiosFieldset.add(radios[ii]);
            if (ii < radios.length - 1) {
               radiosFieldset.add(new Widget () { {
                   setElement(DOM.createElement("br"));
               }});
            }
        }
        capsPanel.add(radiosFieldset);
        capsPanel.setCellVerticalAlignment(radiosFieldset, VerticalPanel.ALIGN_MIDDLE);

        int types[] = { IMAGE_INFO_BACKGROUND, IMAGE_DETAIL_BACKGROUND,
            IMAGE_PEOPLE_BACKGROUND, IMAGE_PEOPLE_UPPER_CAP, IMAGE_PEOPLE_LOWER_CAP };
        String labels[] = { CGroup.msgs.editInfoBG(), CGroup.msgs.editDetailBG(),
                            CGroup.msgs.editPeopleBG(), CGroup.msgs.editPeopleUpperCap(),
                            CGroup.msgs.editPeopleLowerCap() };
        for (int ii = 0; ii < types.length; ii++) {
            VerticalPanel imageBox = new VerticalPanel();
            imageBox.setSpacing(3);
            if (ii < 3) {
                backgroundsPanel.add(imageBox);
            } else {
                capsPanel.add(imageBox);
            }
            updateImageBox(imageBox, types[ii], CGroup.msgs.editSetBG(labels[ii]));
        }
        return imagesPanel;
    }

    protected CellPanel createTextEntryField (String label, int maxLength, int visibleLength,
        String startingText, ChangeListener listener)
    {
        HorizontalPanel textEntryField = new HorizontalPanel();
        textEntryField.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
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
            MsoyUI.error(CGroup.msgs.errInvalidGroupName());
            return;
        }

        final AsyncCallback updateCallback = new AsyncCallback() {
            public void onSuccess (Object result) {
                hide();
                if (_listener != null) {
                    _listener.groupSubmitted(_group);
                } else if (_group.groupId == 0) {
                    // new group created - go to the new group view page
                    Group newGroup = (Group)result;
                    History.newItem(Application.createLinkToken("group", "" + newGroup.groupId));
                }
            }
            public void onFailure (Throwable caught) {
                addError(CGroup.serverError(caught));
            }
        };
        // check if we're trying to set the policy to exclusive on a group that has tags
        if (_group.policy == Group.POLICY_EXCLUSIVE) {
            CGroup.groupsvc.getTags(CGroup.ident, _group.groupId, new AsyncCallback () {
                public void onSuccess (Object result) {
                    if (((Collection)result).size() > 0) {
                        MsoyUI.error(CGroup.msgs.errTagsOnExclusive());
                    } else if (_group.groupId > 0) {
                        CGroup.groupsvc.updateGroup(CGroup.ident, _group, _extras, updateCallback);
                    } else {
                        CGroup.groupsvc.createGroup(CGroup.ident, _group, _extras, updateCallback);
                    }
                } 
                public void onFailure (Throwable caught) {
                    addError(CGroup.serverError(caught));
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

    // update the contents of the image box, e.g. after the image has been changed
    protected void updateImageBox (final CellPanel box, final int type, final String buttonLabel)
    {
        MediaDesc media = null;
        switch (type) {
        case IMAGE_LOGO: media = _group.logo; break;
        case IMAGE_INFO_BACKGROUND: media = _extras.infoBackground; break;
        case IMAGE_DETAIL_BACKGROUND: media = _extras.detailBackground; break;
        case IMAGE_PEOPLE_BACKGROUND: media = _extras.peopleBackground; break;
        case IMAGE_PEOPLE_UPPER_CAP: media = _extras.peopleUpperCap; break;
        case IMAGE_PEOPLE_LOWER_CAP: media = _extras.peopleLowerCap; break;
        default: addError("Internal Error! Unknown image type: " + type); return;
        }

        box.clear();
        if (media != null) {
            Widget w;
            box.add(w = MediaUtil.createMediaView(media, MediaDesc.THUMBNAIL_SIZE));
            box.setCellWidth(w, "160px");
            box.setCellHeight(w, "120px");
            box.setCellHorizontalAlignment(w, VerticalPanel.ALIGN_CENTER);
            box.setCellVerticalAlignment(w, VerticalPanel.ALIGN_MIDDLE);
        }
        Button changeButton = new Button(buttonLabel);
        changeButton.addClickListener(new ClickListener() {
            public void onClick (Widget sender) {
                popupImageChooser(box, type, buttonLabel);
            }
        });
        changeButton.setWidth("160px");
        box.add(changeButton);
    }

    // pop up a scrollable horizontal list of photo items from which to choose a logo
    protected void popupImageChooser (final CellPanel box, final int type, final String buttonLabel)
    {
        // the list of images is cached for this object
        if (_images == null) {
            CGroup.membersvc.loadInventory(CGroup.ident, Item.PHOTO, 0, new AsyncCallback() {
                public void onSuccess (Object result) {
                    _images = (List) result;
                    // will use the cached results this time.
                    popupImageChooser(box, type, buttonLabel);
                }
                public void onFailure (Throwable caught) {
                    CGroup.log("loadInventory failed", caught);
                    addError(CGroup.msgs.errPhotoLoadFailed(CGroup.serverError(caught)));
                }
            });
            return;
        }

        if (_images.size() == 0) {
            addError(CGroup.msgs.errNoPhotos());
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
                    // the background images store the thumbnail's constraint so that they can
                    // be scaled properly in the edit dialog
                    _extras.infoBackground.constraint = photo.thumbMedia.constraint;
                    break;
                case IMAGE_DETAIL_BACKGROUND:
                    _extras.detailBackground = photo.photoMedia;
                    _extras.detailBackground.constraint = photo.thumbMedia.constraint;
                    _extras.detailBackgroundWidth = photo.photoWidth;
                    _extras.detailAreaHeight = photo.photoHeight;
                    break;
                case IMAGE_PEOPLE_BACKGROUND:
                    _extras.peopleBackground = photo.photoMedia;
                    _extras.peopleBackground.constraint = photo.thumbMedia.constraint;
                    break;
                case IMAGE_PEOPLE_UPPER_CAP:
                    _extras.peopleUpperCap = photo.photoMedia;
                    _extras.peopleUpperCapHeight = photo.photoHeight;
                    break;
                case IMAGE_PEOPLE_LOWER_CAP:
                    _extras.peopleLowerCap = photo.photoMedia;
                    _extras.peopleLowerCapHeight = photo.photoHeight;
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
            super(photo.getThumbnailPath());
            this.photo = photo;
        }
    }

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
    protected static final int IMAGE_PEOPLE_UPPER_CAP = 5;
    protected static final int IMAGE_PEOPLE_LOWER_CAP = 6;
}
