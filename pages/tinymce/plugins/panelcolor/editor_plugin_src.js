//
// $Id$

/**
 * The panel color plugin provides a button, command and event for controlling the panel color of
 * a tinymce editor. The button is a standard color split button. When a color is selected, the
 * background of the editor's <code>body</code> tag is set and an event is dispatched via the
 * editor's <code>onPanelColorChanged</code> member. Example:
 * <pre>
 *    function myInit (editor) {
 *        editor.onPanelColorChanged.add(function (ed, color) {
 *            alert("New panel color set: " + color);
 *        });
 *        editor.execCommand("setPanelColor", false, "#0000ff");
 *    }
 * </pre>
 */

(function() {
	// import
	var Dispatcher = tinymce.util.Dispatcher, extend = tinymce.extend, DOM = tinymce.DOM;

	// static
	var NAME = "panelcolor";

	// Load plugin specific language pack
	tinymce.PluginManager.requireLangPack('panelcolor');

	tinymce.create('tinymce.plugins.PanelColorPlugin', {
		/**
		 * Initializes the plugin.
		 *
		 * @param {tinymce.Editor} ed Editor instance that the plugin is initialized in.
		 * @param {string} url Absolute URL to where the plugin is located.
		 */
		init : function(ed, url) {
			var t = this;

			// set up our member variables
			t.ed = ed;
			t.url = url;

			// add a new setting to the editor since we need it to default to "1"
			extend(ed.settings, {
				panelcolor_more_colors : 1
			});

			// add an event dispatcher for when the color changes
			ed.onPanelColorChanged = new Dispatcher(ed);

			// test code
			if (false) {
				ed.onPanelColorChanged.add(function (ed, c) {
					alert("testing... event received " + ed.getElement() + ", " + c);
				});
			}

			// Register the command so that it can be invoked by using
			// tinyMCE.activeEditor.execCommand
			ed.addCommand('setPanelColor', function(ui, c) {
				t._setPanelColor(c);
			});
		},

		/**
		 * Creates the control instance for our plugin based in the incoming name. We are a single
		 * button plugin so only handle requests for a control with the same name as the plugin.
		 *
		 * @param {String} n Name of the control to create.
		 * @param {tinymce.ControlManager} cm Control manager to use inorder to create new control.
		 * @return {tinymce.ui.Control} New control instance or null if no control was created.
		 */
		createControl : function(n, cm) {
			if (n == NAME) {
				var t = this, s = t.ed.settings, o = {}, v;

				if (s.panelcolor_more_colors) {
					o.more_colors_func = function() {
						t._showPicker({
							color : t.control.value,
							func : function(co) {
								t.control.setColor(co);
							}
						});
					};
				}

				if (v = s.panelcolor_colors)
					o.colors = v;

				if (s.panelcolor_default_color)
					o.default_color = s.panelcolor_default_color;

				o.title = NAME + '.desc';
				o.onselect = function(v) {
					t._setPanelColor(c.value);
					t.ed.onPanelColorChanged.dispatch(t.ed, c.value);
				};
				o.image = t.url + '/img/' + NAME + '.gif';

				// TODO: not sure what this is doing
				o.scope = t;

				t.control = c = cm.createColorSplitButton(n, o);

				return c;
			}
		},

		/**
		 * Returns information about the plugin as a name/value array.
		 * The current keys are longname, author, authorurl, infourl and version.
		 *
		 * @return {Object} Name/value array containing information about the plugin.
		 */
		getInfo : function() {
			return {
				longname : 'PanelColor plugin',
				author : 'Some author',
				authorurl : 'http://www.whirled.com',
				infourl : 'http://www.whirled.com',
				version : "1.0"
			};
		},

		_showPicker : function(v) {
			var ed = this.ed;

			v = v || {};

			ed.windowManager.open({
				url : ed.theme.url + '/color_picker.htm',
				width : 375 + parseInt(ed.getLang('advanced.colorpicker_delta_width', 0)),
				height : 250 + parseInt(ed.getLang('advanced.colorpicker_delta_height', 0)),
				close_previous : false,
				inline : true
			}, {
				input_color : v.color,
				func : v.func,
				theme_url : ed.theme.url
			});
		},

		_setPanelColor : function (c) {
			var t = this;
			DOM.setStyle(t.ed.getBody(), 'background-color', c);
		}
	});

	// Register plugin
	tinymce.PluginManager.add(NAME, tinymce.plugins.PanelColorPlugin);
})();

