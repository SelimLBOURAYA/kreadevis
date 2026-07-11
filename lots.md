# Lots de migration — kreadevis-backend

## Contexte

Migration de `kreadevis` (Spring Boot 3.3.4 / MVC / JSP) vers `kreadevis-backend` (Spring Boot 4 / REST API pure / Angular frontend séparé).

- Ancien projet : `/home/selim/ENV/projets/kreadevis/`
- Nouveau projet : `/home/selim/ENV/projets/kreadevis-backend/`
- Stack cible : Spring Boot 4.0.6, Java 25, PostgreSQL 17 (Docker), MapStruct, OpenPDF, JWT

**Périmètre produit** : Kreadevis est un logiciel de **devis** uniquement — la facturation est **hors périmètre** (décision du 10/07/2026). L'endpoint « facture » hérité du legacy (`GET /api/quotes/{id}/invoice/pdf`, config `facture-dir`) sera **supprimé** au lot 14, pas mis en conformité.

## Règle transversale — tests par lot

**Chaque lot inclut ses tests unitaires.** Les tests ne sont pas regroupés à la fin du projet : ils sont écrits en même temps que le code du lot, dans la même PR. `./mvnw verify` doit passer vert avant tout commit et avant l'ouverture de la PR. Voir `CLAUDE.md` (section *Workflow par lot*) pour la règle complète.

Le **lot 9** initialement prévu comme "tests à écrire" est redéfini en **infrastructure de tests** : remplacement des starters fantômes du pom, ajout H2/Testcontainers, tests d'intégration `@WebMvcTest` / `@DataJpaTest`, consolidation de couverture sur les zones déjà couvertes localement.

---

## État des lots

| Lot | Branche                        | Statut      | Catégorie          |
|-----|--------------------------------|-------------|--------------------|
| 1   | chore/lot-1-setup              | ✅ terminé  | infra              |
| 2   | feat/lot-2-entities            | ✅ terminé  | domaine            |
| 3   | feat/lot-3-security            | ✅ terminé  | sécurité (base)    |
| 4   | feat/lot-4-dto-mappers         | ✅ terminé  | API                |
| 5   | feat/lot-5-crud                | ✅ terminé  | API                |
| 6   | feat/lot-6-quote               | ✅ terminé  | métier             |
| 7   | feat/lot-7-pdf                 | ✅ terminé  | métier             |
| 8   | feat/lot-8-csv                 | ✅ terminé  | métier             |
| 8b  | feat/lot-8b-csv-profiles       | ⬜ à faire  | métier (optionnel) |
| 9   | feat/lot-9-tests               | ✅ terminé  | qualité            |
| 10  | chore/lot-10-liquibase         | ✅ terminé  | infra              |
| 11  | feat/lot-11-quote-email-send   | ⬜ à faire  | métier             |
| 11b | feat/lot-11b-email-reminders   | ⬜ optionnel | métier             |
| 12  | feat/lot-12-quote-integrity    | ✅ terminé  | métier             |
| 12b | feat/lot-12b-front-unblock     | ⬜ à faire  | correctifs / API   |
| 13  | feat/lot-13-pagination         | ⬜ à faire  | API                |
| 14  | feat/lot-14-api-hygiene        | ⬜ à faire  | qualité / API      |
| 15  | feat/lot-15-rbac-ownership     | ⬜ à faire  | sécurité           |
| 16  | feat/lot-16-security-hardening | ⬜ à faire  | sécurité           |

**Ordonnancement des derniers lots** : le lot 10 (Liquibase) est posé en premier car toutes les évolutions de schéma des lots suivants doivent passer par des changesets versionnés. Les lots 12→14 traitent ensuite l'intégrité métier, la pagination et l'hygiène API. Les lots 15 et 16, à dominante sécurité, sont volontairement positionnés en fin de cycle pour tester les fonctionnalités métier sans buter sur des restrictions d'autorisation. Voir `security.md` pour la justification technique des lots 15-16.

**Audit du 10/07/2026** — un audit croisé back/front a confirmé (test à l'appui) un bug bloquant : `LazyInitializationException` sur **toutes les lectures de devis** (détail au lot 12, point 0). Il a aussi montré que l'intégration front↔back n'a **jamais** été exercée : aucun bean CORS côté back, lot 0 front non réalisé, lots front validés uniquement avec HTTP mocké. Conséquences sur le plan :
- **lot 12 étendu** : fix transactionnel en tête de lot, appartenance item↔devis, garde de suppression, test d'intégration non transactionnel avec Liquibase actif ;
- **nouveau lot 12b** : CORS (avancé depuis le lot 16), `GET /api/users/me`, correctif header CSV — débloque les lots front 6→10 ;
- **lot 14** : le périmètre « facture » est **retiré** du produit (logiciel de devis uniquement), au lieu d'être renommé ;
- **lot 16** : ajouts §8–10 (identité société externalisée, nettoyage `/actuator`, identifiant canonique) ;
- deux décisions produit à acter — voir « Questions ouvertes » en fin de fichier.

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

**Branche :** `feat/lot-7-pdf` — PR #9 mergée
**Commit :** `8247200 feat(7): implement PDF generation with OpenPDF`

> ⚠️ **Périmètre facture retiré a posteriori** (décision du 10/07/2026) : Kreadevis est un logiciel de devis, pas de facturation. `generateInvoicePdf` et `GET /api/quotes/{id}/invoice/pdf` livrés par ce lot seront supprimés au lot 14 (§5).

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

## LOT 8 — Import CSV ✅

**Branche :** `feat/lot-8-csv` — PR #11 mergée
**Commits :** `2533490 feat(8): implement CSV product import`, upsert + BigDecimal migration

Réalisé :
- Import multipart CSV via Apache Commons CSV
- `CsvImportServiceImpl` : parsing, validation, upsert (referenceCode existant → update, sinon création)
- Colonnes configurables via `CsvColumnProperties` (`@ConfigurationProperties`) dans `application.yaml`
- Migration `float` / `Float` → `BigDecimal` sur `Product`, `Quote`, `QuoteItem` et tous les DTOs monétaires (`fcc9f78`, `9aae3ff`)

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

## LOT 8b — Profils CSV par client (optionnel) ⬜

**Branche :** `feat/lot-8b-csv-profiles`
**Statut :** planifié, pas encore implémenté

### Contexte
Le lot 8 rend les noms de colonnes CSV configurables via `application.yaml` (mono-instance, un seul mapping global). Ce lot étend ce mécanisme pour stocker un profil de configuration CSV par client en base de données : si un `profileId` est fourni à l'import, les mappings de ce profil sont utilisés à la place de ceux du fichier de propriétés.

### Objectif
Permettre à chaque client d'avoir son propre mapping de colonnes CSV (le format du fichier d'export de son ERP peut différer), sans modifier `application.yaml` ni redémarrer l'application.

### Entité à créer
```java
@Entity
@Table(name = "csv_profiles")
public class CsvProfile {
    @Id @GeneratedValue
    private Long id;

    private String name;            // libellé lisible
    private String clientRef;       // référence libre (client, contexte)

    // noms de colonnes — null = fallback sur CsvColumnProperties
    private String colLabel;
    private String colDescription;
    private String colStockQuantity;
    private String colUnitPrice;
    private String colVatRate;
    private String colReferenceCode;
}
```

### Endpoints à ajouter
```
POST   /api/csv-profiles          ← créer un profil
GET    /api/csv-profiles          ← lister les profils
GET    /api/csv-profiles/{id}     ← détail
DELETE /api/csv-profiles/{id}     ← supprimer

POST   /api/products/import?profileId={id}  ← import avec profil optionnel
```

### Logique d'import
```
if profileId != null
    charger CsvProfile depuis la base
    fusionner avec CsvColumnProperties (le profil écrase colonne par colonne)
else
    utiliser CsvColumnProperties par défaut
```

### Critères de validation
- Créer un profil, importer un CSV en le référençant → colonnes lues selon le profil
- Import sans `profileId` → comportement identique au lot 8 (fallback properties)
- Profil inexistant → 404
- `./mvnw verify` passe vert

---

## LOT 9 — Infrastructure de tests & intégration ✅

**Branche :** `feat/lot-9-tests` — en cours (PR à ouvrir)
**Commits :** `chore(9)` pom, `test(9)` QuoteRepository, `test(9)` controllers

Réalisé :
- `pom.xml` : ajout `spring-boot-starter-webmvc-test` et `spring-boot-starter-data-jpa-test` (nouveaux artefacts distincts en Spring Boot 4)
- `QuoteRepositoryTest` (`@DataJpaTest`) : 5 tests sur `findMaxDailySequence` et `findByDateRange`
- `AuthControllerTest` (`@WebMvcTest`) : 4 tests login/register (succès + validation)
- `ClientControllerTest` (`@WebMvcTest`) : 6 tests dont 401 unauthenticated
- `QuoteControllerTest` (`@WebMvcTest`) : 6 tests dont cancel d'un devis finalisé → 500
- Total : 92 tests, 0 failures

Points techniques Spring Boot 4 :
- `@ImportAutoConfiguration(ServletWebSecurityAutoConfiguration.class)` pour activer `@EnableWebSecurity` dans le slice
- `MockMvcBuilderCustomizer` + `springSecurity()` pour que `@WithMockUser` fonctionne
- `HttpStatusEntryPoint(401)` pour retourner 401 (défaut Spring Security 7 = 403)
- `JwtAuthFilter` (`@Component Filter`) toujours chargé par `WebMvcTypeExcludeFilter` → `@MockitoBean JwtUtils, UserDetailsServiceImpl` dans tous les controller tests

---

## LOT 11 — Envoi du devis par email au client ⬜

**Branche :** `feat/lot-11-quote-email-send`
**Commit cible :** `feat(11): send quote pdf to client by email`

### Objectif
Permettre au commerçant d'envoyer le devis (PDF généré au lot 7) directement par email au client en un clic, depuis l'app. Provider : **Mailjet via REST API** (pas de SMTP) en utilisant le `RestClient` Spring. Pose toute l'infra email partagée avec le lot 11b (scheduler optionnel).

### Prérequis
- Lot 3 (Security) terminé → `SecurityContextHolder` exploitable pour récupérer l'utilisateur courant
- Lot 7 (PDF) terminé → `PdfService.generateQuotePdf(quoteId)` disponible

### Modification entité Quote
Ajouter la trace d'envoi (le `createdBy` est déjà présent depuis le lot 6) :
```java
@Column(name = "sent_at")
private LocalDateTime sentAt;     // dernier envoi (null = jamais envoyé)

@Column(name = "sent_to")
private String sentTo;            // email réellement utilisé pour le dernier envoi
```
Changeset Liquibase associé : `003-add-quote-sent-tracking.yaml`.

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
```

### Fichiers à créer
```
config/
  EmailProperties.java              ← @ConfigurationProperties("app.email")
  MailjetClientConfig.java          ← Bean RestClient (baseUrl + Basic Auth)
dto/email/
  EmailMessage.java                 ← record (to, toName, subject, htmlBody, attachment)
  EmailAttachment.java              ← record (filename, contentType, base64Content)
  MailjetPayload.java               ← payload Mailjet v3.1
dto/quote/
  SendQuoteRequest.java             ← record (String recipientOverride, String customMessage)
service/
  EmailService.java                 ← send(EmailMessage)
  impl/MailjetEmailServiceImpl.java ← RestClient → POST /v3.1/send
  QuoteEmailService.java            ← sendQuoteToClient(quoteId, request)
  impl/QuoteEmailServiceImpl.java   ← compose mail + appel PdfService + EmailService
controller/
  QuoteDocumentController.java      ← ajout POST /api/quotes/{id}/send
resources/templates/email/
  quote.html                        ← template Thymeleaf (corps du mail au client)
```

### Endpoints
```
POST /api/quotes/{id}/send         ← body SendQuoteRequest optionnel
                                     → 200 OK { sentAt, sentTo }
                                     → 409 si app.email.enabled=false
                                     → 422 si client.email null et pas d'override
```

### Sécurité
- API key + secret Mailjet **uniquement** via variables d'environnement (`MAILJET_API_KEY`, `MAILJET_API_SECRET`)
- `.env.example` mis à jour (les variables seront enforcées au lot 16)
- HTTPS forcé (URL Mailjet en `https://`)
- Autoescaping Thymeleaf actif → protection XSS native sur les données client/devis
- Kill switch `app.email.enabled` (réponse 409 si désactivé)
- Aucun log de l'API key (logger uniquement le code HTTP de retour Mailjet)
- `customMessage` du body : escapé par Thymeleaf, taille max 1000 chars

### Critères de validation
- `./mvnw verify` passe vert
- Postman : `POST /api/quotes/{id}/send` → 200 + mail effectivement reçu sur compte test Mailjet
- Mail contient : nom du commerçant en signature, référence devis, PDF en pièce jointe
- Sans `recipientOverride` → utilise `client.email` ; avec → utilise l'adresse fournie
- Quote `sentAt` et `sentTo` mis à jour après chaque envoi réussi
- Aucune clé Mailjet présente dans le repo (`git grep` à blanc)

---

## LOT 11b — Relances hebdomadaires automatisées ⬜ (optionnel)

**Branche :** `feat/lot-11b-email-reminders`
**Commit cible :** `feat(11b): implement weekly email reminders for pending quotes`

### Objectif
Envoyer chaque lundi matin un mail récapitulatif au créateur de chaque devis listant ses devis `PENDING` depuis plus de N jours (seuil configurable). Réutilise toute l'infra Mailjet/EmailService/templates du lot 11.

### Prérequis
- Lot 11 terminé → `EmailService`, `EmailProperties`, `MailjetClientConfig` disponibles

### Ajout config `application.yaml`
```yaml
app:
  email:
    reminder:
      enabled: true
      pending-threshold-days: 10
      cron: "0 0 8 * * MON"
```

### Fichiers à créer
```
config/
  SchedulingConfig.java             ← @EnableScheduling
service/
  ReminderService.java              ← buildReminders() : Map<User, List<Quote>>
  impl/ReminderServiceImpl.java
scheduler/
  WeeklyReminderScheduler.java      ← @Scheduled(cron = "${app.email.reminder.cron}")
resources/templates/email/
  reminder.html                     ← template Thymeleaf (récap pour le commerçant)
controller/
  AdminReminderController.java      ← POST /api/admin/reminders/trigger
```

### Repository
```java
@Query("SELECT q FROM Quote q WHERE q.status = 'PENDING' AND q.date < :cutoff")
List<Quote> findPendingOlderThan(LocalDate cutoff);
```
Groupement par `createdBy` côté `ReminderService` (Java stream).

### Sécurité
- Toggle `app.email.reminder.enabled` (désactive uniquement le scheduler, indépendant du kill switch global `app.email.enabled` du lot 11)
- Endpoint admin manuel `POST /api/admin/reminders/trigger` → `ROLE_ADMIN`

### Critères de validation
- `./mvnw verify` passe vert
- Scheduler désactivable via config sans redémarrage logique métier
- Mail récap contient nom client, téléphone, email, référence devis, date, montant
- Endpoint admin déclenche immédiatement le batch (sans attendre lundi)

---

## LOT 10 — Liquibase (migrations versionnées) ✅

**Branche :** `chore/lot-10-liquibase` — PR #13 mergée
**Commit :** `9bec1e3 chore(10): introduce liquibase, baseline current schema (#13)`

> ⚠️ **Réserve (audit 10/07/2026)** : le profil de test désactive Liquibase (`liquibase.enabled: false` + `ddl-auto: create-drop`) — les changesets ne sont donc jamais exercés par `./mvnw verify`. Filet posé au lot 12 (test d'intégration avec Liquibase actif).

### Objectif
Sortir du mode `ddl-auto: update` (dev) et `validate` (prod) qui rend impossible une mise à jour de schéma maîtrisée en production. Toutes les évolutions de schéma à partir de ce lot sont décrites en **changesets Liquibase** versionnés dans le repo.

### Pourquoi maintenant
Le lot 11 (email-reminders) et les lots suivants introduisent de nouvelles colonnes. Sans gestion de migration, l'écart entre dev (qui auto-update) et prod (qui ne fait que valider) garantit un crash au premier déploiement. Liquibase doit être posé **avant** que d'autres changements de schéma soient empilés.

### Dépendances à ajouter dans `pom.xml`
```xml
<dependency>
    <groupId>org.liquibase</groupId>
    <artifactId>liquibase-core</artifactId>
</dependency>
```
(Version gérée par le BOM Spring Boot.)

### Fichiers à créer
```
src/main/resources/db/changelog/
  db.changelog-master.yaml             ← include de tous les changelogs versionnés
  changes/
    001-baseline-schema.yaml           ← schéma actuel capturé tel quel
    002-add-quote-created-by.yaml      ← FK created_by → users (si pas déjà en baseline)
```

### Stratégie de baseline
1. Lancer l'app une dernière fois en `ddl-auto: update` sur une base vierge.
2. Générer le changelog initial via `liquibase --changeLogFile=001-baseline-schema.yaml generateChangeLog` (ou la tâche Maven équivalente).
3. Vérifier le diff à la main, commiter le fichier.
4. Sur une base **existante** (dev local du dev), exécuter `changelogSync` pour marquer les changesets baseline comme déjà appliqués sans les rejouer.
5. Basculer `ddl-auto: validate` sur **tous** les profils (dev compris).

### Modifications config
- `application.yaml` : retirer `ddl-auto: update` du profil `dev`, garder `validate` partout.
- Ajouter :
```yaml
spring:
  liquibase:
    change-log: classpath:db/changelog/db.changelog-master.yaml
    enabled: true
```

### Critères de validation
- `./mvnw verify` passe sur une base vide → Liquibase crée tout le schéma sans `ddl-auto`.
- `./mvnw verify` re-tourné → aucun changeset rejoué, schéma validé.
- Toute évolution de schéma ultérieure (lots 12 et au-delà) **doit** passer par un nouveau changeset numéroté.
- `application.yaml` : aucun `ddl-auto: update` ne subsiste.
- Documentation rapide dans `HELP.md` : "comment ajouter un changeset".

---

## LOT 12 — Intégrité métier devis ✅

**Branche :** `feat/lot-12-quote-integrity` — PR #14
**Commits :**
- `5061847 feat(12): split quote totalPrice into HT/VAT/TTC, snapshot vatRate on items`
- `59d263f feat(12): guard quote items against finalized/cancelled, recompute totals on every change`
- `7b587c9 feat(12): render quote PDF with HT/TVA/TTC breakdown`
- `52a65b3 fix(12): map business IllegalStateException to 409 Conflict`
- `de24ecd test(12): cover status guards, VAT computation, date realignment, 409 mapping`
- `b0e1f24 refactor(12): move recomputeTotals from QuoteTotals utility to Quote entity` *(PR review)*
- `206cd03 fix(12): add transactional reads, item ownership check, and delete guard`
- `00f75af test(12): add non-transactional integration test for quote reads and PDF`
- `eeb8535 docs(12): add lot-12 audit report`

**Audit :** `docs/audits/lot-12.md` — 0 Critical, 3 Warning (N+1 préexistants, listes non paginées, Liquibase inerte sur SB4)

**Fait :**
- Points 0→7 implémentés — lectures transactionnelles, garde-fous statut, recalcul totals, snapshot TVA + TTC, cohérence date↔référence, mapping 409, appartenance item↔devis, garde suppression devis finalisé
- `QuoteTotals` supprimé → `Quote.recomputeTotals()` (rich domain model, convention §1)
- 106 tests, 0 failures, JaCoCo gate verte
- Test d'intégration non transactionnel ajouté (`QuoteReadIntegrationTest`)

**Réserve :** Liquibase non exercé par les tests (Spring Boot 4 sans auto-configuration Liquibase) → lot dédié recommandé par l'audit
**Problèmes connus :** N+1 lazy-loads sur `findAll`/`findByClientId` → lot 13 (pagination) atténuera ; chemin `include` dans `db.changelog-master.yaml` probablement incorrect (masqué car Liquibase ne tourne pas)

### Objectif
Corriger les divergences entre l'état stocké et l'état affiché des devis, et exposer un total HT/TVA/TTC cohérent. Ces bugs sont indépendants de la sécurité et peuvent être validés manuellement via Postman ou le front Angular dès qu'ils sont livrés. **Étendu suite à l'audit du 10/07/2026** (points 0, 6 et 7 + test d'intégration).

### Périmètre

**0. Fix bloquant — lectures de devis transactionnelles** *(constat d'audit confirmé par test le 10/07/2026)*
- `open-in-view: false` + `Quote.items` LAZY + services non transactionnels en lecture : `LazyInitializationException` sur `GET /api/quotes`, `/api/quotes/{id}`, `/api/quotes/search`, `/api/clients/{id}/quotes` et les endpoints PDF (`QuoteMapperImpl` touche la collection hors session). Ces endpoints ne fonctionnent que dans les tests (slices mockés / `@DataJpaTest` transactionnel), jamais sur une app réellement lancée.
- Fix : `@Transactional(readOnly = true)` sur les lectures de `QuoteServiceImpl` et `PdfServiceImpl` (ou fetch join dédié dans `QuoteRepository`).
- À livrer **en premier** : tout le reste du lot se valide par-dessus.

**1. Garde-fous sur statut**
- `QuoteItemServiceImpl.addItem/updateItem/deleteItem` : refuser si `quote.status ∈ { FINALIZED, CANCELLED }`. Lever `IllegalStateException("Cannot modify items of a finalized/cancelled quote")`.

**2. Recalcul du `totalPrice`**
- Après chaque `addItem/updateItem/deleteItem`, recalculer `quote.totalPrice` et le persister.
- Extraire l'appel commun `recomputeTotalPrice(Quote)` (utilisé aussi par `finalize()`).
- Filtrer les items `active = true` dans la somme — corrige la divergence avec le PDF.

**3. Snapshot TVA + total TTC**
- Ajouter `vatRate` (BigDecimal) sur `QuoteItem` (Liquibase changeset).
- `QuoteItemServiceImpl.addItem` snapshote `product.getVatRate()` au moment de l'ajout (comme déjà fait pour `unitPrice`).
- Calculer trois agrégats au niveau `Quote` :
  - `totalPriceHt` (renommé depuis `totalPrice`),
  - `totalVat` (somme `item.totalPrice * item.vatRate`),
  - `totalPriceTtc`.
- Exposer dans `QuoteResponse` (DTO) et dans le PDF (`PdfServiceImpl.addTotal` : 3 lignes HT / TVA / TTC).

**4. Cohérence `referenceCode` ↔ `Quote.date`**
- Décision à acter : soit la `date` du devis = date de finalisation (pratique courante en B2B FR), soit garder la date de création et générer la référence sur cette date. **Choix proposé** : date de finalisation, car c'est la date qui figure sur le document légal.
- `QuoteServiceImpl.finalize` : `quote.setDate(LocalDate.now())` avant `assignReferenceCode`.

**5. Mapping HTTP des erreurs métier**
- `GlobalExceptionHandler` : nouveau mapping `IllegalStateException` → **409 Conflict** (cas "déjà finalisé", "cannot modify").
- Conserver le 500 uniquement pour `Exception.class` (fourre-tout).

**6. Appartenance item ↔ devis** *(audit 10/07/2026)*
- `QuoteItemServiceImpl.updateItem/deleteItem` ignorent le `quoteId` du path : n'importe quel item de n'importe quel devis est modifiable via `PUT /api/quotes/{id}/items/{itemId}`.
- Fix : vérifier `item.getQuote().getId().equals(quoteId)`, sinon `EntityNotFoundException` (→ 404).

**7. Garde de suppression** *(audit 10/07/2026)*
- `QuoteServiceImpl.delete` accepte un devis `FINALIZED` (document contractuel). Refuser avec `IllegalStateException` (→ 409 via le point 5).

### Tests à ajouter
- `QuoteItemServiceImplTest` : addItem sur quote FINALIZED → exception ; updateItem idem ; deleteItem idem.
- `QuoteItemServiceImplTest` : updateItem/deleteItem avec un `quoteId` ne correspondant pas à l'item → 404.
- `QuoteServiceImplTest` : ajout d'item recalcule le total ; suppression d'item recalcule le total ; items inactifs exclus du total ; finalize avec items mixtes (TVA 5,5 / 10 / 20) calcule TTC correctement ; delete d'un devis FINALIZED → exception.
- `GlobalExceptionHandler` indirectement testé via les controller tests existants (mettre à jour pour 409).
- **Test d'intégration non transactionnel** (`@SpringBootTest` + MockMvc, classe de test sans `@Transactional`) : `GET /api/quotes/{id}` et `GET /api/quotes/{id}/pdf` sur un devis avec items → 200. C'est le seul type de test qui attrape les régressions lazy (les `@WebMvcTest` mockent les services, les `@DataJpaTest` gardent la session ouverte).
- Dans ce test, **activer Liquibase sur H2** (profil dédié sans `liquibase.enabled: false` ni `ddl-auto: create-drop`) pour que les changesets soient enfin exercés par la gate — aujourd'hui un changeset cassé passe `./mvnw verify`. Si le changelog n'est pas H2-compatible, basculer ce test sur Testcontainers PostgreSQL.

### Critères de validation
- `./mvnw verify` vert (couverture en hausse).
- Postman : tenter d'ajouter un item à un devis finalisé → réponse `409 Conflict`.
- PDF d'un devis multi-items multi-TVA : affiche bien HT / TVA / TTC.
- `GET /api/quotes/{id}` et `GET /api/quotes/{id}/pdf` répondent 200 sur une app réellement lancée (pas seulement en slice de test).
- Un changeset Liquibase volontairement cassé fait échouer la gate (vérification ponctuelle, non commitée).

---

## LOT 12b — Débloquage front : CORS, /users/me, correctif CSV ⬜

**Branche :** `feat/lot-12b-front-unblock`
**Commits cibles :**
- `feat(12b): wire cors configuration source on app.cors.allowed-origins`
- `feat(12b): expose GET /api/users/me with roles`
- `fix(12b): honor csv file header order on product import`

### Objectif
Lever les trois blocages identifiés à l'audit du 10/07/2026 qui empêchent le front Angular d'avancer (lots front 6→10). Peut être mené en parallèle du lot 12 (pas de fichier commun hors tests).

### Périmètre

**1. CORS effectif** *(extrait du lot 16, avancé ici)*
- La propriété `app.cors.allowed-origins` existe dans `application.yaml` mais n'est lue **nulle part** : aucun bean `CorsConfigurationSource`, pas de `.cors()` dans `SecurityConfig` → tout appel navigateur depuis `http://localhost:4200` est bloqué au preflight. Le front n'a jamais pu parler au back.
- Bean `CorsConfigurationSource` branché sur la propriété + `.cors(withDefaults())` dans la chaîne de filtres.
- Le durcissement (méthodes/headers restreints, `allowCredentials`, origines prod) reste au lot 16.

**2. Utilisateur courant + rôles**
- Le front n'a aucun moyen de connaître l'utilisateur connecté : `AuthResponse` ne contient que le token, le JWT ne porte pas les rôles, pas d'endpoint `me`. Le front fabrique un user factice (`id 0, roles []`) — bloque le lot front 10 (admin users) et l'affichage du profil.
- `GET /api/users/me` → `UserResponse` (id, login, email, roles) depuis le `SecurityContextHolder`.
- Optionnel : enrichir `AuthResponse` avec le `UserResponse` pour économiser un aller-retour au login.

**3. Correctif import CSV : header réel du fichier ignoré**
- `CSVFormat` est construit avec `setHeader(colonnes de la config)` + `skipHeaderRecord(true)` : les colonnes sont lues **positionnellement** dans l'ordre de la config, le header du fichier est jeté. Un fichier avec les mêmes colonnes dans un autre ordre est importé silencieusement faux (prix ↔ stock permutés). La configurabilité des noms de colonnes (prérequis du lot 8b) ne fonctionne pas en l'état.
- Fix : `setHeader()` sans argument (header inféré du fichier) + accès par nom configuré ; 400 explicite si une colonne requise manque.
- Le champ `warnings` de `CsvImportResult` n'est jamais alimenté : l'alimenter ou le retirer.

### Tests
- `@WebMvcTest` : `GET /api/users/me` authentifié → 200 avec roles ; non authentifié → 401.
- `CsvImportServiceImplTest` : colonnes dans un ordre différent de la config → import correct ; colonne requise absente du header → erreur explicite.
- Test CORS (MockMvc) : preflight OPTIONS depuis une origine autorisée → headers CORS présents ; origine inconnue → refus.

### Critères de validation
- `./mvnw verify` vert.
- Depuis le front lancé en local (`ng serve`), le login puis un `GET /api/clients` aboutissent sans erreur CORS — clôture du lot 0 front.

---

## LOT 13 — Pagination & filtres ⬜

**Branche :** `feat/lot-13-pagination`
**Commit cible :** `feat(13): paginate collection endpoints, add basic filters`

### Objectif
Aucune ressource liste actuellement n'est paginée. En prod, le premier `GET /api/clients` ou `/api/products` sur une base réelle renvoie tout. Ce lot impose une pagination par défaut et ajoute les filtres de base demandés par le front.

### Périmètre

**1. Pagination Spring Data**
- Tous les `findAll()` des services retournent désormais `Page<...Response>` au lieu de `List<...Response>`.
- Repositories : signature `Page<T> findByActiveTrue(Pageable)` (et variantes filtrées).
- Controllers : signature `getAll(Pageable pageable)` (Spring résout automatiquement `?page=0&size=20&sort=field,asc`).

**2. Plafond global**
- Ajouter dans `application.yaml` :
```yaml
spring:
  data:
    web:
      pageable:
        default-page-size: 20
        max-page-size: 100
```
Empêche un appel `?size=1000000` de saturer la base.

**3. Filtres de base (sans Specification, on garde simple)**
- `GET /api/clients?search=...` → `lastName` ou `company` contient (LIKE case-insensitive).
- `GET /api/products?search=...` → `label` ou `referenceCode` contient.
- `GET /api/quotes?status=...&startDate=...&endDate=...` (`status` ajouté en plus du filtre date existant).

**4. Format de réponse**
- Conserver le format Spring par défaut (`content`, `totalElements`, `totalPages`, `number`, `size`). Le front Angular est en cours de design, ce format est le plus standard.

### Tests
- `@WebMvcTest` sur chaque controller : `GET ?page=0&size=2` → max 2 éléments retournés, `totalElements` correct.
- Test du plafond : `?size=1000` → tronqué à 100.

### Critères de validation
- `./mvnw verify` vert.
- Aucun endpoint liste ne retourne plus de 100 éléments en un appel.

---

## LOT 14 — Hygiène API & OpenAPI ⬜

**Branche :** `feat/lot-14-api-hygiene`
**Commit cible :** `feat(14): add openapi spec, cleanup api conventions`

### Objectif
Standardiser le contrat HTTP, exposer une spec consommable par le front Angular, et nettoyer les vestiges de couplage entité↔sérialisation.

### Périmètre

**1. OpenAPI / Swagger UI**
- Dépendance `springdoc-openapi-starter-webmvc-ui` (version 2.x compatible Spring Boot 4).
- `application.yaml` : `springdoc.swagger-ui.path: /swagger-ui.html`.
- Bean `OpenAPI` minimal (title, version, security scheme JWT Bearer).
- En prod : désactiver Swagger UI (`springdoc.swagger-ui.enabled: false` sur profil `prod`).

**2. Codes HTTP corrects**
- Tous les `POST /api/.../create` retournent **201 Created** + header `Location: /api/.../{id}` au lieu de 200 OK.
- Utiliser `ResponseEntity.created(URI.create(...)).body(...)`.

**3. Nettoyage entités**
- Retirer **tous** les `@JsonIgnoreProperties` / `@JsonIgnore` sur entités JPA — aucune entité n'est sérialisée directement, ce sont les DTOs qui sortent.
- Exception conservée : `@JsonIgnore` sur `User.password` (ceinture + bretelles).

**4. Contraintes JPA cohérentes avec la logique**
- `Quote.client` → `@ManyToOne(optional = false)`.
- `Quote.createdBy` → `@ManyToOne(optional = false)` (après lot 10/15).
- Liquibase changeset correspondant (NOT NULL sur les colonnes FK).

**5. Retrait du périmètre facturation** *(décision du 10/07/2026 : Kreadevis est un logiciel de devis, pas de facturation)*
- Supprimer `GET /api/quotes/{id}/invoice/pdf` (`QuoteDocumentController`), `PdfService.generateInvoicePdf` et la variante `"FACTURE"` de `buildPdf`.
- Supprimer la config `app.document.facture-dir` (+ variable `DOC_FACTURE_DIR`) de `application.yaml` et `AppProperties`.
- Le PDF « facture » actuel était de toute façon non conforme (pas de numérotation séquentielle, pas de mentions obligatoires) : on supprime, on ne met pas en conformité.

**6. Création client atomique** *(audit 10/07/2026)*
- Le front doit aujourd'hui enchaîner `POST /api/addresses` puis `POST /api/clients` (`ClientRequest` exige un `addressId`) : adresse orpheline si le 2e appel échoue.
- Accepter une adresse **imbriquée** dans `ClientRequest` (création/mise à jour du client et de son adresse en une seule transaction).
- Retirer `AddressController` en ressource REST top-level si plus aucun usage (à synchroniser avec `ClientService` côté front).

### Tests
- Vérifier dans les `@WebMvcTest` existants que `POST` renvoie 201 et `Location`.
- Test smoke OpenAPI : `GET /v3/api-docs` retourne 200, JSON valide.
- `ClientControllerTest` : création d'un client avec adresse imbriquée en un seul POST → 201, adresse persistée.

### Critères de validation
- `./mvnw verify` vert.
- `/swagger-ui.html` accessible en dev, désactivé en prod.
- Aucun `@JsonIgnoreProperties` ne subsiste dans le package `entity`.
- `git grep -i facture` à blanc dans `src/main`.
- Créer un client (avec adresse) = un seul appel HTTP.

---

## LOT 15 — RBAC & Ownership ⬜

**Branche :** `feat/lot-15-rbac-ownership`
**Commits cibles :**
- `feat(15): add createdBy ownership to client and product`
- `feat(15): scope service queries to current user`
- `feat(15): protect admin endpoints with @PreAuthorize`

### Objectif
À partir de ce lot, un utilisateur connecté ne voit et ne modifie **que ses propres ressources**. Les opérations transversales (gestion des utilisateurs notamment) sont restreintes à `ROLE_ADMIN`.

> ⚠️ **Test fonctionnel avant ce lot** : profiter des lots 11→14 pour valider le métier en toute liberté avec un seul user. Une fois le lot 15 livré, les tests d'API exigeront au minimum deux comptes (admin + user) pour couvrir les cas d'accès croisé.

### Périmètre

**1. Ownership sur les entités utilisateur**
- Ajouter `createdBy` (FK `users`) sur `Client` et `Product` (Liquibase changeset).
- `Quote` l'a déjà depuis le lot 10.
- `QuoteItem` n'a pas besoin d'ownership direct (hérité du quote parent).

**2. Peuplement automatique**
- Services `create()` : récupèrent le user courant via `SecurityContextHolder` et l'assignent à `createdBy`.
- Helper commun `SecurityUtils.getCurrentUser()` (déduplique le code déjà présent dans `QuoteServiceImpl`).

**3. Filtrage automatique en lecture**
- Repositories : ajouter les variantes `findByCreatedByIdAndActiveTrue(Long userId, Pageable)` etc.
- Services `findAll/findById` : injectent le user courant dans la requête.
- **Bypass admin** : si l'authentifié a `ROLE_ADMIN`, la requête n'est pas filtrée (méthode séparée dans le service, pas de logique conditionnelle dans le repo).

**4. Endpoints admin-only**
- `UserController` : `@PreAuthorize("hasRole('ADMIN')")` sur **tous** les endpoints.
- Activer `@EnableMethodSecurity` dans `SecurityConfig`.
- Endpoint nouveau `POST /api/admin/users` pour créer un autre admin (registration publique ne crée que des `ROLE_USER`).

**5. Cohérence des 404 vs 403**
- Si un user tente de lire un quote d'un autre user : retourner **404 Not Found** (et pas 403) pour ne pas leaker l'existence de la ressource — bonne pratique OWASP.

### Tests à ajouter
- `userA crée un quote, userB tente de le lire` → 404.
- `userB tente DELETE /api/users/{idA}` → 403.
- `admin liste /api/clients` → voit tous les clients tous users confondus.
- Tester pour chaque ressource owned : Quote, Client, Product.

### Critères de validation
- `./mvnw verify` vert.
- Scénario Postman complet : créer admin + user1 + user2, vérifier cloisonnement.
- Aucun endpoint authentifié n'expose les données d'autrui sans `ROLE_ADMIN`.

---

## LOT 16 — Durcissement sécurité ⬜

**Branche :** `feat/lot-16-security-hardening`
**Commits cibles :**
- `feat(16): wire cors configuration source`
- `feat(16): fail fast on missing secrets via @Validated properties`
- `feat(16): rate-limit auth endpoints`
- `feat(16): enforce csv upload size and mime checks`
- `feat(16): return 401 instead of 500 on stale jwt subject`
- `feat(16): add refresh token endpoint`

### Objectif
Appliquer les mesures de durcissement décrites en détail dans `security.md`. Ce lot ne change pas la logique métier mais ferme les angles d'attaque connus.

### Périmètre (résumé — détail et justification dans `security.md`)

**1. Durcissement CORS** — le bean `CorsConfigurationSource` est posé dès le lot 12b (débloquage front) ; ici : restreindre méthodes/headers autorisés, `allowCredentials=true` côté front Angular, origines prod via variable d'environnement.

**2. Fail-fast sur les secrets** — `@Validated @NotBlank` sur `JwtProperties.secret` et `DatasourceProperties`. Suppression des valeurs par défaut (`changeme-256-bit-secret-key`, `app1pass`). Création de `.env.example` à la racine, listage explicite des variables requises.

**3. Rate-limiting `/api/auth/**`** — Bucket4j en mémoire (ou Redis si l'infra est déjà en place), 5 tentatives / minute / IP sur login et register.

**4. Contrôle d'upload CSV** — Limite Spring `spring.servlet.multipart.max-file-size=2MB`, vérification du Content-Type (`text/csv`), extension (`.csv`), header probing sur les premiers octets. Refus avec 400 si non conforme.

**5. Gestion 401 propre** — Try/catch dans `JwtAuthFilter` autour de `loadUserByUsername` : si `UsernameNotFoundException`, retourner 401 directement (pas de propagation vers `GlobalExceptionHandler` qui mappe en 500).

**6. Refresh token** — Endpoint `POST /api/auth/refresh` qui consomme un refresh token (stocké en DB, hashé, révocable) et émet un nouvel access token. Le champ `refreshTokenExpirationMs` de `JwtProperties` (déjà déclaré, inutilisé) devient effectif.

**7. Mots de passe** — Renforcement de la politique (cf. `security.md` §5) : longueur min 12, vérif breach optionnelle (HIBP API), cost factor BCrypt explicite (12).

**8. Externalisation de l'identité société** *(audit 10/07/2026)* — `app.company` (nom réel + SIREN) est committé en clair dans `application.yaml` : passer par variables d'environnement / config externe, valeurs neutres en fallback.

**9. Nettoyage `SecurityConfig`** *(audit 10/07/2026)* — retirer le `permitAll` sur `/actuator/**` : le starter actuator n'est pas dans le pom (config morte aujourd'hui, exposition totale le jour où quelqu'un l'ajoute). Si actuator est introduit un jour, n'exposer publiquement que `/actuator/health`.

**10. Identifiant canonique** *(audit 10/07/2026)* — le login se fait par `email` mais le subject JWT est le `login` : choisir un identifiant unique (proposé : `email`) et aligner `AuthServiceImpl`, `JwtUtils`, `UserDetailsServiceImpl`.

### Tests
- Test du rate-limiter : 6 appels successifs login → 6e renvoie 429.
- Test upload CSV oversized → 413, mauvais MIME → 400.
- Test JWT avec user supprimé en base → 401 (pas 500).
- Test refresh token valide → nouveau access token ; refresh révoqué → 401.
- Test fail-fast : démarrage sans `JWT_SECRET` → l'app refuse de booter.

### Critères de validation
- `./mvnw verify` vert.
- `git grep -i "changeme\|app1pass"` à blanc.
- `.env.example` présent, listant chaque variable utilisée par l'app.
- `security.md` reflète exactement ce qui a été implémenté (mettre à jour si écart).

---

## Questions ouvertes — décisions à acter

*Constats de l'audit du 10/07/2026, à trancher par le propriétaire du produit avant les lots concernés.*

**1. Format de la référence devis** — la doc (`CLAUDE.md`, mémoire projet) dit `DDMMYY-NNN`, mais le code génère `DDMMYY-{userId}-NNN` (séquence journalière **par utilisateur**, lot 6). Deux options :
- (a) assumer le multi-utilisateurs : garder `DDMMYY-{userId}-NNN` et corriger la doc ;
- (b) revenir à `DDMMYY-NNN` global : nécessite de repasser la séquence en globale par jour (le verrou pessimiste actuel porte sur les devis du user).

À trancher au plus tard pendant le lot 12 (§4, cohérence référence ↔ date).

**2. Rôle du stock produit** — `Product.stockQuantity` existe et est alimenté par l'import CSV, mais aucun mouvement de stock n'est déclenché par le cycle de vie du devis (le legacy ne le faisait pas non plus). Champ purement informatif à assumer tel quel, ou gestion de stock à spécifier dans un lot dédié ?
