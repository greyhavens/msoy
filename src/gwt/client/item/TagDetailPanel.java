//
// $Id$

package client.item;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.TagCodes;

import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;

import com.threerings.msoy.web.gwt.TagHistory;

import client.shell.CShell;
import client.ui.BorderedDialog;
import client.ui.MsoyUI;
import client.ui.PopupMenu;
import client.ui.PromptPopup;
import client.ui.RowPanel;
import client.util.MsoyCallback;
import client.util.ServiceUtil;
import client.shell.ShellMessages;

/**
 * Displays tagging information for a particular item.
 */
public class TagDetailPanel extends VerticalPanel
{
    /**
     * Interface to the interaction between this panel and the service handling tagging in
     * the background.
     */
    public interface TagService
    {
        void tag (String tag, AsyncCallback<TagHistory> callback);
        void untag (String tag, AsyncCallback<TagHistory> callback);
        void getRecentTags (AsyncCallback<List<TagHistory>> callback);
        void getTags (AsyncCallback<List<String>> callback);

        /**
         * If additional entries are required on the Menu that pops up when a tag is clicked, this
         * method can add menu items for that purpose.
         */
        void addMenuItems (String tag, PopupMenu menu);
    }

    /**
     * Interface to the interaction between this panel and the service handling flagging in
     * the background.
     */
    public interface FlagService
    {
        void addFlag (ItemFlag.Kind kind, String comment);
    }

    public TagDetailPanel (
        TagService service, FlagService flagger, List<String> tags, boolean showAddUI)
    {
        setStyleName("tagDetailPanel");
        _service = service;
        _flagger = flagger;
        _canEdit = !CShell.isGuest() && showAddUI;

        _tags = new FlowPanel();
        _tags.setStyleName("Tags");
        _tags.add(new Label(_cmsgs.tagLoading()));
        add(_tags);

        if (_canEdit) {
            RowPanel addRow = new RowPanel();
            addRow.add(new InlineLabel(_cmsgs.tagAddTag(), false, false, false),
                       HasAlignment.ALIGN_MIDDLE);
            addRow.add(new NewTagBox());

//             addRow.add(_quickTagLabel = new Label(_cmsgs.tagQuickAdd()));
//             addRow.add(_quickTags = new ListBox());
//             _quickTags.addChangeListener(new ChangeListener() {
//                 public void onChange (Widget sender) {
//                     ListBox box = (ListBox) sender;
//                     String value = box.getValue(box.getSelectedIndex());
//                     _service.tag(value, new AsyncCallback<TagHistory>() {
//                         public void onSuccess (TagHistory result) {
//                             refreshTags();
//                         }
//                         public void onFailure (Throwable caught) {
//                             GWT.log("tagItem failed", caught);
//                             MsoyUI.error(CShell.serverError(caught));
//                         }
//                     });
//                 }
//             });

            if (_flagger != null) {
                InlineLabel flagLabel = new InlineLabel(_cmsgs.tagFlag());
                new PopupMenu(flagLabel) {
                    protected void addMenuItems () {
                        addCommand(_cmsgs.tagMatureFlag(), ItemFlag.Kind.MATURE);
                        addCommand(_cmsgs.tagCopyrightFlag(), ItemFlag.Kind.COPYRIGHT);
                    }
                    protected void addCommand (String label, ItemFlag.Kind kind) {
                        addMenuItem(label, new FlagCommand(kind));
                    }
                };
                addRow.add(flagLabel, HasAlignment.ALIGN_MIDDLE);
            }

            add(WidgetUtil.makeShim(5, 5));
            add(addRow);
        }

        if (tags == null) {
            refreshTags();
        } else {
            gotTags(tags);
        }
    }

    protected void toggleTagHistory ()
    {
        // TODO: if this is used again, it will need to be abstracted like everything else in this
        // class
//         if (_tagHistory != null) {
//             if (_content.getWidgetDirection(_tagHistory) == null) {
//                 _content.add(_tagHistory, DockPanel.EAST);
//             } else {
//                 _content.remove(_tagHistory);
//             }
//             return;
//         }

//         _itemsvc.getTagHistory(_itemId, new AsyncCallback<List<TagHistory>>() {
//             public void onSuccess (List<TagHistory> result) {
//                 _tagHistory = new FlexTable();
//                 _tagHistory.setBorderWidth(0);
//                 _tagHistory.setCellSpacing(0);
//                 _tagHistory.setCellPadding(2);

//                 int tRow = 0;
//                 Iterator<TagHistory> iterator = result.iterator();
//                 while (iterator.hasNext()) {
//                     TagHistory history = iterator.next();
//                     String date = history.time.toGMTString();
//                     // Fri Sep 29 2006 12:46:12
//                     date = date.substring(0, 23);
//                     _tagHistory.setText(tRow, 0, date);
//                     _tagHistory.setText(tRow, 1, history.member.toString());
//                     String actionString;
//                     switch(history.action) {
//                     case TagHistory.ACTION_ADDED:
//                         actionString = "added";
//                         break;
//                     case TagHistory.ACTION_COPIED:
//                         actionString = "copied";
//                         break;
//                     case TagHistory.ACTION_REMOVED:
//                         actionString = "removed";
//                         break;
//                     default:
//                         actionString = "???";
//                         break;
//                     }
//                     _tagHistory.setText(tRow, 2, actionString);
//                     _tagHistory.setText(
//                         tRow, 3, history.tag == null ? "N/A" : "'" + history.tag + "'");
//                     tRow ++;
//                 }
//                 _content.add(_tagHistory, DockPanel.EAST);
//             }

//             public void onFailure (Throwable caught) {
//                 GWT.log("getTagHistory failed", caught);
//                 MsoyUI.error("Internal error fetching item tag history: " + caught.getMessage());
//             }
//         });
    }

    protected void refreshTags ()
    {
        _service.getTags(new AsyncCallback<List<String>>() {
            public void onSuccess (List<String> tags) {
                gotTags(tags);
            }
            public void onFailure (Throwable caught) {
                _tags.clear();
                _tags.add(new Label(CShell.serverError(caught)));
            }
        });
    }

    protected void gotTags (List<String> tags)
    {
        _tags.clear();
        _tags.add(new InlineLabel("Tags:", false, false, true));

        final List<String> addedTags = new ArrayList<String>();
        for (Iterator<String> iter = tags.iterator(); iter.hasNext() ; ) {
            final String tag = iter.next();
            InlineLabel tagLabel = new InlineLabel(tag);
            if (_canEdit) {
                final Command remove = new Command() {
                    public void execute () {
                        _service.untag(tag, new MsoyCallback<TagHistory>() {
                            public void onSuccess (TagHistory result) {
                                refreshTags();
                            }
                        });
                    }
                };
                new PopupMenu(tagLabel) {
                    protected void addMenuItems () {
                        _service.addMenuItems(tag, this);
                        addMenuItem(_cmsgs.tagRemove(),
                            new PromptPopup(_cmsgs.tagRemoveConfirm(tag), remove));
                    }
                };
            }
            _tags.add(tagLabel);
            addedTags.add(tag);
            if (iter.hasNext()) {
                _tags.add(new InlineLabel(", "));
            }
        }

        if (addedTags.size() == 0) {
            _tags.add(new InlineLabel("none"));
        }

//         if (!CShell.isGuest()) {
//             _service.getRecentTags(new MsoyCallback<List<TagHistory>>() {
//                 public void onSuccess (List<TagHistory> result) {
//                     _quickTags.clear();
//                     _quickTags.addItem(_cmsgs.tagSelectOne());
//                     for (TagHistory history : result) {
//                         String tag = history.tag;
//                         if (tag != null && !addedTags.contains(tag) &&
//                             history.member.getMemberId() == CShell.getMemberId()) {
//                             _quickTags.addItem(tag);
//                             addedTags.add(tag);
//                         }
//                     }
//                     boolean visible = _quickTags.getItemCount() > 1;
//                     _quickTags.setVisible(visible);
//                     _quickTagLabel.setVisible(visible);
//                 }
//             });
//         }
    }

    protected class NewTagBox extends TextBox
        implements ClickListener
    {
        public NewTagBox () {
            setMaxLength(20);
            setVisibleLength(12);
            addKeyboardListener(new EnterClickAdapter(this));
        }

        public void onClick (Widget sender) {
            String tagName = getText().trim().toLowerCase();
            if (tagName.length() == 0) {
                return;
            }
            if (tagName.length() < TagCodes.MIN_TAG_LENGTH) {
                MsoyUI.error(_cmsgs.errTagTooShort("" + TagCodes.MIN_TAG_LENGTH));
                return;
            }
            if (tagName.length() > TagCodes.MAX_TAG_LENGTH) {
                MsoyUI.error(_cmsgs.errTagTooLong("" + TagCodes.MAX_TAG_LENGTH));
                return;
            }
            for (int ii = 0; ii < tagName.length(); ii ++) {
                char c = tagName.charAt(ii);
                if (c == '_' || Character.isLetterOrDigit(c)) {
                    continue;
                }
                MsoyUI.error(_cmsgs.errTagInvalidCharacters());
                return;
            }
            _service.tag(tagName, new MsoyCallback<TagHistory>() {
                public void onSuccess (TagHistory result) {
                    refreshTags();
                }
            });
            setText(null);
        }
    }

    protected class FlagCommand
        implements Command
    {
        public FlagCommand (ItemFlag.Kind kind)
        {
            _kind = kind;
        }

        public void execute ()
        {
            BorderedDialog dialog = new BorderedDialog(false) {};
            dialog.setHeaderTitle(_cmsgs.tagFlagDialogTitle());

            String mature[] = { _cmsgs.tagMaturePrompt1(), _cmsgs.tagMaturePrompt2() };
            String copyright[] = { _cmsgs.tagCopyrightPrompt1(), _cmsgs.tagCopyrightPrompt2() };
            String prompts[] = _kind == ItemFlag.Kind.MATURE ? mature : copyright;

            final SmartTable content = new SmartTable(0, 10);
            content.setWidth("300px");
            content.setText(0, 0, prompts[0]);
            content.setText(1, 0, prompts[1]);
            content.setWidget(2, 0, _comment = MsoyUI.createTextArea(null, 64, 4));
            dialog.setContents(content);
            dialog.addButton(new Button(_cmsgs.cancel(), dialog.onCancel()));
            dialog.addButton(new Button(_cmsgs.tagFlagDialogAccept(), dialog.onAction(
                new Command() {
                    public void execute () {
                        _flagger.addFlag(_kind, _comment.getText());
                        MsoyUI.info(_cmsgs.tagThanks());
                    }
                })));
            dialog.show();
        }

        protected ItemFlag.Kind _kind;
        protected TextArea _comment;
    }

    protected TagService _service;
    protected FlagService _flagger;
    protected boolean _canEdit;

    protected FlowPanel _tags;
    protected ListBox _quickTags;
    protected Label _quickTagLabel;
    protected FlexTable _tagHistory;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final ItemServiceAsync _itemsvc = (ItemServiceAsync)
        ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);
}
