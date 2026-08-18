# CLAUDE.md

Constitution du projet. Chargée au début de chaque session.

Ce fichier ne contient que des **règles permanentes**. Les tâches en cours vivent dans
`docs/tasks.md`, jamais ici.

---

## Ce qu'est ce projet

API REST de suivi de candidatures, à usage personnel et mono-utilisateur.

C'est aussi un projet d'apprentissage du développement piloté par spec : la conception, les
décisions d'architecture et la revue de code sont de Laetitia. Claude exécute des tâches bornées,
définies à l'avance dans `docs/tasks.md`.

## Stack

| Élément | Choix | Version |
|---|---|---|
| Langage | Java | 21 (LTS) |
| Framework | Spring Boot | 4.1.0 |
| Build | Maven via wrapper `./mvnw` | wrapper versionné, ne jamais exiger un Maven global |
| Base de données | PostgreSQL | à conteneuriser en phase DevOps |
| Tests | JUnit 5 + AssertJ | starters de test modulaires de Boot 4 |

Spring Boot 4 diffère de Boot 3 sur deux points qui invalident la plupart des tutoriels en ligne :
`spring-boot-starter-web` s'appelle désormais `spring-boot-starter-webmvc`, et le starter de test
unique a été éclaté en un starter de test par brique (`-webmvc-test`, `-data-jpa-test`, etc.).
Vérifier la version avant de reprendre un exemple trouvé sur le web.

## Architecture

Architecture en couches, un package par couche sous `io.github.laetichallant.jobtracker` :

```
controller/   expose le HTTP, ne contient aucune règle métier
service/      porte les règles métier, seul endroit qui orchestre
repository/   accès aux données, interfaces Spring Data
domain/       entités JPA et objets métier
dto/          objets d'entrée et de sortie de l'API
```

Règles de dépendance, à respecter sans exception :

- `controller` appelle `service`, jamais `repository` directement
- `service` appelle `repository`
- Une entité JPA ne sort **jamais** d'un controller. Toujours passer par un DTO
- Aucune annotation Spring dans `domain/`, hors annotations JPA

Formes attendues par couche :

- `dto/` : des **records** Java. Immuables par construction, sans setter possible,
  `equals`/`hashCode`/`toString` générés par le compilateur
- `domain/` : des **classes** annotées JPA. Un record ne peut pas être une entité, Hibernate exige
  un constructeur sans argument et l'écriture des champs après construction

Justification à formaliser dans `docs/adr/001-architecture-en-couches.md`.

## Règle de tests

**Aucun code métier n'est écrit avant le test qui le décrit.**

Ordre imposé pour chaque tâche :

1. Écrire le test depuis le critère d'acceptation de la spec
2. Le lancer et vérifier qu'il échoue **pour la bonne raison**
3. Écrire le code minimal qui le fait passer
4. Relire, commiter

Un test dont on n'a jamais vu l'échec ne prouve rien.

## Conventions de code

- Code, noms de classes, noms de variables et commentaires : **en anglais**
- Documentation fonctionnelle (`docs/`) et échanges : **en français**
- Pas d'abréviation dans les noms (`application`, pas `app` ; `candidate`, pas `cand`)
- Une classe par fichier, nommée comme le fichier

### Accesseurs et construction

- Pas de champ mutable public
- **Constructeur plutôt que setters** : un objet naît valide ou ne naît pas. Un objet créé vide
  puis rempli par une suite de setters traverse une période où son état est incohérent, et un
  setter oublié ne produit aucune erreur
- **Entités** : un constructeur `protected` sans argument pour Hibernate, un constructeur public
  prenant les champs obligatoires, des getters, et **aucun setter**. Une transition d'état passe
  par une méthode qui la nomme et porte sa règle (`markAsRejected()`, pas `setStatus()`)
- **Services et controllers** : un seul constructeur, pour l'injection des dépendances, champs
  `private final`, **ni getter ni setter**. Une dépendance n'est pas une donnée à exposer, et un
  `getRepository()` sur un service permettrait à un controller de court-circuiter la couche métier
- **`equals` / `hashCode`** : générés d'office sur les records. **Non redéfinis sur les entités**
  tant qu'aucun besoin réel ne l'impose (entité placée dans un `Set`, relation `@OneToMany`
  en `Set`). Le jour où ce besoin arrive, il fait l'objet d'un ADR, pas d'une improvisation :
  une clé basée sur l'`id` change de valeur au moment de la persistance, une clé basée sur tous
  les champs touche les relations en lazy
- **Lombok n'est pas une dépendance du projet.** Java 21 couvre le besoin avec les records. Si
  Lombok est ajouté un jour, `@Data` et `@EqualsAndHashCode` restent interdits sur une entité

## Git

Modèle : **GitHub Flow**. `main` + branches courtes + pull request. Pas de branche `develop` : le
projet est mono-développeur et n'a pas de versions à maintenir en parallèle, une branche tampon
n'y ajouterait qu'un merge sans contenu. Décision à formaliser dans
`docs/adr/003-github-flow.md`.

- `main` est toujours déployable, et toujours vert
- Branches : `feature/<sujet>`, `fix/<sujet>`, `chore/<sujet>`, `docs/<sujet>`. Jamais de commit
  direct sur `main`
- Toute branche revient dans `main` par une **pull request**, jamais par un merge local
- Messages de commit : **Conventional Commits, en anglais**
  (`feat:`, `fix:`, `test:`, `docs:`, `chore:`, `refactor:`, `ci:`)
- Un commit = une tâche terminée = des tests verts
- **Aucune mention de Claude ni de co-auteur IA dans les messages de commit**

### Découpage des pull requests

Une PR est une **tranche verticale** : un seul cas d'usage, traversant toutes les couches
concernées, avec ses tests.

```
feat: create application     domain + repository + service + POST + tests
feat: list applications      GET + tests
feat: update status          transition d'état + tests
```

Pas de découpage horizontal (toutes les entités, puis tous les services, puis tous les
controllers) : rien ne fonctionne avant la dernière PR, et le modèle de données est alors conçu
sans qu'aucun usage réel ne l'ait traversé.

La première tranche est la plus grosse, puisqu'elle crée les packages vides. C'est normal et ça ne
se reproduit pas.

## Où vivent les artefacts

| Fichier | Contenu |
|---|---|
| `docs/spec-fonctionnelle.md` | Le quoi et le pourquoi. Aucun code |
| `docs/adr/NNN-titre.md` | Une décision d'architecture, avec les options écartées |
| `docs/tasks.md` | Les tâches, avec leur critère de « fini » |
| `CLAUDE.md` | Ce fichier. Les règles permanentes |

## Ce que Claude ne doit pas faire

- Implémenter une fonctionnalité absente de `docs/spec-fonctionnelle.md`. Si le besoin est réel,
  proposer d'abord une modification de la spec, et attendre la validation
- Traiter plusieurs tâches de `docs/tasks.md` dans un même passage
- Écrire du code de production avant son test
- Ajouter une dépendance sans le dire explicitement et sans justifier pourquoi
- Modifier `docs/adr/` : un ADR accepté est figé. Le faire évoluer signifie en écrire un nouveau
  qui remplace le précédent
- Écrire un secret, un mot de passe ou une chaîne de connexion en dur dans le dépôt
- Reformuler ou « améliorer » spontanément la spec en la recopiant dans le code

## Définition de « fini »

Une tâche est terminée quand, et seulement quand :

- [ ] Son critère d'acceptation est vérifié par un test automatisé
- [ ] `./mvnw test` est vert
- [ ] Laetitia peut expliquer chaque ligne ajoutée
- [ ] Le commit respecte la convention ci-dessus
