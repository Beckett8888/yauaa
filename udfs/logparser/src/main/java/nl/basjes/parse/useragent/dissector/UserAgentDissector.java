/*
 * Yet Another UserAgent Analyzer
 * Copyright (C) 2013-2018 Niels Basjes
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package nl.basjes.parse.useragent.dissector;

import nl.basjes.parse.core.Casts;
import nl.basjes.parse.core.Dissector;
import nl.basjes.parse.core.Parsable;
import nl.basjes.parse.core.ParsedField;
import nl.basjes.parse.core.exceptions.DissectionFailure;
import nl.basjes.parse.core.exceptions.InvalidDissectorException;
import nl.basjes.parse.useragent.UserAgent;
import nl.basjes.parse.useragent.UserAgentAnalyzer;
import nl.basjes.parse.useragent.UserAgentAnalyzer.UserAgentAnalyzerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class UserAgentDissector extends Dissector {

    private static final Logger LOG = LoggerFactory.getLogger(UserAgentDissector.class);

    private transient UserAgentAnalyzerBuilder<?, ?> userAgentAnalyzerBuilder;
    private UserAgentAnalyzer userAgentAnalyzer = null;
    private static final String INPUT_TYPE = "HTTP.USERAGENT";

    private List<String> additionalResources   = new ArrayList<>();
    private List<String> allPossibleFieldNames = new ArrayList<>();
    private List<String> requestedFieldNames   = new ArrayList<>();

    private boolean      dropPIIFields  = false;

    @Override
    public String getInputType() {
        return INPUT_TYPE;
    }

    private UserAgentAnalyzerBuilder<?, ?> getUserAgentAnalyzerBuilder() {
        if (userAgentAnalyzerBuilder == null) {
            userAgentAnalyzerBuilder = UserAgentAnalyzer
                .newBuilder()
                .delayInitialization()
                .dropTests()
                .hideMatcherLoadStats();
        }
        return userAgentAnalyzerBuilder;
    }

    /**
     * @param rawParameter For this dissector it is a '|' separated list of resource paths.
     */
    @Override
    public boolean initializeFromSettingsParameter(String rawParameter) {
        if (rawParameter == null || rawParameter.isEmpty()) {
            return true; // Nothing to do here
        }

        String[] parameters = rawParameter.trim().split("\\|");
        for (String parameter: parameters) {
            parameter = parameter.trim();
            if (!parameter.isEmpty()) {
                if ("DropPII".equalsIgnoreCase(parameter)) {
                    dropPIIFields = true;
                } else {
                    additionalResources.add(parameter);
                }
            }
        }
        return true;
    }

    @Override
    public void dissect(Parsable<?> parsable, String inputname) throws DissectionFailure {
        final ParsedField agentField = parsable.getParsableField(INPUT_TYPE, inputname);

        String userAgentString = agentField.getValue().getString();

        if (userAgentString == null) {
            return;  // Weird, but it happens
        }

        UserAgent agent = userAgentAnalyzer.parse(userAgentString);

        for (String fieldName : requestedFieldNames) {
            parsable.addDissection(inputname, getFieldOutputType(fieldName), fieldNameToDissectionName(fieldName), agent.getValue(fieldName));
        }
    }

    @Override
    public List<String> getPossibleOutput() {
        List<String> result = new ArrayList<>();

        final UserAgentAnalyzerBuilder<?, ?> uaaBuilder = UserAgentAnalyzer
            .newBuilder()
            .delayInitialization()
            .dropTests()
            .hideMatcherLoadStats();
        for (String additionalResource: additionalResources) {
            uaaBuilder.addResources(additionalResource);
        }
        if (dropPIIFields) {
            uaaBuilder.dropPIIFields();
        }

        // First the standard fields in the standard order, then the non-standard fields alphabetically
        final UserAgentAnalyzerBuilder<?, ?> builder = UserAgentAnalyzer.newBuilder();
        additionalResources.forEach(builder::addResources);

        allPossibleFieldNames = builder.build().getAllPossibleFieldNamesSorted();
        for (String fieldName : allPossibleFieldNames) {
            ensureMappingsExistForFieldName(fieldName);
            result.add(getFieldOutputType(fieldName) + ":" + fieldNameToDissectionName(fieldName));
            if (dropPIIFields) {
                result.add("DROPPIIFLAG:" + fieldNameToDissectionName("DropPII"));
            }
        }
//        result.forEach(f -> LOG.warn("Possible {}", f));
        return result;
    }

    private String getFieldOutputType(String fieldName) {
        switch (fieldName) {
            case "AgentInformationUrl":
                return "HTTP.URI";
            default:
                return "STRING";
        }
    }

    @Override
    public EnumSet<Casts> prepareForDissect(String inputname, String outputname) {
        String name = extractFieldName(inputname, outputname);
        String fieldName = dissectionNameToFieldName(name);

        if (fieldName == null) {
            LOG.error("There is NO fieldname for the requested \"{}\" ({})", outputname, name);
            return null;
        }
        requestedFieldNames.add(fieldName);
//        LOG.info("Wanted: {} -- {} (--> {})", inputname, name, fieldName);
        return Casts.STRING_ONLY; // We ONLY do Strings here
    }

    @Override
    public void prepareForRun() {
        // Build the internal datastructures
        LOG.info("Preparing UserAgentAnalyzer to extract {}", requestedFieldNames.isEmpty()? "all fields" : requestedFieldNames);
        final UserAgentAnalyzerBuilder<?, ?> builder = getUserAgentAnalyzerBuilder();

        additionalResources.forEach(r -> LOG.warn("Loading extra resource: {}", r));
        additionalResources.forEach(builder::addResources);
        requestedFieldNames.forEach(builder::withField);
        if (dropPIIFields) {
            builder.dropPIIFields();
        }
        userAgentAnalyzer = builder.build();
        userAgentAnalyzer.initializeMatchers();
    }

    @Override
    protected void initializeNewInstance(Dissector newInstance) throws InvalidDissectorException {
        if (!(newInstance instanceof UserAgentDissector)) {
            throw new InvalidDissectorException("The provided instance of the dissector is a " +
                newInstance.getClass().getCanonicalName() + " which is not a UserAgentDissector");
        }
        UserAgentDissector newUserAgentDissector = (UserAgentDissector) newInstance;
        newUserAgentDissector.dropPIIFields = dropPIIFields;
        newUserAgentDissector.additionalResources = new ArrayList<>(additionalResources);
        newUserAgentDissector.allPossibleFieldNames = new ArrayList<>(allPossibleFieldNames);
        newUserAgentDissector.requestedFieldNames = new ArrayList<>(requestedFieldNames);
        allPossibleFieldNames.forEach(newUserAgentDissector::ensureMappingsExistForFieldName);
    }

    private final Map<String, String> fieldNameMappingCache      = new HashMap<>(64);
    private final Map<String, String> dissectionNameMappingCache = new HashMap<>(64);

    void ensureMappingsExistForFieldName(String fieldName) {
        if (fieldNameMappingCache.containsKey(fieldName)) {
            return;
        }
        String dissectionName = fieldName
            .replaceAll("([A-Z])", "_$1")
            .toLowerCase(Locale.ENGLISH)
            .replaceFirst("_", "");

        fieldNameMappingCache.put(fieldName, dissectionName);
        dissectionNameMappingCache.put(dissectionName, fieldName);
    }

    String fieldNameToDissectionName(String fieldName) {
        return fieldNameMappingCache.get(fieldName);
    }

    String dissectionNameToFieldName(String dissectionName) {
        return dissectionNameMappingCache.get(dissectionName);
    }

}
