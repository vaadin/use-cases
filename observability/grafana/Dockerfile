# Grafana for the hosted UC7 stack. Built from the repository root by the
# Fly Deploy workflow (context is the repo, not this directory), like the
# module's own Dockerfile.
#
# The provisioning files and the dashboard are the same ones compose.yaml
# mounts locally; only the datasource URL differs, and that is an environment
# variable (PROMETHEUS_URL) set in fly.toml and compose.yaml respectively.
FROM grafana/grafana:latest

COPY observability/grafana/provisioning /etc/grafana/provisioning
COPY observability/grafana/dashboards /var/lib/grafana/dashboards
