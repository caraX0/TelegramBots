package org.telegram.telegrambots.meta.api.objects.stickers;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * This object represents a sticker.
 */
public class Sticker implements BotApiObject {

    private static final String FILEID_FIELD = "file_id";
    private static final String FILEUNIQUEID_FIELD = "file_unique_id";
    private static final String WIDTH_FIELD = "width";
    private static final String HEIGHT_FIELD = "height";
    private static final String THUMB_FIELD = "thumb";
    private static final String FILESIZE_FIELD = "file_size";
    private static final String EMOJI_FIELD = "emoji";
    private static final String SETNAME_FIELD = "set_name";
    private static final String MASKPOSITON_FIELD = "mask_position";
    private static final String ISANIMATED_FIELD = "is_animated";

    @JsonProperty(FILEID_FIELD)
    private String fileId; ///< Identifier for this file, which can be used to download or reuse the file
    /**
     * Unique identifier for this file, which is supposed to be the same over time and for different bots.
     * Can't be used to download or reuse the file.
     */
    @JsonProperty(FILEUNIQUEID_FIELD)
    private String fileUniqueId;
    @JsonProperty(WIDTH_FIELD)
    private Integer width; ///< Sticker width
    @JsonProperty(HEIGHT_FIELD)
    private Integer height; ///< Sticker height
    @JsonProperty(THUMB_FIELD)
    private PhotoSize thumb; ///< Optional. Sticker thumbnail in .webp or .jpg format
    @JsonProperty(FILESIZE_FIELD)
    private Integer fileSize; ///< Optional. File size
    @JsonProperty(EMOJI_FIELD)
    private String emoji; ///< Optional. Emoji associated with the sticker
    @JsonProperty(SETNAME_FIELD)
    private String setName; ///< Optional. Name of the sticker set to which the sticker belongs
    @JsonProperty(MASKPOSITON_FIELD)
    private MaskPosition maskPosition; ///< Optional. For mask stickers, the position where the mask should be placed
    @JsonProperty(ISANIMATED_FIELD)
    private Boolean isAnimated; ///< True, if the sticker is animated

    public Sticker() {
        super();
    }

    public String getFileId() {
        return fileId;
    }

    public Integer getWidth() {
        return width;
    }

    public Integer getHeight() {
        return height;
    }

    public PhotoSize getThumb() {
        return thumb;
    }

    public Integer getFileSize() {
        return fileSize;
    }

    public String getEmoji() {
        return emoji;
    }

    public String getSetName() {
        return setName;
    }

    public MaskPosition getMaskPosition() {
        return maskPosition;
    }

    public Boolean getAnimated() {
        return isAnimated;
    }

    public String getFileUniqueId() {
        return fileUniqueId;
    }

    @Override
    public String toString() {
        return "Sticker{" +
                "fileId='" + fileId + '\'' +
                ", width=" + width +
                ", height=" + height +
                ", thumb=" + thumb +
                ", fileSize=" + fileSize +
                ", emoji='" + emoji + '\'' +
                ", setName='" + setName + '\'' +
                ", maskPosition=" + maskPosition +
                ", isAnimated=" + isAnimated +
                ", fileUniqueId=" + fileUniqueId +
                '}';
    }
}
