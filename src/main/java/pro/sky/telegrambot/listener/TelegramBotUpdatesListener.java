package pro.sky.telegrambot.listener;

import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pro.sky.telegrambot.model.NotificationTask;
import pro.sky.telegrambot.repository.NotificationTaskRepository;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class TelegramBotUpdatesListener implements UpdatesListener {
    private final Logger logger = LoggerFactory.getLogger(TelegramBotUpdatesListener.class);
    private final Pattern pattern = Pattern.compile("([0-9.:\\s]{16})(\\s)([\\W+]+)");
    private final NotificationTaskRepository notificationTaskRepository;
    private final TelegramBot telegramBot;

    public TelegramBotUpdatesListener(NotificationTaskRepository notificationTaskRepository, TelegramBot telegramBot) {
        this.notificationTaskRepository = notificationTaskRepository;
        this.telegramBot = telegramBot;
    }

    @PostConstruct
    public void init() {
        telegramBot.setUpdatesListener(this);
    }

    @Override
    public int process(List<Update> updates) {
        updates.forEach(update -> {
            logger.info("Processing update: {}", update);
            try {
                Matcher matcher = pattern.matcher(update.message().text());
                if (update.message() != null && "/start".equals(update.message().text())) {
                    telegramBot.execute(new SendMessage(update.message().chat().id(), "Hello"));
                } else if (update.message() != null && matcher.matches()) {
                    save(create(update.message().chat().id(), matcher.group(3), matcher.group(1)));
                } else {
                    telegramBot.execute(new SendMessage(update.message().chat().id(), "Неправильно введено сообщение! Шаблон сообщения: 01.01.2022 20:00 Сделать домашнюю работу"));
                }
            } catch (NullPointerException e) {
                telegramBot.execute(new SendMessage(update.message().chat().id(), "Бот работает только с текстовыми сообщениями"));
            }
        });
        return UpdatesListener.CONFIRMED_UPDATES_ALL;
    }

    public void save(NotificationTask notificationTask) {
        logger.info("Saving the notification to the database");
        notificationTaskRepository.save(notificationTask);
    }

    public NotificationTask create(long chatId, String message, String date) {
        logger.info("Filling in notification fields");
        NotificationTask notificationTask = new NotificationTask();
        notificationTask.setChatId(chatId);
        notificationTask.setMessage(message);
        notificationTask.setDate(LocalDateTime.parse(date, DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")));
        return notificationTask;
    }

    @Scheduled(cron = "0 0/1 * * * *")
    public void findByDate() {
        logger.info("Sending notifications to users at a specified time");
        List<NotificationTask> notificationTasks = notificationTaskRepository.findByDate(LocalDateTime.now().truncatedTo(ChronoUnit.MINUTES));
        notificationTasks.forEach(user -> telegramBot.execute(new SendMessage(user.getChatId(), user.getMessage())));
    }


}
