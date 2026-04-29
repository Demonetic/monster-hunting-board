# Projekt - Individuell uppgift 1: DevOps

**Typ:** Individuell uppgift
**Publiceras:** [2026-04-23]
**Deadline:** Löpande
**Betyg:** Icke Godkänt (IG) / Godkänt (G) / Väl Godkänt (VG)

---

## Syfte

Målet med uppgiften är att du ska få praktisk erfarenhet av att bygga och drifta en webbapplikation med hjälp av DevOps-principer och verktyg. Du kommer att arbeta med en Spring Boot-applikation och du ska implementera en CI/CD-pipeline med GitHub Actions för att automatisera byggnaden, testningen och distributionen av applikationen till en molntjänst.

---

## Del 1: Skapa tech stacken

**Deadline:** 2026-04-30 8:00

Ni ska skapa en tech stack som består av följande komponenter:

- En Spring Boot-applikation (Backend)
  - Med säkerhet (exempelvis JWT)
  - Vettiga tester
- Frontend:
  - En React-applikation
  - eller en Vanilla HTML+CSS+JavaScript applikation
- En databas (MySQL) i en docker container
  - `docker run --name datalagring-mysql -p 3306:3306 -e MYSQL_ROOT_PASSWORD=password -d mysql:latest`

### Kriterier

- Ni får vibecoda frontend.
- Valfritt om man vi koda själv eller vibecoda backend ni lär er dock mer genom att koda själv.
- Ni måste ha en databas.
- Valfritt tema på applikationen.
- Korrekt användning av .env variabler för känsliga uppgifter.

Rekommendation är att hålla det enkelt och fokusera på att få en fungerande fullstack-applikation (Frontend + Backend + Databas).

---

## Del 2

**Deadline:** 2026-05-06 8:00

Ni ska nu implementera en CI/CD-pipeline med GitHub Actions för att automatisera testningen samt dockeriseringen av applikationen.

**Kriterier**

- Skapa en CI/CD-pipeline som testar projektet, ni behöver inte bygga jar filer mm.
- Skapa de Dockerfiles som behövs för att dockerisera applikationen.
- Skapa en docker-compose fil som kan starta applikationen med databasen.
- Verifiera att ni kan köra igång applikationen med docker-compose samt om ni har möjlighet att köra igång applikationen på en annan dator eller på Hetzner.
- Ni ska ha README för varje individuell del av projektet.
- Ni ska även ha en övergripande README i roten på projektet som beskriver hur man kan köra igång applikationen.

---

Lycka till!
