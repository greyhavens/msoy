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
 * An implementation of IImageEncoder that encoders raw bitmaps as encoded
 * images using Portable Network Graphics (PNG) lossless compression.
 */
public class PNGEncoder implements IImageEncoder
{
    include "../../core/Version.as";

    public function PNGEncoder()
    {
    }

    /**
     * The MIME type for the PNG encoded image.
     */
    public function get contentType():String
    {
        return CONTENT_TYPE;
    }

    /**
     * Converts the ByteArray of pixels from a raw bitmap to a PNG encoded
     * ByteArray. If transparent is set to false, 4 bytes must still be sent
     * per pixel for ARGB, however, the alpha channel information will be
     * ignored.
     */
    public function encodeByteArray(raw:ByteArray, width:int, height:int, transparent:Boolean=true):ByteArray
    {
        return internalEncode(raw, width, height, transparent);
    }

    /**
     * Converts the pixels of raw BitmapData to a PNG encoded ByteArray.
     */
    public function encode(image:BitmapData):ByteArray
    {
        return internalEncode(image, image.width, image.height, image.transparent);
    }

    private function internalEncode(source:Object, width:int, height:int, transparent:Boolean=true):ByteArray
    {
        // Create output byte array
        var png:ByteArray = new ByteArray();

        // Write PNG signature
        png.writeUnsignedInt(0x89504e47);
        png.writeUnsignedInt(0x0D0A1A0A);

        // Build IHDR chunk
        var IHDR:ByteArray = new ByteArray();
        IHDR.writeInt(width);
        IHDR.writeInt(height);
        IHDR.writeUnsignedInt(0x08060000); // 32bit RGBA
        IHDR.writeByte(0);
        writeChunk(png, 0x49484452, IHDR);

        // Build IDAT chunk
        var IDAT:ByteArray = new ByteArray();
        for (var y:int = 0; y < height; y++)
        {
            var x:int;

            // no filter
            IDAT.writeByte(0);
            var p:uint;
            if (!transparent)
            {
                for (x = 0; x < width; x++)
                {
                    p = getPixel(source, x, y, width, height);
                    IDAT.writeUnsignedInt(uint(((p&0xFFFFFF) << 8) | 0xFF));
                }
            }
            else
            {
                for (x = 0; x < width; x++)
                {
                    p = getPixel32(source, x, y, width, height);
                    IDAT.writeUnsignedInt(uint(((p&0xFFFFFF) << 8) | (p >>> 24)));
                }
            }
        }
        IDAT.compress();
        writeChunk(png, 0x49444154, IDAT);

        // Build IEND chunk
        writeChunk(png, 0x49454E44, null);

        // return PNG
        png.position = 0;
        return png;
    }

    private function writeChunk(png:ByteArray, type:uint, data:ByteArray):void
    {
        var c:uint;

        if (!crcTableComputed)
        {
            crcTableComputed = true;
            crcTable = [];
            for (var n:uint = 0; n < 256; n++)
            {
                c = n;
                for (var k:uint = 0; k < 8; k++)
                {
                    if (c & 1)
                    {
                        c = uint(uint(0xedb88320) ^ uint(c >>> 1));
                    }
                    else
                    {
                        c = uint(c >>> 1);
                    }
                }
                crcTable[n] = c;
            }
        }

        var len:uint = 0;
        if (data != null)
        {
            len = data.length;
        }

        png.writeUnsignedInt(len);
        var p:uint = png.position;
        png.writeUnsignedInt(type);
        if (data != null)
        {
            png.writeBytes(data);
        }

        var e:uint = png.position;
        png.position = p;
        c = 0xffffffff;
        for (var i:int = 0; i < (e - p); i++)
        {
            c = uint(crcTable[(c ^ png.readUnsignedByte()) & uint(0xff)] ^ uint(c >>> 8));
        }
        c = uint(c ^ uint(0xffffffff));
        png.position = e;
        png.writeUnsignedInt(c);
    }

    /**
     * Gets an unmultiplied pixel RGB value as an unsigned integer. No alpha
     * transparency information is included.
     */
    private function getPixel(source:Object, x:int, y:int, width:int=0, height:int=0):uint
    {
        if (source is BitmapData)
        {
            var bitmap:BitmapData = source as BitmapData;
            return bitmap.getPixel(x, y);
        }
        else if (source is ByteArray)
        {
            var byteArray:ByteArray = source as ByteArray;
            byteArray.position = ((y * width) * 4) + (x * 4);
            return byteArray.readUnsignedInt();
        }
        else
        {
            throw new ArgumentError("The source argument must be an instance of flash.display.BitmapData or flash.utils.ByteArray.");
        }
    }

    /**
     * Returns an unmultiplied ARGB color value (that contains alpha channel
     * data and RGB data) as an unsigned integer.
     */
    private function getPixel32(source:Object, x:int, y:int, width:int=0, height:int=0):uint
    {
        if (source is BitmapData)
        {
            var bitmap:BitmapData = source as BitmapData;
            return bitmap.getPixel32(x, y);
        }
        else if (source is ByteArray)
        {
            var byteArray:ByteArray = source as ByteArray;
            byteArray.position = ((y * width) * 4) + (x * 4);
            return byteArray.readUnsignedInt();
        }
        else
        {
            throw new ArgumentError("The source argument must be an instance of flash.display.BitmapData or flash.utils.ByteArray.");
        }
    }

    public static const CONTENT_TYPE:String = "image/png";
    private var crcTable:Array;
    private var crcTableComputed:Boolean = false;
}

}