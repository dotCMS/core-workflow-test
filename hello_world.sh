#!/bin/bash

# hello_world.sh - A demonstration script showing AI capabilities
# This script demonstrates various shell scripting features and best practices
# Execute with: ./hello_world.sh

set -euo pipefail  # Enable strict error handling

# Colors for output
readonly RED='\033[0;31m'
readonly GREEN='\033[0;32m'
readonly BLUE='\033[0;34m'
readonly YELLOW='\033[1;33m'
readonly NC='\033[0m' # No Color

# Function to print colored output
print_colored() {
    local color=$1
    local message=$2
    echo -e "${color}${message}${NC}"
}

# Function to display a banner
display_banner() {
    print_colored "$BLUE" "=================================================="
    print_colored "$BLUE" "         Hello World - AI Demonstration"
    print_colored "$BLUE" "=================================================="
    echo
}

# Function to demonstrate system information
show_system_info() {
    print_colored "$YELLOW" "📋 System Information:"
    echo "   • Operating System: $(uname -s)"
    echo "   • Architecture: $(uname -m)"
    echo "   • Hostname: $(hostname)"
    echo "   • Current User: $(whoami)"
    echo "   • Current Directory: $(pwd)"
    echo "   • Date/Time: $(date)"
    echo
}

# Function to demonstrate file operations
demonstrate_file_operations() {
    print_colored "$YELLOW" "📁 File Operations Demo:"
    
    # Create a temporary file
    local temp_file=$(mktemp)
    echo "Hello from AI!" > "$temp_file"
    echo "   • Created temporary file: $temp_file"
    echo "   • File contents: $(cat "$temp_file")"
    echo "   • File size: $(wc -c < "$temp_file") bytes"
    
    # Clean up
    rm "$temp_file"
    echo "   • Temporary file cleaned up"
    echo
}

# Function to demonstrate mathematical operations
show_math_demo() {
    print_colored "$YELLOW" "🔢 Mathematical Operations:"
    local a=42
    local b=8
    echo "   • Addition: $a + $b = $((a + b))"
    echo "   • Subtraction: $a - $b = $((a - b))"
    echo "   • Multiplication: $a × $b = $((a * b))"
    echo "   • Division: $a ÷ $b = $((a / b))"
    echo "   • Random number (1-100): $((RANDOM % 100 + 1))"
    echo
}

# Function to demonstrate array operations
show_array_demo() {
    print_colored "$YELLOW" "📝 Array Operations:"
    local languages=("Java" "TypeScript" "Shell" "Python" "JavaScript")
    echo "   • Programming languages array:"
    for i in "${!languages[@]}"; do
        echo "     $((i+1)). ${languages[i]}"
    done
    echo "   • Total languages: ${#languages[@]}"
    echo
}

# Function to show process information
show_process_info() {
    print_colored "$YELLOW" "⚙️  Process Information:"
    echo "   • Script PID: $$"
    echo "   • Parent PID: $PPID"
    echo "   • Number of parameters: $#"
    if [ $# -gt 0 ]; then
        echo "   • Parameters passed: $*"
    fi
    echo
}

# Function to demonstrate conditional logic
show_conditional_demo() {
    print_colored "$YELLOW" "🤔 Conditional Logic Demo:"
    local hour=$(date +%H)
    
    if [ "$hour" -lt 12 ]; then
        echo "   • Good morning! ☀️"
    elif [ "$hour" -lt 17 ]; then
        echo "   • Good afternoon! 🌤️"
    else
        echo "   • Good evening! 🌙"
    fi
    
    # Check if running on GitHub Actions
    if [ -n "${GITHUB_ACTIONS:-}" ]; then
        echo "   • Running on GitHub Actions 🚀"
    else
        echo "   • Running locally 💻"
    fi
    echo
}

# Main function
main() {
    display_banner
    
    print_colored "$GREEN" "🎯 What this script demonstrates:"
    echo "   ✓ Proper shell script structure with functions"
    echo "   ✓ Error handling with 'set -euo pipefail'"
    echo "   ✓ Colored output for better readability"
    echo "   ✓ System information gathering"
    echo "   ✓ File operations with cleanup"
    echo "   ✓ Mathematical calculations"
    echo "   ✓ Array handling"
    echo "   ✓ Conditional logic"
    echo "   ✓ Process information"
    echo
    
    show_system_info
    show_process_info "$@"
    demonstrate_file_operations
    show_math_demo
    show_array_demo
    show_conditional_demo
    
    print_colored "$GREEN" "✅ Hello World demonstration completed successfully!"
    print_colored "$BLUE" "This script showcases various AI capabilities in shell scripting."
    echo
}

# Script entry point
main "$@"