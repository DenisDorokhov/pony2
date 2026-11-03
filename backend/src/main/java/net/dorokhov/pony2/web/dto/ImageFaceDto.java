package net.dorokhov.pony2.web.dto;

import net.dorokhov.pony2.api.library.domain.ImageFace;

public record ImageFaceDto(double xMin, double yMin, double xMax, double yMax) {
    public static ImageFaceDto of(ImageFace face) {
        return new ImageFaceDto(face.xMin(), face.yMin(), face.xMax(), face.yMax());
    }
}
