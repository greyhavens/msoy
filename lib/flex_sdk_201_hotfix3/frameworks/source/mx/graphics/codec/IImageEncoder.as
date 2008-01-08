////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.graphics.codec
{

import flash.display.BitmapData;
import flash.utils.ByteArray;

/**
 * Image encoders take raw bitmaps and convert them to popular formats 
 * such as PNG or JPEG.
 * 
 * @see PNGEncoder
 * @see JPEGEncoder
 */
public interface IImageEncoder
{
    /**
     * The MIME type for the encoded image format.
     */
    function get contentType():String;

    /**
     * Encodes a raw BitmapData as a ByteArray.
     */
    function encode(image:BitmapData):ByteArray;

    /**
     * Encodes a raw bitmap ByteArray as a new ByteArray. The ByteArray must
     * be written using 4 bytes per pixel as ARGB, however, if the transparent
     * flag is set to false the alpha channel information will be ignored.
     */
    function encodeByteArray(raw:ByteArray, width:int, height:int, transparent:Boolean=true):ByteArray;
}

}