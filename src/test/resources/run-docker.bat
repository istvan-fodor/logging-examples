docker run -e POSTGRES_PASSWORD=password -e POSTGRES_USER=root -p 5432:5432 -d --rm --name logging-test-postgres postgres:9-alpine