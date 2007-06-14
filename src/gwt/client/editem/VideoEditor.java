//
// $Id$

package client.editem;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TabPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.Video;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

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
    }

    // @Override from ItemEditor
    public Item createBlankItem ()
    {
        return new Video();
    }

    // @Override from ItemEditor
    protected void createInterface (VerticalPanel contents, TabPanel tabs)
    {
        Widget mainUploader = createMainUploader(CEditem.emsgs.videoMainTitle(), new MediaUpdater() {
            public String updateMedia (MediaDesc desc, int width, int height) {
                // TODO: remove this hack?
                if (!desc.isVideo()) {
                    return CEditem.emsgs.errVideoNotVideo();
                }
                _video.videoMedia = desc;
                return null;
            }
        });

        VerticalPanel pan = new VerticalPanel();
        pan.add(mainUploader);
        pan.add(new Label("OR")); // TODO: i18n
        pan.add(createYouTubeUploader());

        tabs.add(pan, CEditem.emsgs.videoMainTab());

        super.createInterface(contents, tabs);
    }

    /**
     * Create the special youtube uploader.
     */
    protected Widget createYouTubeUploader ()
    {
//        FormPanel form = new FormPanel();
//        form.setWidget(panel);

        final TextBox youtubeIdBox = new TextBox();
        youtubeIdBox.setMaxLength(11); // TODO: might youtube use larger ids someday???

        Button submit = new Button("Submit"); // TODO: i18n
        submit.addClickListener(new ClickListener() {
            public void onClick (Widget widget) {
                String text = youtubeIdBox.getText();
                if (text.length() == 11) {
                    setHash(Item.MAIN_MEDIA, text, MediaDesc.VIDEO_YOUTUBE,
                        MediaDesc.NOT_CONSTRAINED, 425, 350, "", 0, 0);
                }
            }
        });
           
        HorizontalPanel panel = new HorizontalPanel();
        panel.add(new Label("Enter a youtube video id")); // TODO: i18n
        panel.add(youtubeIdBox);
        panel.add(submit);

        return panel;
    }

    protected Video _video;
}
