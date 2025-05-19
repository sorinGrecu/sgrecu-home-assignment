#!/usr/bin/env bash
#
# utils-env.sh - Environment variable utilities for project scripts
#

# Sets or appends a variable to an env file
# Parameters:
# $1 - file path
# $2 - variable name
# $3 - variable value
set_or_append_var() {
  local file="$1" 
  local var="$2" 
  local val="$3"
  
  # Create a temporary file
  local tmpfile="${file}.tmp"
  
  # Ensure file exists
  touch "$file"
  
  if grep -qE "^[[:space:]]*#?[[:space:]]*${var}=" "$file"; then
    # Variable exists (commented or uncommented) - update it
    awk -v var="$var" -v val="$val" '
      $0 ~ "^[[:space:]]*#?[[:space:]]*"var"=" {
        print var"="val
        next
      }
      { print }
    ' "$file" > "$tmpfile" && mv "$tmpfile" "$file"
  else
    # Variable doesn't exist - append to end
    printf "%s=%s\n" "$var" "$val" >> "$file"
  fi
  
  # Remove any BOM or binary garbage at the end of the file
  tr -cd '\11\12\15\40-\176' < "$file" > "$tmpfile" && mv "$tmpfile" "$file"
}

# Extract variable value from env file
# Parameters:
# $1 - file path
# $2 - variable name
# Returns the value or empty string if not found
get_env_var() {
  local file="$1"
  local var="$2"
  
  if [ ! -f "$file" ]; then
    echo ""
    return
  fi
  
  local value=""
  if grep -q "^${var}=" "$file"; then
    value=$(grep "^${var}=" "$file" | cut -d'=' -f2)
  fi
  
  echo "$value"
}

# Generate a secure random string for use as a secret
# Returns a random hex string
generate_secure_key() {
  openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | base64
}

# Load all variables from an env file into the current environment
# Parameters:
# $1 - file path
load_dotenv() {
  local env_file="$1"
  
  if [ ! -f "$env_file" ]; then
    return 1
  fi
  
  while IFS= read -r line || [ -n "$line" ]; do
    # Skip comments and empty lines
    [[ $line =~ ^#.*$ || -z $line ]] && continue
    
    # Extract variable name and value
    if [[ $line =~ ^([A-Za-z0-9_]+)=(.*)$ ]]; then
      local var_name="${BASH_REMATCH[1]}"
      local var_value="${BASH_REMATCH[2]}"
      
      # Trim whitespace
      var_value=$(echo "$var_value" | xargs)
      
      # Export to environment
      export "${var_name}=${var_value}"
    fi
  done < "$env_file"
  
  return 0
}
