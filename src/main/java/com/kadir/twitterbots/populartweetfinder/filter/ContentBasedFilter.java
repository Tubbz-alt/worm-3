package com.kadir.twitterbots.populartweetfinder.filter;

import com.google.common.base.Optional;
import com.kadir.twitterbots.populartweetfinder.dao.ContentFilterDao;
import com.kadir.twitterbots.populartweetfinder.entity.TaskPriority;
import com.kadir.twitterbots.populartweetfinder.exceptions.IllegalLanguageKeyException;
import com.kadir.twitterbots.populartweetfinder.exceptions.LanguageIdentifierInitialisingException;
import com.kadir.twitterbots.populartweetfinder.scheduler.BaseScheduledRunnable;
import com.kadir.twitterbots.populartweetfinder.scheduler.TaskScheduler;
import com.kadir.twitterbots.populartweetfinder.util.DataUtil;
import com.optimaize.langdetect.LanguageDetector;
import com.optimaize.langdetect.LanguageDetectorBuilder;
import com.optimaize.langdetect.i18n.LdLocale;
import com.optimaize.langdetect.ngram.NgramExtractors;
import com.optimaize.langdetect.profiles.LanguageProfile;
import com.optimaize.langdetect.profiles.LanguageProfileReader;
import com.optimaize.langdetect.text.CommonTextObjectFactories;
import com.optimaize.langdetect.text.TextObject;
import com.optimaize.langdetect.text.TextObjectFactory;
import com.vdurmont.emoji.EmojiParser;
import org.apache.log4j.Logger;
import twitter4j.Status;
import twitter4j.UserMentionEntity;
import zemberek.langid.LanguageIdentifier;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.kadir.twitterbots.populartweetfinder.util.ApplicationConstants.DEFAULT_DELAY_FOR_SCHEDULED_TASKS;
import static com.kadir.twitterbots.populartweetfinder.util.ApplicationConstants.DEFAULT_INITIAL_DELAY_FOR_SCHEDULED_TASKS;

/**
 * @author akadir
 * Date: 08/12/2018
 * Time: 20:29
 */
public class ContentBasedFilter extends BaseScheduledRunnable implements StatusFilter {
    private final Logger logger = Logger.getLogger(this.getClass());

    private String languageKey;
    private LanguageIdentifier languageIdentifier;
    private LanguageDetector languageDetector;
    private ContentFilterDao contentFilterDao;
    private Set<String> ignoredWords;
    private Set<String> ignoredUsernames;

    public ContentBasedFilter() {
        super(TaskPriority.LOW);
        init();
    }

    private void init() {
        languageKey = System.getProperty("languageKey");
        if (DataUtil.isNullOrEmpty(languageKey)) {
            throw new IllegalLanguageKeyException(languageKey);
        }
        logger.debug("Set languageKey:" + languageKey);
        initializeLanguageIdentifiers();
        contentFilterDao = new ContentFilterDao();
        loadIgnoredKeyWords();
        executorService = Executors.newScheduledThreadPool(1);
    }

    @Override
    public void schedule() {
        executorService.scheduleWithFixedDelay(this, DEFAULT_INITIAL_DELAY_FOR_SCHEDULED_TASKS, DEFAULT_DELAY_FOR_SCHEDULED_TASKS, TimeUnit.MINUTES);
        logger.info("add scheduler to run with fixed delay. initial delay:" + DEFAULT_INITIAL_DELAY_FOR_SCHEDULED_TASKS + " delay:" + DEFAULT_DELAY_FOR_SCHEDULED_TASKS);
        TaskScheduler.addScheduledTask(this);
    }

    /**
     * When an object implementing interface <code>Runnable</code> is used
     * to create a thread, starting the thread causes the object's
     * <code>run</code> method to be called in that separately executing
     * thread.
     * <p>
     * The general contract of the method <code>run</code> is that it may
     * take any action whatsoever.
     *
     * @see Thread#run()
     */
    @Override
    public void run() {
        logger.info("run scheduled task: " + this.getClass().getSimpleName());
        loadIgnoredKeyWords();
    }

    @Override
    public boolean passed(Status status) {
        String statusText = clearStatusText(status);
        return statusText.length() > 30 && !doesStatusContainIgnoredWord(status) && checkLanguageViaZemberek(statusText) && checkLanguageViaOptimaizeLanguageDetector(statusText);
    }

    private String clearStatusText(Status status) {
        String statusText = EmojiParser.removeAllEmojis(status.getText().replaceAll("http\\S+", ""));
        statusText = statusText.replaceAll("[@#]\\S+", "").trim().replaceAll(" +", " ");

        return statusText;
    }

    private boolean doesStatusContainIgnoredWord(Status status) {
        String lowerCaseContent = status.getText().toLowerCase();

        return containsIgnoredWord(lowerCaseContent) || isMentionToIgnoredUsername(status);
    }

    private boolean containsIgnoredWord(String lowerCaseContent) {
        for (String word : ignoredWords) {
            if (lowerCaseContent.contains(word.concat(" ")) || lowerCaseContent.contains(" ".concat(word))) {
                return true;
            }
        }
        return false;
    }

    private boolean isMentionToIgnoredUsername(Status status) {
        for (String username : ignoredUsernames) {
            if (status.getUserMentionEntities().length > 0) {
                for (UserMentionEntity userMentionEntity : status.getUserMentionEntities()) {
                    if (userMentionEntity.getScreenName().equalsIgnoreCase(username)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean checkLanguageViaZemberek(String text) {
        String language = languageIdentifier.identify(text);
        return language.equalsIgnoreCase(languageKey);
    }

    private boolean checkLanguageViaOptimaizeLanguageDetector(String text) {
        boolean isCorrect = false;
        TextObjectFactory textObjectFactory = CommonTextObjectFactories.forDetectingShortCleanText();
        //query:
        TextObject textObject = textObjectFactory.forText(text);
        Optional<LdLocale> lang = languageDetector.detect(textObject);
        if (lang.isPresent()) {
            isCorrect = lang.get().getLanguage().equalsIgnoreCase(languageKey);
        }
        return isCorrect;
    }

    private void loadIgnoredKeyWords() {
        ignoredWords = contentFilterDao.getIgnoredWords();
        ignoredUsernames = contentFilterDao.getIgnoredUsernames();
        logger.info("load ignored keywords from database. words: " + ignoredWords.size() + " - usernames: " + ignoredUsernames.size());
    }

    private void initializeLanguageIdentifiers() {
        try {
            initializeZemberekLanguageIdentifier();
            initializeOptimaizeLanguageDetector();
        } catch (IOException e) {
            logger.error("Error occured while initialising language identifiers.", e);
            throw new LanguageIdentifierInitialisingException(e);
        }
    }

    private void initializeZemberekLanguageIdentifier() throws IOException {
        languageIdentifier = LanguageIdentifier.fromInternalModelGroup("tr_group");
        logger.info("Zemberek language identifier has been initialised");
    }

    private void initializeOptimaizeLanguageDetector() throws IOException {
        List<LanguageProfile> languageProfiles = new ArrayList<>();
        languageProfiles.add(new LanguageProfileReader().readBuiltIn(LdLocale.fromString("tr")));
        languageProfiles.add(new LanguageProfileReader().readBuiltIn(LdLocale.fromString("en")));
        languageProfiles.add(new LanguageProfileReader().readBuiltIn(LdLocale.fromString("de")));
        languageProfiles.add(new LanguageProfileReader().readBuiltIn(LdLocale.fromString("es")));
        languageProfiles.add(new LanguageProfileReader().readBuiltIn(LdLocale.fromString("fr")));
        languageProfiles.add(new LanguageProfileReader().readBuiltIn(LdLocale.fromString("ja")));
        languageProfiles.add(new LanguageProfileReader().readBuiltIn(LdLocale.fromString("ko")));

        languageDetector = LanguageDetectorBuilder.create(NgramExtractors.standard())
                .withProfiles(languageProfiles)
                .build();
        logger.info("Optimaize language detector has been initialised");
    }
}
