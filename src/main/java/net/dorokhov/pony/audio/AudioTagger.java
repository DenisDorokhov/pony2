package net.dorokhov.pony.audio;

import com.google.common.base.Strings;
import com.google.common.io.ByteSource;
import com.google.common.primitives.Ints;
import net.dorokhov.pony.audio.domain.ReadableAudioData;
import net.dorokhov.pony.audio.domain.WritableAudioData;
import net.dorokhov.pony.file.ChecksumCalculator;
import net.dorokhov.pony.file.domain.FileType;
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

import static net.dorokhov.pony.util.RethrowingLambdas.rethrow;

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
        return readMp3(AudioFileIO.read(file), fileType);
    }
    
    private ReadableAudioData readMp3(AudioFile audioFile, FileType fileType) throws Exception {

        AudioHeader audioHeader = audioFile.getAudioHeader();
        Tag tag = audioFile.getTag();

        ReadableAudioData.Builder builder = ReadableAudioData.builder();

        builder.path(audioFile.getFile().getAbsolutePath())
                .fileType(fileType)
                .size(audioFile.getFile().length())
                .duration(audioHeader.getTrackLength())
                .bitRate(audioHeader.getBitRateAsNumber())
                .bitRateVariable(audioHeader.isVariableBitRate())
                .discNumber(parseInteger(tag, FieldKey.DISC_NO)).discCount(parseInteger(tag, FieldKey.DISC_TOTAL))
                .trackNumber(parseInteger(tag, FieldKey.TRACK)).trackCount(parseInteger(tag, FieldKey.TRACK_TOTAL))
                .title(parseString(tag, FieldKey.TITLE))
                .artist(parseString(tag, FieldKey.ARTIST))
                .albumArtist(parseString(tag, FieldKey.ALBUM_ARTIST))
                .album(parseString(tag, FieldKey.ALBUM))
                .year(parseInteger(tag, FieldKey.YEAR))
                .genre(parseGenre(tag));

        parseArtwork(tag).ifPresent(builder::embeddedArtwork);

        return builder.build();
    }
    
    private ReadableAudioData writeMp3(File file, FileType fileType, WritableAudioData data) throws Exception {

        AudioFile audioFile = AudioFileIO.read(file);
        Tag tag = audioFile.getTagOrCreateDefault();
        audioFile.setTag(tag);
        
        data.ifShouldUpdateDiscNumber(rethrow(value -> {
            log.debug("Updating disc number '{}' in file '{}'.", value, file.getAbsolutePath());
            tag.setField(FieldKey.DISC_NO, value.toString());
        }));
        data.ifShouldDeleteDiscNumber(rethrow(() -> {
            log.debug("Deleting disc number in file '{}'.", file.getAbsolutePath());
            tag.deleteField(FieldKey.DISC_NO);
        }));

        data.ifShouldUpdateDiscCount(rethrow(value -> {
            log.debug("Updating disc count '{}' in file '{}'.", value, file.getAbsolutePath());
            tag.setField(FieldKey.DISC_TOTAL, value.toString());
        }));
        data.ifShouldDeleteDiscCount(rethrow(() -> {
            log.debug("Deleting disc count in file '{}'.", file.getAbsolutePath());
            tag.deleteField(FieldKey.DISC_TOTAL);
        }));

        data.ifShouldUpdateTrackNumber(rethrow(value -> {
            log.debug("Updating track number '{}' in file '{}'.", value, file.getAbsolutePath());
            tag.setField(FieldKey.TRACK, value.toString());
        }));
        data.ifShouldDeleteTrackNumber(rethrow(() -> {
            log.debug("Deleting track number in file '{}'.", file.getAbsolutePath());
            tag.deleteField(FieldKey.TRACK);
        }));

        data.ifShouldUpdateTrackCount(rethrow(value -> {
            log.debug("Updating track count '{}' in file '{}'.", value, file.getAbsolutePath());
            tag.setField(FieldKey.TRACK_TOTAL, value.toString());
        }));
        data.ifShouldDeleteTrackCount(rethrow(() -> {
            log.debug("Deleting track count in file '{}'.", file.getAbsolutePath());
            tag.deleteField(FieldKey.TRACK_TOTAL);
        }));

        data.ifShouldUpdateTitle(rethrow(value -> {
            log.debug("Updating title '{}' in file '{}'.", value, file.getAbsolutePath());
            tag.setField(FieldKey.TITLE, value);
        }));
        data.ifShouldDeleteTitle(rethrow(() -> {
            log.debug("Deleting title in file '{}'.", file.getAbsolutePath());
            tag.deleteField(FieldKey.TITLE);
        }));

        data.ifShouldUpdateArtist(rethrow(value -> {
            log.debug("Updating artist '{}' in file '{}'.", value, file.getAbsolutePath());
            tag.setField(FieldKey.ARTIST, value);
        }));
        data.ifShouldDeleteArtist(rethrow(() -> {
            log.debug("Deleting artist in file '{}'.", file.getAbsolutePath());
            tag.deleteField(FieldKey.ARTIST);
        }));

        data.ifShouldUpdateAlbumArtist(rethrow(value -> {
            log.debug("Updating album artist '{}' in file '{}'.", value, file.getAbsolutePath());
            tag.setField(FieldKey.ALBUM_ARTIST, value);
        }));
        data.ifShouldDeleteAlbumArtist(rethrow(() -> {
            log.debug("Delete album artist in file '{}'.", file.getAbsolutePath());
            tag.deleteField(FieldKey.ALBUM_ARTIST);
        }));

        data.ifShouldUpdateAlbum(rethrow(value -> {
            log.debug("Updating album '{}' in file '{}'.", value, file.getAbsolutePath());
            tag.setField(FieldKey.ALBUM, value);
        }));
        data.ifShouldDeleteAlbum(rethrow(() -> {
            log.debug("Deleting album in file '{}'.", file.getAbsolutePath());
            tag.deleteField(FieldKey.ALBUM);
        }));

        data.ifShouldUpdateYear(rethrow(value -> {
            log.debug("Updating year '{}' in file '{}'.", value, file.getAbsolutePath());
            tag.setField(FieldKey.YEAR, value.toString());
        }));
        data.ifShouldDeleteYear(rethrow(() -> {
            log.debug("Deleting year in file '{}'.", file.getAbsolutePath());
            tag.deleteField(FieldKey.YEAR);
        }));

        data.ifShouldUpdateGenre(rethrow(value -> {
            log.debug("Updating genre '{}' in file '{}'.", value, file.getAbsolutePath());
            tag.setField(FieldKey.GENRE, value);
        }));
        data.ifShouldDeleteGenre(rethrow(() -> {
            log.debug("Deleting genre in file '{}'.", file.getAbsolutePath());
            tag.deleteField(FieldKey.GENRE);
        }));

        data.ifShouldUpdateArtwork(rethrow(value -> {
            log.debug("Updating artwork in file '{}'.", file.getAbsolutePath());
            tag.deleteArtworkField();
            tag.setField(StandardArtwork.createArtworkFromFile(value));
        }));
        data.ifShouldDeleteArtwork(rethrow(() -> {
            log.debug("Deleting artwork in file '{}'.", file.getAbsolutePath());
            tag.deleteArtworkField();
        }));

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
    
    private Optional<ReadableAudioData.EmbeddedArtwork> parseArtwork(Tag tag) {
        if (tag != null) {
            Artwork artwork = tag.getFirstArtwork();
            if (artwork != null && artwork.getBinaryData() != null) {
                FileType type = fileTypeResolver.resolve(artwork.getBinaryData());
                if (type.isImage()) {
                    return Optional.of(new ReadableAudioData.EmbeddedArtwork(
                            ByteSource.wrap(artwork.getBinaryData()),
                            type, checksumCalculator.calculate(artwork.getBinaryData())));
                } else {
                    log.debug("Artwork is not an image: '{}'.", type);
                }
            }
        }
        return Optional.empty();
    }
}
