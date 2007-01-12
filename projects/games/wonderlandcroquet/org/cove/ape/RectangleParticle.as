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
	
	/**
	 * A rectangular shaped particle. 
	 */ 
	public class RectangleParticle extends AbstractParticle {
		
		/** @private */
		internal var _cornerPositions:Array;
		
		private var _cornerParticles:Array;
		private var _extents:Array;
		private var _axes:Array;
		private var _rotation:Number;
		
		
		/**
		 * @param x The initial x position.
		 * @param y The initial y position.
		 * @param width The width of this particle.
		 * @param height The height of this particle.
		 * @param rotation The rotation of this particle in radians.
		 * @param fixed Determines if the particle is fixed or not. Fixed particles
		 * are not affected by forces or collisions and are good to use as surfaces.
		 * Non-fixed particles move freely in response to collision and forces.
		 * @param mass The mass of the particle
		 * @param elasticity The elasticity of the particle. Higher values mean more elasticity.
		 * @param friction The surface friction of the particle. 
		 * <p>
		 * Note that RectangleParticles can be fixed but still have their rotation property 
		 * changed.
		 * </p>
		 */
		public function RectangleParticle (
				x:Number, 
				y:Number, 
				width:Number, 
				height:Number, 
				rotation:Number, 
				fixed:Boolean,
				mass:Number = 1, 
				elasticity:Number = 0.3,
				friction:Number = 0) {
			
			super(x, y, fixed, mass, elasticity, friction);
			
			_extents = new Array(width/2, height/2);
			_axes = new Array(new Vector(0,0), new Vector(0,0));
			this.rotation = rotation;	
		}
		
		
		/**
		 * The rotation of the RectangleParticle in radians. For drawing methods you may 
		 * want to use the <code>angle</code> property which gives the rotation in
		 * degrees from 0 to 360.
		 * 
		 * <p>
		 * Note that while the RectangleParticle can be rotated, it does not have angular
		 * velocity. In otherwords, during collisions, the rotation is not altered, 
		 * and the energy of the rotation is not applied to other colliding particles.
		 * A true rigid body is planned for a later release.
		 * </p>
		 */
		public function get rotation():Number {
			return _rotation;
		}
		
		
		/**
		 * @private
		 */		
		public function set rotation(t:Number):void {
			_rotation = t;
			setAxes(t);
		}
			
		/**
		 * An Array of 4 contact particles at the corners of the RectangleParticle. You can attach
		 * other particles or constraints to these particles. Note this is a one-way effect, meaning the
		 * RectangleParticle's motion will move objects attached to the corner particles, but the 
		 * reverse is not true. 
		 * 
		 * <p>
		 * In order to access one of the 4 corner particles, you can use array notation 
		 * e.g., <code>myRectangleParticle.cornerParticles[0]</code>
		 * </p>
		 */					
		public function get cornerParticles():Array {
			
			if (_cornerPositions == null) cornerPositions;
			
			if (_cornerParticles == null) {
				var cp1:CircleParticle = new CircleParticle(0,0,1,false);
				cp1.collidable = false;
				cp1.visible = false;
				APEngine.addParticle(cp1);
				
				var cp2:CircleParticle = new CircleParticle(0,0,1,false);
				cp2.collidable = false;
				cp2.visible = false;
				APEngine.addParticle(cp2);
	
				var cp3:CircleParticle = new CircleParticle(0,0,1,false);
				cp3.collidable = false;
				cp3.visible = false;
				APEngine.addParticle(cp3);
	
				var cp4:CircleParticle = new CircleParticle(0,0,1,false);
				cp4.collidable = false;
				cp4.visible = false;
				APEngine.addParticle(cp4);
				
				_cornerParticles = new Array(cp1, cp2, cp3, cp4);
				
				updateCornerParticles();
			}
			return _cornerParticles;
		}
		
		
		/**
		 * An Array of <code>Vector</code> objects storing the location of the 4
		 * corners of this RectangleParticle. This method would usually be called
		 * in a painting method if the locations of the corners were needed. If the
		 * RectangleParticle is being drawn using its position and angle properties 
		 * then you don't need to access this property.
		 */
		public function get cornerPositions():Array {
					
			if (_cornerPositions == null) {
				_cornerPositions = new Array(
						new Vector(0,0), 
						new Vector(0,0), 
						new Vector(0,0), 
						new Vector(0,0));
						
				 updateCornerPositions();
			}
			return _cornerPositions;
		}
		
		
		/**
		 * The default paint method for the particle. Note that you should only use
		 * the default painting methods for quick prototyping. For anything beyond that
		 * you should always write your own particle classes that extend one of the
		 * APE particle classes. Then within that class you can define your own custom
		 * painting method.
		 */
		public function paint():void {
			if (dc == null) dc = getDefaultContainer();
			
			dc.graphics.clear();
			if (! visible) return;
			
			var c:Array = cornerPositions;
			dc.graphics.lineStyle(0, 0x666666, 100);
			
			with (dc.graphics) {
				for (var j:Number = 0; j < 5; j++) {
					var i:Number = j & 3;
					if (j == 0) {
						moveTo(c[i].x, c[i].y);
					} else {
						lineTo(c[i].x, c[i].y);
					}
				}
			}
		}


		/**
		 * @private
		 */
		internal override function update(dt2:Number):void {		
			super.update(dt2);
			if (_cornerPositions != null) updateCornerPositions();
			if (_cornerParticles != null) updateCornerParticles();
		}
		
		
		/**
		 * @private
		 */	
		internal function get axes():Array {
			return _axes;
		}
		

		/**
		 * @private
		 */	
		internal function get extents():Array {
			return _extents;
		}
		
		
		// REVIEW FOR ANY POSSIBILITY OF PRECOMPUTING
		/**
		 * @private
		 */	
		internal override function getProjection(axis:Vector):Interval {
			
			var radius:Number =
			    extents[0] * Math.abs(axis.dot(axes[0]))+
			    extents[1] * Math.abs(axis.dot(axes[1]));
			
			var c:Number = curr.dot(axis);
			
			interval.min = c - radius;
			interval.max = c + radius;
			return interval;
		}


		/**
		 * @private
		 */	
		internal function updateCornerPositions():void {
		
			var ae0_x:Number = _axes[0].x * _extents[0];
			var ae0_y:Number = _axes[0].y * _extents[0];
			var ae1_x:Number = _axes[1].x * _extents[1];
			var ae1_y:Number = _axes[1].y * _extents[1];
			
			var emx:Number = ae0_x - ae1_x;
			var emy:Number = ae0_y - ae1_y;
			var epx:Number = ae0_x + ae1_x;
			var epy:Number = ae0_y + ae1_y;
			
			_cornerPositions[0].x = curr.x - epx;
			_cornerPositions[0].y = curr.y - epy;
			_cornerPositions[1].x = curr.x + emx;
			_cornerPositions[1].y = curr.y + emy;
			_cornerPositions[2].x = curr.x + epx;
			_cornerPositions[2].y = curr.y + epy;
			_cornerPositions[3].x = curr.x - emx;
			_cornerPositions[3].y = curr.y - emy;
		}
		
		
		/**
		 * 
		 */
		private function updateCornerParticles():void {
			for (var i:int = 0; i < 4; i++) {
				_cornerParticles[i].px = _cornerPositions[i].x; 
				_cornerParticles[i].py = _cornerPositions[i].y; 
			}	
		}


		/**
		 * 
		 */					
		private function setAxes(t:Number):void {
			var s:Number = Math.sin(t);
			var c:Number = Math.cos(t);
			
			axes[0].x = c;
			axes[0].y = s;
			axes[1].x = -s;
			axes[1].y = c;
		}
	}
}
