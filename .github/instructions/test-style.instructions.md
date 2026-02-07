---
applyTo: '**Test.java, Test**.java'
---
# Java Test Style Instructions for AI Coding Agent

## Testing Framework Usage
- **JUnit 5**: Use `@BeforeEach`, `@AfterEach`, `@DisplayName`, parameterized tests
- **Mockito**: Employ `@Mock`, `@InjectMocks`, `@Spy` annotations appropriately
- **Test Independence**: Each test should be independent with proper setup/teardown
- **Coverage**: Focus on critical business logic and edge cases

## Test Naming Convention
Follow pattern: `should[Action]When[Condition]ISSUE-XXX`
```java
@DisplayName("shouldCalculateTotalWhenValidInputISSUE-123")
@Test
void shouldCalculateTotalWhenValidInput() {
    // Arrange - setup test data
    // Act - execute method under test
    // Assert - verify expected outcome
}
```
## General Guidelines

* **Clear and Concise Test Names:**
    * Test names should be descriptive and accurately reflect the behavior being tested.
    * Use meaningful verb-noun pairs (e.g., `shouldCalculateTotal`, `shouldHandleInvalidInput`).
    * Include the Jira issue key in parentheses at the end of the test name (e.g., `shouldCalculateTotalISSUE-123`).
* **Test Independence:**
    * Each test should be independent and not rely on the state of other tests.
    * Use appropriate test data setup and teardown methods to ensure test isolation.
* **Test Coverage:**
    * Aim for high code coverage, especially for critical parts of the code.
    * Use tools like JaCoCo or SonarQube to measure and track test coverage.
* **Edge Cases:**
    * Consider edge cases and boundary conditions to ensure robustness.
    * Test for invalid inputs, unexpected scenarios, and potential errors.
* **Readability:**
    * Write clean, well-formatted, and easy-to-read test code.
    * Use meaningful variable and method names.
    * Add comments to explain complex test logic.

## Java Specific Considerations

* **JUnit 5:**
    * Utilize JUnit 5 features like `@BeforeEach`, `@AfterEach`, `@DisplayName`, and parameterized tests.
    * Leverage JUnit 5's assertions (e.g., `Assertions.assertEquals`, `Assertions.assertThrows`) for concise and readable tests.
* **Mockito:**
    * Use Mockito to effectively mock dependencies and isolate units under test.
    * Employ `@Mock`, `@InjectMocks`, and `@Spy` annotations appropriately.
    * Verify interactions using `verify`, `verifyNoInteractions`, and `verify(times(n))`.
* **Assertions:**
    * Use appropriate assertion libraries (e.g., JUnit's `Assert`, Hamcrest) to verify expected behavior.
    * Use descriptive assertion messages to provide meaningful feedback in case of test failures.
* **Mocks and Stubs:**
    * Use mocking frameworks (e.g., Mockito, EasyMock) to isolate components under test and improve testability.
* **Parameterized Tests:**
    * Use parameterized tests (e.g., JUnit's `@ParameterizedTest`) to efficiently test different input values and expected outputs.
* **Arrange-Act-Assert:**
    * Adhere to the Arrange-Act-Assert pattern:
        * **Arrange:** Set up the test environment, including creating test data, mocking dependencies, and initializing objects.
        * **Act:** Execute the code under test.
        * **Assert:** Verify the expected outcome using assertions.

## Example Test Method

```java
@ExtendWith(MockitoExtension.class)
public class MyServiceTest {

    @InjectMocks
    private MyService myService;

    @Mock
    private MyRepository myRepository;

    @DisplayName("shouldCalculateTotalWhenValidInputISSUE-123")
    @ParameterizedTest
    @ValueSource(doubles = {10.0, 20.0, 5.0})
    void shouldCalculateTotalWhenValidInput(double input) {
        // Arrange
        when(myRepository.getData()).thenReturn(input);

        // Act
        double result = myService.calculateValue(input);

        // Assert
        assertEquals(input * 2, result, 0.01);
    }

    @DisplayName("shouldThrowExceptionWhenInputIsNegativeISSUE-124")
    @Test
    void shouldThrowExceptionWhenInputIsNegative() {
        // Arrange
        double input = -5.0;

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> myService.calculateValue(input));
    }
}