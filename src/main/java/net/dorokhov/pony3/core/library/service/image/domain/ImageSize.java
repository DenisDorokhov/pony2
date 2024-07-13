package net.dorokhov.pony3.core.library.service.image.domain;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.annotation.Nullable;

public final class ImageSize {
    
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
    @SuppressFBWarnings("NP_METHOD_PARAMETER_TIGHTENS_ANNOTATION")
    public boolean equals(@Nullable Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImageSize that = (ImageSize) obj;
        return Objects.equal(width, that.width) &&
                Objects.equal(height, that.height);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(width, height);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("width", width)
                .add("height", height)
                .toString();
    }

    public static ImageSize of(int width, int height) {
        return new ImageSize(width, height);
    }
}
