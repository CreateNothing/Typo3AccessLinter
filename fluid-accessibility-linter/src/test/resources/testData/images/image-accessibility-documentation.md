# Image Accessibility Test Documentation

This directory contains comprehensive test cases for image accessibility inspections in the TYPO3 Fluid Accessibility Linter.

## Test Files Overview

### 1. Test Classes

#### `MissingAltTextInspectionTest.java`
- **Purpose**: Tests basic alt attribute presence detection
- **Test Count**: 43 comprehensive test methods
- **Coverage Areas**:
  - HTML `<img>` tags missing alt attributes
  - Fluid `<f:image>` ViewHelpers missing alt attributes  
  - HTML `<input type="image">` elements missing alt text
  - Edge cases and malformed HTML
  - Performance testing with multiple images
  - Mixed HTML/Fluid scenarios

#### `ImageAccessibilityInspectionTest.java`
- **Purpose**: Tests advanced image accessibility validation
- **Test Count**: 45+ comprehensive test methods
- **Coverage Areas**:
  - Alt text quality validation (redundant phrases, filenames, placeholders)
  - Decorative image detection and validation
  - SVG accessibility (role, title, desc, ARIA attributes)
  - Context-aware validation (images in links)
  - Alt text length recommendations
  - ARIA alternatives (aria-label, aria-labelledby)
  - Complex scenarios and integration testing

### 2. Test Data Files

#### `missing-alt-issues.html`
Contains examples of images that SHOULD trigger the MissingAltTextInspection:
- HTML images without alt attributes
- Fluid images without alt attributes
- Input type="image" elements without alt text
- Mixed scenarios with some missing alt attributes
- Edge cases and malformed markup

#### `missing-alt-valid.html`
Contains examples of images that should NOT trigger the MissingAltTextInspection:
- HTML images with proper alt attributes
- Fluid images with alt attributes (including empty alt="")
- Input type="image" with alt text
- Images with ARIA alternatives
- Valid edge cases

#### `enhanced-accessibility-issues.html`
Contains examples that SHOULD trigger the ImageAccessibilityInspection:
- Images with redundant phrases in alt text ("image of", "picture of", etc.)
- Images using filenames as alt text
- Images with placeholder alt text ("image", "untitled", etc.)
- Very short or very long alt text
- Decorative images with inappropriate alt text
- SVG elements without accessibility features
- Input type="image" without accessible text

#### `enhanced-accessibility-valid.html`
Contains examples that should NOT trigger the ImageAccessibilityInspection:
- High-quality, descriptive alt text
- Properly marked decorative images (empty alt, role="presentation/none")
- SVG elements with proper accessibility (title, desc, aria-label, etc.)
- Input type="image" with appropriate accessible text
- Images with ARIA alternatives
- Context-appropriate descriptions

#### `svg-accessibility-test.html`
Specialized test file focusing on SVG accessibility:
- SVG elements missing accessibility features
- SVG with role="img" but no accessible text
- SVG with proper accessibility (title, desc, ARIA)
- Decorative SVG elements (role="presentation/none")
- Interactive SVG elements
- Complex SVG scenarios

## Test Coverage Areas

### WCAG Success Criteria Covered

1. **1.1.1 Non-text Content (Level A)**
   - All images have text alternatives
   - Decorative images marked appropriately
   - Complex images have adequate descriptions

2. **2.4.4 Link Purpose (In Context) (Level A)**
   - Images in links have descriptive alt text
   - Alt text describes link destination/purpose

3. **4.1.2 Name, Role, Value (Level A)**
   - SVG elements have appropriate roles and names
   - Input type="image" elements have accessible names

### Image Types Tested

1. **HTML Images**
   - `<img>` elements
   - `<input type="image">` elements
   - Images in various contexts (links, figures, cards)

2. **Fluid ViewHelpers**
   - `<f:image>` elements
   - Dynamic alt text generation
   - Conditional image rendering

3. **SVG Graphics**
   - Inline SVG elements
   - Interactive SVG
   - Decorative vs. informative SVG
   - Complex data visualizations

### Alt Text Quality Checks

1. **Prohibited Patterns**
   - Redundant phrases: "image of", "picture of", "photo of", "graphic of", "icon of"
   - Filenames: "IMG_1234.jpg", "photo.png"
   - Placeholders: "image", "untitled", "temp", "dummy"

2. **Length Validation**
   - Too short: < 3 characters (context-dependent)
   - Too long: > 125 characters (suggest aria-describedby)

3. **Context Awareness**
   - Images in links need descriptive alt text
   - Functional images describe action/purpose
   - Decorative images should be marked as such

### Decorative Image Detection

The inspection detects decorative images by:
- **Role attributes**: `role="presentation"` or `role="none"`
- **CSS classes**: Contains words like "decorative", "ornament", "spacer"
- **Filenames**: Contains words like "spacer", "divider", "background"
- **Dimensions**: Very small images (< 50px × 50px)
- **Empty alt**: `alt=""` might indicate decorative intent

### SVG Accessibility Requirements

1. **Informative SVG needs one of**:
   - `<title>` element
   - `<desc>` element  
   - `aria-label` attribute
   - `aria-labelledby` attribute

2. **Decorative SVG should have**:
   - `role="presentation"` or `role="none"`

3. **Interactive SVG should have**:
   - Proper role and accessible name
   - Keyboard accessibility (when applicable)

## Test Execution

### Running Individual Test Classes
```bash
# Run MissingAltTextInspection tests
./gradlew test --tests "*MissingAltTextInspectionTest*"

# Run ImageAccessibilityInspection tests  
./gradlew test --tests "*ImageAccessibilityInspectionTest*"
```

### Running Specific Test Methods
```bash
# Test specific alt text scenario
./gradlew test --tests "*MissingAltTextInspectionTest.testBasicMissingAlt"

# Test redundant phrases detection
./gradlew test --tests "*ImageAccessibilityInspectionTest.testRedundantPhraseImage"
```

### Running All Image Accessibility Tests
```bash
# Run all image-related tests
./gradlew test --tests "*Image*Test*"
```

## Expected Test Results

- **MissingAltTextInspectionTest**: 43 tests covering basic alt attribute detection
- **ImageAccessibilityInspectionTest**: 45+ tests covering advanced accessibility validation
- **Total**: 88+ test methods providing comprehensive coverage

## Maintenance Notes

1. **Adding New Test Cases**
   - Add test methods to appropriate test class
   - Create corresponding test data in HTML files
   - Follow existing naming conventions

2. **Updating Detection Logic**
   - Update both positive (should trigger) and negative (should not trigger) test cases
   - Ensure test data files reflect current validation rules

3. **Performance Considerations**
   - Test files include performance tests with many images
   - Monitor test execution time for large documents
   - Consider test data size for CI/CD environments

## Integration with Inspection Classes

These tests validate the behavior of:
- `MissingAltTextInspection.java` - Basic alt attribute detection
- `ImageAccessibilityInspection.java` - Advanced quality validation

The test framework uses IntelliJ Platform's test infrastructure to:
- Configure test files as PSI elements
- Enable specific inspections
- Capture and validate highlighting results
- Verify expected warning messages are generated
