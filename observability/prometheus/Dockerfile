# Prometheus for the hosted UC7 stack. Built from the repository root by the
# Fly Deploy workflow (context is the repo, not this directory), like the
# module's own Dockerfile.
FROM prom/prometheus:latest

COPY observability/prometheus/fly.yaml /etc/prometheus/prometheus.yml

# The image's default listen address is 0.0.0.0, which is IPv4 only. Fly's
# private network is IPv6, and both Grafana and the app reach this machine as
# observability-cases-prometheus.internal, so listen on every family. Two days
# of retention on the machine's ephemeral disk: this is a demo, not a system of
# record, and a redeploy is allowed to start from empty.
CMD ["--config.file=/etc/prometheus/prometheus.yml", \
     "--storage.tsdb.path=/prometheus", \
     "--storage.tsdb.retention.time=2d", \
     "--web.listen-address=:9090"]
