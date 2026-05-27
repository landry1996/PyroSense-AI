# PyroSense AI Platform — Documentation Pedagogique

> **A qui s'adresse ce document ?**
> A toute personne souhaitant comprendre PyroSense sans connaissances techniques prealables :
> gestionnaires immobiliers, decideurs, assureurs, occupants, etudiants, investisseurs.
>
> Pas de jargon technique. Pas de code. Juste des explications claires.

---

## Table des matieres

1. [C'est quoi PyroSense ?](#1-cest-quoi-pyrosense-)
2. [Le probleme qu'on resout](#2-le-probleme-quon-resout)
3. [L'analogie du medecin](#3-lanalogie-du-medecin)
4. [Comment ca marche — en 5 etapes](#4-comment-ca-marche--en-5-etapes)
5. [Le voyage d'une mesure](#5-le-voyage-dune-mesure)
6. [Le score de risque — votre thermometre](#6-le-score-de-risque--votre-thermometre)
7. [Les alertes — votre systeme d'alarme intelligent](#7-les-alertes--votre-systeme-dalarme-intelligent)
8. [L'electricien dans la boucle](#8-lelectricien-dans-la-boucle)
9. [Le systeme qui apprend](#9-le-systeme-qui-apprend)
10. [Le tableau de bord](#10-le-tableau-de-bord)
11. [Les rapports](#11-les-rapports)
12. [La securite des donnees](#12-la-securite-des-donnees)
13. [Ce que PyroSense detecte (et ne detecte pas)](#13-ce-que-pyrosense-detecte-et-ne-detecte-pas)
14. [Questions frequentes](#14-questions-frequentes)
15. [Les phases du projet](#15-les-phases-du-projet)
16. [Pour aller plus loin](#16-pour-aller-plus-loin)

---

## 1. C'est quoi PyroSense ?

**PyroSense est un gardien electrique pour vos batiments.**

Imaginez un vigile qui surveille votre installation electrique 24 heures sur 24, 7 jours sur 7, et qui vous previent des que quelque chose commence a mal tourner — bien avant qu'un probleme ne devienne dangereux.

C'est exactement ce que fait PyroSense :
- De petits capteurs sont installes dans vos tableaux electriques
- Ils ecoutent en permanence les "bruits" de votre installation
- Quand quelque chose d'inhabituel se produit, vous etes alerte
- Un electricien peut intervenir **avant** que le probleme ne s'aggrave

### En une phrase

> PyroSense detecte les problemes electriques avant qu'ils ne deviennent des incendies.

---

## 2. Le probleme qu'on resout

### Les chiffres

- **70 000** incendies d'habitation par an en France
- **25%** sont d'origine electrique
- **500** deces par an lies aux incendies
- **50 000 EUR** de degats moyens par sinistre

### Pourquoi ca arrive ?

La plupart des incendies electriques ne sont pas des "courts-circuits soudains". Ils sont le resultat d'une **degradation lente** :

```
Semaine 1    : Une vis de borne se desserre legerement (invisible)
Semaine 2-4  : La resistance de contact augmente → chaleur locale
Mois 2-3     : L'isolant autour commence a se deteriorer
Mois 4-6     : Des micro-arcs apparaissent (etincelles microscopiques)
Un jour      : L'isolant s'enflamme → INCENDIE
```

**Le probleme** : entre la semaine 1 et "un jour", personne ne surveille. L'inspection annuelle est un instantane — elle ne voit que l'etat a un moment T.

**La solution PyroSense** : surveiller en **continu** pour detecter la degradation des les premieres semaines.

---

## 3. L'analogie du medecin

PyroSense fonctionne exactement comme un cardiologue qui surveille votre coeur :

| Ce que fait le cardiologue | Ce que fait PyroSense |
|---------------------------|----------------------|
| Pose des electrodes sur votre poitrine | Installe des capteurs sur le tableau electrique |
| Mesure votre rythme cardiaque | Mesure le courant, la tension, la temperature |
| Apprend votre "normal" (60-80 bpm au repos) | Apprend le "normal" de votre installation |
| Detecte un rythme anormal | Detecte une anomalie electrique |
| Calcule un score de sante | Calcule un score de risque (0-100) |
| Vous appelle si quelque chose change | Vous envoie une alerte |
| Vous envoie chez un specialiste | Vous recommande un electricien |
| Ajuste ses seuils selon VOTRE corps | Ajuste ses seuils selon VOTRE installation |

### Points importants de cette analogie

- Le medecin ne **garantit pas** que vous n'aurez jamais de probleme cardiaque
- Il **detecte les signaux d'alerte** pour vous permettre d'agir
- Si votre voisin a un rythme cardiaque de 50 bpm, c'est normal pour lui (sportif) mais anormal pour vous
- De meme, chaque installation electrique a son propre "normal"

---

## 4. Comment ca marche — en 5 etapes

### Etape 1 : Installation du capteur

Un electricien professionnel installe un petit boitier dans votre tableau electrique. Ce boitier contient des capteurs qui mesurent :

- Le **courant** qui circule (comme un compteur d'eau pour l'electricite)
- La **tension** du reseau (la "pression" electrique)
- La **temperature** des connexions (un thermometre integre)
- Les **harmoniques** (des "vibrations" anormales dans le courant)
- Les **micro-arcs** (des mini-etincelles invisibles a l'oeil nu)

> Le capteur est **non intrusif** : il observe sans modifier votre installation.
> C'est un stethoscope, pas un pacemaker.

### Etape 2 : Transmission des donnees

Toutes les secondes, le capteur envoie ses mesures a la plateforme PyroSense via WiFi. C'est comme un SMS automatique : "Tout va bien, voici mes mesures."

Si le WiFi est coupe ? Le capteur **stocke les donnees** pendant 72 heures et les envoie quand la connexion revient. Rien n'est perdu.

### Etape 3 : Analyse par la plateforme

La plateforme recoit les mesures et les compare a ce qu'elle sait etre "normal" pour VOTRE installation :

- "La temperature est 2 degres au-dessus de votre normal — je surveille."
- "Les harmoniques ont augmente de 50% en 3 jours — je m'inquiete."
- "J'ai detecte 3 micro-arcs en 1 heure — c'est anormal."

### Etape 4 : Alerte si necessaire

Si l'analyse detecte quelque chose de preoccupant, vous recevez une alerte :

- **Legere** (INFO) : "Je surveille un changement mineur." → Rien a faire.
- **Moderee** (WARNING) : "Quelque chose merite attention." → Planifiez une inspection.
- **Urgente** (CRITICAL) : "Anomalie serieuse detectee." → Appelez un electricien rapidement.

### Etape 5 : Intervention et apprentissage

L'electricien intervient, trouve (ou ne trouve pas) un probleme, et fait son rapport. PyroSense utilise ce retour pour **s'ameliorer** :

- "J'avais raison de m'inquieter" → Je serai plus confiant la prochaine fois
- "C'etait une fausse alerte" → Je serai moins sensible a ce type de signal

---

## 5. Le voyage d'une mesure

Suivons une mesure de temperature du capteur jusqu'a votre telephone :

```
┌──────────────────────────────────────────────────────────────────┐
│                                                                    │
│  1. CAPTEUR (dans votre tableau electrique)                        │
│     "La temperature de la borne L3 est de 52°C"                   │
│     → Calcule : c'est 8°C de plus qu'hier                         │
│     → Signe le message (comme un sceau de cire)                   │
│     → Envoie via WiFi                                             │
│                                                                    │
│          ~100 millisecondes                                        │
│                     │                                              │
│                     ▼                                              │
│  2. PLATEFORME (serveur securise)                                  │
│     → Verifie le sceau (le message vient bien du capteur)          │
│     → Verifie que ce n'est pas un message rejoue (anti-fraude)     │
│     → Stocke la mesure dans l'historique                           │
│     → Compare a la baseline : "Normal pour ce capteur = 44°C"     │
│     → Conclusion : +8°C en 24h = tendance anormale                │
│                                                                    │
│          ~1 seconde                                                │
│                     │                                              │
│                     ▼                                              │
│  3. SCORING                                                        │
│     → Facteur temperature : 18/20 (contribution elevee)            │
│     → Score global : 67/100 (HIGH)                                 │
│     → Tendance : DEGRADING (ca empire)                             │
│     → Prediction : "Incident possible dans 5-10 jours"            │
│                                                                    │
│          ~2 secondes                                               │
│                     │                                              │
│                     ▼                                              │
│  4. ALERTE                                                         │
│     → Alerte CRITICAL creee                                        │
│     → "Echauffement anormal detecte sur circuit L3"               │
│     → Recommandation : "Inspection thermographique sous 48h"       │
│                                                                    │
│          ~immediat                                                 │
│                     │                                              │
│                     ▼                                              │
│  5. NOTIFICATION                                                   │
│     → SMS au gestionnaire : "Alerte critique - Batiment A"         │
│     → Push a l'electricien : "Intervention urgente assignee"       │
│     → Email avec details + recommandation                          │
│                                                                    │
│  TOTAL : moins de 5 secondes entre la mesure et votre telephone    │
│                                                                    │
└──────────────────────────────────────────────────────────────────┘
```

---

## 6. Le score de risque — votre thermometre

### C'est quoi ?

Le score de risque est un **chiffre entre 0 et 100** qui resume la sante electrique d'un point de mesure. Plus le score est eleve, plus il y a de raisons de s'inquieter.

### Comment le lire ?

```
0          30         60         80        100
│━━━━━━━━━━│━━━━━━━━━━│━━━━━━━━━━│━━━━━━━━━━│
   VERT        JAUNE      ORANGE      ROUGE
   "RAS"    "A surveiller" "Planifier" "Urgent"
```

| Couleur | Score | Ce que ca veut dire | Votre reaction |
|---------|-------|--------------------|--------------------|
| Vert | 0-29 | Tout va bien | Rien a faire |
| Jaune | 30-59 | Quelque chose evolue | Gardez un oeil |
| Orange | 60-79 | Situation preoccupante | Planifiez une inspection |
| Rouge | 80-100 | Situation serieuse | Agissez maintenant |

### D'ou vient le score ?

Le score n'est pas un chiffre magique. Il est compose de **6 facteurs concrets** :

```
Votre score est 67 (ORANGE - Situation preoccupante)

Voici pourquoi :
┌─────────────────────────────────────────────────────┐
│ Temperature        ████████████████████  18/20      │ ← Echauffement detecte
│ Micro-arcs         ████████████          12/30      │ ← Quelques impulsions
│ Harmoniques        ██████████████████    16/20      │ ← THD en hausse
│ Transitoires       ████████              8/10       │ ← Pics de courant
│ Bruit HF           ███████               7/10       │ ← Niveau modere
│ Fiabilite capteur  ██████                6/10       │ ← Capteur stable
├─────────────────────────────────────────────────────┤
│ TOTAL                                    67/100     │
└─────────────────────────────────────────────────────┘

Tendance : EN DEGRADATION (le score augmente depuis 3 jours)
Prediction : Si ca continue, incident possible dans 5-10 jours
Recommandation : Planifier inspection thermographique sous 48h
```

### Pourquoi c'est important ?

Sans le score, vous avez des dizaines de mesures techniques (volts, amperes, degres, pourcentages...) qui ne veulent rien dire pour un non-electricien.

Avec le score, vous avez **un seul chiffre** qui vous dit : "Ca va" ou "Il faut agir."

---

## 7. Les alertes — votre systeme d'alarme intelligent

### Les 3 niveaux

| Niveau | Icone | Ce que ca signifie | Ce que vous devez faire |
|--------|-------|-------------------|-----------------------|
| INFO | Bleu | "Je note un changement mineur" | Rien — pour information |
| WARNING | Orange | "Quelque chose merite votre attention" | Validez si vous voulez une inspection |
| CRITICAL | Rouge | "Anomalie serieuse detectee" | Un electricien est automatiquement alerte |

### Ce n'est PAS une alarme incendie

Important : une alerte PyroSense n'est **pas** un detecteur de fumee. Elle ne signifie pas "il y a le feu". Elle signifie : "Si on ne fait rien, dans quelques jours/semaines, ca pourrait devenir dangereux."

C'est la difference entre :
- 🚨 "Votre maison est en feu" (detecteur de fumee — trop tard)
- ⚠️ "Votre installation se degrade, planifiez une inspection" (PyroSense — a temps)

### Escalation automatique

Si personne ne reagit a une alerte CRITICAL :

```
Heure 0  : Alerte creee → notification immediate
Heure +1 : Rappel par email (premier rappel)
Heure +4 : SMS au responsable hierarchique (escalation)
Heure +8 : Notification direction (urgence maximale)
```

Le systeme s'assure qu'aucune alerte critique ne reste sans reponse.

---

## 8. L'electricien dans la boucle

### Pourquoi l'humain est indispensable

PyroSense **ne remplace pas** l'electricien. Il l'aide en lui donnant :
- La **localisation** exacte (batiment, tableau, circuit)
- Le **type de defaut** suspecte (echauffement, arc, surcharge)
- L'**historique** des mesures (depuis quand ca se degrade)
- Le **score de risque** (pour prioriser ses interventions)

Mais seul un electricien qualifie peut :
- Confirmer visuellement le defaut
- Mesurer avec ses instruments (camera thermique, etc.)
- Effectuer la reparation
- Valider que le probleme est resolu

### Le retour terrain

Apres son intervention, l'electricien indique ce qu'il a trouve :

| Ce qu'il dit | Ce que le systeme fait |
|-------------|----------------------|
| "Defaut confirme et repare" | Augmente sa confiance : "J'avais raison" |
| "Rien d'anormal trouve" | Reduit sa sensibilite : "J'etais trop sensible" |
| "Defaut trouve mais pas reparable" | Maintient l'alerte active |
| "Le capteur capte du bruit d'un appareil voisin" | Apprend a ignorer ce type de bruit |

### Le cercle vertueux

```
        Capteur detecte
             │
             ▼
      Alerte envoyee
             │
             ▼
    Electricien intervient
             │
             ▼
   Retour : "confirme" ou "faux positif"
             │
             ▼
    Systeme s'ajuste ←──────────────────┐
             │                           │
             ▼                           │
  Prochaine detection plus precise ──────┘
```

Apres quelques mois et dizaines d'interventions, le systeme connait les particularites de VOTRE installation et fait de moins en moins d'erreurs.

---

## 9. Le systeme qui apprend

### Phase d'apprentissage (7 premiers jours)

Quand un capteur est installe, il passe 7 jours a **observer** sans rien alerter. Pendant cette periode, il apprend :

- "Le courant moyen sur ce circuit est de 12A"
- "La temperature est normalement entre 38°C et 44°C"
- "Le THD est habituellement autour de 3.5%"

Chaque installation est unique. Un restaurant avec ses fours aura un profil tres different d'un bureau avec des ordinateurs.

### Adaptation continue

Le "normal" evolue avec les saisons :
- En hiver, le chauffage augmente la consommation → le capteur s'adapte
- En ete, la climatisation change le profil → le capteur s'adapte
- Apres des travaux (nouvel equipement installe) → le capteur s'adapte

Le systeme ne compare pas votre installation a une "norme universelle". Il compare votre installation... **a elle-meme d'hier**.

### Amelioration par le terrain

Chaque intervention d'un electricien enrichit le systeme :
- Les confirmations de defaut → patterns a surveiller en priorite
- Les faux positifs → bruit a ignorer
- Les conditions particulieres → contexte a prendre en compte

Apres le pilote (10 capteurs, quelques mois), le systeme sera considerablement plus precis qu'au premier jour.

---

## 10. Le tableau de bord

### A quoi ca ressemble ?

Le tableau de bord est une **page web** accessible depuis un navigateur (ordinateur, tablette, telephone). Voici ce que vous y trouvez :

### Page d'accueil

```
┌─────────────────────────────────────────────────────────────────┐
│  PyroSense AI Platform                    🔔 3  Pierre T.  ▼    │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  ┌──────┐  │
│  │  67  │  │  8/10│  │   2  │  │   1  │  │  3   │  │ 4.2  │  │
│  │Score │  │Online│  │Alertes│  │Critic│  │Interv│  │Qualite│  │
│  │moyen │  │      │  │ouvert│  │      │  │      │  │ /5   │  │
│  └──────┘  └──────┘  └──────┘  └──────┘  └──────┘  └──────┘  │
│                                                                  │
│  ┌─────────────────────────────────────────────────────────────┐│
│  │  Tendance du risque (30 derniers jours)                      ││
│  │                                                              ││
│  │  80 ─                                         ╱              ││
│  │  60 ─                              ╱─────────╱               ││
│  │  40 ─  ─────────────────╱─────────╱                          ││
│  │  20 ─                                                        ││
│  │   0 ┼────┬────┬────┬────┬────┬────┬────                     ││
│  │     1/5  5/5  10/5 15/5 20/5 25/5 28/5                      ││
│  └─────────────────────────────────────────────────────────────┘│
│                                                                  │
│  Batiments les plus a risque :                                   │
│  ┌─────────────────────────────────────┐                        │
│  │ 1. Immeuble A - Rue Victor Hugo  72 │ ████████ ORANGE        │
│  │ 2. Local commercial B            54 │ ██████   JAUNE         │
│  │ 3. Residence C                   23 │ ███      VERT          │
│  └─────────────────────────────────────┘                        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

### Les ecrans disponibles

| Ecran | Ce que vous y trouvez |
|-------|----------------------|
| **Accueil** | Vue d'ensemble : score, alertes, tendance |
| **Batiments** | Liste des batiments avec leur score de risque |
| **Batiment (detail)** | Capteurs, alertes, interventions d'un batiment |
| **Alertes** | Liste de toutes les alertes (filtres par severite, statut) |
| **Interventions** | Tableau kanban des interventions en cours |
| **Rapports** | Generation et telechargement de rapports PDF |
| **Notifications** | Historique des notifications recues |
| **Parametres** | Configuration des seuils et contacts d'urgence |

### Accessible depuis

- Navigateur web (Chrome, Firefox, Safari, Edge)
- Responsive : fonctionne sur telephone et tablette
- Connexion securisee (identifiant + mot de passe)

---

## 11. Les rapports

### Bilan Mensuel

Tous les mois, vous pouvez generer un rapport PDF professionnel qui resume :

- Combien de capteurs fonctionnent normalement
- L'evolution du score de risque sur le mois
- Combien d'alertes ont ete declenchees
- Combien d'interventions ont ete realisees
- Les batiments qui necessitent le plus d'attention
- Des recommandations concretes

### Attestation de Surveillance

Un document officiel qui prouve que votre batiment est surveille 24/7. Utile pour :
- Votre assureur (potentielle reduction de prime)
- Vos locataires (transparence sur la securite)
- Votre conformite interne

### A quoi ca ressemble ?

Un document PDF professionnel (format A4) avec :
- En-tete avec logo et reference unique
- Sections claires avec chiffres cles
- Graphiques de tendance
- Disclaimer legal (le systeme ne remplace pas un diagnostic officiel)
- Signature numerique (preuve d'integrite)

---

## 12. La securite des donnees

### Vos donnees sont protegees

| Protection | Comment |
|-----------|---------|
| **Chiffrement** | Toutes les communications sont chiffrees (comme votre banque en ligne) |
| **Authentification** | Chaque capteur a une "cle secrete" unique pour prouver son identite |
| **Anti-fraude** | Impossible de rejouer un ancien message (protection anti-replay) |
| **Isolation** | Vos donnees sont invisibles pour les autres clients |
| **Pas de donnees personnelles** | Le systeme ne stocke que des mesures electriques, pas d'informations sur les personnes |
| **Connexion securisee** | Le tableau de bord est protege par identifiant + mot de passe |

### Conformite RGPD

- **Aucune donnee personnelle** n'est collectee par les capteurs (seulement des mesures physiques)
- Les identifiants de capteurs sont **pseudonymises** dans les exports de donnees
- Pas de camera, pas de micro, pas de localisation GPS
- Droit a l'oubli applicable sur les comptes utilisateurs

### Que se passe-t-il si un capteur est vole ?

Le capteur peut etre **revoque** en un clic : ses credentials sont immediatement invalides et il ne peut plus communiquer avec la plateforme. Aucune donnee sensible n'est stockee sur le capteur lui-meme.

---

## 13. Ce que PyroSense detecte (et ne detecte pas)

### Ce que PyroSense detecte bien

| Phenomene | Exemple concret | Delai de detection |
|-----------|----------------|-------------------|
| Echauffement progressif | Borne desserree qui chauffe lentement | Jours a semaines avant danger |
| Micro-arcs recurrents | Etincelles dans un isolant degrade | Heures a jours |
| Surcharge electrique | Trop d'appareils sur un meme circuit | Immediat |
| Degradation harmonique | Pollution par equipements electroniques | Jours |
| Defaillance capteur | Le capteur lui-meme a un probleme | Immediat |

### Ce que PyroSense ne detecte PAS

| Limitation | Pourquoi |
|-----------|---------|
| Defaut dans un circuit non instrumente | Pas de capteur = pas de mesure |
| Court-circuit instantane | Trop rapide pour etre predit (mais le disjoncteur protege) |
| Defauts mecaniques sans signature electrique | Un fil coince sans contact electrique |
| Problemes en amont du tableau | Colonne montante, arrivee generale |
| Risque exterieur | Foudre, inondation, vandalisme |

### Important

> PyroSense est un **complement** aux protections existantes (disjoncteurs, differentiels, inspections).
> Il ne les remplace pas. Il ajoute une couche de surveillance **continue** la ou les protections
> classiques sont **ponctuelles** ou **reactives**.

---

## 14. Questions frequentes

### "Est-ce que ca remplace mon disjoncteur ?"

**Non.** Le disjoncteur protege contre les defauts instantanes (court-circuit, surintensity). PyroSense detecte les degradations **lentes** qui se produisent AVANT que le disjoncteur n'ait besoin d'intervenir. Les deux sont complementaires.

### "Est-ce que le capteur consomme de l'electricite ?"

Tres peu. Environ 2 watts — l'equivalent d'une veilleuse LED. Negligeable sur votre facture.

### "Que se passe-t-il si Internet est coupe ?"

Le capteur continue de mesurer et **stocke les donnees** pendant 72 heures. Quand la connexion revient, il envoie tout l'historique. Aucune donnee n'est perdue.

### "Est-ce que ca peut declencher un incendie ?"

**Non.** Le capteur est un dispositif de mesure passif. Il observe sans injecter de courant ni modifier votre installation. C'est un thermometre, pas un radiateur.

### "Qui installe le capteur ?"

Un **electricien qualifie** (habilitation B2V minimum). Jamais un particulier, meme bricoleur. L'installation implique l'ouverture du tableau electrique, ce qui necessite une habilitation.

### "Et si le systeme se trompe ?"

Ca arrive (c'est normal pour un systeme d'aide a la decision). Quand l'electricien signale un faux positif, le systeme **apprend** et reduit sa sensibilite pour ce type de signal. Avec le temps, les faux positifs diminuent.

### "C'est certifie ?"

**Pas encore.** La phase actuelle est un pilote de validation. La certification (IEC 61439, NF C 15-100) est prevue pour une phase ulterieure, avant toute commercialisation.

### "Combien ca coute ?"

Le pilote actuel est une experimentation. Le modele economique (achat capteur + abonnement plateforme) sera defini apres validation du pilote. Estimation : le cout annuel devrait etre inferieur au cout d'une seule inspection approfondie.

### "Mes donnees sont-elles partagees ?"

**Non.** Vos donnees sont strictement isolees. Aucun autre client ne peut y acceder. Elles ne sont ni vendues ni partagees avec des tiers.

### "Quelle est la duree de vie d'un capteur ?"

Objectif de conception : 5-10 ans (selon les composants electroniques). La phase pilote permettra de valider la fiabilite a long terme.

---

## 15. Les phases du projet

### Ou en est-on ?

```
Phase 1 (FAIT)       Phase 2 (FAIT)       Phase 3 (FAIT)       Phase 4 (PREVU)
┌────────────┐      ┌────────────┐      ┌────────────┐      ┌────────────┐
│ Fondations │      │ Dashboard  │      │ IoT Reel   │      │   ML/IA    │
│            │      │            │      │            │      │            │
│ - Services │      │ - Frontend │      │ - Firmware │      │ - Modeles  │
│ - Tests    │      │ - Rapports │      │ - Capteurs │      │ - Scoring+ │
│ - Archi    │      │ - Notifs   │      │ - Securite │      │ - Predict. │
│ - Scoring  │      │ - Maint.   │      │ - Pilote   │      │ - MLOps    │
└────────────┘      └────────────┘      └────────────┘      └────────────┘
     FAIT                FAIT              EN COURS             6 mois
```

### Prochaines etapes

1. **Validation hardware** : Test de stabilite 24h sur le capteur physique
2. **Pilote terrain** : 10 capteurs dans 1-3 batiments pendant 6 mois
3. **Machine Learning** : Modeles d'IA entraines sur les donnees du pilote
4. **Certification** : Tests CEM et certification reglementaire
5. **Commercialisation** : Lancement avec electriciens partenaires

### Calendrier indicatif

| Etape | Duree estimee | Prerequis |
|-------|---------------|-----------|
| Pilote 10 capteurs | 6 mois | Electricien + site valide |
| ML operationnel | +3 mois | Donnees pilote suffisantes |
| Pilote 100 capteurs | +6 mois | ML valide + 5 sites |
| Certification | +6 mois | Tests CEM + dossier technique |
| Commercialisation | +3 mois | Certification obtenue |

---

## 16. Pour aller plus loin

### Documentation detaillee disponible

| Document | Pour qui | Contenu |
|----------|----------|---------|
| Documentation Technique | Developpeurs, architectes | Architecture, API, code, deploiement |
| Documentation Fonctionnelle | Chefs de projet, PO | Specs, workflows, regles metier |
| docs/ml-strategy.md | Data scientists | Strategie ML, algorithmes, roadmap |
| docs/field-pilot-10-devices.md | Equipe pilote | Plan pilote detaille |
| docs/iot-security.md | Securite | Modele de menaces, controles |

### Contacts

| Role | Responsabilite |
|------|---------------|
| Tech Lead | Architecture technique, decisions technologiques |
| Product Manager | Vision produit, priorites fonctionnelles |
| Electricien referent | Validation terrain, installation pilote |
| Responsable securite | Conformite, donnees, certification |

### Demonstrations

Le systeme est demonstrable en local avec des donnees simulees :
- 7 scenarios de degradation electrique
- Dashboard fonctionnel avec alertes en temps reel
- Generation de rapports PDF
- Workflow complet alerte → intervention → feedback

> Pour une demonstration, contactez l'equipe projet.

---

## Resume en une page

```
┌─────────────────────────────────────────────────────────────────┐
│                                                                   │
│                    PyroSense AI Platform                           │
│                                                                   │
│    "Detecter les problemes electriques avant qu'ils               │
│     ne deviennent des incendies"                                  │
│                                                                   │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│    COMMENT :                                                      │
│    ┌─────────┐    ┌──────────┐    ┌─────────┐    ┌─────────┐   │
│    │ Capteur │───▶│ Analyse  │───▶│  Score  │───▶│ Alerte  │   │
│    │ 24/7    │    │ continue │    │ 0-100   │    │ + Notif │   │
│    └─────────┘    └──────────┘    └─────────┘    └─────────┘   │
│                                                                   │
│    POUR QUI :                                                     │
│    • Gestionnaires immobiliers (dashboard, rapports)              │
│    • Electriciens (missions ciblees, feedback)                    │
│    • Assureurs (attestation surveillance continue)                │
│                                                                   │
│    CE QUE CA CHANGE :                                             │
│    • Detection semaines avant l'incident (vs. inspection 1x/an)  │
│    • Score unique et comprehensible (vs. mesures techniques)      │
│    • Amelioration continue (vs. seuils fixes)                     │
│                                                                   │
│    LIMITES :                                                      │
│    • Ne garantit PAS l'absence d'incendie                         │
│    • Ne remplace PAS un diagnostic reglementaire                  │
│    • Installation par electricien qualifie uniquement             │
│    • Phase pilote (pas encore certifie ni commercial)             │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```
