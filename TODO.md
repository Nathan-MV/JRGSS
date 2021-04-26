# TODO

* Use a thin shim ruby module layer that just directly calls the java methods
	* This will allow the ruby scripts to override and modify behavior
	* Not allowing the overriding will prevent the more extensive games that do more than just vanilla operations
	* Ideally this is not required and we can have JRuby allow dynamic rebinding of Java implementations, but this seems to be a limitation of that implementation
	* Optionally, try having the clkass import inside a module declaration of the same name - it might work? (JRGSSGame.java : loadRGSSModule(name))
* Dynamic resizing of games
	* modes : pixel perfect upscalling, letter/pillar boxing, full fill
	* probably default to pixel perfect upscalling
* clean up having the resize code set the draw size to 6 different places on every event - is it really needed to do it so many times?
* Add comments to this abomination of code - a lot of the things in the api folder are sprinked heavily with strange hard-coded numbers and math
	* Window.java
	* Tilemap.java
* Finish adding in prints throughout the code to help with debugging
* Have a flag to dump all scripts to a folder ( --dumpAllScripts )
* Redirect all ruby prints to the logging class at INFO level so we are not spamming the console


# Stretch Goals

* Main Window that is blank and not running a game at all if launched all by itself
	* It would be nice to have a window that has just a few tools available such as selecting a game to try running or simply extracting the data from the game archive
	* These options we would add could be served just as well by command line options, but it would be nice to make it easy for less technical people
* Integrated Debugger
	* --showDebuggerWindows command line option
	* Seperate windows being opened that show what line of code is being executed in Ruby scripts, along with a memory viewer/editor
	* Double stretch goal of the memory viewer having the vaiables and object names tracked, perhaps even pixel rendering for sprites/textures

# Known bugs

## Magical Camp 0.4.7b

* Main Menu - selection of options highlight box is in the wrong location
	* is on the top left out of the window frame
* Loading a save game crashes from attempting access a method that does not exist
	* undefined method xsize' for #<Table:0x59e06b06>
	* RUBY.kinu_make_shadows(Kinu2 - Three-Tiered Shadows v1.01:285)
* Audio on the main menu is not playing
	* debug seems to show the requested volume is 0?