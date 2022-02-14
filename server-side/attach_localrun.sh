set -e
docker exec -it wiki_backend_local sh -c "cd /app/repo-store/repo;exec sh"
