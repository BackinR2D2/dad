# DAD – end-to-end run

## Start
From the project root:

```bash
docker compose build
docker compose up
```

## Open
- UI + API (C01): http://localhost:8081
- Node API (C06): http://localhost:3000
  - SNMP latest: http://localhost:3000/snmp/latest
  - Download image: http://localhost:3000/images/{id}

## API quick test
```bash
curl -F "file=@your.bmp" -F "zoomIn=true" -F "percent=20" http://localhost:8081/api/jobs
```

## Diagram
Open `architecture.drawio` in diagrams.net.
