# Test Workflow Failure

This branch `test-random-branch-should-fail` should trigger a workflow failure because it doesn't contain an issue number in a recognizable pattern.

The enhanced workflow should:
1. Check Development section - none linked
2. Check PR body - no keywords
3. Check branch name - no pattern match
4. Fail with helpful error message