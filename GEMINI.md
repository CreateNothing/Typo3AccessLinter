# Project Overview

This project is a PhpStorm plugin that provides real-time accessibility linting for TYPO3 Fluid template files. It is a Gradle-based project written in Java.

The plugin identifies a wide range of accessibility issues, including:

*   Missing alt text for images
*   Invalid list structures
*   Missing form labels
*   Incorrect heading hierarchies
*   Missing page language attributes
*   Non-descriptive link text
*   Missing navigation landmarks and skip links
*   Table accessibility issues
*   Invalid ARIA roles
*   And many more, as detailed in the `plugin.xml` file.

The plugin also provides quick fixes for some of these issues.

# Architecture

The project has been refactored from a monolithic architecture to a modular, rule-based system. The new architecture is based on the following key components:

*   **Rule-Based System:** A `RuleEngine` manages and executes accessibility rules. Rules are defined in `AccessibilityRule` domain models and are provided by `RuleProvider` implementations (e.g., `DefaultRuleProvider` for HTML rules and `FluidRuleProvider` for Fluid-specific rules).
*   **Strategy Pattern:** The core validation logic is implemented using the Strategy pattern. Each validation is encapsulated in a `ValidationStrategy` class, which makes the system more modular and easier to extend.
*   **Modular Fix System:** Quick fixes are implemented using a `FixStrategy` interface. A `FixRegistry` manages the available fixes, which are associated with specific problem types.
*   **Universal Inspection:** The `UniversalAccessibilityInspection` class serves as a single entry point for all accessibility inspections, replacing the previous monolithic inspection classes.

This new architecture improves maintainability, extensibility, and testability.

# Building and Running

## Building the Plugin

To build the plugin, run the following command in the `fluid-accessibility-linter` directory:

```bash
./gradlew buildPlugin
```

The plugin ZIP file will be created in the `fluid-accessibility-linter/build/distributions/` directory.

## Running the Plugin in a Development Environment

To run the plugin in a development instance of IntelliJ IDEA, run the following command in the `fluid-accessibility-linter` directory:

```bash
./gradlew runIde
```

# Development Conventions

## Testing

The project uses JUnit for testing. Tests are located in the `fluid-accessibility-linter/src/test/java` directory.

To run the tests, you can use the following command in the `fluid-accessibility-linter` directory:

```bash
./gradlew test
```

## Adding New Rules

To add a new accessibility rule, you need to follow the new modular approach:

1.  **Create a Validation Strategy:** Create a new class that implements the `ValidationStrategy` interface. This class will contain the logic for your new validation.
2.  **Create and Register a Rule:** Create a new `AccessibilityRule` instance, configure it (ID, name, description, severity, etc.), and register it with the `RuleEngine` along with your new validation strategy.

Here's an example of how to add a new rule:

```java
// 1. Create a new validation strategy
public class NewValidationStrategy implements ValidationStrategy {
    public List<ValidationResult> validate(PsiFile file, String content) {
        // Your validation logic here
    }
}

// 2. Register the new rule with the rule engine
AccessibilityRule rule = new AccessibilityRule();
rule.setId("new-rule");
rule.setSeverity(RuleSeverity.WARNING);
engine.registerRule(rule, new NewValidationStrategy());
```