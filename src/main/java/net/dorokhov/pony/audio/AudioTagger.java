package net.dorokhov.pony.audio;

import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.common.primitives.Ints;
import net.dorokhov.pony.file.ChecksumCalculator;
import net.dorokhov.pony.file.FileType;
import net.dorokhov.pony.file.FileTypeResolver;
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

import java.io.File;
import java.io.IOException;
import java.util.Optional;

@Component
public class AudioTagger {

    private static final String MP3_MIME_TYPE = "audio/mpeg";
    
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final FileTypeResolver fileTypeResolver;
    private final ChecksumCalculator checksumCalculator;

    public AudioTagger(FileTypeResolver fileTypeResolver, ChecksumCalculator checksumCalculator) {
        this.fileTypeResolver = fileTypeResolver;
        this.checksumCalculator = checksumCalculator;
    }

    public AudioDataReadable read(File file) throws IOException {
        try {
            return doRead(file);
        } catch (Exception e) {
            throw new IOException("Could not read audio data.", e);
        }
    }

    public AudioDataReadable write(File file, AudioDataWritable data) throws IOException {
        try {
            return doWrite(file, data);
        } catch (Exception e) {
            throw new IOException("Could not write audio data.", e);
        }
    }

    private AudioDataReadable doRead(File file) throws Exception {
        FileType fileType = fileTypeResolver.resolve(file);
        if (MP3_MIME_TYPE.equals(fileType.getMimeType())) {
            return readMp3(file, fileType);
        } else {
            throw new IOException(String.format("File type '%s' not supported for reading: '%s'.", fileType.getMimeType(), file.getAbsolutePath()));
        }
    }
    
    private AudioDataReadable doWrite(File file, AudioDataWritable data) throws Exception {
        FileType fileType = fileTypeResolver.resolve(file);
        if (MP3_MIME_TYPE.equals(fileType.getMimeType())) {
            return writeMp3(file, fileType, data);
        } else {
            throw new IOException(String.format("File type '%s' not supported for writing: '%s'.", fileType.getMimeType(), file.getAbsolutePath()));
        }
    }

    private AudioDataReadable readMp3(File file, FileType fileType) throws Exception {
        return readMp3(AudioFileIO.read(file), fileType);
    }
    
    private AudioDataReadable readMp3(AudioFile audioFile, FileType fileType) throws Exception {

        AudioHeader audioHeader = audioFile.getAudioHeader();
        Tag tag = audioFile.getTag();

        AudioDataReadable.Builder builder = new AudioDataReadable.Builder();

        builder.setPath(audioFile.getFile().getAbsolutePath())
                .setFileType(fileType)
                .setSize(audioFile.getFile().length())
                .setDuration(audioHeader.getTrackLength())
                .setBitRate(audioHeader.getBitRateAsNumber())
                .setBitRateVariable(audioHeader.isVariableBitRate())
                .setDiscNumber(parseInteger(tag, FieldKey.DISC_NO)).setDiscCount(parseInteger(tag, FieldKey.DISC_TOTAL))
                .setTrackNumber(parseInteger(tag, FieldKey.TRACK)).setTrackCount(parseInteger(tag, FieldKey.TRACK_TOTAL))
                .setTitle(parseString(tag, FieldKey.TITLE))
                .setArtist(parseString(tag, FieldKey.ARTIST))
                .setAlbumArtist(parseString(tag, FieldKey.ALBUM_ARTIST))
                .setAlbum(parseString(tag, FieldKey.ALBUM))
                .setYear(parseInteger(tag, FieldKey.YEAR))
                .setGenre(parseGenre(tag));

        parseArtwork(tag).ifPresent(builder::setEmbeddedArtwork);

        return builder.build();
    }
    
    private AudioDataReadable writeMp3(File file, FileType fileType, AudioDataWritable data) throws Exception {

        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTagOrCreateDefault();
        audioFile.setTag(tag);
        
        if (data.isWriteDiscNumber()) {
            log.debug("Writing disc number '{}' to file '{}'.", data.getDiscNumber(), file.getAbsolutePath());
            setOrDeleteTagField(tag, FieldKey.DISC_NO, data.getDiscNumber());
        }
        if (data.isWriteDiscCount()) {
            log.debug("Writing disc count '{}' to file '{}'.", data.getDiscCount(), file.getAbsolutePath());
            setOrDeleteTagField(tag, FieldKey.DISC_TOTAL, data.getDiscCount());
        }

        if (data.isWriteTrackNumber()) {
            log.debug("Writing track number '{}' to file '{}'.", data.getTrackNumber(), file.getAbsolutePath());
            setOrDeleteTagField(tag, FieldKey.TRACK, data.getTrackNumber());
        }
        if (data.isWriteTrackCount()) {
            log.debug("Writing track count '{}' to file '{}'.", data.getTrackCount(), file.getAbsolutePath());
            setOrDeleteTagField(tag, FieldKey.TRACK_TOTAL, data.getTrackCount());
        }

        if (data.isWriteTitle()) {
            log.debug("Writing title '{}' to file '{}'.", data.getTitle(), file.getAbsolutePath());
            setOrDeleteTagField(tag, FieldKey.TITLE, data.getTitle());
        }
        if (data.isWriteArtist()) {
            log.debug("Writing artist '{}' to file '{}'.", data.getArtist(), file.getAbsolutePath());
            setOrDeleteTagField(tag, FieldKey.ARTIST, data.getArtist());
        }
        if (data.isWriteAlbumArtist()) {
            log.debug("Writing album artist '{}' to file '{}'.", data.getAlbumArtist(), file.getAbsolutePath());
            setOrDeleteTagField(tag, FieldKey.ALBUM_ARTIST, data.getAlbumArtist());
        }
        if (data.isWriteAlbum()) {
            log.debug("Writing album '{}' to file '{}'.", data.getAlbum(), file.getAbsolutePath());
            setOrDeleteTagField(tag, FieldKey.ALBUM, data.getAlbum());
        }
        if (data.isWriteYear()) {
            log.debug("Writing year '{}' to file '{}'.", data.getYear(), file.getAbsolutePath());
            setOrDeleteTagField(tag, FieldKey.YEAR, data.getYear());
        }
        if (data.isWriteGenre()) {
            log.debug("Writing genre '{}' to file '{}'.", data.getGenre(), file.getAbsolutePath()); 
            setOrDeleteTagField(tag, FieldKey.GENRE, data.getGenre());
        }

        if (data.isWriteArtwork()) {
            tag.deleteArtworkField();
            data.getArtworkFile().ifPresent(artworkFile -> {
                try {
                    log.debug("Writing artwork to file '{}'.", file.getAbsolutePath());
                    tag.setField(StandardArtwork.createArtworkFromFile(artworkFile));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        AudioFileIO.write(audioFile);
        
        return readMp3(audioFile, fileType);
    }

    private String parseString(Tag tag, FieldKey key) {
        return tag != null ? Strings.emptyToNull(tag.getFirst(key).trim()) : null;
    }

    private Integer parseInteger(Tag tag, FieldKey key) {
        if (tag != null) {
            Integer value = Ints.tryParse(tag.getFirst(key).trim());
            if (value != null && value > 0) {
                return value;
            }
        }
        return null;
    }

    private String parseGenre(Tag tag) {
        if (tag != null) {
            Integer type = parseInteger(tag, FieldKey.GENRE);
            if (type != null) {
                String value = GenreTypes.getInstanceOf().getValueForId(type);
                return value != null ? value : parseString(tag, FieldKey.GENRE);
            } else {
                return parseString(tag, FieldKey.GENRE);
            }
        }
        return null;
    }
    
    private Optional<AudioDataReadable.EmbeddedArtwork> parseArtwork(Tag tag) {
        if (tag != null) {
            Artwork artwork = tag.getFirstArtwork();
            if (artwork != null && artwork.getBinaryData() != null) {
                FileType type = fileTypeResolver.resolve(artwork.getBinaryData());
                if (type.isImage()) {
                    return Optional.of(new AudioDataReadable.EmbeddedArtwork(
                            ByteSource.wrap(artwork.getBinaryData()),
                            type, checksumCalculator.calculate(artwork.getBinaryData())));
                } else {
                    log.debug("Artwork is not an image: '{}'.", type);
                }
            }
        }
        return Optional.empty();
    }

    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private void setOrDeleteTagField(Tag tag, FieldKey key, Optional<?> value) throws Exception {
        if (value.isPresent()) {
            tag.setField(key, value.get().toString());
        } else {
            tag.deleteField(key);
        }
    }
}
