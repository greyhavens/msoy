Adobe AIR Application Install Badge
Beta 3

This is a sample Flash movie and supporting HTML file for the seamless detection and installation of the Adobe AIR runtime along with the installation of an AIR application. Using this 'badge' allows you as a developer to have greater control over the installation experience that end users have when setting up your AIR application. Rather than requiring the user to go to the Adobe website to download the standalone AIR runtime installer and then return to your site to download and open the AIR installation package, with this badge any user with Flash Player 7 or higher can install the runtime inlined with the installation of your application.

This badge allows for an image to be loaded into the button dynamically, and for the color of the badge and the button to be changed via variables which are set in the FlashVars parameters in the Object and Embed tags. Please note that the the FlashVars need to be repeated three times in the HTMl file: once for passing to the externalized JavaScript detection, and once each for the Object & Embed tags within the <noscript> tag.

Required variables (to be passed in FlashVars parameter of Object & Embed tags in HTML):
o appname (name of application displayed in message under install button if runtime is not present)
o appurl (url of .air file on server)
o airversion (version of the AIR Runtime required)

Optional variables:
o buttoncolor (six digit hex value of button background; setting value to "transparent" is also possible)
o messagecolor (six digit hex value of text message displayed under install button)
o imageurl (url of .jpg file to load). The URL should either be a relative path or use the HTTP, HTTPS or FTP scheme.

Note that all of these values must be escaped per the requirements of the FlashVars parameter.

Also note that you can set the badge background color with the standard Object/Embed "bgcolor" parameter.

For more information on how to use this badge follow the documentation found here:
http://labs.adobe.com/technologies/air/