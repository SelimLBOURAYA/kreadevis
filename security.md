# Sécurité — kreadevis-backend

Document de référence pour les mesures de sécurité prévues aux **lots 15 et 16**. Il explique pour chaque sujet :
1. **La menace** — ce qu'on cherche à empêcher.
2. **La règle de l'art** — l'approche standard du secteur.
3. **L'implémentation concrète** prévue dans ce projet.
4. **Comment vérifier** que la mesure est effective.

> Ce document doit être mis à jour en même temps que les lots 15 et 16 sont implémentés. Si l'implémentation diverge de ce qui est prévu ici, c'est ce document qui doit être corrigé pour refléter la réalité — pas l'inverse.

---

## 1. Stockage sécurisé des secrets

### Menace
Un secret en dur dans le code source (clé JWT, mot de passe DB, clé API) finit tôt ou tard :
- sur GitHub si le repo bascule en public,
- dans un screenshot partagé sur Slack,
- dans un dump CI d'environnement,
- dans une sauvegarde non chiffrée,
- dans l'historique git, où il restera retrouvable même après suppression.

Un secret leaké est compromis pour toujours — la seule réponse correcte est la **rotation**, jamais le nettoyage. Et tant qu'il n'est pas tourné, n'importe qui peut s'authentifier en tant qu'admin (cas JWT_SECRET) ou lire toute la base (cas DB password).

### Règles de l'art

**Hiérarchie de confiance des mécanismes de stockage** (du moins bon au meilleur) :

| Niveau | Mécanisme | Quand l'utiliser |
|--------|-----------|------------------|
| ❌ | Hardcodé dans le code | **Jamais** |
| ❌ | Hardcodé dans `application.yaml` versionné | **Jamais** pour un secret |
| ⚠️ | Valeur par défaut Spring (`${VAR:default}`) | Acceptable uniquement pour de la config **non sensible** (port, URL publique) |
| ✅ | Variable d'environnement injectée au démarrage | Dev local, conteneur, environnement standard |
| ✅ | Fichier `.env` lu par Docker Compose ou direnv, **non versionné** | Dev local |
| ✅✅ | Gestionnaire de secrets (HashiCorp Vault, AWS Secrets Manager, GCP Secret Manager, Azure Key Vault) | Prod sérieuse — rotation auto, audit, IAM |
| ✅✅ | Spring Cloud Config + chiffrement, ou Kubernetes Secrets + sealed-secrets | Si déployé sur K8s |

**Principes non négociables** :
- `.env` **listé dans `.gitignore`**, jamais committé.
- `.env.example` committé, avec les **noms** des variables mais aucune valeur réelle.
- **Pas de valeur par défaut** pour un secret. L'app doit refuser de démarrer si la variable est absente — c'est mieux qu'un démarrage avec une clé `changeme` qui passe en prod six mois plus tard.
- **Fail fast** : `@Validated` + `@NotBlank` sur les `@ConfigurationProperties` qui portent des secrets.
- **Aucun log** ne doit jamais contenir un secret (vérifier les `toString()`, les exceptions de connexion DB qui affichent parfois le mot de passe…).
- Un secret leaké se **rotate immédiatement**, on ne tente pas de "le retirer du repo" (l'historique git est définitif côté distant).

### Implémentation prévue (Lot 16)

**État actuel du projet** : `application.yaml` contient :
```yaml
app:
  jwt:
    secret: ${JWT_SECRET:changeme-256-bit-secret-key}   # ⚠️ default dangereux
spring:
  datasource:
    password: ${DATASOURCE_PASSWORD:app1pass}            # ⚠️ default dangereux
```

**Cible** :
```yaml
app:
  jwt:
    secret: ${JWT_SECRET}                                # pas de default
spring:
  datasource:
    password: ${DATASOURCE_PASSWORD}
```

```java
@Validated
@ConfigurationProperties("app.jwt")
public record JwtProperties(
    @NotBlank @Size(min = 64) String secret,
    @Positive long accessTokenExpirationMs,
    @Positive long refreshTokenExpirationMs
) {}
```

Si `JWT_SECRET` est absent ou trop court, l'application **refuse de démarrer** avec un message d'erreur explicite. C'est l'effet recherché.

**`.env.example` à committer** :
```
# Base de données PostgreSQL (lot 1)
DATASOURCE_URL=jdbc:postgresql://localhost:5432/app1db
DATASOURCE_USERNAME=app1user
DATASOURCE_PASSWORD=

# Sécurité JWT (lot 3) — générer avec : openssl rand -base64 64
JWT_SECRET=

# Mailjet (lot 10)
MAILJET_API_KEY=
MAILJET_API_SECRET=

# CORS (lot 16)
CORS_ORIGINS=http://localhost:4200
```

### Comment vérifier
- `git grep -i "changeme\|app1pass\|password.*="` → aucun résultat dans le code applicatif.
- Démarrage sans `JWT_SECRET` défini → l'app sort avec un message du type `Binding to target [Bindable@... type = JwtProperties] failed: Property: app.jwt.secret; Value: null`.
- `cat .env.example` → toutes les variables listées dans le tableau de `CLAUDE.md` y figurent, avec valeurs vides.

---

## 2. CORS (Cross-Origin Resource Sharing)

### Menace (ou plutôt : à quoi CORS sert vraiment)
CORS est **mal compris** par défaut. Il ne protège pas le **serveur** — il protège le **navigateur** de l'utilisateur. Sans CORS, un site malveillant `evil.com` ouvert dans le navigateur d'un utilisateur connecté à kreadevis pourrait, via JavaScript, faire des appels `fetch()` vers `kreadevis.fr/api/...` en utilisant les cookies/credentials de l'utilisateur, et exfiltrer les réponses.

Le mécanisme de **Same-Origin Policy** du navigateur bloque par défaut ces appels cross-origin. CORS est le mécanisme par lequel **le serveur autorise explicitement** certaines origines à faire des appels cross-origin.

**Conséquence pratique** : si tu n'as **pas** de CORS configuré, ton front Angular (servi depuis `http://localhost:4200` en dev) ne pourra **pas** appeler ton backend (servi depuis `http://localhost:8080`). Les requêtes seront bloquées par le navigateur avec une erreur `CORS policy: No 'Access-Control-Allow-Origin' header is present`.

**Erreur classique à éviter** : mettre `Access-Control-Allow-Origin: *` "pour que ça marche". C'est désactiver la protection. À éviter dès qu'il y a authentification.

### Règles de l'art

- **Liste blanche d'origines explicites** — jamais `*` quand l'API est authentifiée.
- **`allowCredentials: true`** uniquement si tu utilises des cookies ou l'header `Authorization` (notre cas avec JWT Bearer). Incompatible avec `*` côté origines.
- **Méthodes restreintes** au strict nécessaire (`GET, POST, PUT, DELETE`).
- **Headers exposés** explicites (notamment `Authorization`, `Content-Type`).
- **Max-Age** sur le preflight (3600s) pour réduire le bruit de requêtes `OPTIONS`.
- Origines **différenciées par environnement** : `localhost:4200` en dev, `https://kreadevis.fr` en prod.
- **HTTPS obligatoire en prod** dans la liste — pas de `http://kreadevis.fr`.

### Implémentation prévue (Lot 16)

**État actuel** : `app.cors.allowed-origins` est lue dans `application.yaml` mais **aucun bean** ne la consomme. Le front Angular sera bloqué.

**Cible** : un `CorsConfigurationSource` câblé dans `SecurityConfig`.

```java
@Bean
public CorsConfigurationSource corsConfigurationSource(
        @Value("${app.cors.allowed-origins}") List<String> allowedOrigins) {
    CorsConfiguration config = new CorsConfiguration();
    config.setAllowedOrigins(allowedOrigins);
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
    config.setExposedHeaders(List.of("Location"));
    config.setAllowCredentials(true);
    config.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/api/**", config);
    return source;
}
```

Et dans `filterChain` :
```java
http.cors(cors -> cors.configurationSource(corsConfigurationSource));
```

### Comment vérifier
- `curl -i -H "Origin: http://localhost:4200" -X OPTIONS http://localhost:8080/api/clients` → réponse contient `Access-Control-Allow-Origin: http://localhost:4200`.
- `curl -i -H "Origin: http://evil.com" -X OPTIONS http://localhost:8080/api/clients` → pas de header `Access-Control-Allow-Origin` (rejet).
- Le front Angular `localhost:4200` peut appeler le backend sans erreur console.

---

## 3. Rate limiting

### Menace
Sans rate-limiting, l'endpoint `/api/auth/login` accepte autant de tentatives que l'attaquant veut, depuis n'importe quelle IP. Conséquences :
- **Brute force** : test de millions de mots de passe sur un compte ciblé.
- **Credential stuffing** : test de couples (login, password) leakés depuis d'autres sites (LinkedIn, Adobe, etc.) — extrêmement efficace car ~30% des gens réutilisent leurs mots de passe.
- **Enumeration** : si les messages d'erreur diffèrent entre "user inconnu" et "mauvais password", un attaquant peut énumérer les comptes valides à plein débit.
- **Déni de service applicatif** : `BCryptPasswordEncoder` est volontairement lent (~100ms/appel). Quelques centaines de tentatives/seconde saturent le CPU.

Plus généralement, **tout endpoint public coûteux** (génération PDF, import CSV, export volumineux) mérite un plafond pour empêcher l'abus.

### Règles de l'art

| Endpoint type | Plafond raisonnable |
|---|---|
| `/api/auth/login`, `/api/auth/register` | 5 / minute / IP, 20 / heure / IP |
| `/api/auth/refresh` | 30 / minute / IP |
| Endpoints lourds (PDF, export, import CSV) | 10 / minute / user authentifié |
| API CRUD standard authentifiée | 600 / minute / user (= 10/s, généreux) |

**Stratégie de réponse** :
- Quand le plafond est atteint, retourner **HTTP 429 Too Many Requests** avec l'header `Retry-After: <secondes>`.
- Logger l'IP et le compteur, mais **ne pas leaker** au client le plafond restant (header `X-RateLimit-Remaining` est optionnel et un signal pour l'attaquant).

**Granularité** :
- Sur endpoints non-authentifiés (login) → clé = **IP source**.
- Sur endpoints authentifiés → clé = **user ID** (sinon un user peut spammer en changeant d'IP via proxy).
- Idéalement, **les deux** dimensions : `(IP, user)`.

**Stockage du compteur** :
- **In-memory (Bucket4j local)** : suffisant pour une **seule instance**. Si l'app scale horizontalement, chaque instance a son propre compteur → plafond effectif × N instances.
- **Distribué (Redis)** : nécessaire dès deux instances. Bucket4j supporte Redis nativement.
- Pour ce projet : démarrer en in-memory (mono-instance), prévoir Redis si scale-out.

**Précaution importante** : derrière un load balancer ou un reverse proxy, l'IP source vue par l'app est l'IP du proxy, pas du client. Il faut configurer le proxy pour envoyer `X-Forwarded-For` **et** dire à Spring de la respecter (`server.forward-headers-strategy=native`). Sinon le rate-limit est inutile (toutes les requêtes ont la même IP = celle du LB).

### Implémentation prévue (Lot 16)

Bibliothèque : **Bucket4j** (`com.bucket4j:bucket4j-core`), in-memory pour démarrer.

```java
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();

    private Bucket resolveBucket(String ip) {
        return buckets.computeIfAbsent(ip, k -> Bucket.builder()
            .addLimit(Bandwidth.simple(5, Duration.ofMinutes(1)))
            .build());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain) {
        if (!req.getRequestURI().startsWith("/api/auth/")) {
            chain.doFilter(req, res); return;
        }
        Bucket bucket = resolveBucket(req.getRemoteAddr());
        if (bucket.tryConsume(1)) {
            chain.doFilter(req, res);
        } else {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
        }
    }
}
```

Filtre placé **avant** `JwtAuthFilter` dans la chaîne.

### Comment vérifier
- Boucle de 6 `curl -X POST /api/auth/login` depuis la même IP → la 6e renvoie `429 Too Many Requests`.
- Attendre 60s, ré-essayer → passe.
- Log applicatif contient une trace `Rate limit exceeded for IP <addr>` (à ajouter pour traçabilité).

---

## 4. Contrôle d'upload CSV

### Menace
`ProductController.importCsv` accepte actuellement un `MultipartFile` sans **aucune** contrainte. Risques :

1. **DoS par taille** : envoi d'un fichier de plusieurs Go. Spring le buffer en mémoire ou sur disque temporaire, sature les ressources serveur.
2. **DoS algorithmique (Billion Laughs / Zip Bomb)** : un fichier `text/csv` de 1 MB peut contenir des millions de lignes après expansion (peu de risque sur CSV pur, plus sur Excel/XML, mais à garder en tête si on ajoute le support `.xlsx`).
3. **Path traversal via nom de fichier** : si tu utilises `file.getOriginalFilename()` pour écrire quelque part, un nom `../../etc/passwd` peut sortir du dossier prévu. (Pas notre cas : on lit en mémoire, on ne sauve pas.)
4. **MIME spoofing** : un attaquant uploade un `.exe` renommé `.csv`. Si l'app le traite en CSV brut, peu de risque. Si elle le sert plus tard sans contrôle, risque XSS / drive-by download.
5. **CSV injection (Formula Injection)** : une cellule `=cmd|'/C calc'!A0` est inerte comme texte mais devient une **formule exécutée** quand l'utilisateur ouvre le CSV dans Excel/LibreOffice. Pertinent si on **exporte** du CSV — moins si on **importe** uniquement.

### Règles de l'art

**Au niveau Spring (déclaratif)** :
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 2MB
      enabled: true
```
Au-delà de 2 MB → 413 Payload Too Large automatique, sans toucher au contrôleur.

**Au niveau contrôleur (validation explicite)** :
1. **Vérifier l'extension** : `.csv` uniquement (allowlist, pas blocklist).
2. **Vérifier le Content-Type** : `text/csv` ou `application/vnd.ms-excel` (Excel envoie parfois ce dernier pour un CSV).
3. **Magic bytes / probing** : lire les premiers octets, refuser si binaire (présence de `\x00` dans les premiers 512 bytes, par exemple).
4. **Limiter le nombre de lignes** : `max 10000 lignes` par exemple, refus au-delà.
5. **Encodage** : forcer UTF-8 (avec ou sans BOM), refuser proprement les autres.
6. Pour l'**export** CSV, échapper les cellules qui commencent par `=`, `+`, `-`, `@`, `\t`, `\r` en les préfixant d'une apostrophe `'` (anti-formula-injection).

**Antivirus** (production sérieuse) : scan ClamAV via `clamd` avant traitement. Non prioritaire ici (CSV pur, on ne stocke pas le fichier).

### Implémentation prévue (Lot 16)

**Config** :
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 2MB
      max-request-size: 2MB

app:
  csv:
    import:
      max-rows: 10000
      allowed-content-types:
        - text/csv
        - application/vnd.ms-excel
```

**Validation côté service** :
```java
private void validateUpload(MultipartFile file) {
    if (file.isEmpty()) throw new IllegalArgumentException("Empty file");

    String filename = file.getOriginalFilename();
    if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
        throw new IllegalArgumentException("Only .csv files are accepted");
    }

    if (!properties.allowedContentTypes().contains(file.getContentType())) {
        throw new IllegalArgumentException("Invalid content type: " + file.getContentType());
    }

    // Spring a déjà rejeté > 2MB ; ici on plafonne aussi le nombre de lignes
    // pendant le parsing en comptant dans la boucle existante.
}
```

**Mapping** : `MaxUploadSizeExceededException` → 413 dans `GlobalExceptionHandler`.

### Comment vérifier
- `curl -F "file=@huge.csv" /api/products/import` avec un fichier de 5 MB → 413.
- `curl -F "file=@malware.exe" /api/products/import` (renommé `.csv`) → 400 (content-type rejeté ou extension manquante).
- CSV valide de 10001 lignes → 400 avec message `Too many rows`.

---

## 5. Gestion des utilisateurs et mots de passe — règles de l'art

### Stockage des mots de passe

**Principes immuables** :
- ❌ **Jamais en clair**.
- ❌ **Jamais hashé avec MD5, SHA-1, SHA-256 seul, ou tout hash rapide non salé**. Ces algorithmes sont conçus pour être rapides, ce qui les rend triviaux à brute-forcer (des milliards de hashes/seconde sur un GPU moderne).
- ❌ **Jamais chiffrés réversiblement** (AES, etc.). Un mot de passe ne doit pas pouvoir être déchiffré — même par l'admin.
- ✅ **Hashé avec une fonction lente et salée** spécialement conçue pour les mots de passe :
  - **BCrypt** (utilisé ici) — éprouvé depuis 1999, cost factor ajustable.
  - **Argon2id** — recommandé par OWASP depuis 2021, meilleur que BCrypt sur les attaques GPU et side-channel. Disponible via `spring-security-crypto` à partir de Spring Security 6.
  - **scrypt** — alternative valide, moins courante.
  - **PBKDF2** — acceptable mais plus lent à configurer correctement.

**Paramètres recommandés** :
- BCrypt : **cost factor 12** minimum en 2026 (chaque +1 double le temps de calcul). 12 ≈ 250ms sur un CPU moderne, bon compromis sécurité/UX.
- Argon2id : `memoryCost=19456 KB (19 MiB)`, `iterations=2`, `parallelism=1` (recommandation OWASP).

**Salt** : géré automatiquement par BCrypt et Argon2id, chaque hash inclut son salt. Pas besoin de stocker un salt séparé.

**État actuel du projet** : `BCryptPasswordEncoder` sans paramètre = cost factor **10** (défaut). Acceptable, à monter à 12 dans le lot 16.

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
}
```

**Rotation des hashes** : si on monte le cost factor un jour, on ne peut pas re-hasher les mots de passe existants (on n'a pas le mot de passe en clair). Solution : re-hasher **au prochain login réussi** de chaque utilisateur (détecter le cost factor du hash stocké, comparer au cible, regénérer si différent). Pattern classique avec `DelegatingPasswordEncoder` de Spring Security.

### Politique de mots de passe

**Critères modernes (NIST SP 800-63B, OWASP)** — qui contredisent souvent les "vieilles règles" :

| Critère | Recommandation actuelle |
|---|---|
| **Longueur minimum** | **12 caractères** (8 est obsolète depuis 2017) |
| **Longueur maximum** | Au moins 64, idéalement 128. Limiter à 16 = anti-pattern. |
| **Complexité (majuscule/chiffre/symbole obligatoires)** | **Plus recommandé**. Force les utilisateurs à `P@ssword1` au lieu de `correct horse battery staple`. |
| **Expiration périodique** | **Plus recommandée** sauf si on suspecte une compromission. Encourage la réutilisation et les variations triviales. |
| **Interdiction des mots de passe communs** | **Indispensable**. Bloquer top-10k (liste `rockyou`, `SecLists`). |
| **Vérification de breach** | **Recommandée**. API Have I Been Pwned (k-anonymity, on n'envoie pas le mot de passe complet) → refuser si déjà leaké. |
| **MFA / 2FA** | **Indispensable** pour les comptes admin. TOTP (Google Authenticator) ou WebAuthn. |

**État actuel** : `RegisterRequest` impose `min = 6`. Trop faible. Lot 16 : passer à `min = 12`.

### Authentification

**Bonnes pratiques** :
- **Messages d'erreur unifiés** : "Invalid email or password" — jamais "User not found" vs "Wrong password". Empêche l'énumération des comptes.
  - ✅ Le projet fait déjà ça dans `AuthServiceImpl`.
- **Délai constant** : ne pas court-circuiter la comparaison BCrypt quand le user n'existe pas (sinon le temps de réponse différent leake l'info). Idéalement, faire un `passwordEncoder.matches("dummy", DUMMY_HASH)` même si le user est absent.
- **Rate-limit** (cf. §3).
- **Lockout** après N échecs : optionnel, à manier avec précaution (un attaquant peut DoS un compte en tentant volontairement de mauvais mots de passe pour le bloquer). Préférer le rate-limit par IP.

### Gestion de session (JWT)

**Choix actuel : JWT stateless** — bon choix pour une API REST séparée du front.

**Limites à connaître** :
- **Pas de revocation native**. Un token valide jusqu'à expiration, même après déconnexion. Mitigations :
  - Durée de vie courte (notre cas : 1h pour access token — bien).
  - Refresh token côté serveur (en DB, hashé, révocable) — c'est le pattern du lot 16.
  - Liste de révocation (blacklist) si besoin de "logout immédiat" — coûteux, à éviter sauf besoin réel.
- **Stockage côté client** : `httpOnly` cookie > localStorage pour résister à XSS. Le front Angular doit utiliser un cookie ou un header in-memory (jamais `localStorage` pour le refresh token).
- **Algorithme de signature** : `HS256` (HMAC-SHA256) — notre choix actuel via `Keys.hmacShaKeyFor`. Bien si la clé est ≥ 256 bits. Pour multi-services, préférer `RS256` (asymétrique, le verifier n'a que la clé publique).
- **Claims minimaux** : sub, iat, exp, iss. Pas de données sensibles (le payload JWT est **lisible** par n'importe qui — c'est juste du Base64).

### Permissions et rôles

**Pattern recommandé** :
- **RBAC (Role-Based Access Control)** suffisant pour la plupart des SaaS B2B → c'est ce que prévoit le lot 15.
- **ABAC (Attribute-Based)** si la logique est plus fine (`peut modifier ce devis si client.region == user.region`). Plus complexe, pas nécessaire ici.
- **Permissions** (granularité fine type `quote:read`, `quote:write`) plutôt que des rôles monolithiques — utile à terme si on ajoute des rôles `ACCOUNTANT`, `SALES`, etc.

**Principe du moindre privilège** : un user n'a accès qu'à ce qui est strictement nécessaire à son rôle. C'est l'objectif du **lot 15** (ownership) + restrictions admin.

### Audit et logs

- **Toujours logger** : login réussi, login échoué, changement de mot de passe, création/suppression de compte, accès refusé (403).
- **Jamais logger** : mot de passe (même hashé), token JWT complet, secret, contenu de body sensible.
- Format **structuré** (JSON) pour exploitation par un SIEM ultérieurement.

Spring Security expose des `AuthenticationSuccessEvent` / `AuthenticationFailureEvent` qu'on peut écouter pour centraliser ce logging sans polluer la logique métier.

### Récupération de mot de passe (futur)

Pas dans le périmètre actuel, mais à prévoir :
- Token de reset à usage unique, **expiration courte** (15-30 min).
- Envoi par email, **lien** (pas de mot de passe temporaire en clair dans le mail).
- Invalidation des sessions existantes après reset.
- Même message UI que le user existe ou non (anti-énumération).

---

## Récapitulatif des actions par lot

| Mesure | Lot | État | Référence section |
|---|---|---|---|
| Fail-fast sur secrets, `.env.example`, `@Validated` | 16 | ✅ | §1 |
| `CorsConfigurationSource` câblé et durci | 16 | ✅ | §2 |
| Bucket4j rate-limit `/api/auth/**` | 16 | ✅ | §3 |
| Limites multipart + validation CSV | 16 | ✅ | §4 |
| BCrypt cost factor 12, password min 12 | 16 | ✅ | §5 |
| 401 propre dans `JwtAuthFilter` | 16 | ✅ | §5 (session) |
| Refresh token endpoint | 16 | ✅ | §5 (session) |
| Identité société externalisée (`app.company.*` via env) | 16 | ✅ | §1 |
| Nettoyage `SecurityConfig` (`/actuator/**` retiré) | 16 | ✅ | §2 |
| Identifiant canonique JWT aligné sur `email` | 16 | ✅ | §5 (session) |
| RBAC + Ownership (`@PreAuthorize`, scoping) | 15 | ✅ | §5 (permissions) |

### Écarts entre le plan initial et l'implémentation

- **Rate-limit configurable, pas figé en dur** : `RateLimitFilter` lit `app.rate-limit.auth.capacity` / `.window-seconds` via `@Value` (défauts 5/60s) plutôt qu'une constante `Bandwidth.simple(5, Duration.ofMinutes(1))` codée en dur. Raison : `@ConfigurationProperties` n'est pas résolu dans les contextes `@WebMvcTest` (slices), qui auto-détectent pourtant les beans `Filter` — un `RateLimitFilter` dépendant d'un bean `@ConfigurationProperties` casse ces slices avec `NoSuchBeanDefinitionException`. `@Value` avec valeur par défaut reste résolu dans tous les contextes. Le profil `integration-test` relève la capacité à 1000/min pour que les tests d'intégration partageant un contexte Spring en cache ne se marchent pas dessus.
- **Refresh token avec rotation** : chaque appel à `/api/auth/refresh` révoque le token présenté et en émet un nouveau (à usage unique), au-delà du minimum "stocké en DB, hashé, révocable" du plan initial. Stockage : SHA-256 hex du token brut (aléatoire, 32 octets, base64url) en colonne `token_hash` unique — pas de salt nécessaire vu l'entropie de la source.
- **Vérification HIBP non implémentée** (§5, marquée optionnelle dans le plan) — reste hors périmètre du lot 16, à réévaluer si le besoin se confirme.

---

## Sources

- [OWASP Authentication Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Authentication_Cheat_Sheet.html)
- [OWASP Password Storage Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html)
- [OWASP File Upload Cheat Sheet](https://cheatsheetseries.owasp.org/cheatsheets/File_Upload_Cheat_Sheet.html)
- [NIST SP 800-63B — Digital Identity Guidelines](https://pages.nist.gov/800-63-3/sp800-63b.html)
- [Spring Security Reference](https://docs.spring.io/spring-security/reference/index.html)
- [Bucket4j documentation](https://bucket4j.com/)
