# Content-Based Responsive Table Layout - Usage Guide

## Overview

This table layout system uses **content-based sizing** where column widths are determined by their content rather than fixed widths. Each column automatically sizes to fit its longest content while respecting minimum width thresholds for readability.

## Key Principles

1. **Content-driven width**: Columns size based on `max(minimum_threshold, longest_content_width)`
2. **Automatic expansion**: Table fills container when total width is smaller
3. **Horizontal scrolling**: Enabled when content exceeds container width
4. **No compression**: Columns maintain minimum widths; never become unreadably narrow

---

## Basic Usage

### 1. Table Structure

```html
<div class="table-wrapper">
    <table>
        <colgroup>
            <!-- Optional: Define column types for minimum width thresholds -->
        </colgroup>
        <thead>
            <tr>
                <th>Column 1</th>
                <th>Column 2</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td>Data 1</td>
                <td>Data 2</td>
            </tr>
        </tbody>
    </table>
</div>
```

### 2. How It Works

**Without column classes:**
- Columns size naturally based on content
- Browser determines optimal widths
- Table expands to fill container

**With column classes (recommended):**
- Columns still size based on content
- BUT enforce minimum width thresholds
- Prevents columns from becoming too narrow

---

## Column Types (Minimum Width Thresholds)

Use `<colgroup>` to define minimum widths for different column types:

### Short Metadata Columns
```html
<colgroup>
    <col class="col-id">        <!-- min-width: 60px -->
    <col class="col-status">    <!-- min-width: 80px -->
    <col class="col-date">      <!-- min-width: 110px -->
    <col class="col-datetime">  <!-- min-width: 150px -->
    <col class="col-time">      <!-- min-width: 80px -->
    <col class="col-number">    <!-- min-width: 80px -->
</colgroup>
```

### Text Columns
```html
<colgroup>
    <col class="col-name">         <!-- min-width: 120px -->
    <col class="col-email">        <!-- min-width: 140px -->
    <col class="col-phone">        <!-- min-width: 100px -->
    <col class="col-description">  <!-- min-width: 150px -->
</colgroup>
```

### Action Columns
```html
<colgroup>
    <col class="col-actions">  <!-- min-width: 150px -->
</colgroup>
```

### Generic Size Classes
```html
<colgroup>
    <col class="col-small">   <!-- min-width: 80px -->
    <col class="col-medium">  <!-- min-width: 120px -->
    <col class="col-large">   <!-- min-width: 180px -->
</colgroup>
```

---

## Cell Behavior Modifiers

### Text Wrapping

**Default:** No wrapping (`white-space: nowrap`)

**Allow wrapping for long content:**
```html
<td class="wrap">This text will wrap to multiple lines if needed</td>
```

**Force single line (explicit):**
```html
<td class="nowrap">Always single line</td>
```

### Text Alignment

```html
<td class="center">Centered</td>
<td class="right">Right-aligned (numbers)</td>
<!-- Default: left-aligned -->
```

### Action Cells

For cells with buttons:
```html
<td class="actions">
    <button>Edit</button>
    <button>Delete</button>
</td>
```

---

## Complete Examples

### Example 1: Simple Table (Few Columns)

**Result:** Table fills container width, columns size based on content

```html
<div class="table-wrapper">
    <table>
        <colgroup>
            <col class="col-id">
            <col class="col-name">
            <col class="col-actions">
        </colgroup>
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="center">1</td>
                <td>John Doe</td>
                <td class="actions">
                    <button>Prescriptions</button>
                    <button>Intake History</button>
                </td>
            </tr>
            <tr>
                <td class="center">2</td>
                <td>Jane Smith Anderson Williams</td>
                <td class="actions">
                    <button>Prescriptions</button>
                    <button>Intake History</button>
                </td>
            </tr>
        </tbody>
    </table>
</div>
```

**Behavior:**
- ID column: ~60px (minimum threshold)
- Name column: Sizes to fit "Jane Smith Anderson Williams" (~200px)
- Actions column: Sizes to fit both buttons (~200px)
- Table expands to fill remaining container width

### Example 2: Complex Table (Many Columns)

**Result:** Horizontal scrolling enabled, columns maintain minimum widths

```html
<div class="table-wrapper">
    <table>
        <colgroup>
            <col class="col-id">
            <col class="col-name">
            <col class="col-email">
            <col class="col-phone">
            <col class="col-date">
            <col class="col-status">
            <col class="col-description">
            <col class="col-actions">
        </colgroup>
        <thead>
            <tr>
                <th>ID</th>
                <th>Name</th>
                <th>Email</th>
                <th>Phone</th>
                <th>Created</th>
                <th>Status</th>
                <th>Notes</th>
                <th>Actions</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="center">1</td>
                <td>John Doe</td>
                <td>john.doe@example.com</td>
                <td>555-1234</td>
                <td>2024-01-15</td>
                <td class="center">Active</td>
                <td class="wrap">Patient requires special attention</td>
                <td class="actions">
                    <button>View</button>
                    <button>Edit</button>
                </td>
            </tr>
        </tbody>
    </table>
</div>
```

**Behavior:**
- Each column sizes to its longest content
- Minimum widths prevent columns from becoming too narrow
- If total width > container: horizontal scroll appears
- If total width < container: table expands to fill space

### Example 3: Variable Content Lengths

```html
<div class="table-wrapper">
    <table>
        <colgroup>
            <col class="col-id">
            <col class="col-name">
            <col class="col-status">
        </colgroup>
        <thead>
            <tr>
                <th>ID</th>
                <th>Medicine Name</th>
                <th>Status</th>
            </tr>
        </thead>
        <tbody>
            <tr>
                <td class="center">1</td>
                <td>Aspirin</td>
                <td class="center">Active</td>
            </tr>
            <tr>
                <td class="center">2</td>
                <td>Acetaminophen Extended Release</td>
                <td class="center">Active</td>
            </tr>
            <tr>
                <td class="center">3</td>
                <td>Ibuprofen</td>
                <td class="center">Discontinued</td>
            </tr>
        </tbody>
    </table>
</div>
```

**Behavior:**
- ID column: 60px (minimum)
- Medicine Name column: Sizes to "Acetaminophen Extended Release" (~230px)
- Status column: Sizes to "Discontinued" (~120px)
- Short entries like "Aspirin" don't make column narrow

---

## Best Practices

### 1. When to Use Column Classes

**Always use for:**
- ID columns (`col-id`)
- Date/time columns (`col-date`, `col-time`)
- Status/badge columns (`col-status`)
- Action columns (`col-actions`)

**Optional for:**
- Name columns (if names are consistently short)
- Email columns (if emails vary widely in length)

**Skip for:**
- Tables with very few columns (3 or less)
- Tables where all content is similar length

### 2. Content Wrapping Strategy

**Keep nowrap (default) for:**
- IDs, dates, numbers, short status text
- Columns with consistent short content
- Preserves compact layout

**Use `.wrap` for:**
- Description/notes columns
- Any column with unpredictable long text
- Columns where full content visibility is critical

### 3. Alignment Guidelines

```html
<!-- Numbers and IDs -->
<td class="center">123</td>
<td class="right">$1,234.56</td>

<!-- Status badges -->
<td class="center">Active</td>

<!-- Text content -->
<td>John Doe</td>  <!-- Left-aligned by default -->
```

### 4. Mobile Considerations

- Tables automatically allow wrapping on mobile (≤768px)
- Minimum widths are reduced on mobile for better space usage
- Horizontal scrolling remains available
- Test with actual device viewport sizes

---

## Behavior Summary

### Scenario: Few Columns, Short Content
- **Result:** Table fills container width
- **Column width:** Close to minimum thresholds
- **No scrolling:** Table fits in viewport
- **Example:** 3-column patient list with ID, Name, Actions

### Scenario: Few Columns, Long Content
- **Result:** Columns expand with content
- **Column width:** Much wider than minimums
- **May scroll:** If content is very long
- **Example:** 2-column table with long descriptions

### Scenario: Many Columns
- **Result:** Horizontal scrolling enabled
- **Column width:** Content-based, respecting minimums
- **Always scrolls:** On narrow viewports
- **Example:** 8-column patient details table

### Scenario: Mixed Content Lengths
- **Result:** Each column independently sizes to its content
- **Column width:** Varies by column, no uniform distribution
- **Optimal space usage:** No wasted space
- **Example:** Medicine table with varying name lengths

---

## Troubleshooting

**Problem:** Column is too narrow for content
- **Solution:** Add appropriate `col-*` class to increase minimum width

**Problem:** Table has large empty space on the right
- **Solution:** This is normal with few columns and short content; table fills container

**Problem:** Text is cut off
- **Solution:** Check if you need to add `class="wrap"` to specific cells

**Problem:** Table scrolls even with few columns
- **Solution:** Content is genuinely too wide; this is correct behavior

**Problem:** Columns are too wide on mobile
- **Solution:** System automatically reduces minimums on mobile; ensure proper wrapping

---

## Migration from Old Tables

### Old Table (Auto-layout, no minimums):
```html
<table>
    <thead>
        <tr>
            <th>ID</th>
            <th>Name</th>
        </tr>
    </thead>
</table>
```

### New Table (Content-based with thresholds):
```html
<table>
    <colgroup>
        <col class="col-id">
        <col class="col-name">
    </colgroup>
    <thead>
        <tr>
            <th>ID</th>
            <th>Name</th>
        </tr>
    </thead>
</table>
```

**Benefits:**
- Columns still size based on content
- But now have minimum readability thresholds
- Better handling of short content
- More predictable layout
