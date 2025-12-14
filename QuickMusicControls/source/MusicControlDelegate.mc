using Toybox.WatchUi as Ui;
using Toybox.Application as App;
using Toybox.Communications as Comm;
using Toybox.System as Sys;
import Toybox.Lang;

class MusicControlDelegate extends Ui.InputDelegate {

    private var isVolumeMode = false;
    private var gpsPressStartTime as Number or Null = null;

    private const LONG_PRESS_THRESHOLD = 400;

    function initialize() {
        InputDelegate.initialize();

        isVolumeMode = false;
        gpsPressStartTime = null;

        (App.getApp() as QuickMusicControlsApp).setDelegate(self);
    }

    function getMode() as Boolean {
        return isVolumeMode;
    }

    function onKeyPressed(evt as Ui.KeyEvent) as Boolean {
        var key = evt.getKey();
        var action = null;

        // System.println(key.toString());

        if (key == Ui.KEY_ENTER) {
            if (gpsPressStartTime == null) {
                gpsPressStartTime = Sys.getTimer();
            }
            // Do NOT perform any action yet, wait for release
            return true;
        }

        if (!isVolumeMode) {

            if (key == Ui.KEY_UP) {
                action = "NEXT_TRACK";
            } else if (key == Ui.KEY_DOWN) {
                action = "PREV_TRACK";
            }

        } else {

            if (key == Ui.KEY_UP) {
                action = "VOLUME_UP";
            } else if (key == Ui.KEY_DOWN) {
                action = "VOLUME_DOWN";
            }
        }
        
        if (action != null) {
            sendToMobile(action);
            //System.println("Command Sent : " + action);
            return true;
        }

        return false;
    }

    function onKeyReleased(evt as Ui.KeyEvent) as Boolean {
        var key = evt.getKey();
        
        if (key == Ui.KEY_ENTER) {
            if (gpsPressStartTime != null) {
                var pressDuration = Sys.getTimer() - gpsPressStartTime;
                gpsPressStartTime = null;
                
                if (pressDuration >= LONG_PRESS_THRESHOLD) {

                    isVolumeMode = !isVolumeMode;
                    // Sys.println("Mode Toggled: " + (isVolumeMode ? "Track Skip" : "Volume"));

                    Ui.requestUpdate(); 
                } else {
                    sendToMobile("PLAY_PAUSE");
                    // System.println("Play Pause Command Sent");
                }
            }
            return true;
        }
        return false;
    }

    function sendToMobile(command as String) as Void {
        var message = { "COMMAND" => command };
        Comm.transmit(message, null, new CommListener());
    }
}

// A simple listener to check for communication results (optional but good practice)
class CommListener extends Comm.ConnectionListener {
    function onComplete() as Void {
        // Sys.println("Message sent successfully");
        // Ui.requestUpdate(); // Redraw UI to reflect potential change
    }

    function onError() as Void {
        // Sys.println("Message failed to send");
        // Ui.requestUpdate();
    }
}
