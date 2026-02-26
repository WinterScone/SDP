# Docker Setup Guide

How to build and run the application using Docker.

## Prerequisites

1. Install Docker Desktop:
   - Mac: https://docs.docker.com/desktop/install/mac-install/
   - Windows: https://docs.docker.com/desktop/install/windows-install/
   - Linux: https://docs.docker.com/desktop/install/linux-install/

2. Start Docker Desktop and wait until it's fully running

3. Verify Docker is working:
   ```bash
   docker --version
   ```

## Building the Docker Image

1. Open terminal and navigate to the project root directory:
   ```bash
   cd /path/to/SDP
   ```

2. Build the Docker image:
   ```bash
   docker build -t sdp-09-feb .
   ```

3. Verify the image was created:
   ```bash
   docker images | grep sdp
   ```

## Running the Application

### Option 1: Using Docker Run (Simple)

```bash
docker run -p 8080:8080 sdp-09-feb
```

### Option 2: Using Docker Run with Persistent Database

```bash
docker run -d -p 8080:8080 -v sdp-data:/app/Client/client/data --name sdp-app sdp-09-feb
```

- `-d` runs in background
- `-p 8080:8080` maps port 8080
- `-v sdp-data:/app/Client/client/data` persists database
- `--name sdp-app` names the container

### Option 3: Using Docker Compose (Recommended)

```bash
docker-compose up
```

To run in background:
```bash
docker-compose up -d
```

## Accessing the Application

Once running, open your browser:

- Patient Portal: http://localhost:8080/patient-login.html
- Admin Portal: http://localhost:8080/admin-login.html
- Home Page: http://localhost:8080

## Stopping the Application

### If using docker run:
```bash
docker stop sdp-app
```

### If using docker-compose:
```bash
docker-compose down
```

## Useful Commands

### View running containers:
```bash
docker ps
```

### View logs:
```bash
docker logs sdp-app
```

or with docker-compose:
```bash
docker-compose logs -f
```

### Restart the application:
```bash
docker restart sdp-app
```

or with docker-compose:
```bash
docker-compose restart
```

### Remove the container:
```bash
docker rm sdp-app
```

### Remove the image:
```bash
docker rmi sdp-09-feb
```

## Troubleshooting

### Port 8080 already in use
If you get a port conflict error:
```bash
# Find what's using port 8080
lsof -i :8080

# Use a different port
docker run -p 9090:8080 sdp-09-feb
# Then access at http://localhost:9090
```

### Docker daemon not running
- Make sure Docker Desktop is open and running
- Check the Docker icon in your system tray/menu bar

### Build fails
- Make sure you're in the project root directory (where Dockerfile is located)
- Check that Docker has enough memory allocated (Settings > Resources)

### Database not persisting
- Make sure you're using the `-v` volume flag or docker-compose
- Check volumes: `docker volume ls`

## Rebuilding After Code Changes

If you make changes to the code:

```bash
# Stop and remove old container
docker stop sdp-app
docker rm sdp-app

# Rebuild the image
docker build -t sdp-09-feb .

# Run the new container
docker run -d -p 8080:8080 -v sdp-data:/app/Client/client/data --name sdp-app sdp-09-feb
```

Or with docker-compose:
```bash
docker-compose down
docker-compose build
docker-compose up
```
