package net.ddns.logick;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
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
                    if (retries == 1 && ParseManager.isDisconnectedFormNet(URI.create(s).getHost())) {
                        Main.LOG.error(Language.get("message.error.no_connection"));
                        ParseManager.cancel();
                        return;
                    }
                    e.printStackTrace();
                    Main.LOG.error(String.format(Language.get("message.error.page_getting"), s, retries + 1, Main.MAX_RETRIES));
                }
            images[next] = bi;
            Main.LOG.info(String.format(Language.get("message.info.image_downloaded"), next + 1, images.length, number + 1));
            Main.increaseAndUpdateSecondaryProgressBarState(String.format(Language.get("message.info.image_downloaded"), next + 1, images.length, number + 1));
        }
    }
}
