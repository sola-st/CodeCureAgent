from autogpt.logs import logger

# Classes for tracking changes applied via apply_changes in write_fix.py


class FileChanges():

    def __init__(self, file_path: str, lines_before_change: list[str]):
        self.file_path = file_path
        self.change_tracked_lines: ChangeTrackedList = ChangeTrackedList(
            lines_before_change)

    def __repr__(self):
        return f"FileChanges(file_path='{self.file_path}', change_tracked_lines={repr(self.change_tracked_lines)})"


class ChangeTrackedList(list):
    """
    Remembers how the list was originally at initialization.
    Manages a list of tuples that tracks what line in the modified list corresponds to which line in the original list.

    Fields:
        lines_before_change (list[str]): the initial state of the list when creating it
        map_line_indices_before_after_change (list[BeforeAfterMapping]): Tracks the line correspondences.
        paired_insertions_and_deletions (list[tuple[BeforeAfterMapping, BeforeAfterMapping]]): List of pairings of insertions and deletions, that resemble a kind of modification (inserted and deleted at the same line number)
    """

    def __init__(self, lines_before_change: list[str]):
        super().__init__(lines_before_change)

        self.lines_before_change = lines_before_change

        self.map_line_indices_before_after_change = [
            BeforeAfterMapping(i + 1, i + 1) for i, line in enumerate(lines_before_change)]

        self.paired_insertions_and_deletions: list[tuple[BeforeAfterMapping, BeforeAfterMapping]] = [
        ]

    def __repr__(self):
        return super().__repr__() + "with fields: 'lines_before_change'=" + repr(self.lines_before_change) + " 'map_line_indices_before_after_change'" + repr(self.map_line_indices_before_after_change)

    def update(self, index: int, object: any):
        self[index] = object

        line_mappings_of_line = list(filter(
            lambda line_mapping: line_mapping.before_line == index + 1, self.map_line_indices_before_after_change))

        if len(line_mappings_of_line) == 1:
            line_mapping_of_line = line_mappings_of_line[0]
            line_mapping_of_line.modified = True
        else:
            logger.error(
                f"Expected to find only one before after mapping with before_line {index + 1}", "This should never happen.")

    def insert(self, index: int, object: any):
        super().insert(index, object)

        # Move remembered line correspondence by one line forward for all lines after the inserted one
        for line_number in reversed(range(index + 1, len(self))):
            found_items = filter(lambda mapping, line_number=line_number: mapping.after_line ==
                                 line_number, self.map_line_indices_before_after_change)
            for item in found_items:
                item.after_line += 1

        # Only insert after updating the other line numbers
        inserted_mapping = BeforeAfterMapping(-1, index + 1)
        inserted_mapping.inserted = True
        inserted_mapping.inserted_at_line = index + 1
        self.map_line_indices_before_after_change.insert(
            index, inserted_mapping)

    def append(self, object: any):
        super().append(object)

        appended_mapping = BeforeAfterMapping(-1, len(self))
        appended_mapping.inserted = True
        appended_mapping.inserted_at_line = len(self)
        self.map_line_indices_before_after_change.append(appended_mapping)

    def pop(self, index: int) -> any:
        removed_line = super().pop(index)

        # Set to deleted
        found_items = filter(lambda mapping, line_number=index + 1: mapping.after_line ==
                             line_number, self.map_line_indices_before_after_change)

        # Might be multiple once if consecutive lines are being deleted
        for item in found_items:
            item.deleted = True

        # Move remembered line correspondence by one line back for all lines after the deleted one
        for line_number in range(index + 2, len(self) + 2):
            found_items = filter(lambda mapping, line_number=line_number: mapping.after_line ==
                                 line_number, self.map_line_indices_before_after_change)
            for item in found_items:
                item.after_line -= 1

        return removed_line

    def find_insertion_deletion_pairs_forming_modification(self):
        """
        Remember pairs of insertions and deletions of the same line number (considered as a kind of modification)
        """
        insertions = filter(lambda mapping: mapping.inserted,
                            self.map_line_indices_before_after_change)

        for insertion in insertions:
            matched_deletions = list(filter(lambda mapping, insertion=insertion: mapping.deleted and mapping.before_line ==
                                     insertion.inserted_at_line, self.map_line_indices_before_after_change))

            # Can only be of length 0 or 1
            if len(matched_deletions) == 1:
                self.paired_insertions_and_deletions.append(
                    (insertion, matched_deletions[0]))


class BeforeAfterMapping():

    def __init__(self, before_line: int, after_line: int):
        self.before_line = before_line
        self.after_line = after_line
        self.inserted = False
        self.inserted_at_line = -1
        self.modified = False
        self.deleted = False

    def __repr__(self):
        return f"({str(self.before_line)}, {str(self.after_line)}, inserted={self.inserted}, modified={self.modified}, deleted={self.deleted})"
