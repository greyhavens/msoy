/*
APE (Actionscript Physics Engine) is an AS3 open source 2D physics engine
Copyright 2006, Alec Cove 

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA

Contact: ape@cove.org
*/
package org.cove.ape {
	
	import flash.display.Sprite;
	
	/**
	 * The abstract base class for all constraints. 
	 * 
	 * <p>
	 * You should not instantiate this class directly -- instead use one of the subclasses.
	 * </p>
	 */
	public class AbstractConstraint {
		
		/** @private */
		protected var dc:Sprite;
		
		
		private var _visible:Boolean;
		private var _stiffness:Number;

		/** 
		 * @private
		 */
		public function AbstractConstraint (stiffness:Number = 0.4) {	
			visible = true;
			this.stiffness = stiffness;
		}
			
		
		/**
		 * The stiffness of the constraint. Higher values result in result in 
		 * stiffer constraints. Values should be greater than 0 and less than or 
		 * equal to 1. Depending on the situation, setting constraints to very high 
		 * values may result in instability or unwanted energy.
		 */  
		 
		public function get stiffness():Number {
			return _stiffness;
		}
		
		
		/**
		 * @private
		 */			
		public function set stiffness(s:Number):void {
			_stiffness = s;
		}
		
		
		/**
		 * The visibility of the constraint. This is only implemented for the default painting
		 * methods of the constraints. When you create your painting methods in subclassed constraints 
		 * or composites you should add a check for this property.
		 */			
		public function get visible():Boolean {
			return _visible;
		}	
		
		
		/**
		 * @private
		 */			
		public function set visible(v:Boolean):void {
			_visible = v;
		}
		
				
		/**
		 * @private
		 */
		internal function resolve():void {
		}
		
		
		/**
		 * @private
		 */
		protected function getDefaultContainer():Sprite {
			if (APEngine.defaultContainer == null) {
				var err:String = "";
				err += "You must set the defaultContainer property of the APEngine class ";
				err += "if you wish to use the default paint methods of the constraints";
				throw new Error(err);
			}
			var parentContainer:Sprite = APEngine.defaultContainer;
			var defaultContainer:Sprite = new Sprite();
			parentContainer.addChild(defaultContainer);
			return defaultContainer;
		}
	}
}
