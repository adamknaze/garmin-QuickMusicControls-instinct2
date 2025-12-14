using Toybox.WatchUi as Ui;
using Toybox.Graphics as Gfx;
using Toybox.Application as App;

class MusicControlView extends Ui.View {

    function initialize() {
        View.initialize();
    }

    function onUpdate(dc as Gfx.Dc) as Void {
        dc.setColor(Gfx.COLOR_BLACK, Gfx.COLOR_BLACK);
        dc.clear();
        dc.setColor(Gfx.COLOR_WHITE, Gfx.COLOR_TRANSPARENT);

        var w = dc.getWidth();
        var h = dc.getHeight();

        var app = App.getApp() as QuickMusicControlsApp;
        var delegate = app.getDelegate();
        var mode = delegate != null ? delegate.getMode() : false;

        // ───── Top Left: Mode ─────
        var modeText = mode ? "VOLUME" : "SKIP";
        dc.drawText(30, 5, Gfx.FONT_TINY, "Mode: ", Gfx.TEXT_JUSTIFY_LEFT);
        dc.drawText(30, 25, Gfx.FONT_SMALL, modeText, Gfx.TEXT_JUSTIFY_LEFT);

        // ───── Top Right: Play/Pause Icon ─────
        drawPlayIcon(dc, w - 27, 10, 16, false);
        drawPauseIcon(dc, w - 48, 33, 16);
        dc.drawLine(w - 60, 0, w, 60);

        // ───── Divider ─────
        dc.drawLine(5, 27, w - 70, 27);

        // ───── Button Labels ─────
        if (!mode) {
            // Track mode
            dc.drawText(10, h / 2 - 12, Gfx.FONT_TINY, "Next track", Gfx.TEXT_JUSTIFY_LEFT);
            drawPlayIcon(dc, 90, h / 2 - 5, 10, false);
            drawPlayIcon(dc, 100, h / 2 - 5, 10, false);
            dc.drawText(10, h - 55, Gfx.FONT_TINY, "Prev track", Gfx.TEXT_JUSTIFY_LEFT);
            drawPlayIcon(dc, 90, h - 48, 10, true);
            drawPlayIcon(dc, 100, h - 48, 10, true);
        } else {
            // Volume mode
            dc.drawText(10, h / 2 - 12, Gfx.FONT_TINY, "Volume +", Gfx.TEXT_JUSTIFY_LEFT);
            dc.drawText(10, h - 55, Gfx.FONT_TINY, "Volume -", Gfx.TEXT_JUSTIFY_LEFT);
        }
    }

    function drawPlayIcon(dc, x, y, size, reversed) {
        var points = null;
        if (!reversed) {
            points = [
                [x, y],
                [x, y + size],
                [x + size - 2, y + size / 2]
            ];
        }
        else {
            points = [
                [x + size - 2, y],
                [x + size - 2, y + size],
                [x, y + size / 2]
            ];
        }
        dc.fillPolygon(points);
    }

    function drawPauseIcon(dc, x, y, size) {
        dc.fillRectangle(x, y, 5, size);
        dc.fillRectangle(x + 10, y, 5, size);
    }
}
