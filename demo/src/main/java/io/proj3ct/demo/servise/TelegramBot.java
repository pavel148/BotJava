package io.proj3ct.demo.servise;

import com.vdurmont.emoji.EmojiParser;
import io.proj3ct.demo.config.BotConfig;
import io.proj3ct.demo.model.User;
import io.proj3ct.demo.model.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;
    final BotConfig config;
    static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilities.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself\n\n" +
            "Type /help to see this message again";

    public TelegramBot(BotConfig config) {
        this.config = config;
        List<BotCommand> listofCommands = new ArrayList<>();
        listofCommands.add(new BotCommand("/start", "get a welcome message"));
        listofCommands.add(new BotCommand("/mydata", "get your data stored"));
        listofCommands.add(new BotCommand("/deletedata", "delete my data"));
        listofCommands.add(new BotCommand("/help", "info how to use this bot"));
        listofCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listofCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot's command list: " + e.getMessage());
        }
    }
    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if(update.hasMessage()&&update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();

            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    registerUser(update.getMessage());
                    startCommandReceived(chatId,update.getMessage().getChat().getFirstName());
                    break;

                case "/help":

                    sendMessage(chatId, HELP_TEXT);
                    break;

                case "/register":
                    register(chatId);
                    break;
                default:sendMessage(chatId, "Sory");
            }
            ///метод для проспмотра что предалось сообщение вдруг мы нажади на кнопку
            /// и тогда надо сделать действие
        } else if(update.hasCallbackQuery()){
            ///проверка каую из кнопоку нажал пользователь
            String callbackData = update.getCallbackQuery().getData();
            
            
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();
            ///используем для сранвение equals
            if(callbackData.equals("YES_BUTTON")){
                String text = "Поздравляю ты нажал Да";
                EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatId));
                message.setText(text);
                ///указыаем что текст не просто был направлен а заменен в message
                ///с таким то ID
                ///+ тути тип long приводим к int
                message.setMessageId((int)messageId);

                ///просто тупо пихаем везде try catch -хех

                try{
                    execute(message);
                }catch (TelegramApiException e){
                    log.error("ERROR"+e.getMessage());
                }
            } else if (callbackData.equals("NO_BUTTON")) {
                String text = "ты нажал нет";
                EditMessageText message = new EditMessageText();
                message.setChatId(String.valueOf(chatId));
                message.setText(text);

                message.setMessageId((int)messageId);
                try{
                    execute(message);
                }catch (TelegramApiException e){
                    log.error("ERROR"+e.getMessage());
                }
            }
        }

    }

    private void register(long chatId)
    {
        SendMessage massage = new SendMessage();
        massage.setChatId(String.valueOf(chatId));
        massage.setText("Do you really want to register");
        ///создаем плавующаю клавиатуру
        InlineKeyboardMarkup markupInLine = new InlineKeyboardMarkup();
        ///список списков где харним кнопки
        List<List<InlineKeyboardButton>> rowsInLine = new ArrayList<>();
        ///список где хранятся кнопки одного ряда
        List<InlineKeyboardButton> rowInLine = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Yes");
        ///идентификатор для понимания bot -ом какая кнопка нажата
        yesButton.setCallbackData("YES_BUTTON");

        var noButton = new InlineKeyboardButton();

        noButton.setText("No");
        noButton.setCallbackData("NO_BUTTON");

        rowInLine.add(yesButton);
        rowInLine.add(noButton);

        rowsInLine.add(rowInLine);

        markupInLine.setKeyboard(rowsInLine);
        massage.setReplyMarkup(markupInLine);

        try{
            execute(massage);
        }catch (TelegramApiException e){
            log.error("ERROR"+e.getMessage());
        }

    }

    private void executeMessage(SendMessage massage)
    {
    }

    private void registerUser(Message msg) {

        if(userRepository.findById(msg.getChatId()).isEmpty()){

            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void  startCommandReceived(long chatId, String name ) {
    String answer= EmojiParser.parseToUnicode("Hi "+name+" nice to meet you!"+ ":blush:");
    log.info("Replaid to user "+name);
    sendMessage(chatId,answer);
    }
    private void sendMessage(long chatId, String textToSend){
        //сделай отдельный метод для клавиатуры
        SendMessage message=new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        ///создаем клавиатуру - одна и та же клавиатура будет
        ///вынести в отдельный метод
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();
        KeyboardRow row = new KeyboardRow();
        row.add("сделать заказ");
        row.add("привязать карту");

        keyboardRows.add(row);

        row = new KeyboardRow();
        row.add("скидки");
        row.add("рестораны");
        row.add("объявления");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);

        message.setReplyMarkup(keyboardMarkup);


        try{
            execute(message);
        }
        catch (TelegramApiException e){
             log.error("Error occurred: "+e.getMessage());
        }
    }

    @Override
    public String toString() {
        return "TelegramBot{}";
    }
}