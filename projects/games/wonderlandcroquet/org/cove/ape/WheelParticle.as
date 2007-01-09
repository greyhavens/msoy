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
	 * A particle that simulates the behavior of a wheel 
	 */ 
	public class WheelParticle extends CircleParticle {
	
		private var rp:RimParticle;
		private var tan:Vector;	
		private var normSlip:Vector;
		
		private var _edgePositions:Array;
		private var _edgeParticles:Array;
		private var _traction:Number;
		
	
		/**
		 * @param x The initial x position.
		 * @param y The initial y position.
		 * @param radius The radius of this particle.
		 * @param angle The rotation of this particle in radians.
		 * @param fixed Determines if the particle is fixed or not. Fixed particles
		 * are not affected by forces or collisions and are good to use as surfaces.
		 * Non-fixed particles move freely in response to collision and forces.
		 * @param mass The mass of the particle
		 * @param elasticity The elasticity of the particle. Higher values mean more elasticity.
		 * @param friction The surface friction of the particle. 
		 * <p>
		 * Note that WheelParticles can be fixed but still have their rotation property 
		 * changed.
		 * </p>
		 */
		public function WheelParticle(
				x:Number, 
				y:Number, 
				radius:Number, 
				fixed:Boolean, 
				mass:Number = 1, 
				elasticity:Number = 0.3,
				friction:Number = 0,
				traction:Number = 1) {
	
			super(x,y,radius,fixed, mass, elasticity, friction);
			tan = new Vector(0,0);
			normSlip = new Vector(0,0);
			rp = new RimParticle(radius, 2); 	
			
			this.traction = traction;	
		}	
	
		
		/**
		 * The angular velocity of the WheelParticle. You can alter this value to make the 
		 * WheelParticle spin.
		 */
		public function get angularVelocity():Number {
			return rp.angularVelocity;
		}
		
		
		/**
		 * @private
		 */		
		public function set angularVelocity(a:Number):void {
			rp.angularVelocity = a;
		}
	

		/**
		 * The amount of traction during a collision. This property controls how much traction is 
		 * applied when the WheelParticle is in contact with another particle. If the value is set
		 * to 0, there will be no traction and the WheelParticle will behave as if the 
		 * surface was totally slippery, like ice. Acceptable values are between 0 and 1. 
		 * 
		 * <p>
		 * Note that the friction property behaves differently than traction. If the surface 
		 * friction is set high during a collision, the WheelParticle will move slowly as if
		 * the surface was covered in glue.
		 * </p>
		 */		
		public function get traction():Number {
			return 1 - _traction;
		}
	
	
		/**
		 * @private
		 */				
		public function set traction(t:Number):void {
			_traction = 1 - t;
		}
		
		
		/**
		 * An Array of 4 contact particles on the rim of the wheel.  The edge particles
		 * are positioned relatively at 12, 3, 6, and 9 o'clock positions. You can attach other
		 * particles or constraints to these particles. Note this is a one-way effect, meaning the
		 * WheelParticle's motion will move objects attached to the edge particles, but the reverse
		 * is not true. 
		 * 
		 * <p>
		 * In order to access one of the 4 edge particles, you can use array notation 
		 * e.g., <code>myWheelParticle.edgeParticles[0]</code>
		 * </p>
		 */			
		public function get edgeParticles():Array {
			
			if (_edgePositions == null) edgePositions;
			
			if (_edgeParticles == null) {
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
			
				_edgeParticles = new Array(cp1, cp2, cp3, cp4);
				updateEdgeParticles();
			}
			return _edgeParticles;
		}
	
	
		/**
		 * An Array of 4 <code>Vector</code> objects storing the location of the 4
		 * edge positions of this WheelParticle. The edge positions
		 * are located relatively at the 12, 3, 6, and 9 o'clock positions.
		 */
		public function get edgePositions():Array {
					
			if (_edgePositions == null) {
				_edgePositions = new Array(
						new Vector(0,0), 
						new Vector(0,0), 
						new Vector(0,0), 
						new Vector(0,0));
						
				updateEdgePositions();
			}
			return _edgePositions;
		}
		
		
		/**
		 * The default paint method for the particle. Note that you should only use
		 * the default painting methods for quick prototyping. For anything beyond that
		 * you should always write your own classes that either extend one of the
		 * APE particle and constraint classes, or is a composite of them. Then within that 
		 * class you can define your own custom painting method.
		 */	
		public override function paint():void {
			
			var px:Number = curr.x;
			var py:Number = curr.y;
			var rx:Number = rp.curr.x;
			var ry:Number = rp.curr.y;
			
			if (dc == null) dc = getDefaultContainer();
			dc.graphics.clear();
			if (! visible) return;
			
			dc.graphics.lineStyle(0, 0x666666, 100);
			
			// draw rim cross
			dc.graphics.moveTo(px, py);
			dc.graphics.lineTo(rx + px, ry + py);
			
			dc.graphics.moveTo(px, py);
			dc.graphics.lineTo(-rx + px, -ry + py);
			
			dc.graphics.moveTo(px, py);
			dc.graphics.lineTo(-ry + px, rx + py);
			
			dc.graphics.moveTo(px, py);
			dc.graphics.lineTo(ry + px, -rx + py);
			
			// draw wheel circle
			dc.graphics.drawCircle(curr.x, curr.y, radius);
		}
	
	
		/**
		 * @private
		 */			
		internal override function update(dt:Number):void {
			super.update(dt);
			rp.update(dt);
			
			if (_edgePositions != null) updateEdgePositions();
			if (_edgeParticles != null) updateEdgeParticles();
		}
	
	
		/**
		 * @private
		 */		
		internal override function resolveCollision(
				mtd:Vector, 
				velocity:Vector, 
				normal:Vector,
				depth:Number,
				order:Number):void {
					
			super.resolveCollision(mtd, velocity, normal, depth, order);
			resolve(normal.mult(sign(depth * order)));
		}
		
	
		/**
		 * simulates torque/wheel-ground interaction - n is the surface normal
		 * Origins of this code thanks to Raigan Burns, Metanet software
		 */
		private function resolve(n:Vector):void {
	
			// this is the tangent vector at the rim particle
			tan.setTo(-rp.curr.y, rp.curr.x);
	
			// normalize so we can scale by the rotational speed
			tan = tan.normalize();
	
			// velocity of the wheel's surface 
			var wheelSurfaceVelocity:Vector = tan.mult(rp.speed);
			
			// the velocity of the wheel's surface relative to the ground
			var combinedVelocity:Vector = velocity.plusEquals(wheelSurfaceVelocity);
		
			// the wheel's comb velocity projected onto the contact normal
			var cp:Number = combinedVelocity.cross(n);
	
			// set the wheel's spinspeed to track the ground
			tan.multEquals(cp);
			rp.prev.copy(rp.curr.minus(tan));
	
			// some of the wheel's torque is removed and converted into linear displacement
			var slipSpeed:Number = (1 - _traction) * rp.speed;
			normSlip.setTo(slipSpeed * n.y, slipSpeed * n.x);
			curr.plusEquals(normSlip);
			rp.speed *= _traction;	
		}
		
		
		/**
		 *
		 */	
		private function updateEdgePositions():void {
			
			var px:Number = curr.x;
			var py:Number = curr.y;
			var rx:Number = rp.curr.x;
			var ry:Number = rp.curr.y;
			
			_edgePositions[0].setTo( rx + px,  ry + py);
			_edgePositions[1].setTo(-ry + px,  rx + py);
			_edgePositions[2].setTo(-rx + px, -ry + py);
			_edgePositions[3].setTo( ry + px, -rx + py);	
		}


		/**
		 *
		 */	
		private function updateEdgeParticles():void {
			for (var i:int = 0; i < 4; i++) {
				_edgeParticles[i].px = _edgePositions[i].x; 
				_edgeParticles[i].py = _edgePositions[i].y; 
			}	
		}
		
		
		/**
		 * Returns 1 if the value is >= 0. Returns -1 if the value is < 0.
		 */	
		private function sign(val:Number):int {
			if (val < 0) return -1
			return 1;
		}
	}
}


