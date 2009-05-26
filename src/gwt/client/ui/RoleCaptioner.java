package client.ui;

import client.shell.ShellMessages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.web.gwt.WebCreds;

public class RoleCaptioner
{
    public static Widget add (ThumbBox photo, WebCreds.Role role)
    {
        String roleName;
        String iconPath = null;
        switch (role) {
        case ADMIN:
        case MAINTAINER:
            roleName = _msgs.roleAdmin();
            break;

        case SUPPORT: 
            roleName = _msgs.roleSupport();
            break;

        case SUBSCRIBER:
            roleName = _msgs.roleSubscriber();
            iconPath = "/images/ui/subscriber.gif";
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
        Widget roleUI = new InlineLabel(roleName);
        roleUI.setStyleName("caption");
        if (iconPath != null) {
            roleUI = MsoyUI.createFlowPanel("caption", MsoyUI.createInlineImage(iconPath), roleUI);
        }
        table.setWidget(1, 0, roleUI, 1, "caption");
        table.getFlexCellFormatter().setHorizontalAlignment(
            1, 0, HasHorizontalAlignment.ALIGN_CENTER);
        return table;
    }

    protected static final ShellMessages _msgs = GWT.create(ShellMessages.class);
}
