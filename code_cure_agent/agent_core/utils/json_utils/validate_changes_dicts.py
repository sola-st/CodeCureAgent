from agent_core.logs import logger

error_title = "apply_changes failed"


def validate_changes_dicts_in_correct_format(changes_dicts: list[dict]) -> None:
    """
    Validate the correctness of the changes_dicts.
    We manually check everything here, instead of using a schema checker, in order to be able to provide fine-tuned feedback to the agent, instead of generic and potentially confusing messages.

    Raises:
    ValidateChangesDictsError: For any kind of wrong format of the changes_dicts.
    """

    if type(changes_dicts) is not list:
        logger.error(error_title,
                     f"The write_fix command was in a wrong format. 'changes_dicts' was not an array, but instead of type {type(changes_dicts).__name__}. changes_dicts: " + str(changes_dicts))
        raise ValidateChangesDictsError(
            f"The write_fix command was in a wrong format. 'changes_dicts' was not an array, but instead of type {type(changes_dicts).__name__}. Strictly follow the specified format of the fix."
        )

    if len(changes_dicts) == 0:
        logger.error(error_title,
                     "The write_fix command was in a wrong format. The changes_dicts array was empty.")
        raise ValidateChangesDictsError(
            "The fix you passed is empty. Please provide a non empty implementation of the fix."
        )

    for changes_dict in changes_dicts:
        validate_single_changes_dict(changes_dict, changes_dicts)


def validate_single_changes_dict(changes_dict: dict, changes_dicts: list[dict]):
    if type(changes_dict) is not dict:
        logger.error(error_title,
                     f"The write_fix command was in a wrong format. The elements in the 'changes_dicts' array must be objects. But at least one of the elements was of type {type(changes_dict).__name__}. changes_dicts: " + str(changes_dicts))
        raise ValidateChangesDictsError(
            f"The write_fix command was in a wrong format. The elements in the 'changes_dicts' array must be objects. But at least one of the elements was of type {type(changes_dict).__name__}. Strictly follow the specified format of the fix."
        )

    for key in changes_dict.keys():
        if key not in ["file_name", "insertions", "deletions", "modifications"]:
            logger.error(error_title,
                         f"The write_fix command was in a wrong format. At least one of the file-level change objects in changes_dicts had an unknown key '{key}'. Only 'file_name', 'insertions' and 'deletions' are allowed as top-level keys. changes_dict: " + str(changes_dict))
            raise ValidateChangesDictsError(
                f"The write_fix command was in a wrong format. At least one of the file-level change objects in changes_dicts had an unknown key '{key}'. Only 'file_name', 'insertions' and 'deletions' are allowed as top-level keys. Strictly follow the specified format of the fix."
            )

    validate_file_name(changes_dict)

    insertions = changes_dict.get("insertions", None)
    validate_insertions(insertions, changes_dict)

    deletions = changes_dict.get("deletions", None)
    validate_deletions(deletions, changes_dict)

    modifications = changes_dict.get("modifications", None)

    if all(element is None for element in [insertions, deletions, modifications]):
        logger.error(error_title,
                     "The write_fix command was in a wrong format. Neither `insertions` nor `deletions` was given in one of the elements in changes_dicts. changes_dict: " + str(changes_dict))
        raise ValidateChangesDictsError(
            "The write_fix command was in a wrong format. Neither `insertions` nor `deletions` was given in one of the elements in changes_dicts."
        )


def validate_file_name(changes_dict: dict):
    file_name = changes_dict.get("file_name", None)
    if file_name is None:
        logger.error(error_title,
                     "The write_fix command was in a wrong format. Couldn't find `file_name` in one of the file-level change objects. change_dict: " + str(changes_dict))
        raise ValidateChangesDictsError(
            "The write_fix command was in a wrong format. Couldn't find `file_name` in one of the file-level change objects. Strictly follow the specified format of the fix.")
    elif type(file_name) is not str:
        logger.error(error_title,
                     f"The write_fix command was in a wrong format. `file_name` in one of the file-level change objects was of type {type(file_name).__name__} instead of string. change_dict: " + str(changes_dict))
        raise ValidateChangesDictsError(
            f"The write_fix command was in a wrong format. `file_name` in one of the file-level change objects was of type {type(file_name).__name__} instead of string. Strictly follow the specified format of the fix.")


def validate_insertions(insertions, changes_dict: dict):

    if insertions is not None:
        if type(insertions) is not list:
            logger.error(error_title,
                         f"The write_fix command was in a wrong format. 'insertions' must be an array of insertion objects. But at least one of the 'insertions' keys of file-level change objects in changes_dicts had a value of type {type(insertions).__name__}. changes_dict: " + str(changes_dict))
            raise ValidateChangesDictsError(
                f"The write_fix command was in a wrong format. 'insertions' must be an array of insertion objects. But at least one of the 'insertions' keys of file-level change objects in changes_dicts had a value of type {type(insertions).__name__}."
            )
        for insertion in insertions:
            if type(insertion) is not dict:
                logger.error(error_title,
                             f"The write_fix command was in a wrong format. The elements in the 'insertions' array must be objects. But at least one of the elements of an array of 'insertions' was of type {type(insertion).__name__}. changes_dict: " + str(changes_dict))
                raise ValidateChangesDictsError(
                    f"The write_fix command was in a wrong format. The elements in the 'insertions' array must be objects. But at least one of the elements of an array of 'insertions' was of type {type(insertion).__name__}. Strictly follow the specified format of the fix."
                )
            for key in insertion.keys():
                if key not in ["line_number", "new_lines"]:
                    logger.error(error_title,
                                 f"The write_fix command was in a wrong format. At least one insertion object had an unknown key '{key}'. Only 'line_number' and 'new_lines' are allowed. The problematic insertion object was: {str(insertion)}.")
                    raise ValidateChangesDictsError(
                        f"The write_fix command was in a wrong format. At least one insertion object had an unknown key '{key}'. Only 'line_number' and 'new_lines' are allowed. Strictly follow the specified format of the fix.")

            validate_line_number_in_insertion(
                insertion, insertions)

            validate_new_lines_in_insertion(
                insertion, insertions)


def validate_line_number_in_insertion(insertion: dict, insertions):
    line_number = insertion.get("line_number", None)
    if line_number is None:
        logger.error(error_title,
                     f"The write_fix command was in a wrong format. At least one insertion object had no 'line_number'. The problematic insertion object was: {str(insertion)}. All insertions in the file-object: {str(insertions)}")
        raise ValidateChangesDictsError(
            "The write_fix command was in a wrong format. At least one insertion object had no 'line_number'. Strictly follow the specified format of the fix.")
    elif type(line_number) is not int:
        logger.error(error_title,
                     f"The type of the key 'line_number' of at least one insertion object was {type(line_number).__name__}. However, the 'line_number' must be of type int. The problematic insertion object was: {str(insertion)}. All insertions in the file-object: {str(insertions)}")
        raise ValidateChangesDictsError(
            f"The write_fix command was in a wrong format. The type of the key 'line_number' of at least one insertion object was {type(line_number).__name__}. However, the 'line_number' must be of type int. Strictly follow the specified format of the fix.")


def validate_new_lines_in_insertion(insertion: dict, insertions):
    new_lines = insertion.get("new_lines", None)
    if new_lines is None:
        logger.error(error_title,
                     f"The write_fix command was in a wrong format. At least one insertion object had no 'new_lines'. The problematic insertion object was: {str(insertion)}. All insertions in the file-object: {str(insertions)}")
        raise ValidateChangesDictsError(
            "The write_fix command was in a wrong format. At least one insertion object had no 'new_lines'. Strictly follow the specified format of the fix.")
    elif type(new_lines) is not list:
        logger.error(error_title,
                     f"The write_fix command was in a wrong format. The type of the key 'new_lines' of at least one insertion object was {type(new_lines).__name__}. However, the 'new_lines' must be an array of strings. The problematic insertion object was: {str(insertion)}. All insertions in the file-object: {str(insertions)}")
        raise ValidateChangesDictsError(
            f"The write_fix command was in a wrong format. The type of the key 'new_lines' of at least one insertion object was {type(new_lines).__name__}. However, the 'new_lines' must be an array of strings. Strictly follow the specified format of the fix.")
    for new_line in new_lines:
        if type(new_line) is not str:
            logger.error(error_title,
                         f"The write_fix command was in a wrong format. At least one item in one of the 'new_lines' lists was not of type string but instead {type(new_line).__name__}. The problematic insertion object was: {str(insertion)}. All insertions in the file-object: {str(insertions)}")
            raise ValidateChangesDictsError(
                f"The write_fix command was in a wrong format. At least one item in one of the 'new_lines' lists was not of type string but instead {type(new_line).__name__}. Strictly follow the specified format of the fix.")


def validate_deletions(deletions, changes_dict: dict):
    if deletions is not None:
        if type(deletions) is not list:
            logger.error(error_title,
                         f"The write_fix command was in a wrong format. 'deletions' must be an array of integer numbers. But at least one of the 'deletions' keys of file-level change objects in changes_dicts had a value of type {type(deletions).__name__}. changes_dict: " + str(changes_dict))
            raise ValidateChangesDictsError(
                f"The write_fix command was in a wrong format. 'deletions' must be an array of integer numbers. But at least one of the 'deletions' keys of file-level change objects in changes_dicts had a value of type {type(deletions).__name__}. Strictly follow the specified format of the fix."
            )
        for deletion in deletions:
            if type(deletion) is not int:
                logger.error(error_title,
                             f"The write_fix command was in a wrong format. 'deletions' must be an array of integer numbers. But at least one of the 'deletions' arrays of file-level change objects in changes_dicts had an element of type {type(deletion).__name__}. changes_dict: " + str(changes_dict))
                raise ValidateChangesDictsError(
                    f"The write_fix command was in a wrong format. 'deletions' must be an array of integer numbers. But at least one of the 'deletions' arrays of file-level change objects in changes_dicts had an element of type {type(deletion).__name__}. Strictly follow the specified format of the fix."
                )


class ValidateChangesDictsError(Exception):
    def __init__(self, msg):
        super().__init__(msg)
        self.msg = msg
