# Test Corrected Branch Patterns

This file tests the workflow with corrected branch naming patterns in the failure comment.

## Fix Applied
Updated the failure comment to show the correct branch patterns that match the actual workflow regex:

### Before (Incorrect):
- `issue-123-feature-description`
- `123-feature-description`  
- `feature-issue-123-description`

### After (Correct):
- `123-feature-description` (number at start) - matches `^([0-9]+)-`
- `issue-123-feature-description` (issue-number at start) - matches `^issue-([0-9]+)-`
- `feature-issue-123` (issue-number anywhere) - matches `issue-([0-9]+)`

## Test Branch
Branch name: `test-corrected-patterns` (no issue pattern)

## Expected Results
1. ✅ YAML validation passes
2. ❌ Workflow fails (no issue found)
3. 📝 Comment shows corrected patterns that actually work
4. 🎯 Users get accurate guidance they can actually use

## Success Criteria
- Workflow fails appropriately
- Comment appears with correct branch patterns
- Patterns match the actual regex detection logic
- Users can follow the guidance successfully

This validates that the failure comment now provides accurate, actionable guidance.

Branch: test-corrected-patterns
Expected: Workflow failure with accurate branch pattern guidance