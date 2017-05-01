package net.dorokhov.pony.library.service.impl.image.domain;

import com.google.common.base.Objects;

import java.io.Serializable;

public class ImageSize implements Serializable {
    
    private final int width;
    private final int height;

    public ImageSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImageSize that = (ImageSize) obj;
        return Objects.equal(this.width, that.width) &&
                Objects.equal(this.height, that.height);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(width, height);
    }

    @Override
    public String toString() {
        return "ImageSize{" +
                "width=" + width +
                ", height=" + height +
                '}';
    }
    
    public static ImageSize of(int width, int height) {
        return new ImageSize(width, height);
    }
}
