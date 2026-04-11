# BE Local Infra (MySQL + Redis)

## 1) Environment file
`BE` 폴더에서 `.env`를 만들고 값을 채워주세요.

```bash
cp .env .env
```

## 2) Run MySQL + Redis with Docker

```bash
docker compose up -d
```

확인:

```bash
docker compose ps
```

## 3) Run Spring Boot app

```bash
cd eye-on
./gradlew bootRun
```

앱은 기본값으로 아래를 사용합니다.
- MySQL: `localhost:3306`, DB `eye_on`
- Redis: `localhost:6379`

## 4) Stop infra

```bash
cd ..
docker compose down
```
