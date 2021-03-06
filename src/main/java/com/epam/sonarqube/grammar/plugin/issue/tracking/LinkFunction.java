package com.epam.sonarqube.grammar.plugin.issue.tracking;

import com.epam.sonarqube.grammar.plugin.PluginParameter;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.ServerExtension;
import org.sonar.api.issue.action.Function;
import org.sonar.core.properties.PropertiesDao;
import org.sonar.core.properties.PropertyDto;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

public class LinkFunction implements Function, ServerExtension {

    private static final Logger LOGGER = LoggerFactory.getLogger(LinkFunction.class);
    private PropertiesDao propertiesDao;

    public LinkFunction(PropertiesDao propertiesDao) {
        this.propertiesDao = propertiesDao;
    }

    @Override
    public void execute(Context context) {
        final String word = getMistakeWord(context.issue().message(), PluginParameter.ERROR_DESCRIPTION);
        PropertyDto propertyDto = propertiesDao.selectGlobalProperty(PluginParameter.ALTERNATIVE_DICTIONARY_PROPERTY_KEY);
        if (propertyDto == null) {
            LOGGER.info("Creating new Dictionary. Adding word '{}' to it.", word);
            propertiesDao.saveGlobalProperties(new HashMap<String, String>() {{
                put(PluginParameter.ALTERNATIVE_DICTIONARY_PROPERTY_KEY, word);
            }});
        } else {
            String dictionary = propertyDto.getValue();
            ArrayList<String> wordList = new ArrayList<>(Arrays.asList(dictionary.split(PluginParameter.SEPARATOR_CHAR)));
            writeSortedIfUnique(word, dictionary, wordList);
        }
    }

    private void writeSortedIfUnique(String word, String dictionary, ArrayList<String> wordList) {
        if (wordList.contains(word)) {
            LOGGER.info("Don't add. Word  '{}' is already in dictionary.", word);
        } else {
            wordList.add(word);
            Collections.sort(wordList);
            String sortedDictionary = StringUtils.join(wordList, PluginParameter.SEPARATOR_CHAR);
            propertiesDao.updateProperties(PluginParameter.ALTERNATIVE_DICTIONARY_PROPERTY_KEY, dictionary, sortedDictionary);
            LOGGER.info("Added word '{}' to dictionary.", word);
        }
    }

    private String getMistakeWord(String message, String errDescription) {
        String res = message.replaceFirst(errDescription, "");
        return res.substring(0, res.length() - 1).trim();
    }
}