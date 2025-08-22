package com.typo3.fluid.linter.strategy.implementations;

/**
 * Stub implementation for Fluid form validation.
 * TODO: Implement proper Fluid form ViewHelper checking
 */
public class FluidFormValidationStrategy extends BaseValidationStrategy {
    @Override
    public int getPriority() {
        return 85;
    }
}