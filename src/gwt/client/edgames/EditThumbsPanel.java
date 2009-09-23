//
// $Id$

package client.edgames;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import com.threerings.msoy.game.gwt.GameThumbnail;
import com.threerings.msoy.game.gwt.GameThumbnail.Type;

import client.edgames.EditorUtil.ConfigException;
import client.edgames.EditorUtil.MediaBox;
import client.ui.MsoyUI;

/**
 * Edits a single type of feed thumbnail, organized by variant.
 */
class EditThumbsPanel extends FlowPanel
{
    /**
     * Organizes a mixed list of game thumbnails and provides convenient access by type and
     * variant.
     */
    public static class ThumbnailSet
    {
        /**
         * Creates a new thumbnail set.
         */
        public ThumbnailSet (List<GameThumbnail> allThumbnails)
        {
            for (GameThumbnail thumb : allThumbnails) {
                get(thumb.type, thumb.variant).add(thumb);
            }
        }

        /**
         * Gets a list of thumbnails for a given type and variant, creating the list if it does
         * not exist. Modifications will take effect.
         */
        public List<GameThumbnail> get (Type type, String variant)
        {
            Map<String, List<GameThumbnail>> variants = getVariantMap(type);
            List<GameThumbnail> thumbs = variants.get(variant);
            if (thumbs == null) {
                variants.put(variant, thumbs = new ArrayList<GameThumbnail>());
            }
            return thumbs;
        }

        /**
         * Gets a map of variant to thumbnail list for a given type, creating the map if it does
         * not exist. Modifications will take effect.
         */
        public Map<String, List<GameThumbnail>> getVariantMap (Type type)
        {
            Map<String, List<GameThumbnail>> variants = _organized.get(type);
            if (variants == null) {
                _organized.put(type, variants = new HashMap<String, List<GameThumbnail>>());
                return variants;
            }
            return variants;
        }

        /**
         * Gets a sorted list of variants present in the set for the given type.
         */
        public List<String> getVariants (Type type)
        {
            List<String> variants = new ArrayList<String>();
            variants.addAll(getVariantMap(type).keySet());
            Collections.sort(variants);
            return variants;
        }

        protected Map<Type, Map<String, List<GameThumbnail>>> _organized =
            new HashMap<Type, Map<String, List<GameThumbnail>>>();
    }

    /**
     * Creates a new thumbnail editor for the given type with the given name, populating the
     * initial contents with the thumbnails in the given set.
     */
    public EditThumbsPanel (String typeName, Type type, ThumbnailSet thumbnails)
    {
        this(typeName, type, thumbnails, null);
    }

    /**
     * Creates a new thumbnail editor for the given type with the given name, populating the
     * initial contents with the thumbnails in the given set. If not null, the given runnable will
     * be run whenever any media changes.
     */
    public EditThumbsPanel (String typeName, Type type, ThumbnailSet thumbnails,
        Runnable onMediaModified)
    {
        setStyleName("Type");
        _type = type;
        _typeName = typeName;
        _onMediaModified = onMediaModified;

        add(_title = MsoyUI.createLabel("", "Title"));
        add(_variants = MsoyUI.createFlowPanel("Variants"));

        for (String variant : thumbnails.getVariants(type)) {
            addVariant(new VariantPanel(variant, thumbnails.get(type, variant)));
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
    public List<GameThumbnail> getThumbnails ()
    {
        List<GameThumbnail> thumbs = new ArrayList<GameThumbnail>();
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
        public VariantPanel (String variant, List<GameThumbnail> content)
        {
            setStyleName("Variant");

            setWidget(0, 0, MsoyUI.createFlowPanel(null,
                MsoyUI.createLabel(_msgs.editFeedThumbnailsVariantLabel(), null),
                _variant = MsoyUI.createTextBox(variant, 8, 4)));

            final int size = MediaDesc.FB_FEED_SIZE;
            for (int ii = 0; ii < GameThumbnail.COUNT; ++ii) {
                String id = _type.toString() + variant + ii;
                EditorUtil.MediaBox box = new EditorUtil.MediaBox(size, id,
                    content != null && ii < content.size() ? content.get(ii).media : null) {
                    @Override public void mediaUploaded (
                        String name, MediaDesc desc, int w, int h) {
                        int targetW = MediaDesc.getWidth(size);
                        int targetH = MediaDesc.getHeight(size);
                        if (w != targetW || h != targetH) {
                            MsoyUI.error(_msgs.errInvalidShot(
                                String.valueOf(targetW), String.valueOf(targetH)));
                        } else {
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

        public List<GameThumbnail> getThumbnails ()
        {
            String variant = _variant.getText();
            if (variant.length() == 0) {
                throw new ConfigException(_msgs.errVariantNull());
            }
            List<GameThumbnail> thumbs = new ArrayList<GameThumbnail>();
            byte pos = 0;
            for (MediaBox box : _boxes) {
                thumbs.add(new GameThumbnail(EditorUtil.requireImageMedia(
                    _msgs.egShot(), box.getMedia()), _type, variant, pos++));
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

    protected Type _type;
    protected String _typeName;
    protected Label _title;
    protected FlowPanel _variants;
    protected Runnable _onMediaModified;

    protected static final EditGamesMessages _msgs = GWT.create(EditGamesMessages.class);
}
