package processing.android.jruby;

import processing.core.PApplet;
import processing.core.PVector;

import android.os.Bundle;
import android.os.AsyncTask;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.util.Log;
import android.content.Context;
import android.view.Menu;
import android.view.MenuItem;

import android.os.PowerManager;
import android.os.PowerManager.WakeLock;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;
import org.jruby.exceptions.RaiseException;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.EvalStaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.scope.ManyVarsDynamicScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.javasupport.util.RuntimeHelpers;

import java.io.PrintStream;
import java.io.FileInputStream;
import java.lang.StringBuilder;
import java.io.File;
import java.net.URI;
import java.io.InputStream;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.apache.http.HttpEntity;

import edu.uic.ketai.inputService.*;

public class PJRubyApplet extends PApplet {

  private static final boolean LOGV = true;
  private static final String TAG = PJRubyApplet.class.getSimpleName();

  private static final int RUBY_LOADING = 1;

  private static Ruby __ruby__;
  private IRubyObject __this__;

  private KetaiSensorManager sensorManager;

  public PVector orientation, magneticField, accelerometer;
  public float proximity, light;

  private PowerManager.WakeLock wl;
  private FileWatcher filewatcher;

  private static DynamicScope scope;

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);

    showDialog(RUBY_LOADING);

    if (__ruby__ == null) {
      __ruby__ = setUpJRuby(null, null);
    }

    dismissDialog(RUBY_LOADING);

    __this__ = JavaUtil.convertJavaToRuby(__ruby__, PJRubyApplet.this);

    orientation   = new PVector();
    magneticField = new PVector();
    accelerometer = new PVector();

    sensorManager = new KetaiSensorManager(this);
    sensorManager.start();
  }

  @Override
  public void onResume() {
    Log.d(TAG, "onResume()");
    super.onResume();

    getWindow().setContentView(surfaceView);

    loadScript(getScriptUrl());

    PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
    if (wl == null) {
      wl = pm.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, "ruboto-processing");
    }
    if (!wl.isHeld()) wl.acquire();
    filewatcher.resume();
  }

  @Override protected void onPause() {
    Log.d(TAG, "onPause()");

    super.onPause();

    if (wl != null && wl.isHeld()) wl.release();
    if (filewatcher != null) filewatcher.pause();
  }

  @Override public void onDestroy() {
    Log.d(TAG, "onDestroy()");

    if (filewatcher != null) filewatcher.cancel(true);
    if (wl != null && wl.isHeld()) wl.release();

    super.onDestroy();
  }

  @Override public void setup() {
    RuntimeHelpers.invoke(__ruby__.getCurrentContext(), __this__, "rsetup");
  }

  @Override public void draw() {
    try {
      RuntimeHelpers.invoke(__ruby__.getCurrentContext(), __this__, "rdraw");
    } catch (Exception e) {
       Log.e(TAG, "exception in ruby code", e);
    }
  }

  public synchronized Ruby setUpJRuby(PrintStream out, String scriptsDir) {
        RubyInstanceConfig config = new RubyInstanceConfig();
        config.setCompileMode(RubyInstanceConfig.CompileMode.OFF);
        config.setLoader(PJRubyApplet.class.getClassLoader());
        if (scriptsDir != null) config.setCurrentDirectory(scriptsDir);

        if (out != null) {
          config.setOutput(out);
          config.setError(out);
        }

        /* Set up Ruby environment */
        Ruby ruby = Ruby.newInstance(config);
        scope = setupScope(ruby);
        return ruby;
  }

  public DynamicScope setupScope(Ruby ruby) {
      ThreadContext context = ruby.getCurrentContext();
      DynamicScope currentScope = context.getCurrentScope();
      return new ManyVarsDynamicScope(new EvalStaticScope(currentScope.getStaticScope()), currentScope);
  }

  public URI getScriptUrl() {
    //return URI.create("https://gist.github.com/raw/7eae9d1bef4b1fdf3c9e/gistfile1.txt");
    return URI.create("file:///sdcard/jruby/processing.rb");
  }

  public void loadScript(URI uri) {
    if (this.filewatcher == null) {
      PJRubyApplet.this.paused = true;

      this.filewatcher = new FileWatcher();
      filewatcher.execute(uri);
    }
  }

  @Override
  protected Dialog onCreateDialog(int id) {
    Log.d(TAG, "onCreateDialog(" +id+")");

    switch (id) {
      case RUBY_LOADING:
        ProgressDialog d = new ProgressDialog(this);
        d.setTitle("Loading JRuby");
        d.setMessage("Please wait");
        d.setIndeterminate(true);
        d.setCancelable(false);
        return d;
      default:
        return null;
     }
  }

  @Override public boolean onCreateOptionsMenu(Menu menu) {
    MenuItem exit = menu.add("Exit");
    exit.setIcon(android.R.drawable.ic_menu_close_clear_cancel);
    return true;
  }

  @Override public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getTitle().equals("Exit")) {
      Log.d(TAG, "exit");
      finish();
      return true;
    }

    return false;
  }

  public String sketchRenderer() { return P2D; }

  private class FileWatcher extends AsyncTask<URI, String, String> {

    HttpClient httpclient = new DefaultHttpClient();
    String lastEtag;
    long lastModified;
    long delay = 2000;
    boolean paused = false;

    ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
      @Override
      public String handleResponse(HttpResponse resp)
        throws org.apache.http.client.HttpResponseException,
               java.io.IOException {

        if (resp.getStatusLine().getStatusCode() >= 300) return null;
        FileWatcher.this.lastEtag = resp.getFirstHeader("ETag").getValue();
        HttpEntity entity = resp.getEntity();
        return entity == null ? null : EntityUtils.toString(entity);
      }
    };


    public void pause()  { paused = true; }
    public void resume() { paused = false; }

    @Override
    protected void onPreExecute() {}

    @Override
    protected String doInBackground(final URI... uris) {
      final URI uri = uris[0];

      while (!isCancelled()) {
          if (!paused) {
            try {
              if (LOGV) Log.v(TAG, "getting script from " + uri);

              String result = null;
              if ("http".equals(uri.getScheme())) {
                final HttpGet get = new HttpGet(uri);
                if (lastEtag != null) {
                  if (LOGV) Log.v(TAG, "If-None-Match: " + lastEtag);
                  get.setHeader("If-None-Match", lastEtag);
                }
                result = httpclient.execute(get, responseHandler);
              } else if ("file".equals(uri.getScheme())) {
                File f = new File(uri.getPath());
                if (!f.exists()) {
                  Log.w(TAG, "could not find file " + f);
                } else if (f.lastModified() > lastModified) {

                  Log.d(TAG, "reading file " + f);

                  byte[] buf = new byte[8192];
                  FileInputStream fis = new FileInputStream(f);
                  StringBuilder sb = new StringBuilder();
                  int n;
                  while ((n = fis.read(buf, 0, buf.length)) != -1)
                    sb.append(new String(buf, 0, n));

                  lastModified = f.lastModified();
                  result = sb.toString();
                }
              }

              if (result != null) {
                publishProgress(result);
              }
            } catch (java.io.IOException e) {
              Log.e(TAG, "error getting script", e);
            }
          }

          try {
            if (LOGV) Log.v(TAG, "sleeping");
            Thread.sleep(delay);
          } catch (InterruptedException e) {

          }
      }
      if (LOGV) Log.v(TAG, "canceled");
      return null;
    }

    @Override
    protected void onProgressUpdate(String... p) {
        if (p != null && p.length > 0 && p[0] != null) {
          PJRubyApplet.this.__ruby__.evalScriptlet(p[0], scope);

          if (PJRubyApplet.this.paused) {
            PJRubyApplet.this.paused = false;
          } else {
            RuntimeHelpers.invoke(__ruby__.getCurrentContext(), __this__, "rsetup");
          }
          Log.d(TAG, "loaded new script: " + p[0].length() + " bytes");
        }
    }

    @Override
    protected void onPostExecute(String result) {
    }
  }

  public void onOrientationSensorEvent(long time, int accuracy, float x, float y, float z) {
   //if (LOGV) Log.v(TAG, String.format("onOrientationSensorEvent(%d, %d, %f, %f, %f)", time, accuracy, x, y, z));
   orientation.set(x, y, z);
  }

  public void onAccelerometerSensorEvent(long time, int accuracy, float x, float y, float z) {
   //if (LOGV) Log.v(TAG, String.format("onAccelerometerSensorEvent(%d, %d, %f, %f, %f)", time, accuracy, x, y, z));
   accelerometer.set(x, y, z);
  }

  public void onMagneticFieldSensorEvent(long time, int accuracy, float x, float y, float z) {
   //if (LOGV) Log.v(TAG, String.format("onMagneticFieldSensorEvent(%d, %d, %f, %f, %f)", time, accuracy, x, y, z));
   magneticField.set(x, y, z);
  }

  public void onProximitySensorEvent(long time, int accuracy, float v) {
   //if (LOGV) Log.v(TAG, String.format("onProximitySensorEvent(%d, %d, %f", time, accuracy, v);
   proximity = v;
  }

  public void onLightSensorEvent(long time, int accuracy, float v) {
   //if (LOGV) Log.v(TAG, String.format("onLightSensorEvent(%d, %d, %f)", time, accuracy, v));
   light = v;
  }
}
