Your fixes must follow this structure when calling write_fix:  
This format is a list of dictionaries, each describing edits to a specific file.  
Each dictionary must include:

* "file_name": A string indicating the path or name of the file to be modified.  
* "insertions": A list of dictionaries representing insertions in the file. Each insertion dictionary includes:  
  * "line_number": An integer indicating the line number before which we insert lines. The previous content of the line and all following lines are moved down accordingly.  
  * "new_lines": A list of strings representing the new lines to be inserted.  
* "deletions": A list of integers representing line numbers to be deleted from the file.  

Here is an example:  

```json
[
    // changes in file 1
    {
        "file_name": "org/jfree/data/time/Week.java",
        "insertions": [
            {
                "line_number": 175,
                "new_lines": [
                    "    // ... new lines to insert ...\n",
                    "    // ... more new lines ...\n"
                ]
            },
            {
                "line_number": 180,
                "new_lines": [
                    "    // ... additional new lines ...\n"
                ]
            }
        ],
        "deletions": [179, 183]
    },
    // changes in file 2
    {
        "file_name": "org/jfree/data/time/Day.java",
        "insertions": [{
                "line_number": 203,
                "new_lines": [
                    "    days = 0\n"
                ]
            }],
        "deletions": []
    }
]
```

In order to overwrite an existing line, both delete the line and insert a new line at the same line_number.  
Take great care that you specify the correct line numbers and that you include all the lines in "deletions" that need to be deleted!  

You must always apply all relevant changes in a single write_fix all at once.  
After each write_fix attempt, the project is restored to its original state and all your made changes are lost.  
However, you can then try again and attempt modfied fixes, if your previous attempts failed.  