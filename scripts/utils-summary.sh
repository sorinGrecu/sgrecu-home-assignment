#!/usr/bin/env bash

# Function to print a summary of the current configuration
# Used by both bootstrap.sh and run.sh
print_configuration_summary() {
  echo ""
  echo "✅ Configuration Summary:"
  echo "══════════════════════════════════════════════════════"
  
  # Check if FRONTEND_ONLY and BACKEND_ONLY are defined, otherwise set defaults
  local frontend_only=${FRONTEND_ONLY:-false}
  local backend_only=${BACKEND_ONLY:-false}
  local quick_run=${QUICK_RUN:-false}
  
  # Print backend variables if not in frontend-only mode
  if ! ${frontend_only} && [[ -f "backend/.env" ]]; then
    print_env_section "backend/.env" "📦 Backend Environment Variables"
    echo ""
  fi
  
  # Print frontend variables if not in backend-only mode
  if ! ${backend_only} && [[ -f "frontend/.env" ]]; then
    print_env_section "frontend/.env" "🖥️  Frontend Environment Variables"
  fi
  
  echo "══════════════════════════════════════════════════════"
  
  if ${quick_run}; then
    echo "🚀 Quick run mode is active: Using H2 file-based database"
    echo "   Backend will use dev profile with file-based database for data persistence"
    echo "   Your data will be stored in the backend/data directory"
    echo "   Environment variables related to database connection will be ignored"
  fi
  
  echo "✨ Configuration complete! Run ./run.sh to start the application"
}

# Helper function to print a section of environment variables from a file
# with proper handling of sensitive values
print_env_section() {
  local env_file="$1"
  local section_title="$2"
  
  echo "${section_title}:"
  echo "──────────────────────────────────────────────────────"
  
  while IFS= read -r line || [ -n "$line" ]; do
    # Skip comments and empty lines
    [[ $line =~ ^#.*$ || -z $line ]] && continue
    
    # Extract and print the variable name and value
    if [[ $line =~ ^([A-Za-z0-9_]+)=(.*)$ ]]; then
      local var_name="${BASH_REMATCH[1]}"
      local var_value="${BASH_REMATCH[2]}"
      
      # Mask sensitive values
      if [[ "$var_name" =~ (?i)SECRET|PASSWORD|JWT ]]; then
        if [ -n "$var_value" ]; then
          echo "  ${var_name} = ********"
        else
          echo "  ${var_name} = <empty>"
        fi
      else
        if [ -n "$var_value" ]; then
          echo "  ${var_name} = ${var_value}"
        else
          echo "  ${var_name} = <empty>"
        fi
      fi
    fi
  done < "${env_file}"
} 