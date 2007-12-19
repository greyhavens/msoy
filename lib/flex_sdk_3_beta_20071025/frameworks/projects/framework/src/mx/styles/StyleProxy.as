package mx.styles
{
	import mx.core.mx_internal;
	import mx.core.FlexVersion;
	
	use namespace mx_internal;
	
	public class StyleProxy implements IStyleClient
	{
		public function StyleProxy(source:IStyleClient, filterMap:Object)
		{
			super();
			this.filterMap = filterMap;
			this.source = source;
		}
		
		private var _filterMap:Object;
		
		public function get filterMap():Object
		{
			return FlexVersion.compatibilityVersion < FlexVersion.VERSION_3_0 ? null : _filterMap;
		}
		
		public function set filterMap(value:Object):void
		{
			_filterMap = value;
		}
		
		private var _source:IStyleClient;
		
		public function get source():IStyleClient
		{
			return _source;
		}
		
		public function set source(value:IStyleClient):void
		{
			_source = value;
		}
		
		public function get className():String
		{
			return _source.className;
		}
		
		private var _inheritingStyles:Object;
		
		public function get inheritingStyles():Object
		{
			return _source.inheritingStyles;
		}
		
		public function set inheritingStyles(value:Object):void
		{
			// This should never happen	
		}
		
		public function get styleName():Object
		{
			if (_source.styleName is IStyleClient)
				return new StyleProxy(IStyleClient(_source.styleName), filterMap);
			else
				return _source.styleName;
			
			//return new StyleProxy(IStyleClient(_source.styleName), filterMap);
		}
		
		public function set styleName(value:Object):void
		{
			_source.styleName = value;
		}
		
		private var _nonInheritingStyles:Object;
		
		public function get nonInheritingStyles():Object
		{
			return FlexVersion.compatibilityVersion < FlexVersion.VERSION_3_0 ? _source.nonInheritingStyles : null; // This will always need to get reconstructed
		}
		
		public function set nonInheritingStyles(value:Object):void
		{
			// This should never happen
		}
		
		public function styleChanged(styleProp:String):void
		{
			return _source.styleChanged(styleProp);
		}
		
		public function get styleDeclaration():CSSStyleDeclaration
		{
			return _source.styleDeclaration;
		}
		
		public function set styleDeclaration(value:CSSStyleDeclaration):void
		{
			_source.styleDeclaration = styleDeclaration;
		}
		
		public function getStyle(styleProp:String):*
		{
			return _source.getStyle(styleProp);
		}
		
		public function setStyle(styleProp:String, newValue:*):void
		{
			_source.setStyle(styleProp, newValue);
		}
		
		public function clearStyle(styleProp:String):void
		{
			_source.clearStyle(styleProp);
		}
		
		public function getClassStyleDeclarations():Array
		{
			return _source.getClassStyleDeclarations();
		}
		
		public function notifyStyleChangeInChildren(styleProp:String, recursive:Boolean):void
		{
			return _source.notifyStyleChangeInChildren(styleProp, recursive);
		}
		
		public function regenerateStyleCache(recursive:Boolean):void
		{
			_source.regenerateStyleCache(recursive);
			return;
		}
		
		public function registerEffects(effects:Array):void
		{
			return _source.registerEffects(effects);
		}
		
		public function regenerateStyles():void
		{
			/*_inheritingStyles = {};
			_nonInheritingStyles = {};
			
			for (var i:String in _filterMap)
			{
				if (StyleManager.inheritingStyles[i])
					_inheritingStyles[i] = _source.inheritingStyles[i];
				else
					_nonInheritingStyles[i] = _source.nonInheritingStyles[i];
			}
			
			 _inheritingStyles = _source.inheritingStyles;
			_nonInheritingStyles = _source.nonInheritingStyles;
			
			for (var i:String in _filterMap)
			{
				if (StyleManager.inheritingStyles[i])
					delete _inheritingStyles[i];
				else
					delete _nonInheritingStyles[i];
			} */
			
			/* _inheritingStyles = {};
			_nonInheritingStyles = {};
			
			if (_source && _filterMap)
			{
				for (var i:String in _source.inheritingStyles)
				{
					if (_filterMap[i] == null)
						_inheritingStyles[i] = _source.inheritingStyles[i];
				}
				
				for (var j:String in _source.nonInheritingStyles)
				{
					if (_filterMap[j] == null)
						_nonInheritingStyles[j] = _source.nonInheritingStyles[j];
				} 
			}  */
			
			/* if (_source)
			{
				_inheritingStyles = _source.inheritingStyles;
				_nonInheritingStyles = _source.nonInheritingStyles;
			} */
		}
		
	}
}