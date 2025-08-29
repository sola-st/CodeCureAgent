/*
 * JBoss, Home of Professional Open Source
 * Copyright 2014 Red Hat Inc. and/or its affiliates and other contributors
 * as indicated by the @authors tag. All rights reserved.
 * See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.aesh.command.impl.internal;

import org.aesh.command.Command;
import org.aesh.command.activator.CommandActivator;
import org.aesh.command.impl.activator.NullCommandActivator;
import org.aesh.command.impl.parser.CompleteStatus;
import org.aesh.command.impl.populator.AeshCommandPopulator;
import org.aesh.command.impl.result.NullResultHandler;
import org.aesh.command.invocation.CommandInvocation;
import org.aesh.command.invocation.InvocationProviders;
import org.aesh.command.parser.CommandLineParserException;
import org.aesh.command.parser.OptionParserException;
import org.aesh.command.populator.CommandPopulator;
import org.aesh.command.result.ResultHandler;
import org.aesh.command.validator.CommandValidator;
import org.aesh.readline.terminal.formatting.TerminalString;
import org.aesh.readline.util.Parser;
import org.aesh.selector.SelectorType;
import org.aesh.terminal.utils.Config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author <a href="mailto:stale.pedersen@jboss.org">Ståle W. Pedersen</a>
 */
public class ProcessedCommand<C extends Command<CI>, CI extends CommandInvocation> {

    private final String name;
    private final String description;
    private final CommandValidator<C,CI> validator;
    private final ResultHandler resultHandler;
    private final CommandPopulator<Object,CI> populator;
    private final boolean disableParsing;
    private CommandActivator activator;
    private final boolean generateHelp;
    private String version;

    private List<ProcessedOption> options;
    private ProcessedOption arguments;
    private ProcessedOption argument;
    private final C command;
    private final List<String> aliases;
    private List<CommandLineParserException> parserExceptions;
    private CompleteStatus completeStatus;

    public ProcessedCommand(String name, List<String> aliases, C command,
                            String description, CommandValidator<C,CI> validator,
                            ResultHandler resultHandler,
                            boolean generateHelp, boolean disableParsing,
                            String version,
                            ProcessedOption arguments, List<ProcessedOption> options,
                            ProcessedOption argument,
                            CommandPopulator<Object,CI> populator, CommandActivator activator) throws OptionParserException {
        this.name = name;
        this.description = description;
        this.aliases = aliases == null ? Collections.emptyList() : aliases;
        this.validator = validator;
        this.generateHelp = generateHelp;
        this.disableParsing = disableParsing;
        if(resultHandler != null)
            this.resultHandler = resultHandler;
        else
            this.resultHandler = new NullResultHandler();
        this.arguments = arguments;
        this.argument = argument;
        if(argument != null && arguments != null)
            throw new OptionParserException("Argument and Arguments cannot be defined in the same Command");
        this.options = new ArrayList<>();
        this.command = command;
        this.activator = activator == null ? new NullCommandActivator() : activator;
        if(populator == null)
            this.populator = new AeshCommandPopulator<>(this.command);
        else
            this.populator = populator;
        setOptions(options);

        if(generateHelp)
            doGenerateHelp();

        if(version != null && version.length() > 0) {
            this.version = version;
            doGenerateVersion();
        }

        parserExceptions = new ArrayList<>();
    }

    public List<ProcessedOption> getOptions() {
        return options;
    }

    public CommandActivator getActivator() {
        return activator;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public void addOption(ProcessedOption opt) throws OptionParserException {
        this.options.add(new ProcessedOption(verifyThatNamesAreUnique(opt.shortName(), opt.name()), opt.name(),
                opt.description(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(), opt.askIfNotSet(), opt.selectorType(),
                opt.getDefaultValues(), opt.type(), opt.getFieldName(), opt.getOptionType(), opt.converter(),
                opt.completer(), opt.validator(), opt.activator(), opt.getRenderer(), opt.parser(), opt.doOverrideRequired()));

        options.get(options.size()-1).setParent(this);
    }

    private void setOptions(List<ProcessedOption> options) throws OptionParserException {
        for(ProcessedOption opt : options) {
            this.options.add(new ProcessedOption(verifyThatNamesAreUnique(opt.shortName(), opt.name()), opt.name(),
                    opt.description(), opt.getArgument(), opt.isRequired(), opt.getValueSeparator(), opt.askIfNotSet(), opt.selectorType(),
                    opt.getDefaultValues(), opt.type(), opt.getFieldName(), opt.getOptionType(),
                    opt.converter(), opt.completer(), opt.validator(), opt.activator(), opt.getRenderer(),
                    opt.parser(), opt.doOverrideRequired()));

            this.options.get(this.options.size()-1).setParent(this);
        }
    }

    public String name() {
        return name;
    }

    public String description() {
        return description;
    }

    public CommandValidator<C,CI> validator() {
        return validator;
    }

    public ResultHandler resultHandler() {
      return resultHandler;
    }

    public boolean hasArguments() {
        return arguments != null && arguments.hasMultipleValues();
    }

    public ProcessedOption getArguments() {
        return arguments;
    }

    public void setArguments(ProcessedOption arguments) {
        this.arguments = arguments;
        this.arguments.setParent(this);
    }

    public CommandPopulator<Object, CI> getCommandPopulator() {
        return populator;
    }

    public C getCommand() {
        return command;
    }

    public boolean generateHelp() {
        return generateHelp;
    }

    public boolean disableParsing() {
        return disableParsing;
    }

    public String version() {
        return version;
    }

    private char verifyThatNamesAreUnique(String name, String longName) throws OptionParserException {
        if(name != null)
            return verifyThatNamesAreUnique(name.charAt(0), longName);
        else
            return verifyThatNamesAreUnique('\u0000', longName);
    }

    private char verifyThatNamesAreUnique(char name, String longName) throws OptionParserException {
        if(longName != null && longName.length() > 0 && findLongOption(longName) != null) {
            throw new OptionParserException("Option --"+longName+" is already added to Param: "+this.toString());
        }
        if(name != '\u0000' && findOption(String.valueOf(name)) != null) {
            throw new OptionParserException("Option -"+name+" is already added to Param: "+this.toString());
        }

        //if name is null, use one based on name
        if(name == '\u0000' && (longName == null || longName.length() == 0))
            throw new OptionParserException("Neither option name and option long name can be both null");

        return name;
    }

    private char findPossibleName(String longName) throws OptionParserException {
```java
    public String printHelp(String commandName) {
        int maxLength = 0;
        int width = 80;
        List<ProcessedOption> opts = getOptions();
        for (ProcessedOption o : opts) {
            if(o.getFormattedLength() > maxLength)
                maxLength = o.getFormattedLength();
        }

        StringBuilder sb = new StringBuilder();
        //first line
        sb.append("Usage: ");
        if(commandName == null || commandName.length() == 0)
            sb.append(name());
        else
            sb.append(commandName);
        if(opts.size() > 0)
            sb.append(" [<options>]");

        if(argument != null) {
            if(argument.isTypeAssignableByResourcesOrFile())
                sb.append(" <file>");
            else
                sb.append(" <").append(argument.getFieldName()).append(">");
        }

        if(arguments != null) {
            if(arguments.isTypeAssignableByResourcesOrFile())
                sb.append(" [<files>]");
            else
                sb.append(" [<").append(arguments.getFieldName()).append(">]");
        }
        sb.append(Config.getLineSeparator());
        //second line
        sb.append(description()).append(Config.getLineSeparator());

        appendOptionsAndArgumentsHelp(sb, opts, maxLength, width);

        return sb.toString();
    }

    private void appendOptionsAndArgumentsHelp(StringBuilder sb, List<ProcessedOption> opts, int maxLength, int width) {
        //options and arguments
        if (opts.size() > 0)
            sb.append(Config.getLineSeparator()).append("Options:").append(Config.getLineSeparator());
        for (ProcessedOption o : opts)
            sb.append(o.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
        if(arguments != null) {
            sb.append(Config.getLineSeparator()).append("Arguments:").append(Config.getLineSeparator());
            sb.append(arguments.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
        }
        if(argument != null) {
            sb.append(Config.getLineSeparator()).append("Argument:").append(Config.getLineSeparator());
            sb.append(argument.getFormattedOption(2, maxLength+4, width)).append(Config.getLineSeparator());
        }
    }
