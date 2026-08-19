# ADR 003 : GitHub Flow

---

## Statut

** Accepté **

Date : 2026-08-19

## Contexte

Déterminer l'utilisation de Git pour un développement solo. La phase 4 de l'application est prévue en DevOps, pour travailler mon CCP sur le déploiement. Je n'aurai qu'une branche en production et une fonctionnalité en construction, pas plus.

## Options envisagées

### Option A : Git Flow (main + develop + `feature/*` + `release/*` + `hotfix/*`)

- Pour : C'est le schéma que je connais et que j'ai l'habitude d'utiliser. La release permet d'avoir une version de livraison et le hotfix permet de corriger la branche main, sans prendre develop et les fonctionnalités non encore livrées. C'est aussi un schéma pour les logiciels avec numéro de version.
- Contre : Conçu pour du travail en équipe, en solo le merge de develop sur main n'apporte rien de plus.

### Option B : GitHub Flow

- Pour : Main est la version déployable. Pas de branche develop qui merge sans apport supplémentaire. De plus l'utilisation du DevOps (CI) et des tests rend ce flow mieux adapté.
- Contre : Chaque PR doit livrer une fonctionnalité livrable.

## Décision

J'opte pour le GitHub Flow afin de standardiser un développement solo.

**Pourquoi celle-là :** Pour moderniser mon développement solo: une branche en développement et une branche en production, c'est tout. Et être raccord avec ma volonté de faire du DevOps.

**Pourquoi pas l'autre :** Pas besoin de la branche develop, qui n'apporte rien de plus au projet. Je vais finir mon développement d'une fonctionnalité avant d'en commencer une autre.

## Conséquences

Un travail plus direct. Je risque de mettre en production du code qui passe les tests mais casse en prod tout de même.
Le pipeline prévu en phase 4 avec la CI se déclenchera sur les PR sur main. Je dois donc protéger main sur GitHub pour que la branche n'accepte des push que venant d'une PR avec des tests verts.
