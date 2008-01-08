////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.graphics
{

import flash.display.IBitmapDrawable;
import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;
import flash.display.Stage;
import flash.geom.ColorTransform;
import flash.geom.Matrix;
import flash.geom.Rectangle;
import flash.system.Capabilities;
import flash.utils.ByteArray;
import flash.utils.getDefinitionByName;

import mx.core.IFlexDisplayObject;
import mx.core.IUIComponent;
import mx.core.UIComponent;
import mx.graphics.codec.IImageEncoder;
import mx.graphics.codec.PNGEncoder;
import mx.utils.Base64Encoder;

/**
 * A helper class used to capture a snapshot of any Flash component that 
 * implements <code>flash.display.IBitmapDrawable</code>, including Flex UI
 * components.
 */
[RemoteClass(alias="flex.graphics.ImageSnapshot")]
public dynamic class ImageSnapshot
{
    include "../core/Version.as";

    /**
     * Constructor.
     */
    public function ImageSnapshot(width:int=0, height:int=0,
        data:ByteArray=null, contentType:String=null)
    {
        this.contentType = contentType;
        this.width = width;
        this.height = height;
        this.data = data;
    }

    //--------------------------------------------------------------------------
    //
    // Properties
    // 
    //--------------------------------------------------------------------------

    //----------------------------------
    //  contentType
    //----------------------------------

    [Inspectable(category="General")]
    /**
     * The content type for the image encoding format that was used to
     * capture this snapshot.
     */
    public function get contentType():String
    {
        return _contentType;
    }

    public function set contentType(value:String):void
    {
        _contentType = value;
    }

    //----------------------------------
    //  data
    //----------------------------------

    [Inspectable(category="General")]
    /**
     * The encoded data representing the image snapshot.
     */
    public function get data():ByteArray
    {
        return _data;
    }

    public function set data(value:ByteArray):void
    {
        _data = value;
    }

    //----------------------------------
    //  height
    //----------------------------------

    [Inspectable(category="General")]
    /**
     * The image height in pixels.
     */
    public function get height():int
    {
        return _height;
    }

    public function set height(value:int):void
    {
        _height = value;
    }

    //----------------------------------
    //  width
    //----------------------------------

    [Inspectable(category="General")]
    /**
     * The image width in pixels.
     */
    public function get width():int
    {
        return _width;
    }

    public function set width(value:int):void
    {
        _width = value;
    }


    //--------------------------------------------------------------------------
    //
    // Methods
    // 
    //--------------------------------------------------------------------------

    /**
     * A utility method to grab a raw snapshot of a UI component as BitmapData.
     * 
     * @param source A UI component that implements <code>flash.display.IBitmapDrawable</code>
     * @param matrix A Matrix object used to scale, rotate, or translate the
     * coordinates of the captured bitmap. If you do not want to apply a matrix
     * transformation to the image, set this parameter to an identity matrix,
     * created with the default new Matrix() constructor, or pass a null value.
     * @return BitmapData representing the captured snapshot.
     */
    public static function captureBitmapData(source:*, matrix:Matrix=null,
        colorTransform:ColorTransform=null, blendMode:String=null,
        clipRect:Rectangle=null, smoothing:Boolean=false):BitmapData
    {
        var data:BitmapData;

        var width:int;
        var height:int;

        var normalState:Array;
        if (source is IUIComponent)
        {
            normalState = prepareToPrintObject(IUIComponent(source));
        }

        try
        {
            if (source != null)
            {
                if (source is DisplayObject)
                {
                    width = DisplayObject(source).width;
                    height = DisplayObject(source).height;
                }
                else if (source is BitmapData)
                {
                    width = BitmapData(source).width;
                    height = BitmapData(source).height;
                }
                else if (source is IFlexDisplayObject)
                {
                    width = IFlexDisplayObject(source).width;
                    height = IFlexDisplayObject(source).height;
                }
            }

            // We default to an identity matrix which will match screen resolution
            if (matrix == null)
                matrix = new Matrix(1, 0, 0, 1);

            var scaledWidth:Number = width * matrix.a;
            var scaledHeight:Number = height * matrix.d;
            var reductionScale:Number = 1;

            // Cap width to BitmapData max of 2880 pixels
            if (scaledWidth > MAX_BITMAP_DIMENSION)
            {
                reductionScale = scaledWidth / MAX_BITMAP_DIMENSION;
                scaledWidth = MAX_BITMAP_DIMENSION;
                scaledHeight = scaledHeight / reductionScale;
    
                matrix.a = scaledWidth / width;
                matrix.d = scaledHeight / height;
            }

            // Cap height to BitmapData max of 2880 pixels
            if (scaledHeight > MAX_BITMAP_DIMENSION)
            {
                reductionScale = scaledHeight / MAX_BITMAP_DIMENSION;
                scaledHeight = MAX_BITMAP_DIMENSION;
                scaledWidth = scaledWidth / reductionScale;
    
                matrix.a = scaledWidth / width;
                matrix.d = scaledHeight / height;
            }

            data = new BitmapData(scaledWidth, scaledHeight);
            data.draw(source, matrix, colorTransform, blendMode, clipRect, smoothing);
        }
        finally
        {
            if (source is IUIComponent)
            {
                finishPrintObject(IUIComponent(source), normalState);
            }
        }

        return data;
    }

    /**
     * A utility method to grab a snapshot of a component, scaled to a specific
     * resolution (in dpi) and encoded into a specific image format.
     * 
     * @param source A UI component that implements <code>flash.display.IBitmapDrawable</code>
     * @param dpi The resolution in dots per inch. If a resolution is not
     * provided the current on-screen resolution is used by default.
     * @param encoder The image format used to encode the raw bitmap. If 
     * an encoder is not provided, a default PNGEncoder is used.
     * @param scaleLimited  The maximum width or height of a bitmap in Flash is
     * 2880 pixels - if scaleLimited is set to true the resolution will be
     * reduced proportionately to fit within 2880 pixels, otherwise, if
     * scaleLimited is false, smaller snapshot windows will be taken and
     * stitched together to capture a larger image. The default is true.
     * @return An ImageSnapshot holding an encoded captured snapshot and
     * associated image metadata.
     */
    public static function captureImage(source:*, dpi:Number=0, encoder:IImageEncoder=null, scaleLimited:Boolean=true):ImageSnapshot
    {
        var snapshot:ImageSnapshot;

        // Calculate scaling factor based on current screen resolution (dpi)
        var screenDPI:Number = Capabilities.screenDPI;
        if (dpi <= 0)
            dpi = screenDPI;

        // Create a transformation matrix to scale image to desired resolution
        var scale:Number = dpi / screenDPI;    
        var matrix:Matrix = new Matrix(scale, 0, 0, scale);

        var width:int;
        var height:int;

        var normalState:Array;
        if (source is IUIComponent)
        {
            normalState = prepareToPrintObject(IUIComponent(source));
        }

        try
        {
            if (source != null)
            {
                if (source is DisplayObject)
                {
                    width = DisplayObject(source).width;
                    height = DisplayObject(source).height;
                }
                else if (source is BitmapData)
                {
                    width = BitmapData(source).width;
                    height = BitmapData(source).height;
                }
                else if (source is IFlexDisplayObject)
                {
                    width = IFlexDisplayObject(source).width;
                    height = IFlexDisplayObject(source).height;
                }
            }

            // Use an image encoder on raw pixels to reduce size
            if (encoder == null)
                encoder = new DEFAULT_ENCODER();

            var bytes:ByteArray;
            width = width * matrix.a;
            height = height * matrix.d;

            // If scaleLimited, we limit snapshot to a maximum of 2880x2880
            // pixels irrespective of the requested dpi
            if (scaleLimited || (width <= MAX_BITMAP_DIMENSION && height <= MAX_BITMAP_DIMENSION))
            {
                var data:BitmapData = captureBitmapData(source, matrix);
                var bitmap:Bitmap = new Bitmap(data);
                width = bitmap.width;
                height = bitmap.height;
                bytes = encoder.encode(data);
            }
            else
            {
                // We scale to the requested dpi and try to capture the
                // entire snapshot as a raw bitmap ByteArray
                var bounds:Rectangle = new Rectangle(0, 0, width, height);
                bytes = captureAll(source, bounds, matrix);
                bytes = encoder.encodeByteArray(bytes, width, height);
            }

            snapshot = new ImageSnapshot(width, height, bytes, encoder.contentType);
        }
        finally
        {
            if (source is IUIComponent)
            {
                finishPrintObject(IUIComponent(source), normalState);
            }
        }

        return snapshot;
    }

    /**
     * A utility method to convert an ImageSnapshot into a Base-64 encoded
     * String for transmission in text based serialization formats such as XML.
     * @param snapshot An image captured as a <code>mx.graphics.ImageSnapshot</code>
     * 
     * @see #captureImage
     */
    public static function encodeImageAsBase64(snapshot:ImageSnapshot):String
    {
        var bytes:ByteArray = snapshot.data;

        // Convert to Base64 encoded String
        var base64:Base64Encoder = new Base64Encoder();
        base64.encodeBytes(bytes);
        var base64Image:String = base64.drain();

        return base64Image;
    }

    /**
     * @private
     * Attempts to capture as much of an image for the requested bounds by
     * splitting the scaled source into rectangular windows that fit inside
     * the maximum size of a single BitmapData instance, i.e. 2880x2880 pixels,
     * and stitching the windows together into a larger bitmap with the raw
     * pixels returned as a ByteArray. This ByteArray is limited to around
     * 256MB so scaled images with an area equivalent to about 8192x8192 will
     * result in out of memory errors.
     */
    private static function captureAll(source:*, bounds:Rectangle, matrix:Matrix,
        colorTransform:ColorTransform=null, blendMode:String=null,
        clipRect:Rectangle=null, smoothing:Boolean=false):ByteArray
    {
        var currentMatrix:Matrix = matrix.clone();
        var topLeft:Rectangle = bounds.clone();
        var topRight:Rectangle;
        var bottomLeft:Rectangle;
        var bottomRight:Rectangle;

        // Check if the requested bounds exceeds the maximum width for 
        // a bitmap...
        if (bounds.width > MAX_BITMAP_DIMENSION)
        {
            topLeft.width = MAX_BITMAP_DIMENSION;

            topRight = new Rectangle();
            topRight.x = topLeft.width;
            topRight.y = bounds.y;
            topRight.width = bounds.width - topLeft.width;
            topRight.height = bounds.height;
        }

        // Check if the requested bounds exceeds the maximum height for 
        // a bitmap...
        if (bounds.height > MAX_BITMAP_DIMENSION)
        {
            topLeft.height = MAX_BITMAP_DIMENSION;
            if (topRight != null)
                topRight.height = topLeft.height;

            bottomLeft = new Rectangle();
            bottomLeft.x = bounds.x;
            bottomLeft.y = topLeft.height;
            bottomLeft.width = topLeft.width;
            bottomLeft.height = bounds.height - topLeft.height;

            if (bounds.width > MAX_BITMAP_DIMENSION)
            {
                bottomRight = new Rectangle();
                bottomRight.x = topLeft.width;
                bottomRight.y = topLeft.height;
                bottomRight.width = bounds.width - topLeft.width;
                bottomRight.height = bounds.height - topLeft.height;
            }
        }

        // Capture top-left window
        currentMatrix.translate(-topLeft.x, -topLeft.y);
        topLeft.x = 0;
        topLeft.y = 0;
        var data:BitmapData = new BitmapData(topLeft.width, topLeft.height);
        data.draw(source, currentMatrix, colorTransform, blendMode, clipRect, smoothing);
        var pixels:ByteArray = data.getPixels(topLeft);
        pixels.position = 0;

        // If bounds width exceeded maximum dimensions for a bitmap, we 
        // also need to capture the top-right window (recursively, until we
        // have a window width less that the max). These right side rows have
        // to be merged to the right of each left side row.
        if (topRight != null)
        {
            currentMatrix = matrix.clone();
            currentMatrix.translate(-topRight.x, -topRight.y);
            topRight.x = 0;
            topRight.y = 0;
            var topRightPixels:ByteArray = captureAll(source, topRight, currentMatrix);
            pixels = mergePixelRows(pixels, topLeft.width, topRightPixels, topRight.width, topRight.height);
        }

        // If bounds height exceeded the maximum dimension for a bitmap, we
        // also need to capture the bottom-left window (recursively, until we
        // have a window height less than the max). These rows are appended 
        // to the end of the current 32-bit, 4 channel bitmap as a ByteArray.
        if (bottomLeft != null)
        {
            currentMatrix = matrix.clone();
            currentMatrix.translate(-bottomLeft.x, -bottomLeft.y);
            bottomLeft.x = 0;
            bottomLeft.y = 0;
            var bottomLeftPixels:ByteArray = captureAll(source, bottomLeft, currentMatrix);

            // If both the bounds width and bounds height exceeded the maximum
            // dimensions for a bitmap, we now must to capture the bottom-right
            // window (recursively, until we have a window with less than the
            // max width and/or height). These right side rows have to be merged
            // to the right of each left side row.
            if (bottomRight != null)
            {
                currentMatrix = matrix.clone();
                currentMatrix.translate(-bottomRight.x, -bottomRight.y);
                bottomRight.x = 0;
                bottomRight.y = 0;
                var bottomRightPixels:ByteArray = captureAll(source, bottomRight, currentMatrix);
                bottomLeftPixels = mergePixelRows(bottomLeftPixels, bottomLeft.width, bottomRightPixels, bottomRight.width, bottomRight.height);
            }

            // Append bottomLeft pixels to the end of the ByteArray of pixels
            pixels.position = pixels.length;
            pixels.writeBytes(bottomLeftPixels);
        }
        pixels.position = 0;

        return pixels;
    }

    /**
     * @private
     * Copies the rows of the right hand side of an image onto the ends of
     * the rows of the left hand side of an image. The left and right hand
     * sides must be of equal height.
     */
    private static function mergePixelRows(left:ByteArray, leftWidth:int,
        right:ByteArray, rightWidth:int, rightHeight:int):ByteArray
    {
        var merged:ByteArray = new ByteArray();
        var leftByteWidth:int = leftWidth*4;
        var rightByteWidth:int = rightWidth*4;

        for (var i:int = 0; i < rightHeight; i++)
        {
            merged.writeBytes(left, i*leftByteWidth, leftByteWidth); 
            merged.writeBytes(right, i*rightByteWidth, rightByteWidth);
        }

        merged.position = 0;
        return merged;
    }

    /**
     * @private
     * Prepare the target and its parents for image capture.
     */
    private static function prepareToPrintObject(target:IUIComponent):Array
    {
        var normalStates:Array = [];

        var obj:DisplayObject = (target is DisplayObject) ? DisplayObject(target) : null;
        var index:Number = 0;
        
        while (obj != null)
        {
            if (obj is UIComponent)
            {
                normalStates[index++] = UIComponent(obj).prepareToPrint(UIComponent(target));
            }
            else if (obj is DisplayObject && !(obj is Stage))
            {
                normalStates[index++] = DisplayObject(obj).mask;
                DisplayObject(obj).mask = null;
            }

            obj = (obj.parent is DisplayObject) ? DisplayObject(obj.parent) : null;
        }

        return normalStates;
    }

    /**
     * @private
     * Reverts the target and its parents back to a pre-capture state.
     */
    private static function finishPrintObject(target:IUIComponent, normalStates:Array):void
    {
        var obj:DisplayObject = (target is DisplayObject) ? DisplayObject(target) : null;
        var index:Number = 0;
        while (obj != null)
        {
            if (obj is UIComponent)
            {
                UIComponent(obj).finishPrint(normalStates[index++], UIComponent(target));
            }
            else if (obj is DisplayObject && !(obj is Stage))
            {
                DisplayObject(obj).mask = normalStates[index++];
            }

            obj = (obj.parent is DisplayObject) ? DisplayObject(obj.parent) : null;
        }
    }

    /**
     * The default <code>mx.graphics.codec.IImageEncoder</code> implementation
     * used to capture images. The default encoder uses the PNG format.
     */
    public static var DEFAULT_ENCODER:Class = mx.graphics.codec.PNGEncoder;
    public static var MAX_BITMAP_DIMENSION:int = 2880;

    private var _contentType:String;
    private var _data:ByteArray;    
    private var _height:int;
    private var _width:int;
}

}