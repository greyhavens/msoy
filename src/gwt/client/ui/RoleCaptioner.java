package client.ui;

import client.shell.ShellMessages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.web.gwt.WebCreds;

public class RoleCaptioner
{
    public static Widget add (ThumbBox photo, WebCreds.Role role)
    {
        String roleName;
        switch (role) {
        case ADMIN:
        case MAINTAINER:
            roleName = _msgs.roleAdmin();
            break;

        case SUPPORT: 
            roleName = _msgs.roleSupport();
            break;

        case PERMAGUEST:
            roleName = _msgs.roleGuest();
            break;

        default:
            roleName = _msgs.roleUser();
            break;
        }

        SmartTable table = new SmartTable();
        table.setWidget(0, 0, photo);
        table.setText(1, 0, roleName, 1, "caption");
        table.getFlexCellFormatter().setHorizontalAlignment(
            1, 0, HasHorizontalAlignment.ALIGN_CENTER);
        return table;
    }

    protected static final ShellMessages _msgs = GWT.create(ShellMessages.class);
}
