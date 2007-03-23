package{
	import flash.display.*;
	import flash.events.*;
	import flash.geom.*;
	import flash.ui.Mouse;
	
	[SWF(width="800", height="505")]
	public class world extends MovieClip{
		public var local_player:MovieClip;
		public var goal:MovieClip;
		public var hud:MovieClip;
		public var camera:MovieClip;
		public var ground_cursor:MovieClip;
		public var ground_cursor_hide:Boolean;
		
		public var ground:MovieClip;
		public var bg:MovieClip;
		
		public var pc_list:Array = new Array();
		//public var pc_total: Number = 0;
		public var actor_list:Array = new Array();
		//public var npc_total: Number = 0;
		
		public var time_speed:Number = 1.0;
		public var fps:Number = 40;
		
		public var world_width: Number = 800;
		
		public function world(){
			stage.frameRate = fps*time_speed
        	root.loaderInfo.addEventListener(Event.UNLOAD, world_Unload);
			world_Load ();
		}
		
		//-------------------------------------LOAD----------------------------------------------
    	protected function world_Load () :void
		{
			bg = root.camera._zoom.bg;
			ground = root.camera._zoom.bg.ground;
			ground.addEventListener(MouseEvent.CLICK, floorCLICK_handler);
			
			goal = new destination();
			bg.cursor_zone.addChild(goal);
			
			ground_cursor = new cursor();
			bg.cursor_zone.addChild(ground_cursor);
			
			local_player = create_player("Local", "PC", 100,400);
			local_player.speed = 10;
			testenemy1 = create_player("Enemy1", "NPC", 500,400);
			testenemy2 = create_player("Enemy2", "NPC", 600,400);
			testenemy3 = create_player("Enemy3", "NPC", 700,400);
			testenemy4 = create_player("Enemy4", "NPC", 800,400);
			testenemy5 = create_player("Enemy5", "NPC", 500,400);
			testenemy6 = create_player("Bob", "NPC", 600,400);
			testenemy7 = create_player("Enemy7", "NPC", 700,400);
			testenemy8 = create_player("Enemy8", "NPC", 800,400);
			
			this.hud.addEventListener("enterFrame", hud_enterFrame);
			
			//bg.setChildIndex(ground,1);
			//bg.setChildIndex(goal,0.1);
			//bg.setChildIndex(ground_cursor,0.2);
			
    	}
		
		//-------------------------------------UNLOAD--------------------------------------------
    	protected function world_Unload (event :Event) :void
    	{
    	}
		
		//-------------------------------------UPDATE HUD----------------------------------------
		private function hud_enterFrame(e:Event){
			hud.fps_output.text = "FPS: "+stage.frameRate;
			
			var pos:Point = new Point(local_player.x, local_player.y);
			pos = localToGlobal(pos);
			var cam_x:Number = root.camera.x;
			var cam_goal: Number = (pos.x-stage.stageWidth/2)*-1;
			
			cam_x = (cam_x-cam_goal)*0.1;

			root.camera.x -= cam_x;
			
			//Edge of World Checks
			var bg1_w:Number = bg.bg_1.width-stage.stageWidth;
			if (root.camera.x < bg1_w*(-1)){root.camera.x = bg1_w*(-1);}
			if (root.camera.x > 0){root.camera.x = 0;}
			
			update_bg();
			update_cursor();
			
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
			
		}
		
		//-------------------------------------UPDATE CURSOR-------------------------------------
		private function update_cursor() :void{
			var mpos: Point = new Point( root.mouseX,root.mouseY);
			if (ground.hitTestPoint(mpos.x,mpos.y)){
				Mouse.hide();
				if (ground_cursor_hide){
					ground_cursor_hide = false;
					ground_cursor.gotoAndPlay("on");
				}
				mpos = bg.globalToLocal(mpos);
				ground_cursor.x = mpos.x;
				ground_cursor.y = mpos.y;
				player_scale(ground_cursor);
			}else{
				Mouse.show();
				if (ground_cursor_hide == false){
					ground_cursor_hide = true;
					ground_cursor.gotoAndPlay("off");
				}
			}
		}
		
		//-------------------------------------NEW PLAYER----------------------------------------
		protected function create_player(n: String, t: String, sX: Number, sY: Number) :MovieClip
    	{
			var mc: player = new player();
			mc.pName = n;
			mc.name_plate.text = mc.pName;
			mc.flag = t;
			
			if (mc.flag == "PC"){
				mc.name_plate.textColor = 0x99BFFF;
				pc_list[pc_list.length] = mc;
				actor_list[actor_list.length] = mc;
			} else if (mc.flag == "NPC"){
				mc.name_plate.textColor = 0xCC0000;
				actor_list[actor_list.length] = mc;
			}
						
			trace(mc.flag+" entity '"+mc.pName+"' created! ^__^");
			
			mc.y = sY;
			mc.x = sX;
			
			mc.goal_x = mc.x;
			mc.goal_y = mc.y;
			mc.start_x = mc.x;
			mc.start_y = mc.y;
			mc.move_time = 0;
			mc.move_distance = 0;
			mc.speed = 5;
			mc.addEventListener("enterFrame", player_enterFrame);
			player_scale(mc);
			
			bg.actors.addChild(mc);
			
			return mc
    	}
		
		//-------------------------------------UPDATE PLAYER------------------------------------
		private function player_enterFrame(e:Event){
			
			var lp_current: Point = new Point(e.target.x,e.target.y);
			
			var lp_start: Point = new Point(e.target.start_x,e.target.start_y);
			var lp_goal: Point = new Point(e.target.goal_x,e.target.goal_y);
			var lp_distance: Number = Point.distance(lp_current,lp_goal);
			
			if (e.target.moving){
				if (lp_distance > 5){
					e.target.move_time = e.target.move_time+e.target.speed*e.target.scaleX;
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
			
			if (e.target.flag == "NPC"){
				player_ai(e.target);
			}
			
			player_scale(e.target);
			player_depth(e.target);
		}
		
		//-------------------------------------NPlayer AI---------------------------------------
		private function player_ai(mc:MovieClip){
			if (mc.moving){
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
			var depth: Number = ((e.scaleX-0.5)/0.5)*actor_list.length;
			if (depth > actor_list.length-0.01){
				depth = actor_list.length-0.01;
			}
			//trace(e.pName, depth);
			e.parent.setChildIndex(e,depth);
		}
		
		//-------------------------------------NAVI----------------------------------------------
		private function floorCLICK_handler(e:MouseEvent){
			var pos:Point = new Point(e.stageX, e.stageY);
			pos = bg.globalToLocal(pos);
			
			goal.x = pos.x;
			goal.y = pos.y;
			player_scale(goal);
			goal.gotoAndPlay("on");
			
			plot_goal(local_player, pos.x, pos.y);
			
			//local_player.start_x = local_player.x;
			//local_player.start_y = local_player.y;
			//local_player.goal_x = pos.x;
			//local_player.goal_y = pos.y;
			
			//var lp_start: Point = new Point(local_player.start_x,local_player.start_y);
			//var lp_goal: Point = new Point(local_player.goal_x,local_player.goal_y);
			
			//local_player.move_distance = Point.distance(lp_start,lp_goal);
			//local_player.move_time = 0;
			
			//local_player.moving = true;
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
		}
	}
}