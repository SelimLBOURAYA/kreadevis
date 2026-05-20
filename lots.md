# Lots de migration — kreadevis-backend

## Contexte

Migration de `kreadevis` (Spring Boot 3.3.4 / MVC / JSP) vers `kreadevis-backend` (Spring Boot 4 / REST API pure / Angular frontend séparé).

- Ancien projet : `/home/selim/ENV/projets/kreadevis/`
- Nouveau projet : `/home/selim/ENV/projets/kreadevis-backend/`
- Stack cible : Spring Boot 4.0.6, Java 25, PostgreSQL 17 (Docker), MapStruct, OpenPDF, JWT

## Règle transversale — tests par lot

**Chaque lot inclut ses tests unitaires.** Les tests ne sont pas regroupés à la fin du projet : ils sont écrits en même temps que le code du lot, dans la même PR. `./mvnw verify` doit passer vert avant tout commit et avant l'ouverture de la PR. Voir `CLAUDE.md` (section *Workflow par lot*) pour la règle complète.

Le **lot 9** initialement prévu comme "tests à écrire" est redéfini en **infrastructure de tests** : remplacement des starters fantômes du pom, ajout H2/Testcontainers, tests d'intégration `@WebMvcTest` / `@DataJpaTest`, consolidation de couverture sur les zones déjà couvertes localement.

---

## État des lots

| Lot | Branche                    | Statut      |
|-----|----------------------------|-------------|
| 1   | chore/lot-1-setup           | ✅ terminé  |
| 2   | feat/lot-2-entities         | ✅ terminé  |
| 3   | feat/lot-3-security         | ✅ terminé  |
| 4   | feat/lot-4-dto-mappers      | ✅ terminé  |
| 5   | feat/lot-5-crud             | ✅ terminé  |
| 6   | feat/lot-6-quote            | ✅ terminé  |
| 7   | feat/lot-7-pdf              | ⬜ à faire  |
| 8   | feat/lot-8-csv              | ⬜ à faire  |
| 9   | feat/lot-9-tests            | ⬜ à faire  |
| 10  | feat/lot-10-email-reminders | ⬜ à faire  |

---

## LOT 1 — Setup ✅

**Commit :** `ee81a5a chore(1): setup project structure and dependencies`

Réalisé :
- Dépendances : Security, Validation, MapStruct, OpenPDF, Commons CSV
- Packages : `entity`, `dto`, `service`, `config`, `exception`
- `application.yaml` avec profils `dev` / `prod`
- `SecurityConfig` temporaire `permitAll`
- `GlobalExceptionHandler` avec `ErrorResponse` record

---

## LOT 2 — Entités & Repositories ✅

**Commits :**
- `5ce7fb7 feat(2): migrate entities and repositories from legacy project`
- `d0a5ccb refactor(2): replace QuoteCounter entity with dailySequence field in Quote`

Réalisé :
- Entités traduites FR → EN : `Devis→Quote`, `Reservation→QuoteItem`, `CompteurDevis→QuoteCounter→supprimé`, `Produit→Product`, `Professionnel→Professional`, `Adresse→Address`
- Repositories Spring Data JPA pour chaque entité
- `QuoteService` + `QuoteServiceImpl` : génération `DDMMYY-NNN` avec reset quotidien
- `dailySequence` intégré dans `Quote` (plus d'entité séparée)

Problèmes connus (à corriger dans les lots suivants) :
- ✅ `@Data` sur `Client` et `Address` → corrigé en LOT 4 (`@Getter @Setter @EqualsAndHashCode(of = "id")`)
- ✅ `ERole` n'a que `ROLE_ADMIN` → corrigé en LOT 3 (`ROLE_ADMIN`, `ROLE_USER`)
- ✅ `UserController` appelle directement le repository → corrigé en LOT 5 (passe par `UserService`)
- ✅ `QuoteServiceImpl.generateReferenceCode` thread-safety → corrigé en LOT 6 (`@Lock(PESSIMISTIC_WRITE)` sur `findMaxDailySequenceByDate`)
- `pom.xml` : `spring-boot-starter-data-jpa-test` et `spring-boot-starter-webmvc-test` n'existent pas en SB4 → remplacer par `spring-boot-starter-test` en LOT 9

---

## LOT 3 — Spring Security + JWT ✅

**Commits :**
- `93efdea feat(3): implement JWT authentication`
- `6d0a390 refactor(3): extract AuthService, add @Builder to all entities`

Réalisé :
- Dépendances JJWT 0.12.6 (`jjwt-api`, `jjwt-impl`, `jjwt-jackson`)
- `SecurityConfig` réécrit : stateless, filtre JWT, `/api/auth/**` en `permitAll`, le reste `authenticated`
- `JwtProperties` (`@ConfigurationProperties("app.jwt")`)
- `JwtUtils`, `JwtAuthFilter`, `UserDetailsServiceImpl`, `UserDetailsImpl`
- DTOs auth : `LoginRequest`, `RegisterRequest`, `AuthResponse`
- `AuthController` : `POST /api/auth/login`, `POST /api/auth/register`
- `AuthServiceImpl` extrait du controller (refactor)
- `ERole` complété : `ROLE_ADMIN`, `ROLE_USER`
- `@Builder` ajouté sur toutes les entités

---

## LOT 4 — DTOs + MapStruct ✅

**Commit :** `f41a02f feat(4): add DTOs and MapStruct mappers`

Réalisé :
- DTOs créés par domaine : `address/`, `client/`, `product/`, `professional/`, `user/`, `auth/`
- Mappers MapStruct : `AddressMapper`, `ClientMapper`, `ProductMapper`, `ProfessionalMapper`, `UserMapper`, `QuoteMapper`
- `@Data` retiré sur `Client` et `Address` → remplacé par `@Getter @Setter @EqualsAndHashCode(of = "id")`
- `UserResponse` ne contient jamais `password` (`@JsonIgnore` côté entité + mapper)

---

## LOT 5 — Services + Controllers CRUD ✅

**Commit :** `3accf5f feat(5): implement CRUD services and controllers`

Réalisé :
- Services (interface + impl) : `ClientService`, `ProductService`, `ProfessionalService`, `AddressService`, `UserService`
- Controllers : `ClientController`, `ProductController`, `ProfessionalController`, `AddressController`, `UserController` (tous CRUD complet)
- `UserController` ne touche plus directement le repository → passe par `UserService`
- Tous les endpoints utilisent les DTOs (`*Request` / `*Response`), pas d'entité en body
- `@Valid` appliqué sur les `@RequestBody`

---

## LOT 6 — Business Logic Quote ✅

**Commits :**
- `096da4a feat(6): implement quote business logic, soft delete, QuoteStatus enum`
- `23710a5 feat(6): add date-based quote search (startDate, endDate)`
- `7b32f04 refactor(6): replace deleted+@SQLRestriction with active flag, use per-user sequence`
- `afad27b refactor(6): use @MappingTarget for updates, add unit tests, fix test deps`
- `ce1d236 test(6): replace spy() with behavioral assertions on repository`
- `fa17a5d refactor(6): extract assignReferenceCode and computeTotalPrice from finalize()`

Réalisé :
- DTOs quote : `QuoteRequest`, `QuoteResponse`, `QuoteItemRequest`, `QuoteItemResponse`
- Services : `QuoteService` / `QuoteServiceImpl`, `QuoteItemService` / `QuoteItemServiceImpl`
- Enum `QuoteStatus` (remplace l'ancien `finished` booléen) — valeurs : `DRAFT`, `PENDING`, `FINALIZED`, `CANCELLED`
- Soft delete via flag `active` sur `Quote` et `QuoteItem` (remplace `deleted` + `@SQLRestriction`)
- Séquence journalière par utilisateur : `findMaxDailySequenceByUserAndDate` avec `@Lock(PESSIMISTIC_WRITE)`
- Endpoints `QuoteController` : CRUD complet, items, finalisation, recherche par référence et par plage de dates
- `@MappingTarget` sur les mappers d'update (évite de recréer des objets)
- Tests unitaires : `QuoteServiceImplTest` (17 tests)

---

## LOT 7 — Génération PDF ⬜

**Branche :** `feat/lot-7-pdf`
**Commit cible :** `feat(7): implement PDF generation with OpenPDF`

### Objectif
Générer devis et factures en PDF via OpenPDF. Aucun chemin hardcodé (config externalisée déjà en place dans `application.yaml`).

### Fichiers à créer
```
config/
  AppProperties.java           ← @ConfigurationProperties("app") — company, document
service/
  PdfService.java              ← interface : generateQuotePdf(Long quoteId), generateInvoicePdf(Long quoteId)
  impl/PdfServiceImpl.java     ← implémentation OpenPDF
```

### Endpoints à ajouter dans `QuoteController`
```
GET /api/quotes/{id}/pdf        ← télécharger le devis en PDF
GET /api/quotes/{id}/invoice/pdf ← télécharger la facture en PDF
```

### Config utilisée (déjà dans yaml)
```yaml
app:
  company:
    name, address, phone, email, siren
  document:
    logo-path, output-dir, facture-dir
```

### Critères de validation
- GET `/api/quotes/{id}/pdf` retourne un `application/pdf`
- Le PDF contient le nom du client, les items, le total
- Le logo est chargé depuis `app.document.logo-path`
- Aucun chemin absolu dans le code Java

---

## LOT 8 — Import CSV ⬜

**Branche :** `feat/lot-8-csv`
**Commit cible :** `feat(8): implement CSV product import`

### Objectif
Permettre l'import de produits en masse via un fichier CSV (migration du `CsvHelper` / `CsvService` legacy).

### Format CSV attendu
```
label,description,stockQuantity,unitPrice,vatRate,referenceCode
```

### Fichiers à créer
```
service/
  CsvImportService.java
  impl/CsvImportServiceImpl.java   ← Apache Commons CSV
controller/
  ProductController.java           ← ajouter POST /api/products/import (multipart)
```

### Critères de validation
- POST `/api/products/import` avec un fichier CSV → produits créés en base
- Lignes invalides (champs manquants) → retournées dans la réponse avec leurs erreurs
- `referenceCode` dupliqué → skippé avec message d'avertissement

---

## LOT 9 — Infrastructure de tests & intégration ⬜

**Branche :** `feat/lot-9-tests`
**Commit cible :** `test(9): test infrastructure and integration tests`

> Les **tests unitaires de chaque lot** sont produits dans leur propre lot (règle transversale, cf. en-tête). Ce lot 9 est centré sur l'infrastructure transverse de tests et les tests d'intégration qui demandent un contexte complet (Spring Boot Test, Testcontainers, `@WebMvcTest`).

### Corrections pom.xml d'abord
Remplacer :
```xml
<artifactId>spring-boot-starter-data-jpa-test</artifactId>
<artifactId>spring-boot-starter-webmvc-test</artifactId>
```
Par :
```xml
<artifactId>spring-boot-starter-test</artifactId>
```
Et ajouter H2 pour les tests :
```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

### Tests à écrire
```
test/
  service/
    QuoteServiceImplTest.java        ← generateReferenceCode, thread-safety
    ClientServiceImplTest.java
    PdfServiceImplTest.java
    CsvImportServiceImplTest.java
  controller/
    AuthControllerTest.java          ← login/register (@WebMvcTest)
    ClientControllerTest.java
    QuoteControllerTest.java
  repository/
    QuoteRepositoryTest.java         ← findMaxDailySequenceByDate (@DataJpaTest)
```

### Critères de validation
- `./mvnw test` passe
- Couverture minimale : services métier (Quote, Pdf, Csv)
- Tests controller avec `MockMvc` + token JWT mocké

---

## LOT 10 — Relances hebdomadaires par email ⬜

**Branche :** `feat/lot-10-email-reminders`
**Commit cible :** `feat(10): implement weekly email reminders for pending quotes`

### Objectif
Envoyer chaque lundi matin un mail récapitulatif au créateur de chaque devis listant ses devis `PENDING` depuis plus de N jours (seuil configurable), avec les coordonnées clients pour relance manuelle. Provider : **Mailjet via REST API** (pas de SMTP) en utilisant le `RestClient` Spring.

### Prérequis
- Lot 3 (Security) terminé → `SecurityContextHolder` exploitable pour récupérer l'utilisateur courant
- Lot 6 (Quote) terminé → `QuoteStatus.PENDING` existe

### Modification entité Quote
Ajouter le créateur (FK vers `User`) — peuplé automatiquement à la création via `SecurityContextHolder`.
```java
@ManyToOne
@JoinColumn(name = "created_by")
private User createdBy;
```
Patcher `QuoteServiceImpl.create()` pour renseigner ce champ.

### Dépendances à ajouter dans `pom.xml`
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```
Pas de `spring-boot-starter-mail` (on passe par HTTP/RestClient, pas SMTP).

### Config `application.yaml`
```yaml
app:
  email:
    enabled: true
    mailjet:
      api-url: https://api.mailjet.com/v3.1/send
      api-key: ${MAILJET_API_KEY}
      api-secret: ${MAILJET_API_SECRET}
    sender:
      email: noreply@kreadevis.fr
      name: Kreadevis
    reminder:
      enabled: true
      pending-threshold-days: 10
      cron: "0 0 8 * * MON"
```

### Fichiers à créer
```
config/
  EmailProperties.java              ← @ConfigurationProperties("app.email")
  MailjetClientConfig.java          ← Bean RestClient (baseUrl + Basic Auth)
  SchedulingConfig.java             ← @EnableScheduling
dto/email/
  EmailMessage.java                 ← record (to, toName, subject, htmlBody)
  MailjetPayload.java               ← payload Mailjet v3.1
service/
  EmailService.java                 ← send(EmailMessage)
  impl/MailjetEmailServiceImpl.java ← RestClient → POST /v3.1/send
  ReminderService.java              ← buildReminders() : Map<User, List<Quote>>
  impl/ReminderServiceImpl.java
scheduler/
  WeeklyReminderScheduler.java      ← @Scheduled(cron = "${app.email.reminder.cron}")
resources/templates/email/
  reminder.html                     ← template Thymeleaf
```

### Repository
```java
@Query("SELECT q FROM Quote q WHERE q.status = 'PENDING' AND q.date < :cutoff")
List<Quote> findPendingOlderThan(LocalDate cutoff);
```
Le groupement par `createdBy` se fait dans `ReminderService` (Java stream).

### Sécurité
- API key + secret Mailjet **uniquement** via variables d'environnement (`MAILJET_API_KEY`, `MAILJET_API_SECRET`)
- `.env` ajouté à `.gitignore`
- HTTPS forcé (URL Mailjet en `https://`)
- Autoescaping Thymeleaf actif → protection XSS native sur les données client/devis
- Deux toggles indépendants : `app.email.enabled` (kill switch global) et `app.email.reminder.enabled` (désactive uniquement le scheduler)
- Aucun log de l'API key (logger uniquement le code HTTP de retour Mailjet)

### Endpoint manuel (debug / déclenchement à la demande)
```
POST /api/admin/reminders/trigger  ← ROLE_ADMIN — force l'exécution immédiate
```

### Critères de validation
- `./mvnw compile` passe
- `Quote.createdBy` peuplé automatiquement à chaque création
- Un appel manuel au scheduler envoie effectivement un mail (compte test Mailjet)
- Aucun mail envoyé si `app.email.enabled=false`
- Le mail contient nom client, téléphone, email, référence devis, date, montant
- Aucune clé Mailjet présente dans le repo (`git grep` à blanc)
