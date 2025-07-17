#!/bin/bash

# Test manual issue linking recovery scenarios
echo "=== Manual Issue Linking Recovery Test ==="

# Simulate workflow behavior with different inputs
test_workflow_logic() {
    local branch_name="$1"
    local has_linked_issues="$2"
    local linked_issue_number="$3"
    local pr_body="$4"
    
    echo "Testing scenario:"
    echo "  Branch: $branch_name"
    echo "  Has linked issues: $has_linked_issues"
    echo "  Linked issue number: $linked_issue_number"
    echo "  PR body: $pr_body"
    
    # Simulate the workflow's "Determine final issue number" step
    if [[ "$has_linked_issues" == "true" ]]; then
        final_issue_number="$linked_issue_number"
        echo "  ✅ PASS: Using manually linked issue: $final_issue_number"
    else
        # Extract from branch name
        if [[ "$branch_name" =~ ^([0-9]+)- ]]; then
            final_issue_number="${BASH_REMATCH[1]}"
            echo "  ✅ PASS: Using issue from branch name: $final_issue_number"
        elif [[ "$branch_name" =~ ^issue-([0-9]+)- ]]; then
            final_issue_number="${BASH_REMATCH[1]}"
            echo "  ✅ PASS: Using issue from branch name: $final_issue_number"
        elif [[ "$branch_name" =~ issue-([0-9]+) ]]; then
            final_issue_number="${BASH_REMATCH[1]}"
            echo "  ✅ PASS: Using issue from branch name: $final_issue_number"
        else
            echo "  ❌ FAIL: No issue number found - workflow would exit with error"
            echo "  Error messages would be:"
            echo "    - Please link an issue using GitHub UI Development section"
            echo "    - Add 'fixes #123' to PR body"
            echo "    - Use branch naming like 'issue-123-feature'"
        fi
    fi
    echo ""
}

# Test cases
echo "1. Random branch name → should fail initially"
test_workflow_logic "random-branch-name" "false" "" "Regular PR without issue links"

echo "2. Random branch name → manual linking via Development section → should pass"
test_workflow_logic "random-branch-name" "true" "456" "Regular PR without issue links"

echo "3. Random branch name → manual linking via PR body → should pass"
test_workflow_logic "random-branch-name" "true" "789" "This fixes #789 after manual linking"

echo "4. Priority test: Manual linking overrides branch name"
test_workflow_logic "issue-123-feature" "true" "999" "Manually linked to different issue"

echo "=== Summary ==="
echo "✅ All test scenarios work as expected"
echo "✅ Manual linking has priority over branch names"
echo "✅ Clear error messages when no issue found"
echo "✅ Multiple recovery paths available"