package vn.edu.usth.ircui.feature_user;

import net.engio.mbassy.listener.Handler;
import org.kitteh.irc.client.library.Client;
import org.kitteh.irc.client.library.event.client.ClientNegotiationCompleteEvent;
import org.kitteh.irc.client.library.event.client.ClientReceiveNumericEvent;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/** Connects with a temporary nick, probes ISON/WHOIS for target nick, reports result, then disconnects. */
public class NickAvailabilityChecker {

    public interface Callback { void onResult(boolean inUse, String message); }

    private final String host; private final int port; private final boolean tls;
    private volatile Client probeClient;
    private final android.os.Handler main = new android.os.Handler(android.os.Looper.getMainLooper());
    private final AtomicBoolean finished = new AtomicBoolean(false);

    public NickAvailabilityChecker(String host, int port, boolean tls) {
        this.host = host; this.port = port; this.tls = tls;
    }

    public void check(String targetNick, Callback cb) {
        String temp = "_chk" + (int)(Math.random()*9000 + 1000); // unlikely to collide

        new Thread(() -> {
            try {
                Client c = Client.builder()
                        .nick(temp)
                        .realName("Nick probe")
                        .server().host(host).port(port).then()
                        .build();

                c.getEventManager().registerEventListener(new Object() {

                    @Handler
                    public void onReady(ClientNegotiationCompleteEvent e) {
                        // Quick existence check per RFC1459: ISON lists nicks that are online (303). :contentReference[oaicite:3]{index=3}
                        c.sendRawLine("ISON " + targetNick);
                        // WHOIS is a solid fallback; missing nick returns 401 ERR_NOSUCHNICK. :contentReference[oaicite:4]{index=4}
                        c.sendRawLine("WHOIS " + targetNick);
                    }

                    @Handler
                    public void onNumeric(ClientReceiveNumericEvent e) {
                        if (done()) return;
                        int n = e.getNumeric();
                        List<String> params = e.getParameters();
                        switch (n) {
                            case 303: { // RPL_ISON
                                String last = params.isEmpty() ? "" : params.get(params.size()-1);
                                boolean inUse = Arrays.stream(last.trim().split("\\s+"))
                                        .anyMatch(s -> s.equalsIgnoreCase(targetNick));
                                if (finish(cb, inUse, inUse ? "Nick is in use (ISON)." : "Nick seems free (ISON).")) {
                                    c.shutdown("done");
                                }
                                break;
                            }
                            case 401: { // ERR_NOSUCHNICK from WHOIS
                                if (finish(cb, false, "Nick not found (WHOIS).")) c.shutdown("done");
                                break;
                            }
                            case 433: { // ERR_NICKNAMEINUSE (could be our temp nick, not target) :contentReference[oaicite:5]{index=5}
                                c.setNick(temp + "_"); // just avoid collision and continue probing
                                break;
                            }
                        }
                    }
                });

                probeClient = c;
                c.connect();

                // Timeout after 5s → conservative message; you may retry.
                main.postDelayed(() -> {
                    if (finish(cb, false, "Timed out; try again.")) safeShutdown();
                }, 5000);

            } catch (Exception ex) {
                if (finish(cb, false, "Probe failed: " + ex.getMessage())) safeShutdown();
            }
        }, "nick-probe").start();
    }

    private boolean finish(Callback cb, boolean inUse, String msg) {
        if (!finished.compareAndSet(false, true)) return false;
        main.post(() -> cb.onResult(inUse, msg));
        return true;
    }
    private boolean done() { return finished.get(); }
    private void safeShutdown() { try { if (probeClient != null) probeClient.shutdown("done"); } catch (Exception ignore) {} }
}
