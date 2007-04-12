package{
	import flash.display.*;
	import flash.events.*;
	import flash.geom.*;
	import flash.ui.Mouse;
	import 	flash.utils.Timer;
	
	import com.threerings.ezgame.*; //-W-
	import com.whirled.WhirledGameControl; //-W-
	import com.threerings.util.Random;
	
	[SWF(width="800", height="505")]
	public class world extends MovieClip
		implements MessageReceivedListener, PropertyChangedListener
	{
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
		
		//public var pc :Array;
		public var pc_count:Number = 0;
		public var npc_count:Number = 0;
		
		public var npc_killcount:Number = 0;
		
		public var darkness:Number = 0.0;
		
		//FPS Calculator Variables
		public var timer: Timer = new Timer(1000);
		public var current_fps: Number;
		
		public var poke_timer: Timer = new Timer(2000);
		
		public var time_speed:Number = 1.0;
		public var fps:Number = 20;
		
		//Keyboard inputs
		public var punch_hit: Boolean;
		public var kick_hit: Boolean;
		public var block_hit: Boolean;
		public var sprint_hit: Boolean;
		
		public var punch_code: 		int = 51; 	//[3]
		public var punch_code_alt: 	int = "d".charCodeAt(0);
		
		public var kick_code: 		int = 50;	//[2]
		public var kick_code_alt: 	int = "a".charCodeAt(0);
		
		public var block_code: 		int = 52;	//[4]
		public var block_code_alt: 	int = "s".charCodeAt(0);
		
		public var sprint_code: 	int = 49;	//[1]
		public var sprint_code_alt: int = "w".charCodeAt(0);
		
		public var world_width: Number = 800;
		
		//Stats
		public var local_exp: Number = 0;
		public var local_level: Number = 1;
		public var local_energy: Number = 100;
		public var local_limiter: Boolean = false;
		public var local_score: Number = 0;
		
		public var world_clock: Timer = new Timer(100);
		public var clock: Number = 0;
		public var zone_delay: Number = 0;
		public var gotonextroom: Boolean = false;
		public var npc_pos_clock: Number = 0;
		
		public var health_tick: Number = 0;
		
		
		
		/** Game control. */
    	protected var _control :WhirledGameControl; //-W-
		public var playerIds :Array;
		
		public function world(){
			_control = new WhirledGameControl(this); //-W-
        	_control.registerListener(this); //-W-
			
			//stage.frameRate = fps*time_speed
			//stage.scaleMode = flash.display.StageScaleMode.EXACT_FIT;
			_control.addEventListener(KeyboardEvent.KEY_UP, keyReleased);
			_control.addEventListener(KeyboardEvent.KEY_DOWN, keyPressed);
        	root.loaderInfo.addEventListener(Event.UNLOAD, world_Unload);
			world_Load ();
		}
		
		//-------------------------------------TIMER---------------------------------------------
		private function onTimerEvent( e: Event):void{
			hud.fps_output.text = "FPS: "+current_fps;//+" : "+stage.frameRate;
			current_fps=0;
			
			//************************************
			//                -W-
				if (_control.isConnected() && _control.amInControl()) {
					var msg :Object = new Object;
					msg[0] = room_num;
					msg[1] = _control.getMyId();
					msg[2] = clock
					_control.sendMessage (CLOCK_UPDATE, msg);
				}
			//                -W-
			//************************************
		}
		
		//-------------------------------------TIMER---------------------------------------------
		private function world_tick( e: Event):void{
			clock += 100;
			
			if (local_player.energy < 100 && local_player.sprinting == false){
				if (local_limiter){
					local_player.energy += 2.5;
				}else{
					local_player.energy += 5;
				}
			}
			
			if (local_player && clock >= health_tick){
				health_tick = clock+3000;
				if (local_player.hp < local_player.maxhp){
					local_player.hp +=  local_player.maxhp/20;
				}
				if (local_player.hp > local_player.maxhp){
					local_player.hp = local_player.maxhp;
				}
			}
			
			if (zone_delay == 0){
				
			}else if(zone_delay <= clock){
				zone_delay = 0;
				next_zone();
			}
			
			//************************************
			//                -W-
				if (_control.isConnected() && _control.amInControl()) {
					if (npc_pos_clock+3000 <= clock){
						npc_pos_clock = clock;
						//report_npcs();
					}
				}
			//                -W-
			//************************************
		}
		
		//-------------------------------------LOAD----------------------------------------------
    	protected function world_Load () :void
		{
			timer.addEventListener( TimerEvent.TIMER, onTimerEvent);
			timer.start();
			current_fps = 0;
			
			world_clock.addEventListener( TimerEvent.TIMER, world_tick);
			world_clock.start();
			
			poke_timer.addEventListener( TimerEvent.TIMER, player_poke);
			poke_timer.start();
			
			bg = root.camera._zoom.bg;
			ground = root.camera._zoom.bg.ground;
			ground.doubleClickEnabled = true;
			ground.addEventListener(MouseEvent.CLICK, floorCLICK_handler);
			
			
			goal = new destination();
			bg.cursor_zone.addChild(goal);
			
			ground_cursor = new cursor();
			bg.cursor_zone.addChild(ground_cursor);
			
			if (_control.isConnected()) {
				playerIds = _control.getOccupants();
				var local_name: String = _control.getOccupantName(_control.getMyId());
				local_player = create_player(local_name, "PC", 0,0, 100, 12, _control.getMyId());
			}else{
				local_player = create_player("OFFLINE", "PC", 0,0, 100, 12, 0);
			}
			
			if(local_player.pName == "Jessica"){
				local_exp = 300;
			}
			
			load_npcs();
			
			bg.door_next.x = ground.width;
			bg.door_next.height = ground.height;
			
			this.hud.addEventListener("enterFrame", hud_enterFrame);
			bg.fader.addEventListener("enterFrame", fader_enterFrame);
    	}
		
		//-------------------------------------UNLOAD--------------------------------------------
    	protected function world_Unload (event :Event) :void
    	{
			//Despawn Monsters, Props, and Players.
			//************************************
			//                -W-
				if (_control.isConnected()) {
					var msg :Object = new Object;
					msg[0] = room_num;
					msg[1] = _control.getMyId();
					_control.sendMessage (p_quit, msg);
					msg[0] = room_num;
					msg[1] = _control.getMyId();
					msg[2] = String(local_player.pName+" left the game.");
					_control.sendMessage (p_msg, msg);
				}
			//                -W-
			//************************************
			_control.unregisterListener(this); //-W-
    	}
		
		//-------------------------------------UPDATE FADER--------------------------------------
		private function fader_enterFrame(e:Event){
			var ca:Number = e.target.alpha*100;
			var ga:Number = (darkness*100-ca)*0.25;
			if (ga < 0){
				ga = ga*-1;
			}
			e.target.alpha = ga/100;
		}
	
		
		//-------------------------------------ENTER ZONE----------------------------------------
		private function next_zone():void{
			player_move(local_player, 100,(ground.y-ground.height/2));
			//************************************
			//                -W-
				if (_control.isConnected()) {
					var msg :Object = new Object;
					msg[0] = room_num;
					msg[1] = _control.getMyId();
					msg[2] = 100; //X
					msg[3] = (ground.y-ground.height/2); //Y
					msg[4] = local_player.sprinting;
					msg[5] = local_player.hp;
					msg[6] = local_player.energy;
					_control.sendMessage (p_move, msg);
			}
			//                -W-
			//************************************
			room_num += 1;
			
			bg.bg_1.gotoAndStop(room_num);
			bg.bg_2.gotoAndStop(room_num);
			bg.bg_3.gotoAndStop(room_num);
			bg.bg_4.gotoAndStop(room_num);
			bg.bg_5.gotoAndStop(room_num);
			ground.gotoAndStop(room_num);
			
			bg.door_next.x = ground.width;
			bg.door_next.height = ground.height;

			load_npcs();
			
			ground = root.camera._zoom.bg.ground;
			ground.doubleClickEnabled = true;
			ground.addEventListener(MouseEvent.CLICK, floorCLICK_handler);

			hud.fader.gotoAndPlay("in");
			enable_ai();
			enable_mouse();
			enable_keys();
		}
		
		//-------------------------------------EXIT ZONE-----------------------------------------
		private function exit_zone() :void{
			plot_goal(local_player, ground.width+500, (ground.y-ground.height/2));
			//************************************
			//                -W-
				if (_control.isConnected()) {
					var msg :Object = new Object;
					msg[0] = room_num;
					msg[1] = _control.getMyId();
					msg[2] =  ground.width+500;
					msg[3] = (ground.y-ground.height/2);
					msg[4] = local_player.sprinting;
					msg[5] = local_player.hp;
					msg[6] = local_player.energy;
					_control.sendMessage (p_goal, msg);
			}
			//                -W-
			//************************************
			
			hud.fader.gotoAndPlay("out");
			clear_npcs();
			disable_ai();
			disable_mouse();
			disable_keys();
			
			zone_delay = clock+2000;
			
			//************************************
			// 
			if (_control.isConnected()) {
				var flow :Number = _control.getAvailableFlow()-1;
				_control.awardFlow(flow);
       			_control.localChat("Awarded: " + flow + " flow!");
				var msgg :Object = new Object;
				msgg[0] = room_num;
				msgg[1] = _control.getMyId();
				msgg[2] = String(local_player.pName+" advanced to zone "+(room_num+1)+"!");
				_control.sendMessage (p_msg, msgg);
			}
			//
			//************************************ 
		}
		
		//-------------------------------------KEY UP--------------------------------------------
		private function keyReleased(evt:KeyboardEvent):void{
			if (keys_active){
				if (evt.keyCode == punch_code || evt.charCode == punch_code_alt){
					punch_hit = false;
				}
				if (evt.keyCode == kick_code || evt.charCode == kick_code_alt){
					kick_hit = false;
				}
				if (evt.keyCode == block_code || evt.charCode == block_code_alt){
					player_block_stop(local_player);
				}
				if (evt.keyCode == sprint_code || evt.charCode == sprint_code_alt){
					sprint_hit = false;
				}
			}
		}
		
		//-------------------------------------KEY DOWN-----------------------------------------
		private function keyPressed(evt:KeyboardEvent):void{
			if (keys_active){
				if (evt.keyCode == punch_code || evt.charCode == punch_code_alt){
					if (punch_hit == false){
							punch_hit = true;
							player_punch(local_player);
					}
				}
				if (evt.keyCode == kick_code || evt.charCode == kick_code_alt){
					if (kick_hit == false){
							kick_hit = true;
							player_kick(local_player);
					}
				}
				if (evt.keyCode == block_code || evt.charCode == block_code_alt){
					player_block(local_player);
				}
				if (evt.keyCode == sprint_code || evt.charCode == sprint_code_alt){
					if (sprint_hit == false){
							sprint_hit = true;
					}
				}
			}
		}
		
		//-------------------------------------UPDATE HUD----------------------------------------
		private function hud_enterFrame(e:Event){
			//---Keyboard Listeners

			//---------------------
			current_fps++;
			
			
			//---STAT BAR---
			hud.stats.hpnum.text = Math.round(local_player.hp);
			var hpper :Number = Math.round(local_player.hp/local_player.maxhp*100)+1;
			if (hud.stats.hp.bar.currentFrame >= hpper){
				hud.stats.hp.bar.gotoAndStop(hpper);
			}else if(hud.stats.hp.bar.currentFrame < hpper){
				hud.stats.hp.bar.nextFrame();
			}
			if (hpper < hud.stats.hp.dmg.currentFrame){
				hud.stats.hp.dmg.prevFrame();
				hud.stats.hp.gotoAndStop(1);
				//hud.stats.hp.dmg.stop();
			}else{
				hud.stats.hp.dmg.gotoAndStop(hpper);
				hud.stats.hp.gotoAndStop(2);
			}
			
			if(hpper <= 20){
				hud.hp_warning.gotoAndStop("on");
			}else{
				hud.hp_warning.gotoAndStop("off");
			}
			
			if (local_exp > 300){
				local_exp = 300;
			}else if (local_exp < 0){
				local_exp = 0;
			}
			
			if (local_player.hp <= 0){
				local_exp = 0;
				local_player.energy = 0;
			}
			
			if (hud.stats.exp.currentFrame < Math.round(local_exp)+1){
				hud.stats.exp.nextFrame();
			} else if (hud.stats.exp.currentFrame > Math.round(local_exp)+1){
				hud.stats.exp.prevFrame();
			}
			//hud.stats.exp.gotoAndStop(Math.round(local_exp)+1);
			
			if (local_exp < 100){
				local_level = 1;
			}else if (local_exp < 200){
				local_level = 2;
			}else if (local_exp < 300){
				local_level = 3;
			}else{
				local_level = 4;
			}
			
			hud.score.text = Math.round(local_score);
			
			if (local_player.energy > 100){
				local_player.energy = 100;
			}
			if (local_player.energy < 0){
				local_player.energy = 0;
			}
			
			if (local_limiter){
				hud.energy_warning.gotoAndStop("on");
				if(local_player.energy >= 99){
					local_limiter = false;   
				}
			}else{
				hud.energy_warning.gotoAndStop("off");
				if(local_player.energy <= 0){
					local_limiter = true;   
				}
			}
			
			if (local_player.hp <= 0){
				hud.hp_warning.gotoAndStop("off");
				hud.energy_warning.gotoAndStop("off");
			}
			
			var engper :int = Math.round(local_player.energy/100*100)+1;
			
			if (local_limiter){
				var neg_engper :int = engper+101;
				hud.stats.energy.gotoAndStop(neg_engper);
			}else{
				hud.stats.energy.gotoAndStop(engper);
			}
			hud.stats.energy.num.text = String(engper-1)+"%";
			//--------------
			
			
			//Check For the player to enter the next zone.
			if (bg.door_next.hitTestObject(local_player.boundbox) && npc_killcount == npc_count){
				bg.door_next.height = 0.01;
				//************************************
				// 
				if (_control.isConnected()) {
					var msg :Object = new Object;
					msg[0] = room_num;
					msg[1] = _control.getMyId();
					_control.sendMessage (GTNR, msg);
				}
				//
				//************************************ 
			}
			if (gotonextroom){
				gotonextroom = false;
				exit_zone();
			}
			
			if (npc_killcount == npc_count){
				hud.go.alpha = 1;
			}else{
				hud.go.alpha = 0;
			}
			
			var pos:Point = new Point(local_player.x, local_player.y);
			pos = localToGlobal(pos);
			var cam_x:Number = root.camera.x;
			var cam_goal: Number = (pos.x-world_width/2)*-1;
			
			cam_x = (cam_x-cam_goal)*0.1;

			root.camera.x -= cam_x;
			
			//Edge of World Checks
			var bg1_w:Number = bg.bg_1.width-world_width;
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
			
			var bg1_w:Number = bg1.width-world_width;
			var bg2_w:Number = bg2.width-world_width;
			var bg3_w:Number = bg3.width-world_width;
			var bg4_w:Number = bg4.width-world_width;
			var bg5_w:Number = bg5.width-world_width;
			
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
		protected function create_player(n: String ="Unknown", t: String ="NPC", sX: Number =0, sY: Number =0,
										 hp: Number =100, spd: Number=1, id: Number=0) :MovieClip
    	{
			var mc: player = new player();
			mc.pName = n;
			mc.flag = t;
			mc.id = id;
			
			if (mc.flag == "PC"){
				mc.name_plate.name_plate.text = mc.pName;
				mc.name = id;
				pc_count += 1;
				mc.lastupdate = clock;
			} else if (mc.flag == "NPC"){
				mc.name_plate.name_plate.text = "";
				mc.name_plate.name_plate.textColor = 0xCC0000;
				mc.name = "npc_"+npc_count;
				mc.id = npc_count;
				npc_count += 1;
			}
			
			if (sX == 0&& sY == 0){
				sX = 100;
				sY = (ground.y-ground.height/2);
			}
			
			mc.spawn_x = sX;
			mc.spawn_y = sY;
			
			mc.ai_mode = "idle";
			mc.ai_chase = 900;
			mc.ai_range = 900;
			mc.ai_attrng = 100;
			mc.ai_cooldown = 2500;
			mc.ai_tick = clock+mc.ai_cooldown;
			mc.ai_report = clock;
			
			mc.spd = spd;
			if (mc.spd < 0){
				mc.spd = 0;
			}
			mc.hp = hp;
			mc.maxhp = hp;
			
			mc.energy = 100;
			
			mc.y = sY;
			mc.x = sX;
			
			mc.old_x = sX;
			mc.old_y = sY;
			mc.new_x = sX;
			mc.new_y = sY;
			
			mc.goal_x = mc.x;
			mc.goal_y = mc.y;
			mc.start_x = mc.x;
			mc.start_y = mc.y;
			mc.move_time = 0;
			mc.move_distance = 0;
			
			mc.addEventListener("enterFrame", player_enterFrame);
			player_scale(mc);
			
			if (mc.pName == "Jessica" || mc.pName == "Cherub"){
				//mc.hp = 3200;
				//mc.maxhp = 3200;
				mc.removeChild(mc.character);
				//mc.character = null;
				mc.character = new kosmos();
				mc.addChild(mc.character);
			}
			
			bg.actors.addChild(mc);
			
			return mc
    	}
		
		//-------------------------------------UPDATE PLAYER------------------------------------
		private function player_enterFrame(e:Event){
			if (_control.isConnected() && _control.amInControl()){
				if (e.target.lastupdate+10000 < clock){
					var msgg :Object = new Object;
					msgg[0] = room_num;
					msgg[1] = e.target.name;
					_control.sendMessage (p_quit, msgg);
				}
			}
			
			if (e.target.hp <= 0){
				e.target.effects.gotoAndStop("normal");
			}
			
			if (e.target.flag == "NPC"){
				if (_control.isConnected()){
					var element :String = "r"+String(room_num) + "_m"+String(e.target.id);
					var table :Number = _control.get(element);
					if (table == 0){
						table = e.target.maxhp;
					}
					e.target.hp = Number(table);
					
					//e.target.name_plate.name_plate.text = "HP: "+Math.round(e.target.hp);
				}
			}
			
			var msg :Object = new Object;
			if (e.target.hp > 0){
				//---SLIDE CODE--- MUST BE AT START ---
				e.target.new_x = e.target.x;
				e.target.new_y = e.target.y;
				e.target.new_z = e.target.z;
				//-----------------------------------
				
				//----------------------
				//---Moving to a  new location
				var lp_current: Point = new Point(e.target.x,e.target.y);
				var lp_start: Point = new Point(e.target.start_x,e.target.start_y);
				var lp_goal: Point = new Point(e.target.goal_x,e.target.goal_y);
				var lp_distance: Number = Point.distance(lp_current,lp_goal);
				
				if (e.target.moving){
					if (e.target.sprinting && e.target.energy > 0){
						if (lp_distance > e.target.spd*2){
							e.target.energy -= 5;
							e.target.move_time = e.target.move_time+(e.target.spd*2)*e.target.scaleX;
							lp_current = Point.interpolate(lp_goal,lp_start,e.target.move_time/e.target.move_distance);
						} else {
							e.target.moving = false;
							e.target.sprinting = false;
							e.target.sliding = true;
						}
					}else{
						e.target.sprinting = false;
						if (lp_distance > e.target.spd){
							e.target.move_time = e.target.move_time+e.target.spd*e.target.scaleX;
							lp_current = Point.interpolate(lp_goal,lp_start,e.target.move_time/e.target.move_distance);
						} else {
							e.target.moving = false;
						}
					}
					e.target.x = lp_current.x;
					e.target.y = lp_current.y;
				} else {
					e.target.move_time = 0;
					e.target.move_distance = 0;
					e.target.start_x = e.target.goal_x;
					e.target.start_y = e.target.goal_y;
					if (e.target.sliding){
						e.target.goal_x = e.target.x;
						e.target.goal_y = e.target.y;
					}else{
						e.target.x = e.target.goal_x;
						e.target.y = e.target.goal_y;
					}
					e.target.sprinting = false;
				}
				//----------------------
				
				
				//---STUN!---
				if (e.target.stun){
					if (e.target.stun_counter <= clock){
						e.target.stun = false;
					}
					e.target.effects.gotoAndStop("stunned");
				}else{
					if (e.target.animation == "stun"){
						e.target.animation = "hurt";
					}
					e.target.effects.gotoAndStop("normal");
				}
				//-----------
				
				//---SLIDE!---
				if (e.target.sliding){
					if (ground.hitTestObject(e.target.dot)){
					}else{
						e.target.sliding = false;
					}
				}
				
				if (e.target.sliding){
					var xslide = (e.target.new_x-e.target.old_x)*0.90;
					var yslide = (e.target.new_y-e.target.old_y)*0.90;
					if (e.target.moving == false){
						if ((xslide > 0.5 || xslide < -0.5) || (yslide > 0.5 || yslide < -0.5)){
							e.target.x += xslide;
							e.target.y += yslide;
							if(e.target.knockback != true && xslide > e.target.scaleX && e.target.dustcount <= clock){
								e.target.dustcount = clock+250;
								var tempdust :MovieClip = new slide_dust();
								bg.addChild(tempdust);
								tempdust.x = e.target.x;
								tempdust.y = e.target.y;
								player_scale(tempdust);
							}
						}else{
							e.target.sliding = false;
						}
					}else{
						e.target.sliding = false;
					}
				}else if(e.target.sliding == false && e.target.sprinting == false){
					e.target.old_x = e.target.x;
					e.target.old_y = e.target.y;
					e.target.new_x = e.target.x;
					e.target.new_y = e.target.y;
				}
				
				if (e.target.sliding == false){
					e.target.knockback = false;
					if (e.target.animation == "knockback"){
						e.target.animation = "hurt";
					}
				}
				//------------
				
				
				//----------------------
				//---NPC AI Detection
				if (e.target.flag == "NPC" && ai_active){
					player_ai(e.target);
				}
				//----------------------
				
				
				player_scale(e.target);
				player_depth(e.target);
				
				
				//----------------------
				//---Name Plate Scaling
				var nScale:Number = 1+(1-((e.target.scaleX-0.5)/0.5));
				e.target.name_plate.name_plate.scaleX = nScale;
				e.target.name_plate.name_plate.scaleY = nScale;
				e.target.name_plate.name_plate.x = -1*e.target.name_plate.width/2;
				//----------------------
				
				
				//----------------------
				//---Direction Detection
				if (e.target.moving && e.target.flag != "NPC"){
					if (e.target.x > e.target.last_x){
						e.target.character.scaleX = 1; //Face Right
						e.target.dmgbox.scaleX = 1;
					}else{
						e.target.character.scaleX = -1; //face Left
						e.target.dmgbox.scaleX = -1;
					}
				}else{
					if(e.target.ai_target != "" && bg.actors.getChildByName(e.target.ai_target)){
						var temptarget :MovieClip = bg.actors.getChildByName(e.target.ai_target);
						if (e.target.x < temptarget.x){
							e.target.character.scaleX = 1; //Face Right
							e.target.dmgbox.scaleX = 1;
						}else{
							e.target.character.scaleX = -1; //face Left
							e.target.dmgbox.scaleX = -1;
						}
					}else if(e.target.moving){
						if (e.target.x > e.target.last_x){
							e.target.character.scaleX = 1; //Face Right
							e.target.dmgbox.scaleX = 1;
						}else{
							e.target.character.scaleX = -1; //face Left
							e.target.dmgbox.scaleX = -1;
						}
					}
				}
				e.target.last_x = e.target.x;
				
				if (e.target == local_player){
					if (local_player.moving == false && local_player.hp > 0 && local_player.animation == "idle"){
						if (local_player.x < ground_cursor.x){
							local_player.character.scaleX = 1; //Face Right
							local_player.dmgbox.scaleX = 1;
						}else{
							local_player.character.scaleX = -1; //face Left
							local_player.dmgbox.scaleX = -1;
						}
					}
				}
				//----------------------
				
				
				//----------------------
				//---Animation Detection
				if (e.target.knockback){
					e.target.animation = "knockback";
				}else if(e.target.stun){
					e.target.animation = "stun";
				}
				
				if (e.target.character.currentFrame < 10 && e.target.animation_old != "idle"){
					e.target.animation = "idle";
					e.target.animation_old = "idle";
				}
				
				if (e.target.animation == "hurt"){
				}else if (e.target.animation == "knockback"){
				}else if (e.target.animation == "stun"){
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
					
					if (e.target == local_player){
						if (e.target.animation == "punch"){
							e.target.dmgbox.gotoAndPlay(e.target.animation);
						}else if (e.target.animation == "kick"){
							e.target.dmgbox.gotoAndPlay(e.target.animation);
						}
					}else if(e.target.flag != "PC"){
						if (e.target.animation == "punch"){
							e.target.dmgbox.gotoAndPlay(e.target.animation);
						}else if (e.target.animation == "kick"){
							e.target.dmgbox.gotoAndPlay(e.target.animation);
						}
					}
				}
				
				e.target.animation_old = e.target.animation;
				//e.target.name_plate.text = e.target.hp;
				//----------------------
				
				
				//----------------------
				//---DAMAGE!------------
				var dmg :Number = 0;
				var slidebonus :Number;
				var knock_amount :Number;
				var stun_amount :Number;
				var t:Number;
				var n:Number;
				var mov:MovieClip;
				
				
				//----FLYING BACKWARDS COLLISSION---
				if (e.target.knockback && e.target.ai_target == local_player.name){
						n = 0;
						dmg = 0;
						if (npc_count){
							t = npc_count;
							while(n <= t){
								if (bg.actors.getChildByName("npc_"+n) && bg.actors.getChildByName("npc_"+n) != e.target && xslide > 1){
									mov = bg.actors.getChildByName("npc_"+n);
									if (mov.knockback == false){
										if (mov.hp > 0){
											if (e.target.boundbox.hitTestObject(mov.boundbox)){
												if (mov.animation != "hurt"){
													dmg = (e.target.maxhp/4);
													player_hurt(bg.actors.getChildByName("npc_"+n),dmg, String(e.target.ai_target), xslide, 0);
													//************************************
													//                -W-
													if (_control.isConnected()) {
														msg[0] = room_num;
														msg[1] = _control.getMyId();
														msg[2] = n;
														msg[3] = dmg;
														msg[4] = xslide;
														msg[5] = 0;
														_control.sendMessage (p_hurt, msg);
													}
													//                -W-
													//************************************
												}
											}
										}
									}
								}
								n++;
							}
						}
				}
				//----------------------------------
				
				
				if (e.target.animation == "punch" || e.target.animation == "kick"){
					if (e.target.flag == "PC" && e.target == local_player){
						n = 0;
						dmg = 0;
						if (npc_count){
							t = npc_count;
							while(n <= t){
								if (bg.actors.getChildByName("npc_"+n)){
									mov = bg.actors.getChildByName("npc_"+n);
									if (mov.hp > 0){
										if (e.target.dmgbox.hitTestObject(mov.boundbox)){
											//If it hits dood, hurt dood.
											if (mov.animation != "hurt"){
												dmg = ((Math.random()*50)+10)*(local_level*0.5);
												if (e.target.animation == "kick"){
													knock_amount = 15;
												}
												if (e.target.animation == "punch"){
													//stun_amount = 1.5;
													knock_amount = 0;
												}
												if (e.target.sliding == true){
													knock_amount += xslide*2;
												}
												local_exp += (dmg/5)/(local_level*0.5);
												local_score += dmg*local_level;
												player_hurt(bg.actors.getChildByName("npc_"+n),dmg, String(e.target.name), knock_amount, stun_amount);
												if (e.target == local_player){
													//************************************
													//                -W-
													if (_control.isConnected()) {
														msg[0] = room_num;
														msg[1] = _control.getMyId();
														msg[2] = n;
														msg[3] = dmg;
														msg[4] = knock_amount;
														msg[5] = stun_amount;
														_control.sendMessage (p_hurt, msg);
													}
													//                -W-
													//************************************
												}
											}
										}
									}
								}
								n++;
							}
						}
					} else if (e.target.flag != "PC"){
						dmg = 0;
							if (local_player.hp > 0){
								if (e.target.dmgbox.hitTestObject(local_player.boundbox)){
									if (local_player.animation != "hurt"){
										dmg = (Math.random()*10)+10;
										player_hurt(local_player, dmg, String(e.target.name), 0, 0);
										//************************************
										//                -W-
										if (_control.isConnected()) {
											msg[0] = room_num;
											msg[1] = e.target.name;
											msg[2] = "X";
											msg[3] = dmg;
											msg[4] = 0;
											msg[5] = 0;
											_control.sendMessage (n_hurt, msg);
										}
										//                -W-
										//************************************
									}
								}
							}
					}
				}
				//----------------------
			} else {
				e.target.animation = "dead";
				if (e.target.animation != e.target.animation_old){
					e.target.character.gotoAndPlay(e.target.animation);
					if(e.target.flag != "PC"){
						npc_killcount += 1;
					}
				}
			
				e.target.animation_old = e.target.animation;
			}
			
			//---SLIDE CODE--- MUST BE AT END ---
			e.target.old_x = e.target.new_x;
			e.target.old_y = e.target.new_y;
			e.target.old_z = e.target.new_z;
			//-----------------------------------
			
		}
		
		//-------------------------------------NPlayer AI---------------------------------------
		private function player_ai(mc:MovieClip){
			var msg :Object = new Object;
			
			if (mc.knockback || mc.stun || mc.animation == "hurt" || mc.animation == "dead"){
			}else{
				if (_control.isConnected()) {
					var pc :Array = _control.getOccupants();
					var temp_dis :Number = 0;
					var mypos :Point = new Point(mc.x,mc.y);
					var glpos :Point = new Point(mc.spawn_x, mc.spawn_y);
					var dist :Number;
					var trpos :Point;
					var player_target :MovieClip;
					
					switch (mc.ai_mode) {
						case "idle":
							//Scan for player distances
							var n :Number = 0;
							var t :Number = pc.length;
							
							while(n<t){
								if (bg.actors.getChildByName(String(pc[n]))){
									player_target = bg.actors.getChildByName(String(pc[n]));
									trpos = new Point(player_target.x,player_target.y);
									temp_dis = Point.distance(mypos,trpos);
									if (temp_dis <= mc.ai_range && player_target.hp > 0){ //Is it within target range
										if (dist){
										}else{
											dist = temp_dis+10;
										}
										if (temp_dis <= dist){ //Target closest in range
											dist = temp_dis;
											mc.ai_target = String(player_target.id);
										}
									}
								}
								n++;
							}
							//Switch to Active if target found
							if (mc.ai_target != ""){
								mc.ai_mode = "active";
							}else{
								
							}
							break;
							
						case "flee":
							dist = Point.distance(mypos,glpos);
							if (dist > mc.spd){
								plot_goal(mc, mc.spawn_x, mc.spawn_y);
							}else{
								mc.ai_mode = "idle";
								mc.ai_target = "";
							}
							break;
						
						case "active":
							//Follow Target
							//if (mc.hp < mc.maxhp*0.1){
								//mc.ai_mode = "flee";
								//mc.ai_target = "";
								//_control.localChat("NPC_"+mc.id+": Low on health; breaking off!");
								//break;
							//}
							
							if (mc.ai_target != ""){
								if (bg.actors.getChildByName(mc.ai_target)){ //If target exsists...
									player_target = bg.actors.getChildByName(mc.ai_target);
										if(player_target == local_player && mc.ai_report+1000 <= clock){
											mc.ai_report = clock;
											//************************************
											//                -W-
											if (_control.isConnected()) {
													msg[0] = room_num;
													msg[1] = _control.getMyId();
													if (mc.moving){
														msg[2] = mc.goal_x;
														msg[3] = mc.goal_y;
													}else{
														msg[2] = mc.x;
														msg[3] = mc.y;
													}
													msg[4] = mc.name;
													msg[5] = mc.ai_target;
													_control.sendMessage (n_goal, msg);
											}
											//                -W-
											//************************************
										}
									if (player_target.hp <= 0){ //Target dead, Breaking off.
										mc.ai_mode = "flee";
										mc.ai_target = "";
									}else{
										trpos = new Point(player_target.x,player_target.y);
										
										temp_dis = Point.distance(mypos,trpos);
										var spawn_dis :Number = Point.distance(mypos,glpos);
										
										//if (spawn_dis > mc.ai_chase ){ //Too far, break off.
											//mc.ai_mode = "flee";
											//mc.ai_target = "";
											//_control.localChat("NPC_"+mc.id+": Target too far away; breaking off.");
										//}else{
											if (mc.ai_tick <= clock){
												//---ATTACK MODE---
												if( mc.x > player_target.x){
														if(player_target == local_player){
															plot_goal(mc, player_target.x+mc.ai_attrng, player_target.y);
														}
												}else{
														if(player_target == local_player){
															plot_goal(mc, player_target.x-mc.ai_attrng, player_target.y);
														}
												}
												if(temp_dis <= mc.ai_attrng+mc.spd && (mc.y <= player_target.y+5 && mc.y >= player_target.y-5)){
													//attack!
													mc.ai_tick = clock+mc.ai_cooldown;
													player_punch(mc);
												}
											}else{
												//---EVASIVE MODE---
												
												if (mc.moving != true && (temp_dis < mc.ai_range || temp_dis > mc.ai_range*1.5 )){
													//var seedrnd1 :Number = (mc.hp/mc.maxhp)-(player_target.hp/player_target.maxhp)*(clock/mc.ai_tick);
													//var seedrnd2 :Number = (mc.hp/mc.maxhp)*(player_target.hp/player_target.maxhp)+(clock/mc.ai_tick);
													var rnd1 :Random = new Random(mc.hp+mc.ai_tick);
													var desx :Number = (rnd1.nextNumber()*mc.ai_range)+(mc.ai_range/4);
													var flipcoin :Boolean = rnd1.nextBoolean();
													if (flipcoin){
														desx = desx*(-1);
													}
													var desy :Number = rnd1.nextNumber()*125;
													if (player_target.scaleX > 0.75){
														desy = desy*(-1);
													}
													
													var des :Point = new Point(player_target.x+desx, player_target.y+desy);
														if(player_target == local_player){
															plot_goal(mc, des.x, des.y);
														}
													
												}
											}
										//}
									}
								}else{ //Can't find target, break off.
									mc.ai_mode = "flee";
									mc.ai_target = "";
									//_control.localChat("NPC_"+mc.id+": Can't find target; breaking off.");
								}
							}else{ //No target, break off.
								mc.ai_mode = "flee";
								mc.ai_target = "";
								//_control.localChat("NPC_"+mc.id+": Target is null; breaking off.");
							}
							break;
					}
				}
				
				if (mc.moving ){
				}else{
					var max_Y: Number = ground.y;
					var min_Y: Number = ground.y-ground.height;
						
					var max_X: Number = ground.width;
					var min_X: Number = 0;
						
					var pX: Number = min_X+(max_X*((Math.random()*100)/100));
					var pY: Number = min_Y+(max_Y*((Math.random()*100)/100));
					
					if (_control.isConnected()){
					}else{
						if (ground.hitTestPoint(pX,pY)){
							plot_goal(mc, pX, pY);
						}
					}
					
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
			var depth: int = ((e.scaleX-0.5)/0.5)*e.parent.numChildren;
			var cur_depth: int = e.parent.getChildIndex(e);
			
			if (depth > e.parent.numChildren-0.01){
				depth = e.parent.numChildren-0.01;
			}
			if (depth < 0){
				depth = 0;
			}
			
			var big_d :int;
			var lil_d :int;
			if (depth > cur_depth){
				big_d = depth;
				lil_d = cur_depth;
			}else{
				big_d = cur_depth;
				lil_d = depth;
			}
			
			var moo :MovieClip = e.parent.getChildAt(depth);
			if (moo){
				if (e.scaleX > moo.scaleX){
					e.parent.setChildIndex(e,big_d);
					e.parent.setChildIndex(moo,lil_d);
				}else{
					e.parent.setChildIndex(moo,big_d);
					e.parent.setChildIndex(e,lil_d);
				}
			}else{
				e.parent.setChildIndex(e,depth);
			}
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
			mc.old_x = pX;
			mc.old_y = pY;
			mc.new_x = pX;
			mc.new_y = pY;
			mc.move_distance = 0;
			mc.move_time = 0;
			mc.moving = false;
			mc.sprinting = false;
			mc.sliding = false;
    	}
		
		//-------------------------------------DELETE PC-----------------------------------------
    	protected function player_delete (id:String) :void
    	{
			if (bg.actors.getChildByName(id)){
				bg.actors.getChildByName(id).removeEventListener("enterFrame", player_enterFrame);
				bg.actors.removeChild(bg.actors.getChildByName(id));
				pc_count -= 1;
			}
    	}
		
		//-------------------------------------DELETE NPCS----------------------------------------
    	protected function clear_npcs () :void
    	{
			var n:Number = 0;
			if (npc_count){
				var t:Number = npc_count;
				while(n <= t){
					if (bg.actors.getChildByName("npc_"+n)){
						bg.actors.getChildByName("npc_"+n).removeEventListener("enterFrame", player_enterFrame);
						bg.actors.removeChild(bg.actors.getChildByName("npc_"+n));
					}
					n++;
				}
				npc_count = 0;
			}
    	}
		
		//-------------------------------------REPORT NPCS----------------------------------------
    	protected function report_npcs () :void
    	{
			var n:Number = 0;
			var mob: MovieClip;
			
			var msg :Object = new Object;
			msg[0] = room_num;
			msg[1] = _control.getMyId();
			
			
			var m:Number = 3;
			
			if (npc_count){
				var t:Number = npc_count;
				while(n <= t){
					if (bg.actors.getChildByName("npc_"+n)){
						mob = bg.actors.getChildByName("npc_"+n);
						msg[m] = "npc_"+n;
						msg[m+1] = mob.x;
						msg[m+2] = mob.y;
						msg[m+3] = mob.ai_mode;
						msg[m+4] = mob.ai_target;
						msg[m+5] = mob.stun;
						msg[m+6] = mob.knockback;
						msg[m+7] = mob.stun_counter;
						msg[m+8] = mob.ai_tick;
						msg[m+9] = mob.new_x;
						msg[m+10] = mob.new_y;
						msg[m+11] = mob.old_x;
						msg[m+12] = mob.old_y;
						msg[m+13] = mob.sliding;
						
						msg[m+14] = mob.move_start;
						msg[m+15] = mob.move_time;
						msg[m+16] = mob.move_distance;
						msg[m+17] = mob.goal_x;
						msg[m+18] = mob.goal_y;
						msg[m+19] = mob.start_x;
						msg[m+20] = mob.start_y;
						
						m = m+21;
						
					}
					n++;
				}
			}
			
			//msg[2] = n-1;
			_control.sendMessage (REPORT, msg);
    	}
		
		//-------------------------------------LOAD NPCS----------------------------------------
		private function load_npcs() :void{
			var mobs :MovieClip = new all_mobs();
			//mobs.gotoAndStop(room_num);
			
			var n:Number = 0;
			var t:Number = mobs.numChildren;
			var moo: MovieClip;
			
			var element :String;
			var table :Number;
			var olddmgg :Number;
			
			//_control.localChat("Spawning "+(t)+" Mobs.");
			while(n < t){
					//moo = mobs.getChildByName("mob_"+n);
					moo = mobs.getChildAt(n);
					if (moo && moo.name == "m"+String(room_num)){
						//_control.localChat("Spawning Mob "+n+".");
						newmob = create_player(moo.mt.text, "NPC", moo.x, moo.y, moo.hp.text, moo.spd.text, n);
						newmob = null;
						moo = null;
					}else{
						//_control.localChat("No Child at "+n+"!");
					}
					n++;
			}
			npc_killcount = 0;
		}
		
		//-------------------------------------POKE!---------------------------------------------
		private function player_poke( e: Event):void{
			//************************************
			//                -W-
			if (_control.isConnected()) {
				var msg :Object = new Object;
				msg[0] = room_num;
        		msg[1] = _control.getMyId();
				if (local_player.moving){
					msg[2] = goal.x;
					msg[3] = goal.y;
				}else{
        			msg[2] = local_player.x;
					msg[3] = local_player.y;
				}
				msg[4] = local_player.sprinting;
				msg[5] = local_player.hp;
				msg[6] = local_player.energy;
        		_control.sendMessage (p_goal, msg);
			}
			//                -W-
			//************************************
		}
		
		//-------------------------------------NAVI----------------------------------------------
		private function floorCLICK_handler(e:MouseEvent){
			var pos:Point = new Point(e.stageX, e.stageY);
			pos = bg.globalToLocal(pos);
			
			if (mouse_active){
				if (local_player.sprinting == false){
					goal.x = pos.x;
					goal.y = pos.y;
					player_scale(goal);
					goal.gotoAndPlay("on");
					goal_hide = false;
					
					if (sprint_hit == true && local_limiter != true){
						local_player.sprinting = true;
					}
					plot_goal(local_player, pos.x, pos.y);
					
					
					//************************************
					//                -W-
					if (_control.isConnected()) {
						var msg :Object = new Object;
						msg[0] = room_num;
						msg[1] = _control.getMyId();
						msg[2] = pos.x;
						msg[3] = pos.y;
						msg[4] = local_player.sprinting;
						msg[5] = local_player.hp;
						msg[6] = local_player.energy;
						_control.sendMessage (p_goal, msg);
					}
					//                -W-
					//************************************
				}
			}
		}
		
		//-------------------------------------PLOT GOAL-----------------------------------------
		private function plot_goal(mc:MovieClip, pX:Number, pY:Number){
			if ((mc.animation == "idle" || mc.animation == "walk")){ //&& mc.sliding == false){
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
		}
		
		
		
		
		
		
		
		public var mouse_active :Boolean = true;
		public var keys_active :Boolean = true;
		public var ai_active :Boolean = true;
		//----------------------------------------------------------------------------------------
		private function disable_mouse(){
			mouse_active = false;
		}
		private function enable_mouse(){
			mouse_active = true;
		}
		
		private function disable_keys(){
			keys_active = false;
		}
		private function enable_keys(){
			keys_active = true;
		}
		
		private function disable_ai(){
			ai_active = false;
		}
		private function enable_ai(){
			ai_active = true;
		}
		//----------------------------------------------------------------------------------------
		
		
		
		
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
		//-------------------------------------PUNCH!---------------------------------------------
    	protected function player_punch (mc:MovieClip) :void
    	{	
			if (mc == local_player){
				if (local_player.energy >= 25 && local_limiter != true){
						if ((mc.animation == "idle" || mc.animation == "walk") && mc.animation_old != "punch"){
							local_player.energy -= 25;
							if (mc.sprinting == true){
								mc.sliding = true;
								mc.moving = false;
								mc.sprinting =false;
								mc.move_time = 0;
								mc.move_distance = 0;
								mc.start_x = mc.goal_x;
								mc.start_y =mc.goal_y;
								mc.goal_x = mc.x;
								mc.goal_y = mc.y;
							}
							if (mc.sliding == false){
								player_move(mc, mc.x, mc.y);
							}
							mc.animation = "punch";
							health_tick = clock+3000;
							
							//************************************
							//                -W-
							if (_control.isConnected()) {
								var msg :Object = new Object;
								msg[0] = room_num;
								msg[1] = _control.getMyId();
								_control.sendMessage (p_punch, msg);
							}
							//                -W-
							//************************************
						}
				}
			}else{
				if (mc.animation == "idle" || mc.animation == "walk"){
					player_move(mc, mc.x, mc.y);
					mc.animation = "punch";
				}
			}
    	}
		
		//-------------------------------------KICK!----------------------------------------------
    	protected function player_kick (mc:MovieClip) :void
    	{
			if (mc == local_player){
				if (local_player.energy >= 25 && local_limiter != true){
						if ((mc.animation == "idle" || mc.animation == "walk") && mc.animation_old != "kick"){
							local_player.energy -= 25;
							if (mc.sprinting == true){
								mc.sliding = true;
								mc.moving = false;
								mc.sprinting =false;
								mc.move_time = 0;
								mc.move_distance = 0;
								mc.start_x = mc.goal_x;
								mc.start_y =mc.goal_y;
								mc.goal_x = mc.x;
								mc.goal_y = mc.y;
							}
							if (mc.sliding == false){
								player_move(mc, mc.x, mc.y);
							}
							mc.animation = "kick";
							health_tick = clock+3000;
							
							//************************************
							//                -W-
							if (_control.isConnected()) {
								var msg :Object = new Object;
								msg[0] = room_num;
								msg[1] = _control.getMyId();
								_control.sendMessage (p_kick, msg);
							}
							//                -W-
							//************************************
						}
				}
			}else{
				if (mc.animation == "idle" || mc.animation == "walk"){
					player_move(mc, mc.x, mc.y);
					mc.animation = "kick";
				}
			}
    	}
		
		//-------------------------------------BLOCK!---------------------------------------------
    	protected function player_block (mc:MovieClip) :void //START
    	{
			if (mc.animation == "idle" || mc.animation == "walk"){
				if (mc == local_player){
					if (mc.sprinting == true){
						mc.sliding = true;
						mc.moving = false;
						mc.sprinting =false;
						mc.move_time = 0;
						mc.move_distance = 0;
						mc.start_x = mc.goal_x;
						mc.start_y =mc.goal_y;
						mc.goal_x = mc.x;
						mc.goal_y = mc.y;
					}
					if (mc.sliding == false){
						player_move(mc, mc.x, mc.y);
					}
					mc.animation = "block";
					
					//************************************
					//                -W-
					if (_control.isConnected()) {
						var msg :Object = new Object;
						msg[0] = room_num;
						msg[1] = _control.getMyId();
						_control.sendMessage (p_block, msg);
					}
					//                -W-
					//************************************
				}else{
					player_move(mc, mc.x, mc.y);
					mc.animation = "block";
				}
			}
    	}
    	protected function player_block_stop (mc:MovieClip) :void //STOP
    	{
			if (mc.animation == "block"){
				if (mc == local_player){
					mc.animation = "idle";
					
					//************************************
					//                -W-
					if (_control.isConnected()) {
						var msg :Object = new Object;
						msg[0] = room_num;
						msg[1] = _control.getMyId();
						_control.sendMessage (p_block_stop, msg);
					}
					//                -W-
					//************************************
				}else{
					mc.animation = "idle";
				}
			}
    	}
		
		//-------------------------------------HURT PLAYER----------------------------------------
		protected function player_hurt (mc:MovieClip, dmg:Number, attacker_name:String, knockback:Number, stun:Number) :void
    	{	
			var attacker :MovieClip = bg.actors.getChildByName(attacker_name);
			
			var temp_block :MovieClip;
			
			if (mc.animation_old == "block" || mc.animation == "block" ){
				if (mc == local_player){
					if (local_player.energy > 0){
						dmg = dmg/2;
						local_player.energy -= dmg;
						dmg = 0;
						health_tick = clock+3000;
						
						temp_block = new block();
						bg.addChild(temp_block);
						temp_block.x = mc.x+Math.random()*50;
						temp_block.y = mc.y;
						player_scale(temp_block);
						
					}else{
						if(mc == local_player){
								mc.hp -= dmg;
						}
						health_tick = clock+6000;
						mc.animation = "hurt";
						player_move(mc, mc.x, mc.y);
					}
				}
			}else{
				if (_control.isConnected()){
					if(mc == local_player){
						mc.hp -= dmg;
					}else{
						mc.ai_tick = clock+mc.ai_cooldown+2000*((mc.hp/mc.maxhp)-mc.scaleX);
						mc.ai_target = attacker_name;
						mc.ai_mode = "active";
					}
				}else{
					mc.hp -= dmg;
					mc.ai_tick = clock+mc.ai_cooldown+2000*((mc.hp/mc.maxhp)-mc.scaleX);
					mc.ai_target = attacker_name;
					mc.ai_mode = "active";
				}
				mc.animation = "hurt";
				player_move(mc, mc.x, mc.y);
			}
			
			if (mc == local_player){
				local_exp -= dmg;
			}
			
			if (dmg > 0){
				if (stun > 0){
					mc.stun = true;
					mc.stun_counter = clock+(stun*1000)
				}else{
					mc.stun = false;
				}
				
				if (knockback > 0){
					mc.animation = "knockback";
					mc.knockback = true;
						mc.sliding = true;
						mc.moving = false;
						mc.sprinting =false;
						mc.move_time = 0;
						mc.move_distance = 0;
						mc.start_x = mc.goal_x;
						mc.start_y =mc.goal_y;
						mc.goal_x = mc.x;
						mc.goal_y = mc.y;
					if (attacker.x > mc.x){
						mc.old_x = mc.x+knockback;
					}else{
						mc.old_x = mc.x-knockback;
					}
					
				}
			}
			
			if (dmg > 0){
				var temp_num :MovieClip;
				var temp_snap :MovieClip;
				if (dmg > 80){
					root.camera.gotoAndPlay("x_light");
					if (mc.flag == "PC"){
						temp_num = new dmg_crit_num_player();
					}else{
						temp_num = new dmg_crit_num();
					}
				}else{
					if (mc.flag == "PC"){
						temp_num = new dmg_num_player();
					}else{
						temp_num = new dmg_num();
					}
				}
				temp_num.txt.dmg.text = "-"+String(Math.round(dmg));
				bg.addChild(temp_num);
				temp_num.x = mc.x+Math.random()*50;
				temp_num.y = mc.y;
				player_scale(temp_num);
				
				temp_snap = new dmg_snap();
				bg.addChild(temp_snap);
				temp_snap.x = mc.x;
				temp_snap.y = mc.y;
				player_scale(temp_snap);
				temp_snap.x += Math.random()*20;
				temp_snap.y += Math.random()*20;
				var snapscale :Number = 0.10-Math.random()*0.20;
				temp_snap.scaleX += snapscale;
				temp_snap.scaleY += snapscale;
				temp_snap.snap.rotate = Math.random()*360;
				
				
				
			}
			
			
    	}
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
		
		
		
		
		public static const p_goal :String = "New Player Goal";
		public static const n_goal :String = "New NPC Goal";
		public static const p_move :String = "Move Player";
		public static const p_punch :String = "Player Punched";
		public static const p_kick :String = "Player Kicked";
		public static const p_block :String = "Player Blocked";
		public static const p_block_stop :String = "Player Stopped Blocking";
		
		public static const p_hurt :String = "This guy took damage!";
		public static const n_hurt :String = "This guy hit me!";
		
		public static const p_quit :String = "Player quit the game";
		
		public static const p_msg :String = "Player Sent a Message";
		
		public static const GTNR :String = "Go to the next room!";
		
		public static const CLOCK_UPDATE :String = "Hey, update your clock!";
		
		public static const REPORT :String = "Current status of the NPC via the Host";
		//----------------------------------------------------------------------------------------
		//------------------------------------NETWORKING------------------------------------------
		//----------------------------------------------------------------------------------------
		// from MessageReceivedListener 
		public function messageReceived (event :MessageReceivedEvent) :void
		{
			var room :Number = int(event.value[0]);
			var id :int = int(event.value[1]);
			var moo_id: String = id;
			var moo: MovieClip;
			
			//---PUBLIC---
			switch (event.name) {
				case GTNR:
					room_num = int(event.value[0]);
					gotonextroom = true;
					break;
						
				case CLOCK_UPDATE:
					clock = int(event.value[2]);
					break;
						
				case p_msg:
					var chat :String = String(event.value[2]);
					_control.localChat(chat);
					break;
						
				case p_quit:
					player_delete(moo_id);
					break;
						
				case p_hurt:
					var mobb :Number = event.value[2];
					var dmgg :Number = Number(event.value[3]);
					if (_control.isConnected() && _control.amInControl()){
						var element :String = "r"+String(room)+"_m"+mobb;
						var table :Number = _control.get(element);
						if (table == 0) {
							//_control.localChat(element+" does not exsist: Creating...");
							table = bg.actors.getChildByName("npc_"+mobb).maxhp;
						}
						table -= dmgg;
						//_control.localChat("S: "+element+" = "+table);
						_control.set(element, table);
					}
					break;
					
				case n_goal:
						_control.localChat("n_goal "+String(event.value[4]));
						gX = Number(event.value[2]);
						gY = Number(event.value[3]);
						moo = bg.actors.getChildByName(String(event.value[4]));
						if (moo){
							moo.ai_target = String(event.value[5]);
							plot_goal(moo, gX, gY);
						}
						break;
					
				case REPORT:
						var m:Number = 3;
						var n:Number = 0;
						var moob :MovieClip;
							var t:Number = npc_count;
							while(event.value[m]){//(n <= t){
								if (event.value[m]){
									//_control.localChat(event.value[m] +"  "+event.value[m+1]+":"+event.value[m+2]);
								}
								moob = bg.actors.getChildByName(String(event.value[m]));
								if (moob){
									_control.localChat(moob.name+" reported");
									//moob = bg.actors.getChildByName(String(event.value[m]));
									
									//moob.x = 			Number(event.value[m+1]);
									//moob.y = 			Number(event.value[m+2]);
									
									moob.ai_mode =		String(event.value[m+3]);
									moob.ai_target = 	String(event.value[m+4]);
									//moob.stun = 		Boolean(event.value[m+5]);
									//moob.knockback = 	Boolean(event.value[m+6]);
									//moob.stun_counter = Number(event.value[m+7]);
									moob.ai_tick = 		Number(event.value[m+8]);
									//moob.new_x = 		Number(event.value[m+9]);
									//moob.new_y = 		Number(event.value[m+10]);
									//moob.old_x = 		Number(event.value[m+11]);
									//moob.old_y = 		Number(event.value[m+12]);
									//moob.sliding = 		Boolean(event.value[m+13]);
									
									//moob.move_start = 	Number(event.value[m+14]);
									//moob.move_time =	Number(event.value[m+15]);
									//moob.move_distance =Number(event.value[m+16]);
									moob.goal_x =		Number(event.value[m+17]);
									moob.goal_y =		Number(event.value[m+18]);
									//moob.start_x =		Number(event.value[m+19]);
									//moob.start_y =		Number(event.value[m+20]);
									
									moob = null;
									
									//m += 14;
								}
								m = m+21;
								n++;
							}
			}
			
			
			//---LOCAL---
			if (room == room_num && id != _control.getMyId()){
				if (bg.actors.getChildByName(moo_id)){
				}else{
					var local_name: String = _control.getOccupantName(id);
					moo = create_player(local_name, "PC", 0, 0, 100, 15, moo_id);
				}
				
				var gX :Number;
				var gY :Number;
				switch (event.name){
					case p_goal:
						gX = Number(event.value[2]);
						gY = Number(event.value[3]);
						moo = bg.actors.getChildByName(moo_id);
						moo.sprinting = event.value[4];
						moo.hp = event.value[5];
						moo.energy = event.value[6];
						plot_goal(moo, gX, gY);
						break;
							
					case p_move:
						gX = Number(event.value[2]);
						gY = Number(event.value[3]);
						moo = bg.actors.getChildByName(moo_id);
						moo.sprinting = event.value[4];
						moo.hp = event.value[5];
						moo.energy = event.value[6];
						player_move(moo, gX, gY);
						break;
						
					case n_hurt:
						player_hurt (bg.actors.getChildByName(moo_id), Number(event.value[3]), "npc_x", Number(event.value[4]), Number(event.value[5]));
						break;
					}
					break;
							
					case p_punch:
						moo = bg.actors.getChildByName(moo_id);
						player_punch(moo);
						break;
							
					case p_kick:
						moo = bg.actors.getChildByName(moo_id);
						player_kick(moo);
						break;
							
					case p_block:
						moo = bg.actors.getChildByName(moo_id);
						player_block(moo);
						break;
							
					case p_block_stop:
						moo = bg.actors.getChildByName(moo_id);
						player_block_stop(moo);
						break;
						
					case p_hurt:
						var mob :Number = event.value[2];
						var dmg :Number = Number(event.value[3]);
						player_hurt (bg.actors.getChildByName("npc_"+mob), dmg, String(id), Number(event.value[4]), Number(event.value[5]));
						break;
				}
			}else if (room != room_num && id != _control.getMyId()){
				player_delete(moo_id);
			}
			
			
			//---IDLE OUT---
			if (bg.actors.getChildByName(moo_id)){
				bg.actors.getChildByName(moo_id).lastupdate = clock;
			}
			
			
			//---Things that need to happen no matter what!---
			//if (event.name == GTNR){
				//room_num = int(event.value[0]);
				//gotonextroom = true;
			//}else if (event.name == CLOCK_UPDATE){
				//clock = int(event.value[0]);
			//}else if (event.name == p_msg){
				//var chat :String = String(event.value[1]);
				//_control.localChat(chat);
			//}else if (event.name == p_quit){
				//player_delete(moo_id);
			//}else if (event.name == p_hurt){
				//var mobb :Number = event.value[2];
				//var dmgg :Number = Number(event.value[3]);
				//if (_control.isConnected() && _control.amInControl()){
					//var element :String = "r"+String(room)+"_m"+mobb;
					//var table :Number = _control.get(element);
					//if (table == 0) {
						//_control.localChat(element+" does not exsist: Creating...");
						//table = bg.actors.getChildByName("npc_"+mobb).maxhp;
					//}
					//table -= dmgg;
					//_control.localChat("S: "+element+" = "+table);
					//_control.set(element, table);
				//}
			//}
			
			//---Things that only happen if you're in the same room!---
			//if (room == room_num && id != _control.getMyId()){
				//if (bg.actors.getChildByName(moo_id)){
				//}else{
					//var local_name: String = _control.getOccupantName(id);
					//moo = create_player(local_name, "PC", 0, 0, 100, 15, moo_id);
				//}
				
				//if (event.name == p_goal){
						//var gX :Number = Number(event.value[2]);
						//var gY :Number = Number(event.value[3]);
						//moo = bg.actors.getChildByName(moo_id);
						//moo.sprinting = event.value[4];
						//moo.hp = event.value[5];
						//moo.energy = event.value[6];
						//plot_goal(moo, gX, gY);

				//}else if (event.name == p_move){
						//var gX :Number = Number(event.value[2]);
						//var gY :Number = Number(event.value[3]);
						//moo = bg.actors.getChildByName(moo_id);
						//moo.sprinting = event.value[4];
						//moo.hp = event.value[5];
						//moo.energy = event.value[6];
						//player_move(moo, gX, gY);

				//}else if (event.name == p_punch){
						//moo = bg.actors.getChildByName(moo_id);
						//player_punch(moo);

				//}else if (event.name == p_kick){
						//moo = bg.actors.getChildByName(moo_id);
						//player_kick(moo);
				//}else if (event.name == p_block){
						//moo = bg.actors.getChildByName(moo_id);
						//player_block(moo);
				//}else if (event.name == p_block_stop){
						//moo = bg.actors.getChildByName(moo_id);
						//player_block_stop(moo);
				//}else if (event.name == p_hurt){
						//var mob :Number = event.value[2];
						//var dmg :Number = Number(event.value[3]);
						//player_hurt (bg.actors.getChildByName("npc_"+mob), dmg, String(id), Number(event.value[4]), Number(event.value[5]));
				//}else{
					//_control.localChat("UNKNOWN PACKET");
				//}
			//} else if (room != room_num && id != _control.getMyId()){
				//player_delete(moo_id);
			//}
		}
		 	
		public function propertyChanged( event :PropertyChangedEvent):void
		{
		}
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
		//----------------------------------------------------------------------------------------
	}
}