package org.telegram.abilitybots.api.bot;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.ImmutableMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.abilitybots.api.sender.DefaultSender;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.abilitybots.api.toggle.AbilityToggle;
import org.telegram.abilitybots.api.util.AbilityExtension;
import org.telegram.abilitybots.api.util.AbilityUtils;
import org.telegram.abilitybots.api.util.Pair;
import org.telegram.abilitybots.api.util.Trio;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static java.time.ZonedDateTime.now;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.compile;
import static org.telegram.abilitybots.api.objects.Locality.*;
import static org.telegram.abilitybots.api.objects.MessageContext.newContext;
import static org.telegram.abilitybots.api.objects.Privacy.*;
import static org.telegram.abilitybots.api.util.AbilityMessageCodes.*;
import static org.telegram.abilitybots.api.util.AbilityUtils.*;

/**
 * The <b>father</b> of all ability bots. Bots that need to utilize abilities need to extend this bot.
 * <p>
 * It's important to note that this bot strictly extends {@link TelegramLongPollingBot}.
 * <p>
 * All bots extending the {@link BaseAbilityBot} get implicit abilities:
 * <ul>
 * <li>/claim - Claims this bot</li>
 * <ul>
 * <li>Sets the user as the {@link Privacy#CREATOR} of the bot</li>
 * <li>Only the user with the ID returned by {@link BaseAbilityBot#creatorId()} can genuinely claim the bot</li>
 * </ul>
 * <li>/report - reports all user-defined commands (abilities)</li>
 * <ul>
 * <li>The same format acceptable by BotFather</li>
 * </ul>
 * <li>/commands - returns a list of all possible bot commands based on the privacy of the requesting user</li>
 * <li>/backup - returns a backup of the bot database</li>
 * <li>/recover - recovers the database</li>
 * <li>/promote <code>@username</code> - promotes user to bot admin</li>
 * <li>/demote <code>@username</code> - demotes bot admin to user</li>
 * <li>/ban <code>@username</code> - bans the user from accessing your bot commands and features</li>
 * <li>/unban <code>@username</code> - lifts the ban from the user</li>
 * </ul>
 * <p>
 * Additional information of the implicit abilities are present in the methods that declare them.
 * <p>
 * The two most important handles in the BaseAbilityBot are the {@link DBContext} <b><code>db</code></b> and the {@link MessageSender} <b><code>sender</code></b>.
 * All bots extending BaseAbilityBot can use both handles in their update consumers.
 *
 * @author Abbas Abou Daya
 */
@SuppressWarnings({"ConfusingArgumentToVarargsMethod", "UnusedReturnValue", "WeakerAccess", "unused", "ConstantConditions"})
public abstract class BaseAbilityBot extends DefaultAbsSender implements AbilityExtension {
    private static final Logger log = LogManager.getLogger(BaseAbilityBot.class);

    protected static final String DEFAULT = "default";
    // DB objects
    public static final String ADMINS = "ADMINS";
    public static final String USERS = "USERS";
    public static final String USER_ID = "USER_ID";
    public static final String BLACKLIST = "BLACKLIST";

    // DB and sender
    protected final DBContext db;
    protected MessageSender sender;
    protected SilentSender silent;

    // Ability toggle
    private final AbilityToggle toggle;

    // Bot token and username
    private final String botToken;
    private final String botUsername;

    // Ability registry
    private Map<String, Ability> abilities;

    // Reply registry
    private List<Reply> replies;

    public abstract int creatorId();

    protected BaseAbilityBot(String botToken, String botUsername, DBContext db, AbilityToggle toggle, DefaultBotOptions botOptions) {
        super(botOptions);

        this.botToken = botToken;
        this.botUsername = botUsername;
        this.db = db;
        this.toggle = toggle;
        this.sender = new DefaultSender(this);
        silent = new SilentSender(sender);

        registerAbilities();
    }

    /**
     * @return the map of <ID,User>
     */
    protected Map<Integer, User> users() {
        return db.getMap(USERS);
    }

    /**
     * @return the map of <Username,ID>
     */
    protected Map<String, Integer> userIds() {
        return db.getMap(USER_ID);
    }

    /**
     * @return a blacklist containing all the IDs of the banned users
     */
    protected Set<Integer> blacklist() {
        return db.getSet(BLACKLIST);
    }

    /**
     * @return an admin set of all the IDs of bot administrators
     */
    protected Set<Integer> admins() {
        return db.getSet(ADMINS);
    }

    /**
     * @return the immutable map of <String,Ability>
     */
    public Map<String, Ability> abilities() {
        return abilities;
    }

    /**
     * @return the immutable list carrying the embedded replies
     */
    public List<Reply> replies() {
        return replies;
    }

    /**
     * This method contains the stream of actions that are applied on any update.
     * <p>
     * It will correctly handle addition of users into the DB and the execution of abilities and replies.
     *
     * @param update the update received by Telegram's API
     */
    public void onUpdateReceived(Update update) {
        log.info(format("[%s] New update [%s] received at %s", botUsername, update.getUpdateId(), now()));
        log.info(update.toString());
        long millisStarted = System.currentTimeMillis();

        Stream.of(update)
                .filter(this::checkGlobalFlags)
                .filter(this::checkBlacklist)
                .map(this::addUser)
                .filter(this::filterReply)
                .map(this::getAbility)
                .filter(this::validateAbility)
                .filter(this::checkPrivacy)
                .filter(this::checkLocality)
                .filter(this::checkInput)
                .filter(this::checkMessageFlags)
                .map(this::getContext)
                .map(this::consumeUpdate)
                .forEach(this::postConsumption);

        long processingTime = System.currentTimeMillis() - millisStarted;
        log.info(format("[%s] Processing of update [%s] ended at %s%n---> Processing time: [%d ms] <---%n", botUsername, update.getUpdateId(), now(), processingTime));
    }

    public String getBotToken() {
        return botToken;
    }

    public String getBotUsername() {
        return botUsername;
    }

    /**
     * Test the update against the provided global flags. The default implementation is a passthrough to all updates.
     * <p>
     * This method should be <b>overridden</b> if the user wants to restrict bot usage to only certain updates.
     *
     * @param update a Telegram {@link Update}
     * @return <tt>true</tt> if the update satisfies the global flags
     */
    protected boolean checkGlobalFlags(Update update) {
        return true;
    }

    /**
     * Registers the declared abilities using method reflection. Also, replies are accumulated using the built abilities and standalone methods that return a Reply.
     * <p>
     * <b>Only abilities and replies with the <u>public</u> accessor are registered!</b>
     */
    private void registerAbilities() {
        try {
            // Collect all classes that implement AbilityExtension declared in the bot
            List<AbilityExtension> extensions = stream(getClass().getMethods())
                    .filter(checkReturnType(AbilityExtension.class))
                    .map(returnExtension(this))
                    .collect(Collectors.toList());

            // Add the bot itself as it is an AbilityExtension
            extensions.add(this);

            DefaultAbilities defaultAbs = new DefaultAbilities(this);
            Stream<Ability> defaultAbsStream = stream(DefaultAbilities.class.getMethods())
                .filter(checkReturnType(Ability.class))
                .map(returnAbility(defaultAbs))
                .filter(ab -> !toggle.isOff(ab))
                .map(toggle::processAbility);

            // Extract all abilities from every single extension instance
            abilities = Stream.concat(defaultAbsStream,
                extensions.stream()
                    .flatMap(ext -> stream(ext.getClass().getMethods())
                            .filter(checkReturnType(Ability.class))
                            .map(returnAbility(ext))))
                    // Abilities are immutable, build it respectively
                    .collect(ImmutableMap::<String, Ability>builder,
                            (b, a) -> b.put(a.name(), a),
                            (b1, b2) -> b1.putAll(b2.build()))
                    .build();

            // Extract all replies from every single extension instance
            Stream<Reply> extensionReplies = extensions.stream()
                    .flatMap(ext -> stream(ext.getClass().getMethods())
                            .filter(checkReturnType(Reply.class))
                            .map(returnReply(ext)))
                            .flatMap(Reply::stream);

            // Replies can be standalone or attached to abilities, fetch those too
            Stream<Reply> abilityReplies = abilities.values().stream()
                    .flatMap(ability -> ability.replies().stream());

            // Now create the replies registry (list)
            replies = Stream.concat(abilityReplies, extensionReplies).collect(
                    ImmutableList::<Reply>builder,
                    Builder::add,
                    (b1, b2) -> b1.addAll(b2.build()))
                    .build();
        } catch (IllegalStateException e) {
            log.error("Duplicate names found while registering abilities. Make sure that the abilities declared don't clash with the reserved ones.", e);
            throw new RuntimeException(e);
        }
    }

    /**
     * @param clazz the type to be tested
     * @return a predicate testing the return type of the method corresponding to the class parameter
     */
    private static Predicate<Method> checkReturnType(Class<?> clazz) {
        return method -> clazz.isAssignableFrom(method.getReturnType());
    }

    /**
     * Invokes the method and retrieves its return {@link Reply}.
     *
     * @param obj a bot or extension that this method is invoked with
     * @return a {@link Function} which returns the {@link Reply} returned by the given method
     */
    private Function<? super Method, AbilityExtension> returnExtension(Object obj) {
        return method -> {
            try {
                return (AbilityExtension) method.invoke(obj);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Could not add ability extension", e);
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Invokes the method and retrieves its return {@link Ability}.
     *
     * @param obj a bot or extension that this method is invoked with
     * @return a {@link Function} which returns the {@link Ability} returned by the given method
     */
    private static Function<? super Method, Ability> returnAbility(Object obj) {
        return method -> {
            try {
                return (Ability) method.invoke(obj);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Could not add ability", e);
                throw new RuntimeException(e);
            }
        };
    }

    /**
     * Invokes the method and retrieves its return {@link Reply}.
     *
     * @param obj a bot or extension that this method is invoked with
     * @return a {@link Function} which returns the {@link Reply} returned by the given method
     */
    private static Function<? super Method, Reply> returnReply(Object obj) {
        return method -> {
            try {
                return (Reply) method.invoke(obj);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.error("Could not add reply", e);
                throw new RuntimeException(e);
            }
        };
    }

    private void postConsumption(Pair<MessageContext, Ability> pair) {
        ofNullable(pair.b().postAction())
                .ifPresent(consumer -> consumer.accept(pair.a()));
    }

    Pair<MessageContext, Ability> consumeUpdate(Pair<MessageContext, Ability> pair) {
        pair.b().action().accept(pair.a());
        return pair;
    }

    Pair<MessageContext, Ability> getContext(Trio<Update, Ability, String[]> trio) {
        Update update = trio.a();
        User user = AbilityUtils.getUser(update);

        return Pair.of(newContext(update, user, getChatId(update), trio.c()), trio.b());
    }

    boolean checkBlacklist(Update update) {
        Integer id = AbilityUtils.getUser(update).getId();

        return id == creatorId() || !blacklist().contains(id);
    }

    boolean checkInput(Trio<Update, Ability, String[]> trio) {
        String[] tokens = trio.c();
        int abilityTokens = trio.b().tokens();

        boolean isOk = abilityTokens == 0 || (tokens.length > 0 && tokens.length == abilityTokens);

        if (!isOk)
            silent.send(
                    getLocalizedMessage(
                            CHECK_INPUT_FAIL,
                            AbilityUtils.getUser(trio.a()).getLanguageCode(),
                            abilityTokens, abilityTokens == 1 ? "input" : "inputs"),
                    getChatId(trio.a()));
        return isOk;
    }

    boolean checkLocality(Trio<Update, Ability, String[]> trio) {
        Update update = trio.a();
        Locality locality = isUserMessage(update) ? USER : GROUP;
        Locality abilityLocality = trio.b().locality();

        boolean isOk = abilityLocality == ALL || locality == abilityLocality;

        if (!isOk)
            silent.send(
                    getLocalizedMessage(
                            CHECK_LOCALITY_FAIL,
                            AbilityUtils.getUser(trio.a()).getLanguageCode(),
                            abilityLocality.toString().toLowerCase()),
                    getChatId(trio.a()));
        return isOk;
    }

    boolean checkPrivacy(Trio<Update, Ability, String[]> trio) {
        Update update = trio.a();
        User user = AbilityUtils.getUser(update);
        Privacy privacy;
        int id = user.getId();

        privacy = getPrivacy(update, id);

        boolean isOk = privacy.compareTo(trio.b().privacy()) >= 0;

        if (!isOk)
            silent.send(
                    getLocalizedMessage(
                            CHECK_PRIVACY_FAIL,
                            AbilityUtils.getUser(trio.a()).getLanguageCode()),
                    getChatId(trio.a()));
        return isOk;
    }

    @NotNull
    Privacy getPrivacy(Update update, int id) {
        return isCreator(id) ?
                CREATOR : isAdmin(id) ?
                ADMIN : (isGroupUpdate(update) || isSuperGroupUpdate(update)) && isGroupAdmin(update, id) ?
                GROUP_ADMIN : PUBLIC;
    }

    private boolean isGroupAdmin(Update update, int id) {
        GetChatAdministrators admins = new GetChatAdministrators().setChatId(getChatId(update));

        return silent.execute(admins)
                .orElse(new ArrayList<>()).stream()
                .anyMatch(member -> member.getUser().getId() == id);
    }

    private boolean isCreator(int id) {
        return id == creatorId();
    }

    private boolean isAdmin(Integer id) {
        return admins().contains(id);
    }

    boolean validateAbility(Trio<Update, Ability, String[]> trio) {
        return trio.b() != null;
    }

    Trio<Update, Ability, String[]> getAbility(Update update) {
        // Handle updates without messages
        // Passing through this function means that the global flags have passed
        Message msg = update.getMessage();
        if (!update.hasMessage() || !msg.hasText())
            return Trio.of(update, abilities.get(DEFAULT), new String[]{});

        String[] tokens = msg.getText().split(" ");

        if (tokens[0].startsWith("/")) {
            String abilityToken = stripBotUsername(tokens[0].substring(1)).toLowerCase();
            Ability ability = abilities.get(abilityToken);
            tokens = Arrays.copyOfRange(tokens, 1, tokens.length);
            return Trio.of(update, ability, tokens);
        } else {
            Ability ability = abilities.get(DEFAULT);
            return Trio.of(update, ability, tokens);
        }
    }

    private String stripBotUsername(String token) {
        return compile(format("@%s", botUsername), CASE_INSENSITIVE)
                .matcher(token)
                .replaceAll("");
    }

    Update addUser(Update update) {
        User endUser = AbilityUtils.getUser(update);

        users().compute(endUser.getId(), (id, user) -> {
            if (user == null) {
                updateUserId(user, endUser);
                return endUser;
            }

            if (!user.equals(endUser)) {
                updateUserId(user, endUser);
                return endUser;
            }

            return user;
        });

        db.commit();
        return update;
    }

    private void updateUserId(User oldUser, User newUser) {
        if (oldUser != null && oldUser.getUserName() != null) {
            // Remove old username -> ID
            userIds().remove(oldUser.getUserName());
        }

        if (newUser.getUserName() != null) {
            // Add new mapping with the new username
            userIds().put(newUser.getUserName().toLowerCase(), newUser.getId());
        }
    }

    boolean filterReply(Update update) {
        return replies.stream()
                .filter(reply -> reply.isOkFor(update))
                .map(reply -> {
                    reply.actOn(update);
                    return false;
                })
                .reduce(true, Boolean::logicalAnd);
    }

    boolean checkMessageFlags(Trio<Update, Ability, String[]> trio) {
        Ability ability = trio.b();
        Update update = trio.a();

        // The following variable is required to avoid bug #JDK-8044546
        BiFunction<Boolean, Predicate<Update>, Boolean> flagAnd = (flag, nextFlag) -> flag && nextFlag.test(update);
        return ability.flags().stream()
                .reduce(true, flagAnd, Boolean::logicalAnd);
    }
}