//
// $Id$

package client.support;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.underwire.gwt.client.MyPetitionsPanel;
import com.threerings.underwire.gwt.client.WebContext;
import com.threerings.underwire.gwt.client.UUI;

import com.threerings.underwire.web.data.Event;
import com.threerings.underwire.web.data.Message;
import com.threerings.underwire.web.data.UserPetition;

import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays an interface for submitting a new petition.
 */
public class NewPetitionBox extends FlowPanel
{
    public NewPetitionBox (WebContext ctx, MyPetitionsPanel parent)
    {
        _ctx = ctx;
        _parent = parent;
        _anonymous = _ctx.ainfo == null;

        add(MsoyUI.createLabel(_ctx.cmsgs.fileNewPetition(), "Title"));

        FlexTable create = new FlexTable();
        create.setStyleName("uRegisterPetition");

        int row = 0;
        create.setText(row, 0, (_anonymous ? CSupport.msgs.newAnonymousPetitionHelp() :
                                CSupport.msgs.newPetitionHelp()));
        create.getFlexCellFormatter().setColSpan(row++, 0, 2);

        KeyboardListenerAdapter clearer = new KeyboardListenerAdapter() {
            public void onKeyPress (Widget sender, char keyCode, int modifiers) {
                _status.setText("");
            }
        };

        HorizontalPanel rowp = new HorizontalPanel();
        if (_anonymous) {
            create.setText(row, 0, _ctx.cmsgs.email());
            create.setWidget(row++, 1, _email = new TextBox());
            _email.setVisibleLength(SMALL_VISIBLE_COLUMNS);
            _email.setMaxLength(MAX_EMAIL_LENGTH);
            _email.addKeyboardListener(clearer);
            create.setText(row, 0, CSupport.msgs.permaName());
            rowp.add(_permaname = new TextBox());
            rowp.add(new Label(CSupport.msgs.optional()));
            create.setWidget(row++, 1, rowp);
            _permaname.setVisibleLength(SMALL_VISIBLE_COLUMNS);
            _permaname.setMaxLength(MAX_PERMANAME_LENGTH);

        } else {
            rowp.add(_curemail = new Label(_ctx.cmsgs.newPetitionEmail(_ctx.ainfo.email)));
            rowp.add(Link.create(_ctx.cmsgs.newPetitionUpdate(), Pages.ME, "account"));
            create.setWidget(row, 0, rowp);
            create.getFlexCellFormatter().setColSpan(row++, 0, 2);
        }

        create.setText(row, 0, _ctx.cmsgs.newPetitionMore());
        create.getFlexCellFormatter().setColSpan(row++, 0, 2);

        create.setText(row, 0, CSupport.msgs.supportArea());
        create.getFlexCellFormatter().setWordWrap(row, 0, false);
        _supportAreas = new ListBox();
        _supportAreas.addItem(CSupport.msgs.saTech());
        _supportAreas.addItem(CSupport.msgs.saBan());
        _supportAreas.addItem(CSupport.msgs.saBill());
        _supportAreas.addItem(CSupport.msgs.saOther());
        create.setWidget(row++, 1, _supportAreas);

        create.setText(row, 0, _ctx.cmsgs.subject());
        create.setWidget(row++, 1, _newsub = new TextBox());
        // for some annoying reason textbox takes up more horizontal space than textarea
        _newsub.setVisibleLength(VISIBLE_COLUMNS);
        _newsub.setMaxLength(MAX_SUBJECT_LENGTH);
        _newsub.addKeyboardListener(clearer);

        create.setText(row, 0, _ctx.cmsgs.message());
        create.setWidget(row++, 1, _newmsg = new TextArea());
        _newmsg.addKeyboardListener(clearer);
        _newmsg.setCharacterWidth(VISIBLE_COLUMNS);
        _newmsg.setVisibleLines(VISIBLE_ROWS);

        ClickListener submitListener = new ClickListener() {
            public void onClick (Widget sender) {
                submitPetition((Button)sender);
            }
        };
        create.setWidget(row, 1, new Button(ctx.cmsgs.submitBtn(), submitListener));
        /*
          if (_anonymous) {
          } else {
          create.setWidget(row, 1, UUI.createSubmitCancel(_ctx, this, submitListener));
          }
        */
        create.getFlexCellFormatter().setAlignment(
            row++, 1, VerticalPanel.ALIGN_RIGHT, VerticalPanel.ALIGN_TOP);
        add(create);

        add(_status = new Label(""));
    }

    protected void submitPetition (final Button submitBtn)
    {
        final UserPetition petition = new UserPetition();
        petition.status = Event.OPEN;
        petition.subject = _newsub.getText().trim();
        petition.entered = new Date();
        if (petition.subject.length() == 0) {
            _status.setText(_ctx.cmsgs.errMissingSubject());
            return;
        }
        petition.subject = "(" + _supportAreas.getItemText(_supportAreas.getSelectedIndex()) +
            ") " + petition.subject;

        String message = _newmsg.getText().trim();
        if (message.length() == 0) {
            _status.setText(_ctx.cmsgs.errMissingMessage());
            return;
        }

        if (_anonymous) {
            final String email = _email.getText().trim();
            if (email.length() < 4 || email.indexOf("@") == -1) {
                _status.setText(CSupport.msgs.errMissingEmail());
                return;
            }
            String perma = _permaname.getText().trim();
            if (perma.length() > 0) {
                message = "Permaname: " + perma + "\n\n" + message;
            }

            submitBtn.setEnabled(false);
            _ctx.undersvc.registerAnonymousPetition(email, petition, message, new AsyncCallback() {
                public void onSuccess (Object result) {
                    submitSuccess();
                }
                public void onFailure (Throwable cause) {
                    submitFailure(submitBtn, cause);
                }
            });

        } else {
            submitBtn.setEnabled(false);

            final String fmessage = message;
            _ctx.undersvc.registerPetition(
                _ctx.ainfo.authtok, petition, fmessage, new AsyncCallback<Integer>() {
                public void onSuccess (Integer result) {
                    submitSuccess();

                    petition.eventId = result.intValue();

                    // add our message to the petition (since we did not load this from the database
                    // the message is not already associated with the petition)
                    Message msgrec = new Message();
                    msgrec.author = _ctx.ainfo.name;
                    msgrec.authored = petition.entered;
                    msgrec.body = fmessage;
                    petition.messages.add(msgrec);

                    // tell our parent to display the new petition
                    _parent.addPetition(petition);
                }

                public void onFailure (Throwable cause) {
                    submitFailure(submitBtn, cause);
                }
            });
        }
    }

    protected void submitFailure (Button submitBtn, Throwable cause)
    {
        GWT.log("Failed to register petition.", cause);
        submitBtn.setEnabled(true);
        _status.setText(_ctx.serverError(cause));
    }

    protected void submitSuccess ()
    {
        clear();
        add(MsoyUI.createLabel(CSupport.msgs.submitSuccessTitle(), "Title"));
        add(MsoyUI.createLabel(CSupport.msgs.submitSuccessMsg(), null));
    }

    protected WebContext _ctx;
    protected MyPetitionsPanel _parent;
    protected boolean _anonymous;

    protected Label _curemail;
    protected TextBox _newsub;
    protected TextArea _newmsg;
    protected Label _status;
    protected TextBox _email;
    protected TextBox _permaname;
    protected ListBox _supportAreas;

    protected static final int MAX_SUBJECT_LENGTH = 180;
    protected static final int MAX_EMAIL_LENGTH = 100;
    protected static final int MAX_PERMANAME_LENGTH = 30;

    protected static final int SMALL_VISIBLE_COLUMNS = 30;
    protected static final int VISIBLE_COLUMNS = 60;
    protected static final int VISIBLE_ROWS = 4;
}
