if [ ! "$(docker ps -a -q -f name=postgresdb)" ]; then
    docker run --name postgresdb \
      -e POSTGRES_USER=admin \
      -e POSTGRES_PASSWORD=secret \
      -e POSTGRES_DB=gamerent_db \
      -p 5432:5432 \
      -d postgres:latest
else
    docker start postgresdb 2>/dev/null || true
fi

echo "Waiting for PostgreSQL to start..."
until docker exec postgresdb pg_isready -U admin > /dev/null 2>&1; do
  sleep 1
done
echo "PostgreSQL is ready!"

./mvnw spring-boot:run

docker stop postgresdb 2>/dev/null || true
