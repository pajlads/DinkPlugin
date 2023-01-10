package dinkplugin.util;

import net.runelite.client.callback.ClientThread;

import java.util.function.BooleanSupplier;

public class BlockingClientThread extends ClientThread {
    @Override
    public void invoke(Runnable r) {
        r.run();
    }

    @Override
    public void invoke(BooleanSupplier r) {
        invokeSupplier(r);
    }

    @Override
    public void invokeLater(Runnable r) {
        r.run();
    }

    @Override
    public void invokeLater(BooleanSupplier r) {
        invokeSupplier(r);
    }

    @Override
    public void invokeAtTickEnd(Runnable r) {
        r.run();
    }

    private void invokeSupplier(BooleanSupplier r) {
        while (true) {
            if (r.getAsBoolean())
                break;
        }
    }
}
