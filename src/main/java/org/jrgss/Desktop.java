package org.jrgss;

import com.badlogic.gdx.Files;
import org.jrgss.JRGSSDesktop;
import com.badlogic.gdx.backends.lwjgl.LwjglApplicationConfiguration;
import org.lwjgl.input.Mouse;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Method;
import java.util.Hashtable;
import static java.lang.System.out;
import java.lang.System;
import org.jrgss.JRGSSLogger;
import static org.jrgss.JRGSSLogger.LogLevels.*;

/**
 * Created by mcanterb on 6/26/14.
 */
public class Desktop {
    static Hashtable<String,String> cliOptions = new Hashtable<String,String>(); // gets filled by the parseCLIArgs function

    public static void main(String[] args) {
        try {
            parseCLIArgs(args);
            LwjglApplicationConfiguration cfg = new LwjglApplicationConfiguration();

            cfg.title = "Title";
            cfg.useGL30 = false;
            cfg.width = 800;
            cfg.height = 450;
            cfg.vSyncEnabled = true;
            cfg.stencil = 8;
            cfg.resizable = true;
            cfg.useHDPI = true;
            cfg.fullscreen = false;
            if(System.getProperty("jrgss.icon") != null && !System.getProperty("os.name").toLowerCase().contains("linux")) {
                cfg.addIcon(System.getProperty("jrgss.icon"), Files.FileType.Absolute);

            } else {
                FileUtil.onCaseSensitiveFileSystem = true;
                JRGSSLogger.println(DEBUG,"Using Case Insensitive File lookups!");
            }
            ConfigReader config = new ConfigReader(cliOptions.get("gameDirectory") + File.separator + "Game.ini");
            cfg.title = config.getTitle();
            setDockIconIfOnOSX(System.getProperty("jrgss.icon"));
            new JRGSSDesktop(new JRGSSGame(cliOptions.get("gameDirectory"), cliOptions.get("rtpDirectory"), config), cfg);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "Encountered an unexpected error: "+e.getMessage(),"Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace(System.err);
        }
    }

    public static void setDockIconIfOnOSX(String iconFile) {
        if(iconFile != null && System.getProperty("os.name").toLowerCase().contains("mac")) {
            try {
                Class<?> clazz = Class.forName("com.apple.eawt.Application");
                Method getApplication = clazz.getDeclaredMethod("getApplication");
                Method setDockIconImage = clazz.getDeclaredMethod("setDockIconImage", Image.class);
                Object application = getApplication.invoke(null);
                BufferedImage icon = ImageIO.read(new File(iconFile));
                setDockIconImage.invoke(application, icon);
            } catch (Exception e) {
                //Failed to set Dock Icon. Probably not on OSX
                System.err.println("Failed to set OS X Dock icon! ");
                e.printStackTrace(System.err);
            }
        }
    }


    //===================================================================================
    // CLI Parsing
    //===================================================================================
    public static class CliOption{
        final String longName;
        final String shortName;
        final String helpDescription;
        final String defaultValue;
        final boolean consumesNextArg;
        final boolean nextArgConsumedOptional; // example is the --verbose flag - you can give a number next or give an argument next at it assumes a default
        final String defaultValueIfSeen; // if this argument does not consume an argument, what value do we give it?
        String value;
        public CliOption(String a, String b, String c, String d, boolean e, boolean f, String g){
            longName=a; shortName=b; helpDescription=c; defaultValue=d; consumesNextArg=e; nextArgConsumedOptional=f; defaultValueIfSeen=g;
            value = defaultValue;
        }
    }
    static CliOption[] availableCliOptions = {
        new CliOption("gameDirectory","g","The directory of the game to run.", ".", true, false, null),
        new CliOption("rtpDirectory","r","The directory of the rtp of the game. It defaults to the same directory as the game.", null, true, false, null),
        new CliOption("verbose","v","turns on debug prints for the java code. Can choose a level of ERROR,INFO,DEBUG,PEDANTIC.", null, true, true, "DEBUG"),
        new CliOption("allowRubyPrints",null,"turns on prints coming from ruby code. They are printed at the INFO log level.", "0", false, false, "1")
    };
    public static void parseCLIArgs(String[] args){

        //just quick and easy-to-use lookup tables for use while we are parsing the opts
        Hashtable<String, CliOption> longOpts = new Hashtable<String, CliOption>();
        Hashtable<String, CliOption> shortOpts = new Hashtable<String, CliOption>();
        //lets get our lookup tables set up
        for (CliOption opt : availableCliOptions){
            longOpts.put(opt.longName,opt);
            if( opt.shortName != null )
                shortOpts.put(opt.shortName,opt);
        }

        //Parsing through all the opts and saving their value
        //we get the name of the opt eg 'verbose' and then look it up in the above hash table
        //then the object from the hash table stores the string in .value for use to use afterwards
        for (int i=0; i<args.length; i++){
            String arg = args[i];
            CliOption opt = null;
            if( arg.equals("-h") || arg.equals("--help") ){
                printHelp();
            } else if( arg.indexOf("--") == 0 ){ // long option
                arg = arg.substring(2);
                opt = longOpts.get(arg);
            }else if( arg.indexOf("-") == 0 ){ // short option
                arg = arg.substring(1);
                opt = shortOpts.get(arg);
            }
            if( opt == null ){
                out.println("Unknown CLI Option \""+arg+'"');
                printHelp();
            }

            //now handle setting the value of the argument
            //either the argument is meant to consume the next argument, or it is just a boolean flag
            if( opt.consumesNextArg && opt.nextArgConsumedOptional )
                if( i+1<args.length && args[i+1].indexOf("-")!=0 ){
                    opt.value = args[i+1];
                    i++; // skip the consumed arg
                }else{
                    opt.value = opt.defaultValueIfSeen;
                }
            else if( opt.consumesNextArg && !opt.nextArgConsumedOptional )
                if( i+1<args.length ){
                    opt.value = args[i+1];
                    i++; // skip the consumed arg
                }else{
                    out.println("Missing argument for \""+arg+'"');
                    printHelp();
                }
            else // does not consume - is just a boolean flag
                opt.value = opt.defaultValueIfSeen;
        }

        //Now that we have parsed all the values out of the cli args, lets do some dependency cleanup and handle the values
        { // defualt the rtpDirctory to the game directory if the user has not already given a location
            CliOption ropt = longOpts.get("rtpDirectory");
            if( ropt.value == null )
                ropt.value = longOpts.get("gameDirectory").value;
        }
        { // debug verbosity prints
            CliOption vopt = longOpts.get("verbose");
            if( vopt.value == null ){
                vopt.value = "ERROR"; // just filling in this to not have an error with the hashtable of the cli option values
            }else{
                JRGSSLogger.printCallerInfo = true; // even if we only want error prints, it would be nice to have the stack traceback
                vopt.value = vopt.value.toUpperCase();
                try{
                    JRGSSLogger.loggingLevel = JRGSSLogger.LogLevels.valueOf(vopt.value);
                }catch(Exception e){
                    JRGSSLogger.println(ERROR,"Unable to parse verbose level '"+vopt.value+"' on the command line - assuming DEBUG");
                    JRGSSLogger.loggingLevel = DEBUG;
                }
            }
        }

        //we put the final values into the classwide hashtable for use elsewhere
        for (CliOption opt : availableCliOptions){
            cliOptions.put(opt.longName,opt.value);
        }
    }

    public static void printHelp(){
        out.println("Available Options:");
        for(CliOption opt : availableCliOptions){
            if( opt.consumesNextArg && opt.nextArgConsumedOptional )
                out.println("\t--"+opt.longName+" -"+opt.shortName+" [argument] :");
            else if( opt.consumesNextArg && !opt.nextArgConsumedOptional )
                out.println("\t--"+opt.longName+" -"+opt.shortName+" <argument> :");
            else
                out.println("\t--"+opt.longName+" -"+opt.shortName+":");
            out.println("\t\t"+opt.helpDescription);
            if( opt.defaultValue != null )
                out.println("\t\tDefault Value : "+opt.defaultValue);
        }
        System.exit(-1);
    }
}
