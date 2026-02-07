---
applyTo: '**/*.java'
---
Provide project context and coding guidelines that AI should follow when generating code, answering questions, or reviewing changes.

---

## 🔐 Secure by Default

- Sanitize and escape all user input (prevent XSS) — never render raw data to the page.
- Validate all input strictly — use typed parsers and prefer allow-lists over deny-lists.
- Use parameterized queries and avoid string-based execution (prevent injection).
- Never store secrets in code or env files — use a secure vault (e.g. CyberArk Conjur, Azure Key Vault).
- Default to privacy-preserving data handling — redact PII from logs by default.

---
## 🧩 Java Specific Secure Patterns
 
1. **Java Standards**
   - Follow Java coding conventions and best practices.
   - Use meaningful class, method, and variable names.
   - Write clear and concise Java code.
   - Use proper indentation and formatting.
   - Leverage streams, lambdas, functional interfaces, and other modern Java features.
   - Implement proper exception handling using `try-catch` blocks and appropriate exception types.
   - Consider using `Optional` to handle potential null values gracefully.
   - Choose appropriate data structures (e.g., `List`, `Set`, `Map`) based on the use case.
   - Use prepared statements with `?` placeholders in JDBC — never concat SQL strings.
   - Use `@Valid`, `@NotNull`, and input binding constraints in Spring or Jakarta for validation.
   - Avoid `Runtime.exec()` or `ProcessBuilder` with unsanitized input — prefer safe APIs.
   - Always set character encoding (`UTF-8`) explicitly in HTTP responses to prevent encoding-based attacks.
   - Avoid Java serialization for sensitive objects — use safer formats like JSON with strict schema validation.
   - When using logging frameworks, avoid logging unsanitized user input — consider log injection risks.

2. **Security**
   - Avoid common security vulnerabilities (e.g., XSS, CSRF).
   - Validate and sanitize user input.
   - Use secure coding practices (e.g., least privilege, defense in depth).
   - Default to OWASP Secure Coding Practices — [OWASP Secure Coding Practices](https://owasp.org/www-project-secure-coding-practices)
   - Load secrets using SDK-integrated secret managers, not `System.getenv()` or `.properties` files.
   - Use output encoding libraries like OWASP Java Encoder to prevent XSS in rendered HTML.
   
3. **Performance**
   - Optimize Java code for performance.
   - Use efficient algorithms and data structures.
   - Avoid unnecessary object creation and memory usage.

4. **Testing**
   - Write unit and integration tests for Java code to ensure correctness and maintainability.
   - Use testing frameworks (e.g., JUnit, Mockito) for automated testing.


## General Instructions

- First, prompt the user if they want to integrate static analysis tools (SonarQube, PMD, Checkstyle)
  into their project setup. If yes, provide guidance on tool selection and configuration.
- If the user declines static analysis tools or wants to proceed without them, continue with implementing the Best practices, bug patterns and code smell prevention guidelines outlined below.
- Address code smells proactively during development rather than accumulating technical debt.
- Focus on readability, maintainability, and performance when refactoring identified issues.
- Use IDE / Code editor reported warnings and suggestions to catch common patterns early in development.

## Best practices

- **Records**: For classes primarily intended to store data (e.g., DTOs, immutable data structures), **Java Records should be used instead of traditional classes**.
- **Pattern Matching**: Utilize pattern matching for `instanceof` and `switch` expression to simplify conditional logic and type casting.
- **Type Inference**: Use `var` for local variable declarations to improve readability, but only when the type is explicitly clear from the right-hand side of the expression.
- **Immutability**: Favor immutable objects. Make classes and fields `final` where possible. Use collections from `List.of()`/`Map.of()` for fixed data. Use `Stream.toList()` to create immutable lists.
- **Streams and Lambdas**: Use the Streams API and lambda expressions for collection processing. Employ method references (e.g., `stream.map(Foo::toBar)`).
- **Null Handling**: Avoid returning or accepting `null`. Use `Optional<T>` for possibly-absent values and `Objects` utility methods like `equals()` and `requireNonNull()`.

### Naming Conventions

- Follow Google's Java style guide:
    - `UpperCamelCase` for class and interface names.
    - `lowerCamelCase` for method and variable names.
    - `UPPER_SNAKE_CASE` for constants.
    - `lowercase` for package names.
- Use nouns for classes (`UserService`) and verbs for methods (`getUserById`).
- Avoid abbreviations and Hungarian notation.

### Bug Patterns

| Rule ID | Description                                                 | Example / Notes                                                                                  |
| ------- | ----------------------------------------------------------- | ------------------------------------------------------------------------------------------------ |
| `S2095` | Resources should be closed                                  | Use try-with-resources when working with streams, files, sockets, etc.                           |
| `S1698` | Objects should be compared with `.equals()` instead of `==` | Especially important for Strings and boxed primitives.                                           |
| `S1905` | Redundant casts should be removed                           | Clean up unnecessary or unsafe casts.                                                            |
| `S3518` | Conditions should not always evaluate to true or false      | Watch for infinite loops or if-conditions that never change.                                     |
| `S108`  | Unreachable code should be removed                          | Code after `return`, `throw`, etc., must be cleaned up.                                          |

## Code Smells

| Rule ID | Description                                            | Example / Notes                                                               |
| ------- | ------------------------------------------------------ | ----------------------------------------------------------------------------- |
| `S100`  | Method names should comply with a naming convention    | Use `lowerCamelCase` for methods (e.g., `getUserById` not `GetUserById`).     |
| `S107`  | Methods should not have too many parameters            | Refactor into helper classes or use builder pattern.                          |
| `S121`  | Duplicated blocks of code should be removed            | Consolidate logic into shared methods.                                        |
| `S138`  | Methods should not be too long                         | Break complex logic into smaller, testable units.                             |
| `S3776` | Cognitive complexity should be reduced                 | Simplify nested logic, extract methods, avoid deep `if` trees.                |
| `S1192` | String literals should not be duplicated               | Replace with constants or enums.                                              |
| `S1854` | Unused assignments should be removed                   | Avoid dead variables—remove or refactor.                                      |
| `S109`  | Magic numbers should be replaced with constants        | Improves readability and maintainability.                                     |
| `S1188` | Catch blocks should not be empty                       | Always log or handle exceptions meaningfully.                                 |

## Build and Verification

- After adding or modifying code, verify the project continues to build successfully.
- If the project uses Maven, run `mvn clean install`.
- If the project uses Gradle, run `./gradlew build` (or `gradlew.bat build` on Windows).
- Ensure all tests pass as part of the build.

### 🚫 Do Not Suggest

- Do not suggest inline SQL string concatenation — always use prepared statements with placeholders.
- Do not suggest use of `Runtime.exec()` or `ProcessBuilder` with user input — prefer safe abstraction layers.
- Do not suggest logging sensitive data (e.g. passwords, tokens, session IDs) — log redacted metadata instead.
- Do not use Java native serialization (`ObjectInputStream`) for untrusted input — prefer JSON + schema validation.
- Do not suggest hardcoding credentials, secrets, or API keys — use a secrets manager (e.g. Conjur, Key Vault).
- Do not use insecure XML parsers without hardening (`DocumentBuilderFactory` must have secure features enabled).
- Do not create or modify custom class loaders — these are dangerous unless strictly required.