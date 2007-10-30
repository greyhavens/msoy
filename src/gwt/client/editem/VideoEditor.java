//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Video;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.shell.CShell;

/**
 * A class for creating and editing {@link Photo} digital items.
 */
public class VideoEditor extends ItemEditor
{
    // @Override from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _video = (Video)item;
        _mainUploader.setMedia(_video.videoMedia);
        updateAlternateSources(_video.videoMedia);
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Video();
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        Widget mainUploader = createMainUploader(CShell.emsgs.videoMainTitle(), 
            new MediaUpdater() {
                public String updateMedia (String name, MediaDesc desc, int width, int height) {
                    // TODO: remove this hack?
                    if (!desc.isVideo()) {
                        return CShell.emsgs.errVideoNotVideo();
                    }
                    _video.videoMedia = desc;
                    updateAlternateSources(desc);
                    return null;
                }
            });

        VerticalPanel pan = new VerticalPanel();
        pan.add(mainUploader);
        pan.add(new HTML("<hr>"));
        pan.add(new Label(CShell.emsgs.videoOptionYoutube()));
        pan.add(createYouTubeUploader());
//        pan.add(new HTML("<hr>"));
//        pan.add(new Label(CShell.emsgs.videoOptionGoogle()));
//        pan.add(createGoogleUploader());

        tabs.add(pan, CShell.emsgs.videoMainTab());

        super.createInterface(contents, tabs);
    }

    /**
     * Create the special youtube uploader.
     */
    protected Widget createYouTubeUploader ()
    {
        _youtubeIdBox = new TextBox();
        _youtubeIdBox.setMaxLength(YOUTUBE_ID_LENGTH);

        // TODO: ideally, the Button is disabled until a valid-lengthed string is entered
        // into the ID box. However, the ChangeEvent on a TextBox only gets fired when it loses
        // focus, and the KeyboardListener is cumbersome and I'm not sure it reacts to pastes.
        Button submit = new Button(CShell.cmsgs.update());
        submit.setStyleName("mediaUploader");
        submit.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                String text = _youtubeIdBox.getText();
                if (text.length() == YOUTUBE_ID_LENGTH) {
                    setHash(Item.MAIN_MEDIA, text, MediaDesc.VIDEO_YOUTUBE,
                        MediaDesc.NOT_CONSTRAINED, 425, 350, "", 0, 0);
                    // NOTE: it appears that youtube videos have a thumbnail of sorts hosted in
                    // a predictable and accessible way.
                    //    http://img.youtube.com/vi/VIDEO_ID/0.jpg
                    // Perhaps we should have an 'external image' mime type and ... murrgh
                    // it'd actually have to be a 'youtube thumbnail' mime type.
                    // Something to consider.
                }
            }
        });

        HorizontalPanel row = new HorizontalPanel();
        row.setSpacing(10);
        row.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        row.add(new Label(CShell.emsgs.videoPromptYoutube()));
        row.add(_youtubeIdBox);
        row.add(submit);

        return row;
    }

//    /**
//     * Create the special google uploader.
//     */
//    protected Widget createGoogleUploader ()
//    {
//        _googleIdBox = new TextBox();
//        _googleIdBox.setMaxLength(GOOGLE_ID_LENGTH);
//
//        // TODO: ideally, the Button is disabled until a valid-lengthed string is entered
//        // into the ID box. However, the ChangeEvent on a TextBox only gets fired when it loses
//        // focus, and the KeyboardListener is cumbersome and I'm not sure it reacts to pastes.
//        Button submit = new Button(CShell.cmsgs.update());
//        submit.setStyleName("mediaUploader");
//        submit.addClickListener(new ClickListener() {
//            public void onClick (Widget widget) {
//                String text = _googleIdBox.getText();
//                if (text.length() == GOOGLE_ID_LENGTH) {
//                    setHash(Item.MAIN_MEDIA, text, MediaDesc.VIDEO_GOOGLE,
//                        MediaDesc.NOT_CONSTRAINED, 400, 326, "", 0, 0);
//                }
//            }
//        });
//           
//        HorizontalPanel row = new HorizontalPanel();
//        row.setSpacing(10);
//        row.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
//        row.add(new Label(CShell.emsgs.videoPromptGoogle()));
//        row.add(_googleIdBox);
//        row.add(submit);
//
//        return row;
//    }

    /**
     * Update the alternate video sources if the video ultimately came from one
     * of them.
     */
    protected void updateAlternateSources (MediaDesc video)
    {
        String id = ((video != null) && video.isExternalVideo()) ?
            MediaDesc.bytesToString(video.hash) : "";

        if (_youtubeIdBox != null) {
            _youtubeIdBox.setText(video != null && video.mimeType == MediaDesc.VIDEO_YOUTUBE ? id
                : "");
        }
        if (_googleIdBox != null) {
            _googleIdBox.setText(video != null && video.mimeType == MediaDesc.VIDEO_GOOGLE ? id
                : "");
        }
    }

    protected Video _video;

    protected TextBox _youtubeIdBox;
    protected TextBox _googleIdBox;

    protected static final int YOUTUBE_ID_LENGTH = 11; // Can these change?
    protected static final int GOOGLE_ID_LENGTH = 19; // Can these change?
}
