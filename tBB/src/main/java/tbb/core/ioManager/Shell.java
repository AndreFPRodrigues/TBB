package tbb.core.ioManager;

import android.util.Log;

import java.io.*;
import java.util.concurrent.TimeoutException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import tbb.core.CoreController;
import tbb.core.service.TBBService;

/**
 * Interface to the Superuser shell on Android devices with some helper functions.<p/><p/>
 * Common usage for su shell:<p/>
 * <code>if(ShellInterface.isSuAvailable()) { ShellInterface.runCommand("reboot"); }</code>
 * <p/><p/>
 * To get process output as a String:<p/>
 * <code>if(ShellInterface.isSuAvailable()) { String date = ShellInterface.getProcessOutput("date"); }</code>
 * <p/><p/>
 * To run command with standard shell (no root permissions):
 * <code>ShellInterface.setShell("sh");</code><p/>
 * <code>ShellInterface.runCommand("date");</code>
 * <p/><p/>
 * Date: Mar 24, 2010
 * Time: 4:14:07 PM
 *
 * @author serge
 */

public class Shell {
  private static final String TAG = "Shell";

  private static String shell;

  // uid=0(root) gid=0(root)
  private static final Pattern UID_PATTERN = Pattern.compile("^uid=(\\d+).*?");

  enum OUTPUT {
    STDOUT,
    STDERR,
    BOTH
  }

  private static final String EXIT = "exit\n";

  private static final String[] SU_COMMANDS = new String[]{
          "su",
          "/system/xbin/su",
          "/system/bin/su"
  };

  private static final String[] TEST_COMMANDS = new String[]{
          "id",
          "/system/xbin/id",
          "/system/bin/id"
  };

  public static synchronized boolean isSuAvailable() {
    if (shell == null) {
      checkSu();
    }
    return shell != null;
  }

  public static synchronized void setShell(String shell) {
    Shell.shell = shell;
  }

  private static boolean checkSu() {
    for (String command : SU_COMMANDS) {
      shell = command;
      if (isRootUid()) return true;
    }
    shell = null;
    return false;
  }

  private static boolean isRootUid() {
    String out = null;
    for (String command : TEST_COMMANDS) {
      out = getProcessOutput(command);
      if (out != null && out.length() > 0) break;
    }
    if (out == null || out.length() == 0) return false;
    Matcher matcher = UID_PATTERN.matcher(out);
    if (matcher.matches()) {
      if ("0".equals(matcher.group(1))) {
        return true;
      }
    }
    return false;
  }

  public static String getProcessOutput(String command) {
    try {
      return _runCommand(command, OUTPUT.STDERR);
    } catch (IOException ignored) {
      return null;
    }
  }

  public static boolean runCommand(String command) {
    try {
      _runCommand(command, OUTPUT.BOTH);
      return true;
    } catch (IOException ignored) {
      return false;
    }
  }

  public static boolean runCommandNoWait(String command) {
    try {
      executeCommandLine(command, 1000,OUTPUT.BOTH);
      return true;
    } catch (IOException ignored) {
    } catch (InterruptedException e) {
      e.printStackTrace();
    } catch (TimeoutException e) {
      e.printStackTrace();
    }
    return false;

  }
  public static int executeCommandLine(final String commandLine,
                                       final long timeout,final OUTPUT o)
          throws IOException, InterruptedException, TimeoutException {
    DataOutputStream os = null;
    Process process = null;
    Runtime runtime = Runtime.getRuntime();
    process = Runtime.getRuntime().exec(new String [] {shell, "echo done;\n"});
    os = new DataOutputStream(process.getOutputStream());
    InputStreamHandler sh = sinkProcessOutput(process, o);
    os.writeBytes(commandLine + "&& echo donwe \n" );
    os.flush();
    os.writeBytes(EXIT);
    os.flush();
    os.writeBytes("echo donwe;" );
    os.flush();
    Worker worker = new Worker(process);
    worker.start();
    try {
      worker.join(timeout);
      if (worker.exit != null)
        return worker.exit;
      else
        throw new TimeoutException();
    } catch(InterruptedException ex) {
      worker.interrupt();
      Thread.currentThread().interrupt();
      throw ex;
    } finally {
      process.destroy();
    }
  }

  private static class Worker extends Thread {
    private final Process process;
    private Integer exit;
    private Worker(Process process) {
      this.process = process;
    }
    public void run() {
      try {
        exit = process.waitFor();
      } catch (InterruptedException ignore) {
        return;
      }
    }
  }
  private static String _runCommandNoWait(final String command, final OUTPUT o) throws IOException {
    Thread t = new Thread() {
      @Override
      public void run() {
        DataOutputStream os = null;
        Process process = null;
        try {
          process = Runtime.getRuntime().exec(new String [] {shell, "echo done;\n"});
          os = new DataOutputStream(process.getOutputStream());
          InputStreamHandler sh = sinkProcessOutput(process, o);
          os.writeBytes(command + "&& echo donwe \n" );
          os.flush();
          os.writeBytes(EXIT);
          os.flush();
          os.writeBytes("echo donwe;" );
          os.flush();
          Log.e(TAG, "Waiting init: ");

          process.waitFor();
        } catch (InterruptedException e) {
          e.printStackTrace();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    };

    t.start();
    Log.e(TAG, "Waiting end: ");

    return  "";


  }

  private static String _runCommand(String command, OUTPUT o) throws IOException {
    DataOutputStream os = null;
    Process process = null;
    try {
      process = Runtime.getRuntime().exec(shell);
      os = new DataOutputStream(process.getOutputStream());
      InputStreamHandler sh = sinkProcessOutput(process, o);
      os.writeBytes(command + '\n');
      os.flush();
      os.writeBytes(EXIT);
      os.flush();
      process.waitFor();
      if (sh != null) {
        String output = sh.getOutput();
        return output;
      } else {
        return null;
      }
    } catch (Exception e) {
      final String msg = e.getMessage();
      Log.e(TAG, "runComman d error: " + msg);
      CoreController.sharedInstance().permission=false;
      TBBService.writeToErrorLog(e);
      throw new IOException(msg);
    } finally {
      try {
        if (os != null) {
          os.close();
        }
        if (process != null) {
          process.destroy();
        }
      } catch (Exception ignored) {
      }
    }
  }

  public static InputStreamHandler sinkProcessOutput(Process p, OUTPUT o) {
    InputStreamHandler output = null;
    switch (o) {
      case STDOUT:
        output = new InputStreamHandler(p.getErrorStream(), false);
        new InputStreamHandler(p.getInputStream(), true);
        break;
      case STDERR:
        output = new InputStreamHandler(p.getInputStream(), false);
        new InputStreamHandler(p.getErrorStream(), true);
        break;
      case BOTH:
        new InputStreamHandler(p.getInputStream(), true);
        new InputStreamHandler(p.getErrorStream(), true);
        break;
    }
    return output;
  }

  private static class InputStreamHandler extends Thread {
    private final InputStream stream;
    private final boolean     sink;
    StringBuffer output;

    public String getOutput() {
      return output.toString();
    }

    InputStreamHandler(InputStream stream, boolean sink) {
      this.sink = sink;
      this.stream = stream;
      start();
    }

    @Override
    public void run() {
      try {
        if (sink) {
          while (stream.read() != -1) {}
        } else {
          output = new StringBuffer();
          BufferedReader b = new BufferedReader(new InputStreamReader(stream));
          String s;
          while ((s = b.readLine()) != null) {
            output.append(s);
          }
        }
      } catch (IOException ignored) {}
    }
  }
}
