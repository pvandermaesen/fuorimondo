#!/usr/bin/env bash
# =============================================================================
# deploy.sh — FuoriMondo
# Déploiement complet sur Ubuntu (machine : prod8)
#
# Architecture :
#   prod8 (ce serveur) → Spring Boot :6902 (frontend Vue.js + API embarqués)
#   Caddy (autre machine) → reverse proxy https://www.fuorimondo.com → prod8:6902
#
# Prérequis sur prod8 :
#   java    → JDK natif Ubuntu (sudo apt install openjdk-17-jdk)
#   node    → via nvm (nvm install --lts)
#   Maven   → fourni par le Maven wrapper du projet (mvnw), pas d'install requise
#
# Usage :
#   chmod +x deploy.sh
#   ./deploy.sh
# =============================================================================

set -euo pipefail

# Bash lit les scripts de façon incrémentale : si git reset --hard écrase ce fichier
# pendant l'exécution, les fonctions suivantes sont lues depuis le fichier modifié.
# Fix : se copier en /tmp et se ré-exécuter depuis là avant toute opération git.
if [[ "$0" != /tmp/deploy-fuorimondo.sh ]]; then
  cp "$0" /tmp/deploy-fuorimondo.sh
  chmod +x /tmp/deploy-fuorimondo.sh
  exec /tmp/deploy-fuorimondo.sh "$@"
fi

# Charger les variables d'environnement depuis le fichier .env (si présent)
# Cela permet de lire GIT_TOKEN sans avoir à le passer en ligne de commande.
ENV_FILE="$HOME/.config/fuorimondo.env"
if [[ -f "$ENV_FILE" ]]; then
  # shellcheck disable=SC1090
  set -a; source "$ENV_FILE"; set +a
fi

# =============================================================================
# CONFIGURATION — à adapter
# =============================================================================
GIT_REPO="https://github.com/pvandermaesen/fuorimondo.git"
GIT_BRANCH="main"
# GIT_TOKEN : Personal Access Token GitHub (lu depuis fuorimondo.env)
# Portée minimale requise : "Contents" (read) pour les dépôts privés.
# Laisser vide pour les dépôts publics ou si l'auth SSH est configurée.
GIT_TOKEN="${GIT_TOKEN:-}"
DEPLOY_DIR="$HOME/fuorimondo"
SPRING_PROFILE="prod"
BACK_PORT=6902
CADDY_HOST="www.fuorimondo.com"     # domaine servi par Caddy (info affichage)

# Chemins outils
JAVA_BIN="$(command -v java 2>/dev/null || echo '')"
MVN_BIN="$DEPLOY_DIR/backend/mvnw"   # Maven wrapper inclus dans le projet

# =============================================================================
# COULEURS
# =============================================================================
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

info()    { echo -e "${BLUE}[INFO]${NC}  $*"; }
success() { echo -e "${GREEN}[OK]${NC}    $*"; }
warn()    { echo -e "${YELLOW}[WARN]${NC}  $*"; }
error()   { echo -e "${RED}[ERREUR]${NC} $*" >&2; }
die()     { error "$*"; exit 1; }

# =============================================================================
# VÉRIFICATION DES PRÉREQUIS
# =============================================================================
check_prerequisites() {
  info "Vérification des prérequis..."
  local missing=0

  # Git repo configuré
  if [[ -z "$GIT_REPO" ]]; then
    error "La variable GIT_REPO est vide."
    echo "  → Éditez deploy.sh et renseignez GIT_REPO avec l'URL de votre dépôt Git."
    missing=1
  fi

  # Java
  if [[ -z "$JAVA_BIN" ]]; then
    error "Java introuvable dans le PATH."
    echo "  → Installez le JDK via apt :"
    echo "    sudo apt update && sudo apt install -y openjdk-17-jdk"
    missing=1
  else
    local java_version
    java_version=$("$JAVA_BIN" -version 2>&1 | head -1)
    success "Java trouvé : $java_version ($JAVA_BIN)"
  fi


  # nvm + Node.js
  if ! command -v node &>/dev/null; then
    error "Node.js introuvable."
    echo "  → Installez nvm puis Node.js LTS :"
    echo ""
    echo "    # 1. Installer nvm"
    echo "    curl -o- https://raw.githubusercontent.com/nvm-sh/nvm/v0.39.7/install.sh | bash"
    echo ""
    echo "    # 2. Recharger le shell (ou ouvrir un nouveau terminal)"
    echo "    source ~/.bashrc"
    echo ""
    echo "    # 3. Installer Node.js LTS  ← attention : c'est une option nvm, pas Maven"
    echo "    nvm install --lts"
    echo ""
    echo "    # 4. Vérifier"
    echo "    node --version && npm --version"
    missing=1
  else
    success "Node.js trouvé : $(node --version)"
  fi

  # npm
  if ! command -v npm &>/dev/null; then
    error "npm introuvable (devrait être inclus avec Node.js via nvm)."
    echo "  → Réinstallez Node.js : source ~/.bashrc && nvm install --lts"
    missing=1
  else
    success "npm trouvé : $(npm --version)"
  fi

  # systemctl (pour les services)
  if ! command -v systemctl &>/dev/null; then
    error "systemctl introuvable — ce script nécessite systemd."
    echo "  → Assurez-vous d'être sur Ubuntu 18.04+ avec systemd actif."
    missing=1
  fi

  if [[ $missing -ne 0 ]]; then
    die "Des prérequis sont manquants. Corrigez les erreurs ci-dessus puis relancez le script."
  fi

  success "Tous les prérequis sont satisfaits."
}

# =============================================================================
# RÉCUPÉRATION DU CODE
# =============================================================================
fetch_code() {
  info "Récupération du code source depuis $GIT_REPO (branche $GIT_BRANCH)..."

  # Construire l'URL authentifiée si GIT_TOKEN est défini
  local git_url="$GIT_REPO"
  if [[ -n "$GIT_TOKEN" ]]; then
    # Injecter le token dans l'URL : https://TOKEN@github.com/...
    git_url="${GIT_REPO/https:\/\//https:\/\/${GIT_TOKEN}@}"
  fi

  if [[ -d "$DEPLOY_DIR/.git" ]]; then
    info "Dépôt déjà cloné — mise à jour..."
    # Mettre à jour l'URL remote (token peut avoir changé)
    git -C "$DEPLOY_DIR" remote set-url origin "$git_url"
    git -C "$DEPLOY_DIR" fetch origin
    git -C "$DEPLOY_DIR" reset --hard "origin/$GIT_BRANCH"
  else
    git clone --branch "$GIT_BRANCH" "$git_url" "$DEPLOY_DIR"
  fi

  chmod +x "$MVN_BIN"
  success "Code source à jour dans $DEPLOY_DIR"
}

# =============================================================================
# BUILD FRONTEND
# =============================================================================
build_frontend() {
  info "Compilation du frontend (Vue.js)..."

  cd "$DEPLOY_DIR/frontend"

  npm install
  npm run build

  if [[ ! -d "dist" ]]; then
    die "Le répertoire dist/ est absent. Le build Vue.js a échoué."
  fi

  success "Frontend compilé dans $DEPLOY_DIR/frontend/dist/"
}

# =============================================================================
# COPIE DU FRONTEND DANS SPRING BOOT
# =============================================================================
copy_frontend_to_backend() {
  info "Copie du frontend dans Spring Boot (resources/static/)..."

  local static_dir="$DEPLOY_DIR/backend/src/main/resources/static"

  rm -rf "$static_dir"
  mkdir -p "$static_dir"
  cp -r "$DEPLOY_DIR/frontend/dist/." "$static_dir/"

  success "Frontend copié dans $static_dir"
}

# =============================================================================
# BUILD BACKEND
# =============================================================================
build_backend() {
  info "Compilation du backend (Spring Boot + frontend embarqué)..."

  cd "$DEPLOY_DIR/backend"

  export JAVA_HOME
  JAVA_HOME="$(dirname "$(dirname "$(readlink -f "$JAVA_BIN")")")"

  "$MVN_BIN" clean package -DskipTests -q

  local jar
  jar=$(ls target/*.jar 2>/dev/null | grep -v 'original' | head -1)

  if [[ -z "$jar" ]]; then
    die "Aucun JAR produit dans backend/target/. Vérifiez les logs Maven."
  fi

  success "Backend compilé : $jar (frontend inclus)"
}

# =============================================================================
# SERVICE SYSTEMD — BACKEND (user-level, pas besoin de sudo)
# Prérequis root unique : loginctl enable-linger <user>
# =============================================================================
install_backend_service() {
  info "Installation du service systemd utilisateur pour le backend..."

  local jar
  jar=$(ls "$DEPLOY_DIR/backend/target/"*.jar 2>/dev/null | grep -v 'original' | head -1)

  local service_dir="$HOME/.config/systemd/user"
  mkdir -p "$service_dir"

  cat > "$service_dir/fuorimondo.service" <<EOF
[Unit]
Description=FuoriMondo — Spring Boot
After=network.target

[Service]
WorkingDirectory=$DEPLOY_DIR/backend
EnvironmentFile=$HOME/.config/fuorimondo.env
ExecStart=$JAVA_BIN -jar $jar --spring.profiles.active=$SPRING_PROFILE --server.port=$BACK_PORT
SuccessExitStatus=143
Restart=on-failure
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=fuorimondo

[Install]
WantedBy=default.target
EOF

  # XDG_RUNTIME_DIR requis par systemctl --user en session non-interactive
  # Prérequis : loginctl enable-linger <user> (à faire une fois en root)
  export XDG_RUNTIME_DIR="/run/user/$(id -u)"
  export DBUS_SESSION_BUS_ADDRESS="unix:path=${XDG_RUNTIME_DIR}/bus"

  if [[ ! -d "$XDG_RUNTIME_DIR" ]]; then
    die "Le répertoire $XDG_RUNTIME_DIR n'existe pas. Activez le linger en root : loginctl enable-linger $(whoami)"
  fi

  systemctl --user daemon-reload
  systemctl --user enable fuorimondo
  systemctl --user restart fuorimondo

  success "Service backend démarré sur le port $BACK_PORT"
}

# =============================================================================
# STATUT FINAL
# =============================================================================
print_status() {
  echo ""
  echo -e "${GREEN}============================================${NC}"
  echo -e "${GREEN}  Déploiement terminé avec succès !${NC}"
  echo -e "${GREEN}============================================${NC}"
  echo ""
  local ip
  ip="$(hostname -I | awk '{print $1}')"
  echo -e "  Site public : https://$CADDY_HOST"
  echo -e "  Spring Boot : http://$ip:$BACK_PORT  (accès direct prod8)"
  echo -e "  API         : http://$ip:$BACK_PORT/api/"
  echo -e "  Swagger     : (désactivé en profil $SPRING_PROFILE)"
  echo ""
  echo -e "  Caddy (autre machine) doit pointer vers :"
  echo -e "    reverse_proxy prod8:$BACK_PORT"
  echo ""
  echo -e "  Logs backend :"
  echo -e "    journalctl --user -u fuorimondo -f"
  echo ""
  echo -e "  Redémarrer le backend :"
  echo -e "    systemctl --user restart fuorimondo"
  echo ""
}

# =============================================================================
# POINT D'ENTRÉE
# =============================================================================
main() {
  echo ""
  info "=== Déploiement FuoriMondo — prod8 ==="
  echo ""

  check_prerequisites
  fetch_code
  build_frontend
  copy_frontend_to_backend
  build_backend
  install_backend_service
  print_status
}

main "$@"
