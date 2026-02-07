# GitHub Copilot Review Style Instructions (Java)

## General Guidelines

* **Code Quality:**
  * **Adhere to project coding standards:** Ensure code adheres to established coding style guides (e.g., Google Java Style Guide, project-specific guidelines).
  * **Maintainability:** Prioritize code readability, modularity, and maintainability.
  * **Correctness:** Verify the generated code for correctness and accuracy.
  * **Security:** Pay close attention to security best practices and potential vulnerabilities (e.g., SQL injection, cross-site scripting).
* **Testing:**
  * **Test Coverage:** Ensure the generated code is adequately covered by unit tests.
  * **Test Design:** Suggest improvements to existing tests or propose new test cases.
* **Performance:**
  * **Consider Performance:** Consider potential performance implications of the generated code.
  * **Suggest Optimizations:** Suggest optimizations where applicable.
* **Comments and Documentation:**
  * **Clarity:** Ensure comments are clear, concise, and informative.
  * **Consistency:** Maintain consistent commenting style throughout the codebase.
  * **Javadoc:** For public APIs, suggest using Javadoc to document methods, classes, and interfaces.

## Java-Specific Considerations

* **Java Language Features:**
  * **Utilize Modern Java Features:** Encourage the use of modern Java features like streams, lambdas, and functional interfaces where appropriate.
  * **Avoid Deprecated APIs:** Suggest using modern replacements for deprecated APIs.
* **Error Handling:**
  * **Proper Exception Handling:** Ensure proper handling of exceptions using `try-catch` blocks and appropriate exception types.
  * **Consider Using `Optional`:** Use `Optional` to handle potential null values gracefully.
* **Collections Framework:**
  * **Choose Appropriate Data Structures:** Suggest the use of appropriate data structures (e.g., `List`, `Set`, `Map`) based on the use case.
* **Third-Party Libraries:**
  * **Appropriate Usage:** Suggest the use of appropriate third-party libraries where applicable.
  * **Version Compatibility:** Ensure compatibility with existing dependencies.

## Example Review Comment

> "This code uses `for` loops for iteration. Consider using a stream-based approach for better readability and potentially improved performance."

## Important Notes

* **Context is Key:** The effectiveness of Copilot's reviews depends heavily on the context provided. Clearly communicate the project's specific coding standards and requirements.
* **Human Review is Essential:** Copilot should be used as an assistive tool. Always perform a thorough manual review of the generated code.
* **Continuous Improvement:** Regularly review and refine these instructions based on your team's specific needs and feedback.