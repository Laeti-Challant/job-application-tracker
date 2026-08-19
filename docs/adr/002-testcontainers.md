# ADR 002 : Testcontainers pour les tests d'intégration

---

## Statut

**Accepté**

Date : 2026-08-19 (décision prise le 2026-08-17, formalisée après coup)

## Contexte

La constitution du projet impose qu'aucun code métier ne soit écrit avant le test qui le décrit.
Une partie de ces tests touchera la couche `repository`, donc du vrai SQL : requêtes dérivées de
Spring Data, contraintes d'unicité, comportement des types de colonnes.

La base de données visée en production est PostgreSQL 18.

La phase 4 du projet prévoit une CI qui lance la suite de tests automatiquement. Les tests doivent
donc pouvoir démarrer sans aucune intervention manuelle, sur une machine qui n'est pas celle de
développement.

Aucun test automatisé n'a jamais été écrit ni exécuté auparavant : le dispositif retenu doit rester
compréhensible et ne pas ajouter une couche d'outillage à déboguer en plus du code testé.

## Options envisagées

### Option A : H2 en mémoire

Une base légère démarrée dans le processus de test, configurée en mode de compatibilité
PostgreSQL.

- Pour : très rapide, aucune dépendance externe, aucune installation. C'est la solution proposée
  par défaut dans la majorité des tutoriels Spring Boot, donc la mieux documentée.
- Contre : H2 n'est pas PostgreSQL. Son mode de compatibilité couvre la syntaxe courante mais
  diverge sur les types spécifiques, certaines fonctions de dates, la gestion des séquences et le
  détail des messages d'erreur de contrainte. Un test peut passer au vert sur une requête qui
  échouera en production, ce qui est le pire résultat possible : une suite de tests qui rassure à
  tort.

### Option B : un PostgreSQL local démarré à la main

Une instance PostgreSQL installée sur le poste, lancée avant de jouer les tests.

- Pour : c'est le vrai moteur, donc aucune divergence de dialecte. Aucun outillage supplémentaire
  à apprendre.
- Contre : les tests ne sont plus autonomes. Il faut penser à démarrer la base avant, et se
  souvenir de sa configuration. La même base de test serait alors décrite à deux endroits sans
  lien entre eux : sur le poste de développement d'un côté, dans un service déclaré au niveau de
  la CI de l'autre. Rien ne garantit que les deux restent alignées. Enfin, la base garde son état
  d'une exécution à l'autre : un test qui a laissé des données derrière lui peut faire passer ou
  échouer le suivant sans qu'aucune ligne de code n'ait changé.

### Option C : Testcontainers

Une bibliothèque de test qui démarre un vrai PostgreSQL dans un conteneur Docker jetable, au
lancement de la suite, et le détruit à la fin.

- Pour : c'est le vrai moteur, dans la même version qu'en production, et le cycle de vie est piloté
  par le code de test. Aucune étape manuelle, la même commande fonctionne sur le poste et sur la
  CI. Chaque exécution repart d'une base neuve, donc aucun état résiduel.
- Contre : Docker devient obligatoire pour lancer les tests, et le démarrage du conteneur ajoute
  plusieurs secondes à chaque exécution.

## Décision

Les tests d'intégration s'exécutent contre un PostgreSQL réel démarré par Testcontainers, sur une
image explicitement versionnée (`postgres:18-alpine`).

**Pourquoi celle-là :** c'est la seule option qui réunit les deux exigences du projet. La fidélité
à la production, parce qu'un test qui ment est pire que pas de test. Et l'autonomie d'exécution,
parce que la phase 4 exige que la CI lance la suite sans qu'une main humaine prépare quoi que ce
soit.

**Pourquoi pas les autres :**

H2 est écarté sur un critère unique et non négociable : il peut valider une requête que PostgreSQL
refusera. Sa rapidité et sa simplicité ne compensent pas ce risque, d'autant que ces divergences
se manifestent précisément sur les requêtes non triviales, celles qu'on écrit un test pour
vérifier. Sur un projet dont l'enjeu est d'apprendre à faire confiance à ses tests, une suite qui
peut mentir n'a pas d'intérêt.

Le PostgreSQL local est écarté non pour sa fidélité, qui est totale, mais pour son étape manuelle.
Toute étape manuelle finit par être oubliée, et surtout elle ne se transpose pas telle quelle en
CI : la même base de test devrait être décrite deux fois, sur le poste et au niveau de la CI, avec
la certitude que les deux descriptions divergeront un jour. L'état persistant d'une exécution à
l'autre pose un second problème, plus insidieux : un test qui échoue sans que le code ait changé
est le meilleur moyen de perdre confiance dans toute la suite.

On pourrait objecter que `spring.jpa.hibernate.ddl-auto=create-drop` supprimerait cet état
résiduel. C'est exact, mais partiel : cette option remet le schéma à zéro entre deux exécutions de
la suite, pas entre deux tests d'une même exécution. Elle ne répond ni à l'étape manuelle, ni à la
double description de la base. L'isolation entre tests reste de toute façon un sujet distinct, à
traiter par transaction ou nettoyage explicite, et il se posera à l'identique avec Testcontainers.

## Conséquences

Les tests sont plus lents qu'avec une base en mémoire. Mesures relevées sur le squelette du
projet : 24 secondes au premier build, téléchargement de l'image compris, puis 7,9 secondes une
fois l'image en cache local. En CI, l'image sera à retélécharger à chaque exécution tant qu'aucun
cache n'est configuré, donc l'ordre de grandeur y sera celui du premier chiffre.

Ce coût est le prix direct de la fidélité, et il se paiera à chaque boucle du cycle TDD.

Docker devient une dépendance obligatoire du projet. Un poste sans Docker ne peut pas lancer la
suite de tests, et l'environnement de CI devra pouvoir démarrer des conteneurs.

L'image doit rester versionnée explicitement. Utiliser `postgres:latest` ferait changer le moteur
de base sous les tests à la sortie d'une version majeure, sans qu'une seule ligne du dépôt n'ait
bougé. Le passage à PostgreSQL 19 devra être un changement volontaire et visible dans un diff.

Testcontainers est une dépendance de test supplémentaire, avec ses propres conventions à
connaître. Sous Spring Boot 4, ses artefacts ont été renommés (`testcontainers-junit-jupiter`,
`testcontainers-postgresql`, package `org.testcontainers.postgresql`), ce qui rend la plupart des
exemples trouvés en ligne inutilisables tels quels.

Cette décision serait à revoir si le temps d'exécution devenait un frein réel au rythme du
développement, ce qui n'est pas le cas à l'échelle actuelle du projet.
