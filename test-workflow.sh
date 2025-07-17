#!/bin/bash

# Test script for the improved issue linking workflow
# This simulates the branch name extraction logic from the workflow

test_branch_name() {
    local branch_name="$1"
    local expected_result="$2"
    
    echo "Testing branch: $branch_name"
    
    # Extract issue number using the same logic as the workflow
    issue_number=""
    
    # Try multiple patterns to extract issue number (more flexible but specific)
    if [[ "$branch_name" =~ ^([0-9]+)- ]]; then
        issue_number="${BASH_REMATCH[1]}"
        echo "  Found issue number at start of branch: $issue_number"
    elif [[ "$branch_name" =~ ^issue-([0-9]+)- ]]; then
        issue_number="${BASH_REMATCH[1]}"
        echo "  Found issue number with 'issue-' prefix: $issue_number"
    elif [[ "$branch_name" =~ issue-([0-9]+) ]]; then
        issue_number="${BASH_REMATCH[1]}"
        echo "  Found issue number with 'issue-' anywhere in branch: $issue_number"
    else
        echo "  No issue number found in branch name: $branch_name"
    fi
    
    # Check if result matches expected
    if [[ "$issue_number" == "$expected_result" ]]; then
        echo "  ✅ PASS: Expected '$expected_result', got '$issue_number'"
    else
        echo "  ❌ FAIL: Expected '$expected_result', got '$issue_number'"
    fi
    
    echo ""
}

echo "=== Testing Branch Name Patterns ==="
echo ""

# Test cases that should work
test_branch_name "123-test-feature" "123"
test_branch_name "issue-123-test-feature" "123"
test_branch_name "feature-issue-123-test" "123"

# Test cases that should fail (no issue number found)
test_branch_name "random-branch-name" ""
test_branch_name "feature-v1.2.3-update" ""
test_branch_name "setup-port-8080-config" ""
test_branch_name "update-node-16-deps" ""

echo "=== Testing PR Body Keyword Extraction ==="
echo ""

test_pr_body() {
    local pr_body="$1"
    local expected_result="$2"
    
    echo "Testing PR body: $pr_body"
    
    # Extract issue numbers from PR body using various keywords
    linked_issues=$(echo "$pr_body" | grep -oiE '(fixes?|closes?|resolves?)\s+#([0-9]+)' | grep -oE '[0-9]+' | head -1)
    
    if [[ -n "$linked_issues" ]]; then
        echo "  Found linked issue in PR body: $linked_issues"
    else
        echo "  No linked issues found in PR body"
    fi
    
    # Check if result matches expected
    if [[ "$linked_issues" == "$expected_result" ]]; then
        echo "  ✅ PASS: Expected '$expected_result', got '$linked_issues'"
    else
        echo "  ❌ FAIL: Expected '$expected_result', got '$linked_issues'"
    fi
    
    echo ""
}

# Test PR body keyword extraction
test_pr_body "This fixes #456 and improves performance" "456"
test_pr_body "Closes #789 by implementing new feature" "789"
test_pr_body "Resolves #101 with updated logic" "101"
test_pr_body "This is a regular PR without issue links" ""
test_pr_body "Fix for issue #999 (but not using keyword)" ""

echo "=== Summary ==="
echo "The workflow uses this priority order:"
echo "1. Development section linking (highest priority)"
echo "2. PR body keywords (medium priority)"
echo "3. Branch name patterns (lowest priority)"
echo ""
echo "Branch patterns that work:"
echo "- 123-test-feature (numbers at start)"
echo "- issue-123-test-feature (standard prefix)"
echo "- feature-issue-123-test (issue keyword anywhere)"
echo ""
echo "Edge cases that correctly don't match:"
echo "- feature-v1.2.3-update (version numbers)"
echo "- setup-port-8080-config (port numbers)"
echo "- update-node-16-deps (node version)"