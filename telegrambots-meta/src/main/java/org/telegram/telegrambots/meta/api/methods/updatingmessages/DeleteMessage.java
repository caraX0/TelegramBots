package org.telegram.telegrambots.meta.api.methods.updatingmessages;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.objects.ApiResponse;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiValidationException;

import java.io.IOException;
import java.util.Objects;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Ruben Bermudez
 * @version 1.0
 *
 * Use this method to delete a message, including service messages, with the following limitations:
 * - A message can only be deleted if it was sent less than 48 hours ago.
 * - Bots can delete outgoing messages in private chats, groups, and supergroups.
 * - Bots can delete incoming messages in private chats.
 * - Bots granted can_post_messages permissions can delete outgoing messages in channels.
 * - If the bot is an administrator of a group, it can delete any message there.
 * - If the bot has can_delete_messages permission in a supergroup or a channel, it can delete any message there.
 * Returns True on success.
 */
public class DeleteMessage extends BotApiMethod<Boolean> {
    public static final String PATH = "deleteMessage";

    private static final String CHATID_FIELD = "chat_id";
    private static final String MESSAGEID_FIELD = "message_id";

    /**
     * Unique identifier for the chat to send the message to (Or username for channels)
     */
    @JsonProperty(CHATID_FIELD)
    private String chatId;
    /**
     * Identifier of the message to delete
     */
    @JsonProperty(MESSAGEID_FIELD)
    private Integer messageId;

    public DeleteMessage() {
        super();
    }

    public DeleteMessage(String chatId, Integer messageId) {
        this.chatId = checkNotNull(chatId);
        this.messageId = checkNotNull(messageId);
    }

    public DeleteMessage(Long chatId, Integer messageId) {
        this.chatId = checkNotNull(chatId).toString();
        this.messageId = checkNotNull(messageId);
    }

    public String getChatId() {
        return chatId;
    }

    public DeleteMessage setChatId(String chatId) {
        this.chatId = chatId;
        return this;
    }

    public DeleteMessage setChatId(Long chatId) {
        Objects.requireNonNull(chatId);
        this.chatId = chatId.toString();
        return this;
    }

    public Integer getMessageId() {
        return messageId;
    }

    public DeleteMessage setMessageId(Integer messageId) {
        this.messageId = messageId;
        return this;
    }

    @Override
    public String getMethod() {
        return PATH;
    }

    @Override
    public Boolean deserializeResponse(String answer) throws TelegramApiRequestException {
        try {
            ApiResponse<Boolean> result = OBJECT_MAPPER.readValue(answer,
                    new TypeReference<ApiResponse<Boolean>>() {
                    });
            if (result.getOk()) {
                return result.getResult();
            } else {
                throw new TelegramApiRequestException("Error deleting message", result);
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
        if (messageId == null) {
            throw new TelegramApiValidationException("MessageId parameter can't be empty", this);
        }
    }

    @Override
    public String toString() {
        return "DeleteMessage{" +
                "chatId='" + chatId + '\'' +
                ", messageId=" + messageId +
                '}';
    }
}
