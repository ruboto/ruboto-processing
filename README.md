
A quick hack to use JRuby with [Android processing](http://wiki.processing.org/w/Android).

At the moment it expects a processing sketch with two methods: `rsetup` which
is the ruby equivalent of `setup()` and `rdraw` which is the normal `draw()`
method.

Copy a script (cf. assets/bird.rb) to `/sdcard/jruby/processing.rb` and run the
app.

## Building

You might need to modify the dx script as described in the [Ruboto IRB
readme](https://github.com/ruboto/ruboto-irb#readme).

   $ cp local.properties.EXAMPLE local.properties
   $ vi local.properties
   $ ant debug
   $ adb install -r bin/ruboto-processing-debug.apk
