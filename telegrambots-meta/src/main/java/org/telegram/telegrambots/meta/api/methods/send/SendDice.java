package org.telegram.telegrambots.meta.api.methods.send;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboard;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Ruben Bermudez
 * @version 4.7
 * Use this method to send an animated emoji that will display a random value. On success, the sent Message is returned.
 */
public class SendDice extends BotApiMethod<Message> {
    private static final List<String> VALIDEMOJIS = Collections.unmodifiableList(Arrays.asList("\uD83C\uDFB2", "\uD83C\uDFAF", "\uD83C\uDFC0"));

    public static final String PATH = "sendDice";

    private static final String CHATID_FIELD = "chat_id";
    private static final String EMOJI_FIELD = "emoji";
    private static final String DISABLENOTIFICATION_FIELD = "disable_notification";
    private static final String REPLYTOMESSAGEID_FIELD = "reply_to_message_id";
    private static final String REPLYMARKUP_FIELD = "reply_markup";

    @JsonProperty(CHATID_FIELD)
    private String chatId; ///< Unique identifier for the target chat or username of the target channel (in the format @channelusername)
    /**
     * Emoji on which the dice throw animation is based. Currently, must be one of “🎲”, “🎯”, or “🏀”.
     * Dice can have values 1-6 for “🎲” and “🎯”, and values 1-5 for “🏀”. Defauts to “🎲”
     */
    @JsonProperty(EMOJI_FIELD)
    private String emoji;
    @JsonProperty(DISABLENOTIFICATION_FIELD)
    private Boolean disableNotification; ///< Optional. Sends the message silently. Users will receive a notification with no sound.
    @JsonProperty(REPLYTOMESSAGEID_FIELD)
    private Integer replyToMessageId; ///< Optional. If the message is a reply, ID of the original message
    @JsonProperty(REPLYMARKUP_FIELD)
    private ReplyKeyboard replyMarkup; ///< Optional. JSON-serialized object for a custom reply keyboard

    public SendDice() {
        super();
    }

    public String getChatId() {
        return chatId;
    }

    public SendDice setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public SendDice setChatId(Long chatId) {
        this.chatId = chatId.toString();
        return this;
    }

    public Integer getReplyToMessageId() {
        return replyToMessageId;
    }

    public SendDice setReplyToMessageId(Integer replyToMessageId) {
        this.replyToMessageId = replyToMessageId;
        return this;
    }

    public ReplyKeyboard getReplyMarkup() {
        return replyMarkup;
    }

    public SendDice setReplyMarkup(ReplyKeyboard replyMarkup) {
        this.replyMarkup = replyMarkup;
        return this;
    }

    public Boolean getDisableNotification() {
        return disableNotification;
    }

    public SendDice enableNotification() {
        this.disableNotification = false;
        return this;
    }

    public SendDice disableNotification() {
        this.disableNotification = true;
        return this;
    }

    public String getEmoji() {
        return emoji;
    }

    public SendDice setEmoji(String emoji) {
        this.emoji = emoji;
        return this;
    }

    @Override
    public String getMethod() {
        return PATH;
    }

    @Override
    public Message deserializeResponse(String answer) throws TelegramApiRequestException {
        try {
            ApiResponse<Message> result = OBJECT_MAPPER.readValue(answer,
                    new TypeReference<ApiResponse<Message>>(){});
            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException("Error sending dice", result);
            }
        } catch (IOException e) {
            throw new TelegramApiRequestException("Unable to deserialize response", e);
        }
    }

    @Override
    public void validate() throws TelegramApiValidationException {
        if (chatId == null) {
            throw new TelegramApiValidationException("ChatId parameter can't be empty", this);
        }
        if (emoji != null && !VALIDEMOJIS.contains(emoji)) {
            throw new TelegramApiValidationException("Only \uD83C\uDFB2, \uD83C\uDFAF or \uD83C\uDFC0 are allowed in Emoji field ", this);
        }
        if (replyMarkup != null) {
            replyMarkup.validate();
        }
    }

    @Override
    public String toString() {
        return "SendDice{" +
                "chatId='" + chatId + '\'' +
                ", emoji='" + emoji + '\'' +
                ", disableNotification=" + disableNotification +
                ", replyToMessageId=" + replyToMessageId +
                ", replyMarkup=" + replyMarkup +
                '}';
    }
}
