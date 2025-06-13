import unittest

import os
import shutil

from tests.agent_mock import AgentMock

from agent_core.commands.repository_operations import checkout_project
from agent_core.commands import write_fix
from agent_core.utils.write_fix_utils.change_tracking import FileChanges


class WriteFixTestCase(unittest.TestCase):
    def setUp(self):
        cca_workspace = "cca_workspace"
        if os.path.exists(cca_workspace):
            shutil.rmtree(cca_workspace)
        os.mkdir(cca_workspace)

        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test")
        os.mkdir("experimental_setups/experiment_test/fix_tp")
        os.mkdir("experimental_setups/experiment_test/fix_tp/analysis_reports")
        os.mkdir("experimental_setups/experiment_test/fix_tp/plausible_patches")
        os.mkdir("experimental_setups/experiment_test/fix_tp/implausible_patches")

        warning_repository_URL = "https://github.com/argparse4j/argparse4j.git"
        warning_repository_commit = "a0cef432451487d513382297cec2c5b14c147a30"
        warning_repository_name = "argparse4j"
        warning_file_path = "main/src/main/java/net/sourceforge/argparse4j/internal/TerminalWidth.java"
        warning_rule_key = "S2142"
        warning_start_line = 94
        warning_rule_name = "'InterruptedException' should not be ignored"
        warning_specific_message = "Either re-interrupt this method or rethrow the ""InterruptedException"" that can be caught here."

        self.agent = AgentMock(warning_repository_URL, warning_repository_commit, warning_file_path,
                               warning_repository_name, warning_rule_key, warning_start_line, warning_rule_name, warning_specific_message)

        checkout_project(self.agent)

        with open(os.path.join(self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name, self.agent.ai_config.warning_file_path)) as orginal_file:
            self.file_without_changes = orginal_file.readlines()

    def tearDown(self):
        cca_workspace = "cca_workspace"
        if os.path.exists(cca_workspace):
            shutil.rmtree(cca_workspace)
        os.mkdir(cca_workspace)
        if os.path.exists("experimental_setups/experiment_test"):
            shutil.rmtree("experimental_setups/experiment_test")

    def test_write_fix(self):
        changes_dict = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        },
            {
            "file_name": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java",
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }]
        print(write_fix.write_fix(changes_dict, self.agent))

    def test_write_fix_apply_changes_error_handled(self):
        changes_dict = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 140,
                "modified_line": "    modified_there\n"
            }]
        },
            {
            "file_name": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java",
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }]
        print(write_fix.write_fix(changes_dict, self.agent))

    def test_write_fix_wrong_file_name_key_in_second_dict(self):
        changes_dict = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        },
            {
            "file_path": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java",
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }]
        result = write_fix.write_fix(changes_dict, self.agent)

        self.assertEqual(
            result, "REJECTED  \nFailure when trying to apply the fix: The write_fix command was in a wrong format. Couldn't find `file_name` in the change_dict.  \n\nIMPORTANT: The repository has been restored to its original state! You need to start applying changes from scratch again.")

    def test_write_fix_non_existent_file_name_key_in_second_dict(self):
        changes_dict = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        },
            {
            "file_name": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl2.java",
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }]
        result = write_fix.write_fix(changes_dict, self.agent)
        self.assertEqual(result, """REJECTED  \nFailure when trying to apply the fix: The file_path main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl2.java does not exist.  \n\nIMPORTANT: The repository has been restored to its original state! You need to start applying changes from scratch again.""")

    def test_write_fix_no_insertions_deletions_and_modification_keys_in_second_dict(self):
        changes_dict = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        },
            {
            "file_name": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java"
        }]
        result = write_fix.write_fix(changes_dict, self.agent)

        self.assertEqual(
            result, "REJECTED  \nFailure when trying to apply the fix: The write_fix command was in a wrong format. Neither `insertions`, `deletions` nor `modifications` was given in the change_dict.  \n\nIMPORTANT: The repository has been restored to its original state! You need to start applying changes from scratch again.")

    def test_execute_write_range_multiple_dicts_with_same_file_path(self):
        changes_dict = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            }]
        }, {
            "file_name": self.agent.ai_config.warning_file_path,
            "deletions": [1],
            "modifications": [
                {
                    "line_number": 11,
                    "modified_line": "    modified_there\n"
                }]
        },
            {
            "file_name": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java",
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }]
        result_multiple_dicts_with_same_file = write_fix.execute_write_range(
            changes_dict, self.agent)

        changes_dict_to_test_against = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        },
            {
            "file_name": "main/src/main/java/net/sourceforge/argparse4j/internal/SubparsersImpl.java",
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }]
        self.tearDown()
        self.setUp()
        expected_result = write_fix.execute_write_range(
            changes_dict_to_test_against, self.agent)

        self.assertEqual(
            result_multiple_dicts_with_same_file[0].change_tracked_lines, expected_result[0].change_tracked_lines)

    def test_apply_changes_no_changes(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [],
            "deletions": [],
            "modifications": []
        }]

        file_relative_path = self.agent.ai_config.warning_file_path

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        change_result: FileChanges = write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)

        self.assertEqual(
            list(change_result.change_tracked_lines), self.file_without_changes)

    def test_apply_changes_delete_line_before_a_modified_line_and_insert_line_before_modified_line_changes(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }]

        file_relative_path = self.agent.ai_config.warning_file_path

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        change_result: FileChanges = write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)

        with open("tests/test_write_fix_expected_outputs/expected_apply_changes_delete_line_before_a_modified_line_and_insert_line_before_modified_line_changes.txt") as expected_file:
            expected_result = expected_file.readlines()

        resulting_map = [(map.before_line, map.after_line)
                         for map in change_result.change_tracked_lines.map_line_indices_before_after_change]
        print(resulting_map)

        self.assertEqual(
            list(change_result.change_tracked_lines), expected_result)

        self.assertEqual(change_result.file_path,
                         self.agent.ai_config.warning_file_path)
        map_line_indices_before_after_change = change_result.change_tracked_lines.map_line_indices_before_after_change
        index_element_before_line_10 = list(map(
            lambda correspondance: correspondance.before_line, map_line_indices_before_after_change)).index(10)
        index_element_before_line_2 = list(map(
            lambda correspondance: correspondance.before_line, map_line_indices_before_after_change)).index(2)

        # Assert that line 10 from before has moved to line 11 by inserting two lines to line 10 and deleting line 1
        self.assertEqual(
            map_line_indices_before_after_change[index_element_before_line_10].after_line, 11)

        # Assert that line 2 from before moved to line 1
        self.assertEqual(
            map_line_indices_before_after_change[index_element_before_line_2].after_line, 1)

        self.assertEqual(resulting_map, [(1, 1), (2, 1), (3, 2), (4, 3), (5, 4), (6, 5), (7, 6), (8, 7), (9, 8), (-1, 9), (-1, 10), (10, 11), (11, 12), (12, 13), (13, 14), (14, 15), (15, 16), (16, 17), (17, 18), (18, 19), (19, 20), (20, 21), (21, 22), (22, 23), (23, 24), (24, 25), (25, 26), (26, 27), (27, 28), (28, 29), (29, 30), (30, 31), (31, 32), (32, 33), (33, 34), (34, 35), (35, 36), (36, 37), (37, 38), (38, 39), (39, 40), (40, 41), (41, 42), (42, 43), (43, 44), (44, 45), (45, 46), (46, 47), (47, 48), (48, 49), (49, 50), (50, 51), (51, 52), (52, 53), (53, 54), (54, 55), (55, 56), (56, 57), (57, 58), (58, 59), (59, 60), (60, 61), (61, 62), (62, 63), (63, 64), (64, 65), (65, 66), (66, 67), (67, 68), (68, 69),
                         (69, 70), (70, 71), (71, 72), (72, 73), (73, 74), (74, 75), (75, 76), (76, 77), (77, 78), (78, 79), (79, 80), (80, 81), (81, 82), (82, 83), (83, 84), (84, 85), (85, 86), (86, 87), (87, 88), (88, 89), (89, 90), (90, 91), (91, 92), (92, 93), (93, 94), (94, 95), (95, 96), (96, 97), (97, 98), (98, 99), (99, 100), (100, 101), (101, 102), (102, 103), (103, 104), (104, 105), (105, 106), (106, 107), (107, 108), (108, 109), (109, 110), (110, 111), (111, 112), (112, 113), (113, 114), (114, 115), (115, 116), (116, 117), (117, 118), (118, 119), (119, 120), (120, 121), (121, 122), (122, 123), (123, 124), (124, 125), (125, 126), (126, 127), (127, 128), (128, 129), (129, 130), (130, 131), (131, 132), (132, 133), (133, 134)])

    def test_apply_changes_delete_insert_and_modify_last_line(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 133,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [133],
            "modifications": [{
                "line_number": 133,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 133,
                "modified_line": "    modified_there\n"
            }]
        }]

        file_relative_path = self.agent.ai_config.warning_file_path

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        change_result: FileChanges = write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)

        resulting_map = [(map.before_line, map.after_line)
                         for map in change_result.change_tracked_lines.map_line_indices_before_after_change]
        print(resulting_map)

        self.assertEqual(resulting_map, [(1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (11, 11), (12, 12), (13, 13), (14, 14), (15, 15), (16, 16), (17, 17), (18, 18), (19, 19), (20, 20), (21, 21), (22, 22), (23, 23), (24, 24), (25, 25), (26, 26), (27, 27), (28, 28), (29, 29), (30, 30), (31, 31), (32, 32), (33, 33), (34, 34), (35, 35), (36, 36), (37, 37), (38, 38), (39, 39), (40, 40), (41, 41), (42, 42), (43, 43), (44, 44), (45, 45), (46, 46), (47, 47), (48, 48), (49, 49), (50, 50), (51, 51), (52, 52), (53, 53), (54, 54), (55, 55), (56, 56), (57, 57), (58, 58), (59, 59), (60, 60), (61, 61), (62, 62), (63, 63), (64, 64), (65, 65), (66, 66), (67, 67), (68, 68), (69, 69), (70, 70), (
            71, 71), (72, 72), (73, 73), (74, 74), (75, 75), (76, 76), (77, 77), (78, 78), (79, 79), (80, 80), (81, 81), (82, 82), (83, 83), (84, 84), (85, 85), (86, 86), (87, 87), (88, 88), (89, 89), (90, 90), (91, 91), (92, 92), (93, 93), (94, 94), (95, 95), (96, 96), (97, 97), (98, 98), (99, 99), (100, 100), (101, 101), (102, 102), (103, 103), (104, 104), (105, 105), (106, 106), (107, 107), (108, 108), (109, 109), (110, 110), (111, 111), (112, 112), (113, 113), (114, 114), (115, 115), (116, 116), (117, 117), (118, 118), (119, 119), (120, 120), (121, 121), (122, 122), (123, 123), (124, 124), (125, 125), (126, 126), (127, 127), (128, 128), (129, 129), (130, 130), (131, 131), (132, 132), (-1, 133), (-1, 134), (133, 135)])

        with open("tests/test_write_fix_expected_outputs/expected_apply_changes_delete_insert_and_modify_last_line.txt") as expected_file:
            expected_result = expected_file.readlines()

        self.assertEqual(
            list(change_result.change_tracked_lines), expected_result)

    def test_apply_changes_delete_out_of_bounds_line(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [0],
            "modifications": [{
                "line_number": 4,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 11,
                "modified_line": "    modified_there\n"
            }]
        }]
        with self.assertRaises(write_fix.ApplyChangesError) as ace:
            file_relative_path = self.agent.ai_config.warning_file_path

            project_dir = os.path.join(
                self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

            file_full_path = os.path.join(project_dir, file_relative_path)

            change_result: FileChanges = write_fix.apply_changes(
                change_dict_list, file_relative_path, file_full_path)
        self.assertEqual(
            ace.exception.msg, f"Line 0 to delete was out of range for the file {change_dict_list[0]['file_name']}. The file only has 133 lines.")

    def test_apply_changes_modify_out_of_bounds_line(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 10,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [1],
            "modifications": [{
                "line_number": 2,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 134,
                "modified_line": "    modified_there\n"
            }]
        }]
        with self.assertRaises(write_fix.ApplyChangesError) as ace:
            file_relative_path = self.agent.ai_config.warning_file_path

            project_dir = os.path.join(
                self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

            file_full_path = os.path.join(project_dir, file_relative_path)

            change_result: FileChanges = write_fix.apply_changes(
                change_dict_list, file_relative_path, file_full_path)

        self.assertEqual(
            ace.exception.msg, f"Line 134 to modify was out of range for the file {change_dict_list[0]['file_name']}. The file only has 133 lines.")

    def test_apply_changes_insert_after_last_line(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 134,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [133],
            "modifications": [{
                "line_number": 133,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 133,
                "modified_line": "    modified_there\n"
            }]
        }]

        file_relative_path = self.agent.ai_config.warning_file_path

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        change_result: FileChanges = write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)

        resulting_map = [(map.before_line, map.after_line)
                         for map in change_result.change_tracked_lines.map_line_indices_before_after_change]
        print(resulting_map)

        self.assertEqual(resulting_map, [(1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (11, 11), (12, 12), (13, 13), (14, 14), (15, 15), (16, 16), (17, 17), (18, 18), (19, 19), (20, 20), (21, 21), (22, 22), (23, 23), (24, 24), (25, 25), (26, 26), (27, 27), (28, 28), (29, 29), (30, 30), (31, 31), (32, 32), (33, 33), (34, 34), (35, 35), (36, 36), (37, 37), (38, 38), (39, 39), (40, 40), (41, 41), (42, 42), (43, 43), (44, 44), (45, 45), (46, 46), (47, 47), (48, 48), (49, 49), (50, 50), (51, 51), (52, 52), (53, 53), (54, 54), (55, 55), (56, 56), (57, 57), (58, 58), (59, 59), (60, 60), (61, 61), (62, 62), (63, 63), (64, 64), (65, 65), (66, 66), (67, 67), (68, 68), (69, 69), (70, 70), (
            71, 71), (72, 72), (73, 73), (74, 74), (75, 75), (76, 76), (77, 77), (78, 78), (79, 79), (80, 80), (81, 81), (82, 82), (83, 83), (84, 84), (85, 85), (86, 86), (87, 87), (88, 88), (89, 89), (90, 90), (91, 91), (92, 92), (93, 93), (94, 94), (95, 95), (96, 96), (97, 97), (98, 98), (99, 99), (100, 100), (101, 101), (102, 102), (103, 103), (104, 104), (105, 105), (106, 106), (107, 107), (108, 108), (109, 109), (110, 110), (111, 111), (112, 112), (113, 113), (114, 114), (115, 115), (116, 116), (117, 117), (118, 118), (119, 119), (120, 120), (121, 121), (122, 122), (123, 123), (124, 124), (125, 125), (126, 126), (127, 127), (128, 128), (129, 129), (130, 130), (131, 131), (132, 132), (133, 133), (-1, 134), (-1, 135)])

        with open("tests/test_write_fix_expected_outputs/expected_apply_changes_insert_after_last_line.txt") as expected_file:
            expected_result = expected_file.readlines()

        self.assertEqual(change_result.change_tracked_lines, expected_result)

    def test_apply_changes_insert_multiple_lines_after_last_line(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 138,
                "new_lines": [
                    "    // Some new line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [133],
            "modifications": [{
                "line_number": 133,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 133,
                "modified_line": "    modified_there\n"
            }]
        }]

        file_relative_path = self.agent.ai_config.warning_file_path

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        change_result: FileChanges = write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)

        resulting_map = [(map.before_line, map.after_line)
                         for map in change_result.change_tracked_lines.map_line_indices_before_after_change]
        print(resulting_map)

        self.assertEqual(resulting_map, [(1, 1), (2, 2), (3, 3), (4, 4), (5, 5), (6, 6), (7, 7), (8, 8), (9, 9), (10, 10), (11, 11), (12, 12), (13, 13), (14, 14), (15, 15), (16, 16), (17, 17), (18, 18), (19, 19), (20, 20), (21, 21), (22, 22), (23, 23), (24, 24), (25, 25), (26, 26), (27, 27), (28, 28), (29, 29), (30, 30), (31, 31), (32, 32), (33, 33), (34, 34), (35, 35), (36, 36), (37, 37), (38, 38), (39, 39), (40, 40), (41, 41), (42, 42), (43, 43), (44, 44), (45, 45), (46, 46), (47, 47), (48, 48), (49, 49), (50, 50), (51, 51), (52, 52), (53, 53), (54, 54), (55, 55), (56, 56), (57, 57), (58, 58), (59, 59), (60, 60), (61, 61), (62, 62), (63, 63), (64, 64), (65, 65), (66, 66), (67, 67), (68, 68), (69, 69), (70, 70), (71, 71), (72, 72), (
            73, 73), (74, 74), (75, 75), (76, 76), (77, 77), (78, 78), (79, 79), (80, 80), (81, 81), (82, 82), (83, 83), (84, 84), (85, 85), (86, 86), (87, 87), (88, 88), (89, 89), (90, 90), (91, 91), (92, 92), (93, 93), (94, 94), (95, 95), (96, 96), (97, 97), (98, 98), (99, 99), (100, 100), (101, 101), (102, 102), (103, 103), (104, 104), (105, 105), (106, 106), (107, 107), (108, 108), (109, 109), (110, 110), (111, 111), (112, 112), (113, 113), (114, 114), (115, 115), (116, 116), (117, 117), (118, 118), (119, 119), (120, 120), (121, 121), (122, 122), (123, 123), (124, 124), (125, 125), (126, 126), (127, 127), (128, 128), (129, 129), (130, 130), (131, 131), (132, 132), (133, 133), (-1, 134), (-1, 135), (-1, 136), (-1, 137), (-1, 138), (-1, 139)])

        with open("tests/test_write_fix_expected_outputs/expected_apply_changes_insert_multiple_lines_after_last_line.txt") as expected_file:
            expected_result = expected_file.readlines()

        self.assertEqual(change_result.change_tracked_lines, expected_result)

    def test_apply_changes_pairing_of_insertion_deletion_pairs_that_form_modifications(self):
        change_dict_list = [{
            "file_name": self.agent.ai_config.warning_file_path,
            "insertions": [{
                "line_number": 20,
                "new_lines": [
                    "    // New line\n",
                    "    // next new line\n"
                ]
            },],
            "deletions": [20, 21],
            "modifications": [{
                "line_number": 133,
                "modified_line": "    modified_here\n"
            },
                {
                "line_number": 133,
                "modified_line": "    modified_there\n"
            }]
        }]

        file_relative_path = self.agent.ai_config.warning_file_path

        project_dir = os.path.join(
            self.agent.config.workspace_path, self.agent.ai_config.warning_repository_name)

        file_full_path = os.path.join(project_dir, file_relative_path)

        change_result: FileChanges = write_fix.apply_changes(
            change_dict_list, file_relative_path, file_full_path)

        resulting_pairing = [((pair[0].before_line, pair[0].after_line, pair[0].inserted), (pair[1].before_line, pair[1].after_line,
                                                                                            pair[1].deleted)) for pair in change_result.change_tracked_lines.paired_insertions_and_deletions]
        print(resulting_pairing)
        self.assertEqual(resulting_pairing, [
                         ((-1, 20, True), (20, 22, True)), ((-1, 21, True), (21, 22, True))])
