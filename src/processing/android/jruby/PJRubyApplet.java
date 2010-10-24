package processing.android.jruby;

import processing.core.PApplet;

import android.os.Bundle;
import android.os.AsyncTask;
import android.util.Log;

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

public class PJRubyApplet extends PApplet {

  private static final boolean LOGV = false;
  private static final String TAG = PJRubyApplet.class.getSimpleName();

  private Ruby __ruby__;
  private IRubyObject __this__;

  private static DynamicScope scope;

  @Override
  public void onCreate(Bundle saved) {
    super.onCreate(saved);

    __ruby__ = setUpJRuby(null, null);
    __this__ = JavaUtil.convertJavaToRuby(__ruby__, PJRubyApplet.this);
  }

  @Override
  public void onResume() {
    super.onResume();
    loadScript(getScriptUrl());
  }

  @Override 
  public void setup() {
    RuntimeHelpers.invoke(__ruby__.getCurrentContext(), __this__, "rsetup");
  }

  @Override
  public void draw() {
    RuntimeHelpers.invoke(__ruby__.getCurrentContext(), __this__, "rdraw");
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
    return URI.create("https://gist.github.com/raw/7eae9d1bef4b1fdf3c9e/gistfile1.txt");
  }

  public void loadScript(URI uri) {
    PJRubyApplet.this.paused = true;
    new FileWatcher().execute(uri);
  }

  public int sketchWidth() { return 320; }
  public int sketchHeight() { return 480; }
  public String sketchRenderer() { return P2D; }

  private class FileWatcher extends AsyncTask<URI, String, String> {
    HttpClient httpclient = new DefaultHttpClient();
    String lastEtag;
    long delay = 2000;

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


    @Override
    protected void onPreExecute() {}

    @Override
    protected String doInBackground(final URI... uris) {
      final URI uri = uris[0];

      while (true) {
          try {
            if (LOGV) Log.v(TAG, "getting script from " + uri);

            final HttpGet get = new HttpGet(uri);
            if (lastEtag != null) {
              if (LOGV) Log.v(TAG, "If-None-Match: " + lastEtag);
              get.setHeader("If-None-Match", lastEtag);
            }

            publishProgress(httpclient.execute(get, responseHandler));

          } catch (java.io.IOException e) {
            Log.e(TAG, "error getting script", e);
          }

          try {
            if (LOGV) Log.v(TAG, "sleeping");
            Thread.sleep(delay);
          } catch (InterruptedException e) {

          }
      }
    }

    @Override
    protected void onProgressUpdate(String... p) {
        if (p != null && p.length > 0 && p[0] != null) {
          if (LOGV) Log.v(TAG, "progress: " + p[0].length() + " bytes");
          PJRubyApplet.this.__ruby__.evalScriptlet(p[0], scope);
          PJRubyApplet.this.paused = false;
        }
    }

    @Override
    protected void onPostExecute(String result) {
    }
  }
}
