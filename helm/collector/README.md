# my-app Helm Chart

Generic Helm chart for a single-container Java application, replacing docker-compose.

## Usage

### Install
```bash
helm install my-app ./my-app \
  --set image.repository=your-registry/your-image \
  --set image.tag=1.0.0 \
  --set-file envFile=.env
```

### Upgrade
```bash
helm upgrade my-app ./my-app \
  --set image.repository=your-registry/your-image \
  --set image.tag=1.1.0 \
  --set-file envFile=.env
```

### Uninstall
```bash
helm uninstall my-app
```

## Reusing for multiple apps

Since all your apps share this chart, use it as a local chart with per-app values:

```
apps/
├── my-app/          ← this chart (used as shared chart)
├── app1.values.yaml
├── app1.env
├── app2.values.yaml
└── app2.env
```

**app1.values.yaml:**
```yaml
image:
  repository: your-registry/app1
  tag: 2.3.0
service:
  port: 9090
```

**Deploy app1:**
```bash
helm install app1 ./my-app \
  -f app1.values.yaml \
  --set-file envFile=app1.env
```

## .env file format

Standard format is supported:
```
# comments are ignored
DB_HOST=localhost
DB_PASSWORD=p@$$w0rd!
SOME_URL=https://example.com
```

**Note:** Multiline values are NOT supported. Each var must be on a single line.

## Values

| Key | Default | Description |
|-----|---------|-------------|
| `image.repository` | `your-docker-image` | Docker image |
| `image.tag` | `latest` | Image tag |
| `image.pullPolicy` | `IfNotPresent` | Pull policy |
| `service.enabled` | `true` | Create a Service |
| `service.port` | `8080` | Container and service port |
| `service.type` | `ClusterIP` | Service type |
| `replicaCount` | `1` | Number of replicas |
| `resources` | `{}` | CPU/memory limits and requests |
| `envFile` | `""` | Raw .env file content (use --set-file) |
