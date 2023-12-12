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
 * Use this method to add a new sticker to a set created by the bot.
 * You must use exactly one of the fields png_sticker or tgs_sticker.
 * Animated stickers can be added to animated sticker sets and only to them.
 * Animated sticker sets can have up to 50 stickers.
 * Static sticker sets can have up to 120 stickers.
 * Returns True on success.
 */
public class AddStickerToSet extends PartialBotApiMethod<Boolean> {
    public static final String PATH = "addStickerToSet";

    public static final String USERID_FIELD = "user_id";
    public static final String NAME_FIELD = "name";
    public static final String PNGSTICKER_FIELD = "png_sticker";
    public static final String TGSSTICKER_FIELD = "tgs_sticker";
    public static final String EMOJIS_FIELD = "emojis";
    public static final String MASKPOSITION_FIELD = "mask_position";

    private Integer userId; ///< User identifier of sticker set owner
    private String name; ///< Sticker set name
    private String emojis; ///< One or more emoji corresponding to the sticker
    private MaskPosition maskPosition; ///< Position where the mask should be placed on faces
    /**
     * Png image with the sticker, must be up to 512 kilobytes in size, dimensions must not exceed 512px,
     * and either width or height must be exactly 512px. Pass a file_id as a String to send a file
     * that already exists on the Telegram servers, pass an HTTP URL as a String for Telegram
     * to get a file from the Internet, or upload a new one using multipart/form-data.
     */
    private InputFile pngSticker;
    /**
     * TGS animation with the sticker, uploaded using multipart/form-data.
     * See https://core.telegram.org/animated_stickers#technical-requirements for technical requirements
     */
    private InputFile tgsSticker;

    public AddStickerToSet() {
        super();
    }

    public AddStickerToSet(Integer userId, String name, String emojis) {
        this.userId = checkNotNull(userId);
        this.name = checkNotNull(name);
        this.emojis = checkNotNull(emojis);
    }

    public Integer getUserId() {
        return userId;
    }

    public AddStickerToSet setUserId(Integer userId) {
        this.userId = userId;
        return this;
    }

    public InputFile getPngSticker() {
        return pngSticker;
    }

    public AddStickerToSet setPngSticker(String pngSticker) {
        this.pngSticker = new InputFile(pngSticker);
        return this;
    }

    public AddStickerToSet setPngSticker(File pngSticker) {
        Objects.requireNonNull(pngSticker, "pngSticker cannot be null!");
        this.pngSticker = new InputFile(pngSticker, pngSticker.getName());
        return this;
    }

    public AddStickerToSet setPngSticker(String pngStickerName, InputStream pngSticker) {
        Objects.requireNonNull(pngStickerName, "pngStickerName cannot be null!");
        Objects.requireNonNull(pngSticker, "pngSticker cannot be null!");
        this.pngSticker = new InputFile(pngSticker, pngStickerName);
        return this;
    }

    public AddStickerToSet setTgsSticker(File tgsSticker) {
        this.tgsSticker = new InputFile(checkNotNull(tgsSticker), tgsSticker.getName());
        return this;
    }

    public AddStickerToSet setTgsSticker(String tgsStickerName, InputStream tgsSticker) {
        this.tgsSticker = new InputFile(checkNotNull(tgsSticker), checkNotNull(tgsStickerName));
        return this;
    }

    public InputFile getTgsSticker() {
        return tgsSticker;
    }

    public String getName() {
        return name;
    }

    public AddStickerToSet setName(String name) {
        this.name = name;
        return this;
    }

    public String getEmojis() {
        return emojis;
    }

    public AddStickerToSet setEmojis(String emojis) {
        this.emojis = emojis;
        return this;
    }

    public MaskPosition getMaskPosition() {
        return maskPosition;
    }

    public AddStickerToSet setMaskPosition(MaskPosition maskPosition) {
        this.maskPosition = maskPosition;
        return this;
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
        return "AddStickerToSet{" +
                "userId=" + userId +
                ", name='" + name + '\'' +
                ", emojis='" + emojis + '\'' +
                ", maskPosition=" + maskPosition +
                ", pngSticker=" + pngSticker +
                ", tgsSticker=" + tgsSticker +
                '}';
    }
}
