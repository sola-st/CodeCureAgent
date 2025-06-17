Is the rule violation correctly raised here? Does the problem that the rule points at apply in this case?

### Example of correctly raised rule violation

In the following example (unrelated to your specific task) SonarQube raised a 'S3358': 'Ternary operators should not be nested' 'Extract this nested ternary operation into an independent statement.'  
The relevant code snippet is the following:

```
    private String formatUnrecognizedArgumentErrorMessage(ParseState state,
            String args) {
        return String
                .format(TextHelper.LOCALE_ROOT,
                        localize("unrecognizedArgumentsError"),
                        args,
                        state.index > state.lastFromFileArgIndex ? ""
                                : String.format(
                                        TextHelper.LOCALE_ROOT,
                                        localize("trailingWhiteSpacesInFileTip"),
                                        config_.fromFilePrefixPattern_.getPrefixChars()     // Here a violation of SonarQube rule S3358 is raised
                                                .length() == 1 ? config_.fromFilePrefixPattern_
                                                .getPrefixChars() : "["
                                                + config_.fromFilePrefixPattern_
                                                        .getPrefixChars() + "]"));
    }
```

In this example the rule violation is correctly raised by the SonarQube scanner, as there is indeed a ternary operator nested into another ternary operator at the marked line.  
This reduces code readability and maintainability, which the rule S3358 aims to prevent.

### Example of incorrectly raised rule violation

In the following example (unrelated to your specific task) SonarQube raised a 'S2583': 'Conditionally executed code should be reachable' 'Change this condition so that it does not always evaluate to false'  
The relevant code snippet is the following:

```
...
            if (!verifyStep(verifyPermissions)) {       // Here a violation of SonarQube rule S2583 is raised
                nextFileVerification.verify(parser, arg, file);
            }
        }
    }

    private boolean verifyStep(FileVerificationStep step) throws
            ArgumentParserException {
        boolean result = true;
        try {
            step.verify();
        } catch (ArgumentParserException e) {
            if (nextFileVerification == null) {
                throw e;
            } else {
                result = false;
            }
        }
        return result;
    }
...
```

This is an example, where the violation is actually raised incorrectly. SonarQube states here that the condition `!verifyStep(verifyPermissions)` would always evaluate to false.  
However, there is a case where `verifyStep(verifyPermissions)` can return false and so the condition becomes true. This happens if the call `step.verify();` raises an ArgumentParserException (which it can, as inspecting its implementation via find_definition shows).  
The SonarQube scanner's rule implementation fails to capture this and therefore incorrectly raises the violation in this example.