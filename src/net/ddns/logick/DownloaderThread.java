package net.ddns.logick;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.util.ListIterator;
import java.util.concurrent.Future;

public class DownloaderThread extends Thread {
    private static final Object lock = new Object();
    final int number;
    final BufferedImage[] images;
    volatile ListIterator<String> imagesLocationsIterator;

    public DownloaderThread(int number, ListIterator<String> imagesLocationsIterator, BufferedImage[] images) {
        this.number = number;
        this.imagesLocationsIterator = imagesLocationsIterator;
        this.images = images;
    }

    @Override
    public void run() {
        int next;
        String s;
        while (true) {
            synchronized (lock) {
                if (imagesLocationsIterator.hasNext()) {
                    next = imagesLocationsIterator.nextIndex();
                    s = imagesLocationsIterator.next();
                } else {
                    return;
                }
            }
            if (Thread.interrupted()) {
                return;
            }
            try {
                Thread.sleep(Main.REQUEST_COOLDOWN);
            } catch (InterruptedException e) {
                return;
            }
            BufferedImage bi = null;
            int retries = 0;
            while (retries < Main.MAX_RETRIES && bi == null)
                try {
                    HttpGet httpGet = new HttpGet(s);
                    Future<HttpResponse> response = ParseManager.httpclient.execute(httpGet, null);
                    bi = ImageIO.read(response.get().getEntity().getContent());
                } catch (Exception e) {
                    retries++;
                    try {
                        if (retries == 1 && !ParseManager.isConnectedToNet(URI.create(s).getHost())) {
                            JOptionPane.showMessageDialog(null, Language.get("message.no_connection"));
                            Main.LOG.error(Language.get("message.no_connection"));
                            return;
                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    e.printStackTrace();
                    Main.LOG.error(String.format(Language.get("message.page_getting_error"), e.getMessage(), s, retries, Main.MAX_RETRIES));
                }
            images[next] = bi;
            Main.LOG.info(String.format(Language.get("message.image_downloaded"), next + 1, images.length, number + 1));
            Main.increaseAndUpdateSecondaryProgressBarState(String.format(Language.get("message.image_downloaded"), next + 1, images.length, number + 1));
        }
    }
}
