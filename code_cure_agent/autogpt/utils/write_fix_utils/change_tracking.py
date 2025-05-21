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
                                                                                            before_index is -1 if the line was newly added. after_index is -1 if the line was removed.
    """

    def __init__(self, lines_before_change: list[str]):
        super().__init__(lines_before_change)

        self.lines_before_change = lines_before_change

        self.map_line_indices_before_after_change = [
            BeforeAfterMapping(i + 1, i + 1) for i, line in enumerate(lines_before_change)]

    def __repr__(self):
        return super().__repr__() + "with fields: 'lines_before_change'=" + repr(self.lines_before_change) + " 'map_line_indices_before_after_change'" + repr(self.map_line_indices_before_after_change)

    def insert(self, index: int, object: any):
        super().insert(index, object)

        map_only_after_lines = list(
            map(lambda map_lines: map_lines.after_line, self.map_line_indices_before_after_change))

        # Move remembered line correspondence by one line forward for all lines after the inserted one
        for line_number in range(index + 1, len(self)):
            self.map_line_indices_before_after_change[map_only_after_lines.index(
                line_number)].after_line += 1

        # Only insert after updating the other line numbers
        self.map_line_indices_before_after_change.insert(
            index, BeforeAfterMapping(-1, index + 1))

    def append(self, object: any):
        super().append(object)

        self.map_line_indices_before_after_change.append(
            BeforeAfterMapping(-1, len(self)))

    def pop(self, index: int):
        super().pop(index)

        # Move remembered line correspondence by one line back for all lines after the deleted one
        map_only_after_lines = list(
            map(lambda map_lines: map_lines.after_line, self.map_line_indices_before_after_change))

        for line_number in range(index + 2, len(self) + 2):
            self.map_line_indices_before_after_change[map_only_after_lines.index(
                line_number)].after_line -= 1

        # Set to -1 after updating the succeeding lines
        self.map_line_indices_before_after_change[map_only_after_lines.index(
            index + 1)].after_line = -1


class BeforeAfterMapping():

    def __init__(self, before_line: int, after_line: int):
        self.before_line = before_line
        self.after_line = after_line

    def __repr__(self):
        return f"({str(self.before_line)}, {str(self.after_line)})"
