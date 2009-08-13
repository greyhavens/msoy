//
// $Id$

package client.facebook;

import client.shell.ShellMessages;
import client.ui.BorderedDialog;
import client.ui.MsoyUI;
import client.util.InfoCallback;
import client.util.Link;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.threerings.msoy.facebook.gwt.FacebookGame;
import com.threerings.msoy.facebook.gwt.FacebookService;
import com.threerings.msoy.facebook.gwt.FacebookServiceAsync;
import com.threerings.msoy.facebook.gwt.FacebookService.StoryFields;
import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.Pages;

/**
 * Just shows 3 buttons that direct to different target audiences.
 */
public class FBChallengeSelectPanel extends FlowPanel
{
    public FBChallengeSelectPanel (FacebookGame game, String gameName)
    {
        _game = game;
        setStyleName("challengeSelect");
        add(MsoyUI.createLabel(_msgs.challengeSelect(gameName), "Title"));
        HorizontalPanel buttons = new HorizontalPanel();
        buttons.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);
        buttons.setStyleName("Buttons");
        buttons.add(easyButton(_msgs.challengeAllFriendsBtn(), "AllFriends", new ClickHandler() {
            public void onClick (ClickEvent event) {
                confirmAndSendChallenge(false);
            }
        }));
        buttons.add(easyButton(_msgs.challengeAppFriendsBtn(), "AppFriends", new ClickHandler() {
            public void onClick (ClickEvent event) {
                confirmAndSendChallenge(true);
            }
        }));
        buttons.add(easyButton(_msgs.challengeSomeFriendsBtn(), "SomeFriends",
            Link.createHandler(Pages.FACEBOOK, _game.getChallengeArgs(),
                ArgNames.FB_CHALLENGE_PICK)));
        add(buttons);
    }

    protected PushButton easyButton (String text, String style, ClickHandler handler)
    {
        PushButton button = MsoyUI.createImageButton("easyButton", handler);
        button.setText(text);
        button.addStyleName(style);
        return button;
    }

    protected void confirmAndSendChallenge (final boolean appOnly)
    {
        final Command send = new Command() {
            @Override public void execute () {
                _fbsvc.sendChallengeNotification(_game, appOnly, new InfoCallback<StoryFields>() {
                    @Override public void onSuccess (StoryFields result) {
                        if (result == null || result.template == null) {
                            goPlay();
                            return;
                        }

                        // publish to feed
                        FBChallengeFeeder feeder = new FBChallengeFeeder(_game);
                        feeder.publish(result);
                    }
                });
            }
        };

        BorderedDialog confirm = new BorderedDialog() {
            /* Constructor() */ {
                setStyleName("challengeConfirm");
                setHeaderTitle(_msgs.challengeConfirmTitle());
                setContents(MsoyUI.createLabel(appOnly ? _msgs.challengeAppFriendsConfirm() :
                    _msgs.challengeAllFriendsConfirm(), "Content"));
                addButton(new Button(_msgs.challengeSendBtn(), onAction(send)));
                addButton(new Button(_msgs.challengeCancelBtn(), onAction(null)));
            }
        };
        confirm.show();
    }

    protected void goPlay ()
    {
        Link.go(_game.getPlayPage(), _game.getPlayArgs());
    }

    protected FacebookGame _game;

    protected static final FacebookMessages _msgs = GWT.create(FacebookMessages.class);
    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final FacebookServiceAsync _fbsvc = GWT.create(FacebookService.class);
}
