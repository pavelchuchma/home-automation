package org.chuma.homecontroller.controller.device;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.chuma.homecontroller.controller.actor.IReadableOnOff;

public class RefreshingSwitchIndicator extends SwitchIndicator {
    static Logger log = LoggerFactory.getLogger(SwitchIndicator.class.getName());
    Thread refreshThread;

    public RefreshingSwitchIndicator(SwitchIndicator baseIndicator, int periodMs) {
        super(baseIndicator.pin, baseIndicator.mode);
        refreshThread = new Thread(() -> {
            while (true) {
                try {
                    refresh();
                    Thread.sleep(periodMs);
                } catch (InterruptedException e) {
                    return;
                } catch (Exception e) {
                    log.error("Failed to refresh", e);
                }
            }
        });
    }

    public void startRefresh() {
        refreshThread.start();
    }

    private void refresh() {
        for (IReadableOnOff source : sources) {
            onAction(source, null);
        }
    }
}
