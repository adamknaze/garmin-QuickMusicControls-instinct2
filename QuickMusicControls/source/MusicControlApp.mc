using Toybox.Application as App;
using Toybox.WatchUi as Ui;

class QuickMusicControlsApp extends App.AppBase {

    private var currentDelegate as MusicControlDelegate or Null;

    // Called on application start
    function initialize() {
        AppBase.initialize();
    }

    // Return the initial view and delegate for the widget
    function getInitialView() {
        return [ new MusicControlView(), new MusicControlDelegate() ];
    }

    function setDelegate(delegate as MusicControlDelegate) as Void {
        currentDelegate = delegate;
    }

    function getDelegate() as MusicControlDelegate or Null {
        return currentDelegate;
    }


    // Handle messages from the companion mobile app
    function onMessage(msg) {
        // Implement logic to receive playback status, current track info, etc.
        // from the Android app here.
    }
}
