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
                .setDiscNumber(parseIntegerTag(tag, FieldKey.DISC_NO)).setDiscCount(parseIntegerTag(tag, FieldKey.DISC_TOTAL))
                .setTrackNumber(parseIntegerTag(tag, FieldKey.TRACK)).setTrackCount(parseIntegerTag(tag, FieldKey.TRACK_TOTAL))
                .setTitle(parseStringTag(tag, FieldKey.TITLE))
                .setArtist(parseStringTag(tag, FieldKey.ARTIST))
                .setAlbumArtist(parseStringTag(tag, FieldKey.ALBUM_ARTIST))
                .setAlbum(parseStringTag(tag, FieldKey.ALBUM))
                .setYear(parseIntegerTag(tag, FieldKey.YEAR))
                .setGenre(parseStringTag(tag, FieldKey.GENRE));

        parseArtwork(tag).ifPresent(builder::setEmbeddedArtwork);

        return builder.build();
    }
    
    private AudioDataReadable writeMp3(File file, FileType fileType, AudioDataWritable data) throws Exception {

        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTag();

        if (data.isWriteDiscNumber()) {
            setOrDeleteTagField(tag, FieldKey.DISC_NO, data.getDiscNumber());
        }
        if (data.isWriteDiscCount()) {
            setOrDeleteTagField(tag, FieldKey.DISC_TOTAL, data.getDiscCount());
        }

        if (data.isWriteTrackNumber()) {
            setOrDeleteTagField(tag, FieldKey.TRACK, data.getTrackNumber());
        }
        if (data.isWriteTrackCount()) {
            setOrDeleteTagField(tag, FieldKey.TRACK_TOTAL, data.getTrackCount());
        }

        if (data.isWriteTitle()) {
            setOrDeleteTagField(tag, FieldKey.TITLE, data.getTitle());
        }
        if (data.isWriteArtist()) {
            setOrDeleteTagField(tag, FieldKey.ARTIST, data.getArtist());
        }
        if (data.isWriteAlbumArtist()) {
            setOrDeleteTagField(tag, FieldKey.ALBUM_ARTIST, data.getAlbumArtist());
        }
        if (data.isWriteAlbum()) {
            setOrDeleteTagField(tag, FieldKey.ALBUM, data.getAlbum());
        }
        if (data.isWriteYear()) {
            setOrDeleteTagField(tag, FieldKey.YEAR, data.getYear());
        }
        if (data.isWriteGenre()) {
            setOrDeleteTagField(tag, FieldKey.GENRE, data.getGenre());
        }

        if (data.isWriteArtwork()) {
            tag.deleteArtworkField();
            data.getArtworkFile().ifPresent(artworkFile -> {
                try {
                    StandardArtwork.createArtworkFromFile(artworkFile);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }

        AudioFileIO.write(audioFile);
        
        return readMp3(audioFile, fileType);
    }

    private String parseStringTag(Tag tag, FieldKey key) {
        return Strings.emptyToNull(tag.getFirst(key).trim());
    }

    private Integer parseIntegerTag(Tag tag, FieldKey key) {
        return Ints.tryParse(tag.getFirst(key).trim());
    }
    
    private Optional<AudioDataReadable.EmbeddedArtwork> parseArtwork(Tag tag) {
        Artwork artwork = tag.getFirstArtwork();
        if (artwork != null && artwork.getBinaryData() != null) {
            FileType type = fileTypeResolver.resolve(artwork.getBinaryData());
            if (type.isImage()) {
                return Optional.of(new AudioDataReadable.EmbeddedArtwork(
                        ByteSource.wrap(artwork.getBinaryData()),
                        type, checksumCalculator.calculate(artwork.getBinaryData())));
            } else {
                log.info("Artwork is not an image.");
            }
        }
        return Optional.empty();
    }

    private void setOrDeleteTagField(Tag tag, FieldKey key, Object value) throws Exception {
        if (value != null) {
            tag.setField(key, value.toString());
        } else {
            tag.deleteField(key);
        }
    }
}
