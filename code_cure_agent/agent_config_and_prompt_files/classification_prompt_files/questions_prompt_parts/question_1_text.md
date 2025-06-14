Is the rule violation correctly raised here? Does the problem that the rule points at apply in this case?

### Example correctly raised

In the following example (unrelated to your specific task) SonarQube raised a 'S3358': 'Ternary operators should not be nested' 'Extract this nested ternary operation into an independent statement.' at line number 899 of the following code snippet:

```
Line 889:    private String formatUnrecognizedArgumentErrorMessage(ParseState state,
Line 890:            String args) {
Line 891:        return String
Line 892:                .format(TextHelper.LOCALE_ROOT,
Line 893:                        localize("unrecognizedArgumentsError"),
Line 894:                        args,
Line 895:                        state.index > state.lastFromFileArgIndex ? ""
Line 896:                                : String.format(
Line 897:                                        TextHelper.LOCALE_ROOT,
Line 898:                                        localize("trailingWhiteSpacesInFileTip"),
Line 899:                                        config_.fromFilePrefixPattern_.getPrefixChars()
Line 900:                                                .length() == 1 ? config_.fromFilePrefixPattern_
Line 901:                                                .getPrefixChars() : "["
Line 902:                                                + config_.fromFilePrefixPattern_
Line 903:                                                        .getPrefixChars() + "]"));
Line 904:    }
```

In this example the rule violation is correctly raised by the SonarQube scanner, as there is indeed a ternary operator nested into another ternary operator at line 899.  
This reduces code readability and maintainability, which the rule S3358 aims to prevent.

### Example incorrectly raised

In the following example (unrelated to your specific task) SonarQube raised a 'S2583': 'Conditionally executed code should be reachable' 'Change this condition so that it does not always evaluate to false' at line number 76 of the following code snippet:

```
...
Line 76:            if (!verifyStep(verifyPermissions)) {
Line 77:                nextFileVerification.verify(parser, arg, file);
Line 78:            }
Line 79:        }
Line 80:    }
Line 81:
Line 82:    private boolean verifyStep(FileVerificationStep step) throws
Line 83:            ArgumentParserException {
Line 84:        boolean result = true;
Line 85:        try {
Line 86:            step.verify();
Line 87:        } catch (ArgumentParserException e) {
Line 88:            if (nextFileVerification == null) {
Line 89:                throw e;
Line 90:            } else {
Line 91:                result = false;
Line 92:            }
Line 93:        }
Line 94:        return result;
Line 95:    }
...
```

This is an example, where the violation is actually raised incorrectly. SonarQube states here that the condition `!verifyStep(verifyPermissions)` would always evaluate to false.  
However, there is a case where `verifyStep(verifyPermissions)` can return false and so the condition becomes true. This happens if the call `step.verify();` raises an ArgumentParserException (which it can, as inspecting its implementation shows).  
The SonarQube scanner's rule implementation fails to capture this and therefore incorrectly raises the violation in this example.