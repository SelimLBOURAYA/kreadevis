# kreadevis-backend

> Conventions transversales (workflow par lot, commits, branches, PRs, patterns interdits, ask-before-doing, secrets, architecture REST, rich-domain) sont chargées depuis `~/.claude/coding-conventions.md`. Ce fichier ne contient **que ce qui est spécifique à kreadevis-backend**.

## Projet
Application de création de devis — migration de l'ancien `kreadevis` (Spring Boot 3 MVC + JSP) vers une API REST pure consommée par un frontend Angular séparé.

## Stack
- **Backend** : Spring Boot 4, Java 25
- **Frontend** : Angular 21 (projet séparé, à faire après backend stable)
- **Database** : PostgreSQL 17 (Docker)
- **Build** : Maven
- **Mapping** : MapStruct
- **Auth** : Spring Security + JWT (lot 3, implémenté)
- **PDF / CSV** : OpenPDF, Apache Commons CSV

## Validation gate
```
./mvnw verify
```

## Langue
- Code, commentaires, identifiants : **anglais**
- Messages de commit : **anglais**
- Échanges et docs internes (`lots.md`, PR descriptions) : **français accepté**

## Règles spécifiques au projet
- **Legacy banni** : aucune trace de la couche vue de l'ancien projet (JSP, JSTL, Servlets, Webjars). Si tu vois encore un import suspect, c'est un bug de migration à traiter dans un lot dédié.
- **Traduction FR → EN en cours** sur le code legacy : entités déjà traduites (`Devis→Quote`, `Reservation→QuoteItem`, etc.). Continuer la traduction au fil des lots, jamais comme tâche séparée.

## Contexte migration
- Ancien projet : `/home/selim/ENV/projets/kreadevis/` (quasi fonctionnel, blocage principal sur Spring Security)
- Couche vue (JSP, JSTL, Servlets, Webjars) à abandonner complètement
- Format de référence des devis : `DDMMYY-NNN` avec séquence quotidienne

## Secrets / environnement
| Variable | Lot | Usage |
|---|---|---|
| `POSTGRES_PASSWORD` | 1 | Mot de passe DB |
| `JWT_SECRET` | 3 | Clé de signature JWT |
| `MAILJET_API_KEY` | 10 | Clé API Mailjet (relances mail) |
| `MAILJET_API_SECRET` | 10 | Secret API Mailjet |
