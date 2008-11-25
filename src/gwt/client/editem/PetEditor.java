//
// $Id$

package client.editem;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.ui.PromptPopup;
import client.util.Link;
import client.util.StringUtil;

/**
 * A class for creating and editing {@link Pet} digital items.
 */
public class PetEditor extends ItemEditor
{
    @Override // from ItemEditor
    public void setItem (Item item)
    {
        super.setItem(item);
        _pet = (Pet)item;
    }

    @Override // from ItemEditor
    public Item createBlankItem ()
    {
        return new Pet();
    }

    @Override // from ItemEditor
    protected void addFurniUploader ()
    {
        addSpacer();
        addRow(getFurniTabText(), createFurniUploader(getFurniType(), generateFurniThumbnail(),
            new MediaUpdater() {
                public String updateMedia (String name, MediaDesc desc, int width, int height) {
                    if (desc.isImage()) {
                        promptEasyItem(desc);
                        return SUPPRESS_ERROR;
                    }
                    if (!isValidPrimaryMedia(desc)) {
                        return invalidPrimaryMediaMessage();
                    }
                    _item.setFurniMedia(desc);
                    return null;
                }
            }), getFurniTitleText());
    }

    @Override // from ItemEditor
    protected String getFurniTabText ()
    {
        return _emsgs.petLabel();
    }

    @Override // from ItemEditor
    protected boolean generateFurniThumbnail ()
    {
        return false;
    }

    @Override // from ItemEditor
    protected String getFurniTitleText ()
    {
        return _emsgs.petTip();
    }

    @Override // from ItemEditor
    protected String invalidPrimaryMediaMessage ()
    {
        return _emsgs.errPetNotFlash();
    }

    @Override // from ItemEditor
    protected String getFurniType ()
    {
        return TYPE_FLASH_REMIXABLE;
    }

    protected void promptEasyItem (final MediaDesc image)
    {
        new PromptPopup(_emsgs.makeEasyPet(), new Command() {
            public void execute () {
                createEasyItem(image);
            }
        }).setContext(_emsgs.makeEasyPetDetails()).prompt();
    }

    protected void createEasyItem (MediaDesc image)
    {
        Item easy = createBlankItem();
        _easyImage = image;

        easy.name = StringUtil.isBlank(_pet.name) ? _emsgs.easyPetName() : _pet.name;
        easy.description = _pet.description; // ok if blank
        easy.setThumbnailMedia(_pet.getRawThumbnailMedia()); // ok if null
        easy.setPrimaryMedia(EASY_IMAGE_PROTOTYPE);
        setItem(easy);
        commitEdit();
    }

    @Override
    protected void editComplete (Item item, boolean created)
    {
        if (_easyImage != null) {
            Link.go(Pages.STUFF, Args.compose("r", item.getType(), item.itemId,
                MediaDesc.mdToString(_easyImage)));

        } else {
            super.editComplete(item, created);
        }
    }

    protected Pet _pet;

    /** A reference to an image the user wants to use while creating an easy image pet. */
    protected MediaDesc _easyImage;

    protected static final MediaDesc EASY_IMAGE_PROTOTYPE = new MediaDesc(
        "e883fc843fe9fec451de22f46c0c9b84426472df", MediaDesc.APPLICATION_ZIP,
        MediaDesc.NOT_CONSTRAINED);

    protected static final EditemMessages _emsgs = GWT.create(EditemMessages.class);
}
