#!/usr/bin/env bash
#
# run.sh - Project bootstrap and runner for backend/frontend
#
# Usage: ./run.sh [--wizard] [--backend-only] [--frontend-only] [--quick-run]
# Description: Configures and starts the project's backend and/or frontend components
#
# Features:
# - Automatic dependency building (Gradle build for backend, npm install for frontend)
# - Cross-platform Gradle wrapper support (gradlew/gradlew.bat)
# - Environment configuration wizard
# - Prerequisites checking (Java 21, Node.js 20+)
# - Error handling with helpful troubleshooting tips
#
# Requires: bash, grep, openssl (or /dev/urandom), direnv (optional)
#

set -euo pipefail

# Get the absolute directory where this script is located
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR" # Change to script directory to ensure all relative paths work

# Constants & Configuration
BACKEND_DIR="$SCRIPT_DIR/backend"
FRONTEND_DIR="$SCRIPT_DIR/frontend"
BOOTSTRAP_SCRIPT="$SCRIPT_DIR/scripts/bootstrap.sh"
ENV_FILE=".env"
ENV_EXAMPLE_FILE=".env.example"
QUICK_RUN=false
UTILS_DIR="$SCRIPT_DIR/scripts"

# Import utility functions if they exist
[ -f "${UTILS_DIR}/utils-summary.sh" ] && source "${UTILS_DIR}/utils-summary.sh"
[ -f "${UTILS_DIR}/utils-setup.sh" ] && source "${UTILS_DIR}/utils-setup.sh"
[ -f "${UTILS_DIR}/utils-env.sh" ] && source "${UTILS_DIR}/utils-env.sh"

# Logging Functions
log_info() {
  echo "$1"
}

log_warn() {
  echo "[WARN] $1"
}

log_error() {
  echo "[ERROR] $1" >&2
}

log_success() {
  echo "$1"
}

check_prerequisites() {
  log_info "Checking prerequisites..."
  
  local missing_prereqs=()
  
  if command -v java &>/dev/null; then
    local java_version=$(java -version 2>&1 | head -n 1 | cut -d '"' -f 2 | cut -d '.' -f 1)
    if [[ "$java_version" -ge 21 ]]; then
      log_info "✓ Java $java_version found"
    else
      log_error "✗ Java 21 required, but found Java $java_version"
      missing_prereqs+=("Java 21")
    fi
  else
    log_error "✗ Java not found in PATH"
    missing_prereqs+=("Java 21")
  fi
  
  # Check Node.js 20+
  if command -v node &>/dev/null; then
    local node_version=$(node --version | sed 's/v//' | cut -d '.' -f 1)
    if [[ "$node_version" -ge 20 ]]; then
      log_info "✓ Node.js v$(node --version | sed 's/v//') found"
    else
      log_error "✗ Node.js 20+ required, but found v$(node --version | sed 's/v//')"
      missing_prereqs+=("Node.js 20+")
    fi
  else
    log_error "✗ Node.js not found in PATH"
    missing_prereqs+=("Node.js 20+")
  fi
  
  # Check npm (should come with Node.js)
  if $RUN_FRONTEND && ! command -v npm &>/dev/null; then
    log_error "✗ npm not found in PATH"
    missing_prereqs+=("npm")
  fi
  
  # Check if we're in a bash-compatible shell
  if [[ -z "${BASH_VERSION:-}" ]]; then
    log_error "✗ This script requires bash"
    missing_prereqs+=("bash shell")
  fi
  
  # If any prerequisites are missing, show help and exit
  if [[ ${#missing_prereqs[@]} -gt 0 ]]; then
    log_error ""
    log_error "Missing prerequisites:"
    for prereq in "${missing_prereqs[@]}"; do
      log_error "  - $prereq"
    done
    log_error ""
    log_error "Please install the missing prerequisites and try again."
    log_error ""
    log_error "Installation help:"
    log_error "  Java 21: https://adoptium.net/ or use your system package manager"
    log_error "  Node.js 20+: https://nodejs.org/ or use nvm (https://github.com/nvm-sh/nvm)"
    log_error ""
    log_error "Windows users: Make sure you're using WSL or Git Bash, not Command Prompt."
    exit 1
  fi
  
  log_success "✓ All prerequisites met!"
  echo ""
}

# CLI Help
usage() {
  cat <<EOF
Usage: ./run.sh [--wizard] [--backend-only] [--frontend-only] [--quick-run]

This script will automatically:
  • Check prerequisites (Java 21, Node.js 20+)
  • Configure environment variables (via wizard if needed)
  • Build and install all dependencies (Gradle + npm/yarn/pnpm)
  • Start the selected services

Options:
  --wizard         Run the configuration wizard even if .env files exist
  --backend-only   Start only the Kotlin backend
  --frontend-only  Start only the React/Next.js frontend
  --quick-run      Start in test mode with H2 in-memory database (auto-configures database env vars)
  -h, --help       Display this help message

Note: Running with --wizard will guide you through configuring environment variables
EOF
  exit 1
}

# Flag Parsing Functions
parse_flags() {
  # Default flag values
  WIZARD=false
  RESTART=false
  RUN_BACKEND=true
  RUN_FRONTEND=true
  BOOTSTRAP_FLAGS=""

  while [[ $# -gt 0 ]]; do
    case "$1" in
      --wizard)          WIZARD=true ;;
      --backend-only)    RUN_FRONTEND=false ;;
      --frontend-only)   RUN_BACKEND=false; RUN_FRONTEND=true ;;
      --quick-run)       QUICK_RUN=true ;;
      -h|--help)         usage ;;
      *)                 log_error "Unknown flag: $1"; usage ;;
    esac
    shift
  done

  # Set bootstrap flags based on component selection
  if $RUN_BACKEND && ! $RUN_FRONTEND; then
    BOOTSTRAP_FLAGS="--backend-only"
  elif ! $RUN_BACKEND && $RUN_FRONTEND; then
    BOOTSTRAP_FLAGS="--frontend-only"
  fi
  
  if $QUICK_RUN; then
    BOOTSTRAP_FLAGS="$BOOTSTRAP_FLAGS --quick-run"
  fi
}

# Configuration Helper Functions
show_db_options() {
  echo "Please choose a configuration option:"
  echo "  1) Quick run with H2 in-memory database (for testing)"
  echo "  2) PostgreSQL database (for production)"
  read -p "Enter your choice [1]: " first_choice
  
  first_choice=${first_choice:-1}
  
  if [[ "$first_choice" == "1" ]]; then
    QUICK_RUN=true
    BOOTSTRAP_FLAGS="$BOOTSTRAP_FLAGS --quick-run"
    log_info "Setting up with H2 in-memory database (quick run mode)."
    log_warn "NOTE: If you want to use a real database later, you will need to reconfigure your environment."
  else
    log_info "Setting up with PostgreSQL database."
  fi
}

detect_quick_run_mode() {
  if [[ -f "${BACKEND_DIR}/${ENV_FILE}" ]]; then
    if grep -q "QUICK RUN MODE ACTIVE" "${BACKEND_DIR}/${ENV_FILE}"; then
      QUICK_RUN=true
      log_info "Detected existing configuration: Quick run mode with H2 in-memory database"
    else
      QUICK_RUN=false
      log_info "Detected existing configuration: PostgreSQL database"
    fi
  fi
}

determine_configuration() {
  local missing_backend_env=false
  local missing_frontend_env=false

  if $RUN_BACKEND && [[ ! -f "${BACKEND_DIR}/${ENV_FILE}" ]]; then
    missing_backend_env=true
  fi

  if $RUN_FRONTEND && [[ ! -f "${FRONTEND_DIR}/${ENV_FILE}" ]]; then
    missing_frontend_env=true
  fi

  if $missing_backend_env || $missing_frontend_env; then
    WIZARD=true
    log_info "First-time run detected - starting configuration wizard..."
    show_db_options
  else
    if ! $WIZARD && ! $QUICK_RUN; then
      echo "Environment files exist. Please choose an option:"
      echo "  1) Use existing values (default)"
      echo "  2) Restart configuration wizard"
      read -p "Enter your choice [1]: " choice
      
      choice=${choice:-1}
      
      if [[ "$choice" == "2" ]]; then
        WIZARD=true
        RESTART=true
        if [[ -f "${BACKEND_DIR}/${ENV_FILE}" ]]; then
          rm "${BACKEND_DIR}/${ENV_FILE}"
          log_info "Removed existing ${BACKEND_DIR}/${ENV_FILE} file"
        fi
        if [[ -f "${FRONTEND_DIR}/${ENV_FILE}" ]]; then
          rm "${FRONTEND_DIR}/${ENV_FILE}"
          log_info "Removed existing ${FRONTEND_DIR}/${ENV_FILE} file"
        fi
        log_info "Restarting wizard from scratch..."
        BOOTSTRAP_FLAGS="$BOOTSTRAP_FLAGS --restart"
        
        show_db_options
      else
        log_info "Using existing environment values."
        detect_quick_run_mode
      fi
    fi
  fi

  if $WIZARD; then
    log_info "👉  Running setup wizard..."
    # Check if bootstrap script exists
    if [ ! -f "$BOOTSTRAP_SCRIPT" ]; then
      log_error "Bootstrap script not found at $BOOTSTRAP_SCRIPT"
      log_error "Make sure you're running this script from the project root directory or all project files are present."
      exit 1
    fi
    "${BOOTSTRAP_SCRIPT}" $BOOTSTRAP_FLAGS
  fi
}

# Environment Handling Functions
load_env_file() {
  local env_file="$1"
  local prefix="$2"
  local loaded_count=0
  
  if [ ! -f "$env_file" ]; then
    log_warn "$env_file not found, skipping..."
    return
  fi
  
  while IFS= read -r line || [ -n "$line" ]; do
    line=$(echo "$line" | tr -d $'\xEF\xBB\xBF' | tr -d '\r')
    
    if [[ -z "$line" || "$line" =~ ^[[:space:]]*# ]]; then
      continue
    fi
    
    if [[ "$line" =~ ^[[:space:]]*([A-Za-z0-9_]+)[[:space:]]*=[[:space:]]*(.*)[[:space:]]*$ ]]; then
      key="${BASH_REMATCH[1]}"
      value="${BASH_REMATCH[2]}"
      
      value=$(echo "$value" | xargs)
      
      if [ -n "$key" ]; then
        export "$key=$value"
        loaded_count=$((loaded_count + 1))
      fi
    fi
  done < "$env_file"
  
  log_info "${prefix}Loaded $loaded_count environment variables from $env_file"
}

load_environment_variables() {
  if command -v direnv &>/dev/null; then
    direnv allow . &>/dev/null || true
  else
    if $RUN_BACKEND; then
      log_info "Loading backend environment variables..."
      load_env_file "${BACKEND_DIR}/${ENV_FILE}" "[Backend] "
    fi
    
    if $RUN_FRONTEND; then
      log_info "Loading frontend environment variables..."
      load_env_file "${FRONTEND_DIR}/${ENV_FILE}" "[Frontend] "
    fi
  fi
}

# Service Management Functions
PIDS=()

# Build dependencies for backend and/or frontend
build_dependencies() {
  log_info "Building project dependencies..."
  
  if $RUN_BACKEND; then
    log_info "Building backend dependencies and compiling Kotlin code..."
    
    if [[ ! -f "${BACKEND_DIR}/${ENV_FILE}" ]]; then
      log_error "Backend ${ENV_FILE} file not found! Please run with --wizard flag first."
      exit 2
    fi
    
    (
      cd "${BACKEND_DIR}"
      load_env_in_subshell "${ENV_FILE}"
      
      # Determine correct Gradle wrapper script
      GRADLE_WRAPPER="./gradlew"
      if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin" ]]; then
        GRADLE_WRAPPER="./gradlew.bat"
      fi
      
      # Make gradlew executable if it isn't already (common issue on Unix systems)
      if [[ ! -x "./gradlew" && -f "./gradlew" ]]; then
        chmod +x ./gradlew
        log_info "Made gradlew executable"
      fi
      
      # Build the project to download dependencies and compile
      log_info "Running Gradle build to download dependencies and compile..."
      $GRADLE_WRAPPER --console=plain build -x test -x pitest --no-daemon
      
      if [[ $? -ne 0 ]]; then
        log_error "Backend build failed! Please check the error messages above."
        log_error "Common issues:"
        log_error "  - Ensure Java 21 is installed and JAVA_HOME is set correctly"
        log_error "  - Check network connectivity for downloading dependencies"
        log_error "  - Verify all environment variables are configured properly"
        exit 3
      fi
    )
    
    log_success "✓ Backend dependencies built successfully"
  fi
  
  if $RUN_FRONTEND; then
    log_info "Installing frontend dependencies..."
    
    if [[ ! -f "${FRONTEND_DIR}/${ENV_FILE}" ]]; then
      log_error "Frontend ${ENV_FILE} file not found! Please run with --wizard flag first."
      exit 2
    fi
    
    # Detect package manager
    PKG_MANAGER="npm"
    if [ -f "${FRONTEND_DIR}/yarn.lock" ]; then
      PKG_MANAGER="yarn"
    elif [ -f "${FRONTEND_DIR}/pnpm-lock.yaml" ]; then
      PKG_MANAGER="pnpm"
    fi
    
    log_info "Using ${PKG_MANAGER} as package manager..."
    
    (
      cd "${FRONTEND_DIR}"
      load_env_in_subshell "${ENV_FILE}"
      
      # Install dependencies
      log_info "Installing Node.js dependencies..."
      ${PKG_MANAGER} install
      
      if [[ $? -ne 0 ]]; then
        log_error "Frontend dependency installation failed! Please check the error messages above."
        log_error "Common issues:"
        log_error "  - Ensure Node.js 20+ is installed"
        log_error "  - Check network connectivity for downloading packages"
        log_error "  - Try clearing npm cache: npm cache clean --force"
        exit 3
      fi
    )
    
    log_success "✓ Frontend dependencies installed successfully"
  fi
  
  log_success "✓ All dependencies built successfully"
  echo ""
}

# Load environment variables in a subshell
load_env_in_subshell() {
  local env_file="$1"
  
  if [ -f "$env_file" ]; then
    while IFS= read -r line || [ -n "$line" ]; do
      [[ $line =~ ^#.*$ || -z $line ]] && continue
      
      if [[ $line =~ ^([A-Za-z0-9_]+)=(.*)$ ]]; then
        varname="${BASH_REMATCH[1]}"
        varvalue="${BASH_REMATCH[2]}"
        export "${varname}=${varvalue}"
      fi
    done < "$env_file"
  fi
}

start_backend() {
  log_info "Starting backend..."
  
  local profile="prod"
  if $QUICK_RUN; then
    log_info "Starting in dev mode with file-based H2 database for persistence..."
    profile="dev"
  else
    log_info "Starting with prod profile..."
  fi
  
  (
    cd "${BACKEND_DIR}"
    load_env_in_subshell "${ENV_FILE}"
    
    # Determine correct Gradle wrapper script
    GRADLE_WRAPPER="./gradlew"
    if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "win32" || "$OSTYPE" == "cygwin" ]]; then
      GRADLE_WRAPPER="./gradlew.bat"
    fi
    
    # Run the application (dependencies already built)
    $GRADLE_WRAPPER --console=plain bootRun --args="--spring.profiles.active=${profile} --logging.level.root=INFO" --no-daemon
  ) &
  
  PIDS+=($!)
  log_success "Backend starting (PID ${PIDS[-1]})"
}

start_frontend() {
  log_info "Starting frontend..."
  
  # Detect package manager (already done in build_dependencies, but keeping for safety)
  PKG_MANAGER="npm"
  if [ -f "${FRONTEND_DIR}/yarn.lock" ]; then
    PKG_MANAGER="yarn"
  elif [ -f "${FRONTEND_DIR}/pnpm-lock.yaml" ]; then
    PKG_MANAGER="pnpm"
  fi
  
  log_info "Using ${PKG_MANAGER} as package manager..."
  
  (
    cd "${FRONTEND_DIR}"
    load_env_in_subshell "${ENV_FILE}"
    
    if $QUICK_RUN && [ -z "${NEXT_PUBLIC_BACKEND_URL:-}" ]; then
      export NEXT_PUBLIC_BACKEND_URL="http://localhost:8080"
      log_info "Setting default NEXT_PUBLIC_BACKEND_URL to http://localhost:8080"
    fi
    
    # Build and start (dependencies already installed)
    log_info "Building and starting frontend..."
    ${PKG_MANAGER} run build && ${PKG_MANAGER} start
  ) &
  
  PIDS+=($!)
  log_success "Frontend starting in production mode (PID ${PIDS[-1]})"
}

setup_signal_handlers() {
  trap 'echo; log_info "⇢ Stopping services..."; kill ${PIDS[*]} 2>/dev/null || true; exit' INT TERM
}

start_services() {
  if $RUN_BACKEND; then
    start_backend
  fi

  if $RUN_FRONTEND; then
    start_frontend
  fi
}

# Main Function
main() {
  parse_flags "$@"
  check_prerequisites
  determine_configuration
  load_environment_variables
  build_dependencies
  setup_signal_handlers
  start_services
  
  # Wait for all background processes to complete (or be terminated)
  wait
}

# Execute the main function with all arguments
main "$@" 