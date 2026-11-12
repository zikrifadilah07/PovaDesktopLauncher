package id.unjani.povadesktop;

import android.app.Activity;
import android.app.ActivityOptions;
import android.app.Presentation;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.Build;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.webkit.WebView;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends Activity {
    private DisplayManager displayManager;
    private Presentation externalPresentation;
    private Desktop surface;
    private static final int BLUE = Color.rgb(15, 33, 61);
    private static final int PANEL = Color.rgb(24, 45, 79);
    private static final int WHITE = Color.WHITE;

    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        surface = new Desktop(this, Display.DEFAULT_DISPLAY);
        setContentView(surface.root);
        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        displayManager.registerDisplayListener(listener, null);
        refreshExternal();
    }
    @Override protected void onDestroy() {
        displayManager.unregisterDisplayListener(listener);
        closeExternal();
        super.onDestroy();
    }
    private final DisplayManager.DisplayListener listener = new DisplayManager.DisplayListener() {
        @Override public void onDisplayAdded(int id) { runOnUiThread(() -> refreshExternal()); }
        @Override public void onDisplayRemoved(int id) { runOnUiThread(() -> refreshExternal()); }
        @Override public void onDisplayChanged(int id) { runOnUiThread(() -> refreshExternal()); }
    };
    private void closeExternal() {
        if (externalPresentation != null) {
            externalPresentation.dismiss();
            externalPresentation = null;
        }
    }
    private void refreshExternal() {
        Display chosen = null;
        for (Display d : displayManager.getDisplays(DisplayManager.DISPLAY_CATEGORY_PRESENTATION)) {
            if (d.getDisplayId() != Display.DEFAULT_DISPLAY) { chosen = d; break; }
        }
        if (chosen == null) { closeExternal(); surface.updateMonitorStatus(false); return; }
        if (externalPresentation != null && externalPresentation.getDisplay().getDisplayId() == chosen.getDisplayId()) {
            surface.updateMonitorStatus(true); return;
        }
        closeExternal();
        try {
            final Display selected = chosen;
            externalPresentation = new Presentation(this, selected) {
                @Override protected void onCreate(Bundle b) {
                    super.onCreate(b);
                    Desktop secondary = new Desktop(MainActivity.this, selected.getDisplayId());
                    setContentView(secondary.root);
                }
            };
            externalPresentation.show();
            surface.updateMonitorStatus(true);
        } catch (Exception e) {
            closeExternal();
            surface.updateMonitorStatus(false);
            Toast.makeText(this, "Monitor terdeteksi tetapi desktop eksternal gagal: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private int dp(float n) { return Math.round(getResources().getDisplayMetrics().density * n); }
    private GradientDrawable bg(int color, int radius) {
        GradientDrawable d = new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d;
    }
    private TextView text(String s, int size, boolean bold) {
        TextView t = new TextView(this); t.setText(s); t.setTextColor(WHITE); t.setTextSize(size);
        t.setGravity(Gravity.CENTER_VERTICAL); t.setPadding(dp(12), 0, dp(12), 0);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }
    private TextView button(String label, Runnable callback) {
        TextView b = text(label, 14, true); b.setGravity(Gravity.CENTER); b.setBackground(bg(PANEL, 10));
        b.setOnClickListener(v -> callback.run()); return b;
    }
    private class Desktop {
        final FrameLayout root = new FrameLayout(MainActivity.this);
        final FrameLayout desktop = new FrameLayout(MainActivity.this);
        final LinearLayout bar = new LinearLayout(MainActivity.this);
        final int displayId;
        int count = 0;
        TextView monitorStatus;
        Desktop(Context c, int id) {
            displayId = id;
            root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.rgb(8, 22, 43), Color.rgb(28, 67, 112)}));
            root.addView(desktop, new FrameLayout.LayoutParams(-1,-1));
            LinearLayout shortcuts = new LinearLayout(MainActivity.this);
            shortcuts.setOrientation(LinearLayout.VERTICAL);
            shortcuts.setPadding(dp(14), dp(32), 0, 0);
            addShortcut(shortcuts, "Files", () -> openWindow("Files", "Gunakan menu Aplikasi untuk membuka file manager sistem."));
            addShortcut(shortcuts, "Notes", () -> openWindow("Notes", ""));
            addShortcut(shortcuts, "Browser", () -> openBrowser());
            addShortcut(shortcuts, "Aplikasi", () -> openApps());
            FrameLayout.LayoutParams sc = new FrameLayout.LayoutParams(dp(116),-2,Gravity.TOP|Gravity.LEFT);
            desktop.addView(shortcuts, sc);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            bar.setPadding(dp(8),dp(5),dp(8),dp(5));
            bar.setBackgroundColor(Color.rgb(10,20,39));
            TextView start = button("START", () -> openApps());
            bar.addView(start, new LinearLayout.LayoutParams(dp(80),-1));
            TextView notes = button("Notes", () -> openWindow("Notes", ""));
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(dp(80),-1); np.leftMargin=dp(6); bar.addView(notes,np);
            TextView browser = button("Web", () -> openBrowser());
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(65),-1); bp.leftMargin=dp(6); bar.addView(browser,bp);
            monitorStatus = text("Layar HP", 11, false);
            monitorStatus.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            bar.addView(monitorStatus,new LinearLayout.LayoutParams(0,-1,1));
            FrameLayout.LayoutParams task = new FrameLayout.LayoutParams(-1,dp(54),Gravity.BOTTOM);
            root.addView(bar,task);
            root.post(() -> openWindow("Selamat datang", "Pova Desktop v0.1\n\nJendela internal bisa digeser melalui judul dan diubah ukurannya melalui sudut kanan bawah.\n\nSTART membuka aplikasi yang terpasang. Mode floating aplikasi eksternal bergantung pada HiOS.\n\nMonitor eksternal membutuhkan display yang dikenali Android, bukan hanya kabel atau screen mirroring."));
        }
        void updateMonitorStatus(boolean attached) {
            if (monitorStatus != null) monitorStatus.setText(attached ? "Monitor terhubung" : "Layar HP");
        }
        void addShortcut(LinearLayout parent, String title, Runnable run) {
            TextView t = button(title, run);
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(dp(98),dp(55));p.bottomMargin=dp(10);parent.addView(t,p);
        }
        void openWindow(String title, String value) {
            EditText editor = new EditText(MainActivity.this);
            editor.setText(value); editor.setTextColor(Color.rgb(20,30,45));
            editor.setTextSize(15); editor.setGravity(Gravity.TOP);
            editor.setBackgroundColor(Color.WHITE); editor.setPadding(dp(12), dp(12), dp(12),dp(12));
            editor.setHint("Tulis di sini (catatan tidak disimpan otomatis)...");
            createWindow(title, editor);
        }
        void openBrowser() {
            LinearLayout l = new LinearLayout(MainActivity.this); l.setOrientation(LinearLayout.VERTICAL);
            EditText url = new EditText(MainActivity.this); url.setSingleLine(true); url.setText("https://example.com");
            url.setTextColor(Color.BLACK); url.setBackgroundColor(Color.WHITE);
            l.addView(url,new LinearLayout.LayoutParams(-1,dp(44)));
            WebView web = new WebView(MainActivity.this);
            // No INTERNET permission in prototype. Browser demonstrates UI only.
            TextView note = text("Browser internal nonaktif pada prototipe offline. Gunakan START untuk membuka browser sistem.", 13, false);
            note.setBackgroundColor(Color.rgb(70,84,107));
            l.addView(note,new LinearLayout.LayoutParams(-1,0,1));
            createWindow("Browser (demo offline)",l);
        }
        void openApps() {
            List<ResolveInfo> items = getPackageManager().queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER), 0);
            Collections.sort(items, Comparator.comparing(r -> r.loadLabel(getPackageManager()).toString().toLowerCase()));
            ScrollView scroll = new ScrollView(MainActivity.this);
            LinearLayout list = new LinearLayout(MainActivity.this); list.setOrientation(LinearLayout.VERTICAL);
            list.setBackgroundColor(Color.rgb(245,247,252));
            for (ResolveInfo item : items) {
                String label = item.loadLabel(getPackageManager()).toString();
                TextView entry = text(label, 15, false);
                entry.setTextColor(Color.rgb(25,37,55));
                entry.setOnClickListener(v -> launchApp(item));
                list.addView(entry,new LinearLayout.LayoutParams(-1,dp(46)));
            }
            scroll.addView(list); createWindow("Aplikasi terpasang (" + items.size() + ")",scroll);
        }
        void launchApp(ResolveInfo app) {
            try {
                Intent i = new Intent(Intent.ACTION_MAIN);
                i.addCategory(Intent.CATEGORY_LAUNCHER);
                i.setClassName(app.activityInfo.packageName,app.activityInfo.name);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                ActivityOptions options = ActivityOptions.makeBasic();
                if (displayId != Display.DEFAULT_DISPLAY) options.setLaunchDisplayId(displayId);
                Rect r = new Rect(dp(70),dp(80),dp(540),dp(590));
                options.setLaunchBounds(r);
                startActivity(i,options.toBundle());
            } catch (Exception e) {
                Toast.makeText(MainActivity.this,"Tidak dapat membuka aplikasi: " + e.getMessage(),Toast.LENGTH_LONG).show();
            }
        }
        void createWindow(String title, View content) {
            int w = Math.max(dp(250), Math.min(dp(440), root.getWidth()-dp(32)));
            int h = Math.max(dp(220), Math.min(dp(450), root.getHeight()-dp(80)));
            int x = dp(20 + ((count%5)*18)); int y = dp(42 + ((count%5)*22)); count++;
            LinearLayout window = new LinearLayout(MainActivity.this);
            window.setOrientation(LinearLayout.VERTICAL); window.setBackground(bg(WHITE,12));window.setElevation(dp(12));
            LinearLayout header = new LinearLayout(MainActivity.this);header.setGravity(Gravity.CENTER_VERTICAL);
            header.setBackground(bg(PANEL,10));
            TextView name = text(title, 15,true); header.addView(name,new LinearLayout.LayoutParams(0,dp(46),1));
            TextView hide = button("_", () -> window.setVisibility(View.GONE));
            header.addView(hide,new LinearLayout.LayoutParams(dp(38),dp(40)));
            TextView close = button("X", () -> desktop.removeView(window));
            header.addView(close,new LinearLayout.LayoutParams(dp(38),dp(40)));
            window.addView(header,new LinearLayout.LayoutParams(-1,dp(46)));
            window.addView(content,new LinearLayout.LayoutParams(-1,0,1));
            TextView resize = text("◢  drag to resize", 11,false);
            resize.setTextColor(Color.DKGRAY);resize.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            window.addView(resize,new LinearLayout.LayoutParams(-1,dp(28)));
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w,h);lp.leftMargin=x;lp.topMargin=y;
            desktop.addView(window,lp);
            window.setOnClickListener(v -> window.bringToFront());
            header.setOnTouchListener(new View.OnTouchListener() {
                float downX,downY; int left,top;
                @Override public boolean onTouch(View v, MotionEvent e) {
                    if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getRawX();downY=e.getRawY();FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)window.getLayoutParams();left=p.leftMargin;top=p.topMargin;window.bringToFront();return true;}
                    if(e.getAction()==MotionEvent.ACTION_MOVE){FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)window.getLayoutParams();p.leftMargin=Math.max(0,Math.min(root.getWidth()-dp(80),left+Math.round(e.getRawX()-downX)));p.topMargin=Math.max(0,Math.min(root.getHeight()-dp(90),top+Math.round(e.getRawY()-downY)));window.setLayoutParams(p);return true;}
                    return e.getAction()==MotionEvent.ACTION_UP;
                }
            });
            resize.setOnTouchListener(new View.OnTouchListener(){float downX,downY;int initialW,initialH;
                @Override public boolean onTouch(View v,MotionEvent e){
                    if(e.getAction()==MotionEvent.ACTION_DOWN){downX=e.getRawX();downY=e.getRawY();initialW=window.getWidth();initialH=window.getHeight();return true;}
                    if(e.getAction()==MotionEvent.ACTION_MOVE){FrameLayout.LayoutParams p=(FrameLayout.LayoutParams)window.getLayoutParams();p.width=Math.max(dp(230),Math.min(root.getWidth()-p.leftMargin,initialW+Math.round(e.getRawX()-downX)));p.height=Math.max(dp(160),Math.min(root.getHeight()-dp(56)-p.topMargin,initialH+Math.round(e.getRawY()-downY)));window.setLayoutParams(p);return true;}
                    return e.getAction()==MotionEvent.ACTION_UP;
                }
            });
        }
    }
}
