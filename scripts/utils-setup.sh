#!/usr/bin/env bash
#
# utils-setup.sh - Setup utilities for project scripts
#

# Display environment values from a file
# Parameters:
# $1 - directory path
# $2 - display name for the section
display_env_values() {
  local dir="$1"
  local display_name="$2"
  local env_file="${dir}/.env"
  
  if [[ -f "$env_file" ]]; then
    echo "$display_name environment variables:"
    echo "----------------------------------------"
    
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
            echo "  $var_name = ********"
          else
            echo "  $var_name = <empty value>"
          fi
        else
          if [ -n "$var_value" ]; then
            echo "  $var_name = $var_value"
          else
            echo "  $var_name = <empty value>"
          fi
        fi
      fi
    done < "$env_file"
    
    echo "----------------------------------------"
  fi
}

# Copy .env.example to .env if .env doesn't exist
# Parameters:
# $1 - directory to check
copy_env_if_missing() {
  local dir="$1"
  
  if [[ ! -f "${dir}/.env" ]]; then
    cp "${dir}/.env.example" "${dir}/.env"
    echo "Created ${dir}/.env from example file"
  fi
}

# Extract comment info for a variable from .env.example
# Parameters:
# $1 - env file to check
# $2 - variable name
# Returns the comment lines preceding the variable
get_var_comment() {
  local env_file="$1"
  local var_name="$2"
  local prev_lines=""
  
  # Read the file line by line
  while IFS= read -r line || [ -n "$line" ]; do
    # If we found the variable line, return the previous comment lines
    if [[ "$line" =~ ^[[:space:]]*"$var_name"= ]]; then
      echo "$prev_lines"
      return
    fi
    
    # If the line is a comment, add it to prev_lines
    if [[ "$line" =~ ^[[:space:]]*# ]]; then
      # Add this comment line to our accumulated comments
      if [ -n "$prev_lines" ]; then
        prev_lines+=$'\n'"$line"
      else
        prev_lines="$line"
      fi
    else
      # If it's not a comment, reset the accumulated comments
      # (unless it's a blank line, which we'll ignore)
      if [[ ! "$line" =~ ^[[:space:]]*$ ]]; then
        prev_lines=""
      fi
    fi
  done < "$env_file"
  
  # If we didn't find the variable, return empty
  echo ""
}

# Check if a variable needs auto-generation based on its comments
# Parameters:
# $1 - comment text
# $2 - variable name
# Returns 0 if auto-generation is needed, 1 otherwise
needs_auto_generation() {
  local comment="$1"
  local var_name="$2"
  
  # Check for auto-generation hints in comments
  if echo "$comment" | grep -q -i "auto.generate"; then
    return 0
  fi
  if echo "$comment" | grep -q -i "will be auto.generated"; then
    return 0
  fi
  
  return 1
}

# Check if a variable is database-related
# Parameters:
# $1 - variable name
is_database_var() {
  local var_name="$1"
  
  if [[ "$var_name" == JBHA_DB_* ]]; then
    return 0
  fi
  
  return 1
}

# Check if a variable is JWT-related
# Parameters:
# $1 - variable name
is_jwt_var() {
  local var_name="$1"
  
  if [[ "$var_name" == "NEXTAUTH_SECRET" || "$var_name" == "JBHA_JWT_SECRET_KEY" ]]; then
    return 0
  fi
  
  return 1
} 