package org.telegram.telegrambots.meta.api.objects;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.telegram.telegrambots.meta.api.interfaces.BotApiObject;

/**
 * @author Ruben Bermudez
 * @version 4.7
 * This object represents a dice with random value from 1 to 6.
 * (Yes, we're aware of the “proper” singular of die. But it's awkward, and we decided to help it change. One dice at a time!)
 */
public class Dice implements BotApiObject {
    private static final String VALUE_FIELD = "value";

    @JsonProperty(VALUE_FIELD)
    private Integer value; ///< Value of the dice, 1-6

    public Dice() {
        super();
    }

    public Integer getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "Dice{" +
                "value=" + value +
                '}';
    }
}
