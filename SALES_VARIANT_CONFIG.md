# Sales Entry - Variant Configuration Integration

## Overview
Sales entry now automatically filters available variants based on the customer's configured variants. When a customer is selected, only the variants they have configured will appear in the variant dropdown.

## How It Works

### Customer Selection
1. User selects a customer from the customer autocomplete
2. System loads the customer's `configuredVariants` array
3. Variant dropdown is automatically filtered to show only configured variants

### Variant Filtering Logic
- **If customer has configured variants**: Show only those variants
- **If customer has no configured variants**: Show all available variants (backward compatibility)
- **When customer changes**: Variant selection is reset, and dropdown updates automatically

### Price Auto-Fill
After variant is selected, pricing is auto-filled based on:
- Customer's variant-specific pricing (from `customer_variant_price` table)
- If no specific pricing exists, user can enter manually

## Implementation Details

### Frontend Changes

#### sale-entry.component.ts
- **New property**: `customerConfiguredVariants: number[]` - stores configured variant IDs for the selected customer
- **New method**: `filterVariantsByCustomerConfig()` - filters `filteredVariants` based on customer's configuration
- **Updated**: `ngOnInit()` - calls `filterVariantsByCustomerConfig()` when customer is selected

#### sale-entry.component.html
- **Changed**: Variant dropdown now uses `[items]="filteredVariants"` instead of `[items]="variants"`
- Ensures only configured variants are displayed

## Workflow

### Before:
1. Select Customer → See all variants → Select variant → Auto-fill price

### After:
1. Select Customer → **See only configured variants** → Select variant → Auto-fill price

## Backend Integration

The system expects customer object to include:
```json
{
  "id": 1,
  "name": "Customer Name",
  "configuredVariants": [1, 3, 5]  // Array of variant IDs
}
```

## Backward Compatibility

- If `configuredVariants` array is missing or empty, all variants are shown
- Existing customers without configured variants will see all variants
- No data migration required; feature works with existing customer data

## Testing Checklist

- [ ] Select customer with configured variants → See only those variants
- [ ] Select customer without configured variants → See all variants
- [ ] Switch between customers → Variant dropdown updates correctly
- [ ] Variant selection resets when customer changes
- [ ] Pricing auto-fills from customer's variant-specific pricing
- [ ] Can manually override prices if needed
- [ ] Sale submission works with filtered variants

## Error Handling

- If customer object is missing → Empty variant list, variant reset
- If configuredVariants array is null/undefined → Shows all variants
- Invalid variant IDs are silently ignored

## Notes

- This feature requires that customers have been set up with variant configuration in the customer management page
- Variant filtering happens client-side in real-time
- No additional API calls required for filtering
