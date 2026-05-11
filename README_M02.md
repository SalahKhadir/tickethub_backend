# Module 2 - Ticketing Engine (Backend)

Ce document presente un etat professionnel et general du Module 2 (M02) du backend TicketHub.
Il a pour objectif de fournir une vue claire pour les equipes produit, developpement et QA.

## Apercu

Le Module 2 introduit le coeur du ticketing avec une API REST securisee par JWT.
Les capacites livrees couvrent la creation de tickets, la consultation selon le role utilisateur,
et la mise a jour du statut par les roles operationnels.

## Portee du module

Le module adresse les besoins suivants:
- gestion des tickets (creation et consultation),
- modelisation du cycle de vie d'un ticket (statut/priorite/categorie),
- controle d'acces base sur les roles,
- persistence relationnelle et migration schema.

## Architecture (vue generale)

Le module suit une architecture en couches:
- **API Layer**: controllers REST exposes sous `/api/tickets`,
- **Service Layer**: orchestration metier et regles de securite applicative,
- **Data Layer**: entite `Ticket`, repository JPA, migration Flyway V2,
- **Security Layer**: authentification JWT et autorisation par roles (`CLIENT`, `TECH`, `ADMIN`).

## Capacites livrees

### 1) Donnees et persistence
- Entite `Ticket` avec relation auteur vers `User`.
- Enums metier pour statut, priorite et categorie.
- Migration Flyway `V2__create_tickets_table.sql` avec cle etrangere et indexes.

### 2) API et contrats
- DTOs modernises en Java records (`TicketRequest`, `TicketResponse`, `TicketStatusUpdateRequest`).
- Validation d'entree appliquee sur les champs obligatoires.
- Reponses concues pour ne pas exposer d'information sensible utilisateur.

### 3) Logique metier
- Creation d'un ticket avec association automatique a l'utilisateur authentifie.
- Lecture des tickets avec filtrage intelligent par role:
  - `CLIENT`: uniquement ses tickets,
  - `TECH`/`ADMIN`: ensemble des tickets.
- Mise a jour de statut d'un ticket reservee a `TECH`/`ADMIN`.

### 4) Securite et robustesse
- Autorisation appliquee via `@PreAuthorize` sur les endpoints critiques.
- Mapping des roles normalise pour eviter les incoherences de type `ROLE_ROLE_*`.
- Gestion centralisee des exceptions (`400`, `403`, `404`, `500`).

## Endpoints actuellement disponibles

- `POST /api/auth/register`
- `POST /api/auth/login`
- `POST /api/tickets`
- `GET /api/tickets`
- `PATCH /api/tickets/{id}/status`

## Qualite et validation

Etat de validation actuel:
- build Maven et tests executes avec Java 21,
- verification fonctionnelle des flux JWT principaux,
- verification des regles d'acces (ex: un client ne peut pas modifier le statut).

## Ce qu'il reste pour cloturer M02

### Priorite fonctionnelle
- completer le CRUD ticket (`GET by id`, update contenu, delete),
- formaliser les transitions de statut autorisees,
- ajouter pagination, tri et filtres de recherche.

### Priorite qualite et exploitation
- renforcer les tests d'integration par role,
- publier une documentation API (OpenAPI/Swagger),
- consolider la strategie de configuration (Flyway comme source unique schema, secrets via variables d'environnement).

## Roadmap recommandee

1. **Phase 1 - Completion API**
   - endpoints manquants et regles d'acces fines.
2. **Phase 2 - Workflow metier**
   - transitions de statut et validations metier avancees.
3. **Phase 3 - Industrialisation**
   - couverture de tests, documentation, stabilisation ops.

## Resume executif

M02 est fonctionnel sur le coeur ticketing et deja exploitable pour un usage initial.
La suite du travail porte principalement sur la completion du cycle CRUD,
la formalisation complete du workflow metier et le renforcement qualite/documentation.

