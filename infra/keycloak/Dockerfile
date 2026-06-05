FROM quay.io/keycloak/keycloak:26.0

# Production mode with edge proxy (TLS handled by Traefik)
ENV KC_PROXY=edge
ENV KC_HEALTH_ENABLED=true

# Database config via env vars (injected by Coolify)
# KC_DB, KC_DB_URL, KC_DB_USERNAME, KC_DB_PASSWORD

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]