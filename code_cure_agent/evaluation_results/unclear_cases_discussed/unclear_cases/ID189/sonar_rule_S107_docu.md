**Methods should not have too many parameters**  

A long parameter list can indicate that a new structure should be created to wrap the numerous parameters or that the function is doing too many things.

Noncompliant Code Example

With a maximum number of 4 parameters:
    
    
    public void doSomething(int param1, int param2, int param3, String param4, long param5) {
    ...
    }
    

Compliant Solution
    
    
    public void doSomething(int param1, int param2, int param3, String param4) {
    ...
    }
    

Exceptions

Methods annotated with :

  * Spring’s `@RequestMapping` (and related shortcut annotations, like `@GetRequest`) 
  * JAX-RS API annotations (like `@javax.ws.rs.GET`) 
  * Bean constructor injection with `@org.springframework.beans.factory.annotation.Autowired`
  * CDI constructor injection with `@javax.inject.Inject`
  * `@com.fasterxml.jackson.annotation.JsonCreator`
  * Micronaut’s annotations (like `@io.micronaut.http.annotation.Get`) 



may have a lot of parameters, encapsulation being possible. Such methods are therefore ignored.

Also, if a class annotated as a Spring component (like `@org.springframework.stereotype.Component`) has a single constructor, that constructor will be considered `@Autowired` and ignored by the rule.