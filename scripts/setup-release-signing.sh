#!/usr/bin/env bash
# One-time setup: creates the release signing key OUTSIDE the repo and stores it as GitHub secrets.
# Keep a backup of ~/.config/ciclo-signing: if the key is lost, new APKs can no longer update
# the installed app (it would have to be uninstalled, losing its data).
set -euo pipefail

dir="$HOME/.config/ciclo-signing"
keystore="$dir/ciclo-release.p12"
mkdir -p "$dir"
chmod 700 "$dir"

if [ ! -f "$keystore" ]; then
  openssl rand -base64 24 | tr -d '/+=\n' > "$dir/password.txt"
  openssl req -x509 -newkey rsa:4096 -sha256 -days 10000 -nodes -subj "/CN=Ciclo" \
    -keyout "$dir/key.pem" -out "$dir/cert.pem" 2>/dev/null
  openssl pkcs12 -export -inkey "$dir/key.pem" -in "$dir/cert.pem" -name ciclo \
    -out "$keystore" -passout "file:$dir/password.txt" \
    -keypbe AES-256-CBC -certpbe AES-256-CBC -macalg sha256
  rm "$dir/key.pem"
  chmod 600 "$dir"/*
  echo "Created $keystore"
else
  echo "Reusing existing $keystore"
fi

base64 -w0 "$keystore" | gh secret set CICLO_KEYSTORE_BASE64
gh secret set CICLO_KEYSTORE_PASSWORD < "$dir/password.txt"
printf ciclo | gh secret set CICLO_KEY_ALIAS
gh secret list
