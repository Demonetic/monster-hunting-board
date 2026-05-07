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
- Ni ska ha README för varje individuell del av projektet, dvs för frontend och backend.
- Ni ska även ha en övergripande README i roten på projektet som beskriver hur man kan köra igång applikationen.

---

## Del 3

**Deadline:** 2026-05-13 8:00

Ni ska nu distribuera applikationen till en molntjänst, till exempel Hetzner men det funkar lika bra med Render, Railway, Varsel m.fl. samt lägga till en ny feature.

**Kriterier för G**

- Er kod, docker-compose och alla dockerfiles samt pipelines måste gå att köra utan fel.
- Ni ska distribuera applikationen till en molntjänst, till exempel Hetzner eller annan leverantör.
- Ni ska ha en övergripande README i roten på projektet som beskriver hur man kan köra igång applikationen.

**Kriterier för VG**

Ni ska lägga till en future i backend och frontend. Koppla er till Open Weather API SMHI API, eller andra liknande tjänster (det ska framgå vilken tjänst ni använder på er hemsida) för att hämta väderdata. Den kopplingen sker på backend. Det ni ska kunna göra är att hämta vädret för en stad, till exempel Göteborg, som standard. Man ska även kunna välja vilken stad man vill använda, till exempel Stockholm. Detta matar ni in i ett inputfält så att man kan välja vilken ort man vill. Sedan ska frontend visa **vädret** för staden just nu. Om ni vill, ska ni även lägga till kommande tre dagar.

- Backend sköter all hämtning av data från det externa API:et.
- Frontenden har hand om visualisering och att man kan välja vilken stad man ska hämta data ifrån.
- Det är okej att vädret visas på en annan, helt separat sida så att det inte sabbar er befintliga design. Det är dock trevligt om ni kan se till att det smälter ihop bra med den designen som ni har valt.

## Muntlig redovisning

- Demo enligt tydligare instruktioner, och ni behöver visa att ni även har lagt till, eh, vädertjänsten om det är som så att ni har gått VG-vägen.
- Ni ska demonstrera att tjänsten är i drift online.
- Alla i mötet ska kunna accessa er webbsida och titta på den.
- Om eran sida inte är i drift online, så blir det underkänt på uppgiften.
- Förklara vilka tjänster ni har använt och hur ni har kopplat ihop det.
- Avsluta med en kort reflektion på hur det har gått samt hur det fungerar med vibe-kodning och de erfarenheterna därifrån.

---

Lycka till!
