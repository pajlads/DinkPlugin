package dinkplugin.util;

import lombok.experimental.UtilityClass;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

@UtilityClass
public class TestImageUtil {

    public Image getExample() {
        try (InputStream inputStream = TestImageUtil.class.getResourceAsStream("/screenshot.png")) {
            if (inputStream != null)
                return ImageIO.read(inputStream);
        } catch (Exception ignored) {
        }
        return random(960, 640);
    }

    public Image random(final int width, final int height) {
        Random rand = ThreadLocalRandom.current();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                image.setRGB(x, y, new Color(rand.nextFloat(), rand.nextFloat(), rand.nextFloat()).getRGB());
            }
        }
        return image;
    }

}
