package net.dorokhov.pony3.core.library.service;

import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.common.primitives.Ints;
import net.dorokhov.pony3.api.library.domain.FileType;
import net.dorokhov.pony3.api.library.domain.ReadableAudioData;
import net.dorokhov.pony3.api.library.domain.WritableAudioData;
import net.dorokhov.pony3.core.library.service.file.FileTypeResolver;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.images.Artwork;
import org.jaudiotagger.tag.images.StandardArtwork;
import org.jaudiotagger.tag.reference.GenreTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.Nullable;
import java.io.File;
import java.io.IOException;

@Component
public class AudioTagger {

    private static final String MP3_MIME_TYPE = "audio/mpeg";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final FileTypeResolver fileTypeResolver;

    public AudioTagger(FileTypeResolver fileTypeResolver) {
        this.fileTypeResolver = fileTypeResolver;
    }

    public ReadableAudioData read(File file) throws IOException {
        try {
            return doRead(file);
        } catch (Exception e) {
            throw new IOException("Could not read audio data.", e);
        }
    }

    public ReadableAudioData write(File file, WritableAudioData data) throws IOException {
        try {
            return doWrite(file, data);
        } catch (Exception e) {
            throw new IOException("Could not write audio data.", e);
        }
    }

    private ReadableAudioData doRead(File file) throws Exception {
        FileType fileType = fileTypeResolver.resolve(file);
        if (MP3_MIME_TYPE.equals(fileType.getMimeType())) {
            return readMp3(file, fileType);
        } else {
            throw new IOException(String.format("File type '%s' not supported for reading: '%s'.", fileType.getMimeType(), file.getAbsolutePath()));
        }
    }
    
    private ReadableAudioData doWrite(File file, WritableAudioData data) throws Exception {
        FileType fileType = fileTypeResolver.resolve(file);
        if (MP3_MIME_TYPE.equals(fileType.getMimeType())) {
            return writeMp3(file, fileType, data);
        } else {
            throw new IOException(String.format("File type '%s' not supported for writing: '%s'.", fileType.getMimeType(), file.getAbsolutePath()));
        }
    }

    private ReadableAudioData readMp3(File file, FileType fileType) throws Exception {
        logger.debug("Reading MP3 file: '{}'.", file.getAbsolutePath());
        return tagToAudioData(AudioFileIO.read(file), fileType);
    }
    
    private ReadableAudioData writeMp3(File file, FileType fileType, WritableAudioData data) throws Exception {

        logger.debug("Opening MP3 file for writing: '{}'.", file.getAbsolutePath());
        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTagOrCreateDefault();
        audioFile.setTag(tag);
        
        if (data.isWriteDiscNumber()) {
            Integer value = data.getDiscNumber();
            if (value != null) {
                logger.debug("Updating disc number '{}' in file '{}'.", value, file.getAbsolutePath());
                tag.setField(FieldKey.DISC_NO, value.toString());
            } else {
                logger.debug("Deleting disc number in file '{}'.", file.getAbsolutePath());
                tag.deleteField(FieldKey.DISC_NO);
            }
        }

        if (data.isWriteDiscCount()) {
            Integer value = data.getDiscCount();
            if (value != null) {
                logger.debug("Updating disc count '{}' in file '{}'.", value, file.getAbsolutePath());
                tag.setField(FieldKey.DISC_TOTAL, value.toString());
            } else {
                logger.debug("Deleting disc count in file '{}'.", file.getAbsolutePath());
                tag.deleteField(FieldKey.DISC_TOTAL);
            }
        }

        if (data.isWriteTrackNumber()) {
            Integer value = data.getTrackNumber();
            if (value != null) {
                logger.debug("Updating track number '{}' in file '{}'.", value, file.getAbsolutePath());
                tag.setField(FieldKey.TRACK, value.toString());
            } else {
                logger.debug("Deleting track number in file '{}'.", file.getAbsolutePath());
                tag.deleteField(FieldKey.TRACK);
            }
        }

        if (data.isWriteTrackCount()) {
            Integer value = data.getTrackCount();
            if (value != null) {
                logger.debug("Updating track count '{}' in file '{}'.", value, file.getAbsolutePath());
                tag.setField(FieldKey.TRACK_TOTAL, value.toString());
            } else {
                logger.debug("Deleting track count in file '{}'.", file.getAbsolutePath());
                tag.deleteField(FieldKey.TRACK_TOTAL);
            }
        }

        if (data.isWriteTitle()) {
            String value = data.getTitle();
            if (value != null) {
                logger.debug("Updating title '{}' in file '{}'.", value, file.getAbsolutePath());
                tag.setField(FieldKey.TITLE, value);
            } else {
                logger.debug("Deleting title in file '{}'.", file.getAbsolutePath());
                tag.deleteField(FieldKey.TITLE);
            }
        }

        if (data.isWriteArtist()) {
            String value = data.getArtist();
            if (value != null) {
                logger.debug("Updating artist '{}' in file '{}'.", value, file.getAbsolutePath());
                tag.setField(FieldKey.ARTIST, value);
            } else {
                logger.debug("Deleting artist in file '{}'.", file.getAbsolutePath());
                tag.deleteField(FieldKey.ARTIST);
            }
        }

        if (data.isWriteAlbumArtist()) {
            String value = data.getAlbumArtist();
            if (value != null) {
                logger.debug("Updating album artist '{}' in file '{}'.", value, file.getAbsolutePath());
                tag.setField(FieldKey.ALBUM_ARTIST, value);
            } else {
                logger.debug("Deleting album artist in file '{}'.", file.getAbsolutePath());
                tag.deleteField(FieldKey.ALBUM_ARTIST);
            }
        }

        if (data.isWriteAlbum()) {
            String value = data.getAlbum();
            if (value != null) {
                logger.debug("Updating album '{}' in file '{}'.", value, file.getAbsolutePath());
                tag.setField(FieldKey.ALBUM, value);
            } else {
                logger.debug("Deleting album in file '{}'.", file.getAbsolutePath());
                tag.deleteField(FieldKey.ALBUM);
            }
        }

        if (data.isWriteYear()) {
            Integer value = data.getYear();
            if (value != null) {
                logger.debug("Updating year '{}' in file '{}'.", value, file.getAbsolutePath());
                tag.setField(FieldKey.YEAR, value.toString());
            } else {
                logger.debug("Deleting year in file '{}'.", file.getAbsolutePath());
                tag.deleteField(FieldKey.YEAR);
            }
        }

        if (data.isWriteGenre()) {
            String value = data.getGenre();
            if (value != null) {
                logger.debug("Updating genre '{}' in file '{}'.", value, file.getAbsolutePath());
                tag.setField(FieldKey.GENRE, value);
            } else {
                logger.debug("Deleting genre in file '{}'.", file.getAbsolutePath());
                tag.deleteField(FieldKey.GENRE);
            }
        }

        if (data.isWriteArtwork()) {
            File value = data.getArtworkFile();
            if (value != null) {
                logger.debug("Updating artwork in file '{}'.", file.getAbsolutePath());
                tag.deleteArtworkField();
                tag.setField(StandardArtwork.createArtworkFromFile(value));
            } else {
                logger.debug("Deleting artwork in file '{}'.", file.getAbsolutePath());
                tag.deleteArtworkField();
            }
        }

        logger.debug("Writing MP3 file: '{}'.", file.getAbsolutePath());
        AudioFileIO.write(audioFile);
        return tagToAudioData(audioFile, fileType);
    }

    private ReadableAudioData tagToAudioData(AudioFile audioFile, FileType fileType) {
        AudioHeader audioHeader = audioFile.getAudioHeader();
        return new ReadableAudioData()
                .setPath(audioFile.getFile().getAbsolutePath())
                .setFileType(fileType)
                .setSize(audioFile.getFile().length())
                .setDuration(audioHeader.getTrackLength())
                .setBitRate(audioHeader.getBitRateAsNumber())
                .setBitRateVariable(audioHeader.isVariableBitRate())
                .setDiscNumber(parseInteger(audioFile, FieldKey.DISC_NO))
                .setDiscCount(parseInteger(audioFile, FieldKey.DISC_TOTAL))
                .setTrackNumber(parseInteger(audioFile, FieldKey.TRACK))
                .setTrackCount(parseInteger(audioFile, FieldKey.TRACK_TOTAL))
                .setTitle(parseString(audioFile, FieldKey.TITLE))
                .setArtist(parseString(audioFile, FieldKey.ARTIST))
                .setAlbumArtist(parseString(audioFile, FieldKey.ALBUM_ARTIST))
                .setAlbum(parseString(audioFile, FieldKey.ALBUM))
                .setYear(parseYear(audioFile))
                .setGenre(parseGenre(audioFile))
                .setEmbeddedArtwork(parseArtwork(audioFile));
    }

    private Integer parseYear(AudioFile audioFile) {
        String stringValue = getFirstFromTagByKey(audioFile, FieldKey.YEAR);
        if (stringValue != null) {
            String[] numericParts = stringValue.split("[^\\d]");
            if (numericParts.length > 0) {
                Integer intValue = Ints.tryParse(numericParts[0]);
                if (intValue != null && intValue > 0) {
                    return intValue;
                }
            }
        }
        return null;
    }

    private String parseString(AudioFile audioFile, FieldKey key) {
        return Strings.emptyToNull(getFirstFromTagByKey(audioFile, key));
    }
    
    @Nullable
    private String getFirstFromTagByKey(AudioFile audioFile, FieldKey key) {
        Tag tag = audioFile.getTag();
        if (tag != null) {
            try {
                String value = tag.getFirst(key);
                return value != null ? value.trim() : null;
            } catch (Exception e) {
                logger.debug("Could not fetch tag '{}' from file '{}'.", key, audioFile.getFile().getAbsolutePath(), e);
            }
        }
        return null;
    }

    private Integer parseInteger(AudioFile audioFile, FieldKey key) {
        String stringValue = getFirstFromTagByKey(audioFile, key);
        if (stringValue != null) {
            Integer intValue = Ints.tryParse(stringValue);
            if (intValue != null && intValue > 0) {
                return intValue;
            }
        }
        return null;
    }

    private String parseGenre(AudioFile audioFile) {
        Integer type = parseInteger(audioFile, FieldKey.GENRE);
        if (type != null) {
            String value = GenreTypes.getInstanceOf().getValueForId(type);
            return value != null ? value : parseString(audioFile, FieldKey.GENRE);
        } else {
            return parseString(audioFile, FieldKey.GENRE);
        }
    }

    private ReadableAudioData.EmbeddedArtwork parseArtwork(AudioFile audioFile) {
        Tag tag = audioFile.getTag();
        if (tag != null) {
            Artwork artwork = tag.getFirstArtwork();
            if (artwork != null && artwork.getBinaryData() != null) {
                FileType type = fileTypeResolver.resolve(artwork.getBinaryData());
                if (type.isImage()) {
                    return new ReadableAudioData.EmbeddedArtwork(ByteSource.wrap(artwork.getBinaryData()), type);
                } else {
                    logger.debug("Artwork is not an image: '{}'.", type);
                }
            }
        }
        return null;
    }
}
