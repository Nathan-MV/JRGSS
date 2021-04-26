package org.jrgss;

import static java.lang.System.out;
import java.util.LinkedList;
import java.lang.Thread;
import java.lang.StackTraceElement;

/*TODO : determine if it is a problem using a List for the buffer and consider changing it to a LinkedList for better speed removing the front of the buffer*/

public class JRGSSLogger{
	public static enum LogLevels{
		ERROR,   // A normally silent running program with only prints when things go wrong
		INFO,    // A bit more info but nothing very crazy - RUBY prints will log at this level if turned on
		DEBUG,   // normal debug things, like saying you are setting up windows or deleting sprites or something
		PEDANTIC // Really wondering what is even the point of this level, but for now i will leave it
	}
	static LogLevels loggingLevel = LogLevels.ERROR;
	static int logBufferSize = 25; // how many log messages of debug or less level that is printed on error
	static LinkedList<String> logBuffer = new LinkedList<String>();
	static boolean printCallerInfo = false; // likely you only want to turn this on when you have more verbose printing

	private static String formatMessage(LogLevels lvl, String message){
		if( printCallerInfo ){
			StackTraceElement loggerCaller = Thread.currentThread().getStackTrace()[3];
			String callerInfo = loggerCaller.getFileName()+":"+loggerCaller.getClassName()+":"+loggerCaller.getMethodName()+":"+loggerCaller.getLineNumber()+"  ";
			switch(lvl){
				case ERROR:
					return "[ERROR] "+callerInfo+message;
				case INFO:
					return "[INFO]  "+callerInfo+message;
				case DEBUG:
					return "[DEBUG] "+callerInfo+message;
				case PEDANTIC:
					return "[PEDNT] "+callerInfo+message;
			}
		}else{
			switch(lvl){
				case ERROR:
					return "[ERROR] "+message;
				case INFO:
					return "[INFO]  "+message;
				case DEBUG:
					return "[DEBUG] "+message;
				case PEDANTIC:
					return "[PEDNT] "+message;
			}
		}
		return "[UNKWN] "+message;
	}
	private static void bufferMessage(LogLevels lvl, String message){
		if(lvl == LogLevels.PEDANTIC )
			return;
		logBuffer.add(message);
		while(logBuffer.size() > logBufferSize)
			logBuffer.remove(0);
	}

	//Just normal printing of logs at a certain level
	public static void println(LogLevels lvl, String message){
		message = formatMessage(lvl,message);
		bufferMessage(lvl,message);
		if(lvl.ordinal() <= loggingLevel.ordinal() )
			out.println(message);
	}
	//will print the buffer of messages that we have saved up
	//we do not implicilty print this buffer when asked to do an error level print
	//    because some errors are easily handled and not really eneding extra debug info from users
	public static void printBuffer(){
		out.println();
		out.println("Printing Buffered Messages");
		for( String msg : logBuffer){
			out.println(msg);
		}
	}
}