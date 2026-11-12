package id.unjani.povadesktop;

import android.os.Binder;
import android.os.Parcel;
import android.os.RemoteException;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

/** Runs with the Shizuku shell identity; no root required. */
public final class FloatingUserService extends Binder {
    public static final int CODE_LAUNCH = 1002;
    public FloatingUserService() { super(); }

    @Override protected boolean onTransact(int code, Parcel data, Parcel reply, int flags) throws RemoteException {
        if (code != CODE_LAUNCH) return super.onTransact(code, data, reply, flags);
        String component = data.readString();
        if (component == null || !component.matches("[A-Za-z0-9_.$]+/[A-Za-z0-9_.$]+")) {
            reply.writeNoException(); reply.writeInt(1); reply.writeString("Invalid component"); return true;
        }
        int status = 1;
        String output;
        try {
            // Explicit component, no shell concatenation or arbitrary command execution.
            Process p = new ProcessBuilder("/system/bin/am", "start", "--windowingMode", "5", "-n", component)
                    .redirectErrorStream(true).start();
            boolean finished = p.waitFor(10, TimeUnit.SECONDS);
            if (!finished) { p.destroyForcibly(); output = "Timeout starting app"; }
            else {
                status = p.exitValue();
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                try (InputStream in = p.getInputStream()) {
                    byte[] buf = new byte[1024]; int n;
                    while ((n = in.read(buf)) != -1 && out.size() < 8192) out.write(buf, 0, Math.min(n, 8192-out.size()));
                }
                output = out.toString("UTF-8");
            }
        } catch (Exception e) { output = e.toString(); }
        reply.writeNoException(); reply.writeInt(status); reply.writeString(output);
        return true;
    }
}
