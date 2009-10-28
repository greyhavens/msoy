//
// $Id: GroupEdit.java 18019 2009-09-04 19:24:25Z zell $

package client.groups;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.Theme;
import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.web.gwt.Pages;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.money.data.all.PurchaseResult;

import client.groups.GroupsPage.Nav;
import client.money.BuyPanel;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.ShellMessages;
import client.util.Link;
import client.util.InfoCallback;

/**
 * A popup that lets a member of sufficient rank modify a theme's metadata.
 */
public class ThemeEdit extends FlexTable
{
    /**
     * Create a new theme.
     *
     * Please note that this constructor ends up triggering a service request to
     * get the PriceQuote for creating a theme.
     */
    public ThemeEdit (Group group)
    {
        this(group, null);
    }

    /**
     * Edit an existing theme.
     */
    public ThemeEdit (Group group, Theme theme)
    {
        // permaguests are not allowed to create themes
        String error = CShell.isRegistered() ?
            (CShell.isValidated() ? null : _msgs.editMustValidate()) : _msgs.editMustRegister();
        if (error != null) {
            setHTML(0, 0, error);
            getFlexCellFormatter().setStyleName(0, 0, "infoLabel");
            return;
        }

        boolean isCreate;
        if (theme == null) {
            theme = new Theme(group.getName());
            isCreate = true;
        } else {
            isCreate = false;
        }

        _group = group;
        _theme = theme;

        setStyleName("groupEditor");
        setCellSpacing(5);
        setCellPadding(0);

        _name = group.name;

        CShell.frame.setTitle(isCreate ? _msgs.editCreateTitle() : _name);

        addRow(_msgs.editLogo(), _logo = new PhotoChoiceBox(true, null));
        _logo.setMedia(_theme.getLogo());

        addRow(_msgs.editPlayOnEnter(), _playOnEnter = new CheckBox());
        _playOnEnter.setValue(_theme.playOnEnter);

        HorizontalPanel footer = new HorizontalPanel();
        footer.add(_cancel = new Button(_cmsgs.cancel(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                Link.go(Pages.GROUPS, Nav.DETAIL.composeArgs(_theme.getGroupId()));
            }
        }));
        footer.add(WidgetUtil.makeShim(5, 5));
        if (isCreate) {
            footer.add(new ThemeBuyPanel().createPromptHost(_msgs.createNewTheme()));

        } else {
            footer.add(_submit = new Button(_cmsgs.change(), new ClickHandler() {
                public void onClick (ClickEvent event) {
                    updateGroup();
                }
            }));
        }
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

    /**
     * Copy settings in UI fields back into _group and _extras.
     *
     * @return true if we're ready to go
     */
    protected boolean commitEdits ()
    {
        _theme.logo = _logo.getMedia();
        if (_playOnEnter != null) {
            _theme.playOnEnter = _playOnEnter.getValue();
        }
        return true;
    }

    /**
     * Called to save changes when editing an existing group.
     */
    protected void updateGroup ()
    {
        if (commitEdits()) {
            _groupsvc.updateTheme(_theme, new InfoCallback<Void>(_submit) {
                public void onSuccess (Void result) {
                    Link.go(Pages.GROUPS, "d", _theme.getGroupId(), "r");
                }
            });
        }
    }

    protected class ThemeBuyPanel extends BuyPanel<Theme>
    {
        public ThemeBuyPanel ()
        {
            _groupsvc.quoteCreateTheme(new InfoCallback<PriceQuote>(_cancel) {
                public void onSuccess (PriceQuote quote) {
                    init(quote, new AsyncCallback<Theme>() {
                        public void onSuccess (Theme theme) {
                            Link.go(Pages.GROUPS, "d", theme.getGroupId(), "r");
                        }
                        public void onFailure (Throwable t) {} /* not used */
                    });
                }
            });
        }

        @Override
        protected boolean makePurchase (
            Currency currency, int amount, AsyncCallback<PurchaseResult<Theme>> listener)
        {
            boolean editsOk = commitEdits();
            if (editsOk) {
                _groupsvc.createTheme(_theme, currency, amount, listener);
            }
            return editsOk;
        }
    }

    protected Theme _theme;
    protected Group _group;
    protected String _name;

    protected PhotoChoiceBox _logo;
    protected Button _cancel, _submit;
    protected CheckBox _playOnEnter;

    protected static final GroupsMessages _msgs = GWT.create(GroupsMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);
}
