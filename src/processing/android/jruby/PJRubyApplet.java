package processing.android.jruby;

import processing.core.PApplet;

import android.os.Bundle;
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

public class PJRubyApplet extends PApplet {

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
    __ruby__.evalScriptlet(getScript(), scope);
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

  public String getScript() {
    final URI url = getScriptUrl();
    Log.d(TAG, "getting script from " + url);

    try {
      HttpClient httpclient = new DefaultHttpClient();  
      ResponseHandler<String> responseHandler = new BasicResponseHandler();
      return httpclient.execute(new HttpGet(url), responseHandler);
    } catch (java.io.IOException e) {
      Log.e(TAG, "error getting script", e);
      return "";
    }
  }

  public int sketchWidth() { return 320; }
  public int sketchHeight() { return 480; }
  public String sketchRenderer() { return P2D; }
}
