#!/usr/bin/env bash
set -euo pipefail

# Load utility functions if available
UTILS_DIR="./scripts"
[ -f "${UTILS_DIR}/utils-summary.sh" ] && source "${UTILS_DIR}/utils-summary.sh"
[ -f "${UTILS_DIR}/utils-setup.sh" ] && source "${UTILS_DIR}/utils-setup.sh"
[ -f "${UTILS_DIR}/utils-env.sh" ] && source "${UTILS_DIR}/utils-env.sh"

# Parse command line arguments
FRONTEND_ONLY=false
BACKEND_ONLY=false
RESTART=false
QUICK_RUN=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --frontend-only)   FRONTEND_ONLY=true ;;
    --backend-only)    BACKEND_ONLY=true ;;
    --restart)         RESTART=true ;;
    --quick-run)       QUICK_RUN=true ;;
    *)                 echo "Unknown flag: $1"; exit 1 ;;
  esac
  shift
done

# Helper function to safely set or append a variable to an env file
set_or_append_var() {
  local file=$1 var=$2 val=$3
  
  # Create a temporary file
  local tmpfile="${file}.tmp"
  
  # Ensure file exists
  touch "$file"
  
  if grep -qE "^[[:space:]]*#?[[:space:]]*${var}=" "$file"; then
    # Variable exists (commented or uncommented) - update it using awk instead of sed
    # This will handle special characters better than sed
    awk -v var="$var" -v val="$val" '
      $0 ~ "^[[:space:]]*#?[[:space:]]*"var"=" {
        print var"="val
        next
      }
      { print }
    ' "$file" > "$tmpfile" && mv "$tmpfile" "$file"
  else
    # Variable doesn't exist - append to end
    # Use printf instead of echo to avoid encoding issues
    printf "%s=%s\n" "$var" "$val" >> "$file"
  fi
  
  # Remove any BOM or binary garbage at the end of the file
  tr -cd '\11\12\15\40-\176' < "$file" > "$tmpfile" && mv "$tmpfile" "$file"
}

# Display existing environment values if not restarting
display_env_values() {
  local dir=$1
  local display_name=$2
  
  if [[ -f "$dir/.env" ]]; then
    echo "$display_name environment variables:"
    echo "----------------------------------------"
    
    while IFS= read -r line || [ -n "$line" ]; do
      # Skip comments and empty lines
      [[ $line =~ ^#.*$ || -z $line ]] && continue
      
      # Extract and print the variable name and value
      if [[ $line =~ ^([A-Za-z0-9_]+)=(.*)$ ]]; then
        local var_name="${BASH_REMATCH[1]}"
        local var_value="${BASH_REMATCH[2]}"
        
        if [ -n "$var_value" ]; then
          echo "  $var_name = $var_value"
        else
          echo "  $var_name = <empty value>"
        fi
      fi
    done < "$dir/.env"
    
    echo "----------------------------------------"
  fi
}

# If restarting, remove existing .env files to start fresh
if $RESTART; then
  echo "Restarting configuration from scratch..."
  
  if ! $FRONTEND_ONLY && [[ -f "backend/.env" ]]; then
    rm "backend/.env"
    echo "Removed existing backend/.env file"
  fi
  
  if ! $BACKEND_ONLY && [[ -f "frontend/.env" ]]; then
    rm "frontend/.env"
    echo "Removed existing frontend/.env file"
  fi
else
  # Display existing values if not restarting
  if ! $FRONTEND_ONLY && [[ -f "backend/.env" ]]; then
    display_env_values "backend" "Backend"
  fi
  
  if ! $BACKEND_ONLY && [[ -f "frontend/.env" ]]; then
    display_env_values "frontend" "Frontend"
  fi
fi

# Copy .env.example to .env if .env doesn't exist
copy_if_missing() {
  local dir=$1
  
  if [[ ! -f "$dir/.env" ]]; then
    cp "$dir/.env.example" "$dir/.env"
    echo "Created $dir/.env from example file"
  fi
}

# Only copy missing files for the components being set up
if ! $FRONTEND_ONLY; then
  copy_if_missing backend
fi

if ! $BACKEND_ONLY; then
  copy_if_missing frontend
fi

# If neither flag is set, we're running for both components
if ! $FRONTEND_ONLY && ! $BACKEND_ONLY; then
  echo "── Bootstrap wizard for backend and frontend ──"
else
  if $FRONTEND_ONLY; then
    echo "── Bootstrap wizard for frontend only ──"
  else
    echo "── Bootstrap wizard for backend only ──"
  fi
fi

# Function to extract comment info from variable in .env.example
get_var_comment() {
  local env_file=$1
  local var_name=$2
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

# Check if variable needs auto-generation based on comments
needs_auto_generation() {
  local comment=$1
  local var_name=$2
  
  # Check for auto-generation hints in comments
  if echo "$comment" | grep -q -i "auto.generate"; then
    return 0
  fi
  if echo "$comment" | grep -q -i "will be auto.generated"; then
    return 0
  fi
  
  # Special cases for known keys that should be auto-generated if empty
  # Skip JWT-related secrets since we handle them separately
  if [[ "$var_name" == "NEXTAUTH_SECRET" || "$var_name" == "JBHA_JWT_SECRET_KEY" ]]; then
    return 1
  fi
  
  # For quick run, database variables will be auto-generated
  if $QUICK_RUN && [[ "$var_name" == JBHA_DB_* ]]; then
    return 0
  fi
  
  return 1
}

# Check if variable is a database variable
is_database_var() {
  local var_name=$1
  
  # Check if this is a database-related variable
  if [[ "$var_name" == JBHA_DB_* ]]; then
    return 0
  fi
  
  return 1
}

# Check if variable is a JWT secret variable
is_jwt_var() {
  local var_name=$1
  
  if [[ "$var_name" == "NEXTAUTH_SECRET" || "$var_name" == "JBHA_JWT_SECRET_KEY" ]]; then
    return 0
  fi
  
  return 1
}

# Check if variable is a Google OAuth credential variable
is_google_oauth_var() {
  local var_name=$1
  
  if [[ "$var_name" == "JBHA_GOOGLE_CLIENT_ID" || "$var_name" == "JBHA_GOOGLE_CLIENT_SECRET" ]]; then
    return 0
  fi
  
  return 1
}

# Generate a secure random key
generate_secure_key() {
  openssl rand -hex 32 2>/dev/null || head -c 32 /dev/urandom | base64
}

# Simple function to handle JWT secret for both frontend and backend
setup_jwt_secret() {
  # Only proceed if we're configuring both frontend and backend
  if $FRONTEND_ONLY || $BACKEND_ONLY; then
    return
  fi
  
  # Skip if either .env file is missing
  if [[ ! -f "frontend/.env" || ! -f "backend/.env" ]]; then
    return
  fi
  
  echo "Setting up JWT secret for authentication..."
  
  # Generate a secure key by default
  local jwt_secret=$(generate_secure_key)
  
  # Ask if user wants to provide their own JWT secret
  read -rp "Enter JWT secret (or leave empty to use auto-generated value): " user_jwt
  
  # Use user-provided secret if given
  if [ -n "$user_jwt" ]; then
    jwt_secret="$user_jwt"
  else
    echo "✓ Generated secure JWT secret"
  fi
  
  # Set the same JWT secret for both frontend and backend
  set_or_append_var "frontend/.env" "NEXTAUTH_SECRET" "$jwt_secret"
  set_or_append_var "backend/.env" "JBHA_JWT_SECRET_KEY" "$jwt_secret"
  
  echo "✓ JWT secret configured for both frontend and backend"
}

# Simple function to handle Google OAuth credentials for both frontend and backend
setup_google_credentials() {
  # Only proceed if we're configuring both frontend and backend
  if $FRONTEND_ONLY || $BACKEND_ONLY; then
    return
  fi
  
  # Skip if either .env file is missing
  if [[ ! -f "frontend/.env" || ! -f "backend/.env" ]]; then
    return
  fi
  
  echo "Setting up Google OAuth credentials..."
  
  # Check existing values
  local frontend_client_id=""
  local backend_client_id=""
  local frontend_client_secret=""
  
  if grep -q "^JBHA_GOOGLE_CLIENT_ID=" "frontend/.env"; then
    frontend_client_id=$(grep "^JBHA_GOOGLE_CLIENT_ID=" "frontend/.env" | cut -d'=' -f2)
  fi
  
  if grep -q "^JBHA_GOOGLE_CLIENT_ID=" "backend/.env"; then
    backend_client_id=$(grep "^JBHA_GOOGLE_CLIENT_ID=" "backend/.env" | cut -d'=' -f2)
  fi
  
  if grep -q "^JBHA_GOOGLE_CLIENT_SECRET=" "frontend/.env"; then
    frontend_client_secret=$(grep "^JBHA_GOOGLE_CLIENT_SECRET=" "frontend/.env" | cut -d'=' -f2)
  fi
  
  # Determine the client ID to use
  local client_id=""
  if [ -n "$frontend_client_id" ] && [ -n "$backend_client_id" ] && [ "$frontend_client_id" != "$backend_client_id" ]; then
    echo "⚠️  Different Google Client IDs found:"
    echo "  Frontend: $frontend_client_id"
    echo "  Backend: $backend_client_id"
    read -rp "Which one to use? [f]rontend/[b]ackend: " id_choice
    id_choice=$(echo "$id_choice" | tr '[:upper:]' '[:lower:]')
    
    if [ "$id_choice" = "f" ] || [ "$id_choice" = "frontend" ]; then
      client_id="$frontend_client_id"
    else
      client_id="$backend_client_id"
    fi
  elif [ -n "$frontend_client_id" ]; then
    client_id="$frontend_client_id"
  elif [ -n "$backend_client_id" ]; then
    client_id="$backend_client_id"
  fi
  
  # Prompt for Google Client ID if not found
  if [ -z "$client_id" ]; then
    read -rp "Enter Google Client ID: " client_id
    if [ -z "$client_id" ]; then
      echo "⚠️  Google Client ID is required for OAuth authentication"
      # Retry once more
      read -rp "Enter Google Client ID (required): " client_id
      if [ -z "$client_id" ]; then
        echo "⚠️  No Google Client ID provided. You will need to set this manually later."
      fi
    fi
  else
    # Give option to change existing ID
    read -rp "Enter Google Client ID (or press Enter to keep: $client_id): " new_client_id
    if [ -n "$new_client_id" ]; then
      client_id="$new_client_id"
    fi
  fi
  
  # Prompt for Google Client Secret (frontend only)
  local client_secret=""
  
  echo -e "\033[31mIMPORTANT: The Google Client Secret should have been received in an email from Sorin.\033[0m"
  
  if [ -n "$frontend_client_secret" ]; then
    client_secret="$frontend_client_secret"
    # Give option to change existing secret
    read -rp "Enter Google Client Secret (or press Enter to keep current value): " new_client_secret
    if [ -n "$new_client_secret" ]; then
      client_secret="$new_client_secret"
    fi
  else
    # Only need to prompt for secret if it's not set
    while [ -z "$client_secret" ]; do
      read -rp "Enter Google Client Secret (required for frontend): " client_secret
      if [ -z "$client_secret" ]; then
        echo -e "\033[31mERROR: Google Client Secret is required and must not be empty.\033[0m"
        echo -e "\033[31mThis value should have been received in your email from Sorin.\033[0m"
      fi
    done
  fi
  
  # Set the values in both config files
  if [ -n "$client_id" ]; then
    set_or_append_var "frontend/.env" "JBHA_GOOGLE_CLIENT_ID" "$client_id"
    set_or_append_var "backend/.env" "JBHA_GOOGLE_CLIENT_ID" "$client_id"
    echo "✓ Google Client ID configured for both frontend and backend"
  fi
  
  # Set client secret only for frontend
  if [ -n "$client_secret" ]; then
    set_or_append_var "frontend/.env" "JBHA_GOOGLE_CLIENT_SECRET" "$client_secret"
    echo "✓ Google Client Secret configured for frontend"
    
    # Remove any existing client secret from backend if present
    if grep -q "^JBHA_GOOGLE_CLIENT_SECRET=" "backend/.env"; then
      local tmpfile="backend/.env.tmp"
      grep -v "^JBHA_GOOGLE_CLIENT_SECRET=" "backend/.env" > "$tmpfile" && mv "$tmpfile" "backend/.env"
      echo "✓ Removed Google Client Secret from backend (not needed)"
    fi
  else
    # This shouldn't happen due to the validation above, but just in case
    echo -e "\033[31mERROR: No Google Client Secret provided. Cannot continue.\033[0m"
    exit 1
  fi
}

# Process environment variables for a directory's .env file
process_env_file() {
  local dir=$1
  local display_name=$2
  local env_example="$dir/.env.example"
  local env_file="$dir/.env"
  
  # Extract all environment variables from .env.example
  local all_vars=()
  local required_vars=()
  local optional_vars=()
  local var_values=()
  local var_comments=()
  
  # Read .env.example line by line to categorize variables
  while IFS= read -r line || [ -n "$line" ]; do
    # Skip comments and empty lines
    [[ $line =~ ^#.*$ || -z $line ]] && continue
    
    # Extract the variable name and value
    if [[ $line =~ ^([A-Za-z0-9_]+)=(.*)$ ]]; then
      local var_name="${BASH_REMATCH[1]}"
      local var_value="${BASH_REMATCH[2]}"
      
      all_vars+=("$var_name")
      var_values+=("$var_value")
      
      # Get associated comments for this variable
      local comment=$(get_var_comment "$env_example" "$var_name")
      var_comments+=("$comment")
      
      # Empty value means it's required
      if [ -z "$var_value" ]; then
        required_vars+=("$var_name")
      else
        optional_vars+=("$var_name")
      fi
    fi
  done < "$env_example"
  
  echo "Setting up $display_name environment variables..."
  
  # Process required variables first
  if [ ${#required_vars[@]} -gt 0 ]; then
    echo "Required variables:"
    for i in "${!required_vars[@]}"; do
      local var_name="${required_vars[$i]}"
      local current_value=""
      local comment=""
      
      # Skip JWT variables as they are handled separately
      if is_jwt_var "$var_name"; then
        continue
      fi
      
      # Skip Google OAuth variables as they are handled separately
      if is_google_oauth_var "$var_name"; then
        continue
      fi
      
      # Find the comment for this variable
      for j in "${!all_vars[@]}"; do
        if [[ "${all_vars[$j]}" == "$var_name" ]]; then
          comment="${var_comments[$j]}"
          break
        fi
      done
      
      # Check if variable already has a value in .env
      if grep -q "^$var_name=" "$env_file"; then
        current_value=$(grep "^$var_name=" "$env_file" | cut -d'=' -f2)
      fi
      
      # Handle auto-generation hint from comments
      local can_auto_generate=false
      if needs_auto_generation "$comment" "$var_name"; then
        can_auto_generate=true
        
        # Extract description from comment if available
        local description=""
        if [ -n "$comment" ]; then
          description=$(echo "$comment" | grep -v "auto.generat" | tr '\n' ' ' | sed 's/# //g' | sed 's/^[ \t]*//;s/[ \t]*$//')
          if [ -n "$description" ]; then
            description=" ($description)"
          fi
        fi
        
        # Special handling for database variables in quick run mode
        if $QUICK_RUN && is_database_var "$var_name"; then
          # Auto-generate values for database variables
          local value=""
          if [[ "$var_name" == "JBHA_DB_HOST" ]]; then
            value="localhost"
          elif [[ "$var_name" == "JBHA_DB_PORT" ]]; then
            value="9092"
          elif [[ "$var_name" == "JBHA_DB_NAME" ]]; then
            value="jbhadb"
          elif [[ "$var_name" == "JBHA_DB_USERNAME" ]]; then
            value="sa"
          elif [[ "$var_name" == "JBHA_DB_PASSWORD" ]]; then
            value=""
          else
            # Generate a random value for other db vars
            value=$(generate_secure_key)
          fi
          echo "  ✓ Auto-configured $var_name for quick run mode (H2 database)"
          set_or_append_var "$env_file" "$var_name" "$value"
          continue
        fi
        
        echo "  $var_name$description will be auto-generated if left empty"
        
        # Only prompt if not already set
        if [ -z "$current_value" ]; then
          read -rp "  $display_name $var_name (leave empty to auto-generate): " value
          
          # If left empty, generate a secure key now
          if [ -z "$value" ]; then
            value=$(generate_secure_key)
            echo "  ✓ Generated secure value for $var_name"
          fi
          
          set_or_append_var "$env_file" "$var_name" "$value"
          continue
        else
          echo "  $var_name: already set ($current_value)"
          continue
        fi
      fi
      
      # If quick run and this is a database variable, skip prompting
      if $QUICK_RUN && is_database_var "$var_name"; then
        local value=""
        if [[ "$var_name" == "JBHA_DB_HOST" ]]; then
          value="localhost"
        elif [[ "$var_name" == "JBHA_DB_PORT" ]]; then
          value="9092"
        elif [[ "$var_name" == "JBHA_DB_NAME" ]]; then
          value="jbhadb"
        elif [[ "$var_name" == "JBHA_DB_USERNAME" ]]; then
          value="sa"
        elif [[ "$var_name" == "JBHA_DB_PASSWORD" ]]; then
          value=""
        else
          # Generate a random value for other db vars
          value=$(generate_secure_key)
        fi
        echo "  ✓ Auto-configured $var_name for quick run mode (H2 database)"
        set_or_append_var "$env_file" "$var_name" "$value"
        continue
      fi
      
      # If variable is empty or doesn't exist, prompt for a value
      if [ -z "$current_value" ]; then
        local value=""
        while [ -z "$value" ]; do
          read -rp "  $display_name $var_name: " value
          if [ -z "$value" ]; then
            echo "  ✖︎ Value required for $var_name"
          fi
        done
        
        set_or_append_var "$env_file" "$var_name" "$value"
      else
        echo "  $var_name: already set ($current_value)"
      fi
    done
  fi
  
  # Process optional variables
  if [ ${#optional_vars[@]} -gt 0 ]; then
    echo "Optional variables (press Enter to keep default value):"
    for var_name in "${optional_vars[@]}"; do
      # Skip JWT variables as they are handled separately
      if is_jwt_var "$var_name"; then
        continue
      fi
      
      # Skip Google OAuth variables as they are handled separately
      if is_google_oauth_var "$var_name"; then
        continue
      fi
      
      # Get the current value from .env
      local current_value=""
      if grep -q "^$var_name=" "$env_file"; then
        current_value=$(grep "^$var_name=" "$env_file" | cut -d'=' -f2)
      fi
      
      # Get the default value from .env.example
      local default_value=""
      for i in "${!all_vars[@]}"; do
        if [[ "${all_vars[$i]}" == "$var_name" ]]; then
          default_value="${var_values[$i]}"
          break
        fi
      done
      
      # If quick run and this is a database variable, auto-configure it
      if $QUICK_RUN && is_database_var "$var_name"; then
        local value=""
        if [[ "$var_name" == "JBHA_DB_HOST" ]]; then
          value="localhost"
        elif [[ "$var_name" == "JBHA_DB_PORT" ]]; then
          value="9092"
        elif [[ "$var_name" == "JBHA_DB_NAME" ]]; then
          value="jbhadb"
        elif [[ "$var_name" == "JBHA_DB_USERNAME" ]]; then
          value="sa"
        elif [[ "$var_name" == "JBHA_DB_PASSWORD" ]]; then
          value=""
        else
          # Use default value for other db vars
          value="$default_value"
        fi
        echo "  ✓ Auto-configured $var_name for quick run mode (H2 database)"
        set_or_append_var "$env_file" "$var_name" "$value"
        continue
      fi
      
      # Only prompt if current value is not set or equal to default
      if [ -z "$current_value" ] || [[ "$current_value" == "$default_value" ]]; then
        read -rp "  $display_name $var_name [$default_value]: " value
        
        if [ -z "$value" ]; then
          # Use the default value from .env.example
          value="$default_value"
          echo "  Using default: $default_value"
        fi
        
        set_or_append_var "$env_file" "$var_name" "$value"
      else
        echo "  $var_name: already set ($current_value)"
      fi
    done
  fi
  
  # If quick run mode, add a comment at the top of the file about database settings
  if $QUICK_RUN && [[ "$dir" == "backend" ]]; then
    cat > "$env_file.new" << EOF
# QUICK RUN MODE ACTIVE: Database settings below are configured for H2 file-based database.
# The data will persist between application restarts in the ./data/chatdb directory.
# If you want to use a real PostgreSQL database, you will need to reconfigure these settings.
# The following database settings contain dummy values that will be ignored in dev mode:
# JBHA_DB_HOST, JBHA_DB_PORT, JBHA_DB_NAME, JBHA_DB_USERNAME, JBHA_DB_PASSWORD

EOF

    # First, read the file and collect all non-DB variables
    declare -A non_db_vars
    while IFS= read -r line || [ -n "$line" ]; do
      # Skip comments and empty lines
      [[ $line =~ ^#.*$ || -z $line ]] && continue
      
      # Extract variable name and value
      if [[ "$line" =~ ^([A-Za-z0-9_]+)=(.*)$ ]]; then
        var_name="${BASH_REMATCH[1]}"
        var_value="${BASH_REMATCH[2]}"
        
        # Store non-DB variables
        if [[ ! "$var_name" =~ ^JBHA_DB_ ]]; then
          non_db_vars["$var_name"]="$var_value"
        fi
      fi
    done < "$env_file"
    
    # Now append fixed dummy values for all DB variables
    echo "JBHA_DB_HOST=dummy-host" >> "$env_file.new"
    echo "JBHA_DB_PORT=9999" >> "$env_file.new"
    echo "JBHA_DB_NAME=dummy-db" >> "$env_file.new"
    echo "JBHA_DB_USERNAME=dummy-user" >> "$env_file.new"
    echo "JBHA_DB_PASSWORD=dummy-pass" >> "$env_file.new"
    echo "JBHA_DB_SCHEMA=dummy-schema" >> "$env_file.new"
    
    # Then append all non-DB variables
    for var_name in "${!non_db_vars[@]}"; do
      echo "${var_name}=${non_db_vars[$var_name]}" >> "$env_file.new"
    done
    
    # Replace the original file with the updated version
    mv "$env_file.new" "$env_file"
  fi
}

# Synchronize specific paired variables between frontend and backend (excluding JWT vars)
sync_common_variables() {
  # Only run if both frontend and backend are being configured
  if $FRONTEND_ONLY || $BACKEND_ONLY; then
    return
  fi
  
  echo "Synchronizing common variables between frontend and backend..."
  
  # Skip if either .env file is missing
  if [[ ! -f "frontend/.env" || ! -f "backend/.env" ]]; then
    return
  fi
  
  # Hardcoded list of related variable pairs (frontend:backend) - excluding JWT vars
  declare -a pairs
  pairs=()  # Empty array as we've moved Google credentials to their own function
   
  # Process each pair
  for pair in "${pairs[@]}"; do
    # Split the pair
    frontend_var="${pair%%:*}"
    backend_var="${pair##*:}"
    
    # Get current values
    frontend_value=""
    backend_value=""
    
    if grep -q "^$frontend_var=" "frontend/.env"; then
      frontend_value=$(grep "^$frontend_var=" "frontend/.env" | cut -d'=' -f2)
    fi
    
    if grep -q "^$backend_var=" "backend/.env"; then
      backend_value=$(grep "^$backend_var=" "backend/.env" | cut -d'=' -f2)
    fi
    
    # Skip if both values are empty
    if [ -z "$frontend_value" ] && [ -z "$backend_value" ]; then
      continue
    fi
    
    # If both have values but they're different
    if [ -n "$frontend_value" ] && [ -n "$backend_value" ] && [ "$frontend_value" != "$backend_value" ]; then
      echo "  Conflict found for $frontend_var and $backend_var!"
      echo "  Frontend: $frontend_value"
      echo "  Backend: $backend_value"
      
      read -rp "  Use which value? [f]rontend/[b]ackend: " choice
      choice=$(echo "$choice" | tr '[:upper:]' '[:lower:]')
      
      if [ "$choice" = "f" ] || [ "$choice" = "frontend" ]; then
        echo "  Using frontend value for both..."
        set_or_append_var "backend/.env" "$backend_var" "$frontend_value"
      else
        echo "  Using backend value for both..."
        set_or_append_var "frontend/.env" "$frontend_var" "$backend_value"
      fi
    # If frontend has value but backend doesn't
    elif [ -n "$frontend_value" ] && [ -z "$backend_value" ]; then
      echo "  Copying $frontend_var value from frontend to backend..."
      set_or_append_var "backend/.env" "$backend_var" "$frontend_value"
    # If backend has value but frontend doesn't
    elif [ -z "$frontend_value" ] && [ -n "$backend_value" ]; then
      echo "  Copying $backend_var value from backend to frontend..."
      set_or_append_var "frontend/.env" "$frontend_var" "$backend_value"
    # If both values are empty but the variable exists
    elif [ -z "$frontend_value" ] && [ -z "$backend_value" ] && (grep -q "^$frontend_var=" "frontend/.env" || grep -q "^$backend_var=" "backend/.env"); then
      echo "  Both frontend and backend have empty values for $frontend_var/$backend_var"
    fi
  done
}

# Process backend and/or frontend based on flags
if ! $FRONTEND_ONLY; then
  process_env_file "backend" "Backend"
fi

if ! $BACKEND_ONLY; then
  process_env_file "frontend" "Frontend"
fi

# Setup JWT secret for both frontend and backend
setup_jwt_secret

# Setup Google OAuth credentials for both frontend and backend
setup_google_credentials

# Synchronize other common variables between frontend and backend
sync_common_variables

# Print summary of configured environment variables
print_configuration_summary() {
  echo ""
  echo "✅ Configuration Summary:"
  echo "══════════════════════════════════════════════════════"
  
  # Print backend variables
  if ! $FRONTEND_ONLY && [[ -f "backend/.env" ]]; then
    echo "📦 Backend Environment Variables:"
    echo "──────────────────────────────────────────────────────"
    
    while IFS= read -r line || [ -n "$line" ]; do
      # Skip comments and empty lines
      [[ $line =~ ^#.*$ || -z $line ]] && continue
      
      # Extract variable name and value
      if [[ $line =~ ^([A-Za-z0-9_]+)=(.*)$ ]]; then
        local var_name="${BASH_REMATCH[1]}"
        local var_value="${BASH_REMATCH[2]}"
        
        # Mask sensitive values
        if [[ "$var_name" == *"SECRET"* || "$var_name" == *"PASSWORD"* || "$var_name" == *"JWT"* ]]; then
          if [ -n "$var_value" ]; then
            echo "  $var_name = ********"
          else
            echo "  $var_name = <empty>"
          fi
        else
          if [ -n "$var_value" ]; then
            echo "  $var_name = $var_value"
          else
            echo "  $var_name = <empty>"
          fi
        fi
      fi
    done < "backend/.env"
    
    echo ""
  fi
  
  # Print frontend variables
  if ! $BACKEND_ONLY && [[ -f "frontend/.env" ]]; then
    echo "🖥️  Frontend Environment Variables:"
    echo "──────────────────────────────────────────────────────"
    
    while IFS= read -r line || [ -n "$line" ]; do
      # Skip comments and empty lines
      [[ $line =~ ^#.*$ || -z $line ]] && continue
      
      # Extract variable name and value
      if [[ $line =~ ^([A-Za-z0-9_]+)=(.*)$ ]]; then
        local var_name="${BASH_REMATCH[1]}"
        local var_value="${BASH_REMATCH[2]}"
        
        # Mask sensitive values
        if [[ "$var_name" == *"SECRET"* || "$var_name" == *"PASSWORD"* || "$var_name" == *"JWT"* ]]; then
          if [ -n "$var_value" ]; then
            echo "  $var_name = ********"
          else
            echo "  $var_name = <empty>"
          fi
        else
          if [ -n "$var_value" ]; then
            echo "  $var_name = $var_value"
          else
            echo "  $var_name = <empty>"
          fi
        fi
      fi
    done < "frontend/.env"
  fi
  
  echo "══════════════════════════════════════════════════════"
  
  if $QUICK_RUN; then
    echo "🚀 Quick run mode is active: Using H2 in-memory database"
    echo "   Backend will use test profile with in-memory database"
    echo "   Environment variables related to database connection will be ignored"
  fi
  
  echo "✨ Configuration complete! Run ./run.sh to start the application"
}

# Call the summary function
print_configuration_summary

echo "✔︎  .env files prepared." 