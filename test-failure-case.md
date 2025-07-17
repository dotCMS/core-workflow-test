# Test Failure Case - Random Branch Name

This file tests the enhanced workflow's failure handling when no issue can be detected.

## Test Scenario 3: Failure Case
Branch name: `random-branch-name-should-fail`
- ❌ No issue number at start (not `123-feature`)
- ❌ No `issue-` prefix (not `issue-123-feature`)
- ❌ No `issue-` pattern anywhere (not `feature-issue-123`)
- ❌ No manual linking in Development section
- ❌ No keywords in PR body (fixes/closes/resolves)

## Expected Behavior
The workflow should:
1. Try all branch name patterns - find nothing
2. Check for manual linking - find nothing  
3. Check PR body for keywords - find nothing
4. **FAIL** with clear error message:
   ```
   ::error::No issue number found in Development section, PR body, or branch name
   ::error::Please link an issue using one of these methods:
   ::error::1. Link via GitHub UI: Go to PR → Development section → Link issue
   ::error::2. Add 'fixes #123' (or closes/resolves) to PR body, or
   ::error::3. Use branch naming like 'issue-123-feature' or '123-feature'
   ```

## Success Criteria
- ✅ Workflow run should fail (not succeed)
- ✅ Clear error message with actionable guidance
- ✅ No comment created on any issue
- ✅ No PR body modification
- ✅ Proper exit code (1)

This validates that the enhanced workflow maintains the strict requirement for issue linking while providing helpful guidance for resolution.

Branch: random-branch-name-should-fail
Expected: Workflow failure with helpful error message