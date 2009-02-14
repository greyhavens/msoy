//
// $Id$

package client.ui;

import com.google.gwt.http.client.URL;

import com.google.gwt.user.client.Window;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;

import client.shell.CShell;

public class ShareDialog extends BorderedDialog
{
    public ShareDialog (
        String token, String title, String desc, final MediaDesc image)
    {
        super(true); // autohide
        setHeaderTitle("Share this shit");

        SmartTable panel = new SmartTable();

        String goURL = URL.encodeComponent(DeploymentConfig.serverURL + "go/" + token);
        final String welcURL = URL.encodeComponent(DeploymentConfig.serverURL + "welcome/" +
            (CShell.isGuest() ? "0" : CShell.getMemberId()) + "/" + token);
        final String eTitle = URL.encodeComponent(title);
        final String eDesc = URL.encodeComponent(desc);

        // facebook
        ClickListener facebookListener = new ClickListener() {
            public void onClick (Widget sender) {
                String facebookURL = "http://www.facebook.com/sharer.php" +
                    "?u=" + welcURL + "&t=" + eTitle;
                Window.open(facebookURL, "Whirled", "width=620,height=440");
            }
        };
        panel.setWidget(0, 1, MsoyUI.createButtonPair(
            MsoyUI.createActionImage(
                "http://b.static.ak.fbcdn.net/images/share/facebook_share_icon.gif?8:26981",
                facebookListener),
            MsoyUI.createActionLabel("Share on Facebook", facebookListener)));

        // myspace
        ClickListener myspaceListener = new ClickListener() {
            public void onClick (Widget sender) {
                String myspaceURL = "http://www.myspace.com/index.cfm" +
                    "?fuseaction=postto" +
                    "&u=" + welcURL + "&t=" + eTitle + "&l=1" +
                    // TODO: change this to the embed, and not the snapshot?
                    "&c=" + URL.encodeComponent("<img src='" + image.getMediaPath() + "'>");
                Window.open(myspaceURL, "Whirled", "width=1024,height=650");
            }
        };
        panel.setWidget(0, 2, MsoyUI.createButtonPair(
            MsoyUI.createActionImage(
                "http://cms.myspacecdn.com/cms/post_myspace_icon.gif", myspaceListener),
            MsoyUI.createActionLabel("Share on MySpace", myspaceListener)));

        // Digg:
        // ... is a little different: we need to use the "go" url because all submitted
        // urls should be the same. We also just create a raw HTML link that opens
        // a new page
        String diggURL = "http://digg.com/submit" +
            "?url=" + goURL + "&title=" + eTitle + "&bodytext=" + eDesc +
            "&media=news&topic=playable_web_games";
        panel.setWidget(0, 0, new HTML("<a target='_blank' href='" + diggURL + "'>" +
            "<img src='/images/ui/digg.png' border=0></a>"));

        setContents(panel);
    }
}
