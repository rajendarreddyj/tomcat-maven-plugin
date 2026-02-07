# Commit Message Style Instructions (Java) for AI Coding Agent

## General Guidelines

* **Clear and Concise:** Commit messages should be clear, concise, and informative.
* **Present Tense:** Use the present tense ("Add," "Fix," "Improve") for consistency.
* **50/72 Character Limit:** Keep the first line under 50 characters and the overall message under 72 characters for better readability in tools like `git log`.

## Structure
`type(scope): description (ISSUE-XXX)`

* **Type:**
    * `feat`: A new feature
    * `fix`: A bug fix
    * `docs`: Documentation only changes
    * `style`: Changes that do not affect the meaning of the code (white-space, formatting, etc.)
    * `refactor`: A code change that neither fixes a bug nor adds a feature
    * `perf`: A code change that improves performance
    * `test`: Adding missing tests or correcting existing tests
    * `build`: Changes that affect the build system or external dependencies (e.g., pom.xml, build.gradle)
    * `ci`: Changes to our CI/CD pipelines
    * `chore`: Other changes that don't fall into any of the above categories

* **Scope:** A short, descriptive scope of the change.

* **Description:** A more detailed description of the change.

* **Jira Issue (Optional):** If the commit is related to a Jira issue, include the Jira issue key in parentheses.

## Example

    > feat(user): Add user registration endpoint(ISSUE-123)

## Best Practices
- **Clear and Concise:** Commit messages should be clear, concise, and informative.
- **Present Tense:** Use the present tense ("Add," "Fix," "Improve") for consistency.
- **50/72 Character Limit:** Keep the first line under 50 characters and the overall message under 72 characters.
- **Avoid vague messages:** Instead of "Fix issues," use "Fix incorrect calculation in method X."
- **Use imperative mood:** "Add feature" instead of "Added feature."
* **Keep it concise:** Focus on the core change and avoid unnecessary details.
* **Follow consistent formatting:** Ensure consistency in the use of capitalization, punctuation, and spacing.