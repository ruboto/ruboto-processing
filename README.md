
A quick hack to use JRuby with [Android processing](http://wiki.processing.org/w/Android).

At the moment it expects a processing sketch with two methods: `rsetup` which
is the ruby equivalent of `setup()` and `rdraw` which is the normal `draw()`
method.

Copy a script (cf. assets/bird.rb) to `/sdcard/jruby/processing.rb` and run the
app.
