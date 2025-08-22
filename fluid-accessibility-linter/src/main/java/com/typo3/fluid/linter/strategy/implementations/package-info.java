/**
 * Implementation of validation strategies for accessibility rules.
 * 
 * Each strategy implements a specific accessibility check and can be:
 * - Composed with other strategies
 * - Configured independently
 * - Enabled/disabled at runtime
 * - Extended without modifying existing code
 * 
 * This package replaces the monolithic inspection classes with
 * modular, testable, and maintainable strategies.
 */
package com.typo3.fluid.linter.strategy.implementations;