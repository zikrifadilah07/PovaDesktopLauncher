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
import android.text.format.DateFormat;
import java.util.Date;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Parcel;
import android.content.ComponentName;
import android.content.ServiceConnection;
import rikka.shizuku.Shizuku;
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
    private static final int BLUE = Color.rgb(13, 27, 53);
    private static final int PANEL = Color.rgb(35, 57, 92);
    private static final int WHITE = Color.WHITE;

    private static final int SHIZUKU_REQUEST_CODE = 1001;
    private IBinder freeformService;
    private boolean bindingShizuku = false;
    private String pendingComponent;
    private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, grantResult) -> {
        if (requestCode != SHIZUKU_REQUEST_CODE) return;
        runOnUiThread(() -> {
            if (grantResult == PackageManager.PERMISSION_GRANTED) bindFreeformService();
            else Toast.makeText(this, "Izin Shizuku ditolak. Freeform tidak dijalankan.", Toast.LENGTH_LONG).show();
        });
    };
    private final ServiceConnection freeformConnection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {
            bindingShizuku = false;
            freeformService = service;
            runOnUiThread(() -> {
                Toast.makeText(MainActivity.this, "Shizuku siap untuk Freeform", Toast.LENGTH_SHORT).show();
                if (pendingComponent != null) {
                    String component = pendingComponent;
                    pendingComponent = null;
                    launchThroughShizuku(component);
                }
            });
        }
        @Override public void onServiceDisconnected(ComponentName name) {
            freeformService = null;
            bindingShizuku = false;
        }
    };
    private Shizuku.UserServiceArgs serviceArgs() {
        return new Shizuku.UserServiceArgs(new ComponentName(this, FloatingUserService.class))
            .processNameSuffix("freeform").daemon(false).version(1);
    }
    private void ensureShizuku() {
        try {
            if (!Shizuku.pingBinder()) {
                Toast.makeText(this, "Jalankan Shizuku terlebih dahulu.", Toast.LENGTH_LONG).show();
                return;
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE);
                return;
            }
            bindFreeformService();
        } catch (Exception e) {
            Toast.makeText(this, "Shizuku belum siap: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void bindFreeformService() {
        if (freeformService != null && freeformService.isBinderAlive()) {
            Toast.makeText(this, "Shizuku sudah terhubung", Toast.LENGTH_SHORT).show();
            if (pendingComponent != null) {
                String c = pendingComponent; pendingComponent = null; launchThroughShizuku(c);
            }
            return;
        }
        if (bindingShizuku) return;
        try {
            bindingShizuku = true;
            Shizuku.bindUserService(serviceArgs(), freeformConnection);
        } catch (Exception e) {
            bindingShizuku = false;
            Toast.makeText(this, "Gagal menghubungkan Shizuku: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    private void openFreeform(String component) {
        pendingComponent = component;
        ensureShizuku();
    }
    private void launchThroughShizuku(String component) {
        IBinder service = freeformService;
        if (service == null || !service.isBinderAlive()) {
            pendingComponent = component;
            ensureShizuku();
            return;
        }
        new Thread(() -> {
            Parcel data = Parcel.obtain();
            Parcel reply = Parcel.obtain();
            try {
                data.writeString(component);
                boolean received = service.transact(FloatingUserService.CODE_LAUNCH, data, reply, 0);
                if (!received) throw new IllegalStateException("UserService tidak menerima perintah");
                reply.readException();
                int exit = reply.readInt();
                String output = reply.readString();
                runOnUiThread(() -> Toast.makeText(this,
                    exit == 0 ? "Freeform dijalankan: " + component : "Freeform gagal (" + exit + "): " + output,
                    Toast.LENGTH_LONG).show());
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(this, "Kesalahan Shizuku: " + e.getMessage(), Toast.LENGTH_LONG).show());
            } finally {
                data.recycle(); reply.recycle();
            }
        }, "launch-freeform").start();
    }
    @Override public void onCreate(Bundle b) {
        super.onCreate(b);
        Shizuku.addRequestPermissionResultListener(permissionListener);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING);
        surface = new Desktop(this, Display.DEFAULT_DISPLAY);
        setContentView(surface.root);
        displayManager = (DisplayManager) getSystemService(DISPLAY_SERVICE);
        displayManager.registerDisplayListener(listener, null);
        refreshExternal();
    }
    @Override protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        if (bindingShizuku || freeformService != null) {
            try { Shizuku.unbindUserService(serviceArgs(), freeformConnection, false); } catch (Exception ignored) { }
        }
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
        final java.util.Map<View, TextView> restoreButtons = new java.util.HashMap<>();
        Desktop(Context c, int id) {
            displayId = id;
            root.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                    new int[]{Color.rgb(10, 24, 53), Color.rgb(23, 80, 121)}));
            root.addView(desktop, new FrameLayout.LayoutParams(-1,-1));
            LinearLayout shortcuts = new LinearLayout(MainActivity.this);
            shortcuts.setOrientation(LinearLayout.VERTICAL);
            shortcuts.setPadding(dp(14), dp(28), 0, 0);
            addShortcut(shortcuts, "Files", () -> launchPackage("com.google.android.documentsui", "File Manager"));
            addShortcut(shortcuts, "Notes", () -> openWindow("Notes", ""));
            addShortcut(shortcuts, "Chrome", () -> launchPackage("com.android.chrome", "Chrome"));
            addShortcut(shortcuts, "Shizuku", () -> ensureShizuku());
            addShortcut(shortcuts, "Apps", () -> openApps());
            addShortcut(shortcuts, "About", () -> openWindow("Pova Desktop v0.2", "Mode landscape otomatis.\n\nJendela internal bisa digeser dan diperbesar.\n\nAplikasi lain dicoba dengan launch bounds, tetapi HiOS dapat membuka fullscreen.\n\nMode freeform yang berhasil dengan ADB tidak otomatis memberi izin yang sama kepada aplikasi Android biasa."));
            FrameLayout.LayoutParams sc = new FrameLayout.LayoutParams(dp(116),-2,Gravity.TOP|Gravity.LEFT);
            desktop.addView(shortcuts, sc);
            bar.setGravity(Gravity.CENTER_VERTICAL);
            bar.setPadding(dp(8),dp(5),dp(8),dp(5));
            bar.setBackgroundColor(Color.rgb(12,26,50));
            TextView start = button("⊞  Start", () -> openApps());
            bar.addView(start, new LinearLayout.LayoutParams(dp(108),-1));
            TextView chrome = button("Chrome", () -> launchPackage("com.android.chrome", "Chrome"));
            LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(dp(93),-1); cp.leftMargin=dp(6); bar.addView(chrome,cp);
            TextView whatsapp = button("WhatsApp", () -> launchPackage("com.whatsapp", "WhatsApp"));
            LinearLayout.LayoutParams wp = new LinearLayout.LayoutParams(dp(105),-1); wp.leftMargin=dp(6); bar.addView(whatsapp,wp);
            TextView notes = button("Notes", () -> openWindow("Notes", ""));
            LinearLayout.LayoutParams np = new LinearLayout.LayoutParams(dp(80),-1); np.leftMargin=dp(6); bar.addView(notes,np);
            TextView browser = button("Web", () -> openBrowser());
            LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(dp(65),-1); bp.leftMargin=dp(6); bar.addView(browser,bp);
            monitorStatus = text("Desktop • v0.2", 11, false);
            monitorStatus.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
            bar.addView(monitorStatus,new LinearLayout.LayoutParams(0,-1,1));
            FrameLayout.LayoutParams task = new FrameLayout.LayoutParams(-1,dp(54),Gravity.BOTTOM);
            root.addView(bar,task);
            root.post(() -> openWindow("Welcome to Pova Desktop", "Pova Desktop v0.2 • Landscape\n\nSTART: tampilkan daftar aplikasi.\nChrome / WhatsApp: coba jalankan dalam jendela kecil.\nNotes: contoh jendela yang bisa digeser dan diubah ukurannya.\n\nPENTING: Dukungan floating dari ADB tidak menjamin aplikasi launcher mendapat hak windowingMode 5. Jika Chrome atau WhatsApp tampil fullscreen, itu batasan sistem HiOS, bukan kegagalan instalasi.\n\nMonitor eksternal memerlukan display yang dikenali Android."));
        }
        void updateMonitorStatus(boolean attached) {
            if (monitorStatus != null) monitorStatus.setText(attached ? "External display • v0.2" : "Desktop • v0.2");
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
        void launchPackage(String pkg, String label) {
            Intent launch = getPackageManager().getLaunchIntentForPackage(pkg);
            if (launch == null || launch.getComponent() == null) {
                Toast.makeText(MainActivity.this, label + " tidak tersedia", Toast.LENGTH_LONG).show();
                return;
            }
            openFreeform(launch.getComponent().flattenToString());
        }
        void launchApp(ResolveInfo app) {
            if (app.activityInfo == null) return;
            openFreeform(new ComponentName(app.activityInfo.packageName, app.activityInfo.name).flattenToString());
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
            TextView hide = button("_", () -> { window.setVisibility(View.GONE); TextView restore = button("Restore " + title, () -> { window.setVisibility(View.VISIBLE); window.bringToFront(); bar.removeView(restoreButtons.get(window)); restoreButtons.remove(window); }); restoreButtons.put(window, restore); bar.addView(restore, new LinearLayout.LayoutParams(dp(115), -1)); });
            header.addView(hide,new LinearLayout.LayoutParams(dp(38),dp(40)));
            TextView close = button("X", () -> { desktop.removeView(window); TextView rb = restoreButtons.remove(window); if(rb != null) bar.removeView(rb); });
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
