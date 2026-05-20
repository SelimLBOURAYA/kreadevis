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
| 7   | feat/lot-7-pdf              | ✅ terminé  |
| 8   | feat/lot-8-csv              | ✅ terminé  |
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

## LOT 7 — Génération PDF ✅

**Commit :** `feat(7): implement PDF generation with OpenPDF`

Réalisé :
- `AppProperties` (`@ConfigurationProperties("app")`) avec nested records `Company` et `DocumentConfig`
- `PdfService` / `PdfServiceImpl` : génération OpenPDF pour devis (`DEVIS`) et facture (`FACTURE`)
- Logo chargé via `ResourceLoader` depuis `app.document.logo-path` (classpath ou filesystem) — absent → graceful skip
- Endpoints `GET /api/quotes/{id}/pdf` et `GET /api/quotes/{id}/invoice/pdf` → `application/pdf` + `Content-Disposition`
- Fix `GlobalExceptionHandler` : `EntityNotFoundException` → 404 (manquait depuis lot 6)
- 7 tests unitaires `PdfServiceImplTest`

---

## LOT 8 — Import CSV ✅

**Commit :** `feat(8): implement CSV product import`

Réalisé :
- `CsvImportService` / `CsvImportServiceImpl` : import via Apache Commons CSV 1.14.0
- Format CSV : `label,description,stockQuantity,unitPrice,vatRate,referenceCode`
- `CsvImportResult` DTO : `importedCount`, `imported`, `errors` (numéro de ligne + message), `warnings`
- Lignes invalides (label vide, champs non-numériques) → collectées dans `errors` sans interrompre l'import
- `referenceCode` existant → skippé avec message dans `warnings`
- `POST /api/products/import` (multipart) ajouté dans `ProductController`
- `existsByReferenceCodeAndActiveTrue` ajouté dans `ProductRepository`
- 9 tests unitaires `CsvImportServiceImplTest`

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

## LOT 11 — Envoi automatique du devis au client ⬜

**Branche :** `feat/lot-11-quote-auto-send`
**Commit cible :** `feat(11): implement automatic quote sending to client with delayed scheduler`

### Objectif

Envoyer automatiquement le devis (PDF en pièce jointe) par mail au client après finalisation, avec un délai de réflexion configurable (défaut 24h) pendant lequel le créateur peut annuler. Un batch tourne plusieurs fois par jour dans une plage horaire ouvrée et envoie les devis dont le délai est écoulé. En cas d'échec d'envoi, le créateur est notifié par mail.

### Prérequis

- **LOT 7 (PDF) terminé** → `PdfService.generateQuotePdf(quoteId)` disponible pour produire le byte[] à attacher
- **LOT 10 (relances email) terminé** → réutilisation de `EmailService`, `MailjetEmailServiceImpl`, `MailjetClientConfig`, `EmailProperties` (et `Quote.createdBy` peuplé via `SecurityContext`)

L'ordre forcé est donc : 7 → 10 → 11.

### Modèle de données

#### Enum `EmailSendStatus` (nouveau)
```java
public enum EmailSendStatus {
    NOT_APPLICABLE,   // pas d'email client renseigné à la finalisation
    PENDING,          // en attente du batch (ou en attente d'un retry)
    SENT,             // envoyé avec succès
    FAILED,           // échec après épuisement des retries
    CANCELLED         // créateur a annulé (ou repassage en DRAFT)
}
```

#### Modifications entité `Quote`
```java
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private EmailSendStatus emailStatus = EmailSendStatus.NOT_APPLICABLE;

private Instant scheduledSendAt;     // null si pas planifié
private Instant emailSentAt;          // null tant que pas envoyé
private Instant emailLastAttemptAt;   // dernier essai (succès ou échec)
private String emailFailureReason;    // message court de la dernière erreur

@Column(nullable = false)
private Integer emailRetryCount = 0;
```

Migration Flyway / script JPA : à ajouter en `V<n>__add_quote_email_send_fields.sql` selon la stratégie en place dans le projet.

### Modifications service existant

#### `QuoteServiceImpl.finalize(Long quoteId)` (modifié)
À l'appel de finalisation existante (LOT 6), enchaîner :
1. `status = FINALIZED` (déjà fait)
2. Si `client.email` non nul et non vide :
   - `emailStatus = PENDING`
   - `scheduledSendAt = now() + app.email.quote-send.review-delay-hours` (en heures)
3. Sinon :
   - `emailStatus = NOT_APPLICABLE`
   - `scheduledSendAt = null`

L'absence d'email **ne bloque pas** la finalisation : le devis est toujours téléchargeable depuis l'app.

### Dépendances

Aucune nouvelle dépendance Maven : tout le nécessaire est déjà introduit par LOT 7 (OpenPDF), LOT 10 (Thymeleaf, RestClient), et LOT 6 (JPA, Scheduling via `@EnableScheduling` activé en LOT 10).

### Config `application.yaml` (additions sous `app.email`)
```yaml
app:
  email:
    # … contenu existant LOT 10 (enabled, mailjet, sender, reminder) …
    quote-send:
      enabled: true
      review-delay-hours: 24
      cron: "0 0 9,12,15,18 * * MON-SAT"
      send-window:
        start: "09:00"
        end: "18:00"
      retry:
        max-attempts: 3
        backoff-minutes: 15
      template: quote-to-client
      failure-template: quote-send-failure
```

Le toggle `app.email.quote-send.enabled` est indépendant de `app.email.enabled` (kill switch global) et de `app.email.reminder.enabled` (LOT 10). Chacun désactive uniquement sa feature.

### Fichiers à créer

```
config/
  QuoteSendProperties.java                ← @ConfigurationProperties("app.email.quote-send")
service/
  QuoteEmailService.java                  ← interface
  impl/QuoteEmailServiceImpl.java         ← orchestration : sélection + envoi + retry + notif échec
scheduler/
  QuoteSendScheduler.java                 ← @Scheduled(cron = "${app.email.quote-send.cron}")
resources/templates/email/
  quote-to-client.html                    ← template Thymeleaf (mail vers client)
  quote-send-failure.html                 ← template Thymeleaf (mail vers créateur en cas d'échec)
```

### Repository

Ajouter dans `QuoteRepository` :
```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("""
    SELECT q FROM Quote q
    WHERE q.emailStatus = 'PENDING'
      AND q.scheduledSendAt <= :now
      AND q.status = 'FINALIZED'
""")
List<Quote> findReadyToSend(@Param("now") Instant now);
```

Le verrou pessimiste protège contre les doubles envois en cas d'exécutions concurrentes du scheduler (relance manuelle pendant un cron, par exemple).

### Logique `QuoteSendScheduler`

1. Lecture des propriétés `send-window` ; si `now()` est hors fenêtre → log debug et return.
2. Vérifier `app.email.enabled` ET `app.email.quote-send.enabled` ; sinon return.
3. Appeler `quoteEmailService.processPendingSends()`.

### Logique `QuoteEmailServiceImpl.processPendingSends()`

Dans une transaction :
1. `quotes = repository.findReadyToSend(now)`
2. Pour chaque devis :
   - Générer le PDF via `pdfService.generateQuotePdf(quote.id)`
   - Construire `EmailMessage` à partir du template `quote-to-client` (variables : `quote`, `client`, `company`)
   - Appeler `emailService.sendWithAttachment(emailMessage, pdfBytes, "devis-{ref}.pdf")` (à ajouter dans `EmailService` du LOT 10 si pas déjà présent)
   - Si succès : `emailStatus = SENT`, `emailSentAt = now()`, `emailLastAttemptAt = now()`, `scheduledSendAt = null`
   - Si échec :
     - `emailRetryCount++`, `emailLastAttemptAt = now()`, `emailFailureReason = e.getMessage()` (tronqué à 500 chars)
     - Si `emailRetryCount < max-attempts` : `scheduledSendAt = now() + backoff-minutes`, `emailStatus` reste `PENDING`
     - Sinon : `emailStatus = FAILED`, `scheduledSendAt = null`, puis tenter d'envoyer la notif d'échec au créateur (template `quote-send-failure`) — **1 seul essai**, échec silencieux (logué uniquement)

### Templates Thymeleaf

#### `quote-to-client.html`
Variables : `quote.referenceCode`, `quote.date`, `quote.totalAmount`, `client.name`, `company.name`, `company.email`, `company.phone`.
Contenu minimal : salutation, mention du devis joint en PDF, validité éventuelle, signature avec coordonnées de l'entreprise.

#### `quote-send-failure.html`
Variables : `quote.referenceCode`, `client.name`, `client.email`, `client.phoneNumber` (si dispo), `failureReason`, `retryCount`, `quoteUrl` (lien direct vers le devis dans l'app, base à mettre en config).
Contenu : alerte d'échec, raison technique, coordonnées du client pour relance manuelle, lien vers le devis.

### Endpoints

#### Annulation manuelle de l'envoi programmé
```
POST /api/quotes/{id}/cancel-send
```
- Requiert auth (utilisateur connecté)
- 200 si le devis était `PENDING` → passage à `CANCELLED`, `scheduledSendAt = null`
- 409 Conflict si le devis n'est pas dans un état annulable (déjà `SENT`, `FAILED`, `NOT_APPLICABLE`)
- 404 si le devis n'existe pas

#### Forçage manuel de l'envoi (sans attendre le cron)
```
POST /api/quotes/{id}/send-now
```
- Requiert auth
- 200 si le devis est `PENDING` ou `FAILED` → déclenchement immédiat (passe par la même logique que le scheduler, mais sur ce seul devis)
- 409 Conflict si état non envoyable (déjà `SENT`, `CANCELLED`, `NOT_APPLICABLE`)

#### Repassage en DRAFT (existant en LOT 6, à patcher)
Si l'endpoint de revert/édition repasse un devis `FINALIZED` en `DRAFT`, alors :
- `emailStatus = CANCELLED` (si était `PENDING`) ou conservé (si `SENT` ou `FAILED`)
- `scheduledSendAt = null`

### Sécurité

- Aucune nouvelle clé secrète à introduire : on réutilise `MAILJET_API_KEY` et `MAILJET_API_SECRET` du LOT 10
- Validation stricte du contenu utilisateur dans les templates : autoescaping Thymeleaf actif → XSS bloqué nativement
- Pas de log de l'API key (déjà la règle du LOT 10)
- Le PDF est généré en mémoire (byte[]), pas écrit sur disque, donc pas de fuite par chemin temp accessible
- Le mail au client est envoyé en `Reply-To: {company.email}` pour que les réponses arrivent à l'entreprise et non au sender Mailjet

### Tests à inclure dans le lot (règle transversale)

```
test/
  service/
    QuoteEmailServiceImplTest.java
      - send happy path → status SENT
      - send échec transitoire → retry incrémenté, status PENDING, scheduledSendAt repoussé
      - send échec après max-attempts → status FAILED + notif créateur tentée
      - send hors fenêtre → no-op
      - send avec quote-send.enabled=false → no-op
      - quote sans email client → exclu de la sélection
  scheduler/
    QuoteSendSchedulerTest.java
      - intégration : cron déclenche bien processPendingSends
  controller/
    QuoteControllerTest.java (ajouts)
      - cancel-send : 200 si PENDING, 409 sinon
      - send-now : 200 si PENDING ou FAILED, 409 sinon
  repository/
    QuoteRepositoryTest.java (ajout)
      - findReadyToSend filtre correctement (status, scheduledSendAt, emailStatus)
```

L'envoi réel Mailjet est mocké (`@MockBean EmailService`) — pas d'appel HTTP en test. La couverture du chemin Mailjet réel est assurée par les tests du LOT 10.

### Critères de validation

- `./mvnw verify` passe vert
- À la finalisation d'un devis avec email client : `emailStatus = PENDING`, `scheduledSendAt = finalizedAt + 24h`
- À la finalisation d'un devis sans email client : `emailStatus = NOT_APPLICABLE`, PDF toujours téléchargeable
- Le scheduler tourne à la fréquence configurée et n'envoie pas hors `send-window`
- L'appel à `POST /api/quotes/{id}/cancel-send` annule effectivement l'envoi programmé
- L'appel à `POST /api/quotes/{id}/send-now` déclenche immédiatement
- Trois échecs consécutifs simulés font basculer le devis en `FAILED` et déclenchent un mail au créateur avec coordonnées client
- Le mail au client contient : référence devis, PDF joint, coordonnées entreprise en Reply-To
- Repasser un devis `FINALIZED` en `DRAFT` annule l'envoi programmé
- Aucune clé Mailjet présente dans le repo (`git grep -i mailjet` à blanc côté secrets)
- Toggles `app.email.enabled = false` ET `app.email.quote-send.enabled = false` désactivent chacun la feature indépendamment
