//
// $Id$

package client.games;

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
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.game.gwt.GameThumbnail;
import com.threerings.msoy.game.gwt.GameThumbnail.Type;

import client.ui.MsoyUI;
import client.util.ClickCallback;
import client.util.InfoCallback;
import client.games.EditorUtil.ConfigException;
import client.games.EditorUtil.MediaBox;

/**
 * Panel for editing Facebook feed story thumbnails.
 */
public class EditFeedThumbsPanel extends FlowPanel
{
    public EditFeedThumbsPanel ()
    {
        setStyleName("editFeedThumbs");
        add(MsoyUI.createLabel(_msgs.editFeedThumbnailsTitle(), "Title"));
        add(MsoyUI.createLabel(_msgs.editFeedThumbnailsTip(), "Tip"));
        _gamesvc.loadAdditionalThumbnails(0, new InfoCallback<List<GameThumbnail>>() {
            public void onSuccess (List<GameThumbnail> result) {
                init(result);
            }
        });
    }

    public void init (List<GameThumbnail> result)
    {
        Map<Type, Map<String, List<GameThumbnail>>> organized =
            new HashMap<Type, Map<String, List<GameThumbnail>>>();

        for (GameThumbnail thumb : result) {
            Map<String, List<GameThumbnail>> variants = organized.get(thumb.type);
            if (variants == null) {
                organized.put(thumb.type, variants = new HashMap<String, List<GameThumbnail>>());
            }
            List<GameThumbnail> thumbs = variants.get(thumb.variant);
            if (thumbs == null) {
                variants.put(thumb.variant, thumbs = new ArrayList<GameThumbnail>());
            }
            thumbs.add(thumb);
        }

        addPanel(_msgs.editFeedThumbnailsTrophy(), Type.TROPHY, organized);
        addPanel(_msgs.editFeedThumbnailsChallenge(), Type.CHALLENGE, organized);
        addPanel(_msgs.editFeedThumbnailsLevelUp(), Type.LEVELUP, organized);

        Button save = new Button(_msgs.editFeedThumbnailsSave());
        add(save);
        new ClickCallback<Void>(save) {
            @Override protected boolean callService () {
                try {
                    List<GameThumbnail> thumbnails = new ArrayList<GameThumbnail>();
                    for (ThumbsPanel panel : _panels) {
                        thumbnails.addAll(panel.getThumbnails());
                    }
                    _gamesvc.updateAdditionalThumbnails(0, thumbnails, this);

                } catch (ConfigException e) {
                    MsoyUI.error(e.getMessage());
                    return false;
                }
                return true;
            }

            @Override protected boolean gotResult (Void result) {
                MsoyUI.info(_msgs.editFeedThumbnailsSaved());
                return true;
            }
        };
    }

    public void addPanel (String typeName, Type type,
        Map<Type, Map<String, List<GameThumbnail>>> result)
    {
        ThumbsPanel panel = new ThumbsPanel(typeName, type, result.get(type));
        add(panel);
        _panels.add(panel);
    }

    /**
     * Edits a single list of images for a feed thumbnail variant.
     */
    protected class VariantPanel extends SmartTable
    {
        public VariantPanel (Type type, String variant, List<GameThumbnail> content)
        {
            setStyleName("Variant");
            _type = type;

            setWidget(0, 0, MsoyUI.createFlowPanel(null,
                MsoyUI.createLabel(_msgs.editFeedThumbnailsVariantLabel(), null),
                _variant = MsoyUI.createTextBox(variant, 8, 4)));

            final int size = MediaDesc.FB_FEED_SIZE;
            for (int ii = 0; ii < type.count; ++ii) {
                String id = type.toString() + variant + ii;
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

        protected Type _type;
        protected TextBox _variant;
        protected List<MediaBox> _boxes = new ArrayList<MediaBox>();
    }

    /**
     * Edits a single type of feed thumbnail, organized by variant.
     */
    protected class ThumbsPanel extends FlowPanel
    {
        public ThumbsPanel (String typeName, Type type, Map<String, List<GameThumbnail>> variants)
        {
            setStyleName("Type");
            _type = type;
            _typeName = typeName;
            add(_title = MsoyUI.createLabel("", "Title"));

            List<String> variantStrs = new ArrayList<String>();
            if (variants != null) {
                variantStrs.addAll(variants.keySet());
            }
            Collections.sort(variantStrs);

            _variants = MsoyUI.createFlowPanel("Variants");
            add(_variants);

            for (String variant : variantStrs) {
                addVariant(new VariantPanel(_type, variant, variants.get(variant)));
            }

            add(new Button(_msgs.editFeedThumbnailsNewVariant(typeName), new ClickHandler() {
                @Override public void onClick (ClickEvent event) {
                    addVariant(new VariantPanel(_type, "", null));
                    updateTitle();
                }
            }));
            updateTitle();
        }

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

        protected GameThumbnail.Type _type;
        protected String _typeName;
        protected Label _title;
        protected FlowPanel _variants;
    }

    protected List<ThumbsPanel> _panels = new ArrayList<ThumbsPanel>();

    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
}
