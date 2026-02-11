**Whitespace and control characters in literals should be explicit**  

Non-encoded control characters and whitespace characters are often injected in the source code because of a bad manipulation. They are either invisible or difficult to recognize, which can result in bugs when the string is not what the developer expects. If you actually need to use a control character use their encoded version (ex: ASCII `\n,\t,`…​ or Unicode `U+000D, U+0009,`…​).

This rule raises an issue when the following characters are seen in a literal string:

  * [ASCII control character](https://en.wikipedia.org/wiki/ASCII#Control_characters). (character index < 32 or = 127) 
  * Unicode [whitespace characters](https://en.wikipedia.org/wiki/Unicode_character_property#Whitespace). 
  * Unicode [C0 control characters](https://en.wikipedia.org/wiki/C0_and_C1_control_codes)
  * Unicode characters `U+200B, U+200C, U+200D, U+2060, U+FEFF, U+2028, U+2029`



No issue will be raised on the simple space character. Unicode `U+0020`, ASCII 32.

Noncompliant Code Example
    
    
    String tabInside = "A	B";  // Noncompliant, contains a tabulation
    String zeroWidthSpaceInside = "foo​bar"; // Noncompliant, it contains a U+200B character inside
    char tab = '	';
    

Compliant Solution
    
    
    String tabInside = "A\tB";  // Compliant, uses escaped value
    String zeroWidthSpaceInside = "foo\u200Bbar";  // Compliant, uses escaped value
    char tab = '\t';
    