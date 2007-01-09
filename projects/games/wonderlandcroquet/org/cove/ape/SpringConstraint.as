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
	import flash.display.Shape;
	
	
	/**
	 * A Spring-like constraint that connects two particles
	 */
	public class SpringConstraint extends AbstractConstraint {
		
		private var p1:AbstractParticle;
		private var p2:AbstractParticle;
		
		private var restLen:Number;

		private var delta:Vector;
		private var deltaLength:Number;
		
		private var _collisionRectWidth:Number;
		private var _collisionRectScale:Number;
		private var _collidable:Boolean;
		private var collisionRect:SpringConstraintParticle;
		
		/**
		 * @param p1 The first particle this constraint is connected to.
		 * @param p2 The second particle this constraint is connected to.
		 * @param stiffness The strength of the spring. Valid values are between 0 and 1. Lower values
		 * result in softer springs. Higher values result in stiffer, stronger springs.
		 */
		public function SpringConstraint(
				p1:AbstractParticle, 
				p2:AbstractParticle, 
				stiffness:Number = 0.5) {
			
			super(stiffness);
			this.p1 = p1;
			this.p2 = p2;
			checkParticlesLocation();
			
			collisionRectWidth = 1;
			collisionRectScale = 1;
			collidable = false;
			
			delta = p1.curr.minus(p2.curr);
			deltaLength = p1.curr.distance(p2.curr);
			restLength = deltaLength;
		}
		
		
		/**
		 * The rotational value created by the positions of the two particles attached to this
		 * SpringConstraint. You can use this property to in your own painting methods, along with the 
		 * center property.
		 */			
		public function get rotation():Number {
			return Math.atan2(delta.y, delta.x);;
		}
		
		
		/**
		 * The center position created by the relative positions of the two particles attached to this
		 * SpringConstraint. You can use this property to in your own painting methods, along with the 
		 * rotation property.
		 * 
		 * @returns A Vector representing the center of this SpringConstraint
		 */			
		public function get center():Vector {
			return (p1.curr.plus(p2.curr)).divEquals(2);
		}
		
		
		/**
		 * If the <code>collidable</code> property is true, you can set the scale of the collidible area
		 * between the two attached particles. Valid values are from 0 to 1. If you set the value to 1, then
		 * the collision area will extend all the way to the two attached particles. Setting the value lower
		 * will result in an collision area that spans a percentage of that distance.
		 */			
		public function get collisionRectScale():Number {
			return _collisionRectScale;
		}
		
		
		/**
		 * @private
		 */			
		public function set collisionRectScale(scale:Number):void {
			_collisionRectScale = scale;
		}		
		

		/**
		 * If the <code>collidable</code> property is true, you can set the width of the collidible area
		 * between the two attached particles. Valid values are greater than 0. If you set the value to 10, then
		 * the collision area will be 10 pixels wide. The width is perpendicular to a line connecting the two 
		 * particles
		 */				
		public function get collisionRectWidth():Number {
			return _collisionRectWidth;
		}
		
		
		/**
		 * @private
		 */			
		public function set collisionRectWidth(w:Number):void {
			_collisionRectWidth = w;
		}
		
		
		/**
		 * The <code>restLength</code> property sets the length of SpringConstraint. This value will be
		 * the distance between the two particles unless their position is altered by external forces. The
		 * SpringConstraint will always try to keep the particles this distance apart.
		 */			
		public function get restLength():Number {
			return restLen;
		}
		
		
		/**
		 * @private
		 */	
		public function set restLength(r:Number):void {
			restLen = r;
		}
	
	
		/**
		 * Determines if the area between the two particles is tested for collision. If this value is on
		 * you can set the <code>collisionRectScale</code> and <code>collisionRectWidth</code> properties 
		 * to alter the dimensions of the collidable area.
		 */			
		public function get collidable():Boolean {
			return _collidable;
		}
	
				
		/**
		 * @private
		 */		
		public function set collidable(b:Boolean):void {
			_collidable = b;
			if (_collidable) {
				collisionRect = new SpringConstraintParticle(p1, p2);
				orientCollisionRectangle();
			} else {
				collisionRect = null;
			}
		}
		
		
		/**
		 * Returns true if the passed particle is one of the particles specified in the constructor.
		 */		
		public function isConnectedTo(p:AbstractParticle):Boolean {
			return (p == p1 || p == p2);
		}
		
		
		/**
		 * The default paint method for the constraint. Note that you should only use
		 * the default painting methods for quick prototyping. For anything beyond that
		 * you should always write your own classes that either extend one of the
		 * APE particle and constraint classes, or is a composite of them. Then within that 
		 * class you can define your own custom painting method.
		 */			
		public function paint():void {
			
			if (dc == null) dc = getDefaultContainer();
			
			if (collidable) {
				collisionRect.paint();
			} else {
				dc.graphics.clear();
				if (! visible) return;
				dc.graphics.lineStyle(0, 0x666666, 100);
				dc.graphics.moveTo(p1.curr.x, p1.curr.y);
				dc.graphics.lineTo(p2.curr.x, p2.curr.y);
			}
		}
		
		
		/**
		 * @private
		 */
		internal override function resolve():void {
			
			if (p1.fixed && p2.fixed) return;
			
			delta = p1.curr.minus(p2.curr);
			deltaLength = p1.curr.distance(p2.curr);
			if (collidable) orientCollisionRectangle();
			
			var diff:Number = (deltaLength - restLength) / deltaLength;
			var dmd:Vector = delta.mult(diff * stiffness);
	
			var invM1:Number = p1.invMass;
			var invM2:Number = p2.invMass;
			var sumInvMass:Number = invM1 + invM2;
			
			// REVIEW TO SEE IF A SINGLE FIXED PARTICLE IS RESOLVED CORRECTLY
			if (! p1.fixed) p1.curr.minusEquals(dmd.mult(invM1 / sumInvMass));
			if (! p2.fixed) p2.curr.plusEquals( dmd.mult(invM2 / sumInvMass));
		}
		
		
		/**
		 * @private
		 */		
		internal function getCollisionRect():RectangleParticle {
			return collisionRect;
		}
	
	
		/**
		 * @private
		 */	
		private function orientCollisionRectangle():void {
	
			var c:Vector = center;
			var rot:Number = rotation;
			
			collisionRect.curr.setTo(c.x, c.y);
			collisionRect.extents[0] = (deltaLength / 2) * collisionRectScale;
			collisionRect.extents[1] = collisionRectWidth / 2;
			collisionRect.rotation = rot;
		}
	
	
		/**
		 * if the two particles are at the same location warn the user
		 */
		private function checkParticlesLocation():void {
			if (p1.curr.x == p2.curr.x && p1.curr.y == p2.curr.y) {
				throw new Error("The two particles specified for a SpringContraint can't be at the same location");
			}
		}
	}
}
