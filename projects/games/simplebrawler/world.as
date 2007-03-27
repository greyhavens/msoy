package{
	import flash.display.*;
	import flash.events.*;
	import flash.geom.*;
	import flash.ui.Mouse;
	import 	flash.utils.Timer;
	
	[SWF(width="800", height="505")]
	public class world extends MovieClip{
		public var local_player:MovieClip;
		public var goal:MovieClip;
		public var hud:MovieClip;
		public var camera:MovieClip;
		public var ground_cursor:MovieClip;
		public var ground_cursor_hide:Boolean;
		public var goal_hide:Boolean;
		
		public var ground:MovieClip;
		public var bg:MovieClip;
		
		public var room_num:Number = 1;
		
		public var pc_count:Number = 0;
		public var npc_count:Number = 0;
		
		//FPS Calculator Variables
		public var timer: Timer = new Timer(1000);
		public var current_fps: Number;
		
		public var time_speed:Number = 1.0;
		public var fps:Number = 40;
		
		//Keyboard inputs
		public var punch_hit: Boolean;
		public var kick_hit: Boolean;
		public var block_hit: Boolean;
		
		public var punch_code: Number = 51;
		public var kick_code: Number = 50;
		public var block_code: Number = 49;
		
		
		public var world_width: Number = 800;
		
		public function world(){
			stage.frameRate = fps*time_speed
			stage.scaleMode = flash.display.StageScaleMode.EXACT_FIT;
			stage.addEventListener(KeyboardEvent.KEY_UP, keyReleased);
			stage.addEventListener(KeyboardEvent.KEY_DOWN, keyPressed);
        	root.loaderInfo.addEventListener(Event.UNLOAD, world_Unload);
			world_Load ();
		}
		
		//-------------------------------------TIMER---------------------------------------------
		private function onTimerEvent( e: Event):void{
			hud.fps_output.text = "FPS: "+current_fps+" : "+stage.frameRate;
			current_fps=0;
		}
		
		//-------------------------------------LOAD----------------------------------------------
    	protected function world_Load () :void
		{
			timer.addEventListener( TimerEvent.TIMER, onTimerEvent);
			timer.start();
			current_fps = 0;
			
			bg = root.camera._zoom.bg;
			ground = root.camera._zoom.bg.ground;
			ground.addEventListener(MouseEvent.CLICK, floorCLICK_handler);
			
			goal = new destination();
			bg.cursor_zone.addChild(goal);
			
			ground_cursor = new cursor();
			bg.cursor_zone.addChild(ground_cursor);
			
			local_player = create_player("Local", "PC", 50+(Math.random()*100),400+(Math.random()*100), 100, 10);
			load_npcs();
			
			bg.door_next.x = ground.width;
			bg.door_next.height = ground.height;
			
			this.hud.addEventListener("enterFrame", hud_enterFrame);
    	}
		
		//-------------------------------------UNLOAD--------------------------------------------
    	protected function world_Unload (event :Event) :void
    	{
			//Despawn Monsters, Props, and Players.
    	}
		
		//-------------------------------------ENTER ZONE----------------------------------------
		private function next_zone():void{
			exit_zone();
			
			room_num += 1;
			
			bg.bg_1.gotoAndStop(room_num);
			bg.bg_2.gotoAndStop(room_num);
			bg.bg_3.gotoAndStop(room_num);
			bg.bg_4.gotoAndStop(room_num);
			bg.bg_5.gotoAndStop(room_num);
			ground.gotoAndStop(room_num);
			bg.mobs.gotoAndStop(room_num);
			
			bg.door_next.x = ground.width;
			bg.door_next.height = ground.height;
			
			load_npcs();
			
			ground = root.camera._zoom.bg.ground;
			ground.addEventListener(MouseEvent.CLICK, floorCLICK_handler);
			//hud.fader.gotoAndPlay("in");
		}
		
		//-------------------------------------EXIT ZONE-----------------------------------------
		private function exit_zone() :void{
			//hud.fader.gotoAndPlay("out");
			player_move(local_player, 50+(Math.random()*100),400+(Math.random()*100));
			clear_npcs();
		}
		
		//-------------------------------------KEY UP--------------------------------------------
		private function keyReleased(evt:KeyboardEvent):void{
			if (evt.keyCode == punch_code){
				punch_hit = false;
			}
			if (evt.keyCode == kick_code){
				kick_hit = false;
			}
			if (evt.keyCode == block_code){
				//block_hit = true;
				player_block_stop(local_player);
			}
		}
		
		//-------------------------------------KEY DOWN-----------------------------------------
		private function keyPressed(evt:KeyboardEvent):void{
			if (evt.keyCode == punch_code){
				if (punch_hit == false){
					punch_hit = true;
					player_punch(local_player);
				}
			}
			if (evt.keyCode == kick_code){
				if (kick_hit == false){
					kick_hit = true;
					player_kick(local_player);
				}
			}
			if (evt.keyCode == block_code){
				player_block(local_player);
			}
		}
		
		//-------------------------------------UPDATE HUD----------------------------------------
		private function hud_enterFrame(e:Event){
			//---Keyboard Listeners

			//---------------------
			current_fps++;
			
			var pos:Point = new Point(local_player.x, local_player.y);
			pos = localToGlobal(pos);
			var cam_x:Number = root.camera.x;
			var cam_goal: Number = (pos.x-stage.stageWidth/2)*-1;
			
			cam_x = (cam_x-cam_goal)*0.05;

			root.camera.x -= cam_x;
			
			//Edge of World Checks
			var bg1_w:Number = bg.bg_1.width-stage.stageWidth;
			if (root.camera.x < bg1_w*(-1)){root.camera.x = bg1_w*(-1);}
			if (root.camera.x > 0){root.camera.x = 0;}
			
			update_bg();
			update_cursor();
			
			//Check For the player to enter the next zone.
			if (bg.door_next.hitTestObject(local_player.boundbox)){
				next_zone();
			}
			
			
			
		}
		
		//-------------------------------------UPDATE BG-----------------------------------------
		private function update_bg() :void{
			var cam: MovieClip = root.camera;
			var bg1: MovieClip = bg.bg_1;
			var bg2: MovieClip = bg.bg_2;
			var bg3: MovieClip = bg.bg_3;
			var bg4: MovieClip = bg.bg_4;
			var bg5: MovieClip = bg.bg_5;
			
			var bg1_w:Number = bg1.width-stage.stageWidth;
			var bg2_w:Number = bg2.width-stage.stageWidth;
			var bg3_w:Number = bg3.width-stage.stageWidth;
			var bg4_w:Number = bg4.width-stage.stageWidth;
			var bg5_w:Number = bg5.width-stage.stageWidth;
			
			var bg2_r:Number = bg2_w/bg1_w;
			var bg3_r:Number = bg3_w/bg1_w;
			var bg4_r:Number = bg4_w/bg1_w;
			var bg5_r:Number = bg5_w/bg1_w;
			
			var pos:Number = (cam.x)/bg1_w;
			
			bg2.x = bg2_w*pos+(cam.x*-1);
			bg3.x = bg3_w*pos+(cam.x*-1);
			bg4.x = bg4_w*pos+(cam.x*-1);
			bg5.x = bg5_w*pos+(cam.x*-1);
			
			if (local_player.moving == false){
				if (goal_hide == false){
					goal.gotoAndPlay("off");
					goal_hide = true;
				}
			}
			
		}
		
		//-------------------------------------UPDATE CURSOR-------------------------------------
		private function update_cursor() :void{
			var mpos: Point = new Point( root.mouseX,root.mouseY);
			if (ground.hitTestPoint(mpos.x,mpos.y)){
				//Mouse.hide();
				if (ground_cursor_hide){
					ground_cursor_hide = false;
					ground_cursor.gotoAndPlay("on");
				}
				mpos = bg.globalToLocal(mpos);
				ground_cursor.x = mpos.x;
				ground_cursor.y = mpos.y;
				player_scale(ground_cursor);
			}else{
				//Mouse.show();
				if (ground_cursor_hide == false){
					ground_cursor_hide = true;
					ground_cursor.gotoAndPlay("off");
				}
			}
		}
		
		//-------------------------------------NEW PLAYER----------------------------------------
		protected function create_player(n: String ="Unknown", t: String ="NPC", sX: Number =0, sY: Number =0, hp: Number =100, spd: Number=1) :MovieClip
    	{
			var mc: player = new player();
			mc.pName = n;
			mc.flag = t;
			
			if (mc.flag == "PC"){
				mc.name_plate.text = mc.pName;
				mc.name_plate.textColor = 0x99BFFF;
				//pc_list[pc_list.pc_count] = mc;
				mc.name = "pc_"+pc_count;
				pc_count += 1;
			} else if (mc.flag == "NPC"){
				mc.name_plate.text = "";
				mc.name_plate.textColor = 0xCC0000;
				mc.name = "npc_"+npc_count;
				npc_count += 1;
			}
			
			mc.spd = spd;
			if (mc.spd < 0){
				mc.spd = 0;
			}
			mc.hp = hp;
						
			//trace(mc.flag+" entity '"+mc.pName+"' created! ^__^");
			
			mc.y = sY;
			mc.x = sX;
			
			mc.goal_x = mc.x;
			mc.goal_y = mc.y;
			mc.start_x = mc.x;
			mc.start_y = mc.y;
			mc.move_time = 0;
			mc.move_distance = 0;
			
			mc.addEventListener("enterFrame", player_enterFrame);
			player_scale(mc);
			
			bg.actors.addChild(mc);
			
			return mc
    	}
		
		//-------------------------------------UPDATE PLAYER------------------------------------
		private function player_enterFrame(e:Event){
			if (e.target.hp > 0){
				//----------------------
				//---Moving to a  new location
				var lp_current: Point = new Point(e.target.x,e.target.y);
				var lp_start: Point = new Point(e.target.start_x,e.target.start_y);
				var lp_goal: Point = new Point(e.target.goal_x,e.target.goal_y);
				var lp_distance: Number = Point.distance(lp_current,lp_goal);
				
				if (e.target.moving){
					if (lp_distance > 5){
						e.target.move_time = e.target.move_time+e.target.spd*e.target.scaleX;
						lp_current = Point.interpolate(lp_goal,lp_start,e.target.move_time/e.target.move_distance);
					} else {
						e.target.moving = false;
					}
					e.target.x = lp_current.x;
					e.target.y = lp_current.y;
				} else {
					e.target.move_time = 0;
					e.target.move_distance = 0;
					e.target.start_x = e.target.goal_x;
					e.target.start_y = e.target.goal_y;
					e.target.x = e.target.goal_x;
					e.target.y = e.target.goal_y;
				}
				//----------------------
				
				
				//----------------------
				//---NPC AI Detection
				if (e.target.flag == "NPC"){
					player_ai(e.target);
				}
				//----------------------
				
				
				player_scale(e.target);
				player_depth(e.target);
				
				
				//----------------------
				//---Name Plate Scaling
				var nScale:Number = 1+(1-((e.target.scaleX-0.5)/0.5));
				e.target.name_plate.scaleX = nScale;
				e.target.name_plate.scaleY = nScale;
				e.target.name_plate.x = -1*e.target.name_plate.width/2;
				//----------------------
				
				
				//----------------------
				//---Direction Detection
				if (e.target.moving){
					if (e.target.x > e.target.last_x){
						e.target.character.scaleX = 1; //Face Right
						e.target.dmgbox.scaleX = 1;
					}else{
						e.target.character.scaleX = -1; //face Left
						e.target.dmgbox.scaleX = -1;
					}
				}
				e.target.last_x = e.target.x;
				//----------------------
				
				
				//----------------------
				//---Animation Detection
				if (e.target.character.currentFrame < 10 && e.target.animation_old != "idle"){
					e.target.animation = "idle";
					e.target.animation_old = "idle";
				}
				
				if (e.target.animation == "hurt"){
					
				}else if (e.target.animation == "punch"){
					
				}else if (e.target.animation == "kick"){
					
				}else if (e.target.animation == "block"){
					
				}else{
					if (e.target.moving){
						e.target.animation = "walk";
					} else {
						e.target.animation = "idle";
					}
				}
	
				if (e.target.animation != e.target.animation_old){
					e.target.character.gotoAndPlay(e.target.animation);
					
					if (e.target.animation == "punch"){
						e.target.dmgbox.gotoAndPlay(e.target.animation);
					}else if (e.target.animation == "kick"){
						e.target.dmgbox.gotoAndPlay(e.target.animation);
					}
				}
				
				e.target.animation_old = e.target.animation;
				//e.target.name_plate.text = e.target.hp;
				//----------------------
				
				
				//----------------------
				//---DAMAGE!------------
				if (e.target.animation == "punch" || e.target.animation == "kick"){
					if (e.target.flag == "PC"){
						var n:Number = 0;
						var dmg:Number = 0;
						var mov:MovieClip;
						if (npc_count){
							var t:Number = npc_count;
							while(n <= t){
								if (bg.actors.getChildByName("npc_"+n)){
									mov = bg.actors.getChildByName("npc_"+n);
									if (mov.hp > 0){
										if (e.target.dmgbox.hitTestObject(mov.boundbox)){
											//If it hits dood, hurt dood.
											if (mov.animation != "block" || mov.animation != "hurt"){
												dmg = Math.random()*100;
												player_hurt(bg.actors.getChildByName("npc_"+n),dmg);
											}
										}
									}
								}
								n++;
							}
						}
					} else if (e.target.flag == "NPC"){
					}
				}
				//----------------------
			} else {
				e.target.animation = "dead";
				if (e.target.animation != e.target.animation_old){
					e.target.character.gotoAndPlay(e.target.animation);
				}
			
				e.target.animation_old = e.target.animation;
			}
			
		}
		
		//-------------------------------------NPlayer AI---------------------------------------
		private function player_ai(mc:MovieClip){
			if (mc.moving || mc.animation == "hurt" || mc.animation == "dead"){
				
			}else{
				var max_Y: Number = ground.y;
				var min_Y: Number = ground.y-ground.height;
				
				var max_X: Number = ground.width;
				var min_X: Number = 0;
				
				var pX: Number = min_X+(max_X*((Math.random()*100)/100));
				var pY: Number = min_Y+(max_Y*((Math.random()*100)/100));
				
				if (ground.hitTestPoint(pX,pY)){
					plot_goal(mc, pX, pY);
				}
			}
		}
		
		//-------------------------------------PLAYER Z-SCALE------------------------------------
		private function player_scale(e:MovieClip){
			var ground_max: Number = ground.y;
			var ground_min: Number = ground.y-ground.height;
			ground_max = ground_max - ground_min;
			var size: Number = e.y-ground_min;
			size = ((size/ground_max)*0.5)+0.5
			e.scaleX = size;
			e.scaleY = size;
		}
		
		//-------------------------------------PLAYER Z-DEPTH-----------------------------------
		private function player_depth(e:MovieClip){
			var depth: Number = ((e.scaleX-0.5)/0.5)*e.parent.numChildren;
			if (depth > e.parent.numChildren-0.01){
				depth = e.parent.numChildren-0.01;
			}

			e.parent.setChildIndex(e,depth);
		}
		
		//-------------------------------------MOVE PLAYER---------------------------------------
    	protected function player_move (mc:MovieClip, pX:Number, pY:Number) :void
    	{
			mc.x = pX;
			mc.y = pY;
			mc.start_x = pX;
			mc.start_y = pY;
			mc.goal_x = pX;
			mc.goal_y = pY;
			mc.move_distance = 0;
			mc.move_time = 0;
			mc.moving = false;
    	}
		
		//-------------------------------------DELETE NPCS----------------------------------------
    	protected function clear_npcs () :void
    	{
			var n:Number = 0;
			if (npc_count){
				//trace("Begin deletion");
				var t:Number = npc_count;
				while(n <= t){
					//trace("Deleting npc_"+n);
					if (bg.actors.getChildByName("npc_"+n)){
						bg.actors.getChildByName("npc_"+n).removeEventListener("enterFrame", player_enterFrame);
						bg.actors.removeChild(bg.actors.getChildByName("npc_"+n));
					}
					n++;
				}
				npc_count = 0;
			}
    	}
		
		//-------------------------------------LOAD NPCS----------------------------------------
		private function load_npcs() :void{
			var n:Number = 1;
			var t:Number = bg.mobs.numChildren;
			var moo: MovieClip;
			while(n <= t){
					//trace("Loading mob_"+n);
					if (bg.mobs.getChildByName("mob_"+n)){
						moo = bg.mobs.getChildByName("mob_"+n);
						newmob = create_player(moo.mt.text, "NPC", moo.x,moo.y, moo.hp.text, moo.spd.text);
						newmob = null;
						moo.alpha = 0;
						moo.scaleX = 0;
						moo.scaleY = 0;
					}
					n++;
					moo = null;
			}
		}
		
		//-------------------------------------NAVI----------------------------------------------
		private function floorCLICK_handler(e:MouseEvent){
			var pos:Point = new Point(e.stageX, e.stageY);
			pos = bg.globalToLocal(pos);
			
			goal.x = pos.x;
			goal.y = pos.y;
			player_scale(goal);
			goal.gotoAndPlay("on");
			goal_hide = false;
			
			plot_goal(local_player, pos.x, pos.y);
		}
		
		//-------------------------------------PLOT GOAL-----------------------------------------
		private function plot_goal(mc:MovieClip, pX:Number, pY:Number){
			
			mc.start_x = mc.x;
			mc.start_y = mc.y;
			mc.goal_x = pX;
			mc.goal_y = pY;
			
			var lp_start: Point = new Point(mc.start_x,mc.start_y);
			var lp_goal: Point = new Point(mc.goal_x,mc.goal_y);
			
			mc.move_distance = Point.distance(lp_start,lp_goal);
			mc.move_time = 0;
			
			mc.moving = true;
			mc.animation = "walk";
		}
		
		
		
		
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
		//-------------------------------------PUNCH!---------------------------------------------
    	protected function player_punch (mc:MovieClip) :void
    	{	
			if (mc.animation == "idle" || mc.animation == "walk"){
				player_move(mc, mc.x, mc.y);
				mc.animation = "punch";
			}
    	}
		
		//-------------------------------------KICK!----------------------------------------------
    	protected function player_kick (mc:MovieClip) :void
    	{
			if (mc.animation == "idle" || mc.animation == "walk"){
				player_move(mc, mc.x, mc.y);
				mc.animation = "kick";
			}
    	}
		
		//-------------------------------------BLOCK!---------------------------------------------
    	protected function player_block (mc:MovieClip) :void //START
    	{
			if (mc.animation == "idle" || mc.animation == "walk"){
				player_move(mc, mc.x, mc.y);
				mc.animation = "block";
			}
    	}
    	protected function player_block_stop (mc:MovieClip) :void //STOP
    	{
			if (mc.animation == "block"){
				mc.animation = "idle";
			}
    	}
		
		//-------------------------------------HURT PLAYER----------------------------------------
		protected function player_hurt (mc:MovieClip, dmg:Number) :void
    	{	
			if (dmg > 60){// && dmg <= 80){
				root.camera.gotoAndPlay("x_light");
			}// else if (dmg > 90){
				//root.camera.gotoAndPlay("medium");
			//}
			mc.hp -= dmg;
			mc.animation = "hurt";
			player_move(mc, mc.x, mc.y);
    	}
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
	}
}