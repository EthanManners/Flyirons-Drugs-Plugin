package com.drugs.addiction;

public final class AddictionTickTask implements Runnable {

    @Override
    public void run() {
        AddictionManager.runHeartbeat();
    }
}
