////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.skins
{

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Graphics;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Shape;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.geom.Rectangle;
import flash.net.URLRequest;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.utils.getDefinitionByName;
import mx.core.Container;
import mx.core.EdgeMetrics;
import mx.core.FlexLoader;
import mx.core.FlexShape;
import mx.core.IChildList;
import mx.core.mx_internal;
import mx.managers.ISystemManager;
import mx.managers.SystemManager;
import mx.resources.ResourceBundle;
import mx.styles.ISimpleStyleClient;
import mx.utils.StringUtil;

use namespace mx_internal;

/**
 *  The RectangularBorder class is an abstract base class for various classes
 *  that draw rectangular borders around UIComponents.
 *
 *  <p>This class implements support for the <code>backgroundImage</code>,
 *  <code>backgroundSize</code>, and <code>backgroundAttachment</code> styles.</p>
 */
public class RectangularBorder extends Border
{
	include "../core/Version.as";
	
	//--------------------------------------------------------------------------
	//
	//  Class initialization
	//
	//--------------------------------------------------------------------------

	loadResources();

	//--------------------------------------------------------------------------
	//
	//  Class resources
	//
	//--------------------------------------------------------------------------

	[ResourceBundle("skins")]

    /**
	 *  @private
     */	
	private static var packageResources:ResourceBundle;

    /**
	 *  @private
     */	
	private static var resourceNotLoaded:String;

	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private    
     *  Loads resources for this class.
     */
	private static function loadResources():void
	{
		resourceNotLoaded = packageResources.getString("notLoaded");
	}
	
	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function RectangularBorder()
	{
		super();
		
		addEventListener(Event.REMOVED, removedHandler);
	}
			
	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  The value of the backgroundImage style may be either a string
	 *  or a Class pointer. Either way, the value of the backgroundImage
	 *  style is stored here, so that we can detect when it changes.
	 */
	private var backgroundImageStyle:Object
						
	/**
	 *  @private
	 *  Original width of background image, before it is scaled.
	 */
	private var	backgroundImageWidth:Number;

	/**
	 *  @private
	 *  Original height of background image, before it is scaled.
	 */	
	private var	backgroundImageHeight:Number;

	/**
	 *  @private
	 *  The loader instance used to load an external background image
	 */	
	mx_internal var loader:Loader;
				
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  backgroundImage
	//----------------------------------

	/**
	 *  The DisplayObject containing the background image, if any.
	 *  This object is a sibling of the RectangularBorder.
	 */	
	private var backgroundImage:DisplayObject;

	/**
	 *  @private
	 *  Return true if we have a background image.
	 */
	mx_internal function get hasBackgroundImage():Boolean
	{
		return backgroundImage != null;
	}
	
	//----------------------------------
	//  backgroundImageRect
	//----------------------------------

	/**
	 *  @private
	 *  Storage for backgroundImageRect property.
	 */
	private var _backgroundImageRect:Rectangle;

	/**
	 *  @private
	 *  Rectangular area within which to draw the background image.
	 *
	 *  This can be larger than the dimensions of the border
	 *  if the parent container has scrollable content.
	 *  If this property is null, the border can use
	 *  the parent's size and viewMetrics to determine its value.
	 */
	mx_internal function get backgroundImageRect():Rectangle
	{
		return _backgroundImageRect;
	}

	/**
	 *  @private
	 */
	mx_internal function set backgroundImageRect(value:Rectangle):void
	{
		_backgroundImageRect = value;

		invalidateDisplayList();
	}

	//----------------------------------
	//  backgroundMask
	//----------------------------------

	/**
	 *  @private
	 */	
	private var backgroundMask:Shape;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override protected function updateDisplayList(unscaledWidth:Number,
												  unscaledHeight:Number):void
	{
		if (!parent)
			return;
			
		// If background image has changed, then load new one.	
		var newStyle:Object = getStyle("backgroundImage");
		if (newStyle != backgroundImageStyle)
		{
			backgroundImageStyle = newStyle;
			
			// Discard old background image.
			if (backgroundImage)
			{
				var imgParent:DisplayObjectContainer = backgroundImage.parent;
				var childrenList:IChildList =
						imgParent is Container ?
						Container(imgParent).rawChildren :
						IChildList(imgParent);			
				childrenList.removeChild(backgroundImage);
			}
			backgroundImage = null;

			// The code below looks a lot like Loader.loadContent().

			var cls:Class;

			// The "as" operator checks to see if newStyle
			// can be coerced to a Class.
			if (newStyle && newStyle as Class)
			{
				// Load background image given a class pointer
				cls = Class(newStyle);
				initBackgroundImage(new cls());
			}
			else if (newStyle && newStyle is String)
			{
				try
				{
					cls = Class(getDefinitionByName(String(newStyle)));
				}
				catch(e:Error)
				{
					// ignore
				}

				if (cls)
				{
					var newStyleObj:DisplayObject = new cls();
					initBackgroundImage(newStyleObj);
				}
				else
				{
					// This code is a subset of Loader.loadContent().

					// Load background image from external URL.
					loader = new FlexLoader();
					loader.contentLoaderInfo.addEventListener(
						Event.COMPLETE, completeEventHandler);
					loader.contentLoaderInfo.addEventListener(
						IOErrorEvent.IO_ERROR, errorEventHandler);
					loader.contentLoaderInfo.addEventListener(
						ErrorEvent.ERROR, errorEventHandler);
					var loaderContext:LoaderContext = new LoaderContext();
					loaderContext.applicationDomain = new ApplicationDomain(ApplicationDomain.currentDomain);
					loader.load(new URLRequest(String(newStyle)), loaderContext);		
				}
			}
			else if (newStyle) 
			{
				throw new Error(StringUtil.substitute(resourceNotLoaded, newStyle));
			}
		}

		if (backgroundImage)
			mx_internal::adjustBackgroundImage();
	}

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private function initBackgroundImage(image:DisplayObject):void
	{
		backgroundImage = image;
		
		if (image is Loader)
		{
			backgroundImageWidth = Loader(image).contentLoaderInfo.width;
			backgroundImageHeight = Loader(image).contentLoaderInfo.height;
		}
		else
		{
			backgroundImageWidth = backgroundImage.width;
			backgroundImageHeight = backgroundImage.height;
			
			if (image is ISimpleStyleClient)
			{
				// Set the image's styleName to our styleName. We
				// can't set styleName to this since we aren't an
				// IStyleClient.
				ISimpleStyleClient(image).styleName = styleName;
			}
		}
		// To optimize memory use, we've declared RectangularBorder to be a Shape.
		// As a result, it cannot have any children.
		// Make the backgroundImage a sibling of this RectangularBorder,
		// which is positioned just on top of the RectangularBorder.
		var childrenList:IChildList = parent is Container ?
										 Container(parent).rawChildren :
										 IChildList(parent);

		backgroundMask = new FlexShape();
		backgroundMask.name = "backgroundMask";
		backgroundMask.x = 0;
		backgroundMask.y = 0;
		childrenList.addChild(backgroundMask);

		var myIndex:int = childrenList.getChildIndex(this);
		childrenList.addChildAt(backgroundImage, myIndex + 1);					
		
        backgroundImage.mask = backgroundMask;
	}

	/**
	 *  @private
	 */
	mx_internal function adjustBackgroundImage():void 
	{
		var p:DisplayObject = parent;
		
		var bm:EdgeMetrics = p is Container ?
							 Container(p).viewMetrics :
							 borderMetrics;
		
		var scrollableBk:Boolean =
			getStyle("backgroundAttachment") != "fixed";

		if (_backgroundImageRect)
		{
			var sW:Number = _backgroundImageRect.width;
			var sH:Number = _backgroundImageRect.height;
		}
		else
		{
			sW = width - bm.left - bm.right;
			sH = height - bm.top - bm.bottom;
		}

		// Scale according to backgroundSize.
		var percentage:Number = getBackgroundSize();
		if (isNaN(percentage))
		{
			var sX:Number = 1.0;
			var sY:Number = 1.0;
		}
		else
		{
			var scale:Number = percentage * 0.01;
			sX = scale * sW / backgroundImageWidth;
			sY = scale * sH / backgroundImageHeight;
		}
		backgroundImage.scaleX = sX;
		backgroundImage.scaleY = sY;

		// Center everything.
		// Use a scrollRect to position and clip the image.
		var offsetX:Number =
				Math.round(0.5 * (sW - backgroundImageWidth * sX));
		var offsetY:Number =
				Math.round(0.5 * (sH - backgroundImageHeight * sY));

		backgroundImage.x = bm.left;
		backgroundImage.y = bm.top;

		backgroundMask.x = bm.left;
		backgroundMask.y = bm.top;

  		// Adjust offsets by scroll positions.
  		if (scrollableBk && p is Container)
  		{
  			offsetX -= Container(p).horizontalScrollPosition;
  			offsetY -= Container(p).verticalScrollPosition;
  		}

		// Adjust alpha to match backgroundAlpha
		backgroundImage.alpha = getStyle("backgroundAlpha");
		
		backgroundImage.x += offsetX;
		backgroundImage.y += offsetY;

		var maskWidth:Number = width - bm.left - bm.right;
		var maskHeight:Number = height - bm.top - bm.bottom; 
		if (backgroundMask.width != maskWidth ||
			backgroundMask.height != maskHeight)
        {
	        var g:Graphics = backgroundMask.graphics;
        	g.clear();
	        g.beginFill(0xFFFFFF);
	        g.drawRect(0, 0, maskWidth, maskHeight);
	        g.endFill();
        }
	}

	/**
	 *  @private
	 */
	private function getBackgroundSize():Number
	{	
		var percentage:Number =	NaN;
		var backgroundSize:Object = getStyle("backgroundSize");

		if (backgroundSize && backgroundSize is String)
		{
			var index:int = backgroundSize.indexOf("%");
			if (index != -1)
				percentage = Number(backgroundSize.substr(0, index));
		}
		
		return percentage;
	}

	//--------------------------------------------------------------------------
	//
	//  Event handlers
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private function errorEventHandler(event:Event):void 
	{
		// Ignore errors that occure during background image loading.	
	}
	
	/**
	 *  @private
	 */
	private function completeEventHandler(event:Event):void 
	{
		if (!parent)
			return;
			
		var target:DisplayObject = DisplayObject(LoaderInfo(event.target).loader);
		initBackgroundImage(target);
		mx_internal::adjustBackgroundImage();
		//  rebroadcast for automation support
		dispatchEvent(event.clone());
	}
	
	/**
	 *  @private
	 */
	private function removedHandler(event:Event):void
	{
		var childrenList:IChildList = parent is Container ?
										 Container(parent).rawChildren :
										 IChildList(parent);

		if (backgroundImage)
		{
			childrenList.removeChild(backgroundImage);
			backgroundImage = null;
		}
		
		if (backgroundMask)
		{
			childrenList.removeChild(backgroundMask);
			backgroundMask = null;
		}
	}
}

}
