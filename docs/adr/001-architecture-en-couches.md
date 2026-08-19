# ADR 001 : architecture en couches

---

## Statut

**Accepté**

Date : 2026-08-19 (décision prise le 2026-08-17, formalisée après coup)

## Contexte

Le projet est une API REST de suivi de candidatures, à usage personnel et mono-utilisateur. Le
domaine métier est simple : des candidatures, des entreprises, des statuts et des dates qui
évoluent. Aucune règle de calcul complexe, et aucune règle de cohérence qui engagerait plusieurs
objets à la fois. Chaque candidature est indépendante des autres.

Le périmètre doit être terminé avant le CCP déploiement d'octobre 2026, et le projet est mené en
solo.

L'objectif principal n'est pas d'apprendre une architecture, il est d'apprendre le développement
piloté par spec, les tests automatisés et le DevOps. L'architecture est ici un support, pas le
sujet.

Compétence disponible au moment de décider : l'architecture en couches est connue et pratiquée
(formation ENI, titre CDA). Aucune autre architecture applicative n'a été pratiquée.

## Options envisagées

### Option A : architecture en couches

Un package par responsabilité : `controller`, `service`, `repository`, `domain`, `dto`. Les
dépendances vont dans un seul sens, du HTTP vers la base.

- Pour : déjà maîtrisée, donc aucun coût d'apprentissage prélevé sur le temps disponible. C'est
  la structure attendue par défaut dans l'écosystème Spring, donc la documentation et les exemples
  s'appliquent directement. Elle sépare assez le métier de l'infrastructure pour rendre la couche
  service testable seule, ce qui est la condition d'existence de la règle TDD du projet.
- Contre : le domaine dépend de JPA. Les entités portent des annotations de persistance, donc la
  couche métier n'est pas totalement indépendante de la technologie de stockage. Changer de mode
  de persistance imposerait de retoucher `domain/`.

### Option B : architecture hexagonale (ports et adaptateurs)

> Cette option n'était pas connue au moment de décider, le 2026-08-17 : le choix s'est fait en
> pratique entre l'option A et l'option C. Elle est documentée ici parce qu'elle est la réponse
> classique au défaut de l'option A, et qu'elle sera la première à réexaminer le jour où cet ADR
> devra être revu.

Le domaine est au centre, sans aucune dépendance technique. Il définit des interfaces (les ports)
que des adaptateurs implémentent en périphérie, côté HTTP comme côté base de données. La
dépendance est inversée : l'infrastructure dépend du domaine, jamais l'inverse.

- Pour : le domaine devient totalement indépendant de Spring et de JPA, testable sans aucune
  infrastructure. Changer de base de données ou exposer la même logique par un autre canal ne
  touche pas le métier. C'est l'architecture qui vieillit le mieux quand le domaine grossit.
- Contre : elle coûte cher en volume de code sur un domaine simple. Chaque cas d'usage demande un
  port, un adaptateur, et un objet de domaine distinct de l'entité de persistance, avec le mapping
  entre les deux. Elle ne supprime pas le code de persistance, qui reste à réécrire entièrement en
  cas de changement de moteur : elle garantit seulement que ce changement ne touche pas le domaine.
  Elle n'a jamais été pratiquée, donc son apprentissage entrerait en concurrence directe avec les
  objectifs réels du projet.

### Option C : aucune couche, tout dans le controller

Le controller reçoit la requête, interroge le repository et répond.

- Pour : le moins de code possible, et une lecture immédiate pour un CRUD trivial.
- Contre : la logique métier se retrouve mélangée au code HTTP, donc elle ne peut être testée
  qu'en démarrant un serveur web. La règle « aucun code métier avant son test » deviendrait très
  coûteuse à respecter. Toute règle partagée entre deux endpoints serait dupliquée.

## Décision

Le projet adopte une architecture en couches : `controller`, `service`, `repository`, `domain`,
`dto`, avec des dépendances à sens unique et une entité JPA qui ne sort jamais d'un controller.

**Pourquoi celle-là :** c'est la seule des trois qui soit à la fois déjà maîtrisée et suffisante
pour rendre le métier testable en isolation. Elle ne prélève rien sur le temps et l'attention que
le projet doit consacrer à la spec, aux tests et au DevOps.

**Pourquoi pas les autres :**

L'hexagonale est écartée pour son coût, pas pour ses défauts. Ses bénéfices sont réels, mais ils
se paient sur un domaine complexe, et celui-ci ne l'est pas : il n'y a rien à protéger derrière un
port. L'apprendre en même temps que la spec, les tests et la CI reviendrait à mener deux
apprentissages de front dans un projet contraint par une échéance d'examen. Elle n'était de toute
façon pas dans le champ des options connues au moment de décider.

L'absence de couche est écartée parce qu'elle est incompatible avec la règle de tests du projet.
Un métier écrit dans un controller ne se teste qu'à travers HTTP, ce qui rend chaque test lent et
indirect. Ce n'est pas une question de style, c'est une contradiction avec la constitution.

## Conséquences

Sur un CRUD simple, la couche service ne fera parfois que transmettre l'appel au repository sans
rien ajouter. Ces classes paraîtront vides et inutiles, et la tentation sera réelle de les
court-circuiter. Elles restent nécessaires : elles réservent l'endroit où la première vraie règle
métier viendra se poser, et leur absence obligerait à réorganiser le code au pire moment.

La règle « une entité JPA ne sort jamais d'un controller » impose d'écrire un DTO même quand il est
strictement identique à l'entité, et de maintenir le mapping entre les deux. C'est du code
répétitif et sans intérêt intellectuel, à assumer.

Le domaine reste couplé à JPA. Un changement de mode de persistance toucherait `domain/`. Ce
couplage est accepté en connaissance de cause.

Cette décision serait à revoir si le domaine se mettait à porter des règles métier nombreuses ou
subtiles, ou s'il fallait exposer la même logique par plusieurs canaux. Dans ce cas, un nouvel ADR
remplacerait celui-ci.
