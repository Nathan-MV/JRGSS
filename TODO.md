# TODO

* Use a thin shim ruby module layer that just directly calls the java methods
	* This will allow the ruby scripts to override and modify behavior
	* Not allowing the overriding will prevent the more extensive games that do more than just vanilla operations
	* Ideally this is not required and we can have JRuby allow dynamic rebinding of Java implementations, but this seems to be a limitation of that implementation
	* I have tried and currently have failed at figuring out how import a java class as a module, so for now we will simply have a shim layer
* Dynamic resizing of games
	* modes : pixel perfect upscalling, letter/pillar boxing, full fill
	* probably default to pixel perfect upscalling
* clean up having the resize code set the draw size to 6 different places on every event - is it really needed to do it so many times?
* Add comments to this abomination of code - a lot of the things in the api folder are sprinked heavily with strange hard-coded numbers and math
	* Window.java and Tilemap.java are the really terrible ones i have seen so far
* Have a flag to dump all scripts to a folder ( --dumpAllScripts )
* Redirect all ruby prints to the logging class at INFO level so we are not spamming the console
* Run through the documentation pages in VXA and make sure all methods mentioned are implemented
	* the Table.xsize parameter was obviously missed, so other things are likely missing as well
	* Also make the shim layer for the Graphics module, similar to the Audio module


# Stretch Goals

* Main Window that is blank and not running a game at all if launched all by itself
	* It would be nice to have a window that has just a few tools available such as selecting a game to try running or simply extracting the data from the game archive
	* These options we would add could be served just as well by command line options, but it would be nice to make it easy for less technical people
* Integrated Debugger
	* --showDebuggerWindows command line option
	* Seperate windows being opened that show what line of code is being executed in Ruby scripts, along with a memory viewer/editor
	* Double stretch goal of the memory viewer having the vaiables and object names tracked, perhaps even pixel rendering for sprites/textures
* Have a keyboard shortcut to open the console to see prints

# Known bugs

## Magical Camp 0.4.7b

* Main Menu - selection of options highlight box is in the wrong location
	* is on the top left out of the window frame
* The Character portaits next to the dialog is just a black box
* Audio on Grumpy's computer is not playing
	* Works on Cam's system using pipewire
	* Works on windows
	* Maybe something like not have alsa support through pulse working correctly on my computer?
* Remapped controls are not working (ie z and x are not performing the same functions at enter and esc)