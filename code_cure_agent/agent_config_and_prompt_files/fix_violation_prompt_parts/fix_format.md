## The format of the fix

When calling write_fix, you must provide a JSON array (`changes_dicts`) of file-level change objects. Each object must only have the following top-level keys:

* "file_name": (string) — the path or name of the file to modify.
* "insertions": (array of insertion objects) — each object defines where and what to insert.
* "deletions": (array of integers) — each integer is a line number to delete.

### Important Structural Rules

* The top-level array contains one or more objects per modified file.
* Each file-level object must contain only these three keys: "file_name", "insertions", and "deletions".
* Do NOT nest "deletions" inside "insertions". The "deletions" array belongs at the same level as "insertions" — both are top-level keys in the file’s object.
* Inside the "insertions" array each object must include:
  * "line_number": (integer) — the line number before which to insert.
  * "new_lines": (array of strings) — the new content to insert.

### Example usage of the format (not related to your specific task)

```json
[
    {
        "file_name": "src/main/File1.java",
        "insertions": [
            {
                "line_number": 10,
                "new_lines": [
                    "    int x = 5;",
                    "    System.out.println(x);"
                ]
            }
        ],
        "deletions": [12, 13]
    },
    {
        "file_name": "src/main/File2.java",
        "insertions": [],
        "deletions": [20]
    }
]
```

### Common Mistake to Avoid

```json
...
  "insertions": [
      {
          "line_number": 42,
          "new_lines": ["foo"],
          "deletions": [43]  // ❌ This is invalid. "deletions" must not be inside an insertion.
      }
  ]
...
```

### Always double-check that

* "deletions" is not inside any "insertions" object.
* The structure exactly matches the example.
* Take great care that you specify the correct line numbers and that you include all the lines in "deletions" that need to be deleted!  

### Further notes

In order to overwrite an existing line, both delete the line and insert a new line at the same line_number.  

You must always apply all relevant changes in a single write_fix all at once.  
After each write_fix attempt, the project is restored to its original state and all your made changes are lost.  
However, you can then try again and attempt modfied fixes, if your previous attempts were rejected.  

### Limitations

* You are not allowed to create, rename, move, or delete files.
* You are not allowed to add new external dependencies to the project. You may only import:  
  * Types (classes, interfaces, etc.) and static members from the Java Standard Library,
  * Types and static members from libraries already included in the project’s dependencies,
  * Project-local types and static members from other source files.