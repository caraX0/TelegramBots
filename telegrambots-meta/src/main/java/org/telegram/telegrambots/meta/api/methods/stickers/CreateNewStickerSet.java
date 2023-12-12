package org.telegram.telegrambots.meta.api.methods.stickers;

import com.fasterxml.jackson.core.type.TypeReference;
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.stickers.MaskPosition;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Ruben Bermudez
 * @version 1.0
 * Use this method to create a new sticker set owned by a user. The bot will be able to edit the sticker set thus created.
 * You must use exactly one of the fields png_sticker or tgs_sticker.
 * Returns True on success.
 */
public class CreateNewStickerSet extends PartialBotApiMethod<Boolean> {
    public static final String PATH = "createNewStickerSet";

    public static final String USERID_FIELD = "user_id";
    public static final String NAME_FIELD = "name";
    public static final String TITLE_FIELD = "title";
    public static final String PNGSTICKER_FIELD = "png_sticker";
    public static final String TGSSTICKER_FIELD = "tgs_sticker";
    public static final String EMOJIS_FIELD = "emojis";
    public static final String CONTAINSMASKS_FIELD = "contains_masks";
    public static final String MASKPOSITION_FIELD = "mask_position";

    private Integer userId; ///< User identifier of created sticker set owner
    /**
     * Name of sticker set, to be used in t.me/addstickers/<name> URLs.
     * Can contain only english letters, digits and underscores.
     * Must begin with a letter, can't contain consecutive underscores and must end in “_by_<bot username>”.
     * <bot_username> is case insensitive. 7-64 characters.
     */
    private String name; ///< Sticker set title, 1-64 characters
    private String title; ///< User identifier of created sticker set owner
    private String emojis; ///< One or more emoji corresponding to the sticker
    private Boolean containsMasks; ///< Pass True, if a set of mask stickers should be created
    private MaskPosition maskPosition; ///< Position where the mask should be placed on faces
    /**
     * Png image with the sticker, must be up to 512 kilobytes in size, dimensions must not exceed 512px,
     * and either width or height must be exactly 512px.
     * Pass a file_id as a String to send a file that already exists on the Telegram servers,
     * pass an HTTP URL as a String for Telegram to get a file from the Internet, or upload a new one
     * using multipart/form-data. More info on Sending Files »
     */
    private InputFile pngSticker;

    /**
     * TGS animation with the sticker, uploaded using multipart/form-data.
     * See https://core.telegram.org/animated_stickers#technical-requirements for technical requirements
     */
    private InputFile tgsSticker;

    public CreateNewStickerSet() {
        super();
    }

    public CreateNewStickerSet(Integer userId, String name, String title, String emojis) {
        this.userId = checkNotNull(userId);
        this.name = checkNotNull(name);
        this.title = checkNotNull(title);
        this.emojis = checkNotNull(emojis);
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public InputFile getPngSticker() {
        return pngSticker;
    }

    public CreateNewStickerSet setPngSticker(String pngSticker) {
        this.pngSticker = new InputFile(pngSticker);
        return this;
    }

    public CreateNewStickerSet setPngStickerFile(InputFile pngStickerFile) {
        Objects.requireNonNull(pngStickerFile, "pngStickerFile cannot be null!");
        this.pngSticker = pngStickerFile;
        return this;
    }

    public CreateNewStickerSet setPngStickerFile(File pngStickerFile) {
        Objects.requireNonNull(pngStickerFile, "pngStickerFile cannot be null!");
        this.pngSticker = new InputFile(pngStickerFile, pngStickerFile.getName());
        return this;
    }

    public CreateNewStickerSet setPngStickerStream(String pngStickerName, InputStream pngStickerStream) {
        Objects.requireNonNull(pngStickerName, "pngStickerName cannot be null!");
        Objects.requireNonNull(pngStickerStream, "pngStickerStream cannot be null!");
        this.pngSticker = new InputFile(pngStickerStream, pngStickerName);
        return this;
    }

    public CreateNewStickerSet setTgsSticker(InputFile tgsSticker) {
        this.tgsSticker = checkNotNull(tgsSticker);
        return this;
    }

    public CreateNewStickerSet setTgsSticker(File tgsStickerFile) {
        this.tgsSticker =  new InputFile(checkNotNull(tgsStickerFile), tgsStickerFile.getName());;
        return this;
    }

    public CreateNewStickerSet setTgsSticker(String tgsStickerName, InputStream tgsStickerStream) {
        this.pngSticker = new InputFile(checkNotNull(tgsStickerStream), checkNotNull(tgsStickerName));
        return this;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getEmojis() {
        return emojis;
    }

    public void setEmojis(String emojis) {
        this.emojis = emojis;
    }

    public Boolean getContainsMasks() {
        return containsMasks;
    }

    public void setContainsMasks(Boolean containsMasks) {
        this.containsMasks = containsMasks;
    }

    public MaskPosition getMaskPosition() {
        return maskPosition;
    }

    public void setMaskPosition(MaskPosition maskPosition) {
        this.maskPosition = maskPosition;
    }

    public InputFile getTgsSticker() {
        return tgsSticker;
    }

    @Override
    public Boolean deserializeResponse(String answer) throws TelegramApiRequestException {
        try {
            ApiResponse<Boolean> result = OBJECT_MAPPER.readValue(answer,
                    new TypeReference<ApiResponse<Boolean>>(){});
            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException("Error creating new sticker set", result);
            }
        } catch (IOException e) {
            throw new TelegramApiRequestException("Unable to deserialize response", e);
        }
    }

    @Override
    public void validate() throws TelegramApiValidationException {
        if (userId == null || userId <= 0) {
            throw new TelegramApiValidationException("userId can't be empty", this);
        }
        if (name == null || name.isEmpty()) {
            throw new TelegramApiValidationException("name can't be empty", this);
        }
        if (title == null || title.isEmpty()) {
            throw new TelegramApiValidationException("title can't be empty", this);
        }
        if (emojis == null || emojis.isEmpty()) {
            throw new TelegramApiValidationException("emojis can't be empty", this);
        }

        if (pngSticker == null && tgsSticker == null) {
            throw new TelegramApiValidationException("One of pngSticker or tgsSticker is needed", this);
        }

        if (pngSticker != null && tgsSticker != null) {
            throw new TelegramApiValidationException("Only one of pngSticker or tgsSticker are allowed", this);
        }

        if (pngSticker != null) {
            pngSticker.validate();
        }
        if (tgsSticker != null) {
            tgsSticker.validate();
        }

        if (maskPosition != null) {
            maskPosition.validate();
        }
    }

    @Override
    public String toString() {
        return "CreateNewStickerSet{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", title='" + title + '\'' +
                ", emojis='" + emojis + '\'' +
                ", containsMasks=" + containsMasks +
                ", maskPosition=" + maskPosition +
                ", pngSticker=" + pngSticker +
                ", tgsSticker=" + tgsSticker +
                '}';
    }
}
