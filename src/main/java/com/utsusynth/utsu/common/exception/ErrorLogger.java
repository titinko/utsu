package com.utsusynth.utsu.common.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

/** A service for handling exceptions. Currently just logs everything to console. */
public class ErrorLogger {

  // Prevent instantiation.
  private ErrorLogger() {}

  private static final ErrorLogger INSTANCE = new ErrorLogger();

  public static ErrorLogger getLogger() {
    return INSTANCE;
  }

  public void logError(Throwable e) {
    // TODO: Inject a callable that can display this error as an alert to the user.
    logToConsole(e);
  }

  public void logWarning(Throwable e) {
    logToConsole(e);
  }

  public void logInfo(Throwable e) {
    logToConsole(e);
  }

  public void logVerbose(Throwable e) {
    logToConsole(e);
  }

  public void logDebug(Throwable e) {
    logToConsole(e);
  }

  private void logToConsole(Throwable e) {
    String errorLine = getCaughtLocation();
    System.out.print(String.format("%s:%n%s", errorLine, getStackTrace(e)));
  }

  /** Returns the location in the code where the exception was caught. */
  private String getCaughtLocation() {
    StackTraceElement caller = Thread.currentThread().getStackTrace()[2];
    return String.format(
        "%s#%s:%s", caller.getFileName(), caller.getMethodName(), caller.getLineNumber());
  }

  /** Prints the stack trace of the given exception to a string. */
  private String getStackTrace(Throwable e) {
    StringWriter stringWriter = new StringWriter();
    PrintWriter printWriter = new PrintWriter(stringWriter);
    e.printStackTrace(printWriter);
    return stringWriter.toString();
  }
}
