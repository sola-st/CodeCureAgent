**Formatting SQL queries is security-sensitive**  

Formatted SQL queries can be difficult to maintain, debug and can increase the risk of SQL injection when concatenating untrusted values into the query. However, this rule doesn’t detect SQL injections (unlike rule {rule:javasecurity:S3649}), the goal is only to highlight complex/formatted queries.

Ask Yourself Whether

  * Some parts of the query come from untrusted values (like user inputs). 
  * The query is repeated/duplicated in other parts of the code. 
  * The application must support different types of relational databases. 



There is a risk if you answered yes to any of those questions.

Sensitive Code Example
    
    
    public User getUser(Connection con, String user) throws SQLException {
    
      Statement stmt1 = null;
      Statement stmt2 = null;
      PreparedStatement pstmt;
      try {
        stmt1 = con.createStatement();
        ResultSet rs1 = stmt1.executeQuery("GETDATE()"); // No issue; hardcoded query
    
        stmt2 = con.createStatement();
        ResultSet rs2 = stmt2.executeQuery("select FNAME, LNAME, SSN " +
                     "from USERS where UNAME=" + user);  // Sensitive
    
        pstmt = con.prepareStatement("select FNAME, LNAME, SSN " +
                     "from USERS where UNAME=" + user);  // Sensitive
        ResultSet rs3 = pstmt.executeQuery();
    
        //...
    }
    
    public User getUserHibernate(org.hibernate.Session session, String data) {
    
      org.hibernate.Query query = session.createQuery(
                "FROM students where fname = " + data);  // Sensitive
      // ...
    }
    

Recommended Secure Coding Practices

  * Use [parameterized queries, prepared statements, or stored procedures](https://cheatsheetseries.owasp.org/cheatsheets/Query_Parameterization_Cheat_Sheet.html) and bind variables to SQL query parameters. 
  * Consider using ORM frameworks if there is a need to have an abstract layer to access data. 



Compliant Solution
    
    
    public User getUser(Connection con, String user) throws SQLException {
    
      Statement stmt1 = null;
      PreparedStatement pstmt = null;
      String query = "select FNAME, LNAME, SSN " +
                     "from USERS where UNAME=?"
      try {
        stmt1 = con.createStatement();
        ResultSet rs1 = stmt1.executeQuery("GETDATE()");
    
        pstmt = con.prepareStatement(query);
        pstmt.setString(1, user);  // Good; PreparedStatements escape their inputs.
        ResultSet rs2 = pstmt.executeQuery();
    
        //...
      }
    }
    
    public User getUserHibernate(org.hibernate.Session session, String data) {
    
      org.hibernate.Query query =  session.createQuery("FROM students where fname = ?");
      query = query.setParameter(0,data);  // Good; Parameter binding escapes all input
    
      org.hibernate.Query query2 =  session.createQuery("FROM students where fname = " + data); // Sensitive
      // ...
    

See

  * [OWASP Top 10 2021 Category A3](https://owasp.org/Top10/A03_2021-Injection/) \- Injection 
  * [OWASP Top 10 2017 Category A1](https://owasp.org/www-project-top-ten/2017/A1_2017-Injection) \- Injection 
  * [MITRE, CWE-89](https://cwe.mitre.org/data/definitions/89) \- Improper Neutralization of Special Elements used in an SQL Command 
  * [MITRE, CWE-564](https://cwe.mitre.org/data/definitions/564) \- SQL Injection: Hibernate 
  * [MITRE, CWE-20](https://cwe.mitre.org/data/definitions/20) \- Improper Input Validation 
  * [MITRE, CWE-943](https://cwe.mitre.org/data/definitions/943) \- Improper Neutralization of Special Elements in Data Query Logic 
  * [CERT, IDS00-J.](https://wiki.sei.cmu.edu/confluence/x/ITdGBQ) \- Prevent SQL injection 
  * [SANS Top 25](https://www.sans.org/top25-software-errors/#cat1) \- Insecure Interaction Between Components 
  * Derived from FindSecBugs rules [Potential SQL/JPQL Injection (JPA)](https://h3xstream.github.io/find-sec-bugs/bugs.htm#SQL_INJECTION_JPA), [Potential SQL/JDOQL Injection (JDO)](https://h3xstream.github.io/find-sec-bugs/bugs.htm#SQL_INJECTION_JDO), [Potential SQL/HQL Injection (Hibernate)](https://h3xstream.github.io/find-sec-bugs/bugs.htm#SQL_INJECTION_HIBERNATE)