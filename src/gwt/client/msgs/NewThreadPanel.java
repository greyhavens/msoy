//
// $Id$

package client.msgs;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.fora.gwt.ForumService;
import com.threerings.msoy.fora.gwt.ForumServiceAsync;
import com.threerings.msoy.fora.gwt.ForumThread;

import client.shell.CShell;
import client.shell.ShellMessages;
import client.ui.MsoyUI;
import client.ui.RowPanel;
import client.util.ServiceUtil;

/**
 * Displays an interface for creating a new thread.
 */
public class NewThreadPanel extends TableFooterPanel
{
    public NewThreadPanel (int groupId, boolean isManager, boolean isAnnounce)
    {
        addStyleName("newThreadPanel");
        _groupId = groupId;

        addRow(_mmsgs.ntpSubject(), _subject = new TextBox());
        _subject.setMaxLength(ForumThread.MAX_SUBJECT_LENGTH);
        _subject.setVisibleLength(40);

        final Button[] spambuts = new Button[2];
        if (isManager) {
            RowPanel bits = new RowPanel();
            bits.add(_announce = new CheckBox());
            bits.add(MsoyUI.createLabel(isAnnounce ? _mmsgs.ntpMustAnnounceTip() :
                                        _mmsgs.ntpAnnounceTip(), "FlagTip"));
            addRow(_mmsgs.ntpAnnounce(), bits);
            // force announce to be checked for the global announce group
            _announce.setChecked(isAnnounce);
            _announce.setEnabled(!isAnnounce);

            if (isAnnounce && CShell.isSupport()) {
                bits = new RowPanel();
                bits.add(_spam = new CheckBox());
                bits.add(MsoyUI.createLabel(_mmsgs.ntpSpamTip(), "FlagTip"));
                addRow(_mmsgs.ntpSpam(), bits);

                spambuts[0] = new Button(_mmsgs.ntpSendPreview());
                spambuts[0].setTitle(_mmsgs.ntpSendPreviewTip());
                spambuts[0].setEnabled(false);
                new ForumCallback<Void>(spambuts[0]) {
                    public boolean callService () {
                        return sendPreviewEmail(false, this);
                    }
                    public boolean gotResult (Void result) {
                        MsoyUI.info(_mmsgs.ntpPreviewSent());
                        return true;
                    }
                };

                spambuts[1] = new Button(_mmsgs.ntpSendProbe());
                spambuts[1].setTitle(_mmsgs.ntpSendProbeTip());
                spambuts[1].setEnabled(false);
                new ForumCallback<Void>(spambuts[1]) {
                    public boolean callService () {
                        return sendPreviewEmail(true, this);
                    }
                    public boolean gotResult (Void result) {
                        MsoyUI.info(_mmsgs.ntpProbeSent());
                        return true;
                    }
                };

                _spam.addClickListener(new ClickListener() {
                    public void onClick (Widget sender) {
                        for (Button b : spambuts) {
                            b.setEnabled(_spam.isChecked());
                        }
                    }
                });
            }

            bits = new RowPanel();
            bits.add(_sticky = new CheckBox());
            bits.add(MsoyUI.createLabel(_mmsgs.ntpStickyTip(), "FlagTip"));
            addRow(_mmsgs.ntpSticky(), bits);
        }

        addRow(WidgetUtil.makeShim(5, 5));
        addRow(_mmsgs.ntpFirstMessage());
        addRow(_message = new MessageEditor());

        addFooterButton(new Button(_cmsgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                ((ForumPanel)getParent()).newThreadCanceled(_groupId);
            }
        }));

        // add our spam-related buttons if we have them
        for (Button b : spambuts) {
            if (b != null) {
                addFooterButton(b);
            }
        }

        Button submit = new Button(_mmsgs.ntpSubmit());
        new ForumCallback<ForumThread>(submit) {
            public boolean callService () {
                return submitNewThread(this);
            }
            public boolean gotResult (ForumThread result) {
                ((ForumPanel)getParent()).newThreadPosted(result);
                return false;
            }
            @Override protected String getConfirmMessage () {
                return (_spam != null && _spam.isChecked()) ? _mmsgs.ntpSpamConfirm() : null;
            }
        };
        addFooterButton(submit);
    }

    protected boolean sendPreviewEmail (boolean includeProbes, ForumCallback<Void> callback)
    {
        String subject = _subject.getText().trim();
        if (subject.length() == 0) {
            MsoyUI.error(_mmsgs.errNoSubject());
            return false;
        }

        String message = _message.getHTML();
        if (message.length() == 0) {
            MsoyUI.error(_mmsgs.errNoMessage());
            return false;
        }
        _forumsvc.sendPreviewEmail(subject, message, includeProbes, callback);
        return true;
    }

    protected boolean submitNewThread (ForumCallback<ForumThread> callback)
    {
        String subject = _subject.getText().trim();
        if (subject.length() == 0) {
            MsoyUI.error(_mmsgs.errNoSubject());
            return false;
        }

        String message = _message.getHTML();
        if (message.length() == 0) {
            MsoyUI.error(_mmsgs.errNoMessage());
            return false;
        }

        int flags = 0;
        if (_announce != null && _announce.isChecked()) {
            flags |= ForumThread.FLAG_ANNOUNCEMENT;
        }
        if (_sticky != null && _sticky.isChecked()) {
            flags |= ForumThread.FLAG_STICKY;
        }
        boolean spam = (_spam != null && _spam.isChecked());
        _forumsvc.createThread(_groupId, flags, spam, subject, message, callback);
        return true;
    }

    protected int _groupId;
    protected TextBox _subject;
    protected CheckBox _announce, _sticky, _spam;
    protected MessageEditor _message;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MsgsMessages _mmsgs = (MsgsMessages)GWT.create(MsgsMessages.class);
    protected static final ForumServiceAsync _forumsvc = (ForumServiceAsync)
        ServiceUtil.bind(GWT.create(ForumService.class), ForumService.ENTRY_POINT);
}
