package org.telegram.abilitybots.api.bot;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.telegram.abilitybots.api.db.DBContext;
import org.telegram.abilitybots.api.objects.*;
import org.telegram.abilitybots.api.sender.MessageSender;
import org.telegram.abilitybots.api.sender.SilentSender;
import org.telegram.abilitybots.api.util.Pair;
import org.telegram.abilitybots.api.util.Trio;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatAdministrators;
import org.telegram.telegrambots.meta.api.objects.ChatMember;
import org.telegram.telegrambots.meta.api.objects.Document;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.User;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Sets.newHashSet;
import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static org.apache.commons.io.FileUtils.deleteQuietly;
import static org.apache.commons.lang3.ArrayUtils.addAll;
import static org.apache.commons.lang3.StringUtils.EMPTY;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;
import static org.telegram.abilitybots.api.bot.DefaultBot.getDefaultBuilder;
import static org.telegram.abilitybots.api.db.MapDBContext.offlineInstance;
import static org.telegram.abilitybots.api.objects.Flag.DOCUMENT;
import static org.telegram.abilitybots.api.objects.Flag.MESSAGE;
import static org.telegram.abilitybots.api.objects.Locality.ALL;
import static org.telegram.abilitybots.api.objects.Locality.GROUP;
import static org.telegram.abilitybots.api.objects.MessageContext.newContext;
import static org.telegram.abilitybots.api.objects.Privacy.*;

public class AbilityBotTest {
  // Messages
  private static final String RECOVERY_MESSAGE = "I am ready to receive the backup file. Please reply to this message with the backup file attached.";
  private static final String RECOVER_SUCCESS = "I have successfully recovered.";

  private static final String[] EMPTY_ARRAY = {};
  private static final long GROUP_ID = 10L;
  private static final String TEST = "test";
  private static final String[] TEXT = {TEST};
  public static final User USER = new User(1, "first", false, "last", "username", null);
  public static final User CREATOR = new User(1337, "creatorFirst", false, "creatorLast", "creatorUsername", null);

  private DefaultBot bot;
  private DBContext db;
  private MessageSender sender;
  private SilentSender silent;

  @BeforeEach
  void setUp() {
    db = offlineInstance("db");
    bot = new DefaultBot(EMPTY, EMPTY, db);

    sender = mock(MessageSender.class);
    silent = mock(SilentSender.class);

    bot.sender = sender;
    bot.silent = silent;
  }

  @AfterEach
  void tearDown() throws IOException {
    db.clear();
    db.close();
  }

  @Test
  void sendsPrivacyViolation() {
    Update update = mockFullUpdate(USER, "/admin");

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send("Sorry, you don't have the required access level to do that.", USER.getId());
  }

  @Test
  void sendsLocalityViolation() {
    Update update = mockFullUpdate(USER, "/group");

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(format("Sorry, %s-only feature.", "group"), USER.getId());

  }

  @Test
  void sendsInputArgsViolation() {
    Update update = mockFullUpdate(USER, "/count 1 2 3");

    bot.onUpdateReceived(update);

    verify(silent, times(1)).send(format("Sorry, this feature requires %d additional inputs.", 4), USER.getId());
  }

  @Test
  void canProcessRepliesIfSatisfyRequirements() {
    Update update = mockFullUpdate(USER, "must reply");

    // False means the update was not pushed down the stream since it has been consumed by the reply
    assertFalse(bot.filterReply(update));
    verify(silent, times(1)).send("reply", USER.getId());
  }

  @Test
  void canBackupDB() throws TelegramApiException {
    MessageContext context = defaultContext();

    bot.backupDB().action().accept(context);
    deleteQuietly(new java.io.File("backup.json"));

    verify(sender, times(1)).sendDocument(any());
  }

  @Test
  void canRecoverDB() throws TelegramApiException, IOException {
    Update update = mockBackupUpdate();
    Object backup = getDbBackup();
    java.io.File backupFile = createBackupFile(backup);

    when(sender.downloadFile(any(File.class))).thenReturn(backupFile);
    bot.recoverDB().replies().get(0).actOn(update);

    verify(silent, times(1)).send(RECOVER_SUCCESS, GROUP_ID);
    assertEquals(db.getSet(TEST), newHashSet(TEST), "Bot recovered but the DB is still not in sync");
    assertTrue(backupFile.delete(), "Could not delete backup file");
  }

  @Test
  void canFilterOutReplies() {
    Update update = mock(Update.class);
    when(update.hasMessage()).thenReturn(false);

    assertTrue(bot.filterReply(update));
  }

  @Test
  void canDemote() {
    addUsers(USER);
    bot.admins().add(USER.getId());

    MessageContext context = defaultContext();

    bot.demoteAdmin().action().accept(context);

    Set<Integer> actual = bot.admins();
    Set<Integer> expected = emptySet();
    assertEquals(expected, actual, "Could not sudont super-admin");
  }

  @Test
  void canPromote() {
    addUsers(USER);

    MessageContext context = defaultContext();

    bot.promoteAdmin().action().accept(context);

    Set<Integer> actual = bot.admins();
    Set<Integer> expected = newHashSet(USER.getId());
    assertEquals(expected, actual, "Could not sudo user");
  }

  @Test
  void canBanUser() {
    addUsers(USER);
    MessageContext context = defaultContext();

    bot.banUser().action().accept(context);

    Set<Integer> actual = bot.blacklist();
    Set<Integer> expected = newHashSet(USER.getId());
    assertEquals(expected, actual, "The ban was not emplaced");
  }

  @Test
  void canUnbanUser() {
    addUsers(USER);
    bot.blacklist().add(USER.getId());

    MessageContext context = defaultContext();

    bot.unbanUser().action().accept(context);

    Set<Integer> actual = bot.blacklist();
    Set<Integer> expected = newHashSet();
    assertEquals(expected, actual, "The ban was not lifted");
  }

  @NotNull
  private MessageContext defaultContext() {
    return mockContext(CREATOR, GROUP_ID, USER.getUserName());
  }

  @Test
  void cannotBanCreator() {
    addUsers(USER, CREATOR);
    MessageContext context = mockContext(USER, GROUP_ID, CREATOR.getUserName());

    bot.banUser().action().accept(context);

    Set<Integer> actual = bot.blacklist();
    Set<Integer> expected = newHashSet(USER.getId());
    assertEquals(expected, actual, "Impostor was not added to the blacklist");
  }

  private void addUsers(User... users) {
    Arrays.stream(users).forEach(user -> {
      bot.users().put(user.getId(), user);
      bot.userIds().put(user.getUserName().toLowerCase(), user.getId());
    });
  }

  @Test
  void creatorCanClaimBot() {
    MessageContext context = mockContext(CREATOR, GROUP_ID);

    bot.claimCreator().action().accept(context);

    Set<Integer> actual = bot.admins();
    Set<Integer> expected = newHashSet(CREATOR.getId());
    assertEquals(expected, actual, "Creator was not properly added to the super admins set");
  }

  @Test
  void bannedCreatorPassesBlacklistCheck() {
    bot.blacklist().add(CREATOR.getId());
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    User user = mock(User.class);

    mockUser(update, message, user);

    boolean notBanned = bot.checkBlacklist(update);
    assertTrue(notBanned, "Creator is banned");
  }

  @Test
  void canAddUser() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    mockAlternateUser(update, message, USER);

    bot.addUser(update);

    Map<String, Integer> expectedUserIds = ImmutableMap.of(USER.getUserName(), USER.getId());
    Map<Integer, User> expectedUsers = ImmutableMap.of(USER.getId(), USER);
    assertEquals(expectedUserIds, bot.userIds(), "User was not added");
    assertEquals(expectedUsers, bot.users(), "User was not added");
  }

  @Test
  void canEditUser() {
    addUsers(USER);
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    String newUsername = USER.getUserName() + "-test";
    String newFirstName = USER.getFirstName() + "-test";
    String newLastName = USER.getLastName() + "-test";
    int sameId = USER.getId();
    User changedUser = new User(sameId, newFirstName, false, newLastName, newUsername, null);

    mockAlternateUser(update, message, changedUser);

    bot.addUser(update);

    Map<String, Integer> expectedUserIds = ImmutableMap.of(changedUser.getUserName(), changedUser.getId());
    Map<Integer, User> expectedUsers = ImmutableMap.of(changedUser.getId(), changedUser);
    assertEquals(bot.userIds(), expectedUserIds, "User was not properly edited");
    assertEquals(expectedUsers, expectedUsers, "User was not properly edited");
  }

  @Test
  void canValidateAbility() {
    Trio<Update, Ability, String[]> invalidPair = Trio.of(null, null, null);
    Ability validAbility = getDefaultBuilder().build();
    Trio<Update, Ability, String[]> validPair = Trio.of(null, validAbility, null);

    assertFalse(bot.validateAbility(invalidPair), "Bot can't validate ability properly");
    assertTrue(bot.validateAbility(validPair), "Bot can't validate ability properly");
  }

  @Test
  void canCheckInput() {
    Update update = mockFullUpdate(USER, "/something");
    Ability abilityWithOneInput = getDefaultBuilder()
        .build();
    Ability abilityWithZeroInput = getDefaultBuilder()
        .input(0)
        .build();

    Trio<Update, Ability, String[]> trioOneArg = Trio.of(update, abilityWithOneInput, TEXT);
    Trio<Update, Ability, String[]> trioZeroArg = Trio.of(update, abilityWithZeroInput, TEXT);

    assertTrue(bot.checkInput(trioOneArg), "Unexpected result when applying token filter");

    trioOneArg = Trio.of(update, abilityWithOneInput, addAll(TEXT, TEXT));
    assertFalse(bot.checkInput(trioOneArg), "Unexpected result when applying token filter");

    assertTrue(bot.checkInput(trioZeroArg), "Unexpected result  when applying token filter");

    trioZeroArg = Trio.of(update, abilityWithZeroInput, EMPTY_ARRAY);
    assertTrue(bot.checkInput(trioZeroArg), "Unexpected result when applying token filter");
  }

  @Test
  void canCheckPrivacy() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    User user = mock(User.class);
    Ability publicAbility = getDefaultBuilder().privacy(PUBLIC).build();
    Ability groupAdminAbility = getDefaultBuilder().privacy(GROUP_ADMIN).build();
    Ability adminAbility = getDefaultBuilder().privacy(ADMIN).build();
    Ability creatorAbility = getDefaultBuilder().privacy(Privacy.CREATOR).build();

    Trio<Update, Ability, String[]> publicTrio = Trio.of(update, publicAbility, TEXT);
    Trio<Update, Ability, String[]> groupAdminTrio = Trio.of(update, groupAdminAbility, TEXT);
    Trio<Update, Ability, String[]> adminTrio = Trio.of(update, adminAbility, TEXT);
    Trio<Update, Ability, String[]> creatorTrio = Trio.of(update, creatorAbility, TEXT);

    mockUser(update, message, user);

    assertTrue(bot.checkPrivacy(publicTrio), "Unexpected result when checking for privacy");
    assertFalse(bot.checkPrivacy(groupAdminTrio), "Unexpected result when checking for privacy");
    assertFalse(bot.checkPrivacy(adminTrio), "Unexpected result when checking for privacy");
    assertFalse(bot.checkPrivacy(creatorTrio), "Unexpected result when checking for privacy");
  }

  @Test
  void canValidateGroupAdminPrivacy() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    User user = mock(User.class);
    Ability groupAdminAbility = getDefaultBuilder().privacy(GROUP_ADMIN).build();

    Trio<Update, Ability, String[]> groupAdminTrio = Trio.of(update, groupAdminAbility, TEXT);

    mockUser(update, message, user);
    when(message.isGroupMessage()).thenReturn(true);

    ChatMember member = mock(ChatMember.class);
    when(member.getUser()).thenReturn(user);
    when(member.getUser()).thenReturn(user);

    when(silent.execute(any(GetChatAdministrators.class))).thenReturn(Optional.of(newArrayList(member)));

    assertTrue(bot.checkPrivacy(groupAdminTrio), "Unexpected result when checking for privacy");
  }

  @Test
  void canRestrictNormalUsersFromGroupAdminAbilities() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    User user = mock(User.class);
    Ability groupAdminAbility = getDefaultBuilder().privacy(GROUP_ADMIN).build();

    Trio<Update, Ability, String[]> groupAdminTrio = Trio.of(update, groupAdminAbility, TEXT);

    mockUser(update, message, user);
    when(message.isGroupMessage()).thenReturn(true);

    when(silent.execute(any(GetChatAdministrators.class))).thenReturn(empty());

    assertFalse(bot.checkPrivacy(groupAdminTrio), "Unexpected result when checking for privacy");
  }

  @Test
  void canBlockAdminsFromCreatorAbilities() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    User user = mock(User.class);
    Ability creatorAbility = getDefaultBuilder().privacy(Privacy.CREATOR).build();

    Trio<Update, Ability, String[]> creatorTrio = Trio.of(update, creatorAbility, TEXT);

    bot.admins().add(USER.getId());
    mockUser(update, message, user);

    assertFalse(bot.checkPrivacy(creatorTrio), "Unexpected result when checking for privacy");
  }

  @Test
  void canCheckLocality() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    User user = mock(User.class);
    Ability allAbility = getDefaultBuilder().locality(ALL).build();
    Ability userAbility = getDefaultBuilder().locality(Locality.USER).build();
    Ability groupAbility = getDefaultBuilder().locality(GROUP).build();

    Trio<Update, Ability, String[]> publicTrio = Trio.of(update, allAbility, TEXT);
    Trio<Update, Ability, String[]> userTrio = Trio.of(update, userAbility, TEXT);
    Trio<Update, Ability, String[]> groupTrio = Trio.of(update, groupAbility, TEXT);

    mockUser(update, message, user);
    when(message.isUserMessage()).thenReturn(true);

    assertTrue(bot.checkLocality(publicTrio), "Unexpected result when checking for locality");
    assertTrue(bot.checkLocality(userTrio), "Unexpected result when checking for locality");
    assertFalse(bot.checkLocality(groupTrio), "Unexpected result when checking for locality");
  }

  @Test
  void canRetrieveContext() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    Ability ability = getDefaultBuilder().build();
    Trio<Update, Ability, String[]> trio = Trio.of(update, ability, TEXT);

    when(message.getChatId()).thenReturn(GROUP_ID);
    mockUser(update, message, USER);

    Pair<MessageContext, Ability> actualPair = bot.getContext(trio);
    Pair<MessageContext, Ability> expectedPair = Pair.of(newContext(update, USER, GROUP_ID, TEXT), ability);

    assertEquals(expectedPair, actualPair, "Unexpected result when fetching for context");
  }

  @Test
  void defaultGlobalFlagIsTrue() {
    Update update = mock(Update.class);
    assertTrue(bot.checkGlobalFlags(update), "Unexpected result when checking for the default global flags");
  }

  @Test
  void canConsumeUpdate() {
    Ability ability = getDefaultBuilder()
        .action((context) -> {
          int x = 1 / 0;
        }).build();
    MessageContext context = mock(MessageContext.class);

    Pair<MessageContext, Ability> pair = Pair.of(context, ability);

    Assertions.assertThrows(ArithmeticException.class, () -> bot.consumeUpdate(pair));
  }

  @Test
  void canFetchAbility() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    String text = "/test";
    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(update.getMessage().hasText()).thenReturn(true);
    when(message.getText()).thenReturn(text);

    Trio<Update, Ability, String[]> trio = bot.getAbility(update);

    Ability expected = bot.testAbility();
    Ability actual = trio.b();

    assertEquals(expected, actual, "Wrong ability was fetched");
  }

  @Test
  void canFetchAbilityCaseInsensitive() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    String text = "/tESt";
    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(update.getMessage().hasText()).thenReturn(true);
    when(message.getText()).thenReturn(text);

    Trio<Update, Ability, String[]> trio = bot.getAbility(update);

    Ability expected = bot.testAbility();
    Ability actual = trio.b();

    assertEquals(expected, actual, "Wrong ability was fetched");
  }

  @Test
  void canFetchDefaultAbility() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    String text = "test tags";
    when(update.getMessage()).thenReturn(message);
    when(message.getText()).thenReturn(text);

    Trio<Update, Ability, String[]> trio = bot.getAbility(update);

    Ability expected = bot.defaultAbility();
    Ability actual = trio.b();

    assertEquals(expected, actual, "Wrong ability was fetched");
  }

  @Test
  void canCheckAbilityFlags() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasDocument()).thenReturn(false);
    when(message.hasText()).thenReturn(true);

    Ability documentAbility = getDefaultBuilder().flag(DOCUMENT, MESSAGE).build();
    Ability textAbility = getDefaultBuilder().flag(Flag.TEXT, MESSAGE).build();

    Trio<Update, Ability, String[]> docTrio = Trio.of(update, documentAbility, TEXT);
    Trio<Update, Ability, String[]> textTrio = Trio.of(update, textAbility, TEXT);

    assertFalse(bot.checkMessageFlags(docTrio), "Unexpected result when checking for message flags");
    assertTrue(bot.checkMessageFlags(textTrio), "Unexpected result when checking for message flags");
  }

  @Test
  void canReportCommands() {
    MessageContext context = mockContext(USER, GROUP_ID);

    bot.reportCommands().action().accept(context);

    verify(silent, times(1)).send("default - dis iz default command", GROUP_ID);
  }

  @NotNull
  static MessageContext mockContext(User user) {
    return mockContext(user, user.getId());
  }

  @NotNull
  static MessageContext mockContext(User user, long groupId, String... args) {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);

    when(message.getFrom()).thenReturn(user);
    when(message.hasText()).thenReturn(true);

    return newContext(update, user, groupId, args);
  }

  @Test
  void canPrintCommandsBasedOnPrivacy() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasText()).thenReturn(true);
    MessageContext creatorCtx = newContext(update, CREATOR, GROUP_ID);

    bot.commands().action().accept(creatorCtx);

    String expected = "PUBLIC\n/commands\n/count\n/default - dis iz default command\n/group\n/test\nADMIN\n/admin\n/ban\n/demote\n/promote\n/unban\nCREATOR\n/backup\n/claim\n/recover\n/report";
    verify(silent, times(1)).send(expected, GROUP_ID);
  }

  @Test
  void printsOnlyPublicCommandsForNormalUser() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);

    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.hasText()).thenReturn(true);

    MessageContext userCtx = newContext(update, USER, GROUP_ID);

    bot.commands().action().accept(userCtx);

    String expected = "PUBLIC\n/commands\n/count\n/default - dis iz default command\n/group\n/test";
    verify(silent, times(1)).send(expected, GROUP_ID);
  }

  @NotNull
  private Update mockFullUpdate(User user, String args) {
    bot.users().put(USER.getId(), USER);
    bot.users().put(CREATOR.getId(), CREATOR);
    bot.userIds().put(CREATOR.getUserName(), CREATOR.getId());
    bot.userIds().put(USER.getUserName(), USER.getId());

    bot.admins().add(CREATOR.getId());

    Update update = mock(Update.class);
    when(update.hasMessage()).thenReturn(true);
    Message message = mock(Message.class);
    when(message.getFrom()).thenReturn(user);
    when(message.getText()).thenReturn(args);
    when(message.hasText()).thenReturn(true);
    when(message.isUserMessage()).thenReturn(true);
    when(message.getChatId()).thenReturn((long) user.getId());
    when(update.getMessage()).thenReturn(message);
    return update;
  }

  private void mockUser(Update update, Message message, User user) {
    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
    when(message.getFrom()).thenReturn(user);
  }

  private void mockAlternateUser(Update update, Message message, User user) {
    when(message.getFrom()).thenReturn(user);
    when(update.hasMessage()).thenReturn(true);
    when(update.getMessage()).thenReturn(message);
  }

  private Update mockBackupUpdate() {
    Update update = mock(Update.class);
    Message message = mock(Message.class);
    Message botMessage = mock(Message.class);
    Document document = mock(Document.class);

    when(message.getFrom()).thenReturn(CREATOR);
    when(update.getMessage()).thenReturn(message);
    when(message.getDocument()).thenReturn(document);
    when(botMessage.getText()).thenReturn(RECOVERY_MESSAGE);
    when(message.isReply()).thenReturn(true);
    when(update.hasMessage()).thenReturn(true);
    when(message.hasDocument()).thenReturn(true);
    when(message.getReplyToMessage()).thenReturn(botMessage);
    when(message.getChatId()).thenReturn(GROUP_ID);
    return update;
  }

  private Object getDbBackup() {
    db.getSet(TEST).add(TEST);
    Object backup = db.backup();
    db.clear();
    return backup;
  }

  private java.io.File createBackupFile(Object backup) throws IOException {
    java.io.File backupFile = new java.io.File(TEST);
    BufferedWriter writer = Files.newWriter(backupFile, Charset.defaultCharset());
    writer.write(backup.toString());
    writer.flush();
    writer.close();
    return backupFile;
  }
}
