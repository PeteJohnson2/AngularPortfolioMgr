docker pull postgres:13
docker run --name local-postgres-pfm -e POSTGRES_PASSWORD=sven1 -e POSTGRES_USER=sven1 -e POSTGRES_DB=portfoliomgr -p 5432:5432 -d postgres

# docker start local-postgres-pfm
# docker stop local-postgres-pfm
# docker exec -it local-postgres-pfm bash