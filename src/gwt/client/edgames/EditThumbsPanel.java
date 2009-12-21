//
// $Id$

package client.edgames;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MediaDescSize;
import com.threerings.msoy.facebook.gwt.FeedThumbnail;

import client.edutil.EditorUtil;
import client.edutil.ThumbnailSet;
import client.edutil.EditorUtil.MediaBox;
import client.edutil.EditorUtil.ConfigException;
import client.ui.MsoyUI;

/**
 * Edits a single type of feed thumbnail, organized by variant.
 */
class EditThumbsPanel extends FlowPanel
{
    /**
     * Creates a new thumbnail editor for the given type with the given name, populating the
     * initial contents with the thumbnails in the given set.
     */
    public EditThumbsPanel (String typeName, String code, ThumbnailSet thumbnails)
    {
        this(typeName, code, thumbnails, null);
    }

    /**
     * Creates a new thumbnail editor for the given type with the given name, populating the
     * initial contents with the thumbnails in the given set. If not null, the given runnable will
     * be run whenever any media changes.
     */
    public EditThumbsPanel (String typeName, String code, ThumbnailSet thumbnails,
        Runnable onMediaModified)
    {
        setStyleName("Type");
        _code = code;
        _typeName = typeName;
        _onMediaModified = onMediaModified;

        add(_title = MsoyUI.createLabel("", "Title"));
        add(_variants = MsoyUI.createFlowPanel("Variants"));

        for (String variant : thumbnails.getVariants(code)) {
            addVariant(new VariantPanel(variant, thumbnails.get(code, variant)));
        }

        add(new Button(_msgs.editFeedThumbnailsNewVariant(typeName), new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                addVariant(new VariantPanel("", null));
                updateTitle();
            }
        }));
        updateTitle();
    }

    /**
     * Gets a list of all thumbnails (of all variations) in the editor.
     */
    public List<FeedThumbnail> getThumbnails ()
    {
        List<FeedThumbnail> thumbs = new ArrayList<FeedThumbnail>();
        Set<String> variants = new HashSet<String>();
        for (VariantPanel vpanel : getPanels()) {
            if (variants.contains(vpanel.getVariant())) {
                throw new ConfigException(_msgs.errVariantDuplicate(vpanel.getVariant()));
            }
            thumbs.addAll(vpanel.getThumbnails());
        }
        return thumbs;
    }

    protected List<VariantPanel> getPanels ()
    {
        List<VariantPanel> panels = new ArrayList<VariantPanel>();
        for (int ii = 0; ii < _variants.getWidgetCount(); ++ii) {
            if (_variants.getWidget(ii) instanceof VariantPanel) {
                panels.add((VariantPanel)_variants.getWidget(ii));
            }
        }
        return panels;
    }

    protected void addVariant (final VariantPanel vpanel)
    {
        vpanel.addDeleteButton(MsoyUI.createCloseButton(new ClickHandler() {
            @Override public void onClick (ClickEvent event) {
                _variants.remove(vpanel);
                updateTitle();
            }
        }));
        _variants.add(vpanel);
    }

    protected void updateTitle ()
    {
        _title.setText(_msgs.editFeedThumbnailsTypeTitle(_typeName, "" + getPanels().size()));
    }

    /**
     * Edits a single list of images for a feed thumbnail variant.
     */
    protected class VariantPanel extends SmartTable
    {
        public VariantPanel (String variant, List<FeedThumbnail> content)
        {
            setStyleName("Variant");

            setWidget(0, 0, MsoyUI.createFlowPanel(null,
                MsoyUI.createLabel(_msgs.editFeedThumbnailsVariantLabel(), null),
                _variant = MsoyUI.createTextBox(variant, 8, 4)));

            final int size = MediaDescSize.FB_FEED_SIZE;
            for (int ii = 0; ii < FeedThumbnail.COUNT; ++ii) {
                String id = _code + variant + ii;
                EditorUtil.MediaBox box = new EditorUtil.MediaBox(size, id,
                    content != null && ii < content.size() ? content.get(ii).media : null) {
                    @Override public void mediaUploaded (
                        String name, MediaDesc desc, int w, int h) {
                        if (checkSize(w, h)) {
                            super.mediaUploaded(name, desc, w, h);
                        }
                    }
                    @Override protected void mediaModified () {
                        if (_onMediaModified != null) {
                            _onMediaModified.run();
                        }
                    }
                };
                setWidget(0, ii + 1, box);
                _boxes.add(box);
            }
        }

        public List<FeedThumbnail> getThumbnails ()
        {
            String variant = _variant.getText();
            if (variant.length() == 0) {
                throw new ConfigException(_msgs.errVariantNull());
            }
            List<FeedThumbnail> thumbs = new ArrayList<FeedThumbnail>();
            byte pos = 0;
            for (MediaBox box : _boxes) {
                thumbs.add(new FeedThumbnail(EditorUtil.requireImageMedia(
                    _msgs.egShot(), box.getMedia()), _code, variant, pos++));
            }
            return thumbs;
        }

        public void addDeleteButton (Widget button)
        {
            setWidget(0, getCellCount(0), button);
        }

        public String getVariant ()
        {
            return _variant.getText();
        }

        protected TextBox _variant;
        protected List<MediaBox> _boxes = new ArrayList<MediaBox>();
    }

    protected String _code;
    protected String _typeName;
    protected Label _title;
    protected FlowPanel _variants;
    protected Runnable _onMediaModified;

    protected static final EditGamesMessages _msgs = GWT.create(EditGamesMessages.class);
}
