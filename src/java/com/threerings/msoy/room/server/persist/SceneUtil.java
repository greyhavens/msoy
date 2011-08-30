//
// $Id$

package com.threerings.msoy.room.server.persist;

import java.nio.ByteBuffer;

import com.google.common.base.Preconditions;

import com.samskivert.util.ByteEnumUtil;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.item.data.all.DefaultItemMediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.server.MediaDescFactory;

/**
 * Scene related utility methods.
 */
public class SceneUtil
{
    /**
     * Creates a {@link com.threerings.orth.data.MediaDesc} of the appropriate type based on the supplied hash and mime
     * type. The hash should previously have been created by calling {@link #flattenMediaDesc} on a
     * media descriptor.
     */
    public static MediaDesc createMediaDesc (byte[] mediaHash, byte mimeType)
    {
        if (mediaHash.length == 4 || mediaHash.length == 8) {
            // note: 8-byte descriptors are no longer supported
            // only the itemType int is used, and the media type is always assumed to be Furni
            byte itemByte = (byte)ByteBuffer.wrap(mediaHash).asIntBuffer().get();
            return Item.getDefaultFurniMediaFor(ByteEnumUtil.fromByte(MsoyItemType.class, itemByte));
        } else {
            return MediaDescFactory.createMediaDesc(mediaHash, mimeType);
        }
    }

    /**
     * Flattens the supplied {@link MediaDesc} into bytes that can later be decoded by
     * {@link #createMediaDesc} into the appropriate type of descriptor.
     */
    public static byte[] flattenMediaDesc (MediaDesc desc)
    {
        if (desc instanceof DefaultItemMediaDesc) {
            DefaultItemMediaDesc sdesc = (DefaultItemMediaDesc)desc;

            // sanity check; if we later need to flatten other static types than furni, we can have
            // the type constant map to an integer and stuff that into the byte array as well
            Preconditions.checkArgument(sdesc.getMediaType().equals(Item.FURNI_MEDIA),
                                        "Cannot flatten non-furni static media " + desc + ".");

            ByteBuffer data = ByteBuffer.allocate(4);
            data.asIntBuffer().put(sdesc.getItemTypeCode().toByte());
            return data.array();

        } else if (desc instanceof MediaDesc) {
            return HashMediaDesc.unmakeHash(desc);
        } else {
            throw new IllegalArgumentException("Unknown media descriptor type: " + desc);
        }

    }
}
