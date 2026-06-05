FROM quay.io/keycloak/keycloak:26.0

# Production mode with edge proxy (TLS handled by Traefik)
ENV KC_PROXY=edge
ENV KC_HEALTH_ENABLED=true
ENV KC_HOSTNAME=keycloak.antigravitylabs.cloud
ENV KC_HOSTNAME_STRICT=false
ENV KC_HOSTNAME_STRICT_HTTPS=false
ENV KC_DB=postgres
ENV KC_DB_URL=jdbc:postgresql://std8ft2txgni1hswnvoetrt5:5432/appdb
ENV KC_DB_USERNAME=appuser
ENV KC_DB_PASSWORD=BSYawOw2EBCamG5q21II2InEMcFs5R6hxVlZ1iDqv6si3F5m6VESzvpfM6sdhJmh

# Build optimized Keycloak with DB config baked in
RUN /opt/keycloak/bin/kc.sh build

EXPOSE 8080

ENTRYPOINT ["/opt/keycloak/bin/kc.sh"]
CMD ["start", "--optimized"]