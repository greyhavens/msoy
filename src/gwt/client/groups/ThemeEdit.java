//
// $Id: GroupEdit.java 18019 2009-09-04 19:24:25Z zell $

package client.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.Theme;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import client.edutil.EditorTable;
import client.edutil.EditorUtil;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.util.ClickCallback;

/**
 * A popup that lets a member of sufficient rank modify a theme's metadata.
 */
public class ThemeEdit extends FlowPanel
{
    /**
     * Edit an existing theme.
     */
    public ThemeEdit (Group group, Theme theme)
    {
        _group = group;
        _theme = theme;
        _name = group.name;

        setStyleName("themeEditor");

        // permaguests are not allowed to create themes
        String error = CShell.isRegistered() ?
            (CShell.isValidated() ? null : _msgs.editMustValidate()) : _msgs.editMustRegister();
        if (error != null) {
            add(MsoyUI.createHTML(error, "infoLabel"));
            return;
        }

        SmartTable header = new SmartTable("Header", 0, 10);
        header.setText(0, 0, _name, 1, "Title");
        header.setWidget(0, 1, MsoyUI.createHTML(_msgs.etTip(), null), 1, "Tip");
        add(header);

        add(new ThemeEditorTable());
    }

    protected class ThemeEditorTable extends EditorTable
    {
        protected ThemeEditorTable ()
        {
            // bits for editing the logo
            final MediaBox tbox =
                new MediaBox(MediaDesc.LOGO_SIZE, Theme.LOGO_MEDIA, _theme.getLogo()) {
                @Override public void mediaUploaded (String name, MediaDesc desc, int w, int h) {
                    if (checkSize(w, h)) {
                        super.mediaUploaded(name, desc, w, h);
                    }
                }
            };
            addRow(_msgs.etLogo(), _msgs.etLogoTip(), tbox, new Command() {
                public void execute () {
                    _theme.logo = EditorUtil.requireImageMedia(
                        _msgs.editLogo(), tbox.getMedia());
                }
            });

            // bits for editing the tab navigation button
            final MediaBox navBox =
                new MediaBox(MediaDesc.NAV_SIZE, Theme.NAV_MEDIA, _theme.getNavButton()) {
                @Override public void mediaUploaded (String name, MediaDesc desc, int w, int h) {
                    if (checkSize(w, h)) {
                        super.mediaUploaded(name, desc, w, h);
                    }
                }
            };
            addRow(_msgs.etNavBtn(), _msgs.etNavBtnTip(), navBox, new Command() {
                public void execute () {
                    _theme.navButton = EditorUtil.requireImageMedia(
                        _msgs.editLogo(), navBox.getMedia());
                }
            });

            // bits for editing the selected tab navigation button
            final MediaBox navSelBox =
                new MediaBox(MediaDesc.NAV_SIZE, Theme.NAV_SEL_MEDIA, _theme.getNavSelButton()) {
                @Override public void mediaUploaded (String name, MediaDesc desc, int w, int h) {
                    if (checkSize(w, h)) {
                        super.mediaUploaded(name, desc, w, h);
                    }
                }
            };
            addRow(_msgs.etNavSelBtn(), _msgs.etNavSelBtnTip(), navSelBox, new Command() {
                public void execute () {
                    _theme.navSelButton = EditorUtil.requireImageMedia(
                        _msgs.editLogo(), navSelBox.getMedia());
                }
            });

            // bits for editing the background colour
            final TextBox bgColBox = MsoyUI.createTextBox(hexColor(_theme.backgroundColor), 6, 6);
            addRow(_msgs.etBackgroundColor(), _msgs.etBgColTip(), bgColBox, new Command() {
                public void execute () {
                    _theme.backgroundColor = Integer.valueOf(bgColBox.getText(), 16);
                }
            });

            final CheckBox poeBox = new CheckBox();
            poeBox.setValue(_theme.playOnEnter);
            addRow(_msgs.etPlayOnEnter(), _msgs.etPoeTip(), poeBox, new Command() {
                public void execute () {
                    _theme.playOnEnter = poeBox.getValue();
                }
            });

            Button save = addSaveRow();
            new ClickCallback<Void>(save) {
                protected boolean callService () {
                    if (!bindChanges()) {
                        return false;
                    }
                    _groupsvc.updateTheme(_theme, this);
                    return true;
                }
                protected boolean gotResult (Void result) {
                    MsoyUI.info("Theme updated.");
                    return true;
                }
            };
        }
    }

    protected Theme _theme;
    protected Group _group;
    protected String _name;

    protected static String hexColor (int rgb)
    {
        String str = ("000000" + Integer.toHexString(rgb));
        return str.substring(str.length() - 6);
    }

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);
}
